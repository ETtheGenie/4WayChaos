
package defaultPackage;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Collects simulation statistics independently of the controller and view.
 * Tracks arrivals, completed vehicle waiting times, emergency traffic, incident
 * starts, and sampled queue lengths for every approach. Emergency vehicles are
 * included in ordinary arrival and completion totals. Only completed vehicles
 * contribute to waiting-time averages.
 */
public final class StatisticsAccumulator
{

    private int totalArrived;
    private int totalEmergencyArrived;
    private int totalCompleted;
    private int totalEmergencyCompleted;
    private long totalWaitingSecondsCompleted;
    private long totalEmergencyWaitingSecondsCompleted;
    private int totalAccidents;
    private int totalSlowdowns;
    private int queueSampleCount;

    private final EnumMap<Direction, Long> queueLengthSums;
    private final EnumMap<Direction, Integer> maximumQueueLengths;
    private final Set<Long> completedVehicleIds;

    /**
     * Creates an accumulator with empty completion history and zeroed counters.
     */
    public StatisticsAccumulator()
    {
        totalArrived = 0;
        totalEmergencyArrived = 0;
        totalCompleted = 0;
        totalEmergencyCompleted = 0;
        totalWaitingSecondsCompleted = 0L;
        totalEmergencyWaitingSecondsCompleted = 0L;
        totalAccidents = 0;
        totalSlowdowns = 0;
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
     * Records a batch of generated vehicles, including the emergency subset.
     * Callers can obtain emergency counts from changes to each Lane's
     * getTotalEmergencyGenerated() value when generating a batch.
     *
     * @param count
     *            total new arrivals, including emergency vehicles
     * @param emergencyCount
     *            emergency vehicles among these arrivals
     * @throws IllegalArgumentException
     *             if counts are invalid or totals overflow
     */
    public void recordArrivals(int count, int emergencyCount)
    {
        if (count < 0 || emergencyCount < 0 || emergencyCount > count
            || count > Integer.MAX_VALUE - totalArrived)
        {
            throw new IllegalArgumentException("Invalid arrival counts.");
        }

        totalArrived += count;
        totalEmergencyArrived += emergencyCount;
    }


    /**
     * Records one completed vehicle and its queue waiting time. A vehicle ID
     * can only be recorded once. Completed vehicles must also fit within the
     * arrival totals for their vehicle classification.
     *
     * @param vehicle
     *            completed normal or emergency vehicle
     * @throws IllegalArgumentException
     *             if vehicle is null or ID was counted
     * @throws IllegalStateException
     *             if vehicle is not DONE, or its completion would exceed
     *             recorded arrivals of its type
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

        boolean emergency = vehicle.getVehicleType() == VehicleType.EMERGENCY;
        if (emergency && totalEmergencyCompleted >= totalEmergencyArrived)
        {
            throw new IllegalStateException(
                "Emergency completions cannot exceed emergency arrivals.");
        }
        int normalArrivals = totalArrived - totalEmergencyArrived;
        int normalCompleted = totalCompleted - totalEmergencyCompleted;
        if (!emergency && normalCompleted >= normalArrivals)
        {
            throw new IllegalStateException(
                "Normal completions cannot exceed normal arrivals.");
        }

        int waitingSeconds = vehicle.getWaitingTimeSeconds();
        completedVehicleIds.add(vehicle.getId());
        totalCompleted++;
        totalWaitingSecondsCompleted += waitingSeconds;

        if (emergency)
        {
            totalEmergencyCompleted++;
            totalEmergencyWaitingSecondsCompleted += waitingSeconds;
        }
    }


    /**
     * Records one newly started accident or slowdown. Call this once for each
     * new event returned by TrafficEventManager.updateForSecond(), not once per
     * second while an event remains active.
     *
     * @param type
     *            ACCIDENT or SLOWDOWN
     * @throws IllegalArgumentException
     *             if type is null or NONE
     * @throws IllegalStateException
     *             if the relevant counter would overflow
     */
    public void recordIncidentStart(TrafficEventType type)
    {
        if (type == null || type == TrafficEventType.NONE)
        {
            throw new IllegalArgumentException(
                "An incident must be ACCIDENT or SLOWDOWN.");
        }

        if (type == TrafficEventType.ACCIDENT)
        {
            if (totalAccidents == Integer.MAX_VALUE)
            {
                throw new IllegalStateException("Accident count overflow.");
            }
            totalAccidents++;
        }
        else if (type == TrafficEventType.SLOWDOWN)
        {
            if (totalSlowdowns == Integer.MAX_VALUE)
            {
                throw new IllegalStateException("Slowdown count overflow.");
            }
            totalSlowdowns++;
        }
    }


    /**
     * Samples all four waiting queues once. Validates the complete lane map
     * before updating any statistics. Crossing vehicles are not included.
     *
     * @param lanes
     *            map containing one matching Lane for every Direction
     * @throws IllegalArgumentException
     *             if a lane is missing, null, or mismatched
     * @throws IllegalStateException
     *             if the sample counter would overflow
     */
    public void sampleQueues(Map<Direction, Lane> lanes)
    {
        if (lanes == null || lanes.size() != Direction.values().length)
        {
            throw new IllegalArgumentException("Provide exactly four lanes.");
        }

        for (Direction direction : Direction.values())
        {
            Lane lane = lanes.get(direction);
            if (lane == null || lane.getApproach() != direction)
            {
                throw new IllegalArgumentException(
                    "Invalid lane for " + direction + ".");
            }
        }

        if (queueSampleCount == Integer.MAX_VALUE)
        {
            throw new IllegalStateException("Too many queue samples.");
        }

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
     * @return total arrivals, including emergency vehicles
     */
    public int getTotalArrived()
    {
        return totalArrived;
    }


    /**
     * @return total emergency vehicles generated
     */
    public int getTotalEmergencyArrived()
    {
        return totalEmergencyArrived;
    }


    /**
     * @return total completed vehicles, including emergency vehicles
     */
    public int getTotalCompleted()
    {
        return totalCompleted;
    }


    /**
     * @return total completed emergency vehicles
     */
    public int getTotalEmergencyCompleted()
    {
        return totalEmergencyCompleted;
    }


    /**
     * @return sum of all completed vehicle queue waiting times, in seconds
     */
    public long getTotalWaitingSecondsCompleted()
    {
        return totalWaitingSecondsCompleted;
    }


    /**
     * @return sum of completed emergency vehicle waiting times, in seconds
     */
    public long getTotalEmergencyWaitingSecondsCompleted()
    {
        return totalEmergencyWaitingSecondsCompleted;
    }


    /**
     * Returns the average queue waiting time among all completed vehicles.
     * Waiting and crossing vehicles are excluded.
     *
     * @return average seconds waited, or null if no vehicles completed
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
     * Returns the average waiting time among completed emergency vehicles.
     * Emergency vehicles still waiting or crossing are excluded.
     *
     * @return average emergency wait in seconds, or null if none completed
     */
    public Double getAverageEmergencyWaitingTimeSeconds()
    {
        if (totalEmergencyCompleted == 0)
        {
            return null;
        }
        return (double)totalEmergencyWaitingSecondsCompleted
            / totalEmergencyCompleted;
    }


    /**
     * @return number of accidents started during the simulation
     */
    public int getTotalAccidents()
    {
        return totalAccidents;
    }


    /**
     * @return number of slowdowns started during the simulation
     */
    public int getTotalSlowdowns()
    {
        return totalSlowdowns;
    }


    /**
     * @return number of full four-direction queue samples recorded
     */
    public int getQueueSampleCount()
    {
        return queueSampleCount;
    }


    /**
     * Returns the average number of queued vehicles at a given approach.
     *
     * @param direction
     *            incoming approach to inspect
     * @return mean sampled queue length, or 0.0 before the first sample
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
     * Returns the greatest sampled waiting queue length for an approach.
     *
     * @param direction
     *            incoming approach to inspect
     * @return maximum sampled queue length, or zero before any samples
     * @throws IllegalArgumentException
     *             if direction is null
     */
    public int getMaximumQueueLength(Direction direction)
    {
        validateDirection(direction);
        return maximumQueueLengths.get(direction);
    }


    /**
     * Validates an incoming approach used to index queue statistics.
     *
     * @param direction
     *            approach to validate
     * @throws IllegalArgumentException
     *             if direction is null
     */
    private static void validateDirection(Direction direction)
    {
        if (direction == null)
        {
            throw new IllegalArgumentException("Direction cannot be null.");
        }
    }
}
