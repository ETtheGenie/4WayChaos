
package defaultPackage;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * JUnit 4 tests for immutable TrafficEvent records, time boundaries,
 * validation, and overflow-safe duration handling.
 */
public class TrafficEventTest
{

    /**
     * Checks that invalid input throws the expected exception.
     *
     * @param action
     *            operation expected to reject its arguments
     */
    private void assertInvalid(Runnable action)
    {
        try
        {
            action.run();
            fail("Expected IllegalArgumentException.");
        }
        catch (IllegalArgumentException expected)
        {
            // Expected invalid-input rejection.
        }
    }


    /**
     * Tests that an accident retains every constructor parameter.
     */
    @Test
    public void testAccidentConstructionAndGetters()
    {
        TrafficEvent event = new TrafficEvent(
            TrafficEventType.ACCIDENT,
            Direction.NORTH,
            60,
            45);

        assertEquals(TrafficEventType.ACCIDENT, event.getType());
        assertEquals(Direction.NORTH, event.getApproach());
        assertEquals(60, event.getStartSecond());
        assertEquals(45, event.getDurationSeconds());
        assertEquals(105, event.getEndSecond());
    }


    /**
     * Tests that a slowdown stores the correct classification and interval.
     */
    @Test
    public void testSlowdownConstructionAndGetters()
    {
        TrafficEvent event =
            new TrafficEvent(TrafficEventType.SLOWDOWN, Direction.EAST, 30, 60);

        assertEquals(TrafficEventType.SLOWDOWN, event.getType());
        assertEquals(Direction.EAST, event.getApproach());
        assertEquals(30, event.getStartSecond());
        assertEquals(60, event.getDurationSeconds());
        assertEquals(90, event.getEndSecond());
    }


    /**
     * Tests that all four approaches accept both real incident types.
     */
    @Test
    public void testEveryDirectionAndRealEventType()
    {
        for (Direction direction : Direction.values())
        {
            for (TrafficEventType type : new TrafficEventType[] {
                TrafficEventType.ACCIDENT, TrafficEventType.SLOWDOWN })
            {
                TrafficEvent event = new TrafficEvent(type, direction, 0, 5);
                assertEquals(type, event.getType());
                assertEquals(direction, event.getApproach());
            }
        }
    }


    /**
     * Tests that an event starts at simulation time zero.
     */
    @Test
    public void testZeroStartTime()
    {
        TrafficEvent event =
            new TrafficEvent(TrafficEventType.ACCIDENT, Direction.WEST, 0, 5);
        assertTrue(event.isActiveAt(0));
        assertFalse(event.isActiveAt(5));
    }


    /**
     * Tests that the start time is inclusive and the end time exclusive.
     */
    @Test
    public void testActiveIntervalBoundaries()
    {
        TrafficEvent event = new TrafficEvent(
            TrafficEventType.ACCIDENT,
            Direction.SOUTH,
            60,
            45);
        assertFalse(event.isActiveAt(59));
        assertTrue(event.isActiveAt(60));
        assertTrue(event.isActiveAt(104));
        assertFalse(event.isActiveAt(105));
    }


    /**
     * Tests intermediate times, far-before times, and far-after times.
     */
    @Test
    public void testActiveAtIntermediateAndOutsideTimes()
    {
        TrafficEvent event =
            new TrafficEvent(TrafficEventType.SLOWDOWN, Direction.WEST, 50, 20);
        assertFalse(event.isActiveAt(0));
        assertTrue(event.isActiveAt(55));
        assertTrue(event.isActiveAt(69));
        assertFalse(event.isActiveAt(70));
        assertFalse(event.isActiveAt(500));
    }


    /**
     * Tests the inclusive minimum permitted event duration.
     */
    @Test
    public void testMinimumDuration()
    {
        TrafficEvent event = new TrafficEvent(
            TrafficEventType.ACCIDENT,
            Direction.NORTH,
            10,
            SimulationOptions.MIN_EVENT_DURATION_SECONDS);
        assertEquals(15, event.getEndSecond());
        assertTrue(event.isActiveAt(14));
        assertFalse(event.isActiveAt(15));
    }


    /**
     * Tests the inclusive maximum permitted event duration.
     */
    @Test
    public void testMaximumDuration()
    {
        TrafficEvent event = new TrafficEvent(
            TrafficEventType.SLOWDOWN,
            Direction.NORTH,
            10,
            SimulationOptions.MAX_EVENT_DURATION_SECONDS);
        assertEquals(310, event.getEndSecond());
        assertTrue(event.isActiveAt(309));
        assertFalse(event.isActiveAt(310));
    }


