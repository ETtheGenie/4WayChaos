
package defaultPackage;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable read-only description of the intersection at a simulation boundary.
 * Contains the most recently applied signal phase, all waiting queues
 * (including emergency subsets), active traffic incidents, and vehicle
 * accounting. No mutable model objects are exposed. Elapsed time counts
 * completed one-second simulation steps. Signal timing describes the most
 * recently applied signal phase. Incidents are reported only when active at
 * elapsedSeconds, using [startSecond, endSecond).
 */
public final class SimulationSnapshot
{

    private final int elapsedSeconds;
    private final SignalPhase signalPhase;
    private final int phaseStartSecond;
    private final int phaseSwitchCount;
    private final Map<Direction, Integer> queueLengths;
    private final Map<Direction, Integer> emergencyQueueLengths;
    private final Map<Direction, TrafficEvent> activeEvents;
    private final int totalArrived;
    private final int totalCompleted;
    private final int crossingCount;
    private final int waitingCount;
    private final int emergencyWaitingCount;

    /**
     * Creates a validated snapshot at a simulation boundary.
     *
     * @param elapsedSeconds
     *            number of one-second steps completed
     * @param signalPhase
     *            most recently applied signal phase
     * @param phaseStartSecond
     *            time when that phase began
     * @param phaseSwitchCount
     *            total number of signal phase switches
     * @param queueLengths
     *            waiting vehicles for every incoming direction
     * @param emergencyQueueLengths
     *            emergency subset for every direction
     * @param activeEvents
     *            incidents active at elapsedSeconds, keyed by approach
     * @param totalArrived
     *            total generated vehicles, including emergencies
     * @param totalCompleted
     *            completed vehicles, including emergencies
     * @param crossingCount
     *            vehicles currently crossing
     * @throws IllegalArgumentException
     *             if data is null, incomplete, negative, inconsistent, or
     *             includes inactive events
     */
    public SimulationSnapshot(
        int elapsedSeconds,
        SignalPhase signalPhase,
        int phaseStartSecond,
        int phaseSwitchCount,
        Map<Direction, Integer> queueLengths,
        Map<Direction, Integer> emergencyQueueLengths,
        Map<Direction, TrafficEvent> activeEvents,
        int totalArrived,
        int totalCompleted,
        int crossingCount)
    {

        if (elapsedSeconds < 0)
        {
            throw new IllegalArgumentException(
                "Elapsed time cannot be negative.");
        }
        if (signalPhase == null)
        {
            throw new IllegalArgumentException("Signal phase cannot be null.");
        }
        if (phaseStartSecond < 0 || phaseStartSecond > elapsedSeconds)
        {
            throw new IllegalArgumentException(
                "Phase start must lie between zero and elapsed time.");
        }
        if (phaseSwitchCount < 0)
        {
            throw new IllegalArgumentException(
                "Phase switch count cannot be negative.");
        }
        if (totalArrived < 0 || totalCompleted < 0 || crossingCount < 0)
        {
            throw new IllegalArgumentException(
                "Vehicle counts cannot be negative.");
        }
        if (queueLengths == null
            || queueLengths.size() != Direction.values().length
            || emergencyQueueLengths == null
            || emergencyQueueLengths.size() != Direction.values().length)
        {
            throw new IllegalArgumentException(
                "Both queue maps must contain exactly four directions.");
        }
        if (activeEvents == null)
        {
            throw new IllegalArgumentException(
                "Active incident map cannot be null.");
        }

        EnumMap<Direction, Integer> queues = new EnumMap<>(Direction.class);
        EnumMap<Direction, Integer> emergencyQueues =
            new EnumMap<>(Direction.class);
        long waiting = 0L;
        long emergencyWaiting = 0L;

        for (Direction direction : Direction.values())
        {
            Integer count = queueLengths.get(direction);
            Integer emergencyCount = emergencyQueueLengths.get(direction);
            if (count == null || count < 0)
            {
                throw new IllegalArgumentException(
                    "Invalid waiting queue for " + direction + ".");
            }
            if (emergencyCount == null || emergencyCount < 0
                || emergencyCount > count)
            {
                throw new IllegalArgumentException(
                    "Invalid emergency queue for " + direction + ".");
            }
            queues.put(direction, count);
            emergencyQueues.put(direction, emergencyCount);
            waiting += count;
            emergencyWaiting += emergencyCount;
        }

        // Use long arithmetic to avoid overflow before comparing totals.
        if (waiting + (long)crossingCount + totalCompleted != totalArrived)
        {
            throw new IllegalArgumentException(
                "Arrived must equal waiting + crossing + completed.");
        }

        EnumMap<Direction, TrafficEvent> incidents =
            new EnumMap<>(Direction.class);
        for (Map.Entry<Direction, TrafficEvent> entry : activeEvents.entrySet())
        {
            Direction direction = entry.getKey();
            TrafficEvent event = entry.getValue();
            if (direction == null || event == null
                || event.getApproach() != direction
                || !event.isActiveAt(elapsedSeconds))
            {
                throw new IllegalArgumentException(
                    "Incident must match its direction and be active at "
                        + "the snapshot time.");
            }
            incidents.put(direction, event);
        }

        this.elapsedSeconds = elapsedSeconds;
        this.signalPhase = signalPhase;
        this.phaseStartSecond = phaseStartSecond;
        this.phaseSwitchCount = phaseSwitchCount;
        this.queueLengths = Collections.unmodifiableMap(queues);
        this.emergencyQueueLengths =
            Collections.unmodifiableMap(emergencyQueues);
        this.activeEvents = Collections.unmodifiableMap(incidents);
        this.totalArrived = totalArrived;
        this.totalCompleted = totalCompleted;
        this.crossingCount = crossingCount;
        this.waitingCount = (int)waiting;
        this.emergencyWaitingCount = (int)emergencyWaiting;
    }


