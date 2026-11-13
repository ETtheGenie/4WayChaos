package defaultPackage;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable final report for a completed intersection simulation. Contains
 * aggregate vehicle counts, average waiting time, and final, average, and
 * maximum queue lengths for each incoming direction.
 */
public class SimulationResults
{

    private final int durationSeconds;
    private final int totalArrived;
    private final int totalCompleted;
    private final int waitingCount;
    private final int crossingCount;

    private final Double averageWaitingTimeSeconds;

    private final Map<Direction, Integer> finalQueueLengths;
    private final Map<Direction, Double> averageQueueLengths;
    private final Map<Direction, Integer> maximumQueueLengths;

    /**
     * Constructs an immutable report for a completed simulation. Validates all
     * supplied statistics, including vehicle conservation and consistency of
     * queue measurements.
     *
     * @param durationSeconds
     *            total simulated duration
     * @param totalArrived
     *            number of generated vehicles
     * @param totalCompleted
     *            number of completed vehicles
     * @param crossingCount
     *            vehicles still crossing at termination
     * @param averageWaitingTimeSeconds
     *            average completed-vehicle wait, or null when no vehicles
     *            completed
     * @param finalQueueLengths
     *            final queue sizes by direction
     * @param averageQueueLengths
     *            average queue sizes by direction
     * @param maximumQueueLengths
     *            maximum queue sizes by direction
     * @throws IllegalArgumentException
     *             if data is invalid
     */
    public SimulationResults(
        int durationSeconds,
        int totalArrived,
        int totalCompleted,
        int crossingCount,
        Double averageWaitingTimeSeconds,
        Map<Direction, Integer> finalQueueLengths,
        Map<Direction, Double> averageQueueLengths,
        Map<Direction, Integer> maximumQueueLengths)
    {

        // Validate simulation duration.
        if (durationSeconds < SimulationConfig.MIN_DURATION_SECONDS
            || durationSeconds > SimulationConfig.MAX_DURATION_SECONDS)
        {

            throw new IllegalArgumentException("Invalid simulation duration.");
        }

        // Validate vehicle counts.
        if (totalArrived < 0 || totalCompleted < 0 || crossingCount < 0)
        {

            throw new IllegalArgumentException(
                "Vehicle counts cannot be negative.");
        }

        // Validate average waiting time.
        if (totalCompleted == 0)
        {
            if (averageWaitingTimeSeconds != null)
            {
                throw new IllegalArgumentException(
                    "Average waiting time must be null "
                        + "without completed vehicles.");
            }
        }
        else if (averageWaitingTimeSeconds == null
            || !Double.isFinite(averageWaitingTimeSeconds)
            || averageWaitingTimeSeconds < 0)
        {

            throw new IllegalArgumentException("Invalid average waiting time.");
        }

        // Require complete statistics for all directions.
        if (finalQueueLengths == null || averageQueueLengths == null
            || maximumQueueLengths == null
            || finalQueueLengths.size() != Direction.values().length
            || averageQueueLengths.size() != Direction.values().length
            || maximumQueueLengths.size() != Direction.values().length)
        {

            throw new IllegalArgumentException(
                "Each statistics map must contain four directions.");
        }

        // Create defensive copies.
        EnumMap<Direction, Integer> finalCopy = new EnumMap<>(Direction.class);

        EnumMap<Direction, Double> averageCopy = new EnumMap<>(Direction.class);

        EnumMap<Direction, Integer> maximumCopy =
            new EnumMap<>(Direction.class);

        long totalWaiting = 0L;

        // Validate and copy each direction's statistics.
        for (Direction direction : Direction.values())
        {

            Integer finalLength = finalQueueLengths.get(direction);

            Double averageLength = averageQueueLengths.get(direction);

            Integer maximumLength = maximumQueueLengths.get(direction);

            if (finalLength == null || finalLength < 0 || averageLength == null
                || !Double.isFinite(averageLength) || averageLength < 0
                || maximumLength == null || maximumLength < 0
                || maximumLength < finalLength || averageLength > maximumLength)
            {

                throw new IllegalArgumentException(
                    "Invalid queue statistics for " + direction + ".");
            }

            finalCopy.put(direction, finalLength);
            averageCopy.put(direction, averageLength);
            maximumCopy.put(direction, maximumLength);

            totalWaiting += finalLength;
        }

        // Verify that every generated vehicle is accounted for.
        if (totalWaiting + (long)crossingCount + totalCompleted != totalArrived)
        {

            throw new IllegalArgumentException(
                "Arrived must equal waiting + crossing + completed.");
        }

        // Store validated results.
        this.durationSeconds = durationSeconds;
        this.totalArrived = totalArrived;
        this.totalCompleted = totalCompleted;
        this.crossingCount = crossingCount;
        this.waitingCount = (int)totalWaiting;

        this.averageWaitingTimeSeconds = averageWaitingTimeSeconds;

        this.finalQueueLengths = Collections.unmodifiableMap(finalCopy);

        this.averageQueueLengths = Collections.unmodifiableMap(averageCopy);

        this.maximumQueueLengths = Collections.unmodifiableMap(maximumCopy);
    }


