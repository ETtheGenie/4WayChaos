
package defaultPackage;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * JUnit 4 tests for immutable V2 snapshots, adaptive signal state, emergency
 * queues, active incidents, vehicle conservation, and invalid input.
 */
public class SimulationSnapshotTest
{

    /**
     * Constructs a mutable complete queue map for test scenarios.
     *
     * @param north
     *            north waiting count
     * @param south
     *            south waiting count
     * @param east
     *            east waiting count
     * @param west
     *            west waiting count
     * @return populated map keyed by direction
     */
    private
        Map<Direction, Integer>
        counts(int north, int south, int east, int west)
    {
        EnumMap<Direction, Integer> map = new EnumMap<>(Direction.class);
        map.put(Direction.NORTH, north);
        map.put(Direction.SOUTH, south);
        map.put(Direction.EAST, east);
        map.put(Direction.WEST, west);
        return map;
    }


    /**
     * Builds an empty incident map.
     *
     * @return mutable map containing no incidents
     */
    private Map<Direction, TrafficEvent> noEvents()
    {
        return new EnumMap<>(Direction.class);
    }


    /**
     * Creates a fully valid snapshot with ten waiting vehicles, three
     * emergencies waiting, two crossing vehicles, and five completed vehicles.
     * At t=70 an accident is active on NORTH and a slowdown on EAST.
     *
     * @return valid populated snapshot
     */
    private SimulationSnapshot populated()
    {
        Map<Direction, TrafficEvent> events = noEvents();
        events.put(
            Direction.NORTH,
            new TrafficEvent(
                TrafficEventType.ACCIDENT,
                Direction.NORTH,
                60,
                45));
        events.put(
            Direction.EAST,
            new TrafficEvent(
                TrafficEventType.SLOWDOWN,
                Direction.EAST,
                65,
                60));
        return snapshot(
            70,
            SignalPhase.EW_GREEN,
            60,
            3,
            counts(4, 3, 2, 1),
            counts(2, 1, 0, 0),
            events,
            17,
            5,
            2);
    }


    /**
     * Creates a snapshot directly from specified input values.
     *
     * @param elapsed
     *            completed steps
     * @param phase
     *            signal phase
     * @param phaseStart
     *            time current phase began
     * @param switches
     *            number of phase switches
     * @param queues
     *            all waiting queues
     * @param emergencyQueues
     *            emergency waiting subsets
     * @param events
     *            map of current incidents
     * @param arrived
     *            total generated
     * @param completed
     *            total completed
     * @param crossing
     *            currently crossing
     * @return immutable snapshot
     */
    private SimulationSnapshot snapshot(
        int elapsed,
        SignalPhase phase,
        int phaseStart,
        int switches,
        Map<Direction, Integer> queues,
        Map<Direction, Integer> emergencyQueues,
        Map<Direction, TrafficEvent> events,
        int arrived,
        int completed,
        int crossing)
    {
        return new SimulationSnapshot(
            elapsed,
            phase,
            phaseStart,
            switches,
            queues,
            emergencyQueues,
            events,
            arrived,
            completed,
            crossing);
    }


    /**
     * Creates a valid empty snapshot at the beginning of a simulation.
     *
     * @return empty initial snapshot
     */
    private SimulationSnapshot initial()
    {
        return snapshot(
            0,
            SignalPhase.NS_GREEN,
            0,
            0,
            counts(0, 0, 0, 0),
            counts(0, 0, 0, 0),
            noEvents(),
            0,
            0,
            0);
    }


