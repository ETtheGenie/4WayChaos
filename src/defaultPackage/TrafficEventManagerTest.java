
package defaultPackage;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Test;

/**
 * JUnit 4 tests for TrafficEventManager scheduling, incident selection,
 * expiration, reproducibility, snapshots, and bad-input behavior.
 */
public class TrafficEventManagerTest
{

    /**
     * Supplies exact random draws so probability boundaries can be tested.
     */
    private static final class ScriptedRandom
        extends Random
    {
        private static final long serialVersionUID = 1L;
        private final int[] draws;
        private int index;

        /**
         * Creates a deterministic sequence of values in [0, 99].
         *
         * @param draws
         *            ordered values for subsequent random draws
         */
        ScriptedRandom(int... draws)
        {
            this.draws = draws.clone();
            this.index = 0;
        }


        /**
         * Returns the next scripted draw and fails on unexpected consumption.
         *
         * @param bound
         *            requested exclusive upper limit
         * @return next scripted draw
         */
        @Override
        public int nextInt(int bound)
        {
            if (index >= draws.length)
            {
                throw new AssertionError("Unexpected random-number draw.");
            }
            int result = draws[index++];
            if (result < 0 || result >= bound)
            {
                throw new AssertionError("Invalid scripted random draw.");
            }
            return result;
        }
    }

    /**
     * Creates valid options with a ten-second interval and chosen chances.
     *
     * @param accident
     *            accident chance percentage
     * @param slowdown
     *            slowdown chance percentage
     * @param accidentDuration
     *            accident lifetime in seconds
     * @param slowdownDuration
     *            slowdown lifetime in seconds
     * @return validated simulation options
     */
    private SimulationOptions options(
        int accident,
        int slowdown,
        int accidentDuration,
        int slowdownDuration)
    {
        return SimulationOptions.builder().eventCheckIntervalSeconds(10)
            .accidentChancePercent(accident).slowdownChancePercent(slowdown)
            .accidentDurationSeconds(accidentDuration)
            .slowdownDurationSeconds(slowdownDuration).build();
    }


    /**
     * Creates a manager with a fixed random seed for reproducible testing.
     *
     * @param options
     *            validated incident settings
     * @return manager using seed 12345
     */
    private TrafficEventManager manager(SimulationOptions options)
    {
        return new TrafficEventManager(options, new Random(12345L));
    }


    /**
     * Processes all times from zero through a specified second.
     *
     * @param manager
     *            manager to advance
     * @param lastSecond
     *            last second, inclusive
     */
    private void advanceThrough(TrafficEventManager manager, int lastSecond)
    {
        for (int second = 0; second <= lastSecond; second++)
        {
            manager.updateForSecond(second);
        }
    }


    /**
     * Advances a previously started manager through a later time.
     *
     * @param manager
     *            manager to advance
     * @param firstSecond
     *            first unprocessed second
     * @param lastSecond
     *            last second, inclusive
     */
    private void advanceRange(
        TrafficEventManager manager,
        int firstSecond,
        int lastSecond)
    {
        for (int second = firstSecond; second <= lastSecond; second++)
        {
            manager.updateForSecond(second);
        }
    }


    /**
     * Verifies that an action throws IllegalArgumentException.
     *
     * @param operation
     *            operation expected to reject invalid input
     */
    private void assertInvalid(Runnable operation)
    {
        try
        {
            operation.run();
            fail("Expected IllegalArgumentException.");
        }
        catch (IllegalArgumentException expected)
        {
            // Expected invalid-input rejection.
        }
    }


    /**
     * Tests that a new manager has no incidents and starts before second zero.
     */
    @Test
    public void testInitialState()
    {
        TrafficEventManager events = manager(options(20, 30, 5, 5));
        assertEquals(-1, events.getLastUpdatedSecond());
        assertTrue(events.getActiveEvents().isEmpty());
        assertTrue(events.getEventsEndedLastUpdate().isEmpty());
        assertNull(events.getActiveEvent(Direction.NORTH));
        assertFalse(events.isAccidentActive(Direction.EAST));
        assertFalse(events.isSlowdownActive(Direction.WEST));
        assertTrue(events.getActiveEventsAt(0).isEmpty());
    }


    /**
     * Tests rejection of a missing options object or random generator.
     */
    @Test
    public void testConstructorRejectsNull()
    {
        assertInvalid(() -> new TrafficEventManager(null, new Random(1)));
        assertInvalid(
            () -> new TrafficEventManager(options(20, 30, 5, 5), null));
    }


