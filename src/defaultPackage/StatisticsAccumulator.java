package defaultPackage;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Collects traffic statistics independently of the view. Records generated
 * arrivals, completed vehicles and their waiting times, and queue-length
 * samples for each direction during the simulation.
 */
public class StatisticsAccumulator
{

    private int totalArrived;
    private int totalCompleted;
    private long totalWaitingSecondsCompleted;
    private int queueSampleCount;

    private final EnumMap<Direction, Long> queueLengthSums;
    private final EnumMap<Direction, Integer> maximumQueueLengths;
    private final Set<Long> completedVehicleIds;

    /**
     * Constructs a statistics accumulator with all counters and queue
     * statistics initialized to zero.
     */
    public StatisticsAccumulator()
    {
        totalArrived = 0;
        totalCompleted = 0;
        totalWaitingSecondsCompleted = 0L;
        queueSampleCount = 0;

        queueLengthSums = new EnumMap<>(Direction.class);
        maximumQueueLengths = new EnumMap<>(Direction.class);
        completedVehicleIds = new HashSet<>();

        for (Direction direction : Direction.values())
        {
            queueLengthSums.put(direction, 0L);
            maximumQueueLengths.put(direction, 0);
        }
    }


    /**
     * Records a batch of newly generated vehicles. Intended to receive the
     * count returned by Lane.generateArrivals().
     *
     * @param count
     *            number of arriving vehicles
     * @throws IllegalArgumentException
     *             if count is negative or causes integer overflow
     */
    public void recordArrivals(int count)
    {
        if (count < 0 || count > Integer.MAX_VALUE - totalArrived)
        {
            throw new IllegalArgumentException("Invalid arrival count.");
        }

        totalArrived += count;
    }


    /**
     * Records a vehicle that has finished crossing the intersection, including
     * its waiting time. Each completed vehicle can only be recorded once.
     *
     * @param vehicle
     *            completed vehicle
     * @throws IllegalArgumentException
     *             if vehicle is null or its completion was already recorded
     * @throws IllegalStateException
     *             if vehicle is not DONE or completions would exceed arrivals
     */
    public void recordCompletion(Vehicle vehicle)
    {
        if (vehicle == null)
        {
            throw new IllegalArgumentException("Vehicle cannot be null.");
        }

        if (vehicle.getState() != VehicleState.DONE)
        {
            throw new IllegalStateException(
                "Only DONE vehicles can be counted.");
        }

        if (completedVehicleIds.contains(vehicle.getId()))
        {
            throw new IllegalArgumentException(
                "Vehicle completion already recorded.");
        }

        if (totalCompleted >= totalArrived)
        {
            throw new IllegalStateException(
                "Completions cannot exceed arrivals.");
        }

        int waitingSeconds = vehicle.getWaitingTimeSeconds();

        completedVehicleIds.add(vehicle.getId());
        totalCompleted++;
        totalWaitingSecondsCompleted += waitingSeconds;
    }


    /**
     * Records one queue-length sample for every lane. This method should be
     * called once after each simulation step updates vehicle queues. All four
     * directions must be present, and each Lane must match its map key.
     *
     * @param lanes
     *            map containing all four lanes
     * @throws IllegalArgumentException
     *             if the map contains missing, null, or mismatched lanes
     * @throws IllegalStateException
     *             if the sample counter would overflow
     */
    public void sampleQueues(Map<Direction, Lane> lanes)
    {
        if (lanes == null || lanes.size() != Direction.values().length)
        {
            throw new IllegalArgumentException("Provide exactly four lanes.");
        }

        // Validate every lane before modifying statistics.
        for (Direction direction : Direction.values())
        {
            Lane lane = lanes.get(direction);

            if (lane == null || lane.getApproach() != direction)
            {
                throw new IllegalArgumentException(
                    "Invalid lane for " + direction);
            }
        }

        if (queueSampleCount == Integer.MAX_VALUE)
        {
            throw new IllegalStateException("Too many queue samples.");
        }

        // Record current and maximum queue lengths.
        for (Direction direction : Direction.values())
        {
            int length = lanes.get(direction).getQueueLength();

            queueLengthSums
                .put(direction, queueLengthSums.get(direction) + length);

            maximumQueueLengths.put(
                direction,
                Math.max(maximumQueueLengths.get(direction), length));
        }

        queueSampleCount++;
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
     * Returns the total number of vehicles that have finished crossing the
     * intersection.
     *
     * @return total completed vehicles
     */
    public int getTotalCompleted()
    {
        return totalCompleted;
    }


    /**
     * Returns the combined waiting time of all completed vehicles.
     *
     * @return total waiting time in seconds
     */
    public long getTotalWaitingSecondsCompleted()
    {
        return totalWaitingSecondsCompleted;
    }


    /**
     * Calculates the average waiting time of vehicles that completed the
     * intersection. Vehicles still waiting or crossing are excluded from this
     * average.
     *
     * @return average waiting time in seconds, or null if no vehicles have
     *             completed
     */
    public Double getAverageWaitingTimeSeconds()
    {
        if (totalCompleted == 0)
        {
            return null;
        }

        return (double)totalWaitingSecondsCompleted / totalCompleted;
    }


    /**
     * Returns the number of queue samples collected.
     *
     * @return total queue samples
     */
    public int getQueueSampleCount()
    {
        return queueSampleCount;
    }


    /**
     * Calculates the average queue length for a specified incoming direction.
     *
     * @param direction
     *            incoming traffic direction
     * @return average number of waiting vehicles, or zero if no samples have
     *             been taken
     * @throws IllegalArgumentException
     *             if direction is null
     */
    public double getAverageQueueLength(Direction direction)
    {

        validateDirection(direction);

        if (queueSampleCount == 0)
        {
            return 0.0;
        }

        return (double)queueLengthSums.get(direction) / queueSampleCount;
    }


    /**
     * Returns the largest observed queue length for a specified incoming
     * direction.
     *
     * @param direction
     *            incoming traffic direction
     * @return maximum number of queued vehicles, or zero if no samples have
     *             been taken
     * @throws IllegalArgumentException
     *             if direction is null
     */
    public int getMaximumQueueLength(Direction direction)
    {

        validateDirection(direction);

        return maximumQueueLengths.get(direction);
    }


    /**
     * Validates an incoming direction before accessing direction-indexed
     * statistics.
     *
     * @param direction
     *            direction to validate
     * @throws IllegalArgumentException
     *             if direction is null
     */
    private void validateDirection(Direction direction)
    {
        if (direction == null)
        {
            throw new IllegalArgumentException("Direction cannot be null.");
        }
    }
}
