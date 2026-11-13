package defaultPackage;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.EnumMap;
import java.util.Map;
import org.junit.Test;

/**
 * JUnit 4 integration tests for the queue-backed SimulationModel. These tests
 * use no view or controller and verify the complete intersection simulation,
 * time boundaries, and final statistics.
 */
public class SimulationModelTest
{

    /**
     * Builds a valid configuration with individually specified arrival rates
     * and fixed-time signal phases.
     *
     * @param north
     *            north approach arrivals per hour
     * @param south
     *            south approach arrivals per hour
     * @param east
     *            east approach arrivals per hour
     * @param west
     *            west approach arrivals per hour
     * @param ns
     *            north-south green duration in seconds
     * @param ew
     *            east-west green duration in seconds
     * @param duration
     *            total run duration in seconds
     * @param left
     *            percentage of left turns
     * @param u
     *            percentage of U-turns
     * @return validated simulation configuration
     */
    private SimulationConfig config(
        int north,
        int south,
        int east,
        int west,
        int ns,
        int ew,
        int duration,
        int left,
        int u)
    {
        Map<Direction, Integer> rates = new EnumMap<>(Direction.class);
        rates.put(Direction.NORTH, north);
        rates.put(Direction.SOUTH, south);
        rates.put(Direction.EAST, east);
        rates.put(Direction.WEST, west);
        return new SimulationConfig(rates, ns, ew, duration, left, u);
    }


    /**
     * Advances a model until all configured simulation steps have run.
     *
     * @param model
     *            simulation model to execute
     */
    private void runToCompletion(SimulationModel model)
    {
        while (!model.isComplete())
        {
            model.step();
        }
    }


    /**
     * Verifies that a Runnable throws IllegalStateException.
     *
     * @param operation
     *            invalid operation to execute
     */
    private void assertBadState(Runnable operation)
    {
        try
        {
            operation.run();
            fail("Expected IllegalStateException");
        }
        catch (IllegalStateException expected)
        {
            // Expected exception.
        }
    }


    /**
     * Tests that a null configuration is rejected by the constructor.
     */
    @Test
    public void testNullConfiguration()
    {
        try
        {
            new SimulationModel(null);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected)
        {
            // Expected exception.
        }
    }


    /**
     * Tests empty initial queues, zero counts, initial green phase, and
     * incomplete status before any steps execute.
     */
    @Test
    public void testInitialState()
    {
        SimulationModel model =
            new SimulationModel(config(600, 400, 800, 500, 30, 30, 60, 20, 10));
        SimulationSnapshot snapshot = model.getSnapshot();
        assertEquals(0, snapshot.getElapsedSeconds());
        assertEquals(SignalPhase.NS_GREEN, snapshot.getSignalPhase());
        assertEquals(0, snapshot.getTotalArrived());
        assertEquals(0, snapshot.getTotalCompleted());
        assertEquals(0, snapshot.getCrossingCount());
        assertEquals(0, snapshot.getWaitingCount());
        assertFalse(model.isComplete());
        for (Direction direction : Direction.values())
        {
            assertEquals(0, snapshot.getQueueLength(direction));
        }
    }


    /**
     * Tests a full zero-traffic run, including valid final results, all queue
     * statistics and undefined mean waiting time.
     */
    @Test
    public void testZeroTrafficRun()
    {
        SimulationModel model =
            new SimulationModel(config(0, 0, 0, 0, 30, 30, 60, 0, 0));
        runToCompletion(model);
        assertTrue(model.isComplete());
        SimulationResults results = model.getResults();
        assertEquals(60, results.getDurationSeconds());
        assertEquals(0, results.getTotalArrived());
        assertEquals(0, results.getTotalCompleted());
        assertEquals(0, results.getWaitingCount());
        assertEquals(0, results.getCrossingCount());
        assertNull(results.getAverageWaitingTimeSeconds());
        for (Direction direction : Direction.values())
        {
            assertEquals(0, results.getFinalQueueLength(direction));
            assertEquals(0, results.getMaximumQueueLength(direction));
            assertEquals(0.0, results.getAverageQueueLength(direction), 0.0001);
        }
    }


    /**
     * Tests that final results cannot be requested before the run ends.
     */
    @Test
    public void testResultsBeforeCompletion()
    {
        SimulationModel model =
            new SimulationModel(config(0, 0, 0, 0, 30, 30, 60, 0, 0));
        assertBadState(() -> model.getResults());
        model.step();
        assertBadState(() -> model.getResults());
    }


