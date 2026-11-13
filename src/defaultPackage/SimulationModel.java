package defaultPackage;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Runs the traffic simulation for a four-way intersection. The model owns one
 * Lane per approach, the fixed-time TrafficSignal, active crossing vehicles,
 * and accumulated statistics. Each call to step() processes exactly one
 * simulated second. No view or controller objects are required.
 */
public class SimulationModel
{

    /** Constant modeled crossing distance in meters. */
    public static final int CROSSING_DISTANCE_METERS = 20;

    /** Distance a crossing vehicle travels in one simulated second. */
    public static final int VEHICLE_SPEED_METERS_PER_SECOND = 10;

    private final SimulationConfig config;
    private final EnumMap<Direction, Lane> lanes;
    private final TrafficSignal signal;
    private final List<Vehicle> crossingVehicles;
    private final StatisticsAccumulator statistics;

    private int elapsedSeconds;
    private long nextVehicleId;

    /**
     * Initializes an empty simulation from a validated configuration. A
     * separate queue is created for each incoming approach.
     *
     * @param config
     *            immutable parameters for the run
     * @throws IllegalArgumentException
     *             if config is null
     */
    public SimulationModel(SimulationConfig config)
    {
        if (config == null)
        {
            throw new IllegalArgumentException(
                "Simulation config cannot be null.");
        }

        this.config = config;
        this.lanes = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values())
        {
            lanes.put(
                direction,
                new Lane(
                    direction,
                    config.getArrivalRatePerHour(direction),
                    config.getLeftTurnPercent(),
                    config.getUTurnPercent()));
        }
        this.signal = new TrafficSignal(
            config.getNsGreenSeconds(),
            config.getEwGreenSeconds());
        this.crossingVehicles = new ArrayList<>();
        this.statistics = new StatisticsAccumulator();
        this.elapsedSeconds = 0;
        this.nextVehicleId = 1L;
    }


    /**
     * Processes one simulated second, from elapsedSeconds to elapsedSeconds +
     * 1: arrivals, green-light releases, crossing movement and completions,
     * queue sampling, then clock advancement. A newly released vehicle moves
     * during the same step.
     *
     * @throws IllegalStateException
     *             if all configured steps have run
     */
    public void step()
    {
        if (isComplete())
        {
            throw new IllegalStateException("Simulation is already complete.");
        }

        final int currentSecond = elapsedSeconds;

        // 1. Generate all due arrivals and assign unique IDs across lanes.
        for (Direction direction : Direction.values())
        {
            Lane lane = lanes.get(direction);
            int created = lane.generateArrivals(currentSecond, nextVehicleId);
            nextVehicleId += created;
            statistics.recordArrivals(created);
        }

        // 2. Release at most one waiting vehicle per eligible lane.
        for (Direction direction : Direction.values())
        {
            Lane lane = lanes.get(direction);
            boolean green = signal.allows(direction, currentSecond);
            Vehicle released = lane.releaseNext(currentSecond, green);
            if (released != null)
            {
                crossingVehicles.add(released);
            }
        }

        // 3. Move vehicles already crossing, including new releases.
        // Remove completed vehicles safely while iterating.
        Iterator<Vehicle> iterator = crossingVehicles.iterator();
        while (iterator.hasNext())
        {
            Vehicle vehicle = iterator.next();
            vehicle.advance(VEHICLE_SPEED_METERS_PER_SECOND);
            if (vehicle.getProgressMeters() >= CROSSING_DISTANCE_METERS)
            {
                vehicle.finishCrossing();
                statistics.recordCompletion(vehicle);
                iterator.remove();
            }
        }

        // 4. Sample queue sizes at the end of this simulated interval.
        statistics.sampleQueues(lanes);
        elapsedSeconds++;
    }


    /**
     * Returns a defensive, immutable aggregate view of current state. The
     * signal phase corresponds to the next interval to be processed.
     *
     * @return current snapshot of signal, queues and vehicle counts
     */
    public SimulationSnapshot getSnapshot()
    {
        Map<Direction, Integer> queueLengths = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values())
        {
            queueLengths.put(direction, lanes.get(direction).getQueueLength());
        }

        return new SimulationSnapshot(
            elapsedSeconds,
            signal.phaseAt(elapsedSeconds),
            queueLengths,
            statistics.getTotalArrived(),
            statistics.getTotalCompleted(),
            crossingVehicles.size());
    }


    /**
     * Builds an immutable report for a completed simulation.
     *
     * @return final counts and queue/wait statistics
     * @throws IllegalStateException
     *             if steps remain to be processed
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
            maximumQueues);
    }


    /**
     * Indicates whether the configured simulation duration has elapsed.
     *
     * @return true once all configured steps have been processed
     */
    public boolean isComplete()
    {
        return elapsedSeconds >= config.getDurationSeconds();
    }
}
