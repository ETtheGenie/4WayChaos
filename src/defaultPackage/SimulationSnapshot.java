package defaultPackage;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable summary of the intersection at one simulation instant. Contains
 * traffic signal status, queue lengths, and vehicle counts. The snapshot does
 * not expose mutable model objects.
 */
public class SimulationSnapshot
{

    private final int elapsedSeconds;
    private final SignalPhase signalPhase;
    private final Map<Direction, Integer> queueLengths;

    private final int totalArrived;
    private final int totalCompleted;
    private final int crossingCount;
    private final int waitingCount;

    /**
     * Creates a read-only snapshot of an intersection. Vehicle counts must obey
     * conservation: arrived = waiting + crossing + completed.
     *
     * @param elapsedSeconds
     *            completed simulation steps
     * @param signalPhase
     *            active phase at this time
     * @param queueLengths
     *            waiting vehicles in each approach
     * @param totalArrived
     *            all generated vehicles
     * @param totalCompleted
     *            vehicles that finished crossing
     * @param crossingCount
     *            vehicles currently crossing
     * @throws IllegalArgumentException
     *             for null, incomplete, negative, or inconsistent snapshot data
     */
    public SimulationSnapshot(
        int elapsedSeconds,
        SignalPhase signalPhase,
        Map<Direction, Integer> queueLengths,
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

        if (queueLengths == null
            || queueLengths.size() != Direction.values().length)
        {
            throw new IllegalArgumentException(
                "Queue lengths must include all four approaches.");
        }

        if (totalArrived < 0 || totalCompleted < 0 || crossingCount < 0)
        {
            throw new IllegalArgumentException(
                "Vehicle counts cannot be negative.");
        }

        // Copy and validate the queue lengths.
        EnumMap<Direction, Integer> copy = new EnumMap<>(Direction.class);

        long sumWaiting = 0L;

        for (Direction direction : Direction.values())
        {
            Integer count = queueLengths.get(direction);

            if (count == null || count < 0)
            {
                throw new IllegalArgumentException(
                    "Invalid queue length for " + direction + ".");
            }

            copy.put(direction, count);
            sumWaiting += count;
        }

        // Verify conservation of vehicles.
        if (sumWaiting + (long)crossingCount + totalCompleted != totalArrived)
        {
            throw new IllegalArgumentException(
                "Arrived must equal waiting + crossing + completed.");
        }

        // Store the validated state.
        this.elapsedSeconds = elapsedSeconds;
        this.signalPhase = signalPhase;
        this.queueLengths = Collections.unmodifiableMap(copy);

        this.totalArrived = totalArrived;
        this.totalCompleted = totalCompleted;
        this.crossingCount = crossingCount;
        this.waitingCount = (int)sumWaiting;
    }


    /**
     * Returns the number of completed simulation steps.
     *
     * @return elapsed simulated seconds
     */
    public int getElapsedSeconds()
    {
        return elapsedSeconds;
    }


    /**
     * Returns the active traffic signal phase.
     *
     * @return current signal phase
     */
    public SignalPhase getSignalPhase()
    {
        return signalPhase;
    }


    /**
     * Returns a read-only map of waiting vehicles for each incoming approach.
     *
     * @return unmodifiable queue-length map
     */
    public Map<Direction, Integer> getQueueLengths()
    {
        return queueLengths;
    }


    /**
     * Returns the queue length of a specified approach.
     *
     * @param direction
     *            incoming traffic direction
     * @return number of waiting vehicles
     * @throws IllegalArgumentException
     *             if direction is null
     */
    public int getQueueLength(Direction direction)
    {

        if (direction == null)
        {
            throw new IllegalArgumentException("Direction cannot be null.");
        }

        return queueLengths.get(direction);
    }


    /**
     * Returns the total number of vehicles generated since the beginning of the
     * simulation.
     *
     * @return total arrived vehicles
     */
    public int getTotalArrived()
    {
        return totalArrived;
    }


    /**
     * Returns the number of vehicles that have completed their intersection
     * crossing.
     *
     * @return total completed vehicles
     */
    public int getTotalCompleted()
    {
        return totalCompleted;
    }


    /**
     * Returns the number of vehicles currently moving through the intersection.
     *
     * @return crossing vehicle count
     */
    public int getCrossingCount()
    {
        return crossingCount;
    }


    /**
     * Returns the total number of vehicles waiting in the four approach queues.
     *
     * @return total queued vehicles
     */
    public int getWaitingCount()
    {
        return waitingCount;
    }
}