    /**
     * Returns the simulation duration.
     *
     * @return duration in simulated seconds
     */
    public int getDurationSeconds()
    {
        return durationSeconds;
    }


    /**
     * Returns the total number of generated vehicles.
     *
     * @return total vehicle arrivals
     */
    public int getTotalArrived()
    {
        return totalArrived;
    }


    /**
     * Returns the number of completed vehicles.
     *
     * @return total completed crossings
     */
    public int getTotalCompleted()
    {
        return totalCompleted;
    }


    /**
     * Returns the number of vehicles still queued.
     *
     * @return total waiting vehicles
     */
    public int getWaitingCount()
    {
        return waitingCount;
    }


    /**
     * Returns the number of vehicles still crossing.
     *
     * @return number of crossing vehicles
     */
    public int getCrossingCount()
    {
        return crossingCount;
    }


    /**
     * Returns the mean waiting time of completed vehicles.
     *
     * @return average waiting time in seconds, or null if no vehicles completed
     */
    public Double getAverageWaitingTimeSeconds()
    {
        return averageWaitingTimeSeconds;
    }


    /**
     * Returns final queue lengths for all directions.
     *
     * @return unmodifiable final queue-length map
     */
    public Map<Direction, Integer> getFinalQueueLengths()
    {
        return finalQueueLengths;
    }


    /**
     * Returns average queue lengths for all directions.
     *
     * @return unmodifiable average queue-length map
     */
    public Map<Direction, Double> getAverageQueueLengths()
    {
        return averageQueueLengths;
    }


    /**
     * Returns maximum queue lengths for all directions.
     *
     * @return unmodifiable maximum queue-length map
     */
    public Map<Direction, Integer> getMaximumQueueLengths()
    {
        return maximumQueueLengths;
    }


    /**
     * Returns the final queue length for one approach.
     *
     * @param direction
     *            incoming traffic direction
     * @return final number of waiting vehicles
     * @throws IllegalArgumentException
     *             if direction is null
     */
    public int getFinalQueueLength(Direction direction)
    {
        requireDirection(direction);
        return finalQueueLengths.get(direction);
    }


    /**
     * Returns the average queue length for one approach.
     *
     * @param direction
     *            incoming traffic direction
     * @return average number of waiting vehicles
     * @throws IllegalArgumentException
     *             if direction is null
     */
    public double getAverageQueueLength(Direction direction)
    {
        requireDirection(direction);
        return averageQueueLengths.get(direction);
    }


    /**
     * Returns the maximum queue length for one approach.
     *
     * @param direction
     *            incoming traffic direction
     * @return maximum observed waiting vehicles
     * @throws IllegalArgumentException
     *             if direction is null
     */
    public int getMaximumQueueLength(Direction direction)
    {
        requireDirection(direction);
        return maximumQueueLengths.get(direction);
    }


    /**
     * Validates a direction used for a statistics query.
     *
     * @param direction
     *            direction to validate
     * @throws IllegalArgumentException
     *             if direction is null
     */
    private void requireDirection(Direction direction)
    {
        if (direction == null)
        {
            throw new IllegalArgumentException("Direction cannot be null.");
        }
    }
}