    /**
     * Verifies that the action rejects an invalid input.
     *
     * @param action
     *            operation expected to throw IllegalArgumentException
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
            // Expected invalid input.
        }
    }


    /**
     * Verifies all fields and getters of a zero-traffic initial snapshot.
     */
    @Test
    public void testInitialSnapshot()
    {
        SimulationSnapshot value = initial();
        assertEquals(0, value.getElapsedSeconds());
        assertEquals(SignalPhase.NS_GREEN, value.getSignalPhase());
        assertEquals(0, value.getPhaseStartSecond());
        assertEquals(0, value.getPhaseElapsedSeconds());
        assertEquals(0, value.getPhaseSwitchCount());
        assertEquals(0, value.getTotalArrived());
        assertEquals(0, value.getTotalCompleted());
        assertEquals(0, value.getCrossingCount());
        assertEquals(0, value.getWaitingCount());
        assertEquals(0, value.getEmergencyWaitingCount());
        assertTrue(value.getActiveEvents().isEmpty());
        for (Direction direction : Direction.values())
        {
            assertEquals(0, value.getQueueLength(direction));
            assertEquals(0, value.getEmergencyQueueLength(direction));
            assertNull(value.getActiveEvent(direction));
            assertEquals(
                TrafficEventType.NONE,
                value.getActiveIncidentType(direction));
            assertEquals(0, value.getRemainingIncidentSeconds(direction));
        }
    }


    /**
     * Tests all vehicle and adaptive-signal metrics in a populated snapshot.
     */
    @Test
    public void testPopulatedSnapshot()
    {
        SimulationSnapshot value = populated();
        assertEquals(70, value.getElapsedSeconds());
        assertEquals(SignalPhase.EW_GREEN, value.getSignalPhase());
        assertEquals(60, value.getPhaseStartSecond());
        assertEquals(10, value.getPhaseElapsedSeconds());
        assertEquals(3, value.getPhaseSwitchCount());
        assertEquals(17, value.getTotalArrived());
        assertEquals(5, value.getTotalCompleted());
        assertEquals(2, value.getCrossingCount());
        assertEquals(10, value.getWaitingCount());
        assertEquals(3, value.getEmergencyWaitingCount());
        assertEquals(4, value.getQueueLength(Direction.NORTH));
        assertEquals(3, value.getQueueLength(Direction.SOUTH));
        assertEquals(2, value.getQueueLength(Direction.EAST));
        assertEquals(1, value.getQueueLength(Direction.WEST));
        assertEquals(2, value.getEmergencyQueueLength(Direction.NORTH));
        assertEquals(1, value.getEmergencyQueueLength(Direction.SOUTH));
    }


    /**
     * Verifies accident and slowdown types and their remaining durations.
     */
    @Test
    public void testIncidentTypesAndRemainingDurations()
    {
        SimulationSnapshot value = populated();
        assertEquals(2, value.getActiveEvents().size());
        assertEquals(
            TrafficEventType.ACCIDENT,
            value.getActiveIncidentType(Direction.NORTH));
        assertEquals(
            TrafficEventType.SLOWDOWN,
            value.getActiveIncidentType(Direction.EAST));
        assertEquals(35, value.getRemainingIncidentSeconds(Direction.NORTH));
        assertEquals(55, value.getRemainingIncidentSeconds(Direction.EAST));
        assertEquals(0, value.getRemainingIncidentSeconds(Direction.SOUTH));
        assertEquals(
            TrafficEventType.NONE,
            value.getActiveIncidentType(Direction.WEST));
        assertEquals(
            60,
            value.getActiveEvent(Direction.NORTH).getStartSecond());
    }


    /**
     * Verifies the start of an event is included in its active interval.
     */
    @Test
    public void testIncidentActiveAtStart()
    {
        Map<Direction, TrafficEvent> events = noEvents();
        events.put(
            Direction.WEST,
            new TrafficEvent(TrafficEventType.ACCIDENT, Direction.WEST, 60, 5));
        SimulationSnapshot value = snapshot(
            60,
            SignalPhase.NS_GREEN,
            60,
            1,
            counts(0, 0, 0, 0),
            counts(0, 0, 0, 0),
            events,
            0,
            0,
            0);
        assertEquals(5, value.getRemainingIncidentSeconds(Direction.WEST));
    }


    /**
     * Verifies the final active second shows one remaining incident second.
     */
    @Test
    public void testIncidentActiveAtFinalSecond()
    {
        Map<Direction, TrafficEvent> events = noEvents();
        events.put(
            Direction.WEST,
            new TrafficEvent(TrafficEventType.ACCIDENT, Direction.WEST, 60, 5));
        SimulationSnapshot value = snapshot(
            64,
            SignalPhase.NS_GREEN,
            60,
            1,
            counts(0, 0, 0, 0),
            counts(0, 0, 0, 0),
            events,
            0,
            0,
            0);
        assertEquals(1, value.getRemainingIncidentSeconds(Direction.WEST));
    }


