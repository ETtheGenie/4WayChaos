
package defaultPackage;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * JUnit 4 tests for V2 Vehicle classification, lifecycle transitions, movement,
 * boundary conditions, and invalid input handling.
 */
public class VehicleTest
{

    /**
     * Creates a normal, waiting vehicle for use in lifecycle tests.
     *
     * @return a normal vehicle arriving from the north at t=5
     */
    private Vehicle makeNormalVehicle()
    {
        return new Vehicle(
            1L,
            Direction.NORTH,
            Turn.STRAIGHT,
            5,
            VehicleType.NORMAL);
    }


    /**
     * Verifies that a Runnable throws IllegalArgumentException.
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
     * Verifies that a Runnable throws IllegalStateException.
     *
     * @param operation
     *            operation expected to reject its current state
     */
    private void assertIllegalState(Runnable operation)
    {
        try
        {
            operation.run();
            fail("Expected IllegalStateException.");
        }
        catch (IllegalStateException expected)
        {
            // Expected invalid-state rejection.
        }
    }


    /**
     * Tests all initial properties of an ordinary vehicle.
     */
    @Test
    public void testNormalVehicleConstructionAndGetters()
    {
        Vehicle vehicle = makeNormalVehicle();

        assertEquals(1L, vehicle.getId());
        assertEquals(Direction.NORTH, vehicle.getApproach());
        assertEquals(Turn.STRAIGHT, vehicle.getTurn());
        assertEquals(5, vehicle.getArrivalSecond());
        assertEquals(VehicleType.NORMAL, vehicle.getVehicleType());
        assertEquals(VehicleState.WAITING, vehicle.getState());
        assertEquals(-1, vehicle.getCrossingStartSecond());
        assertEquals(0, vehicle.getProgressMeters());
    }


    /**
     * Tests the emergency classification without changing the initial
     * lifecycle.
     */
    @Test
    public void testEmergencyVehicleConstructionAndGetters()
    {
        Vehicle vehicle = new Vehicle(
            42L,
            Direction.EAST,
            Turn.LEFT,
            0,
            VehicleType.EMERGENCY);

        assertEquals(42L, vehicle.getId());
        assertEquals(Direction.EAST, vehicle.getApproach());
        assertEquals(Turn.LEFT, vehicle.getTurn());
        assertEquals(0, vehicle.getArrivalSecond());
        assertEquals(VehicleType.EMERGENCY, vehicle.getVehicleType());
        assertEquals(VehicleState.WAITING, vehicle.getState());
    }


    /**
     * Verifies both classifications work with every approach and turn.
     */
    @Test
    public void testAllDirectionsTurnsAndTypes()
    {
        long id = 1L;
        for (Direction direction : Direction.values())
        {
            for (Turn turn : Turn.values())
            {
                for (VehicleType type : VehicleType.values())
                {
                    Vehicle vehicle =
                        new Vehicle(id++, direction, turn, 0, type);
                    assertEquals(direction, vehicle.getApproach());
                    assertEquals(turn, vehicle.getTurn());
                    assertEquals(type, vehicle.getVehicleType());
                }
            }
        }
    }


    /**
     * Rejects zero and negative vehicle identifiers.
     */
    @Test
    public void testInvalidVehicleIds()
    {
        assertInvalid(
            () -> new Vehicle(
                0L,
                Direction.NORTH,
                Turn.STRAIGHT,
                0,
                VehicleType.NORMAL));
        assertInvalid(
            () -> new Vehicle(
                -1L,
                Direction.NORTH,
                Turn.STRAIGHT,
                0,
                VehicleType.NORMAL));
        assertInvalid(
            () -> new Vehicle(
                Long.MIN_VALUE,
                Direction.NORTH,
                Turn.STRAIGHT,
                0,
                VehicleType.NORMAL));
    }


    /**
     * Accepts the largest positive identifier without overflow.
     */
    @Test
    public void testMaximumVehicleId()
    {
        Vehicle vehicle = new Vehicle(
            Long.MAX_VALUE,
            Direction.WEST,
            Turn.U_TURN,
            0,
            VehicleType.EMERGENCY);
        assertEquals(Long.MAX_VALUE, vehicle.getId());
    }


    /**
     * Rejects a null approach direction.
     */
    @Test
    public void testNullApproachRejected()
    {
        assertInvalid(
            () -> new Vehicle(1L, null, Turn.STRAIGHT, 0, VehicleType.NORMAL));
    }