    /**
     * @return total number of completed one-second simulation steps
     */
    public int getElapsedSeconds()
    {
        return elapsedSeconds;
    }


    /**
     * @return most recently applied NS_GREEN or EW_GREEN phase
     */
    public SignalPhase getSignalPhase()
    {
        return signalPhase;
    }


    /**
     * @return simulated second at which the current phase started
     */
    public int getPhaseStartSecond()
    {
        return phaseStartSecond;
    }


    /**
     * Returns elapsed time since the current phase began, measured at this
     * snapshot's simulation boundary.
     *
     * @return elapsed phase duration in simulated seconds
     */
    public int getPhaseElapsedSeconds()
    {
        return elapsedSeconds - phaseStartSecond;
    }


    /**
     * @return number of phase switches recorded so far
     */
    public int getPhaseSwitchCount()
    {
        return phaseSwitchCount;
    }


    /**
     * @return unmodifiable waiting queue counts for all four directions
     */
    public Map<Direction, Integer> getQueueLengths()
    {
        return queueLengths;
    }


    /**
     * Returns the waiting queue length for one incoming direction.
     *
     * @param direction
     *            approach to inspect
     * @return number of waiting vehicles
     * @throws IllegalArgumentException
     *             if direction is null
     */
    public int getQueueLength(Direction direction)
    {
        requireDirection(direction);
        return queueLengths.get(direction);
    }


    /**
     * @return unmodifiable emergency waiting counts for all four directions
     */
    public Map<Direction, Integer> getEmergencyQueueLengths()
    {
        return emergencyQueueLengths;
    }


    /**
     * Returns emergency vehicles still waiting in an approach's FIFO queue.
     *
     * @param direction
     *            approach to inspect
     * @return waiting emergency vehicle count
     * @throws IllegalArgumentException
     *             if direction is null
     */
    public int getEmergencyQueueLength(Direction direction)
    {
        requireDirection(direction);
        return emergencyQueueLengths.get(direction);
    }


    /**
     * @return unmodifiable map of active events, omitting unaffected directions
     */
    public Map<Direction, TrafficEvent> getActiveEvents()
    {
        return activeEvents;
    }


    /**
     * Returns the active event affecting a direction, if any.
     *
     * @param direction
     *            approach to inspect
     * @return active accident or slowdown, or null if none
     * @throws IllegalArgumentException
     *             if direction is null
     */
    public TrafficEvent getActiveEvent(Direction direction)
    {
        requireDirection(direction);
        return activeEvents.get(direction);
    }


    /**
     * Returns the incident classification without exposing a nullable value.
     *
     * @param direction
     *            approach to inspect
     * @return ACCIDENT, SLOWDOWN, or NONE
     * @throws IllegalArgumentException
     *             if direction is null
     */
    public TrafficEventType getActiveIncidentType(Direction direction)
    {
        TrafficEvent event = getActiveEvent(direction);
        return event == null ? TrafficEventType.NONE : event.getType();
    }


    /**
     * Returns time remaining before an incident expires at this snapshot.
     *
     * @param direction
     *            approach to inspect
     * @return remaining simulated seconds, or zero if no incident is active
     * @throws IllegalArgumentException
     *             if direction is null
     */
    public int getRemainingIncidentSeconds(Direction direction)
    {
        TrafficEvent event = getActiveEvent(direction);
        return event == null ? 0 : event.getEndSecond() - elapsedSeconds;
    }


    /**
     * @return total generated vehicles, including emergency vehicles
     */
    public int getTotalArrived()
    {
        return totalArrived;
    }


    /**
     * @return number of vehicles that have completed crossing
     */
    public int getTotalCompleted()
    {
        return totalCompleted;
    }


    /**
     * @return number of vehicles currently crossing the intersection
     */
    public int getCrossingCount()
    {
        return crossingCount;
    }


    /**
     * @return total number of waiting vehicles across all four approaches
     */
    public int getWaitingCount()
    {
        return waitingCount;
    }


    /**
     * @return total waiting emergency vehicles across all approaches
     */
    public int getEmergencyWaitingCount()
    {
        return emergencyWaitingCount;
    }


    /**
     * Validates a direction used by a per-approach getter.
     *
     * @param direction
     *            direction to validate
     * @throws IllegalArgumentException
     *             if direction is null
     */
    private static void requireDirection(Direction direction)
    {
        if (direction == null)
        {
            throw new IllegalArgumentException("Direction cannot be null.");
        }
    }
}
