
package defaultPackage;

import java.util.Random;

/**
 * Models one incoming approach to the four-way intersection. Each lane owns a
 * FIFO queue, generates vehicles at a deterministic arrival rate, assigns
 * independent random turns and emergency classifications, and enforces
 * departure headway. Signal and incident decisions belong to the surrounding
 * simulation model, not the lane.
 */
public class Lane
{

    /** Normal minimum interval between successive lane departures. */
    public static final int DEPARTURE_HEADWAY_SECONDS = 2;

    private static final int SECONDS_PER_HOUR = 3600;
    private static final int MAX_PERCENT = 100;
    private static final int MAX_HEADWAY_SECONDS = 20;

    private final Direction approach;
    private final int arrivalRatePerHour;
    private final int leftTurnPercent;
    private final int uTurnPercent;
    private final int emergencyVehiclePercent;
    private final LinkedQueue<Vehicle> waitingVehicles;
    private final Random turnRandom;
    private final Random emergencyRandom;

    private long arrivalCreditUnits;
    private int lastGeneratedSecond;
    private int lastReleaseSecond;
    private int emergencyWaitingCount;
    private int totalEmergencyGenerated;

    /**
     * Creates an initially empty approach lane with independent random sources.
     * The caller should supply separately seeded generators for turn selection
     * and emergency classification to make runs reproducible.
     *
     * @param approach
     *            incoming approach, not null
     * @param arrivalRatePerHour
     *            arrivals per hour, between 0 and 1800
     * @param leftTurnPercent
     *            probability of a left turn, between 0 and 100
     * @param uTurnPercent
     *            probability of a U-turn, between 0 and 100
     * @param emergencyVehiclePercent
     *            probability of an emergency vehicle
     * @param turnRandom
     *            random source used only for turning decisions
     * @param emergencyRandom
     *            random source used only for vehicle classification
     * @throws IllegalArgumentException
     *             if a parameter is invalid
     */
    public Lane(
        Direction approach,
        int arrivalRatePerHour,
        int leftTurnPercent,
        int uTurnPercent,
        int emergencyVehiclePercent,
        Random turnRandom,
        Random emergencyRandom)
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
        if (leftTurnPercent < 0 || leftTurnPercent > MAX_PERCENT
            || uTurnPercent < 0 || uTurnPercent > MAX_PERCENT
            || leftTurnPercent + uTurnPercent > MAX_PERCENT)
        {
            throw new IllegalArgumentException(
                "Turn probabilities are invalid.");
        }
        if (emergencyVehiclePercent < 0
            || emergencyVehiclePercent > MAX_PERCENT)
        {
            throw new IllegalArgumentException(
                "Emergency vehicle probability must be between 0 and 100.");
        }
        if (turnRandom == null || emergencyRandom == null)
        {
            throw new IllegalArgumentException(
                "Random sources cannot be null.");
        }

