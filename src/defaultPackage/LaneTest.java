
package defaultPackage;

import static org.junit.Assert.*;

import java.util.Random;
import org.junit.Test;

/**
 * JUnit 4 tests for V2 Lane arrival generation, randomized turning and
 * emergency classification, FIFO behavior, headway, and invalid inputs.
 */
public class LaneTest
{

    /**
     * A predictable random-number source that returns scripted draws. Each
     * value must already be in the range requested by nextInt().
     */
    private static class ScriptedRandom
        extends Random
    {
        private static final long serialVersionUID = 1L;
        private final int[] draws;
        private int index;

        /**
         * Creates a random source with a fixed sequence of integer draws.
         *
         * @param draws
         *            values to return in order
         */
        ScriptedRandom(int... draws)
        {
            this.draws = draws.clone();
            index = 0;
        }


        /**
         * Returns the next scripted random value.
         *
         * @param bound
         *            exclusive upper bound
         * @return next scripted draw
         */
        @Override
        public int nextInt(int bound)
        {
            if (index >= draws.length)
            {
                throw new AssertionError("Unexpected random draw.");
            }
            int draw = draws[index++];
            if (draw < 0 || draw >= bound)
            {
                throw new AssertionError(
                    "Scripted draw outside requested range.");
            }
            return draw;
        }
    }

    /**
     * Builds a lane whose random-number generators have fixed seeds.
     *
     * @param rate
     *            vehicle arrival rate per hour
     * @param left
     *            left-turn percentage
     * @param uTurn
     *            U-turn percentage
     * @param emergency
     *            emergency percentage
     * @return independently seeded northbound-approach lane
     */
    private Lane makeLane(int rate, int left, int uTurn, int emergency)
    {
        return new Lane(
            Direction.NORTH,
            rate,
            left,
            uTurn,
            emergency,
            new Random(1234L),
            new Random(5678L));
    }


    /**
     * Generates arrivals through consecutive one-second steps.
     *
     * @param lane
     *            lane to advance
     * @param seconds
     *            number of steps, starting at zero
     * @return total number of vehicles generated
     */
    private int generateForSeconds(Lane lane, int seconds)
    {
        int total = 0;
        long nextId = 1L;
        for (int second = 0; second < seconds; second++)
        {
            int created = lane.generateArrivals(second, nextId);
            total += created;
            nextId += created;
        }
        return total;
    }


    /**
     * Checks for rejection of an invalid argument.
     *
     * @param action
     *            action expected to throw IllegalArgumentException
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
            // Invalid input was correctly rejected.
        }
    }


    /**
     * Tests a valid empty lane and all basic accessors.
     */
    @Test
    public void testConstructorAndInitialGetters()
    {
        Lane lane = makeLane(600, 20, 10, 5);
        assertEquals(Direction.NORTH, lane.getApproach());
        assertEquals(600, lane.getArrivalRatePerHour());
        assertEquals(0, lane.getQueueLength());
        assertEquals(0, lane.getEmergencyWaitingCount());
        assertEquals(0, lane.getTotalEmergencyGenerated());
        assertEquals(2, Lane.DEPARTURE_HEADWAY_SECONDS);
    }


    /**
     * Tests rejected null direction and out-of-range arrival rates.
     */
    @Test
    public void testInvalidDirectionAndArrivalRates()
    {
        assertInvalid(
            () -> new Lane(null, 600, 20, 10, 5, new Random(1), new Random(2)));
        assertInvalid(() -> makeLane(-1, 20, 10, 5));
        assertInvalid(() -> makeLane(1801, 20, 10, 5));
        assertEquals(0, makeLane(0, 20, 10, 5).getArrivalRatePerHour());
        assertEquals(1800, makeLane(1800, 20, 10, 5).getArrivalRatePerHour());
    }


    /**
     * Tests negative, over-100, and over-total turning probabilities.
     */
    @Test
    public void testInvalidTurnProbabilities()
    {
        assertInvalid(() -> makeLane(600, -1, 10, 5));
        assertInvalid(() -> makeLane(600, 101, 0, 5));
        assertInvalid(() -> makeLane(600, 20, -1, 5));
        assertInvalid(() -> makeLane(600, 0, 101, 5));
        assertInvalid(() -> makeLane(600, 80, 21, 5));
        assertInvalid(() -> makeLane(600, 100, 1, 5));
    }