    /**
     * Tests nonempty queues with all waiting vehicles classified as emergency.
     */
    @Test
    public void testEveryWaitingVehicleCanBeEmergency()
    {
        SimulationSnapshot value = snapshot(
            10,
            SignalPhase.NS_GREEN,
            0,
            0,
            counts(2, 1, 3, 0),
            counts(2, 1, 3, 0),
            noEvents(),
            6,
            0,
            0);
        assertEquals(6, value.getEmergencyWaitingCount());
        assertEquals(6, value.getWaitingCount());
    }


    /**
     * Rejects negative elapsed time.
     */
    @Test
    public void testNegativeElapsedTimeRejected()
    {
        assertInvalid(
            () -> snapshot(
                -1,
                SignalPhase.NS_GREEN,
                0,
                0,
                counts(0, 0, 0, 0),
                counts(0, 0, 0, 0),
                noEvents(),
                0,
                0,
                0));
    }


    /**
     * Rejects a missing current signal phase.
     */
    @Test
    public void testNullSignalPhaseRejected()
    {
        assertInvalid(
            () -> snapshot(
                10,
                null,
                0,
                0,
                counts(0, 0, 0, 0),
                counts(0, 0, 0, 0),
                noEvents(),
                0,
                0,
                0));
    }


    /**
     * Rejects phase start times outside the snapshot's elapsed interval.
     */
    @Test
    public void testInvalidPhaseStartRejected()
    {
        assertInvalid(
            () -> snapshot(
                10,
                SignalPhase.NS_GREEN,
                -1,
                0,
                counts(0, 0, 0, 0),
                counts(0, 0, 0, 0),
                noEvents(),
                0,
                0,
                0));
        assertInvalid(
            () -> snapshot(
                10,
                SignalPhase.NS_GREEN,
                11,
                0,
                counts(0, 0, 0, 0),
                counts(0, 0, 0, 0),
                noEvents(),
                0,
                0,
                0));
    }


    /**
     * Rejects negative phase switch counts.
     */
    @Test
    public void testNegativePhaseSwitchCountRejected()
    {
        assertInvalid(
            () -> snapshot(
                10,
                SignalPhase.NS_GREEN,
                0,
                -1,
                counts(0, 0, 0, 0),
                counts(0, 0, 0, 0),
                noEvents(),
                0,
                0,
                0));
    }


    /**
     * Rejects missing or incomplete waiting queue maps.
     */
    @Test
    public void testMissingWaitingQueueDataRejected()
    {
        assertInvalid(
            () -> snapshot(
                0,
                SignalPhase.NS_GREEN,
                0,
                0,
                null,
                counts(0, 0, 0, 0),
                noEvents(),
                0,
                0,
                0));
        Map<Direction, Integer> missing = counts(0, 0, 0, 0);
        missing.remove(Direction.WEST);
        assertInvalid(
            () -> snapshot(
                0,
                SignalPhase.NS_GREEN,
                0,
                0,
                missing,
                counts(0, 0, 0, 0),
                noEvents(),
                0,
                0,
                0));
    }


    /**
     * Rejects null and negative waiting counts.
     */
    @Test
    public void testInvalidWaitingQueueCountsRejected()
    {
        Map<Direction, Integer> nullCount = counts(0, 0, 0, 0);
        nullCount.put(Direction.EAST, null);
        assertInvalid(
            () -> snapshot(
                0,
                SignalPhase.NS_GREEN,
                0,
                0,
                nullCount,
                counts(0, 0, 0, 0),
                noEvents(),
                0,
                0,
                0));
        assertInvalid(
            () -> snapshot(
                0,
                SignalPhase.NS_GREEN,
                0,
                0,
                counts(-1, 0, 0, 0),
                counts(0, 0, 0, 0),
                noEvents(),
                0,
                0,
                0));
    }


