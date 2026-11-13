package defaultPackage;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.EnumMap;
import java.util.Map;
import org.junit.Test;

/**
 * JUnit 4 tests for StatisticsAccumulator. Verifies vehicle counts, waiting
 * times, queue statistics, and invalid input handling without relying on the
 * controller or terminal view.
 */
public class StatisticsAccumulatorTest
{

    /**
     * Creates an empty Lane for each incoming approach.
     *
     * @return map containing all four lane objects
     */
    private Map<Direction, Lane> createLanes()
    {
        Map<Direction, Lane> lanes = new EnumMap<>(Direction.class);

        for (Direction direction : Direction.values())
        {
            lanes.put(direction, new Lane(direction, 0, 0, 0));
        }

        return lanes;
    }


    /**
     * Creates a vehicle that has completed its crossing.
     *
     * @param id
     *            unique vehicle identifier
     * @param arrived
     *            vehicle arrival second
     * @param released
     *            crossing start second
     * @return completed vehicle with known waiting time
     */
    private Vehicle completedVehicle(long id, int arrived, int released)
    {

        Vehicle vehicle =
            new Vehicle(id, Direction.NORTH, Turn.STRAIGHT, arrived);

        vehicle.startCrossing(released);
        vehicle.advance(20);
        vehicle.finishCrossing();

        return vehicle;
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
     * Verifies that an operation throws IllegalStateException.
     *
     * @param action
     *            operation expected to fail
     */
    private void assertBadState(Runnable action)
    {
        try
        {
            action.run();
            fail("Expected IllegalStateException");
        }
        catch (IllegalStateException expected)
        {
            // Expected exception.
        }
    }


    /**
     * Tests that the constructor initializes all statistics to zero and average
     * wait to null.
     */
    @Test
    public void testConstructor()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();

        assertEquals(0, stats.getTotalArrived());
        assertEquals(0, stats.getTotalCompleted());
        assertEquals(0L, stats.getTotalWaitingSecondsCompleted());
        assertEquals(0, stats.getQueueSampleCount());
        assertNull(stats.getAverageWaitingTimeSeconds());

        for (Direction direction : Direction.values())
        {
            assertEquals(0.0, stats.getAverageQueueLength(direction), 0.0001);

            assertEquals(0, stats.getMaximumQueueLength(direction));
        }
    }


    /**
     * Tests recording several batches of arriving vehicles, including a batch
     * of zero.
     */
    @Test
    public void testRecordArrivals()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();

        stats.recordArrivals(3);
        stats.recordArrivals(0);
        stats.recordArrivals(2);