        this.approach = approach;
        this.arrivalRatePerHour = arrivalRatePerHour;
        this.leftTurnPercent = leftTurnPercent;
        this.uTurnPercent = uTurnPercent;
        this.emergencyVehiclePercent = emergencyVehiclePercent;
        this.turnRandom = turnRandom;
        this.emergencyRandom = emergencyRandom;
        waitingVehicles = new LinkedQueue<>();
        arrivalCreditUnits = 0L;
        lastGeneratedSecond = -1;
        lastReleaseSecond = -1;
        emergencyWaitingCount = 0;
        totalEmergencyGenerated = 0;
    }


    /**
     * Generates arrivals for exactly one consecutive simulated second.
     * Fractional arrival credit is carried forward, so the average arrival rate
     * is exact even though each step generates an integer number of cars. Each
     * vehicle independently receives a random turn and classification.
     *
     * @param elapsedSecond
     *            next zero-based step (0, 1, 2, ...)
     * @param firstVehicleId
     *            first positive ID available for this step
     * @return number of new vehicles enqueued
     * @throws IllegalArgumentException
     *             if the step sequence or ID is invalid
     */
    public int generateArrivals(int elapsedSecond, long firstVehicleId)
    {
        if (elapsedSecond < 0
            || (long)elapsedSecond != (long)lastGeneratedSecond + 1L)
        {
            throw new IllegalArgumentException(
                "Arrival steps must start at zero and proceed in order.");
        }
        if (firstVehicleId <= 0)
        {
            throw new IllegalArgumentException(
                "First vehicle ID must be positive.");
        }

        long pendingCredit = arrivalCreditUnits + arrivalRatePerHour;
        int arrivals = (int)(pendingCredit / SECONDS_PER_HOUR);
        if (arrivals > 0 && firstVehicleId > Long.MAX_VALUE - (arrivals - 1L))
        {
            throw new IllegalArgumentException("Vehicle IDs would overflow.");
        }

        for (int i = 0; i < arrivals; i++)
        {
            Turn turn = assignRandomTurn();
            VehicleType type = assignVehicleType();
            Vehicle vehicle = new Vehicle(
                firstVehicleId + i,
                approach,
                turn,
                elapsedSecond,
                type);
            waitingVehicles.enqueue(vehicle);
            if (type == VehicleType.EMERGENCY)
            {
                emergencyWaitingCount++;
                totalEmergencyGenerated++;
            }
        }

        arrivalCreditUnits = pendingCredit % SECONDS_PER_HOUR;
        lastGeneratedSecond = elapsedSecond;
        return arrivals;
    }


    /**
     * Releases the FIFO head if the model permits release and sufficient time
     * has elapsed since the previous actual departure. A changed headway takes
     * effect immediately, including when a slowdown starts or expires.
     *
     * @param currentSecond
     *            nonnegative simulated second
     * @param canRelease
     *            true only if signal and incident rules allow departure
     * @param headwaySeconds
     *            minimum seconds since the preceding lane departure, between 2
     *            and 20 inclusive
     * @return the vehicle entering the intersection, or null if unavailable
     * @throws IllegalArgumentException
     *             if time or headway is invalid, or if a queued vehicle has not
     *             yet arrived
     */
    public
        Vehicle
        releaseNext(int currentSecond, boolean canRelease, int headwaySeconds)
    {
        if (currentSecond < 0)
        {
            throw new IllegalArgumentException(
                "Release time cannot be negative.");
        }
        if (headwaySeconds < DEPARTURE_HEADWAY_SECONDS
            || headwaySeconds > MAX_HEADWAY_SECONDS)
        {
            throw new IllegalArgumentException(
                "Departure headway must be between 2 and 20 seconds.");
        }
        if (!canRelease || waitingVehicles.isEmpty())
        {
            return null;
        }
        if (lastReleaseSecond >= 0
            && (long)currentSecond - lastReleaseSecond < headwaySeconds)
        {
            return null;
        }

        Vehicle vehicle = waitingVehicles.peek();
        if (currentSecond < vehicle.getArrivalSecond())
        {
            throw new IllegalArgumentException(
                "Vehicle cannot depart before its arrival time.");
        }

        vehicle.startCrossing(currentSecond);
        waitingVehicles.dequeue();
        lastReleaseSecond = currentSecond;
        if (vehicle.getVehicleType() == VehicleType.EMERGENCY)
        {
            emergencyWaitingCount--;
        }
        return vehicle;
    }


    /**
     * Assigns an independently sampled movement using integer percentages.
     *
     * @return the randomized turning movement
     */
    private Turn assignRandomTurn()
    {
        int draw = turnRandom.nextInt(MAX_PERCENT);
        if (draw < leftTurnPercent)
        {
            return Turn.LEFT;
        }
        if (draw < leftTurnPercent + uTurnPercent)
        {
            return Turn.U_TURN;
        }
        return Turn.STRAIGHT;
    }


    /**
     * Assigns a vehicle type using its independent random-number source.
     *
     * @return EMERGENCY with the configured probability; NORMAL otherwise
     */
    private VehicleType assignVehicleType()
    {
        return emergencyRandom.nextInt(MAX_PERCENT) < emergencyVehiclePercent
            ? VehicleType.EMERGENCY
            : VehicleType.NORMAL;
    }


    /**
     * Returns the incoming direction represented by this lane.
     *
     * @return incoming approach direction
     */
    public Direction getApproach()
    {
        return approach;
    }


    /**
     * Returns the configured deterministic arrival rate.
     *
     * @return arrivals per hour
     */
    public int getArrivalRatePerHour()
    {
        return arrivalRatePerHour;
    }


    /**
     * Returns the number of vehicles currently waiting in FIFO order.
     *
     * @return waiting queue length, excluding crossing vehicles
     */
    public int getQueueLength()
    {
        return waitingVehicles.size();
    }


    /**
     * Returns the number of emergency vehicles still in the lane's queue.
     *
     * @return emergency vehicles waiting, excluding crossing vehicles
     */
    public int getEmergencyWaitingCount()
    {
        return emergencyWaitingCount;
    }


    /**
     * Returns the cumulative number of emergency arrivals in this lane.
     *
     * @return emergency vehicles generated so far
     */
    public int getTotalEmergencyGenerated()
    {
        return totalEmergencyGenerated;
    }
}