    /**
     * Rejects a waiting queue map containing an extra null key.
     */
    @Test
    public void testExtraWaitingQueueEntryRejected()
    {
        Map<Direction, Integer> extra = new HashMap<>(counts(0, 0, 0, 0));
        extra.put(null, 0);
        assertInvalid(
            () -> snapshot(
                0,
                SignalPhase.NS_GREEN,
                0,
                0,
                extra,
                counts(0, 0, 0, 0),
                noEvents(),
                0,
                0,
                0));
    }


    /**
     * Rejects missing or incomplete emergency queue maps.
     */
    @Test
    public void testMissingEmergencyQueueDataRejected()
    {
        assertInvalid(
            () -> snapshot(
                0,
                SignalPhase.NS_GREEN,
                0,
                0,
                counts(0, 0, 0, 0),
                null,
                noEvents(),
                0,
                0,
                0));
        Map<Direction, Integer> missing = counts(0, 0, 0, 0);
        missing.remove(Direction.SOUTH);
        assertInvalid(
            () -> snapshot(
                0,
                SignalPhase.NS_GREEN,
                0,
                0,
                counts(0, 0, 0, 0),
                missing,
                noEvents(),
                0,
                0,
                0));
    }


    /**
     * Rejects null and negative emergency queue counts.
     */
    @Test
    public void testInvalidEmergencyQueueCountsRejected()
    {
        Map<Direction, Integer> nullCount = counts(0, 0, 0, 0);
        nullCount.put(Direction.EAST, null);
        assertInvalid(
            () -> snapshot(
                0,
                SignalPhase.NS_GREEN,
                0,
                0,
                counts(0, 0, 0, 0),
                nullCount,
                noEvents(),
                0,
                0,
                0));
        assertInvalid(
            () -> snapshot(
                0,
                SignalPhase.NS_GREEN,
                0,
                0,
                counts(0, 0, 0, 0),
                counts(-1, 0, 0, 0),
                noEvents(),
                0,
                0,
                0));
    }


    /**
     * Rejects a greater emergency queue count than total waiting vehicles.
     */
    @Test
    public void testEmergencyCannotExceedApproachQueue()
    {
        assertInvalid(
            () -> snapshot(
                0,
                SignalPhase.NS_GREEN,
                0,
                0,
                counts(2, 0, 0, 0),
                counts(3, 0, 0, 0),
                noEvents(),
                2,
                0,
                0));
    }


    /**
     * Rejects emergency queue maps with extra keys.
     */
    @Test
    public void testExtraEmergencyQueueEntryRejected()
    {
        Map<Direction, Integer> extra = new HashMap<>(counts(0, 0, 0, 0));
        extra.put(null, 0);
        assertInvalid(
            () -> snapshot(
                0,
                SignalPhase.NS_GREEN,
                0,
                0,
                counts(0, 0, 0, 0),
                extra,
                noEvents(),
                0,
                0,
                0));
    }


    /**
     * Rejects negative arrived, completed, and crossing totals.
     */
    @Test
    public void testNegativeVehicleTotalsRejected()
    {
        assertInvalid(
            () -> snapshot(
                0,
                SignalPhase.NS_GREEN,
                0,
                0,
                counts(0, 0, 0, 0),
                counts(0, 0, 0, 0),
                noEvents(),
                -1,
                0,
                0));
        assertInvalid(
            () -> snapshot(
                0,
                SignalPhase.NS_GREEN,
                0,
                0,
                counts(0, 0, 0, 0),
                counts(0, 0, 0, 0),
                noEvents(),
                0,
                -1,
                0));
        assertInvalid(
            () -> snapshot(
                0,
                SignalPhase.NS_GREEN,
                0,
                0,
                counts(0, 0, 0, 0),
                counts(0, 0, 0, 0),
                noEvents(),
                0,
                0,
                -1));
    }


    /**
     * Rejects snapshots whose vehicle counts violate conservation.
     */
    @Test
    public void testVehicleConservationRejected()
    {
        assertInvalid(
            () -> snapshot(
                10,
                SignalPhase.NS_GREEN,
                0,
                0,
                counts(2, 1, 0, 0),
                counts(0, 0, 0, 0),
                noEvents(),
                5,
                1,
                0));
        assertInvalid(
            () -> snapshot(
                10,
                SignalPhase.NS_GREEN,
                0,
                0,
                counts(2, 1, 0, 0),
                counts(0, 0, 0, 0),
                noEvents(),
                3,
                1,
                0));
    }