        assertEquals(5, stats.getTotalArrived());
        assertEquals(0, stats.getTotalCompleted());
    }


    /**
     * Tests rejection of a negative arrival count without modifying existing
     * statistics.
     */
    @Test
    public void testNegativeArrivalCount()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();

        stats.recordArrivals(2);

        assertInvalid(() -> stats.recordArrivals(-1));

        assertEquals(2, stats.getTotalArrived());
    }


    /**
     * Tests that an arrival count causing integer overflow is rejected.
     */
    @Test
    public void testArrivalOverflow()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();

        stats.recordArrivals(Integer.MAX_VALUE);

        assertInvalid(() -> stats.recordArrivals(1));

        assertEquals(Integer.MAX_VALUE, stats.getTotalArrived());
    }


    /**
     * Tests recording a completed vehicle and its queue waiting time.
     */
    @Test
    public void testRecordCompletion()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();

        stats.recordArrivals(1);
        stats.recordCompletion(completedVehicle(1, 5, 12));

        assertEquals(1, stats.getTotalCompleted());
        assertEquals(7L, stats.getTotalWaitingSecondsCompleted());

        assertEquals(7.0, stats.getAverageWaitingTimeSeconds(), 0.0001);
    }


    /**
     * Tests average waiting time across multiple completed vehicles. Vehicles
     * that have arrived but not completed must not contribute to the average.
     */
    @Test
    public void testAverageWait()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();

        stats.recordArrivals(3);

        stats.recordCompletion(completedVehicle(1, 5, 12));

        stats.recordCompletion(completedVehicle(2, 7, 10));

        assertEquals(2, stats.getTotalCompleted());
        assertEquals(10L, stats.getTotalWaitingSecondsCompleted());

        assertEquals(5.0, stats.getAverageWaitingTimeSeconds(), 0.0001);

        assertEquals(3, stats.getTotalArrived());
    }


    /**
     * Tests that average waiting time remains undefined when no vehicles have
     * completed.
     */
    @Test
    public void testNoCompletedVehicles()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();

        stats.recordArrivals(10);

        assertNull(stats.getAverageWaitingTimeSeconds());
    }


    /**
     * Tests rejection of a null vehicle without changing the completed vehicle
     * count.
     */
    @Test
    public void testNullCompletion()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();

        stats.recordArrivals(1);

        assertInvalid(() -> stats.recordCompletion(null));

        assertEquals(0, stats.getTotalCompleted());
    }


    /**
     * Tests that WAITING and CROSSING vehicles cannot be recorded as completed.
     */
    @Test
    public void testUnfinishedCompletion()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();

        stats.recordArrivals(1);

        Vehicle vehicle = new Vehicle(1, Direction.NORTH, Turn.STRAIGHT, 0);

        assertBadState(() -> stats.recordCompletion(vehicle));

        vehicle.startCrossing(4);

        assertBadState(() -> stats.recordCompletion(vehicle));

        vehicle.advance(20);
        vehicle.finishCrossing();

        stats.recordCompletion(vehicle);

        assertEquals(1, stats.getTotalCompleted());
    }


    /**
     * Tests that the same vehicle cannot be counted as completed more than
     * once.
     */
    @Test
    public void testDuplicateCompletion()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();

        stats.recordArrivals(2);

        Vehicle vehicle = completedVehicle(1, 0, 3);

        stats.recordCompletion(vehicle);

        assertInvalid(() -> stats.recordCompletion(vehicle));

        assertEquals(1, stats.getTotalCompleted());

        assertEquals(3L, stats.getTotalWaitingSecondsCompleted());
    }


    /**
     * Tests that a completed vehicle cannot be recorded without a corresponding
     * arrival.
     */
    @Test
    public void testCompletionWithoutArrival()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();

        assertBadState(() -> stats.recordCompletion(completedVehicle(1, 0, 1)));

        assertEquals(0, stats.getTotalCompleted());
    }


    /**
     * Tests sampling of four completely empty lane queues over multiple
     * simulation steps.
     */
    @Test
    public void testEmptyQueueSamples()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();

        Map<Direction, Lane> lanes = createLanes();

        stats.sampleQueues(lanes);
        stats.sampleQueues(lanes);

        assertEquals(2, stats.getQueueSampleCount());

        for (Direction direction : Direction.values())
        {
            assertEquals(0.0, stats.getAverageQueueLength(direction), 0.0001);

            assertEquals(0, stats.getMaximumQueueLength(direction));
        }
    }


    /**
     * Tests average and maximum queue sizes using actual vehicles in a Lane's
     * LinkedQueue. Three vehicles are queued for the first sample; one is
     * released before the second sample.
     */
    @Test
    public void testNonemptyQueueSamples()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();

        Map<Direction, Lane> lanes = createLanes();

        Lane north = new Lane(Direction.NORTH, 1800, 0, 0);

        lanes.put(Direction.NORTH, north);

        long nextId = 1L;

        // Generate three arrivals in six steps.
        for (int second = 0; second < 6; second++)
        {
            int created = north.generateArrivals(second, nextId);

            nextId += created;
        }

        assertEquals(3, north.getQueueLength());

        // First sample: north queue length = 3.
        stats.sampleQueues(lanes);

        Vehicle released = north.releaseNext(6, true);
        assertNotNull(released);

        // Second sample: north queue length = 2.
        stats.sampleQueues(lanes);

        assertEquals(2, stats.getQueueSampleCount());

        assertEquals(2.5, stats.getAverageQueueLength(Direction.NORTH), 0.0001);

        assertEquals(3, stats.getMaximumQueueLength(Direction.NORTH));

        assertEquals(0.0, stats.getAverageQueueLength(Direction.SOUTH), 0.0001);

        assertEquals(0, stats.getMaximumQueueLength(Direction.SOUTH));
    }


    /**
     * Tests rejection of null, incomplete, and mismatched lane maps. Invalid
     * samples must not change statistics.
     */
    @Test
    public void testInvalidQueueSampleMap()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();

        assertInvalid(() -> stats.sampleQueues(null));

        Map<Direction, Lane> lanes = createLanes();

        lanes.remove(Direction.WEST);

        assertInvalid(() -> stats.sampleQueues(lanes));

        lanes.put(Direction.WEST, null);

        assertInvalid(() -> stats.sampleQueues(lanes));

        lanes.put(Direction.WEST, new Lane(Direction.NORTH, 0, 0, 0));

        assertInvalid(() -> stats.sampleQueues(lanes));

        assertEquals(0, stats.getQueueSampleCount());
    }


    /**
     * Tests that null directions are rejected by queue-statistics getter
     * methods.
     */
    @Test
    public void testNullDirectionQueries()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();

        assertInvalid(() -> stats.getAverageQueueLength(null));

        assertInvalid(() -> stats.getMaximumQueueLength(null));
    }
}
