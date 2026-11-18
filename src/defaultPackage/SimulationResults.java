
package defaultPackage;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable final report for a completed V2 intersection simulation. Includes
 * overall and emergency vehicle performance, incident counts, adaptive signal
 * switching, and final, average, and maximum queue lengths. Emergency vehicles
 * are included in the overall arrival/completion totals.
 */
public final class SimulationResults
{

    private final int durationSeconds;
    private final int totalArrived;
    private final int totalCompleted;
    private final int waitingCount;
    private final int crossingCount;
    private final int totalEmergencyArrived;
    private final int totalEmergencyCompleted;
    private final int totalAccidents;
    private final int totalSlowdowns;
    private final int phaseSwitchCount;

    private final Double averageWaitingTimeSeconds;
    private final Double averageEmergencyWaitingTimeSeconds;

    private final Map<Direction, Integer> finalQueueLengths;
    private final Map<Direction, Double> averageQueueLengths;
    private final Map<Direction, Integer> maximumQueueLengths;

    /**
     * Constructs a validated immutable report for one completed simulation.
     * Queue waiting-time averages include only vehicles that have completed.
     * Each incident is counted once at its start, not once per active second.
     *
     * @param durationSeconds
     *            total simulated duration in seconds
     * @param totalArrived
     *            all generated vehicles, including emergencies
     * @param totalCompleted
     *            all completed crossings, including emergencies
     * @param crossingCount
     *            vehicles still crossing at simulation termination
     * @param averageWaitingTimeSeconds
     *            average wait of completed vehicles, or null if none completed
     * @param finalQueueLengths
     *            final waiting queue length per direction
     * @param averageQueueLengths
     *            mean sampled waiting queue per direction
     * @param maximumQueueLengths
     *            greatest sampled queue per direction
     * @param totalEmergencyArrived
     *            generated emergency vehicles (subset)
     * @param totalEmergencyCompleted
     *            completed emergency vehicles (subset)
     * @param averageEmergencyWaitingTimeSeconds
     *            average wait of completed emergency vehicles, or null if none
     *            completed
     * @param totalAccidents
     *            number of accidents started during the run
     * @param totalSlowdowns
     *            number of slowdowns started during the run
     * @param phaseSwitchCount
     *            number of adaptive signal phase changes
     * @throws IllegalArgumentException
     *             if any supplied data is inconsistent
     */
    public SimulationResults(
        int durationSeconds,
        int totalArrived,
        int totalCompleted,
        int crossingCount,
        Double averageWaitingTimeSeconds,
        Map<Direction, Integer> finalQueueLengths,
        Map<Direction, Double> averageQueueLengths,
        Map<Direction, Integer> maximumQueueLengths,
        int totalEmergencyArrived,
        int totalEmergencyCompleted,
        Double averageEmergencyWaitingTimeSeconds,
        int totalAccidents,
        int totalSlowdowns,
        int phaseSwitchCount)
    {

        if (durationSeconds < SimulationConfig.MIN_DURATION_SECONDS
            || durationSeconds > SimulationConfig.MAX_DURATION_SECONDS)
        {
            throw new IllegalArgumentException("Invalid simulation duration.");
        }
        if (totalArrived < 0 || totalCompleted < 0 || crossingCount < 0)
        {
            throw new IllegalArgumentException(
                "Vehicle counts cannot be negative.");
        }
        if (totalEmergencyArrived < 0 || totalEmergencyArrived > totalArrived
            || totalEmergencyCompleted < 0
            || totalEmergencyCompleted > totalEmergencyArrived
            || totalEmergencyCompleted > totalCompleted
            || totalCompleted - totalEmergencyCompleted > totalArrived
                - totalEmergencyArrived)
        {
            throw new IllegalArgumentException(
                "Emergency and normal completion counts must be subsets of arrivals.");
        }
        if (totalAccidents < 0 || totalSlowdowns < 0 || phaseSwitchCount < 0
            || phaseSwitchCount > durationSeconds)
        {
            throw new IllegalArgumentException(
                "Incident and phase-switch counts must be valid.");
        }
        requireAverage(
            "Overall waiting time",
            averageWaitingTimeSeconds,
            totalCompleted);
        requireAverage(
            "Emergency waiting time",
            averageEmergencyWaitingTimeSeconds,
            totalEmergencyCompleted);

        if (finalQueueLengths == null || averageQueueLengths == null
            || maximumQueueLengths == null
            || finalQueueLengths.size() != Direction.values().length
            || averageQueueLengths.size() != Direction.values().length
            || maximumQueueLengths.size() != Direction.values().length)
        {
            throw new IllegalArgumentException(
                "Each queue map must contain exactly four directions.");
        }

        EnumMap<Direction, Integer> finalCopy = new EnumMap<>(Direction.class);
        EnumMap<Direction, Double> averageCopy = new EnumMap<>(Direction.class);
        EnumMap<Direction, Integer> maximumCopy =
            new EnumMap<>(Direction.class);
        long totalWaiting = 0L;

        for (Direction direction : Direction.values())
        {
            Integer finalLength = finalQueueLengths.get(direction);
            Double averageLength = averageQueueLengths.get(direction);
            Integer maximumLength = maximumQueueLengths.get(direction);

            if (finalLength == null || finalLength < 0 || averageLength == null
                || !Double.isFinite(averageLength) || averageLength < 0.0
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

        // Long arithmetic ensures no overflow before conservation is checked.
        if (totalWaiting + (long)crossingCount + totalCompleted != totalArrived)
        {
            throw new IllegalArgumentException(
                "Arrived must equal waiting + crossing + completed.");
        }

        this.durationSeconds = durationSeconds;
        this.totalArrived = totalArrived;
        this.totalCompleted = totalCompleted;
        this.waitingCount = (int)totalWaiting;
        this.crossingCount = crossingCount;
        this.totalEmergencyArrived = totalEmergencyArrived;
        this.totalEmergencyCompleted = totalEmergencyCompleted;
        this.averageWaitingTimeSeconds = averageWaitingTimeSeconds;
        this.averageEmergencyWaitingTimeSeconds =
            averageEmergencyWaitingTimeSeconds;
        this.totalAccidents = totalAccidents;
        this.totalSlowdowns = totalSlowdowns;
        this.phaseSwitchCount = phaseSwitchCount;
        this.finalQueueLengths = Collections.unmodifiableMap(finalCopy);
        this.averageQueueLengths = Collections.unmodifiableMap(averageCopy);
        this.maximumQueueLengths = Collections.unmodifiableMap(maximumCopy);
    }


    /**
     * Checks that a waiting-time average is defined exactly when its group
     * contains at least one completed vehicle.
     *
     * @param label
     *            metric name included in an error message
     * @param average
     *            nullable mean waiting time in seconds
     * @param completed
     *            number of completed vehicles in the group
     * @throws IllegalArgumentException
     *             for missing, unexpected, or bad averages
     */
    private static
        void
        requireAverage(String label, Double average, int completed)
    {
        if ((completed == 0 && average != null) || (completed > 0
            && (average == null || !Double.isFinite(average) || average < 0.0)))
        {
            throw new IllegalArgumentException("Invalid " + label + ".");
        }
    }


    /**
     * @return total simulation duration in seconds
     */
    public int getDurationSeconds()
    {
        return durationSeconds;
    }


    /**
     * @return total arrivals, including emergency vehicles
     */
    public int getTotalArrived()
    {
        return totalArrived;
    }


    /**
     * @return total completed vehicles, including emergency vehicles
     */
    public int getTotalCompleted()
    {
        return totalCompleted;
    }


    /**
     * @return vehicles still waiting in all four approach queues
     */
    public int getWaitingCount()
    {
        return waitingCount;
    }


    /**
     * @return vehicles still crossing at the end of the run
     */
    public int getCrossingCount()
    {
        return crossingCount;
    }


    /**
     * @return average queue wait in seconds, or null if none completed
     */
    public Double getAverageWaitingTimeSeconds()
    {
        return averageWaitingTimeSeconds;
    }


    /**
     * @return emergency vehicles generated, included in total arrivals
     */
    public int getTotalEmergencyArrived()
    {
        return totalEmergencyArrived;
    }


    /**
     * @return emergency vehicles completed, included in total completions
     */
    public int getTotalEmergencyCompleted()
    {
        return totalEmergencyCompleted;
    }


    /**
     * @return emergency vehicles that have not completed by the end of the run
     */
    public int getEmergencyUnfinishedCount()
    {
        return totalEmergencyArrived - totalEmergencyCompleted;
    }


    /**
     * @return average completed emergency vehicle wait, or null if none
     *             completed
     */
    public Double getAverageEmergencyWaitingTimeSeconds()
    {
        return averageEmergencyWaitingTimeSeconds;
    }


    /**
     * @return total accidents that began during the run
     */
    public int getTotalAccidents()
    {
        return totalAccidents;
    }


    /**
     * @return total slowdowns that began during the run
     */
    public int getTotalSlowdowns()
    {
        return totalSlowdowns;
    }


    /**
     * @return total signal phase changes during the run
     */
    public int getPhaseSwitchCount()
    {
        return phaseSwitchCount;
    }


    /**
     * @return immutable map of final waiting queue lengths
     */
    public Map<Direction, Integer> getFinalQueueLengths()
    {
        return finalQueueLengths;
    }


    /**
     * @return immutable map of mean sampled waiting queue lengths
     */
    public Map<Direction, Double> getAverageQueueLengths()
    {
        return averageQueueLengths;
    }


    /**
     * @return immutable map of maximum sampled waiting queue lengths
     */
    public Map<Direction, Integer> getMaximumQueueLengths()
    {
        return maximumQueueLengths;
    }


    /**
     * Returns the number of vehicles waiting in an approach at termination.
     *
     * @param direction
     *            incoming approach
     * @return final waiting queue length
     * @throws IllegalArgumentException
     *             if direction is null
     */
    public int getFinalQueueLength(Direction direction)
    {
        requireDirection(direction);
        return finalQueueLengths.get(direction);
    }


    /**
     * Returns average waiting queue length in a given approach over the run.
     *
     * @param direction
     *            incoming approach
     * @return average waiting queue length
     * @throws IllegalArgumentException
     *             if direction is null
     */
    public double getAverageQueueLength(Direction direction)
    {
        requireDirection(direction);
        return averageQueueLengths.get(direction);
    }


    /**
     * Returns the largest sampled queue length for a given approach.
     *
     * @param direction
     *            incoming approach
     * @return maximum waiting queue length
     * @throws IllegalArgumentException
     *             if direction is null
     */
    public int getMaximumQueueLength(Direction direction)
    {
        requireDirection(direction);
        return maximumQueueLengths.get(direction);
    }


    /**
     * Validates a direction used for queue statistics queries.
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
