
package defaultPackage;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * JUnit 4 tests for adaptive traffic-signal state, congestion switching,
 * emergency priority, minimum/maximum phase times, and invalid input.
 */
public class TrafficSignalTest
{

    /**
     * Creates an adaptive signal with chosen timing and queue threshold.
     *
     * @param minimum
     *            shortest green in seconds
     * @param maximum
     *            longest green in seconds
     * @param threshold
     *            opposing serviceable queue advantage to switch
     * @return a new adaptive controller
     */
    private TrafficSignal signal(int minimum, int maximum, int threshold)
    {

        return new TrafficSignal(
            SimulationOptions.builder().minGreenSeconds(minimum)
                .maxGreenSeconds(maximum).queueSwitchThreshold(threshold)
                .build());
    }


    /**
     * Updates a fresh signal through each second, inclusive.
     *
     * @param signal
     *            fresh signal controller
     * @param through
     *            last second to process
     * @param ns
     *            number of serviceable NS vehicles
     * @param ew
     *            number of serviceable EW vehicles
     * @param nsEmergency
     *            NS emergency request
     * @param ewEmergency
     *            EW emergency request
     */
    private void runThrough(
        TrafficSignal signal,
        int through,
        int ns,
        int ew,
        boolean nsEmergency,
        boolean ewEmergency)
    {

        for (int second = 0; second <= through; second++)
        {
            signal.updateForSecond(second, ns, ew, nsEmergency, ewEmergency);
        }
    }


    /**
     * Verifies an invalid input throws IllegalArgumentException.
     *
     * @param action
     *            action expected to fail
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
     * Rejects a missing immutable configuration.
     */
    @Test
    public void testConstructorRejectsNullOptions()
    {
        assertInvalid(() -> new TrafficSignal(null));
    }


    /**
     * Verifies initial phase, counters, and directions without an update.
     */
    @Test
    public void testInitialStateAndDirectionPermissions()
    {
        TrafficSignal signal = signal(10, 60, 3);

        assertEquals(SignalPhase.NS_GREEN, signal.getCurrentPhase());
        assertEquals(0, signal.getPhaseStartSecond());
        assertEquals(0, signal.getPhaseSwitchCount());
        assertEquals(-1, signal.getLastUpdatedSecond());

        assertTrue(signal.allows(Direction.NORTH));
        assertTrue(signal.allows(Direction.SOUTH));
        assertFalse(signal.allows(Direction.EAST));
        assertFalse(signal.allows(Direction.WEST));
    }


    /**
     * Confirms a valid update at time zero does not change initial green.
     */
    @Test
    public void testFirstUpdateAtZero()
    {
        TrafficSignal signal = signal(10, 60, 3);

        signal.updateForSecond(0, 0, 100, false, true);

        assertEquals(SignalPhase.NS_GREEN, signal.getCurrentPhase());
        assertEquals(0, signal.getPhaseStartSecond());
        assertEquals(0, signal.getLastUpdatedSecond());
        assertEquals(0, signal.getPhaseSwitchCount());
    }


    /**
     * Confirms congestion cannot cause a switch before minimum green.
     */
    @Test
    public void testMinimumGreenProtectsAgainstCongestion()
    {
        TrafficSignal signal = signal(10, 60, 3);

        runThrough(signal, 9, 0, 100, false, false);

        assertEquals(SignalPhase.NS_GREEN, signal.getCurrentPhase());
        assertEquals(0, signal.getPhaseSwitchCount());
    }


    /**
     * Confirms a queued emergency cannot break minimum green.
     */
    @Test
    public void testMinimumGreenProtectsAgainstEmergency()
    {
        TrafficSignal signal = signal(10, 60, 3);

        runThrough(signal, 9, 50, 1, false, true);

        assertEquals(SignalPhase.NS_GREEN, signal.getCurrentPhase());
        assertEquals(0, signal.getPhaseSwitchCount());
    }


