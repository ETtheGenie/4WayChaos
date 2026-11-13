package defaultPackage;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.EnumMap;
import java.util.Map;
import org.junit.Test;

/**
 * JUnit 4 tests for SimulationSnapshot. Verifies snapshot creation, vehicle
 * conservation, immutability, and invalid input handling.
 */
public class SimulationSnapshotTest
{

    /**
     * Creates a valid map of queue lengths. The queues contain four waiting
     * vehicles total.
     *
     * @return queue lengths for all four directions
     */
    private Map<Direction, Integer> createQueueLengths()
    {
        Map<Direction, Integer> queues = new EnumMap<>(Direction.class);

        queues.put(Direction.NORTH, 2);
        queues.put(Direction.SOUTH, 1);
        queues.put(Direction.EAST, 0);
        queues.put(Direction.WEST, 1);

        return queues;
    }


    /**
     * Constructs a valid snapshot containing four waiting, two crossing, and
     * three completed vehicles.
     *
     * @return valid snapshot with nine total arrivals
     */
    private SimulationSnapshot createValidSnapshot()
    {
        return new SimulationSnapshot(
            10,
            SignalPhase.NS_GREEN,
            createQueueLengths(),
            9,
            3,
            2);
    }


    /**
     * Verifies that an operation throws IllegalArgumentException.
     *
     * @param action
     *            operation expected to fail
     */
    private void assertInvalid(Runnable action)
    {
        try
        {
            action.run();
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected)
        {
            // Expected exception.
        }
    }


    /**
     * Tests that the constructor stores all supplied data and calculates the
     * total waiting count.
     */
    @Test
    public void testConstructorAndGetters()
    {
        SimulationSnapshot snapshot = createValidSnapshot();

        assertEquals(10, snapshot.getElapsedSeconds());
        assertEquals(SignalPhase.NS_GREEN, snapshot.getSignalPhase());

        assertEquals(9, snapshot.getTotalArrived());
        assertEquals(3, snapshot.getTotalCompleted());
        assertEquals(2, snapshot.getCrossingCount());
        assertEquals(4, snapshot.getWaitingCount());
    }


    /**
     * Tests construction of an initial snapshot containing no vehicles or
     * elapsed time.
     */
    @Test
    public void testEmptySnapshot()
    {
        Map<Direction, Integer> queues = new EnumMap<>(Direction.class);

        for (Direction direction : Direction.values())
        {
            queues.put(direction, 0);
        }

        SimulationSnapshot snapshot =
            new SimulationSnapshot(0, SignalPhase.NS_GREEN, queues, 0, 0, 0);

        assertEquals(0, snapshot.getElapsedSeconds());
        assertEquals(0, snapshot.getWaitingCount());
        assertEquals(0, snapshot.getTotalArrived());
        assertEquals(0, snapshot.getTotalCompleted());
        assertEquals(0, snapshot.getCrossingCount());
    }


    /**
     * Tests retrieval of queue lengths for all four incoming directions.
     */
    @Test
    public void testQueueLengthGetters()
    {
        SimulationSnapshot snapshot = createValidSnapshot();

        assertEquals(2, snapshot.getQueueLength(Direction.NORTH));

        assertEquals(1, snapshot.getQueueLength(Direction.SOUTH));

        assertEquals(0, snapshot.getQueueLength(Direction.EAST));

        assertEquals(1, snapshot.getQueueLength(Direction.WEST));

        assertEquals(4, snapshot.getQueueLengths().size());
    }


    /**
     * Tests that changes to the original queue map do not affect an existing
     * snapshot.
     */
    @Test
    public void testDefensiveCopy()
    {
        Map<Direction, Integer> queues = createQueueLengths();

        SimulationSnapshot snapshot =
            new SimulationSnapshot(10, SignalPhase.NS_GREEN, queues, 9, 3, 2);

        queues.put(Direction.NORTH, 100);
        queues.remove(Direction.SOUTH);

        assertEquals(2, snapshot.getQueueLength(Direction.NORTH));

        assertEquals(1, snapshot.getQueueLength(Direction.SOUTH));

        assertEquals(4, snapshot.getWaitingCount());
    }


    /**
     * Tests that the map returned by the snapshot cannot be modified by another
     * class.
     */
    @Test
    public void testUnmodifiableQueueMap()
    {
        SimulationSnapshot snapshot = createValidSnapshot();

        Map<Direction, Integer> queues = snapshot.getQueueLengths();

        try
        {
            queues.put(Direction.NORTH, 50);
            fail("Expected UnsupportedOperationException");
        }
        catch (UnsupportedOperationException expected)
        {
            // Expected exception.
        }

        assertEquals(2, snapshot.getQueueLength(Direction.NORTH));
    }


    /**
     * Tests that a previously created snapshot remains unchanged after a newer
     * snapshot is constructed with updated information.
     */
    @Test
    public void testIndependentSnapshots()
    {
        Map<Direction, Integer> queues = createQueueLengths();

        SimulationSnapshot first =
            new SimulationSnapshot(10, SignalPhase.NS_GREEN, queues, 9, 3, 2);

        queues.put(Direction.NORTH, 3);

        SimulationSnapshot second =
            new SimulationSnapshot(11, SignalPhase.EW_GREEN, queues, 10, 3, 2);

        assertEquals(2, first.getQueueLength(Direction.NORTH));

        assertEquals(3, second.getQueueLength(Direction.NORTH));

        assertEquals(10, first.getElapsedSeconds());
        assertEquals(11, second.getElapsedSeconds());

        assertEquals(SignalPhase.NS_GREEN, first.getSignalPhase());

        assertEquals(SignalPhase.EW_GREEN, second.getSignalPhase());
    }


