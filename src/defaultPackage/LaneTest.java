package defaultPackage;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * JUnit 4 tests for the Lane class. Tests deterministic arrivals, FIFO queue
 * behavior, turning assignments, departures, and invalid input.
 */
public class LaneTest
{

    /**
     * Generates arrivals for the specified number of simulation seconds.
     *
     * @param lane
     *            lane to generate vehicles in
     * @param seconds
     *            number of steps to simulate
     * @return total number of generated vehicles
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
     * Verifies that an operation rejects invalid input.
     *
     * @param operation
     *            operation expected to fail
     */
    private void assertInvalid(Runnable operation)
    {
        try
        {
            operation.run();
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected)
        {
            // Expected exception.
        }
    }


    /**
     * Tests construction of a valid, initially empty lane and retrieval of its
     * fixed properties.
     */
    @Test
    public void testConstructorAndGetters()
    {
        Lane lane = new Lane(Direction.NORTH, 600, 20, 10);

        assertEquals(Direction.NORTH, lane.getApproach());

        assertEquals(600, lane.getArrivalRatePerHour());

        assertEquals(0, lane.getQueueLength());

        assertEquals(2, Lane.DEPARTURE_HEADWAY_SECONDS);
    }


    /**
     * Tests rejection of invalid lane directions, arrival rates, and turning
     * percentages.
     */
    @Test
    public void testInvalidConstructorArguments()
    {
        assertInvalid(() -> new Lane(null, 600, 20, 10));

        assertInvalid(() -> new Lane(Direction.NORTH, -1, 20, 10));

        assertInvalid(() -> new Lane(Direction.NORTH, 1801, 20, 10));

        assertInvalid(() -> new Lane(Direction.NORTH, 600, -1, 10));

        assertInvalid(() -> new Lane(Direction.NORTH, 600, 20, -1));

        assertInvalid(() -> new Lane(Direction.NORTH, 600, 101, 0));

        assertInvalid(() -> new Lane(Direction.NORTH, 600, 0, 101));

        assertInvalid(() -> new Lane(Direction.NORTH, 600, 80, 30));
    }


    /**
     * Tests deterministic generation of ten vehicles over sixty steps at 600
     * vehicles/hour. Also verifies FIFO removal, vehicle IDs, arrival times,
     * direction, and movement state.
     */
    @Test
    public void testDeterministicArrivalsAndFIFO()
    {
        Lane lane = new Lane(Direction.NORTH, 600, 0, 0);

        assertEquals(10, generateForSeconds(lane, 60));

        assertEquals(10, lane.getQueueLength());

        for (int i = 0; i < 10; i++)
        {
            Vehicle vehicle = lane.releaseNext(60 + 2 * i, true);

            assertNotNull(vehicle);

            assertEquals(i + 1L, vehicle.getId());

            assertEquals(Direction.NORTH, vehicle.getApproach());

            assertEquals(5 + 6 * i, vehicle.getArrivalSecond());

            assertEquals(Turn.STRAIGHT, vehicle.getTurn());

            assertEquals(VehicleState.CROSSING, vehicle.getState());
        }

        assertEquals(0, lane.getQueueLength());
    }


    /**
     * Tests that the maximum supported arrival rate generates one vehicle every
     * two seconds.
     */
    @Test
    public void testMaximumArrivalRate()
    {
        Lane lane = new Lane(Direction.EAST, 1800, 0, 0);

        assertEquals(0, lane.generateArrivals(0, 1));
        assertEquals(1, lane.generateArrivals(1, 1));
        assertEquals(0, lane.generateArrivals(2, 2));
        assertEquals(1, lane.generateArrivals(3, 2));

        assertEquals(2, lane.getQueueLength());
    }


    /**
     * Tests that a lane with zero arrival rate never generates vehicles.
     */
    @Test
    public void testZeroTraffic()
    {
        Lane lane = new Lane(Direction.SOUTH, 0, 20, 10);

        assertEquals(0, generateForSeconds(lane, 60));

        assertEquals(0, lane.getQueueLength());
        assertNull(lane.releaseNext(60, true));
    }


    /**
     * Tests that a red signal prevents vehicle release without modifying the
     * queue.
     */
    @Test
    public void testRedSignalBlocksRelease()
    {
        Lane lane = new Lane(Direction.WEST, 1800, 0, 0);

        generateForSeconds(lane, 2);

        assertNull(lane.releaseNext(1, false));
        assertEquals(1, lane.getQueueLength());

        Vehicle released = lane.releaseNext(1, true);

        assertNotNull(released);
        assertEquals(0, released.getWaitingTimeSeconds());
        assertEquals(0, lane.getQueueLength());
    }


    /**
     * Tests enforcement of the minimum two-second departure headway between
     * vehicles.
     */
    @Test
    public void testReleaseHeadway()
    {
        Lane lane = new Lane(Direction.NORTH, 1800, 0, 0);

        generateForSeconds(lane, 4);

        Vehicle first = lane.releaseNext(4, true);
        assertEquals(1L, first.getId());

        assertNull(lane.releaseNext(5, true));
        assertEquals(1, lane.getQueueLength());

        Vehicle second = lane.releaseNext(6, true);

        assertEquals(2L, second.getId());
        assertEquals(3, second.getWaitingTimeSeconds());
        assertEquals(0, lane.getQueueLength());
    }