    /**
     * Tests that a completed model rejects additional steps without altering
     * its snapshot or final result counts.
     */
    @Test
    public void testStepAfterCompletion()
    {
        SimulationModel model =
            new SimulationModel(config(600, 0, 0, 0, 30, 30, 60, 0, 0));
        runToCompletion(model);
        SimulationSnapshot before = model.getSnapshot();
        assertBadState(() -> model.step());
        SimulationSnapshot after = model.getSnapshot();
        assertEquals(before.getElapsedSeconds(), after.getElapsedSeconds());
        assertEquals(before.getTotalArrived(), after.getTotalArrived());
        assertEquals(before.getTotalCompleted(), after.getTotalCompleted());
        assertEquals(before.getWaitingCount(), after.getWaitingCount());
        assertEquals(before.getCrossingCount(), after.getCrossingCount());
    }


    /**
     * Tests one-second increments and the exact NS-to-EW and EW-to-NS phase
     * boundaries reported by snapshots.
     */
    @Test
    public void testClockAndSignalBoundaries()
    {
        SimulationModel model =
            new SimulationModel(config(0, 0, 0, 0, 30, 30, 60, 0, 0));
        for (int i = 0; i < 29; i++)
        {
            model.step();
        }
        assertEquals(29, model.getSnapshot().getElapsedSeconds());
        assertEquals(
            SignalPhase.NS_GREEN,
            model.getSnapshot().getSignalPhase());
        model.step();
        assertEquals(30, model.getSnapshot().getElapsedSeconds());
        assertEquals(
            SignalPhase.EW_GREEN,
            model.getSnapshot().getSignalPhase());
        runToCompletion(model);
        assertEquals(60, model.getSnapshot().getElapsedSeconds());
        assertEquals(
            SignalPhase.NS_GREEN,
            model.getSnapshot().getSignalPhase());
    }


    /**
     * Tests exact 600-vehicle/hour generation in all four approaches and
     * conservation of generated, waiting, crossing and completed cars.
     */
    @Test
    public void testFourDirectionArrivalCounts()
    {
        SimulationModel model =
            new SimulationModel(config(600, 600, 600, 600, 30, 30, 60, 20, 10));
        runToCompletion(model);
        SimulationResults results = model.getResults();
        assertEquals(40, results.getTotalArrived());
        assertEquals(
            results.getTotalArrived(),
            results.getWaitingCount() + results.getCrossingCount()
                + results.getTotalCompleted());
        assertTrue(results.getTotalCompleted() > 0);
    }


    /**
     * Tests that the NORTH queue releases during the NS phase only, and
     * continues to accumulate arrivals while its signal is red. Also checks
     * exact average and maximum sampled queue sizes.
     */
    @Test
    public void testNorthRedPhaseBacklogAndQueueAverage()
    {
        SimulationModel model =
            new SimulationModel(config(1800, 0, 0, 0, 30, 30, 60, 0, 0));
        runToCompletion(model);
        SimulationResults results = model.getResults();
        assertEquals(30, results.getTotalArrived());
        assertEquals(15, results.getTotalCompleted());
        assertEquals(15, results.getFinalQueueLength(Direction.NORTH));
        assertEquals(0, results.getCrossingCount());
        assertEquals(15, results.getMaximumQueueLength(Direction.NORTH));
        assertEquals(
            3.75,
            results.getAverageQueueLength(Direction.NORTH),
            0.0001);
        assertEquals(0.0, results.getAverageWaitingTimeSeconds(), 0.0001);
    }


    /**
     * Tests that the EAST approach waits for its first green phase. With 1
     * arrival every 2 seconds and 2-second release headway, the first 15
     * completed vehicles each wait exactly 29 seconds.
     */
    @Test
    public void testEastRedPhaseWaitingTime()
    {
        SimulationModel model =
            new SimulationModel(config(0, 0, 1800, 0, 30, 30, 60, 0, 0));
        runToCompletion(model);
        SimulationResults results = model.getResults();
        assertEquals(30, results.getTotalArrived());
        assertEquals(15, results.getTotalCompleted());
        assertEquals(15, results.getFinalQueueLength(Direction.EAST));
        assertEquals(29.0, results.getAverageWaitingTimeSeconds(), 0.0001);
    }


    /**
     * Tests that NORTH and SOUTH independently discharge vehicles during their
     * shared green phase, with globally unique IDs. Duplicate completion IDs
     * would cause the accumulator to throw.
     */
    @Test
    public void testSimultaneousNorthSouthReleases()
    {
        SimulationModel model =
            new SimulationModel(config(1800, 1800, 0, 0, 30, 30, 60, 0, 0));
        runToCompletion(model);
        SimulationResults results = model.getResults();
        assertEquals(60, results.getTotalArrived());
        assertEquals(30, results.getTotalCompleted());
        assertEquals(15, results.getFinalQueueLength(Direction.NORTH));
        assertEquals(15, results.getFinalQueueLength(Direction.SOUTH));
    }


