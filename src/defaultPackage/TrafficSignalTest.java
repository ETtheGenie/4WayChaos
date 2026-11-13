package defaultPackage;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * JUnit 4 tests for TrafficSignal. Verifies fixed-time signal transitions,
 * directional permissions, timing boundaries, input validation, and
 * deterministic operation.
 */
public class TrafficSignalTest
{

    /**
     * Verifies that an operation rejects invalid arguments with
     * IllegalArgumentException.
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
     * Tests correct construction and retrieval of signal timing parameters.
     */
    @Test
    public void testConstructorAndGetters()
    {
        TrafficSignal signal = new TrafficSignal(30, 20);

        assertEquals(30, signal.getNsGreenSeconds());
        assertEquals(20, signal.getEwGreenSeconds());
    }


    /**
     * Tests that the intersection initially starts with north-south traffic
     * receiving green.
     */
    @Test
    public void testInitialPhase()
    {
        TrafficSignal signal = new TrafficSignal(30, 20);

        assertEquals(SignalPhase.NS_GREEN, signal.phaseAt(0));
    }


    /**
     * Tests correct phase changes at exact boundaries of a 50-second signal
     * cycle.
     */
    @Test
    public void testPhaseTransitions()
    {
        TrafficSignal signal = new TrafficSignal(30, 20);

        assertEquals(SignalPhase.NS_GREEN, signal.phaseAt(0));

        assertEquals(SignalPhase.NS_GREEN, signal.phaseAt(29));

        assertEquals(SignalPhase.EW_GREEN, signal.phaseAt(30));

        assertEquals(SignalPhase.EW_GREEN, signal.phaseAt(49));

        assertEquals(SignalPhase.NS_GREEN, signal.phaseAt(50));
    }


    /**
     * Tests that phase timing repeats correctly over multiple full cycles.
     */
    @Test
    public void testRepeatedCycles()
    {
        TrafficSignal signal = new TrafficSignal(30, 20);

        assertEquals(SignalPhase.NS_GREEN, signal.phaseAt(100));

        assertEquals(SignalPhase.NS_GREEN, signal.phaseAt(129));

        assertEquals(SignalPhase.EW_GREEN, signal.phaseAt(130));

        assertEquals(SignalPhase.EW_GREEN, signal.phaseAt(149));

        assertEquals(SignalPhase.NS_GREEN, signal.phaseAt(150));
    }


    /**
     * Tests that north and south are permitted during NS_GREEN while east and
     * west are not.
     */
    @Test
    public void testNorthSouthPermissions()
    {
        TrafficSignal signal = new TrafficSignal(30, 20);

        assertTrue(signal.allows(Direction.NORTH, 10));
        assertTrue(signal.allows(Direction.SOUTH, 10));

        assertFalse(signal.allows(Direction.EAST, 10));
        assertFalse(signal.allows(Direction.WEST, 10));
    }


    /**
     * Tests that east and west are permitted during EW_GREEN while north and
     * south are not.
     */
    @Test
    public void testEastWestPermissions()
    {
        TrafficSignal signal = new TrafficSignal(30, 20);

        assertTrue(signal.allows(Direction.EAST, 35));
        assertTrue(signal.allows(Direction.WEST, 35));

        assertFalse(signal.allows(Direction.NORTH, 35));
        assertFalse(signal.allows(Direction.SOUTH, 35));
    }


    /**
     * Tests that directional permissions change at the exact signal transition
     * times.
     */
    @Test
    public void testPermissionTransitions()
    {
        TrafficSignal signal = new TrafficSignal(30, 20);

        assertTrue(signal.allows(Direction.NORTH, 29));
        assertFalse(signal.allows(Direction.EAST, 29));

        assertFalse(signal.allows(Direction.NORTH, 30));
        assertTrue(signal.allows(Direction.EAST, 30));

        assertFalse(signal.allows(Direction.EAST, 50));
        assertTrue(signal.allows(Direction.NORTH, 50));
    }


    /**
     * Tests valid minimum and maximum signal durations defined by
     * SimulationConfig.
     */
    @Test
    public void testValidDurationBoundaries()
    {
        TrafficSignal minimum = new TrafficSignal(5, 5);

        assertEquals(SignalPhase.NS_GREEN, minimum.phaseAt(4));

        assertEquals(SignalPhase.EW_GREEN, minimum.phaseAt(5));

        assertEquals(SignalPhase.NS_GREEN, minimum.phaseAt(10));

        TrafficSignal maximum = new TrafficSignal(120, 120);

        assertEquals(SignalPhase.NS_GREEN, maximum.phaseAt(119));

        assertEquals(SignalPhase.EW_GREEN, maximum.phaseAt(120));

        assertEquals(SignalPhase.NS_GREEN, maximum.phaseAt(240));
    }


    /**
     * Tests rejection of signal durations below their permitted minimum or
     * above their maximum.
     */
    @Test
    public void testInvalidDurations()
    {
        assertInvalid(() -> new TrafficSignal(0, 30));
        assertInvalid(() -> new TrafficSignal(4, 30));
        assertInvalid(() -> new TrafficSignal(-1, 30));
        assertInvalid(() -> new TrafficSignal(121, 30));

        assertInvalid(() -> new TrafficSignal(30, 0));
        assertInvalid(() -> new TrafficSignal(30, 4));
        assertInvalid(() -> new TrafficSignal(30, -1));
        assertInvalid(() -> new TrafficSignal(30, 121));
    }


    /**
     * Tests that negative simulation times are rejected by both phase and
     * permission queries.
     */
    @Test
    public void testNegativeSimulationTime()
    {
        TrafficSignal signal = new TrafficSignal(30, 20);

        assertInvalid(() -> signal.phaseAt(-1));

        assertInvalid(() -> signal.allows(Direction.NORTH, -1));
    }


    /**
     * Tests that a null approach is rejected rather than being interpreted as a
     * red signal.
     */
    @Test
    public void testNullDirection()
    {
        TrafficSignal signal = new TrafficSignal(30, 20);

        assertInvalid(() -> signal.allows(null, 10));
    }


    /**
     * Tests that repeated queries at the same time return identical results
     * without altering state.
     */
    @Test
    public void testDeterministicBehavior()
    {
        TrafficSignal signal = new TrafficSignal(30, 20);

        SignalPhase first = signal.phaseAt(75);

        for (int i = 0; i < 100; i++)
        {
            assertEquals(first, signal.phaseAt(75));
        }

        assertEquals(SignalPhase.NS_GREEN, signal.phaseAt(0));

        assertEquals(SignalPhase.NS_GREEN, signal.phaseAt(75));

        assertEquals(SignalPhase.EW_GREEN, signal.phaseAt(85));
    }


    /**
     * Tests signal phase calculation near the largest representable integer
     * simulation time. Confirms that large elapsed times do not cause overflow
     * or incorrect cycle calculations.
     */
    @Test
    public void testLargeSimulationTime()
    {
        TrafficSignal signal = new TrafficSignal(30, 20);

        int time = Integer.MAX_VALUE;
        int cyclePosition = time % 50;

        SignalPhase expected =
            cyclePosition < 30 ? SignalPhase.NS_GREEN : SignalPhase.EW_GREEN;

        assertEquals(expected, signal.phaseAt(time));
    }
}