    /**
     * Tests switching exactly at the minimum green boundary with demand.
     */
    @Test
    public void testCongestionSwitchAtMinimumBoundary()
    {
        TrafficSignal signal = signal(10, 60, 3);

        runThrough(signal, 10, 1, 4, false, false);

        assertEquals(SignalPhase.EW_GREEN, signal.getCurrentPhase());
        assertEquals(10, signal.getPhaseStartSecond());
        assertEquals(1, signal.getPhaseSwitchCount());
        assertFalse(signal.allows(Direction.NORTH));
        assertTrue(signal.allows(Direction.EAST));
    }


    /**
     * Tests that a queue advantage below the threshold does not switch.
     */
    @Test
    public void testQueueThresholdBelowBoundary()
    {
        TrafficSignal signal = signal(10, 60, 3);

        runThrough(signal, 11, 1, 3, false, false);

        assertEquals(SignalPhase.NS_GREEN, signal.getCurrentPhase());
        assertEquals(0, signal.getPhaseSwitchCount());
    }


    /**
     * Tests that the queue difference exactly meeting threshold switches.
     */
    @Test
    public void testQueueThresholdExactBoundary()
    {
        TrafficSignal signal = signal(10, 60, 3);

        runThrough(signal, 9, 1, 3, false, false);
        signal.updateForSecond(10, 1, 4, false, false);

        assertEquals(SignalPhase.EW_GREEN, signal.getCurrentPhase());
    }


    /**
     * Tests idle-green yielding even if the opposing queue is below threshold.
     */
    @Test
    public void testIdleCurrentPhaseYieldsToSingleOpposingVehicle()
    {
        TrafficSignal signal = signal(10, 60, 50);

        runThrough(signal, 10, 0, 1, false, false);

        assertEquals(SignalPhase.EW_GREEN, signal.getCurrentPhase());
    }


    /**
     * Tests equal zero demand is maintained until maximum green.
     */
    @Test
    public void testNoDemandHoldsPhaseUntilMaximum()
    {
        TrafficSignal signal = signal(10, 20, 3);

        runThrough(signal, 19, 0, 0, false, false);

        assertEquals(SignalPhase.NS_GREEN, signal.getCurrentPhase());
        assertEquals(0, signal.getPhaseSwitchCount());

        signal.updateForSecond(20, 0, 0, false, false);

        assertEquals(SignalPhase.EW_GREEN, signal.getCurrentPhase());
    }


    /**
     * Tests serviceable current demand greater than opposing demand.
     */
    @Test
    public void testCurrentDemandWinsBelowMaximum()
    {
        TrafficSignal signal = signal(10, 60, 3);

        runThrough(signal, 20, 20, 1, false, false);

        assertEquals(SignalPhase.NS_GREEN, signal.getCurrentPhase());
    }


    /**
     * Tests an opposing emergency request can override large queue imbalance.
     */
    @Test
    public void testOpposingEmergencySwitchesAtMinimum()
    {
        TrafficSignal signal = signal(10, 60, 3);

        runThrough(signal, 10, 100, 1, false, true);

        assertEquals(SignalPhase.EW_GREEN, signal.getCurrentPhase());
        assertEquals(10, signal.getPhaseStartSecond());
        assertEquals(1, signal.getPhaseSwitchCount());
    }


    /**
     * Tests an emergency on the active green prevents congestion-based
     * switching.
     */
    @Test
    public void testCurrentEmergencyKeepsGreenAgainstOpposingCongestion()
    {
        TrafficSignal signal = signal(10, 20, 3);

        runThrough(signal, 19, 1, 100, true, false);

        assertEquals(SignalPhase.NS_GREEN, signal.getCurrentPhase());

        signal.updateForSecond(20, 1, 100, true, false);

        assertEquals(SignalPhase.EW_GREEN, signal.getCurrentPhase());
    }


    /**
     * Tests simultaneous emergency requests defer to maximum-green fairness.
     */
    @Test
    public void testConflictingEmergencyRequestsHoldUntilMaximum()
    {
        TrafficSignal signal = signal(10, 20, 3);

        runThrough(signal, 19, 1, 100, true, true);

        assertEquals(SignalPhase.NS_GREEN, signal.getCurrentPhase());

        signal.updateForSecond(20, 1, 100, true, true);

        assertEquals(SignalPhase.EW_GREEN, signal.getCurrentPhase());
    }