    /**
     * Tests emergency probability bounds and required random sources.
     */
    @Test
    public void testInvalidEmergencyProbabilityAndRandomSources()
    {
        assertInvalid(() -> makeLane(600, 20, 10, -1));
        assertInvalid(() -> makeLane(600, 20, 10, 101));
        assertInvalid(
            () -> new Lane(
                Direction.NORTH,
                600,
                20,
                10,
                5,
                null,
                new Random(2)));
        assertInvalid(
            () -> new Lane(
                Direction.NORTH,
                600,
                20,
                10,
                5,
                new Random(1),
                null));
    }


    /**
     * Tests that zero-rate lanes do not generate cars or draw randomness.
     */
    @Test
    public void testZeroTraffic()
    {
        Lane lane = new Lane(
            Direction.EAST,
            0,
            20,
            10,
            5,
            new ScriptedRandom(),
            new ScriptedRandom());
        assertEquals(0, generateForSeconds(lane, 120));
        assertEquals(0, lane.getQueueLength());
        assertEquals(0, lane.getEmergencyWaitingCount());
        assertNull(lane.releaseNext(120, true, 2));
    }


    /**
     * Tests exact arrival count, order, IDs and times at 600 vehicles/hour.
     */
    @Test
    public void testArrivalsAndFIFOAtSixHundred()
    {
        Lane lane = makeLane(600, 0, 0, 0);
        assertEquals(10, generateForSeconds(lane, 60));
        assertEquals(10, lane.getQueueLength());
        for (int i = 0; i < 10; i++)
        {
            Vehicle vehicle = lane.releaseNext(60 + 2 * i, true, 2);
            assertNotNull(vehicle);
            assertEquals(i + 1L, vehicle.getId());
            assertEquals(5 + 6 * i, vehicle.getArrivalSecond());
            assertEquals(Direction.NORTH, vehicle.getApproach());
            assertEquals(Turn.STRAIGHT, vehicle.getTurn());
            assertEquals(VehicleType.NORMAL, vehicle.getVehicleType());
            assertEquals(VehicleState.CROSSING, vehicle.getState());
        }
        assertEquals(0, lane.getQueueLength());
    }


    /**
     * Tests arrivals every other step at the maximum configured rate.
     */
    @Test
    public void testMaximumArrivalRateTiming()
    {
        Lane lane = makeLane(1800, 20, 10, 0);
        assertEquals(0, lane.generateArrivals(0, 1));
        assertEquals(1, lane.generateArrivals(1, 1));
        assertEquals(0, lane.generateArrivals(2, 2));
        assertEquals(1, lane.generateArrivals(3, 2));
        assertEquals(2, lane.getQueueLength());
    }


    /**
     * Tests that skipped, repeated, and negative steps are rejected without
     * advancing arrival accounting.
     */
    @Test
    public void testInvalidGenerationOrderDoesNotMutate()
    {
        Lane lane = makeLane(1800, 20, 10, 5);
        assertInvalid(() -> lane.generateArrivals(-1, 1));
        assertInvalid(() -> lane.generateArrivals(1, 1));
        assertEquals(0, lane.generateArrivals(0, 1));
        assertInvalid(() -> lane.generateArrivals(0, 1));
        assertInvalid(() -> lane.generateArrivals(2, 1));
        assertEquals(1, lane.generateArrivals(1, 1));
        assertEquals(1, lane.getQueueLength());
    }


    /**
     * Tests that invalid IDs are rejected without advancing the clock.
     */
    @Test
    public void testInvalidGenerationIdDoesNotMutate()
    {
        Lane lane = makeLane(1800, 0, 0, 0);
        assertInvalid(() -> lane.generateArrivals(0, 0));
        assertInvalid(() -> lane.generateArrivals(0, -1));
        assertEquals(0, lane.generateArrivals(0, 1));
        assertInvalid(() -> lane.generateArrivals(1, 0));
        assertEquals(1, lane.generateArrivals(1, 1));
        assertEquals(1, lane.getQueueLength());
    }