    /**
     * Tests that vehicle conservation uses long arithmetic to avoid overflow.
     */
    @Test
    public void testConservationArithmeticCannotOverflow()
    {
        assertInvalid(
            () -> snapshot(
                10,
                SignalPhase.NS_GREEN,
                0,
                0,
                counts(Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0),
                counts(0, 0, 0, 0),
                noEvents(),
                0,
                0,
                0));
        SimulationSnapshot maximum = snapshot(
            10,
            SignalPhase.NS_GREEN,
            0,
            0,
            counts(Integer.MAX_VALUE, 0, 0, 0),
            counts(0, 0, 0, 0),
            noEvents(),
            Integer.MAX_VALUE,
            0,
            0);
        assertEquals(Integer.MAX_VALUE, maximum.getWaitingCount());
    }


    /**
     * Rejects a null active incident map.
     */
    @Test
    public void testNullIncidentMapRejected()
    {
        assertInvalid(
            () -> snapshot(
                0,
                SignalPhase.NS_GREEN,
                0,
                0,
                counts(0, 0, 0, 0),
                counts(0, 0, 0, 0),
                null,
                0,
                0,
                0));
    }


    /**
     * Rejects active incident maps containing a null direction key.
     */
    @Test
    public void testNullIncidentKeyRejected()
    {
        Map<Direction, TrafficEvent> events = new HashMap<>();
        events.put(
            null,
            new TrafficEvent(
                TrafficEventType.ACCIDENT,
                Direction.NORTH,
                10,
                10));
        assertInvalid(
            () -> snapshot(
                10,
                SignalPhase.NS_GREEN,
                0,
                0,
                counts(0, 0, 0, 0),
                counts(0, 0, 0, 0),
                events,
                0,
                0,
                0));
    }


    /**
     * Rejects active incident maps containing a null incident.
     */
    @Test
    public void testNullIncidentValueRejected()
    {
        Map<Direction, TrafficEvent> events = noEvents();
        events.put(Direction.NORTH, null);
        assertInvalid(
            () -> snapshot(
                10,
                SignalPhase.NS_GREEN,
                0,
                0,
                counts(0, 0, 0, 0),
                counts(0, 0, 0, 0),
                events,
                0,
                0,
                0));
    }


    /**
     * Rejects incidents stored under a different direction than affected.
     */
    @Test
    public void testMismatchedIncidentDirectionRejected()
    {
        Map<Direction, TrafficEvent> events = noEvents();
        events.put(
            Direction.NORTH,
            new TrafficEvent(
                TrafficEventType.ACCIDENT,
                Direction.EAST,
                10,
                10));
        assertInvalid(
            () -> snapshot(
                10,
                SignalPhase.NS_GREEN,
                0,
                0,
                counts(0, 0, 0, 0),
                counts(0, 0, 0, 0),
                events,
                0,
                0,
                0));
    }


    /**
     * Rejects incidents that have not started by the snapshot time.
     */
    @Test
    public void testFutureIncidentRejected()
    {
        Map<Direction, TrafficEvent> events = noEvents();
        events.put(
            Direction.NORTH,
            new TrafficEvent(
                TrafficEventType.ACCIDENT,
                Direction.NORTH,
                11,
                10));
        assertInvalid(
            () -> snapshot(
                10,
                SignalPhase.NS_GREEN,
                0,
                0,
                counts(0, 0, 0, 0),
                counts(0, 0, 0, 0),
                events,
                0,
                0,
                0));
    }


    /**
     * Rejects incidents at or after their exclusive end time.
     */
    @Test
    public void testExpiredIncidentRejected()
    {
        Map<Direction, TrafficEvent> events = noEvents();
        events.put(
            Direction.NORTH,
            new TrafficEvent(
                TrafficEventType.ACCIDENT,
                Direction.NORTH,
                10,
                5));
        assertInvalid(
            () -> snapshot(
                15,
                SignalPhase.NS_GREEN,
                0,
                0,
                counts(0, 0, 0, 0),
                counts(0, 0, 0, 0),
                events,
                0,
                0,
                0));
    }


