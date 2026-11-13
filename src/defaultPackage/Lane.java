package defaultPackage;

/**
 * Represents one incoming lane at the intersection. Each lane maintains a FIFO
 * vehicle queue, generates vehicles at a deterministic arrival rate, assigns
 * turning movements, and controls departure headway. Traffic signal decisions
 * and global vehicle IDs are managed externally by SimulationModel.
 */
public class Lane
{

    public static final int DEPARTURE_HEADWAY_SECONDS = 2;

    private static final int SECONDS_PER_HOUR = 3600;
    private static final int TOTAL_PERCENT = 100;

    // Fixed lane configuration
    private final Direction approach;
    private final int arrivalRatePerHour;
    private final int leftTurnPercent;
    private final int uTurnPercent;

    // Vehicle queue
    private final LinkedQueue<Vehicle> waitingVehicles;

    // Arrival and turning accumulators
    private long arrivalCreditUnits;
    private int straightTurnCredit;
    private int leftTurnCredit;
    private int uTurnCredit;

    // Departure and arrival timing
    private int nextReleaseSecond;
    private int lastGeneratedSecond;

    /**
     * Constructs an empty lane with the specified traffic configuration.
     *
     * @param approach
     *            direction vehicles arrive from
     * @param arrivalRatePerHour
     *            vehicles arriving per hour
     * @param leftTurnPercent
     *            percentage turning left
     * @param uTurnPercent
     *            percentage making U-turns
     * @throws IllegalArgumentException
     *             if any configuration parameter is invalid
     */
    public Lane(
        Direction approach,
        int arrivalRatePerHour,
        int leftTurnPercent,
        int uTurnPercent)
    {

        if (approach == null)
        {
            throw new IllegalArgumentException("Approach cannot be null.");
        }

        if (arrivalRatePerHour < SimulationConfig.MIN_ARRIVAL_RATE
            || arrivalRatePerHour > SimulationConfig.MAX_ARRIVAL_RATE)
        {
            throw new IllegalArgumentException("Arrival rate is out of range.");
        }

        if (leftTurnPercent < SimulationConfig.MIN_TURN_PERCENT
            || leftTurnPercent > SimulationConfig.MAX_TURN_PERCENT
            || uTurnPercent < SimulationConfig.MIN_TURN_PERCENT
            || uTurnPercent > SimulationConfig.MAX_TURN_PERCENT
            || leftTurnPercent + uTurnPercent > TOTAL_PERCENT)
        {
            throw new IllegalArgumentException("Turn percentages are invalid.");
        }

        this.approach = approach;
        this.arrivalRatePerHour = arrivalRatePerHour;
        this.leftTurnPercent = leftTurnPercent;
        this.uTurnPercent = uTurnPercent;

        waitingVehicles = new LinkedQueue<>();

        arrivalCreditUnits = 0L;
        straightTurnCredit = 0;
        leftTurnCredit = 0;
        uTurnCredit = 0;

        nextReleaseSecond = 0;
        lastGeneratedSecond = -1;
    }


