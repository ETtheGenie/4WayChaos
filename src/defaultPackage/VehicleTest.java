package defaultPackage;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * JUnit 4 tests for the Vehicle class. Verifies vehicle construction, state
 * transitions, movement tracking, waiting time, and invalid inputs.
 */
public class VehicleTest
{

    /**
     * Tests that the constructor correctly initializes all vehicle fields and
     * the initial WAITING state.
     */
    @Test
    public void testConstructor()
    {
        Vehicle car = new Vehicle(1, Direction.NORTH, Turn.LEFT, 5);

        assertEquals(1L, car.getId());
        assertEquals(Direction.NORTH, car.getApproach());
        assertEquals(Turn.LEFT, car.getTurn());
        assertEquals(5, car.getArrivalSecond());
        assertEquals(-1, car.getCrossingStartSecond());
        assertEquals(0, car.getProgressMeters());
        assertEquals(VehicleState.WAITING, car.getState());
    }


    /**
     * Tests that a vehicle correctly transitions from WAITING to CROSSING.
     */
    @Test
    public void testStartCrossing()
    {
        Vehicle car = new Vehicle(1, Direction.SOUTH, Turn.STRAIGHT, 5);

        car.startCrossing(12);

        assertEquals(VehicleState.CROSSING, car.getState());
        assertEquals(12, car.getCrossingStartSecond());
    }


    /**
     * Tests that movement increases crossing progress by the expected distance.
     */
    @Test
    public void testAdvance()
    {
        Vehicle car = new Vehicle(1, Direction.EAST, Turn.STRAIGHT, 0);

        car.startCrossing(0);
        car.advance(10);
        car.advance(10);

        assertEquals(20, car.getProgressMeters());
        assertEquals(VehicleState.CROSSING, car.getState());
    }


    /**
     * Tests the transition from CROSSING to DONE.
     */
    @Test
    public void testFinishCrossing()
    {
        Vehicle car = new Vehicle(1, Direction.WEST, Turn.U_TURN, 0);

        car.startCrossing(5);
        car.advance(20);
        car.finishCrossing();

        assertEquals(VehicleState.DONE, car.getState());
        assertEquals(20, car.getProgressMeters());
    }


    /**
     * Tests calculation of vehicle queue waiting time.
     */
    @Test
    public void testWaitingTime()
    {
        Vehicle car = new Vehicle(1, Direction.NORTH, Turn.LEFT, 5);

        car.startCrossing(12);

        assertEquals(7, car.getWaitingTimeSeconds());
    }


    /**
     * Tests that zero waiting time is valid when a vehicle begins crossing
     * immediately upon arrival.
     */
    @Test
    public void testZeroWaitingTime()
    {
        Vehicle car = new Vehicle(1, Direction.NORTH, Turn.STRAIGHT, 3);

        car.startCrossing(3);

        assertEquals(0, car.getWaitingTimeSeconds());
    }


    /**
     * Tests rejection of invalid constructor arguments.
     */
    @Test
    public void testInvalidConstructor()
    {
        try
        {
            new Vehicle(0, Direction.NORTH, Turn.LEFT, 0);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // Expected.
        }

        try
        {
            new Vehicle(1, null, Turn.LEFT, 0);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // Expected.
        }

        try
        {
            new Vehicle(1, Direction.NORTH, null, 0);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // Expected.
        }

        try
        {
            new Vehicle(1, Direction.NORTH, Turn.LEFT, -1);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // Expected.
        }
    }


    /**
     * Tests that crossing cannot start before the vehicle's arrival time.
     */
    @Test
    public void testCrossingBeforeArrival()
    {
        Vehicle car = new Vehicle(1, Direction.NORTH, Turn.STRAIGHT, 10);

        try
        {
            car.startCrossing(5);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // Expected.
        }

        assertEquals(VehicleState.WAITING, car.getState());
        assertEquals(-1, car.getCrossingStartSecond());
    }


    /**
     * Tests rejection of repeated crossing transitions.
     */
    @Test
    public void testStartCrossingTwice()
    {
        Vehicle car = new Vehicle(1, Direction.NORTH, Turn.STRAIGHT, 0);

        car.startCrossing(0);

        try
        {
            car.startCrossing(5);
            fail("Expected IllegalStateException");
        }
        catch (IllegalStateException e)
        {
            // Expected.
        }

        assertEquals(0, car.getCrossingStartSecond());
    }


    /**
     * Tests that a waiting vehicle cannot advance.
     */
    @Test
    public void testAdvanceWhileWaiting()
    {
        Vehicle car = new Vehicle(1, Direction.EAST, Turn.LEFT, 0);

        try
        {
            car.advance(10);
            fail("Expected IllegalStateException");
        }
        catch (IllegalStateException e)
        {
            // Expected.
        }

        assertEquals(0, car.getProgressMeters());
    }


    /**
     * Tests that zero and negative movement distances are rejected without
     * changing position.
     */
    @Test
    public void testInvalidAdvanceDistance()
    {
        Vehicle car = new Vehicle(1, Direction.WEST, Turn.STRAIGHT, 0);

        car.startCrossing(0);

        try
        {
            car.advance(0);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // Expected.
        }

        try
        {
            car.advance(-10);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // Expected.
        }

        assertEquals(0, car.getProgressMeters());
    }


    /**
     * Tests that a vehicle cannot finish crossing before entering the
     * intersection.
     */
    @Test
    public void testFinishWhileWaiting()
    {
        Vehicle car = new Vehicle(1, Direction.SOUTH, Turn.STRAIGHT, 0);

        try
        {
            car.finishCrossing();
            fail("Expected IllegalStateException");
        }
        catch (IllegalStateException e)
        {
            // Expected.
        }

        assertEquals(VehicleState.WAITING, car.getState());
    }


    /**
     * Tests that a completed vehicle cannot be advanced or marked DONE a second
     * time.
     */
    @Test
    public void testCompletedVehicleCannotMove()
    {
        Vehicle car = new Vehicle(1, Direction.SOUTH, Turn.STRAIGHT, 0);

        car.startCrossing(0);
        car.advance(20);
        car.finishCrossing();

        try
        {
            car.advance(10);
            fail("Expected IllegalStateException");
        }
        catch (IllegalStateException e)
        {
            // Expected.
        }

        try
        {
            car.finishCrossing();
            fail("Expected IllegalStateException");
        }
        catch (IllegalStateException e)
        {
            // Expected.
        }

        assertEquals(VehicleState.DONE, car.getState());
        assertEquals(20, car.getProgressMeters());
    }


    /**
     * Tests that waiting time cannot be finalized before the vehicle begins
     * crossing.
     */
    @Test
    public void testWaitingTimeBeforeCrossing()
    {
        Vehicle car = new Vehicle(1, Direction.NORTH, Turn.LEFT, 5);

        try
        {
            car.getWaitingTimeSeconds();
            fail("Expected IllegalStateException");
        }
        catch (IllegalStateException e)
        {
            // Expected.
        }
    }


    /**
     * Tests that movement cannot overflow the integer used to store vehicle
     * progress.
     */
    @Test
    public void testAdvanceOverflow()
    {
        Vehicle car = new Vehicle(1, Direction.EAST, Turn.STRAIGHT, 0);

        car.startCrossing(0);
        car.advance(Integer.MAX_VALUE);

        try
        {
            car.advance(1);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // Expected.
        }

        assertEquals(Integer.MAX_VALUE, car.getProgressMeters());
    }
}
