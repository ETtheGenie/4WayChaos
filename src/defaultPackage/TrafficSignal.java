package defaultPackage;

/**
 * Models the fixed-time traffic signals at a four-way intersection. The
 * intersection alternates between north-south and east-west green phases. The
 * active phase is calculated from simulation time rather than stored as mutable
 * state. This class determines whether a traffic approach is permitted to
 * release vehicles, but does not modify any vehicle queues.
 */
public class TrafficSignal
{

    private final int nsGreenSeconds;
    private final int ewGreenSeconds;

    /**
     * Constructs traffic signals with fixed green durations for each direction
     * pair. The initial phase at time zero is NS_GREEN.
     *
     * @param nsGreenSeconds
     *            north-south green duration
     * @param ewGreenSeconds
     *            east-west green duration
     * @throws IllegalArgumentException
     *             if either duration is outside the allowed limits
     */
    public TrafficSignal(int nsGreenSeconds, int ewGreenSeconds)
    {

        if (nsGreenSeconds < SimulationConfig.MIN_GREEN_SECONDS
            || nsGreenSeconds > SimulationConfig.MAX_GREEN_SECONDS)
        {
            throw new IllegalArgumentException(
                "North-south green duration must be " + "between "
                    + SimulationConfig.MIN_GREEN_SECONDS + " and "
                    + SimulationConfig.MAX_GREEN_SECONDS + " seconds.");
        }

        if (ewGreenSeconds < SimulationConfig.MIN_GREEN_SECONDS
            || ewGreenSeconds > SimulationConfig.MAX_GREEN_SECONDS)
        {
            throw new IllegalArgumentException(
                "East-west green duration must be " + "between "
                    + SimulationConfig.MIN_GREEN_SECONDS + " and "
                    + SimulationConfig.MAX_GREEN_SECONDS + " seconds.");
        }

        this.nsGreenSeconds = nsGreenSeconds;
        this.ewGreenSeconds = ewGreenSeconds;
    }


    /**
     * Calculates the active traffic signal phase at a given simulation time.
     * The intersection begins with NS_GREEN, then alternates with EW_GREEN
     * according to the configured durations.
     *
     * @param elapsedSecond
     *            zero-based simulation time
     * @return active signal phase at the given time
     * @throws IllegalArgumentException
     *             if time is negative
     */
    public SignalPhase phaseAt(int elapsedSecond)
    {

        if (elapsedSecond < 0)
        {
            throw new IllegalArgumentException(
                "Simulation time cannot be negative.");
        }

        int cycleLength = nsGreenSeconds + ewGreenSeconds;

        int cyclePosition = elapsedSecond % cycleLength;

        if (cyclePosition < nsGreenSeconds)
        {
            return SignalPhase.NS_GREEN;
        }

        return SignalPhase.EW_GREEN;
    }


    /**
     * Determines whether an incoming traffic approach has a green light at the
     * given time. NORTH and SOUTH are permitted during NS_GREEN. EAST and WEST
     * are permitted during EW_GREEN. A green signal does not guarantee
     * departure; lane queue and headway rules also apply.
     *
     * @param approach
     *            incoming vehicle direction
     * @param elapsedSecond
     *            zero-based simulation time
     * @return true if the approach has green, otherwise false
     * @throws IllegalArgumentException
     *             if direction is null or simulation time is negative
     */
    public boolean allows(Direction approach, int elapsedSecond)
    {

        if (approach == null)
        {
            throw new IllegalArgumentException("Approach cannot be null.");
        }

        SignalPhase currentPhase = phaseAt(elapsedSecond);

        switch (approach)
        {
            case NORTH:
            case SOUTH:
                return currentPhase == SignalPhase.NS_GREEN;

            case EAST:
            case WEST:
                return currentPhase == SignalPhase.EW_GREEN;

            default:
                throw new IllegalArgumentException(
                    "Unsupported traffic direction.");
        }
    }


    /**
     * Returns the configured north-south green duration.
     *
     * @return north-south green duration in seconds
     */
    public int getNsGreenSeconds()
    {
        return nsGreenSeconds;
    }


    /**
     * Returns the configured east-west green duration.
     *
     * @return east-west green duration in seconds
     */
    public int getEwGreenSeconds()
    {
        return ewGreenSeconds;
    }
}