    /**
     * Tests that time zero never starts incidents, even at 100% probability.
     */
    @Test
    public void testNoIncidentsAtTimeZero()
    {
        TrafficEventManager events = new TrafficEventManager(
            options(100, 0, 5, 5),
            new ScriptedRandom());
        assertTrue(events.updateForSecond(0).isEmpty());
        assertTrue(events.getActiveEvents().isEmpty());
        assertEquals(0, events.getLastUpdatedSecond());
    }


    /**
     * Tests that no event checks happen before the first configured interval.
     */
    @Test
    public void testNoDrawsBeforeCheckInterval()
    {
        TrafficEventManager events = new TrafficEventManager(
            options(100, 0, 5, 5),
            new ScriptedRandom());
        advanceThrough(events, 9);
        assertTrue(events.getActiveEvents().isEmpty());
        assertEquals(9, events.getLastUpdatedSecond());
    }


    /**
     * Tests that negative, repeated, and skipped updates are rejected without
     * moving the last processed time or affecting active incident state.
     */
    @Test
    public void testUpdateOrderValidation()
    {
        TrafficEventManager events = manager(options(100, 0, 5, 5));
        assertInvalid(() -> events.updateForSecond(-1));
        assertInvalid(() -> events.updateForSecond(1));
        assertEquals(-1, events.getLastUpdatedSecond());
        events.updateForSecond(0);
        assertInvalid(() -> events.updateForSecond(0));
        assertInvalid(() -> events.updateForSecond(2));
        assertEquals(0, events.getLastUpdatedSecond());
        assertTrue(events.getActiveEvents().isEmpty());
        events.updateForSecond(1);
        assertEquals(1, events.getLastUpdatedSecond());
    }


    /**
     * Tests that an incident check at t=10 creates four accidents at 100%.
     */
    @Test
    public void testCertainAccidentsOnAllApproaches()
    {
        TrafficEventManager events = manager(options(100, 0, 15, 5));
        advanceThrough(events, 9);
        List<TrafficEvent> started = events.updateForSecond(10);
        assertEquals(4, started.size());
        assertEquals(4, events.getActiveEvents().size());
        for (int i = 0; i < Direction.values().length; i++)
        {
            Direction direction = Direction.values()[i];
            TrafficEvent event = started.get(i);
            assertEquals(direction, event.getApproach());
            assertEquals(TrafficEventType.ACCIDENT, event.getType());
            assertEquals(10, event.getStartSecond());
            assertEquals(15, event.getDurationSeconds());
            assertEquals(25, event.getEndSecond());
            assertTrue(events.isAccidentActive(direction));
            assertFalse(events.isSlowdownActive(direction));
            assertTrue(event == events.getActiveEvent(direction));
        }
    }


    /**
     * Tests that a 100% slowdown configuration creates four slowdowns.
     */
    @Test
    public void testCertainSlowdownsOnAllApproaches()
    {
        TrafficEventManager events = manager(options(0, 100, 5, 20));
        advanceThrough(events, 10);
        assertEquals(4, events.getActiveEvents().size());
        for (Direction direction : Direction.values())
        {
            TrafficEvent event = events.getActiveEvent(direction);
            assertEquals(TrafficEventType.SLOWDOWN, event.getType());
            assertEquals(30, event.getEndSecond());
            assertFalse(events.isAccidentActive(direction));
            assertTrue(events.isSlowdownActive(direction));
        }
    }


    /**
     * Tests that zero probability disables both event types without drawing.
     */
    @Test
    public void testZeroProbabilitiesNeverDraw()
    {
        TrafficEventManager events =
            new TrafficEventManager(options(0, 0, 5, 5), new ScriptedRandom());
        advanceThrough(events, 50);
        assertTrue(events.getActiveEvents().isEmpty());
        assertTrue(events.getEventsEndedLastUpdate().isEmpty());
    }


    /**
     * Tests exact percent boundaries for accident and slowdown selection.
     */
    @Test
    public void testScriptedProbabilityBoundaries()
    {
        TrafficEventManager events = new TrafficEventManager(
            options(25, 25, 5, 5),
            new ScriptedRandom(0, 24, 25, 49));
        advanceThrough(events, 10);
        assertEquals(
            TrafficEventType.ACCIDENT,
            events.getActiveEvent(Direction.NORTH).getType());
        assertEquals(
            TrafficEventType.ACCIDENT,
            events.getActiveEvent(Direction.SOUTH).getType());
        assertEquals(
            TrafficEventType.SLOWDOWN,
            events.getActiveEvent(Direction.EAST).getType());
        assertEquals(
            TrafficEventType.SLOWDOWN,
            events.getActiveEvent(Direction.WEST).getType());
    }


