
package defaultPackage;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Coordinates one V2 four-way intersection simulation without any view or
 * controller dependencies. Each call to step() processes exactly one simulated
 * second: incidents, arrivals, adaptive signal selection, lane departures,
 * crossing movement, completions, and queue statistics, in that order. All
 * random sources are derived from one configured seed. Each lane receives
 * separate turning and emergency streams, and incidents use a further stream.
 * The same configuration and seed reproduce the same simulation behavior.
 */
public final class SimulationModel
{

    /** Distance required to complete the modeled intersection crossing. */
    public static final int CROSSING_DISTANCE_METERS = 20;

    /** Normal progress made in one simulated second, in meters. */
    public static final int VEHICLE_SPEED_METERS_PER_SECOND = 10;

    private final SimulationConfig config;
    private final EnumMap<Direction, Lane> lanes;
    private final TrafficSignal signal;
    private final TrafficEventManager eventManager;
    private final List<Vehicle> crossingVehicles;
    private final StatisticsAccumulator statistics;

    private int elapsedSeconds;
    private long nextVehicleId;

    /**
     * Creates an empty model with four FIFO lanes, an adaptive signal, and a
     * seeded event manager. The simulation does not advance until step() runs.
     *
     * @param config
     *            immutable, validated V2 simulation settings
     * @throws IllegalArgumentException
     *             if config is null
     */
    public SimulationModel(SimulationConfig config)
    {
        if (config == null)
        {
            throw new IllegalArgumentException(
                "Simulation configuration cannot be null.");
        }

        this.config = config;
        SimulationOptions options = config.getOptions();
        Random seedSource = new Random(options.getRandomSeed());

        lanes = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values())
        {
            lanes.put(
                direction,
                new Lane(
                    direction,
                    config.getArrivalRatePerHour(direction),
                    config.getLeftTurnPercent(),
                    config.getUTurnPercent(),
                    options.getEmergencyVehiclePercent(),
                    new Random(seedSource.nextLong()),
                    new Random(seedSource.nextLong())));
        }

