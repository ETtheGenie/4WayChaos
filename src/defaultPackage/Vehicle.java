
package defaultPackage;

/**
 * Represents an individual vehicle moving through the simulated intersection.
 * Each vehicle has immutable identity, arrival, movement, and classification
 * information, along with mutable crossing progress and lifecycle state.
 * Emergency vehicles follow the same WAITING-CROSSING-DONE lifecycle as normal
 * vehicles; signal priority is determined elsewhere in the model.
 */
public class Vehicle
{

    private final long id;
    private final Direction approach;
    private final Turn turn;
    private final int arrivalSecond;
    private final VehicleType vehicleType;

    private int crossingStartSecond;
    private int progressMeters;
    private VehicleState state;

    /**
     * Creates a vehicle in the WAITING state.
     *
     * @param id
     *            globally unique positive vehicle identifier
     * @param approach
     *            incoming direction
     * @param turn
     *            intended turning movement
     * @param arrivalSecond
     *            simulated arrival time, zero or greater
     * @param vehicleType
     *            NORMAL or EMERGENCY classification
     * @throws IllegalArgumentException
     *             if any argument is invalid
     */
    public Vehicle(
        long id,
        Direction approach,
        Turn turn,
        int arrivalSecond,
        VehicleType vehicleType)
    {
        if (id <= 0)
        {
            throw new IllegalArgumentException("Vehicle ID must be positive.");
        }
        if (approach == null || turn == null || vehicleType == null)
        {
            throw new IllegalArgumentException(
                "Approach, turn, and vehicle type cannot be null.");
        }
        if (arrivalSecond < 0)
        {
            throw new IllegalArgumentException(
                "Arrival time cannot be negative.");
        }

        this.id = id;
        this.approach = approach;
        this.turn = turn;
        this.arrivalSecond = arrivalSecond;
        this.vehicleType = vehicleType;
        crossingStartSecond = -1;
        progressMeters = 0;
        state = VehicleState.WAITING;
    }


    /**
     * Transitions a waiting vehicle into the CROSSING state.
     *
     * @param currentSecond
     *            simulated time at which crossing begins
     * @throws IllegalArgumentException
     *             if currentSecond precedes arrival
     * @throws IllegalStateException
     *             if the vehicle is not WAITING
     */
    public void startCrossing(int currentSecond)
    {
        if (state != VehicleState.WAITING)
        {
            throw new IllegalStateException(
                "Vehicle must be waiting to start crossing.");
        }
        if (currentSecond < arrivalSecond)
        {
            throw new IllegalArgumentException(
                "Crossing cannot begin before arrival.");
        }
        crossingStartSecond = currentSecond;
        state = VehicleState.CROSSING;
    }


    /**
     * Adds a positive travel distance to a crossing vehicle's progress. The
     * model determines crossing speed and required distance.
     *
     * @param distanceMeters
     *            positive distance to advance, in meters
     * @throws IllegalArgumentException
     *             if distance is nonpositive or overflows
     * @throws IllegalStateException
     *             if the vehicle is not CROSSING
     */
    public void advance(int distanceMeters)
    {
        if (state != VehicleState.CROSSING)
        {
            throw new IllegalStateException(
                "Only crossing vehicles can advance.");
        }
        if (distanceMeters <= 0
            || distanceMeters > Integer.MAX_VALUE - progressMeters)
        {
            throw new IllegalArgumentException(
                "Distance must be positive and not overflow.");
        }
        progressMeters += distanceMeters;
    }


    /**
     * Transitions a crossing vehicle into the DONE state. The model is
     * responsible for checking whether the crossing distance is complete.
     *
     * @throws IllegalStateException
     *             if the vehicle is not CROSSING
     */
    public void finishCrossing()
    {
        if (state != VehicleState.CROSSING)
        {
            throw new IllegalStateException(
                "Only crossing vehicles can finish.");
        }
        state = VehicleState.DONE;
    }


    /**
     * Returns the final queue waiting time, calculated at crossing entry.
     *
     * @return seconds spent waiting before entering the intersection
     * @throws IllegalStateException
     *             if crossing has not begun
     */
    public int getWaitingTimeSeconds()
    {
        if (state == VehicleState.WAITING)
        {
            throw new IllegalStateException(
                "Waiting time is not final until crossing begins.");
        }
        return crossingStartSecond - arrivalSecond;
    }


    /**
     * @return globally unique vehicle identifier
     */
    public long getId()
    {
        return id;
    }


    /**
     * @return incoming approach direction
     */
    public Direction getApproach()
    {
        return approach;
    }


    /**
     * @return intended turning movement
     */
    public Turn getTurn()
    {
        return turn;
    }


    /**
     * @return simulated arrival time in seconds
     */
    public int getArrivalSecond()
    {
        return arrivalSecond;
    }


    /**
     * @return crossing start time, or -1 if the vehicle is still waiting
     */
    public int getCrossingStartSecond()
    {
        return crossingStartSecond;
    }


    /**
     * @return distance already traveled across the intersection, in meters
     */
    public int getProgressMeters()
    {
        return progressMeters;
    }


    /**
     * @return current WAITING, CROSSING, or DONE lifecycle state
     */
    public VehicleState getState()
    {
        return state;
    }


    /**
     * Returns the immutable vehicle classification.
     *
     * @return NORMAL or EMERGENCY vehicle type
     */
    public VehicleType getVehicleType()
    {
        return vehicleType;
    }
}
