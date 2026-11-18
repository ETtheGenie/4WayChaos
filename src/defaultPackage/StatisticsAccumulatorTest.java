
package defaultPackage;

import static org.junit.Assert.*;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.Test;

/**
 * JUnit 4 tests for V2 statistics, emergency metrics, incident counts,
 * waiting-time calculations, queue samples, and bad-input handling.
 */
public class StatisticsAccumulatorTest
{

    /**
     * Creates a vehicle that has entered and finished crossing.
     *
     * @param id
     *            unique vehicle identifier
     * @param type
     *            vehicle classification
     * @param arrival
     *            arrival second
     * @param crossing
     *            crossing start second
     * @return vehicle in DONE state
     */
    private
        Vehicle
        completed(long id, VehicleType type, int arrival, int crossing)
    {
        Vehicle vehicle =
            new Vehicle(id, Direction.NORTH, Turn.STRAIGHT, arrival, type);
        vehicle.startCrossing(crossing);
        vehicle.finishCrossing();
        return vehicle;
    }


    /**
     * Builds four valid V2 lanes with independently seeded random sources.
     *
     * @param north
     *            northbound arrival rate
     * @param south
     *            southbound arrival rate
     * @param east
     *            eastbound arrival rate
     * @param west
     *            westbound arrival rate
     * @return map containing one matching lane for every direction
     */
    private Map<Direction, Lane> lanes(int north, int south, int east, int west)
    {
        int[] rates = { north, south, east, west };
        EnumMap<Direction, Lane> result = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values())
        {
            int index = direction.ordinal();
            result.put(
                direction,
                new Lane(
                    direction,
                    rates[index],
                    0,
                    0,
                    0,
                    new Random(11L + index),
                    new Random(71L + index)));
        }
        return result;
    }


    /**
     * Generates consecutive arrival steps for all four lanes.
     *
     * @param lanes
     *            map to advance
     * @param seconds
     *            number of steps beginning at second zero
     */
    private void generate(Map<Direction, Lane> lanes, int seconds)
    {
        for (int second = 0; second < seconds; second++)
        {
            for (Direction direction : Direction.values())
            {
                long id = direction.ordinal() * 100000L + second * 2L + 1L;
                lanes.get(direction).generateArrivals(second, id);
            }
        }
    }


    /**
     * Confirms that an operation rejects invalid arguments.
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
     * Confirms that an operation rejects an invalid simulator state.
     *
     * @param action
     *            operation expected to throw IllegalStateException
     */
    private void assertIllegalState(Runnable action)
    {
        try
        {
            action.run();
            fail("Expected IllegalStateException.");
        }
        catch (IllegalStateException expected)
        {
            // Expected invalid state.
        }
    }


    /**
     * Verifies initial totals, both nullable averages, and zero queue stats.
     */
    @Test
    public void testInitialState()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        assertEquals(0, stats.getTotalArrived());
        assertEquals(0, stats.getTotalEmergencyArrived());
        assertEquals(0, stats.getTotalCompleted());
        assertEquals(0, stats.getTotalEmergencyCompleted());
        assertEquals(0L, stats.getTotalWaitingSecondsCompleted());
        assertEquals(0L, stats.getTotalEmergencyWaitingSecondsCompleted());
        assertNull(stats.getAverageWaitingTimeSeconds());
        assertNull(stats.getAverageEmergencyWaitingTimeSeconds());
        assertEquals(0, stats.getTotalAccidents());
        assertEquals(0, stats.getTotalSlowdowns());
        assertEquals(0, stats.getQueueSampleCount());
        for (Direction direction : Direction.values())
        {
            assertEquals(0.0, stats.getAverageQueueLength(direction), 0.0);
            assertEquals(0, stats.getMaximumQueueLength(direction));
        }
    }


    /**
     * Tests zero arrivals and separately accumulated normal/emergency batches.
     */
    @Test
    public void testArrivalBatches()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        stats.recordArrivals(0, 0);
        stats.recordArrivals(10, 2);
        stats.recordArrivals(3, 3);
        stats.recordArrivals(5, 0);
        assertEquals(18, stats.getTotalArrived());
        assertEquals(5, stats.getTotalEmergencyArrived());
        assertEquals(0, stats.getTotalCompleted());
    }


    /**
     * Rejects negative total or emergency arrival counts without mutation.
     */
    @Test
    public void testNegativeArrivalCountsRejected()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        stats.recordArrivals(3, 1);
        assertInvalid(() -> stats.recordArrivals(-1, 0));
        assertInvalid(() -> stats.recordArrivals(1, -1));
        assertInvalid(() -> stats.recordArrivals(-1, -1));
        assertEquals(3, stats.getTotalArrived());
        assertEquals(1, stats.getTotalEmergencyArrived());
    }


    /**
     * Rejects emergency counts greater than the containing arrival batch.
     */
    @Test
    public void testEmergencyCountCannotExceedBatch()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        assertInvalid(() -> stats.recordArrivals(0, 1));
        assertInvalid(() -> stats.recordArrivals(2, 3));
        assertEquals(0, stats.getTotalArrived());
        assertEquals(0, stats.getTotalEmergencyArrived());
    }


    /**
     * Rejects integer arrival overflow while preserving prior totals.
     */
    @Test
    public void testArrivalOverflowRejectedWithoutMutation()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        stats.recordArrivals(Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 1);
        assertInvalid(() -> stats.recordArrivals(2, 0));
        assertEquals(Integer.MAX_VALUE - 1, stats.getTotalArrived());
        stats.recordArrivals(1, 0);
        assertEquals(Integer.MAX_VALUE, stats.getTotalArrived());
        assertEquals(Integer.MAX_VALUE - 1, stats.getTotalEmergencyArrived());
        assertInvalid(() -> stats.recordArrivals(1, 0));
    }


    /**
     * Counts an ordinary vehicle completion and its queue waiting time.
     */
    @Test
    public void testNormalCompletion()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        stats.recordArrivals(1, 0);
        stats.recordCompletion(completed(1, VehicleType.NORMAL, 4, 9));
        assertEquals(1, stats.getTotalCompleted());
        assertEquals(0, stats.getTotalEmergencyCompleted());
        assertEquals(5L, stats.getTotalWaitingSecondsCompleted());
        assertEquals(0L, stats.getTotalEmergencyWaitingSecondsCompleted());
        assertEquals(5.0, stats.getAverageWaitingTimeSeconds(), 0.000001);
        assertNull(stats.getAverageEmergencyWaitingTimeSeconds());
    }


    /**
     * Counts an emergency completion in both general and emergency metrics.
     */
    @Test
    public void testEmergencyCompletion()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        stats.recordArrivals(1, 1);
        stats.recordCompletion(completed(10, VehicleType.EMERGENCY, 5, 12));
        assertEquals(1, stats.getTotalCompleted());
        assertEquals(1, stats.getTotalEmergencyCompleted());
        assertEquals(7L, stats.getTotalWaitingSecondsCompleted());
        assertEquals(7L, stats.getTotalEmergencyWaitingSecondsCompleted());
        assertEquals(
            7.0,
            stats.getAverageEmergencyWaitingTimeSeconds(),
            0.000001);
    }


    /**
     * Verifies overall and emergency waiting-time averages are different.
     */
    @Test
    public void testMixedVehicleWaitingTimeAverages()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        stats.recordArrivals(5, 2);
        stats.recordCompletion(completed(1, VehicleType.NORMAL, 0, 2));
        stats.recordCompletion(completed(2, VehicleType.EMERGENCY, 0, 8));
        stats.recordCompletion(completed(3, VehicleType.NORMAL, 0, 4));
        stats.recordCompletion(completed(4, VehicleType.EMERGENCY, 0, 12));
        assertEquals(5, stats.getTotalArrived());
        assertEquals(4, stats.getTotalCompleted());
        assertEquals(2, stats.getTotalEmergencyCompleted());
        assertEquals(26L, stats.getTotalWaitingSecondsCompleted());
        assertEquals(20L, stats.getTotalEmergencyWaitingSecondsCompleted());
        assertEquals(6.5, stats.getAverageWaitingTimeSeconds(), 0.000001);
        assertEquals(
            10.0,
            stats.getAverageEmergencyWaitingTimeSeconds(),
            0.000001);
    }


    /**
     * Confirms entering the intersection upon arrival gives zero waiting time.
     */
    @Test
    public void testZeroWaitingTimeCompletion()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        stats.recordArrivals(1, 1);
        stats.recordCompletion(completed(1, VehicleType.EMERGENCY, 20, 20));
        assertEquals(0L, stats.getTotalWaitingSecondsCompleted());
        assertEquals(0.0, stats.getAverageWaitingTimeSeconds(), 0.0);
        assertEquals(0.0, stats.getAverageEmergencyWaitingTimeSeconds(), 0.0);
    }


    /**
     * Confirms large waiting-time totals use long rather than int arithmetic.
     */
    @Test
    public void testWaitingTimesAccumulateInLong()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        stats.recordArrivals(2, 1);
        stats.recordCompletion(
            completed(1, VehicleType.NORMAL, 0, Integer.MAX_VALUE));
        stats.recordCompletion(
            completed(2, VehicleType.EMERGENCY, 0, Integer.MAX_VALUE));
        assertEquals(
            2L * Integer.MAX_VALUE,
            stats.getTotalWaitingSecondsCompleted());
        assertEquals(
            (long)Integer.MAX_VALUE,
            stats.getTotalEmergencyWaitingSecondsCompleted());
        assertEquals(
            (double)Integer.MAX_VALUE,
            stats.getAverageWaitingTimeSeconds(),
            0.0);
    }


    /**
     * Rejects null completed vehicles.
     */
    @Test
    public void testNullCompletionRejected()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        stats.recordArrivals(1, 0);
        assertInvalid(() -> stats.recordCompletion(null));
        assertEquals(0, stats.getTotalCompleted());
    }


    /**
     * Rejects a WAITING vehicle without incrementing completion counters.
     */
    @Test
    public void testWaitingVehicleCompletionRejected()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        stats.recordArrivals(1, 0);
        Vehicle waiting = new Vehicle(
            1,
            Direction.NORTH,
            Turn.STRAIGHT,
            0,
            VehicleType.NORMAL);
        assertIllegalState(() -> stats.recordCompletion(waiting));
        assertEquals(0, stats.getTotalCompleted());
    }


    /**
     * Rejects a CROSSING vehicle that has not yet reached DONE state.
     */
    @Test
    public void testCrossingVehicleCompletionRejected()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        stats.recordArrivals(1, 1);
        Vehicle crossing = new Vehicle(
            1,
            Direction.SOUTH,
            Turn.LEFT,
            0,
            VehicleType.EMERGENCY);
        crossing.startCrossing(5);
        assertIllegalState(() -> stats.recordCompletion(crossing));
        assertEquals(0, stats.getTotalCompleted());
        assertEquals(0, stats.getTotalEmergencyCompleted());
    }


    /**
     * Rejects counting the same completed vehicle more than once.
     */
    @Test
    public void testDuplicateVehicleCompletionRejected()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        stats.recordArrivals(2, 0);
        Vehicle vehicle = completed(1, VehicleType.NORMAL, 0, 3);
        stats.recordCompletion(vehicle);
        assertInvalid(() -> stats.recordCompletion(vehicle));
        assertEquals(1, stats.getTotalCompleted());
        assertEquals(3L, stats.getTotalWaitingSecondsCompleted());
    }


    /**
     * Rejects a second distinct Vehicle object sharing a completed ID.
     */
    @Test
    public void testDuplicateVehicleIdRejected()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        stats.recordArrivals(2, 0);
        stats.recordCompletion(completed(5, VehicleType.NORMAL, 0, 3));
        assertInvalid(
            () -> stats
                .recordCompletion(completed(5, VehicleType.NORMAL, 0, 9)));
        assertEquals(1, stats.getTotalCompleted());
        assertEquals(3L, stats.getTotalWaitingSecondsCompleted());
    }


    /**
     * Rejects completions when no more arrivals have been recorded.
     */
    @Test
    public void testCompletionCannotExceedTotalArrivals()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        assertIllegalState(
            () -> stats
                .recordCompletion(completed(1, VehicleType.NORMAL, 0, 1)));
        stats.recordArrivals(1, 0);
        stats.recordCompletion(completed(1, VehicleType.NORMAL, 0, 1));
        assertIllegalState(
            () -> stats
                .recordCompletion(completed(2, VehicleType.NORMAL, 0, 2)));
        assertEquals(1, stats.getTotalCompleted());
    }


    /**
     * Rejects emergency completions beyond the recorded emergency arrivals.
     */
    @Test
    public void testEmergencyCompletionCannotExceedEmergencyArrivals()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        stats.recordArrivals(2, 1);
        stats.recordCompletion(completed(1, VehicleType.EMERGENCY, 0, 5));
        assertIllegalState(
            () -> stats
                .recordCompletion(completed(2, VehicleType.EMERGENCY, 0, 10)));
        assertEquals(1, stats.getTotalCompleted());
        assertEquals(1, stats.getTotalEmergencyCompleted());
        assertEquals(5L, stats.getTotalWaitingSecondsCompleted());
    }


    /**
     * Rejects ordinary completions when all recorded arrivals are emergency.
     */
    @Test
    public void testNormalCompletionCannotExceedNormalArrivals()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        stats.recordArrivals(2, 2);
        assertIllegalState(
            () -> stats
                .recordCompletion(completed(1, VehicleType.NORMAL, 0, 5)));
        assertEquals(0, stats.getTotalCompleted());
        stats.recordCompletion(completed(2, VehicleType.EMERGENCY, 0, 8));
        assertEquals(1, stats.getTotalCompleted());
    }


    /**
     * Tests a previously rejected completion can succeed after valid arrivals
     * are recorded, demonstrating failure does not consume the vehicle ID.
     */
    @Test
    public void testFailedCompletionDoesNotConsumeVehicleId()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        Vehicle emergency = completed(5, VehicleType.EMERGENCY, 0, 7);
        stats.recordArrivals(1, 0);
        assertIllegalState(() -> stats.recordCompletion(emergency));
        stats.recordArrivals(1, 1);
        stats.recordCompletion(emergency);
        assertEquals(1, stats.getTotalEmergencyCompleted());
        assertEquals(7L, stats.getTotalWaitingSecondsCompleted());
    }


    /**
     * Tracks started accidents and slowdowns independently of one another.
     */
    @Test
    public void testIncidentStartCounts()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        stats.recordIncidentStart(TrafficEventType.ACCIDENT);
        stats.recordIncidentStart(TrafficEventType.SLOWDOWN);
        stats.recordIncidentStart(TrafficEventType.ACCIDENT);
        assertEquals(2, stats.getTotalAccidents());
        assertEquals(1, stats.getTotalSlowdowns());
    }


    /**
     * Rejects null or NONE as a genuine incident without changing counters.
     */
    @Test
    public void testInvalidIncidentTypesRejected()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        assertInvalid(() -> stats.recordIncidentStart(null));
        assertInvalid(() -> stats.recordIncidentStart(TrafficEventType.NONE));
        assertEquals(0, stats.getTotalAccidents());
        assertEquals(0, stats.getTotalSlowdowns());
    }


    /**
     * Verifies incident counts never modify arrival or completion totals.
     */
    @Test
    public void testIncidentsDoNotModifyVehicleStatistics()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        stats.recordArrivals(2, 1);
        stats.recordIncidentStart(TrafficEventType.ACCIDENT);
        stats.recordIncidentStart(TrafficEventType.SLOWDOWN);
        assertEquals(2, stats.getTotalArrived());
        assertEquals(1, stats.getTotalEmergencyArrived());
        assertEquals(0, stats.getTotalCompleted());
        assertNull(stats.getAverageWaitingTimeSeconds());
    }


    /**
     * Records an empty four-lane sample without introducing false queues.
     */
    @Test
    public void testEmptyLanesSampleAsZero()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        stats.sampleQueues(lanes(0, 0, 0, 0));
        assertEquals(1, stats.getQueueSampleCount());
        for (Direction direction : Direction.values())
        {
            assertEquals(0.0, stats.getAverageQueueLength(direction), 0.0);
            assertEquals(0, stats.getMaximumQueueLength(direction));
        }
    }


    /**
     * Verifies averages and maxima across two different queue observations.
     */
    @Test
    public void testQueueAveragesAndMaxima()
    {
        Map<Direction, Lane> four = lanes(1800, 1800, 0, 0);
        generate(four, 8);
        StatisticsAccumulator stats = new StatisticsAccumulator();
        stats.sampleQueues(four);
        assertEquals(4, four.get(Direction.NORTH).getQueueLength());
        assertEquals(4, four.get(Direction.SOUTH).getQueueLength());

        four.get(Direction.NORTH).releaseNext(8, true, 2);
        four.get(Direction.NORTH).releaseNext(10, true, 2);
        four.get(Direction.SOUTH).releaseNext(8, true, 2);
        stats.sampleQueues(four);

        assertEquals(2, stats.getQueueSampleCount());
        assertEquals(3.0, stats.getAverageQueueLength(Direction.NORTH), 0.0);
        assertEquals(3.5, stats.getAverageQueueLength(Direction.SOUTH), 0.0);
        assertEquals(0.0, stats.getAverageQueueLength(Direction.EAST), 0.0);
        assertEquals(0.0, stats.getAverageQueueLength(Direction.WEST), 0.0);
        assertEquals(4, stats.getMaximumQueueLength(Direction.NORTH));
        assertEquals(4, stats.getMaximumQueueLength(Direction.SOUTH));
        assertEquals(0, stats.getMaximumQueueLength(Direction.EAST));
        assertEquals(0, stats.getMaximumQueueLength(Direction.WEST));
    }


    /**
     * Confirms queue sampling excludes vehicles already released to CROSSING.
     */
    @Test
    public void testQueueSamplesExcludeCrossingVehicles()
    {
        Map<Direction, Lane> four = lanes(1800, 0, 0, 0);
        generate(four, 4);
        assertEquals(2, four.get(Direction.NORTH).getQueueLength());
        assertEquals(
            VehicleState.CROSSING,
            four.get(Direction.NORTH).releaseNext(4, true, 2).getState());
        StatisticsAccumulator stats = new StatisticsAccumulator();
        stats.sampleQueues(four);
        assertEquals(1.0, stats.getAverageQueueLength(Direction.NORTH), 0.0);
        assertEquals(1, stats.getMaximumQueueLength(Direction.NORTH));
    }


    /**
     * Rejects a null map of lanes.
     */
    @Test
    public void testNullQueueSampleMapRejected()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        assertInvalid(() -> stats.sampleQueues(null));
        assertEquals(0, stats.getQueueSampleCount());
    }


    /**
     * Rejects an incomplete lane map.
     */
    @Test
    public void testMissingLaneRejected()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        Map<Direction, Lane> four = lanes(0, 0, 0, 0);
        four.remove(Direction.WEST);
        assertInvalid(() -> stats.sampleQueues(four));
        assertEquals(0, stats.getQueueSampleCount());
    }


    /**
     * Rejects a map with a null lane value.
     */
    @Test
    public void testNullLaneRejected()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        Map<Direction, Lane> four = lanes(0, 0, 0, 0);
        four.put(Direction.EAST, null);
        assertInvalid(() -> stats.sampleQueues(four));
        assertEquals(0, stats.getQueueSampleCount());
    }


    /**
     * Rejects a map containing a lane keyed under the wrong direction.
     */
    @Test
    public void testMismatchedLaneRejected()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        Map<Direction, Lane> four = lanes(0, 0, 0, 0);
        four.put(Direction.NORTH, four.get(Direction.SOUTH));
        assertInvalid(() -> stats.sampleQueues(four));
        assertEquals(0, stats.getQueueSampleCount());
    }


    /**
     * Rejects an extra null map key even if all four directions are present.
     */
    @Test
    public void testExtraLaneKeyRejected()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        Map<Direction, Lane> extra = new HashMap<>(lanes(0, 0, 0, 0));
        extra.put(null, extra.get(Direction.NORTH));
        assertInvalid(() -> stats.sampleQueues(extra));
        assertEquals(0, stats.getQueueSampleCount());
    }


    /**
     * Rejects malformed samples without changing previously accumulated data.
     */
    @Test
    public void testInvalidSampleDoesNotCorruptPriorStatistics()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        Map<Direction, Lane> four = lanes(1800, 0, 0, 0);
        generate(four, 4);
        stats.sampleQueues(four);
        four.put(Direction.WEST, four.get(Direction.NORTH));
        assertInvalid(() -> stats.sampleQueues(four));
        assertEquals(1, stats.getQueueSampleCount());
        assertEquals(2.0, stats.getAverageQueueLength(Direction.NORTH), 0.0);
        assertEquals(2, stats.getMaximumQueueLength(Direction.NORTH));
    }


    /**
     * Rejects null directions for both queue statistics getters.
     */
    @Test
    public void testNullDirectionQueriesRejected()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        assertInvalid(() -> stats.getAverageQueueLength(null));
        assertInvalid(() -> stats.getMaximumQueueLength(null));
    }


    /**
     * Verifies queue samples and vehicle counts are independently collected.
     */
    @Test
    public void testQueueSamplingDoesNotChangeVehicleTotals()
    {
        StatisticsAccumulator stats = new StatisticsAccumulator();
        stats.recordArrivals(5, 2);
        stats.sampleQueues(lanes(0, 0, 0, 0));
        assertEquals(5, stats.getTotalArrived());
        assertEquals(2, stats.getTotalEmergencyArrived());
        assertEquals(0, stats.getTotalCompleted());
    }
}