    /**
     * Tests forced switching at maximum even without opposing demand.
     */
    @Test
    public void testMaximumGreenForcesSwitchWithoutOpposingDemand()
    {
        TrafficSignal signal = signal(5, 15, 3);

        runThrough(signal, 14, 10, 0, false, false);

        assertEquals(SignalPhase.NS_GREEN, signal.getCurrentPhase());

        signal.updateForSecond(15, 10, 0, false, false);

        assertEquals(SignalPhase.EW_GREEN, signal.getCurrentPhase());
        assertEquals(15, signal.getPhaseStartSecond());
    }


    /**
     * Tests minimum-green time is reestablished after every switch.
     */
    @Test
    public void testMinimumResetsAfterSwitch()
    {
        TrafficSignal signal = signal(10, 60, 3);

        runThrough(signal, 10, 0, 10, false, false);
        assertEquals(SignalPhase.EW_GREEN, signal.getCurrentPhase());

        for (int second = 11; second < 20; second++)
        {
            signal.updateForSecond(second, 10, 0, false, false);
            assertEquals(SignalPhase.EW_GREEN, signal.getCurrentPhase());
        }

        signal.updateForSecond(20, 10, 0, false, false);

        assertEquals(SignalPhase.NS_GREEN, signal.getCurrentPhase());
        assertEquals(20, signal.getPhaseStartSecond());
        assertEquals(2, signal.getPhaseSwitchCount());
    }


    /**
     * Tests that emergency priority also works when east-west is active.
     */
    @Test
    public void testNorthSouthEmergencyPriorityWhenEastWestActive()
    {
        TrafficSignal signal = signal(10, 60, 3);

        runThrough(signal, 10, 0, 5, false, false);
        assertEquals(SignalPhase.EW_GREEN, signal.getCurrentPhase());

        for (int second = 11; second < 20; second++)
        {
            signal.updateForSecond(second, 1, 100, true, false);
        }

        assertEquals(SignalPhase.EW_GREEN, signal.getCurrentPhase());

        signal.updateForSecond(20, 1, 100, true, false);

        assertEquals(SignalPhase.NS_GREEN, signal.getCurrentPhase());
    }


    /**
     * Tests that a blocked approach contributing zero serviceable demand cannot
     * induce a switch despite having a raw queue elsewhere in the model.
     */
    @Test
    public void testNoPriorityFromBlockedApproach()
    {
        TrafficSignal signal = signal(10, 60, 3);

        // A blocked EW approach contributes zero serviceable demand.
        runThrough(signal, 15, 3, 0, false, false);

        assertEquals(SignalPhase.NS_GREEN, signal.getCurrentPhase());
    }


    /**
     * Tests a boundary configuration where minimum and maximum green are equal.
     */
    @Test
    public void testEqualMinimumMaximumSwitchesAtExactInterval()
    {
        TrafficSignal signal = signal(5, 5, 1);

        runThrough(signal, 4, 0, 0, false, false);
        assertEquals(SignalPhase.NS_GREEN, signal.getCurrentPhase());

        signal.updateForSecond(5, 0, 0, false, false);
        assertEquals(SignalPhase.EW_GREEN, signal.getCurrentPhase());

        for (int second = 6; second <= 10; second++)
        {
            signal.updateForSecond(second, 0, 0, false, false);
        }

        assertEquals(SignalPhase.NS_GREEN, signal.getCurrentPhase());
        assertEquals(2, signal.getPhaseSwitchCount());
    }