    /**
     * Tests all input maps are copied rather than retained by the snapshot.
     */
    @Test
    public void testInputMapsDefensivelyCopied()
    {
        Map<Direction, Integer> queues = counts(1, 0, 0, 0);
        Map<Direction, Integer> emergencies = counts(1, 0, 0, 0);
        Map<Direction, TrafficEvent> events = noEvents();
        TrafficEvent accident = new TrafficEvent(
            TrafficEventType.ACCIDENT,
            Direction.NORTH,
            10,
            10);
        events.put(Direction.NORTH, accident);
        SimulationSnapshot value = snapshot(
            10,
            SignalPhase.NS_GREEN,
            0,
            0,
            queues,
            emergencies,
            events,
            1,
            0,
            0);
        queues.put(Direction.NORTH, 100);
        emergencies.put(Direction.NORTH, 0);
        events.clear();
        assertEquals(1, value.getQueueLength(Direction.NORTH));
        assertEquals(1, value.getEmergencyQueueLength(Direction.NORTH));
        assertEquals(1, value.getEmergencyWaitingCount());
        assertTrue(accident == value.getActiveEvent(Direction.NORTH));
    }


    /**
     * Tests all returned maps are read-only.
     */
    @Test
    public void testReturnedMapsUnmodifiable()
    {
        SimulationSnapshot value = populated();
        try
        {
            value.getQueueLengths().put(Direction.NORTH, 99);
            fail("Expected immutable normal queue map.");
        }
        catch (UnsupportedOperationException expected)
        {
            // Expected read-only map.
        }
        try
        {
            value.getEmergencyQueueLengths().clear();
            fail("Expected immutable emergency queue map.");
        }
        catch (UnsupportedOperationException expected)
        {
            // Expected read-only map.
        }
        try
        {
            value.getActiveEvents().remove(Direction.NORTH);
            fail("Expected immutable incident map.");
        }
        catch (UnsupportedOperationException expected)
        {
            // Expected read-only map.
        }
        assertEquals(4, value.getQueueLength(Direction.NORTH));
        assertEquals(2, value.getEmergencyQueueLength(Direction.NORTH));
        assertEquals(2, value.getActiveEvents().size());
    }


    /**
     * Verifies all direction-specific getters reject null input.
     */
    @Test
    public void testNullDirectionLookupsRejected()
    {
        SimulationSnapshot value = populated();
        assertInvalid(() -> value.getQueueLength(null));
        assertInvalid(() -> value.getEmergencyQueueLength(null));
        assertInvalid(() -> value.getActiveEvent(null));
        assertInvalid(() -> value.getActiveIncidentType(null));
        assertInvalid(() -> value.getRemainingIncidentSeconds(null));
    }


    /**
     * Tests snapshots do not share mutable state with one another.
     */
    @Test
    public void testIndependentSnapshots()
    {
        SimulationSnapshot first = populated();
        SimulationSnapshot second = initial();
        assertEquals(17, first.getTotalArrived());
        assertEquals(0, second.getTotalArrived());
        assertEquals(2, first.getActiveEvents().size());
        assertTrue(second.getActiveEvents().isEmpty());
        assertEquals(3, first.getEmergencyWaitingCount());
        assertEquals(0, second.getEmergencyWaitingCount());
    }


    /**
     * Tests empty immutable input maps are rejected if incomplete.
     */
    @Test
    public void testEmptyQueueMapsRejected()
    {
        assertInvalid(
            () -> snapshot(
                0,
                SignalPhase.NS_GREEN,
                0,
                0,
                Collections.<Direction, Integer> emptyMap(),
                counts(0, 0, 0, 0),
                noEvents(),
                0,
                0,
                0));
        assertInvalid(
            () -> snapshot(
                0,
                SignalPhase.NS_GREEN,
                0,
                0,
                counts(0, 0, 0, 0),
                Collections.<Direction, Integer> emptyMap(),
                noEvents(),
                0,
                0,
                0));
    }
}