    /**
     * Tests that NONE cannot be instantiated as an actual event.
     */
    @Test
    public void testNoneTypeRejected()
    {
        assertInvalid(
            () -> new TrafficEvent(
                TrafficEventType.NONE,
                Direction.NORTH,
                0,
                10));
    }


    /**
     * Tests that a null incident classification is rejected.
     */
    @Test
    public void testNullTypeRejected()
    {
        assertInvalid(() -> new TrafficEvent(null, Direction.NORTH, 0, 10));
    }


    /**
     * Tests that a null affected approach is rejected.
     */
    @Test
    public void testNullApproachRejected()
    {
        assertInvalid(
            () -> new TrafficEvent(TrafficEventType.ACCIDENT, null, 0, 10));
    }


    /**
     * Tests that negative start times are rejected.
     */
    @Test
    public void testNegativeStartRejected()
    {
        assertInvalid(
            () -> new TrafficEvent(
                TrafficEventType.SLOWDOWN,
                Direction.EAST,
                -1,
                10));
    }


    /**
     * Tests that event durations below the minimum are rejected.
     */
    @Test
    public void testTooShortDurationRejected()
    {
        assertInvalid(
            () -> new TrafficEvent(
                TrafficEventType.ACCIDENT,
                Direction.NORTH,
                0,
                0));
        assertInvalid(
            () -> new TrafficEvent(
                TrafficEventType.ACCIDENT,
                Direction.NORTH,
                0,
                4));
        assertInvalid(
            () -> new TrafficEvent(
                TrafficEventType.ACCIDENT,
                Direction.NORTH,
                0,
                -1));
    }


    /**
     * Tests that event durations above the maximum are rejected.
     */
    @Test
    public void testTooLongDurationRejected()
    {
        assertInvalid(
            () -> new TrafficEvent(
                TrafficEventType.SLOWDOWN,
                Direction.WEST,
                0,
                301));
        assertInvalid(
            () -> new TrafficEvent(
                TrafficEventType.SLOWDOWN,
                Direction.WEST,
                0,
                Integer.MAX_VALUE));
    }


    /**
     * Tests that a negative query time is rejected.
     */
    @Test
    public void testNegativeQueryTimeRejected()
    {
        TrafficEvent event =
            new TrafficEvent(TrafficEventType.ACCIDENT, Direction.SOUTH, 5, 10);
        assertInvalid(() -> event.isActiveAt(-1));
    }


    /**
     * Tests that event end time overflow is rejected before state is stored.
     */
    @Test
    public void testEndTimeOverflowRejected()
    {
        assertInvalid(
            () -> new TrafficEvent(
                TrafficEventType.ACCIDENT,
                Direction.NORTH,
                Integer.MAX_VALUE - 4,
                5));
        assertInvalid(
            () -> new TrafficEvent(
                TrafficEventType.SLOWDOWN,
                Direction.SOUTH,
                Integer.MAX_VALUE,
                300));
    }


    /**
     * Tests that an event may expire exactly at Integer.MAX_VALUE.
     */
    @Test
    public void testMaximumRepresentableEndAccepted()
    {
        TrafficEvent event = new TrafficEvent(
            TrafficEventType.ACCIDENT,
            Direction.EAST,
            Integer.MAX_VALUE - 5,
            5);
        assertEquals(Integer.MAX_VALUE, event.getEndSecond());
        assertTrue(event.isActiveAt(Integer.MAX_VALUE - 1));
        assertFalse(event.isActiveAt(Integer.MAX_VALUE));
    }


    /**
     * Tests that independent events do not share timing or identity state.
     */
    @Test
    public void testIndependentEvents()
    {
        TrafficEvent accident = new TrafficEvent(
            TrafficEventType.ACCIDENT,
            Direction.NORTH,
            30,
            10);
        TrafficEvent slowdown = new TrafficEvent(
            TrafficEventType.SLOWDOWN,
            Direction.SOUTH,
            35,
            20);
        assertFalse(accident.isActiveAt(41));
        assertTrue(slowdown.isActiveAt(41));
        assertEquals(40, accident.getEndSecond());
        assertEquals(55, slowdown.getEndSecond());
    }
}