    /**
     * Rejects a null intended turning movement.
     */
    @Test
    public void testNullTurnRejected()
    {
        assertInvalid(
            () -> new Vehicle(
                1L,
                Direction.NORTH,
                null,
                0,
                VehicleType.NORMAL));
    }


    /**
     * Rejects a null emergency/normal classification.
     */
    @Test
    public void testNullVehicleTypeRejected()
    {
        assertInvalid(
            () -> new Vehicle(1L, Direction.NORTH, Turn.STRAIGHT, 0, null));
    }


    /**
     * Rejects arrival timestamps before simulation time zero.
     */
    @Test
    public void testNegativeArrivalRejected()
    {
        assertInvalid(
            () -> new Vehicle(
                1L,
                Direction.NORTH,
                Turn.STRAIGHT,
                -1,
                VehicleType.NORMAL));
    }


    /**
     * Allows arrival and crossing to start at the same second, giving zero
     * wait.
     */
    @Test
    public void testCrossingAtArrivalTime()
    {
        Vehicle vehicle = makeNormalVehicle();
        vehicle.startCrossing(5);

        assertEquals(VehicleState.CROSSING, vehicle.getState());
        assertEquals(5, vehicle.getCrossingStartSecond());
        assertEquals(0, vehicle.getWaitingTimeSeconds());
        assertEquals(0, vehicle.getProgressMeters());
    }


    /**
     * Records a positive waiting time when crossing begins after arrival.
     */
    @Test
    public void testCrossingAfterWaiting()
    {
        Vehicle vehicle = makeNormalVehicle();
        vehicle.startCrossing(12);

        assertEquals(12, vehicle.getCrossingStartSecond());
        assertEquals(7, vehicle.getWaitingTimeSeconds());
        assertEquals(VehicleState.CROSSING, vehicle.getState());
    }


    /**
     * Rejects crossing before arrival without changing the waiting vehicle.
     */
    @Test
    public void testCrossingBeforeArrivalRejectedWithoutMutation()
    {
        Vehicle vehicle = makeNormalVehicle();
        assertInvalid(() -> vehicle.startCrossing(4));
        assertInvalid(() -> vehicle.startCrossing(-1));
        assertEquals(VehicleState.WAITING, vehicle.getState());
        assertEquals(-1, vehicle.getCrossingStartSecond());
        vehicle.startCrossing(5);
        assertEquals(0, vehicle.getWaitingTimeSeconds());
    }


    /**
     * Prevents beginning a crossing twice, preserving its original timestamp.
     */
    @Test
    public void testCannotStartCrossingTwice()
    {
        Vehicle vehicle = makeNormalVehicle();
        vehicle.startCrossing(8);
        assertIllegalState(() -> vehicle.startCrossing(10));
        assertEquals(8, vehicle.getCrossingStartSecond());
        assertEquals(3, vehicle.getWaitingTimeSeconds());
    }


    /**
     * Prevents restarting a vehicle that has already completed its crossing.
     */
    @Test
    public void testCannotRestartCompletedVehicle()
    {
        Vehicle vehicle = makeNormalVehicle();
        vehicle.startCrossing(8);
        vehicle.finishCrossing();
        assertIllegalState(() -> vehicle.startCrossing(10));
        assertEquals(VehicleState.DONE, vehicle.getState());
    }


    /**
     * Rejects attempts to move a vehicle that has not started crossing.
     */
    @Test
    public void testCannotAdvanceWaitingVehicle()
    {
        Vehicle vehicle = makeNormalVehicle();
        assertIllegalState(() -> vehicle.advance(10));
        assertEquals(0, vehicle.getProgressMeters());
    }


    /**
     * Advances crossing progress cumulatively by the supplied distances.
     */
    @Test
    public void testAdvanceAccumulatesDistance()
    {
        Vehicle vehicle = makeNormalVehicle();
        vehicle.startCrossing(5);
        vehicle.advance(10);
        vehicle.advance(5);

        assertEquals(15, vehicle.getProgressMeters());
        assertEquals(VehicleState.CROSSING, vehicle.getState());
    }