    /**
     * Tests negative, skipped, and repeated update times are rejected.
     */
    @Test
    public void testInvalidUpdateTimeOrder()
    {
        TrafficSignal signal = signal(10, 60, 3);

        assertInvalid(() -> signal.updateForSecond(-1, 1, 1, false, false));
        assertInvalid(() -> signal.updateForSecond(1, 1, 1, false, false));

        assertEquals(-1, signal.getLastUpdatedSecond());

        signal.updateForSecond(0, 1, 1, false, false);

        assertInvalid(() -> signal.updateForSecond(0, 1, 1, false, false));
        assertInvalid(() -> signal.updateForSecond(2, 1, 1, false, false));

        assertEquals(0, signal.getLastUpdatedSecond());

        signal.updateForSecond(1, 1, 1, false, false);
        assertEquals(1, signal.getLastUpdatedSecond());
    }


    /**
     * Tests that invalid serviceable queues do not advance signal time.
     */
    @Test
    public void testNegativeQueueCountsRejectedWithoutMutation()
    {
        TrafficSignal signal = signal(10, 60, 3);

        assertInvalid(() -> signal.updateForSecond(0, -1, 1, false, false));
        assertInvalid(() -> signal.updateForSecond(0, 1, -1, false, false));

        assertEquals(-1, signal.getLastUpdatedSecond());
        assertEquals(0, signal.getPhaseSwitchCount());

        signal.updateForSecond(0, 0, 0, false, false);

        assertEquals(0, signal.getLastUpdatedSecond());
    }


    /**
     * Tests that emergency requests without serviceable waiting cars are
     * rejected.
     */
    @Test
    public void testEmergencyWithoutServiceableQueueRejected()
    {
        TrafficSignal signal = signal(10, 60, 3);

        assertInvalid(() -> signal.updateForSecond(0, 0, 1, true, false));
        assertInvalid(() -> signal.updateForSecond(0, 1, 0, false, true));

        assertEquals(-1, signal.getLastUpdatedSecond());

        signal.updateForSecond(0, 1, 1, true, true);

        assertEquals(0, signal.getLastUpdatedSecond());
    }


    /**
     * Tests null directions cannot be passed to signal permission lookup.
     */
    @Test
    public void testAllowsRejectsNullDirection()
    {
        TrafficSignal signal = signal(10, 60, 3);

        assertInvalid(() -> signal.allows(null));
    }


    /**
     * Tests phase-change counters and timestamps across repeated maximum-green
     * switches.
     */
    @Test
    public void testRepeatedMaximumSwitchesAndCounters()
    {
        TrafficSignal signal = signal(10, 20, 3);

        runThrough(signal, 60, 0, 0, false, false);

        assertEquals(SignalPhase.EW_GREEN, signal.getCurrentPhase());
        assertEquals(60, signal.getPhaseStartSecond());
        assertEquals(3, signal.getPhaseSwitchCount());
        assertEquals(60, signal.getLastUpdatedSecond());

        assertFalse(signal.allows(Direction.NORTH));
        assertFalse(signal.allows(Direction.SOUTH));
        assertTrue(signal.allows(Direction.EAST));
        assertTrue(signal.allows(Direction.WEST));
    }


    /**
     * Stress-tests timing invariants and sequential updates for 7200 seconds.
     */
    @Test
    public void testLongRunRespectsMinAndMaxGreen()
    {
        TrafficSignal signal = signal(5, 37, 3);

        int lastStart = 0;
        int changes = 0;

        for (int t = 0; t < 7200; t++)
        {
            int ns = t % 79 < 37 ? 100 : 1;
            int ew = t % 79 < 37 ? 1 : 100;

            signal.updateForSecond(t, ns, ew, false, false);

            if (signal.getPhaseStartSecond() != lastStart)
            {
                int interval = signal.getPhaseStartSecond() - lastStart;

                assertTrue(
                    "Phase shorter than minimum at t=" + t,
                    interval >= 5);
                assertTrue(
                    "Phase longer than maximum at t=" + t,
                    interval <= 37);

                changes++;
                lastStart = signal.getPhaseStartSecond();
            }
        }

        assertTrue(changes > 0);
        assertEquals(changes, signal.getPhaseSwitchCount());
        assertTrue(7199 - lastStart < 37);
    }
}
