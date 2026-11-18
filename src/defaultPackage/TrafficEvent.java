
package defaultPackage;

/**
 * Immutable record of an accident or slowdown affecting one incoming
 * intersection approach. Event timing uses simulated seconds and the half-open
 * interval [startSecond, endSecond). An event describes an incident but does
 * not apply traffic effects. TrafficEventManager manages active events and
 * SimulationModel applies blocking or speed changes.
 */
public final class TrafficEvent
{

    private final TrafficEventType type;
    private final Direction approach;
    private final int startSecond;
    private final int durationSeconds;
    private final int endSecond;

    /**
     * Creates a validated accident or slowdown event.
     *
     * @param type
     *            ACCIDENT or SLOWDOWN; NONE is not a real event
     * @param approach
     *            incoming direction affected by the incident
     * @param startSecond
     *            nonnegative simulated starting second
     * @param durationSeconds
     *            duration within SimulationOptions event limits
     * @throws IllegalArgumentException
     *             if an argument is invalid or the calculated end time
     *             overflows int
     */
    public TrafficEvent(
        TrafficEventType type,
        Direction approach,
        int startSecond,
        int durationSeconds)
    {
        if (type == null || type == TrafficEventType.NONE)
        {
            throw new IllegalArgumentException(
                "Event type must be ACCIDENT or SLOWDOWN.");
        }
        if (approach == null)
        {
            throw new IllegalArgumentException(
                "Affected approach cannot be null.");
        }
        if (startSecond < 0)
        {
            throw new IllegalArgumentException(
                "Event start time cannot be negative.");
        }
        if (durationSeconds < SimulationOptions.MIN_EVENT_DURATION_SECONDS
            || durationSeconds > SimulationOptions.MAX_EVENT_DURATION_SECONDS)
        {
            throw new IllegalArgumentException(
                "Event duration must be between "
                    + SimulationOptions.MIN_EVENT_DURATION_SECONDS + " and "
                    + SimulationOptions.MAX_EVENT_DURATION_SECONDS
                    + " seconds.");
        }

        long calculatedEnd = (long)startSecond + durationSeconds;
        if (calculatedEnd > Integer.MAX_VALUE)
        {
            throw new IllegalArgumentException(
                "Event end time exceeds the supported simulation time range.");
        }

        this.type = type;
        this.approach = approach;
        this.startSecond = startSecond;
        this.durationSeconds = durationSeconds;
        this.endSecond = (int)calculatedEnd;
    }


    /**
     * Checks whether this incident is active at the given simulated second. The
     * start is included and the end is excluded.
     *
     * @param second
     *            nonnegative simulation second to query
     * @return true when startSecond <= second < endSecond
     * @throws IllegalArgumentException
     *             if second is negative
     */
    public boolean isActiveAt(int second)
    {
        if (second < 0)
        {
            throw new IllegalArgumentException(
                "Simulation time cannot be negative.");
        }
        return second >= startSecond && second < endSecond;
    }


    /**
     * Returns the incident classification.
     *
     * @return ACCIDENT or SLOWDOWN
     */
    public TrafficEventType getType()
    {
        return type;
    }


    /**
     * Returns the affected incoming approach.
     *
     * @return nonnull direction
     */
    public Direction getApproach()
    {
        return approach;
    }


    /**
     * Returns when the event begins.
     *
     * @return inclusive simulated start second
     */
    public int getStartSecond()
    {
        return startSecond;
    }


    /**
     * Returns how long the event lasts.
     *
     * @return duration in simulated seconds
     */
    public int getDurationSeconds()
    {
        return durationSeconds;
    }


    /**
     * Returns when the event has expired.
     *
     * @return exclusive simulated end second
     */
    public int getEndSecond()
    {
        return endSecond;
    }
}