    /**
     * Tests that draws at or above the combined probability start no incident.
     */
    @Test
    public void testNoEventProbabilityBranch()
    {
        TrafficEventManager events = new TrafficEventManager(
            options(25, 25, 5, 5),
            new ScriptedRandom(50, 75, 99, 80));
        advanceThrough(events, 10);
        assertTrue(events.getActiveEvents().isEmpty());
    }


    /**
     * Tests that no new incident is created when an approach is already
     * affected, even on another incident-check boundary.
     */
    @Test
    public void testNoOverlappingIncidentsOrExtraDraws()
    {
        TrafficEventManager events = new TrafficEventManager(
            options(100, 0, 25, 5),
            new ScriptedRandom(0, 0, 0, 0, 0, 0, 0, 0));
        advanceThrough(events, 10);
        Map<Direction, TrafficEvent> originals = events.getActiveEvents();
        advanceRange(events, 11, 20);
        for (Direction direction : Direction.values())
        {
            assertTrue(
                originals.get(direction) == events.getActiveEvent(direction));
        }
        assertTrue(events.getEventsEndedLastUpdate().isEmpty());
        advanceRange(events, 21, 30);
        assertEquals(4, events.getActiveEvents().size());
        for (Direction direction : Direction.values())
        {
            assertTrue(
                originals.get(direction) == events.getActiveEvent(direction));
        }
    }


    /**
     * Tests expiration exactly at the half-open end time and its ending log.
     */
    @Test
    public void testExpirationAtExactEndSecond()
    {
        TrafficEventManager events = manager(options(100, 0, 5, 5));
        advanceThrough(events, 10);
        advanceRange(events, 11, 14);
        assertEquals(4, events.getActiveEvents().size());
        assertTrue(events.getEventsEndedLastUpdate().isEmpty());
        assertTrue(events.updateForSecond(15).isEmpty());
        assertTrue(events.getActiveEvents().isEmpty());
        assertEquals(4, events.getEventsEndedLastUpdate().size());
        for (TrafficEvent ended : events.getEventsEndedLastUpdate())
        {
            assertEquals(15, ended.getEndSecond());
        }
        events.updateForSecond(16);
        assertTrue(events.getEventsEndedLastUpdate().isEmpty());
    }


    /**
     * Tests that an expired approach can receive a new event at a later check.
     */
    @Test
    public void testNewIncidentAfterExpiration()
    {
        TrafficEventManager events = manager(options(100, 0, 5, 5));
        advanceThrough(events, 19);
        assertTrue(events.getActiveEvents().isEmpty());
        assertEquals(4, events.updateForSecond(20).size());
        for (Direction direction : Direction.values())
        {
            assertEquals(20, events.getActiveEvent(direction).getStartSecond());
        }
    }


    /**
     * Tests expiration and replacement in the same update at a check boundary.
     */
    @Test
    public void testReplacementOnExactCheckBoundary()
    {
        TrafficEventManager events = manager(options(100, 0, 10, 5));
        advanceThrough(events, 19);
        Map<Direction, TrafficEvent> old = events.getActiveEvents();
        assertEquals(4, events.updateForSecond(20).size());
        assertEquals(4, events.getEventsEndedLastUpdate().size());
        for (Direction direction : Direction.values())
        {
            assertFalse(old.get(direction) == events.getActiveEvent(direction));
            assertEquals(20, events.getActiveEvent(direction).getStartSecond());
        }
    }


    /**
     * Tests that ordinary seconds never trigger additional event draws.
     */
    @Test
    public void testNonCheckStepReturnsEmpty()
    {
        TrafficEventManager events = manager(options(100, 0, 20, 5));
        advanceThrough(events, 9);
        assertEquals(4, events.updateForSecond(10).size());
        assertTrue(events.updateForSecond(11).isEmpty());
        assertEquals(4, events.getActiveEvents().size());
    }


    /**
     * Tests that active-event maps are unmodifiable and defensively copied.
     */
    @Test
    public void testActiveEventMapImmutableSnapshot()
    {
        TrafficEventManager events = manager(options(100, 0, 5, 5));
        advanceThrough(events, 10);
        Map<Direction, TrafficEvent> snapshot = events.getActiveEvents();
        try
        {
            snapshot.clear();
            fail("Expected unmodifiable event map.");
        }
        catch (UnsupportedOperationException expected)
        {
            // Expected defensive map protection.
        }
        advanceRange(events, 11, 15);
        assertEquals(4, snapshot.size());
        assertTrue(events.getActiveEvents().isEmpty());
    }