    /**
     * Tests rejection of a null traffic signal phase.
     */
    @Test
    public void testNullSignalPhase()
    {
        assertInvalid(
            () -> new SimulationSnapshot(
                10,
                null,
                createQueueLengths(),
                9,
                3,
                2));
    }


    /**
     * Tests rejection of a null queue-length map.
     */
    @Test
    public void testNullQueueMap()
    {
        assertInvalid(
            () -> new SimulationSnapshot(
                10,
                SignalPhase.NS_GREEN,
                null,
                9,
                3,
                2));
    }


    /**
     * Tests rejection of queue maps that omit a direction or contain a null
     * queue length.
     */
    @Test
    public void testIncompleteQueueMap()
    {
        Map<Direction, Integer> queues = createQueueLengths();

        queues.remove(Direction.WEST);

        assertInvalid(
            () -> new SimulationSnapshot(
                10,
                SignalPhase.NS_GREEN,
                queues,
                9,
                3,
                2));

        queues.put(Direction.WEST, null);

        assertInvalid(
            () -> new SimulationSnapshot(
                10,
                SignalPhase.NS_GREEN,
                queues,
                9,
                3,
                2));
    }


    /**
     * Tests rejection of negative queue lengths.
     */
    @Test
    public void testNegativeQueueLength()
    {
        Map<Direction, Integer> queues = createQueueLengths();

        queues.put(Direction.NORTH, -1);

        assertInvalid(
            () -> new SimulationSnapshot(
                10,
                SignalPhase.NS_GREEN,
                queues,
                9,
                3,
                2));
    }


    /**
     * Tests rejection of negative elapsed time.
     */
    @Test
    public void testNegativeElapsedTime()
    {
        assertInvalid(
            () -> new SimulationSnapshot(
                -1,
                SignalPhase.NS_GREEN,
                createQueueLengths(),
                9,
                3,
                2));
    }


    /**
     * Tests rejection of negative arrival, completion, and crossing counts.
     */
    @Test
    public void testNegativeVehicleCounts()
    {
        Map<Direction, Integer> queues = createQueueLengths();

        assertInvalid(
            () -> new SimulationSnapshot(
                10,
                SignalPhase.NS_GREEN,
                queues,
                -1,
                3,
                2));

        assertInvalid(
            () -> new SimulationSnapshot(
                10,
                SignalPhase.NS_GREEN,
                queues,
                9,
                -1,
                2));

        assertInvalid(
            () -> new SimulationSnapshot(
                10,
                SignalPhase.NS_GREEN,
                queues,
                9,
                3,
                -1));
    }


    /**
     * Tests that the constructor rejects vehicle counts violating the
     * conservation equation.
     */
    @Test
    public void testVehicleConservation()
    {
        Map<Direction, Integer> queues = createQueueLengths();

        // Correct total is 4 + 2 + 3 = 9.
        assertInvalid(
            () -> new SimulationSnapshot(
                10,
                SignalPhase.NS_GREEN,
                queues,
                10,
                3,
                2));

        assertInvalid(
            () -> new SimulationSnapshot(
                10,
                SignalPhase.NS_GREEN,
                queues,
                8,
                3,
                2));

        SimulationSnapshot valid =
            new SimulationSnapshot(10, SignalPhase.NS_GREEN, queues, 9, 3, 2);

        assertEquals(
            valid.getTotalArrived(),
            valid.getWaitingCount() + valid.getCrossingCount()
                + valid.getTotalCompleted());
    }


    /**
     * Tests that a null approach cannot be used to query an individual queue
     * length.
     */
    @Test
    public void testNullDirectionGetter()
    {
        SimulationSnapshot snapshot = createValidSnapshot();

        assertInvalid(() -> snapshot.getQueueLength(null));
    }


    /**
     * Tests a very large valid vehicle count and confirms that queue-sum
     * validation does not overflow integer arithmetic.
     */
    @Test
    public void testLargeVehicleCounts()
    {
        Map<Direction, Integer> empty = new EnumMap<>(Direction.class);

        for (Direction direction : Direction.values())
        {
            empty.put(direction, 0);
        }

        SimulationSnapshot valid = new SimulationSnapshot(
            Integer.MAX_VALUE,
            SignalPhase.NS_GREEN,
            empty,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            0);

        assertEquals(Integer.MAX_VALUE, valid.getTotalArrived());

        Map<Direction, Integer> huge = new EnumMap<>(Direction.class);

        for (Direction direction : Direction.values())
        {
            huge.put(direction, Integer.MAX_VALUE);
        }

        assertInvalid(
            () -> new SimulationSnapshot(
                10,
                SignalPhase.NS_GREEN,
                huge,
                Integer.MAX_VALUE,
                0,
                0));
    }
}