    /**
     * Tests the largest valid positive vehicle ID can be generated.
     */
    @Test
    public void testMaximumVehicleIdAccepted()
    {
        Lane lane = makeLane(1800, 0, 0, 0);
        lane.generateArrivals(0, Long.MAX_VALUE);
        assertEquals(1, lane.generateArrivals(1, Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE, lane.releaseNext(1, true, 2).getId());
    }


    /**
     * Tests exact left/U-turn/straight selection at probability boundaries.
     */
    @Test
    public void testScriptedTurnDrawBoundaries()
    {
        Lane lane = new Lane(
            Direction.SOUTH,
            1800,
            20,
            10,
            0,
            new ScriptedRandom(0, 19, 20, 29, 30, 99),
            new ScriptedRandom(99, 99, 99, 99, 99, 99));
        assertEquals(6, generateForSeconds(lane, 12));
        Turn[] expected = { Turn.LEFT, Turn.LEFT, Turn.U_TURN, Turn.U_TURN,
            Turn.STRAIGHT, Turn.STRAIGHT };
        for (int i = 0; i < expected.length; i++)
        {
            assertEquals(
                expected[i],
                lane.releaseNext(12 + 2 * i, true, 2).getTurn());
        }
    }


    /**
     * Tests 0% turning and 100% left/U-turn extremes.
     */
    @Test
    public void testTurnProbabilityExtremes()
    {
        Lane straight = makeLane(1800, 0, 0, 0);
        Lane left = makeLane(1800, 100, 0, 0);
        Lane uTurn = makeLane(1800, 0, 100, 0);
        generateForSeconds(straight, 20);
        generateForSeconds(left, 20);
        generateForSeconds(uTurn, 20);
        for (int i = 0; i < 10; i++)
        {
            int second = 20 + 2 * i;
            assertEquals(
                Turn.STRAIGHT,
                straight.releaseNext(second, true, 2).getTurn());
            assertEquals(
                Turn.LEFT,
                left.releaseNext(second, true, 2).getTurn());
            assertEquals(
                Turn.U_TURN,
                uTurn.releaseNext(second, true, 2).getTurn());
        }
    }


    /**
     * Tests reproducibility of full turn and type sequences with fixed seeds.
     */
    @Test
    public void testSeededRunsAreReproducible()
    {
        Lane first = makeLane(1800, 20, 10, 15);
        Lane second = makeLane(1800, 20, 10, 15);
        assertEquals(100, generateForSeconds(first, 200));
        assertEquals(100, generateForSeconds(second, 200));
        for (int i = 0; i < 100; i++)
        {
            Vehicle a = first.releaseNext(200 + 2 * i, true, 2);
            Vehicle b = second.releaseNext(200 + 2 * i, true, 2);
            assertEquals(a.getId(), b.getId());
            assertEquals(a.getTurn(), b.getTurn());
            assertEquals(a.getVehicleType(), b.getVehicleType());
        }
        assertEquals(
            first.getTotalEmergencyGenerated(),
            second.getTotalEmergencyGenerated());
    }


    /**
     * Tests that separate random streams keep turn draws independent from
     * emergency classification and vice versa.
     */
    @Test
    public void testIndependentRandomStreams()
    {
        Lane a = new Lane(
            Direction.NORTH,
            1800,
            20,
            10,
            0,
            new Random(42),
            new Random(123));
        Lane b = new Lane(
            Direction.NORTH,
            1800,
            20,
            10,
            100,
            new Random(42),
            new Random(999));
        Lane c = new Lane(
            Direction.NORTH,
            1800,
            100,
            0,
            50,
            new Random(13),
            new Random(123));
        Lane d = new Lane(
            Direction.NORTH,
            1800,
            0,
            100,
            50,
            new Random(99),
            new Random(123));
        generateForSeconds(a, 120);
        generateForSeconds(b, 120);
        generateForSeconds(c, 120);
        generateForSeconds(d, 120);
        for (int i = 0; i < 60; i++)
        {
            int second = 120 + 2 * i;
            assertEquals(
                a.releaseNext(second, true, 2).getTurn(),
                b.releaseNext(second, true, 2).getTurn());
            assertEquals(
                c.releaseNext(second, true, 2).getVehicleType(),
                d.releaseNext(second, true, 2).getVehicleType());
        }
    }


    /**
     * Tests that different turn seeds produce distinguishable sequences.
     */
    @Test
    public void testDifferentTurnSeedsCanChangeSequence()
    {
        Lane a = new Lane(
            Direction.WEST,
            1800,
            30,
            20,
            0,
            new Random(1),
            new Random(8));
        Lane b = new Lane(
            Direction.WEST,
            1800,
            30,
            20,
            0,
            new Random(2),
            new Random(8));
        generateForSeconds(a, 120);
        generateForSeconds(b, 120);
        boolean different = false;
        for (int i = 0; i < 60; i++)
        {
            int second = 120 + 2 * i;
            if (a.releaseNext(second, true, 2).getTurn() != b
                .releaseNext(second, true, 2).getTurn())
            {
                different = true;
            }
        }
        assertTrue("Different seeds should be able to change turns", different);
    }


    /**
     * Tests exact 0% and 100% emergency vehicle probabilities.
     */
    @Test
    public void testEmergencyProbabilityExtremes()
    {
        Lane none = makeLane(1800, 20, 10, 0);
        Lane all = makeLane(1800, 20, 10, 100);
        assertEquals(10, generateForSeconds(none, 20));
        assertEquals(10, generateForSeconds(all, 20));
        assertEquals(0, none.getEmergencyWaitingCount());
        assertEquals(0, none.getTotalEmergencyGenerated());
        assertEquals(10, all.getEmergencyWaitingCount());
        assertEquals(10, all.getTotalEmergencyGenerated());
        for (int i = 0; i < 10; i++)
        {
            int second = 20 + 2 * i;
            assertEquals(
                VehicleType.NORMAL,
                none.releaseNext(second, true, 2).getVehicleType());
            assertEquals(
                VehicleType.EMERGENCY,
                all.releaseNext(second, true, 2).getVehicleType());
        }
        assertEquals(0, all.getEmergencyWaitingCount());
        assertEquals(10, all.getTotalEmergencyGenerated());
    }


    /**
     * Tests emergency count changes only when emergency cars are released.
     */
    @Test
    public void testEmergencyWaitingCountTracksFIFOReleases()
    {
        Lane lane = new Lane(
            Direction.EAST,
            1800,
            0,
            0,
            50,
            new ScriptedRandom(0, 0, 0, 0),
            new ScriptedRandom(0, 99, 49, 50));
        generateForSeconds(lane, 8);
        assertEquals(4, lane.getQueueLength());
        assertEquals(2, lane.getEmergencyWaitingCount());
        assertEquals(2, lane.getTotalEmergencyGenerated());
        assertEquals(
            VehicleType.EMERGENCY,
            lane.releaseNext(8, true, 2).getVehicleType());
        assertEquals(1, lane.getEmergencyWaitingCount());
        assertEquals(
            VehicleType.NORMAL,
            lane.releaseNext(10, true, 2).getVehicleType());
        assertEquals(1, lane.getEmergencyWaitingCount());
        assertEquals(
            VehicleType.EMERGENCY,
            lane.releaseNext(12, true, 2).getVehicleType());
        assertEquals(0, lane.getEmergencyWaitingCount());
        lane.releaseNext(14, true, 2);
        assertEquals(0, lane.getQueueLength());
        assertEquals(2, lane.getTotalEmergencyGenerated());
    }


    /**
     * Tests a denied release preserves the queue and emergency count.
     */
    @Test
    public void testBlockedReleasePreservesQueue()
    {
        Lane lane = makeLane(1800, 0, 0, 100);
        generateForSeconds(lane, 2);
        assertNull(lane.releaseNext(1, false, 2));
        assertEquals(1, lane.getQueueLength());
        assertEquals(1, lane.getEmergencyWaitingCount());
        assertEquals(1L, lane.releaseNext(1, true, 2).getId());
        assertEquals(0, lane.getEmergencyWaitingCount());
    }


    /**
     * Tests normal two-second headway and no reordering of queued cars.
     */
    @Test
    public void testNormalHeadway()
    {
        Lane lane = makeLane(1800, 0, 0, 0);
        generateForSeconds(lane, 6);
        assertEquals(1L, lane.releaseNext(6, true, 2).getId());
        assertNull(lane.releaseNext(7, true, 2));
        assertEquals(2L, lane.releaseNext(8, true, 2).getId());
        assertNull(lane.releaseNext(9, true, 2));
        assertEquals(3L, lane.releaseNext(10, true, 2).getId());
        assertEquals(0, lane.getQueueLength());
    }


    /**
     * Tests that changing headway affects the next release immediately,
     * including the transition back to normal after a slowdown.
     */
    @Test
    public void testDynamicHeadwayAndRecovery()
    {
        Lane lane = makeLane(1800, 0, 0, 0);
        generateForSeconds(lane, 12);
        assertEquals(1L, lane.releaseNext(12, true, 2).getId());
        assertNull(lane.releaseNext(13, true, 4));
        assertNull(lane.releaseNext(15, true, 4));
        assertEquals(2L, lane.releaseNext(16, true, 4).getId());
        assertNull(lane.releaseNext(17, true, 2));
        assertEquals(3L, lane.releaseNext(18, true, 2).getId());
        assertNull(lane.releaseNext(19, true, 4));
        assertEquals(4L, lane.releaseNext(22, true, 4).getId());
    }


    /**
     * Tests invalid headway and negative time are rejected without mutation.
     */
    @Test
    public void testInvalidReleaseArguments()
    {
        Lane lane = makeLane(1800, 0, 0, 100);
        generateForSeconds(lane, 2);
        assertInvalid(() -> lane.releaseNext(-1, true, 2));
        assertInvalid(() -> lane.releaseNext(1, true, 1));
        assertInvalid(() -> lane.releaseNext(1, true, 21));
        assertInvalid(() -> lane.releaseNext(1, false, 0));
        assertEquals(1, lane.getQueueLength());
        assertEquals(1, lane.getEmergencyWaitingCount());
        assertNotNull(lane.releaseNext(1, true, 2));
        assertEquals(0, lane.getEmergencyWaitingCount());
    }


    /**
     * Tests invalid headway is rejected even for an empty lane.
     */
    @Test
    public void testInvalidHeadwayWithEmptyQueue()
    {
        Lane lane = makeLane(0, 0, 0, 0);
        assertInvalid(() -> lane.releaseNext(0, true, 1));
        assertInvalid(() -> lane.releaseNext(0, false, 21));
        assertNull(lane.releaseNext(0, true, 2));
    }


    /**
     * Tests a queued car cannot be released before its recorded arrival.
     */
    @Test
    public void testReleaseBeforeArrivalRejected()
    {
        Lane lane = makeLane(1800, 0, 0, 100);
        generateForSeconds(lane, 2);
        assertInvalid(() -> lane.releaseNext(0, true, 2));
        assertEquals(1, lane.getQueueLength());
        assertEquals(1, lane.getEmergencyWaitingCount());
        assertEquals(1L, lane.releaseNext(1, true, 2).getId());
    }


    /**
     * Tests releases from an empty queue do not create or lose cars.
     */
    @Test
    public void testReleaseFromEmptyQueue()
    {
        Lane lane = makeLane(600, 20, 10, 5);
        assertNull(lane.releaseNext(0, true, 2));
        assertNull(lane.releaseNext(0, false, 4));
        assertEquals(0, lane.getQueueLength());
        assertEquals(0, lane.getEmergencyWaitingCount());
    }


    /**
     * Tests that two distinct lanes maintain their own queues and IDs.
     */
    @Test
    public void testIndependentApproachQueues()
    {
        Lane north = makeLane(1800, 0, 0, 0);
        Lane east = new Lane(
            Direction.EAST,
            1800,
            0,
            0,
            100,
            new Random(3),
            new Random(4));
        north.generateArrivals(0, 1);
        east.generateArrivals(0, 1);
        north.generateArrivals(1, 10);
        east.generateArrivals(1, 20);
        assertEquals(1, north.getQueueLength());
        assertEquals(1, east.getQueueLength());
        assertEquals(10L, north.releaseNext(1, true, 2).getId());
        Vehicle other = east.releaseNext(1, true, 2);
        assertEquals(20L, other.getId());
        assertEquals(Direction.EAST, other.getApproach());
        assertEquals(VehicleType.EMERGENCY, other.getVehicleType());
        assertEquals(0, north.getQueueLength());
        assertEquals(0, east.getEmergencyWaitingCount());
    }


    /**
     * Tests headway checks at Integer.MAX_VALUE without arithmetic overflow.
     */
    @Test
    public void testLargeReleaseTimeDoesNotOverflow()
    {
        Lane lane = makeLane(1800, 0, 0, 0);
        generateForSeconds(lane, 4);
        assertEquals(
            1L,
            lane.releaseNext(Integer.MAX_VALUE - 1, true, 2).getId());
        assertNull(lane.releaseNext(Integer.MAX_VALUE, true, 2));
        assertEquals(1, lane.getQueueLength());
    }
}