    /**
     * Rejects zero or negative distances without modifying crossing progress.
     */
    @Test
    public void testInvalidAdvanceDistances()
    {
        Vehicle vehicle = makeNormalVehicle();
        vehicle.startCrossing(5);
        assertInvalid(() -> vehicle.advance(0));
        assertInvalid(() -> vehicle.advance(-1));
        assertEquals(0, vehicle.getProgressMeters());
    }


    /**
     * Rejects movement that would exceed the maximum integer progress value.
     */
    @Test
    public void testAdvanceOverflowRejectedWithoutMutation()
    {
        Vehicle vehicle = makeNormalVehicle();
        vehicle.startCrossing(5);
        vehicle.advance(Integer.MAX_VALUE - 1);
        assertInvalid(() -> vehicle.advance(2));
        assertEquals(Integer.MAX_VALUE - 1, vehicle.getProgressMeters());
        vehicle.advance(1);
        assertEquals(Integer.MAX_VALUE, vehicle.getProgressMeters());
        assertInvalid(() -> vehicle.advance(1));
        assertEquals(Integer.MAX_VALUE, vehicle.getProgressMeters());
    }


    /**
     * Rejects completion of a vehicle that is still waiting.
     */
    @Test
    public void testCannotFinishWaitingVehicle()
    {
        Vehicle vehicle = makeNormalVehicle();
        assertIllegalState(() -> vehicle.finishCrossing());
        assertEquals(VehicleState.WAITING, vehicle.getState());
    }


    /**
     * Completes a crossing and retains the recorded movement and waiting time.
     */
    @Test
    public void testFinishCrossingAndRetainHistory()
    {
        Vehicle vehicle = new Vehicle(
            20L,
            Direction.SOUTH,
            Turn.U_TURN,
            7,
            VehicleType.EMERGENCY);
        vehicle.startCrossing(11);
        vehicle.advance(20);
        vehicle.finishCrossing();

        assertEquals(VehicleState.DONE, vehicle.getState());
        assertEquals(20, vehicle.getProgressMeters());
        assertEquals(11, vehicle.getCrossingStartSecond());
        assertEquals(4, vehicle.getWaitingTimeSeconds());
        assertEquals(VehicleType.EMERGENCY, vehicle.getVehicleType());
        assertEquals(Turn.U_TURN, vehicle.getTurn());
    }


    /**
     * Rejects attempts to mark an already completed vehicle as done again.
     */
    @Test
    public void testCannotFinishTwice()
    {
        Vehicle vehicle = makeNormalVehicle();
        vehicle.startCrossing(5);
        vehicle.finishCrossing();
        assertIllegalState(() -> vehicle.finishCrossing());
        assertEquals(VehicleState.DONE, vehicle.getState());
    }


    /**
     * Rejects movement after completion, leaving progress unchanged.
     */
    @Test
    public void testCannotAdvanceAfterCompletion()
    {
        Vehicle vehicle = makeNormalVehicle();
        vehicle.startCrossing(5);
        vehicle.advance(10);
        vehicle.finishCrossing();
        assertIllegalState(() -> vehicle.advance(5));
        assertEquals(10, vehicle.getProgressMeters());
    }


    /**
     * Rejects requesting the final waiting time before crossing begins.
     */
    @Test
    public void testWaitingTimeUnavailableBeforeCrossing()
    {
        Vehicle vehicle = makeNormalVehicle();
        assertIllegalState(() -> vehicle.getWaitingTimeSeconds());
    }


    /**
     * Checks that classification never changes during the full lifecycle.
     */
    @Test
    public void testEmergencyLifecycleMatchesNormalLifecycle()
    {
        Vehicle emergency = new Vehicle(
            99L,
            Direction.WEST,
            Turn.LEFT,
            3,
            VehicleType.EMERGENCY);
        Vehicle normal =
            new Vehicle(100L, Direction.WEST, Turn.LEFT, 3, VehicleType.NORMAL);

        emergency.startCrossing(5);
        normal.startCrossing(5);
        emergency.advance(10);
        normal.advance(10);
        emergency.finishCrossing();
        normal.finishCrossing();

        assertEquals(normal.getState(), emergency.getState());
        assertEquals(normal.getProgressMeters(), emergency.getProgressMeters());
        assertEquals(
            normal.getWaitingTimeSeconds(),
            emergency.getWaitingTimeSeconds());
        assertEquals(VehicleType.EMERGENCY, emergency.getVehicleType());
        assertEquals(VehicleType.NORMAL, normal.getVehicleType());
    }
}