    /**
     * Tests that a car released during the last simulation step remains
     * CROSSING rather than being counted as DONE.
     */
    @Test
    public void testUnfinishedCrossingAtEnd()
    {
        SimulationModel model =
            new SimulationModel(config(1800, 0, 0, 0, 120, 5, 60, 0, 0));
        runToCompletion(model);
        SimulationResults results = model.getResults();
        assertEquals(30, results.getTotalArrived());
        assertEquals(29, results.getTotalCompleted());
        assertEquals(0, results.getWaitingCount());
        assertEquals(1, results.getCrossingCount());
        assertEquals(0.0, results.getAverageWaitingTimeSeconds(), 0.0001);
    }


    /**
     * Tests that an old immutable snapshot is unaffected by later simulation
     * steps and its queue map cannot be modified.
     */
    @Test
    public void testSnapshotIndependenceAndImmutability()
    {
        SimulationModel model =
            new SimulationModel(config(0, 0, 1800, 0, 30, 30, 60, 0, 0));
        for (int i = 0; i < 10; i++)
        {
            model.step();
        }
        SimulationSnapshot earlier = model.getSnapshot();
        assertEquals(5, earlier.getQueueLength(Direction.EAST));
        for (int i = 0; i < 10; i++)
        {
            model.step();
        }
        SimulationSnapshot later = model.getSnapshot();
        assertEquals(5, earlier.getQueueLength(Direction.EAST));
        assertEquals(10, later.getQueueLength(Direction.EAST));
        try
        {
            earlier.getQueueLengths().put(Direction.EAST, 50);
            fail("Expected UnsupportedOperationException");
        }
        catch (UnsupportedOperationException expected)
        {
            // Expected: snapshots must be read-only.
        }
        assertEquals(5, earlier.getQueueLength(Direction.EAST));
    }


    /**
     * Tests vehicle conservation after every step of a multi-lane run.
     */
    @Test
    public void testConservationAtEveryStep()
    {
        SimulationModel model = new SimulationModel(
            config(1800, 1200, 600, 900, 20, 15, 120, 20, 10));
        while (!model.isComplete())
        {
            model.step();
            SimulationSnapshot snapshot = model.getSnapshot();
            assertEquals(
                snapshot.getTotalArrived(),
                snapshot.getWaitingCount() + snapshot.getCrossingCount()
                    + snapshot.getTotalCompleted());
        }
    }


    /**
     * Tests that two independent models with identical settings produce
     * identical results and no shared mutable state.
     */
    @Test
    public void testRepeatabilityAndIndependentModels()
    {
        SimulationConfig settings =
            config(600, 1200, 900, 300, 25, 35, 120, 20, 10);
        SimulationModel first = new SimulationModel(settings);
        SimulationModel second = new SimulationModel(settings);
        runToCompletion(first);
        assertEquals(0, second.getSnapshot().getTotalArrived());
        runToCompletion(second);
        SimulationResults a = first.getResults();
        SimulationResults b = second.getResults();
        assertEquals(a.getTotalArrived(), b.getTotalArrived());
        assertEquals(a.getTotalCompleted(), b.getTotalCompleted());
        assertEquals(a.getCrossingCount(), b.getCrossingCount());
        assertEquals(a.getWaitingCount(), b.getWaitingCount());
        assertEquals(
            a.getAverageWaitingTimeSeconds(),
            b.getAverageWaitingTimeSeconds());
        for (Direction d : Direction.values())
        {
            assertEquals(a.getFinalQueueLength(d), b.getFinalQueueLength(d));
            assertEquals(
                a.getMaximumQueueLength(d),
                b.getMaximumQueueLength(d));
            assertEquals(
                a.getAverageQueueLength(d),
                b.getAverageQueueLength(d),
                0.0001);
        }
    }


    /**
     * Tests the maximum allowed duration and arrival rate across all
     * approaches; verifies the run ends with consistent vehicle counts.
     */
    @Test
    public void testMaximumSupportedLoad()
    {
        SimulationModel model = new SimulationModel(
            config(1800, 1800, 1800, 1800, 5, 5, 7200, 20, 10));
        runToCompletion(model);
        SimulationResults results = model.getResults();
        assertEquals(14400, results.getTotalArrived());
        assertEquals(
            results.getTotalArrived(),
            results.getWaitingCount() + results.getCrossingCount()
                + results.getTotalCompleted());
        assertTrue(results.getTotalCompleted() > 0);
    }


    /**
     * Tests that the constant physical crossing parameters remain aligned with
     * the agreed V1 specification.
     */
    @Test
    public void testCrossingConstants()
    {
        assertEquals(20, SimulationModel.CROSSING_DISTANCE_METERS);
        assertEquals(10, SimulationModel.VEHICLE_SPEED_METERS_PER_SECOND);
    }
}