    /**
     * Tests that returned lists cannot be edited and survive later updates.
     */
    @Test
    public void testStartedListUnmodifiableAndStable()
    {
        TrafficEventManager events = manager(options(100, 0, 5, 5));
        advanceThrough(events, 9);
        List<TrafficEvent> started = events.updateForSecond(10);
        try
        {
            started.clear();
            fail("Expected unmodifiable started list.");
        }
        catch (UnsupportedOperationException expected)
        {
            // Expected immutable list.
        }
        events.updateForSecond(11);
        assertEquals(4, started.size());
    }


    /**
     * Tests that the returned ended-event list is unmodifiable and stable.
     */
    @Test
    public void testEndedListUnmodifiableAndStable()
    {
        TrafficEventManager events = manager(options(100, 0, 5, 5));
        advanceThrough(events, 15);
        List<TrafficEvent> ended = events.getEventsEndedLastUpdate();
        assertEquals(4, ended.size());
        try
        {
            ended.remove(0);
            fail("Expected unmodifiable ended list.");
        }
        catch (UnsupportedOperationException expected)
        {
            // Expected immutable list.
        }
        events.updateForSecond(16);
        assertEquals(4, ended.size());
        assertTrue(events.getEventsEndedLastUpdate().isEmpty());
    }


    /**
     * Tests that incident lookups reject null approach directions.
     */
    @Test
    public void testNullDirectionQueriesRejected()
    {
        TrafficEventManager events = manager(options(100, 0, 5, 5));
        assertInvalid(() -> events.getActiveEvent(null));
        assertInvalid(() -> events.isAccidentActive(null));
        assertInvalid(() -> events.isSlowdownActive(null));
    }


    /**
     * Tests filtering tracked events at a boundary without advancing time.
     */
    @Test
    public void testActiveEventsAtBoundary()
    {
        TrafficEventManager events = manager(options(100, 0, 5, 5));
        advanceThrough(events, 10);
        assertEquals(4, events.getActiveEventsAt(10).size());
        assertEquals(4, events.getActiveEventsAt(14).size());
        assertTrue(events.getActiveEventsAt(15).isEmpty());
        assertTrue(events.getActiveEventsAt(9).isEmpty());
        assertEquals(4, events.getActiveEvents().size());
        assertInvalid(() -> events.getActiveEventsAt(-1));
    }


    /**
     * Tests read-only event map filtering cannot alter tracked state.
     */
    @Test
    public void testFilteredEventMapUnmodifiable()
    {
        TrafficEventManager events = manager(options(100, 0, 5, 5));
        advanceThrough(events, 10);
        Map<Direction, TrafficEvent> filtered = events.getActiveEventsAt(14);
        try
        {
            filtered.remove(Direction.NORTH);
            fail("Expected unmodifiable filtered map.");
        }
        catch (UnsupportedOperationException expected)
        {
            // Expected read-only output.
        }
        assertEquals(4, events.getActiveEvents().size());
    }


    /**
     * Tests that identical event configurations and seeds reproduce history.
     */
    @Test
    public void testReproducibleWithEqualSeeds()
    {
        SimulationOptions settings = options(25, 35, 5, 7);
        TrafficEventManager a =
            new TrafficEventManager(settings, new Random(987654321L));
        TrafficEventManager b =
            new TrafficEventManager(settings, new Random(987654321L));
        for (int second = 0; second <= 80; second++)
        {
            List<TrafficEvent> aStarted = a.updateForSecond(second);
            List<TrafficEvent> bStarted = b.updateForSecond(second);
            assertEquals(aStarted.size(), bStarted.size());
            for (int i = 0; i < aStarted.size(); i++)
            {
                TrafficEvent x = aStarted.get(i);
                TrafficEvent y = bStarted.get(i);
                assertEquals(x.getApproach(), y.getApproach());
                assertEquals(x.getType(), y.getType());
                assertEquals(x.getStartSecond(), y.getStartSecond());
                assertEquals(x.getEndSecond(), y.getEndSecond());
            }
            assertEquals(
                a.getActiveEvents().keySet(),
                b.getActiveEvents().keySet());
        }
    }


    /**
     * Tests that the manager records the correct most recent update time.
     */
    @Test
    public void testLastUpdatedSecondChangesOnlyAfterSuccess()
    {
        TrafficEventManager events = manager(options(0, 0, 5, 5));
        assertEquals(-1, events.getLastUpdatedSecond());
        events.updateForSecond(0);
        assertEquals(0, events.getLastUpdatedSecond());
        assertInvalid(() -> events.updateForSecond(10));
        assertEquals(0, events.getLastUpdatedSecond());
        events.updateForSecond(1);
        assertEquals(1, events.getLastUpdatedSecond());
    }
}
