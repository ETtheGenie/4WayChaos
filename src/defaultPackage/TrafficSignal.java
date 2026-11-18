
package defaultPackage;

/**
 * Stateful adaptive signal controller for the four-way intersection. Exactly
 * one direction pair is green at a time. Decisions are based on serviceable
 * waiting queues and eligible emergency requests supplied by SimulationModel.
 * The controller never accesses lanes, incidents, or vehicles. A phase is
 * guaranteed to last at least the configured minimum and switches upon reaching
 * the maximum, even when the opposing queue is empty. Updates must be applied
 * exactly once per second, beginning at second zero.
 */
public final class TrafficSignal
{

    private final int minGreenSeconds;
    private final int maxGreenSeconds;
    private final int queueSwitchThreshold;

    private SignalPhase currentPhase;
    private int phaseStartSecond;
    private int phaseSwitchCount;
    private int lastUpdatedSecond;

    /**
     * Creates an adaptive controller that starts with north-south green.
     *
     * @param options
     *            validated V2 signal options
     * @throws IllegalArgumentException
     *             if options is null
     */
    public TrafficSignal(SimulationOptions options)
    {
        if (options == null)
        {
            throw new IllegalArgumentException(
                "Simulation options cannot be null.");
        }

        minGreenSeconds = options.getMinGreenSeconds();
        maxGreenSeconds = options.getMaxGreenSeconds();
        queueSwitchThreshold = options.getQueueSwitchThreshold();

        currentPhase = SignalPhase.NS_GREEN;
        phaseStartSecond = 0;
        phaseSwitchCount = 0;
        lastUpdatedSecond = -1;
    }


    /**
     * Selects the signal phase to be used during the specified second. The
     * arguments describe queues that can actually discharge: a lane blocked by
     * an accident must contribute zero serviceable vehicles and must not
     * request emergency priority. Each queue is the combined count for one
     * opposing direction pair (NORTH+SOUTH or EAST+WEST). Switching priority
     * is: maximum green, minimum green, unopposed emergency request, then
     * opposing queue demand. Competing emergency requests keep the present
     * phase until maximum green. If there are no emergency requests, an idle
     * current phase yields to positive opposing demand; otherwise the opposing
     * queue must exceed the current queue by at least the configured threshold.
     * At the maximum, switching is forced.
     *
     * @param currentSecond
     *            next consecutive zero-based simulation second
     * @param nsServiceableQueue
     *            serviceable NORTH+SOUTH waiting vehicles
     * @param ewServiceableQueue
     *            serviceable EAST+WEST waiting vehicles
     * @param nsEmergencyPriority
     *            whether NS has a serviceable waiting emergency
     * @param ewEmergencyPriority
     *            whether EW has a serviceable waiting emergency
     * @throws IllegalArgumentException
     *             for invalid times, queue counts, or priority without
     *             serviceable vehicles
     */
    public void updateForSecond(
        int currentSecond,
        int nsServiceableQueue,
        int ewServiceableQueue,
        boolean nsEmergencyPriority,
        boolean ewEmergencyPriority)
    {

        // Validate all inputs before changing state.
        if (currentSecond < 0
            || (long)currentSecond != (long)lastUpdatedSecond + 1L)
        {
            throw new IllegalArgumentException(
                "Signal updates must start at zero and be consecutive.");
        }

        if (nsServiceableQueue < 0 || ewServiceableQueue < 0)
        {
            throw new IllegalArgumentException(
                "Serviceable queue counts cannot be negative.");
        }

        if ((nsEmergencyPriority && nsServiceableQueue == 0)
            || (ewEmergencyPriority && ewServiceableQueue == 0))
        {
            throw new IllegalArgumentException(
                "Emergency priority requires a serviceable waiting vehicle.");
        }

        int elapsedInPhase = currentSecond - phaseStartSecond;
        boolean switchPhase = false;

        if (elapsedInPhase >= maxGreenSeconds)
        {
            // Enforce maximum green, including during emergency requests.
            switchPhase = true;
        }
        else if (elapsedInPhase >= minGreenSeconds)
        {

            boolean currentIsNS = currentPhase == SignalPhase.NS_GREEN;

            boolean currentEmergency =
                currentIsNS ? nsEmergencyPriority : ewEmergencyPriority;

            boolean opposingEmergency =
                currentIsNS ? ewEmergencyPriority : nsEmergencyPriority;

            int currentQueue =
                currentIsNS ? nsServiceableQueue : ewServiceableQueue;

            int opposingQueue =
                currentIsNS ? ewServiceableQueue : nsServiceableQueue;

            if (opposingEmergency && !currentEmergency)
            {
                switchPhase = true;
            }
            else if (!currentEmergency && !opposingEmergency)
            {
                switchPhase = (currentQueue == 0 && opposingQueue > 0)
                    || (long)opposingQueue
                        - currentQueue >= queueSwitchThreshold;
            }

            // If an emergency exists in the current directions, or
            // both directions request emergency priority, retain the
            // current phase until the maximum duration is reached.
        }

        if (switchPhase)
        {
            currentPhase = currentPhase == SignalPhase.NS_GREEN
                ? SignalPhase.EW_GREEN
                : SignalPhase.NS_GREEN;

            phaseStartSecond = currentSecond;
            phaseSwitchCount++;
        }

        lastUpdatedSecond = currentSecond;
    }


    /**
     * Returns the phase selected for the most recently updated second. Before
     * the first update the initial phase is north-south green.
     *
     * @return NS_GREEN or EW_GREEN
     */
    public SignalPhase getCurrentPhase()
    {
        return currentPhase;
    }


    /**
     * Indicates whether an approach is permitted by the current signal. Actual
     * release still depends on the lane and active incident state.
     *
     * @param approach
     *            incoming approach to check
     * @return true if this direction has the current green signal
     * @throws IllegalArgumentException
     *             if approach is null
     */
    public boolean allows(Direction approach)
    {
        if (approach == null)
        {
            throw new IllegalArgumentException("Approach cannot be null.");
        }

        switch (approach)
        {
            case NORTH:
            case SOUTH:
                return currentPhase == SignalPhase.NS_GREEN;

            case EAST:
            case WEST:
                return currentPhase == SignalPhase.EW_GREEN;

            default:
                throw new IllegalArgumentException("Unsupported direction.");
        }
    }


    /**
     * Returns when the currently active phase began.
     *
     * @return inclusive simulated starting second
     */
    public int getPhaseStartSecond()
    {
        return phaseStartSecond;
    }


    /**
     * Returns the number of switches made since initialization.
     *
     * @return nonnegative count of phase changes
     */
    public int getPhaseSwitchCount()
    {
        return phaseSwitchCount;
    }


    /**
     * Returns the most recently updated simulation second.
     *
     * @return last updated second, or -1 before the first update
     */
    public int getLastUpdatedSecond()
    {
        return lastUpdatedSecond;
    }
}
