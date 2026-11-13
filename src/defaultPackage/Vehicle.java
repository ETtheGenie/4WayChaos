package defaultPackage;

/**
 * Represents an individual vehicle moving through the simulated intersection.
 * Each vehicle tracks its identity, approach direction, intended turn, arrival
 * time, crossing progress, and current movement state.
 */
public class Vehicle
{

    private final long id;
    private final Direction approach;
    private final Turn turn;
    private final int arrivalSecond;

    private int crossingStartSecond;
    private int progressMeters;
    private VehicleState state;

    /**
     * Constructs a vehicle in the WAITING state.
     *
     * @param id
     *            unique positive vehicle identifier
     * @param approach
     *            direction the vehicle arrives from
     * @param turn
     *            intended turning movement
     * @param arrivalSecond
     *            simulated arrival time in seconds
     * @throws IllegalArgumentException
     *             if any parameter is invalid
     */
    public Vehicle(long id, Direction approach, Turn turn, int arrivalSecond)
    {

        if (id <= 0)
        {
            throw new IllegalArgumentException("Vehicle ID must be positive.");
        }

        if (approach == null || turn == null)
        {
            throw new IllegalArgumentException(
                "Approach and turn cannot be null.");
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

        crossingStartSecond = -1;
        progressMeters = 0;
        state = VehicleState.WAITING;
    }


    /**
     * Transitions the vehicle from WAITING to CROSSING.
     *
     * @param currentSecond
     *            simulated time crossing begins
     * @throws IllegalArgumentException
     *             if time is before arrival
     * @throws IllegalStateException
     *             if vehicle is not waiting
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
     * Advances the vehicle through the intersection.
     *
     * @param distanceMeters
     *            positive distance traveled
     * @throws IllegalArgumentException
     *             if distance is invalid
     * @throws IllegalStateException
     *             if vehicle is not crossing
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
     * Marks the vehicle as having completed its crossing. The simulation model
     * determines when the vehicle has traveled far enough to complete the
     * crossing.
     *
     * @throws IllegalStateException
     *             if vehicle is not crossing
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
     * Calculates time spent waiting before entering the intersection.
     *
     * @return waiting time in simulated seconds
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
     * Returns the vehicle's unique identifier.
     *
     * @return vehicle ID
     */
    public long getId()
    {
        return id;
    }


    /**
     * Returns the approach from which the vehicle arrived.
     *
     * @return approach direction
     */
    public Direction getApproach()
    {
        return approach;
    }


    /**
     * Returns the intended turning movement.
     *
     * @return vehicle turn
     */
    public Turn getTurn()
    {
        return turn;
    }


    /**
     * Returns the vehicle's simulated arrival time.
     *
     * @return arrival time in seconds
     */
    public int getArrivalSecond()
    {
        return arrivalSecond;
    }


    /**
     * Returns the time the vehicle began crossing.
     *
     * @return crossing start time, or -1 if still waiting
     */
    public int getCrossingStartSecond()
    {
        return crossingStartSecond;
    }


    /**
     * Returns distance traveled across the intersection.
     *
     * @return crossing progress in meters
     */
    public int getProgressMeters()
    {
        return progressMeters;
    }


    /**
     * Returns the current movement state.
     *
     * @return WAITING, CROSSING, or DONE
     */
    public VehicleState getState()
    {
        return state;
    }
}