    /**
     * Generates any vehicles due during one simulated second and enqueues them
     * in FIFO order. Calls must process steps 0, 1, 2, ... exactly once and in
     * sequence. SimulationModel supplies the next globally unique vehicle ID
     * and increments it by the returned count.
     *
     * @param elapsedSecond
     *            zero-based simulation step
     * @param firstVehicleId
     *            first ID available for arrivals
     * @return number of vehicles generated during this step
     * @throws IllegalArgumentException
     *             if time, step order, or vehicle ID is invalid
     */
    public int generateArrivals(int elapsedSecond, long firstVehicleId)
    {

        if (elapsedSecond < 0
            || (long)elapsedSecond != (long)lastGeneratedSecond + 1L)
        {
            throw new IllegalArgumentException(
                "Arrival steps must be processed once, "
                    + "in order, starting at zero.");
        }

        if (firstVehicleId <= 0L)
        {
            throw new IllegalArgumentException(
                "First vehicle ID must be positive.");
        }

        // Accumulate arrival rate over one second.
        long pendingCredit = arrivalCreditUnits + arrivalRatePerHour;

        int arrivals = (int)(pendingCredit / SECONDS_PER_HOUR);

        // Prevent vehicle ID overflow.
        if (arrivals > 0 && firstVehicleId > Long.MAX_VALUE - (arrivals - 1L))
        {
            throw new IllegalArgumentException("Vehicle IDs would overflow.");
        }

        // Create and enqueue new vehicles.
        for (int i = 0; i < arrivals; i++)
        {
            Turn turn = assignTurn();

            Vehicle vehicle =
                new Vehicle(firstVehicleId + i, approach, turn, elapsedSecond);

            waitingVehicles.enqueue(vehicle);
        }

        // Preserve remaining fractional arrival credit.
        arrivalCreditUnits = pendingCredit % SECONDS_PER_HOUR;

        lastGeneratedSecond = elapsedSecond;

        return arrivals;
    }


    /**
     * Releases the first waiting vehicle if its signal is green and departure
     * headway permits. If release is not permitted, returns null without
     * changing the queue.
     *
     * @param currentSecond
     *            current simulation second
     * @param signalGreen
     *            true if lane has green signal
     * @return released vehicle, or null if none released
     * @throws IllegalArgumentException
     *             if release time is invalid
     */
    public Vehicle releaseNext(int currentSecond, boolean signalGreen)
    {

        if (currentSecond < 0)
        {
            throw new IllegalArgumentException("Time cannot be negative.");
        }

        // No departure during red or without vehicles.
        if (!signalGreen || waitingVehicles.isEmpty()
            || currentSecond < nextReleaseSecond)
        {
            return null;
        }

        Vehicle vehicle = waitingVehicles.peek();

        if (currentSecond < vehicle.getArrivalSecond()
            || currentSecond > Integer.MAX_VALUE - DEPARTURE_HEADWAY_SECONDS)
        {
            throw new IllegalArgumentException("Invalid release time.");
        }

        // Transition the vehicle before removing it.
        vehicle.startCrossing(currentSecond);

        waitingVehicles.dequeue();

        // Enforce minimum time before next departure.
        nextReleaseSecond = currentSecond + DEPARTURE_HEADWAY_SECONDS;

        return vehicle;
    }


    /**
     * Selects a deterministic turning movement using weighted credits for
     * straight, left, and U-turns. The method distributes movements across
     * arrivals while producing the exact configured percentages over each
     * complete set of 100 generated vehicles.
     *
     * @return turning movement for the next vehicle
     */
    private Turn assignTurn()
    {

        straightTurnCredit += TOTAL_PERCENT - leftTurnPercent - uTurnPercent;

        leftTurnCredit += leftTurnPercent;
        uTurnCredit += uTurnPercent;

        // Select movement with the greatest credit.
        if (straightTurnCredit >= leftTurnCredit
            && straightTurnCredit >= uTurnCredit)
        {

            straightTurnCredit -= TOTAL_PERCENT;
            return Turn.STRAIGHT;
        }

        if (leftTurnCredit >= uTurnCredit)
        {
            leftTurnCredit -= TOTAL_PERCENT;
            return Turn.LEFT;
        }

        uTurnCredit -= TOTAL_PERCENT;
        return Turn.U_TURN;
    }


    /**
     * Returns the incoming approach direction.
     *
     * @return lane approach direction
     */
    public Direction getApproach()
    {
        return approach;
    }


    /**
     * Returns the configured arrival rate.
     *
     * @return vehicles per simulated hour
     */
    public int getArrivalRatePerHour()
    {
        return arrivalRatePerHour;
    }


    /**
     * Returns the number of waiting vehicles. Does not include vehicles already
     * crossing.
     *
     * @return number of queued vehicles
     */
    public int getQueueLength()
    {
        return waitingVehicles.size();
    }
}