    /**
     * Tests that releasing from an empty lane returns null without an
     * exception.
     */
    @Test
    public void testReleaseFromEmptyQueue()
    {
        Lane lane = new Lane(Direction.NORTH, 600, 0, 0);

        assertNull(lane.releaseNext(0, true));
        assertEquals(0, lane.getQueueLength());
    }


    /**
     * Tests that skipped, repeated, and negative arrival steps are rejected.
     * Failed calls must not change the arrival clock.
     */
    @Test
    public void testInvalidGenerationOrder()
    {
        Lane lane = new Lane(Direction.NORTH, 1800, 0, 0);

        assertInvalid(() -> lane.generateArrivals(-1, 1));

        assertInvalid(() -> lane.generateArrivals(1, 1));

        assertEquals(0, lane.generateArrivals(0, 1));

        assertInvalid(() -> lane.generateArrivals(0, 1));

        assertInvalid(() -> lane.generateArrivals(2, 1));

        assertEquals(1, lane.generateArrivals(1, 1));
        assertEquals(1, lane.getQueueLength());
    }


    /**
     * Tests that invalid vehicle IDs are rejected without changing queue or
     * timing state.
     */
    @Test
    public void testInvalidGenerationId()
    {
        Lane lane = new Lane(Direction.EAST, 1800, 0, 0);

        assertInvalid(() -> lane.generateArrivals(0, 0));

        assertInvalid(() -> lane.generateArrivals(0, -1));

        assertEquals(0, lane.getQueueLength());

        assertEquals(0, lane.generateArrivals(0, 1));
        assertEquals(1, lane.generateArrivals(1, 1));

        assertEquals(1, lane.getQueueLength());
    }


    /**
     * Tests rejection of negative release times without losing queued vehicles.
     */
    @Test
    public void testNegativeReleaseTime()
    {
        Lane lane = new Lane(Direction.EAST, 1800, 0, 0);

        generateForSeconds(lane, 2);

        assertInvalid(() -> lane.releaseNext(-1, true));

        assertEquals(1, lane.getQueueLength());

        assertEquals(1L, lane.releaseNext(1, true).getId());
    }


    /**
     * Tests that a vehicle cannot leave the queue before its recorded arrival
     * time.
     */
    @Test
    public void testReleaseBeforeArrival()
    {
        Lane lane = new Lane(Direction.EAST, 1800, 0, 0);

        generateForSeconds(lane, 2);

        assertInvalid(() -> lane.releaseNext(0, true));

        assertEquals(1, lane.getQueueLength());

        assertEquals(1L, lane.releaseNext(1, true).getId());
    }


    /**
     * Tests that 100 generated vehicles are assigned turning movements
     * according to their configured deterministic percentages.
     */
    @Test
    public void testDeterministicTurnDistribution()
    {
        Lane lane = new Lane(Direction.NORTH, 1800, 20, 10);

        assertEquals(100, generateForSeconds(lane, 200));

        int straight = 0;
        int left = 0;
        int uTurn = 0;

        for (int i = 0; i < 100; i++)
        {
            Vehicle vehicle = lane.releaseNext(200 + 2 * i, true);

            assertNotNull(vehicle);

            if (vehicle.getTurn() == Turn.STRAIGHT)
            {
                straight++;
            }
            else if (vehicle.getTurn() == Turn.LEFT)
            {
                left++;
            }
            else if (vehicle.getTurn() == Turn.U_TURN)
            {
                uTurn++;
            }
        }

        assertEquals(70, straight);
        assertEquals(20, left);
        assertEquals(10, uTurn);
        assertEquals(0, lane.getQueueLength());
    }


    /**
     * Tests a boundary configuration where every incoming vehicle makes a left
     * turn.
     */
    @Test
    public void testAllLeftTurns()
    {
        Lane lane = new Lane(Direction.SOUTH, 1800, 100, 0);

        generateForSeconds(lane, 2);

        assertEquals(Turn.LEFT, lane.releaseNext(1, true).getTurn());
    }


    /**
     * Tests that separate lanes maintain independent queues, directions, and
     * vehicle identities.
     */
    @Test
    public void testIndependentLaneQueues()
    {
        Lane north = new Lane(Direction.NORTH, 1800, 0, 0);

        Lane south = new Lane(Direction.SOUTH, 1800, 0, 0);

        north.generateArrivals(0, 1);
        south.generateArrivals(0, 1);

        north.generateArrivals(1, 1);
        south.generateArrivals(1, 2);

        Vehicle northVehicle = north.releaseNext(1, true);

        assertEquals(Direction.NORTH, northVehicle.getApproach());

        assertEquals(1L, northVehicle.getId());
        assertEquals(0, north.getQueueLength());
        assertEquals(1, south.getQueueLength());

        assertEquals(2L, south.releaseNext(1, true).getId());
    }
}