        signal = new TrafficSignal(options);
        eventManager =
            new TrafficEventManager(options, new Random(seedSource.nextLong()));
        crossingVehicles = new ArrayList<>();
        statistics = new StatisticsAccumulator();
        elapsedSeconds = 0;
        nextVehicleId = 1L;
    }


    /**
     * Processes the interval [elapsedSeconds, elapsedSeconds + 1). Newly
     * released cars move immediately in the same interval. Incidents starting
     * at t affect that interval; incidents ending at t do not. The adaptive
     * signal uses queues after this second's arrivals and after incident
     * changes, but before departures. Each direction can release at most one
     * car per second, subject to headway and signal/incident rules.
     *
     * @throws IllegalStateException
     *             if the configured run has already ended
     */
    public void step()
    {
        if (isComplete())
        {
            throw new IllegalStateException("Simulation is already complete.");
        }

        int currentSecond = elapsedSeconds;

        // Events are updated before traffic decisions, so a new accident
        // blocks releases immediately and an expired accident no longer does.
        List<TrafficEvent> startedEvents =
            eventManager.updateForSecond(currentSecond);
        for (TrafficEvent event : startedEvents)
        {
            statistics.recordIncidentStart(event.getType());
        }

        generateArrivalsForSecond(currentSecond);
        updateSignalForSecond(currentSecond);
        releaseVehiclesForSecond(currentSecond);
        advanceCrossingVehicles();
        statistics.sampleQueues(lanes);
        elapsedSeconds++;
    }


    /**
     * Generates arrivals in stable Direction.values() order, assigning unique
     * IDs across all approaches and recording emergency arrivals as subsets.
     *
     * @param currentSecond
     *            simulated interval currently being processed
     */
    private void generateArrivalsForSecond(int currentSecond)
    {
        for (Direction direction : Direction.values())
        {
            Lane lane = lanes.get(direction);
            int emergencyBefore = lane.getTotalEmergencyGenerated();
            int created = lane.generateArrivals(currentSecond, nextVehicleId);
            int emergencyCreated =
                lane.getTotalEmergencyGenerated() - emergencyBefore;

            statistics.recordArrivals(created, emergencyCreated);
            nextVehicleId += created;
        }
    }


    /**
     * Aggregates demand and emergency requests from lanes without active
     * accidents, then selects the phase for the current interval. An emergency
     * anywhere in a serviceable FIFO queue can request green, but it cannot
     * jump ahead of other queued vehicles. The TrafficSignal enforces minimum
     * and maximum green durations.
     *
     * @param currentSecond
     *            simulated interval currently being processed
     */
    private void updateSignalForSecond(int currentSecond)
    {
        int nsQueue = 0;
        int ewQueue = 0;
        boolean nsEmergency = false;
        boolean ewEmergency = false;

        for (Direction direction : Direction.values())
        {
            if (eventManager.isAccidentActive(direction))
            {
                continue;
            }

            Lane lane = lanes.get(direction);
            boolean northSouth =
                direction == Direction.NORTH || direction == Direction.SOUTH;

            if (northSouth)
            {
                nsQueue += lane.getQueueLength();
                nsEmergency |= lane.getEmergencyWaitingCount() > 0;
            }
            else
            {
                ewQueue += lane.getQueueLength();
                ewEmergency |= lane.getEmergencyWaitingCount() > 0;
            }
        }

        signal.updateForSecond(
            currentSecond,
            nsQueue,
            ewQueue,
            nsEmergency,
            ewEmergency);
    }


    /**
     * Releases at most one vehicle per approach on an eligible green signal.
     * Accidents prevent release; slowdowns increase the departure headway.
     * Vehicles retain FIFO order even when requesting emergency priority.
     *
     * @param currentSecond
     *            simulated interval currently being processed
     */
    private void releaseVehiclesForSecond(int currentSecond)
    {
        SimulationOptions options = config.getOptions();

        for (Direction direction : Direction.values())
        {
            boolean canRelease = signal.allows(direction)
                && !eventManager.isAccidentActive(direction);
            int headwaySeconds = eventManager.isSlowdownActive(direction)
                ? options.getSlowdownHeadwaySeconds()
                : Lane.DEPARTURE_HEADWAY_SECONDS;

            Vehicle released = lanes.get(direction)
                .releaseNext(currentSecond, canRelease, headwaySeconds);
            if (released != null)
            {
                crossingVehicles.add(released);
            }
        }
    }


    /**
     * Advances all vehicles currently in the intersection, including new
     * departures. During a slowdown, all crossing vehicles originating from the
     * affected approach move at reduced speed, even if they entered before the
     * event started. An accident affects departures only, so vehicles already
     * crossing continue normally. Completed vehicles are counted once and
     * safely removed from the active crossing list.
     */
    private void advanceCrossingVehicles()
    {
        int slowSpeed = config.getOptions().getSlowdownSpeedMetersPerSecond();

        Iterator<Vehicle> iterator = crossingVehicles.iterator();
        while (iterator.hasNext())
        {
            Vehicle vehicle = iterator.next();
            int speed = eventManager.isSlowdownActive(vehicle.getApproach())
                ? slowSpeed
                : VEHICLE_SPEED_METERS_PER_SECOND;

            vehicle.advance(speed);
            if (vehicle.getProgressMeters() >= CROSSING_DISTANCE_METERS)
            {
                vehicle.finishCrossing();
                statistics.recordCompletion(vehicle);
                iterator.remove();
            }
        }
    }


    /**
     * Returns an immutable view of the state at the current step boundary. The
     * phase shown is the phase used in the most recently completed step; before
     * the first step it is the initial NS_GREEN phase. Incident status is
     * filtered against elapsedSeconds, excluding events whose end is that
     * boundary even if the manager has not yet processed the next step.
     *
     * @return defensive immutable aggregate of signal, queues, incidents,
     *             emergency waiting vehicles, and vehicle totals
     */
    public SimulationSnapshot getSnapshot()
    {
        Map<Direction, Integer> queues = new EnumMap<>(Direction.class);
        Map<Direction, Integer> emergencyQueues =
            new EnumMap<>(Direction.class);

        for (Direction direction : Direction.values())
        {
            Lane lane = lanes.get(direction);
            queues.put(direction, lane.getQueueLength());
            emergencyQueues.put(direction, lane.getEmergencyWaitingCount());
        }

        return new SimulationSnapshot(
            elapsedSeconds,
            signal.getCurrentPhase(),
            signal.getPhaseStartSecond(),
            signal.getPhaseSwitchCount(),
            queues,
            emergencyQueues,
            eventManager.getActiveEventsAt(elapsedSeconds),
            statistics.getTotalArrived(),
            statistics.getTotalCompleted(),
            crossingVehicles.size());
    }


    /**
     * Produces the immutable final report using statistics collected from all
     * completed intervals. Cars remaining in queues or crossing at termination
     * are not silently discarded or treated as completed.
     *
     * @return completed simulation results with overall and emergency metrics,
     *             incident counts, adaptive switch count, and queue statistics
     * @throws IllegalStateException
     *             if simulation steps remain
     */
    public SimulationResults getResults()
    {
        if (!isComplete())
        {
            throw new IllegalStateException("Simulation has not finished.");
        }

        Map<Direction, Integer> finalQueues = new EnumMap<>(Direction.class);
        Map<Direction, Double> averageQueues = new EnumMap<>(Direction.class);
        Map<Direction, Integer> maximumQueues = new EnumMap<>(Direction.class);

        for (Direction direction : Direction.values())
        {
            finalQueues.put(direction, lanes.get(direction).getQueueLength());
            averageQueues
                .put(direction, statistics.getAverageQueueLength(direction));
            maximumQueues
                .put(direction, statistics.getMaximumQueueLength(direction));
        }

        return new SimulationResults(
            config.getDurationSeconds(),
            statistics.getTotalArrived(),
            statistics.getTotalCompleted(),
            crossingVehicles.size(),
            statistics.getAverageWaitingTimeSeconds(),
            finalQueues,
            averageQueues,
            maximumQueues,
            statistics.getTotalEmergencyArrived(),
            statistics.getTotalEmergencyCompleted(),
            statistics.getAverageEmergencyWaitingTimeSeconds(),
            statistics.getTotalAccidents(),
            statistics.getTotalSlowdowns(),
            signal.getPhaseSwitchCount());
    }


    /**
     * Reports whether all configured one-second intervals have been processed.
     *
     * @return true after durationSeconds calls to step(), otherwise false
     */
    public boolean isComplete()
    {
        return elapsedSeconds >= config.getDurationSeconds();
    }
}
