
package defaultPackage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Generates and manages random traffic incidents for four incoming approaches.
 * At each configured check interval after second zero, every unblocked approach
 * receives one independent accident/slowdown/no-event draw. Events remain
 * active over [startSecond, endSecond), and at most one event can be active on
 * an approach at a time. This class manages event state only. SimulationModel
 * applies blocking, movement speed, and departure headway effects.
 */
public final class TrafficEventManager
{

    private static final int PERCENT_SCALE = 100;

    private final SimulationOptions options;
    private final Random eventRandom;
    private final EnumMap<Direction, TrafficEvent> activeEvents;

    private int lastUpdatedSecond;
    private List<TrafficEvent> endedLastUpdate;

    /**
     * Creates an incident manager with a dedicated random source.
     * SimulationModel should pass a generator seeded independently from
     * generators for turns and emergency vehicle classification.
     *
     * @param options
     *            validated settings for incident checks and durations
     * @param eventRandom
     *            random source used only for incident selection
     * @throws IllegalArgumentException
     *             if either argument is null
     */
    public TrafficEventManager(SimulationOptions options, Random eventRandom)
    {
        if (options == null || eventRandom == null)
        {
            throw new IllegalArgumentException(
                "Incident options and random source cannot be null.");
        }
        this.options = options;
        this.eventRandom = eventRandom;
        this.activeEvents = new EnumMap<>(Direction.class);
        this.lastUpdatedSecond = -1;
        this.endedLastUpdate = Collections.emptyList();
    }


    /**
     * Processes exactly the next simulated second (0, 1, 2, ...). First expires
     * incidents whose end time has been reached. At positive multiples of the
     * configured interval, each approach without an active incident receives
     * one draw: accident, slowdown, or no event. An incident ending on a check
     * boundary is eligible for a new event.
     *
     * @param second
     *            next zero-based simulation second
     * @return unmodifiable list of incidents started during this update, in
     *             Direction.values() order
     * @throws IllegalArgumentException
     *             if second is negative, repeated, or skips a simulation second
     */
    public List<TrafficEvent> updateForSecond(int second)
    {
        if (second < 0 || (long)second != (long)lastUpdatedSecond + 1L)
        {
            throw new IllegalArgumentException(
                "Incident updates must start at zero and be consecutive.");
        }

        // Stage changes first, so invalid step calls never alter state.
        EnumMap<Direction, TrafficEvent> next = new EnumMap<>(activeEvents);
        List<TrafficEvent> ended = new ArrayList<>();
        List<TrafficEvent> started = new ArrayList<>();

        for (Direction direction : Direction.values())
        {
            TrafficEvent current = next.get(direction);
            if (current != null && !current.isActiveAt(second))
            {
                next.remove(direction);
                ended.add(current);
            }
        }

        int accidentPercent = options.getAccidentChancePercent();
        int slowdownPercent = options.getSlowdownChancePercent();
        boolean checkDue =
            second > 0 && second % options.getEventCheckIntervalSeconds() == 0;

        if (checkDue && accidentPercent + slowdownPercent > 0)
        {
            for (Direction direction : Direction.values())
            {
                if (next.containsKey(direction))
                {
                    continue;
                }
                int draw = eventRandom.nextInt(PERCENT_SCALE);
                TrafficEventType type = TrafficEventType.NONE;
                if (draw < accidentPercent)
                {
                    type = TrafficEventType.ACCIDENT;
                }
                else if (draw < accidentPercent + slowdownPercent)
                {
                    type = TrafficEventType.SLOWDOWN;
                }

                if (type != TrafficEventType.NONE)
                {
                    int duration = type == TrafficEventType.ACCIDENT
                        ? options.getAccidentDurationSeconds()
                        : options.getSlowdownDurationSeconds();
                    TrafficEvent event =
                        new TrafficEvent(type, direction, second, duration);
                    next.put(direction, event);
                    started.add(event);
                }
            }
        }

        activeEvents.clear();
        activeEvents.putAll(next);
        lastUpdatedSecond = second;
        endedLastUpdate = Collections.unmodifiableList(ended);
        return Collections.unmodifiableList(started);
    }


    /**
     * Returns the incident active on an approach at the last processed second.
     * This should be queried after updateForSecond() for the current step.
     *
     * @param direction
     *            approach to inspect
     * @return active incident, or null when the approach is unaffected
     * @throws IllegalArgumentException
     *             if direction is null
     */
    public TrafficEvent getActiveEvent(Direction direction)
    {
        requireDirection(direction);
        return activeEvents.get(direction);
    }


    /**
     * Reports whether an accident blocks an approach at the last update.
     *
     * @param direction
     *            approach to inspect
     * @return true if the approach currently has an active accident
     * @throws IllegalArgumentException
     *             if direction is null
     */
    public boolean isAccidentActive(Direction direction)
    {
        TrafficEvent event = getActiveEvent(direction);
        return event != null && event.getType() == TrafficEventType.ACCIDENT;
    }


    /**
     * Reports whether a slowdown affects an approach at the last update.
     *
     * @param direction
     *            approach to inspect
     * @return true if the approach currently has an active slowdown
     * @throws IllegalArgumentException
     *             if direction is null
     */
    public boolean isSlowdownActive(Direction direction)
    {
        TrafficEvent event = getActiveEvent(direction);
        return event != null && event.getType() == TrafficEventType.SLOWDOWN;
    }


    /**
     * Returns a read-only snapshot of currently tracked incidents. Only
     * affected approaches appear in the map.
     *
     * @return unmodifiable defensive copy of active events by approach
     */
    public Map<Direction, TrafficEvent> getActiveEvents()
    {
        EnumMap<Direction, TrafficEvent> copy = new EnumMap<>(Direction.class);
        copy.putAll(activeEvents);
        return Collections.unmodifiableMap(copy);
    }


    /**
     * Filters currently known events to those active at a specified time. This
     * does not generate or predict future incidents. It is useful when
     * displaying a snapshot at the boundary after the most recent step.
     *
     * @param second
     *            nonnegative simulation time to query
     * @return unmodifiable map of tracked incidents active at that time
     * @throws IllegalArgumentException
     *             if second is negative
     */
    public Map<Direction, TrafficEvent> getActiveEventsAt(int second)
    {
        if (second < 0)
        {
            throw new IllegalArgumentException("Time cannot be negative.");
        }
        EnumMap<Direction, TrafficEvent> copy = new EnumMap<>(Direction.class);
        for (Map.Entry<Direction, TrafficEvent> entry : activeEvents.entrySet())
        {
            if (entry.getValue().isActiveAt(second))
            {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(copy);
    }


    /**
     * Returns incidents that expired during the most recent update. The list is
     * cleared on the next successful update and never includes incidents that
     * are still active.
     *
     * @return unmodifiable list of most recently expired incidents
     */
    public List<TrafficEvent> getEventsEndedLastUpdate()
    {
        return endedLastUpdate;
    }


    /**
     * Returns the latest simulated second successfully processed.
     *
     * @return last update time, or -1 before the first update
     */
    public int getLastUpdatedSecond()
    {
        return lastUpdatedSecond;
    }


    /**
     * Validates an approach before performing an incident lookup.
     *
     * @param direction
     *            approach to validate
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
