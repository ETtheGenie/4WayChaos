package defaultPackage;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.EnumMap;
import java.util.Map;
import org.junit.Test;

/**
 * JUnit 4 tests for SimulationResults. Verifies final report construction,
 * statistics, immutability, and input validation.
 */
public class SimulationResultsTest
{

    /**
     * Creates valid final queue lengths.
     *
     * @return queue lengths totaling six vehicles
     */
    private Map<Direction, Integer> finalQueues()
    {
        Map<Direction, Integer> values = new EnumMap<>(Direction.class);

        values.put(Direction.NORTH, 2);
        values.put(Direction.SOUTH, 1);
        values.put(Direction.EAST, 2);
        values.put(Direction.WEST, 1);

        return values;
    }


    /**
     * Creates valid average queue lengths.
     *
     * @return average lengths for all four approaches
     */
    private Map<Direction, Double> averageQueues()
    {
        Map<Direction, Double> values = new EnumMap<>(Direction.class);

        values.put(Direction.NORTH, 1.5);
        values.put(Direction.SOUTH, 0.5);
        values.put(Direction.EAST, 1.0);
        values.put(Direction.WEST, 0.25);

        return values;
    }


    /**
     * Creates valid maximum queue lengths.
     *
     * @return maximum lengths for all four approaches
     */
    private Map<Direction, Integer> maximumQueues()
    {
        Map<Direction, Integer> values = new EnumMap<>(Direction.class);

        values.put(Direction.NORTH, 3);
        values.put(Direction.SOUTH, 2);
        values.put(Direction.EAST, 4);
        values.put(Direction.WEST, 1);

        return values;
    }


    /**
     * Constructs a valid report with twenty arrivals.
     *
     * @return valid SimulationResults instance
     */
    private SimulationResults validResults()
    {
        return new SimulationResults(
            60,
            20,
            12,
            2,
            6.5,
            finalQueues(),
            averageQueues(),
            maximumQueues());
    }


    /**
     * Verifies that an operation rejects invalid input.
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
     * Tests construction and all summary getters.
     */
    @Test
    public void testConstructorAndGetters()
    {
        SimulationResults results = validResults();

        assertEquals(60, results.getDurationSeconds());
        assertEquals(20, results.getTotalArrived());
        assertEquals(12, results.getTotalCompleted());
        assertEquals(6, results.getWaitingCount());
        assertEquals(2, results.getCrossingCount());

        assertEquals(6.5, results.getAverageWaitingTimeSeconds(), 0.0001);

        assertEquals(2, results.getFinalQueueLength(Direction.NORTH));

        assertEquals(
            1.5,
            results.getAverageQueueLength(Direction.NORTH),
            0.0001);

        assertEquals(3, results.getMaximumQueueLength(Direction.NORTH));

        assertEquals(4, results.getFinalQueueLengths().size());
        assertEquals(4, results.getAverageQueueLengths().size());
        assertEquals(4, results.getMaximumQueueLengths().size());
    }


    /**
     * Tests an empty simulation with zero arrivals and undefined average
     * waiting time.
     */
    @Test
    public void testNoTraffic()
    {
        Map<Direction, Integer> zeros = new EnumMap<>(Direction.class);

        Map<Direction, Double> averageZeros = new EnumMap<>(Direction.class);

        for (Direction direction : Direction.values())
        {
            zeros.put(direction, 0);
            averageZeros.put(direction, 0.0);
        }

        SimulationResults results = new SimulationResults(
            60,
            0,
            0,
            0,
            null,
            zeros,
            averageZeros,
            zeros);

        assertEquals(0, results.getTotalArrived());
        assertEquals(0, results.getWaitingCount());
        assertNull(results.getAverageWaitingTimeSeconds());
    }


    /**
     * Tests that unfinished vehicles are preserved even if no vehicles
     * completed the intersection.
     */
    @Test
    public void testNoCompletionsButVehiclesRemain()
    {
        SimulationResults results = new SimulationResults(
            60,
            8,
            0,
            2,
            null,
            finalQueues(),
            averageQueues(),
            maximumQueues());

        assertEquals(6, results.getWaitingCount());
        assertEquals(2, results.getCrossingCount());
        assertNull(results.getAverageWaitingTimeSeconds());
    }


    /**
     * Tests that modifying input maps after construction does not modify the
     * completed report.
     */
    @Test
    public void testDefensiveCopies()
    {
        Map<Direction, Integer> finals = finalQueues();
        Map<Direction, Double> averages = averageQueues();
        Map<Direction, Integer> maxima = maximumQueues();

        SimulationResults results =
            new SimulationResults(60, 20, 12, 2, 6.5, finals, averages, maxima);

        finals.put(Direction.NORTH, 99);
        averages.put(Direction.NORTH, 99.0);
        maxima.put(Direction.NORTH, 99);

        assertEquals(2, results.getFinalQueueLength(Direction.NORTH));

        assertEquals(
            1.5,
            results.getAverageQueueLength(Direction.NORTH),
            0.0001);

        assertEquals(3, results.getMaximumQueueLength(Direction.NORTH));
    }


    /**
     * Tests that all returned statistics maps are unmodifiable.
     */
    @Test
    public void testUnmodifiableMaps()
    {
        SimulationResults results = validResults();

        try
        {
            results.getFinalQueueLengths().put(Direction.NORTH, 99);
            fail("Expected UnsupportedOperationException");
        }
        catch (UnsupportedOperationException expected)
        {
            // Expected.
        }

        try
        {
            results.getAverageQueueLengths().put(Direction.NORTH, 99.0);
            fail("Expected UnsupportedOperationException");
        }
        catch (UnsupportedOperationException expected)
        {
            // Expected.
        }

        try
        {
            results.getMaximumQueueLengths().put(Direction.NORTH, 99);
            fail("Expected UnsupportedOperationException");
        }
        catch (UnsupportedOperationException expected)
        {
            // Expected.
        }
    }


    /**
     * Tests rejection of null directions in individual queue-statistics
     * getters.
     */
    @Test
    public void testNullDirectionGetters()
    {
        SimulationResults results = validResults();

        assertInvalid(() -> results.getFinalQueueLength(null));

        assertInvalid(() -> results.getAverageQueueLength(null));

        assertInvalid(() -> results.getMaximumQueueLength(null));
    }


    /**
     * Tests rejection of simulation durations outside the established
     * configuration limits.
     */
    @Test
    public void testInvalidDuration()
    {
        assertInvalid(
            () -> new SimulationResults(
                59,
                20,
                12,
                2,
                6.5,
                finalQueues(),
                averageQueues(),
                maximumQueues()));

        assertInvalid(
            () -> new SimulationResults(
                7201,
                20,
                12,
                2,
                6.5,
                finalQueues(),
                averageQueues(),
                maximumQueues()));
    }


    /**
     * Tests rejection of negative arrival, completion, and crossing counts.
     */
    @Test
    public void testNegativeVehicleCounts()
    {
        assertInvalid(
            () -> new SimulationResults(
                60,
                -1,
                12,
                2,
                6.5,
                finalQueues(),
                averageQueues(),
                maximumQueues()));

        assertInvalid(
            () -> new SimulationResults(
                60,
                20,
                -1,
                2,
                6.5,
                finalQueues(),
                averageQueues(),
                maximumQueues()));

        assertInvalid(
            () -> new SimulationResults(
                60,
                20,
                12,
                -1,
                6.5,
                finalQueues(),
                averageQueues(),
                maximumQueues()));
    }


    /**
     * Tests that the total number of arrivals must equal waiting + crossing +
     * completed.
     */
    @Test
    public void testInconsistentVehicleTotals()
    {
        assertInvalid(
            () -> new SimulationResults(
                60,
                19,
                12,
                2,
                6.5,
                finalQueues(),
                averageQueues(),
                maximumQueues()));

        assertInvalid(
            () -> new SimulationResults(
                60,
                21,
                12,
                2,
                6.5,
                finalQueues(),
                averageQueues(),
                maximumQueues()));
    }


    /**
     * Tests rejection of missing, negative, non-finite, or incorrectly defined
     * average waiting times.
     */
    @Test
    public void testInvalidAverageWaitingTime()
    {
        assertInvalid(
            () -> new SimulationResults(
                60,
                20,
                12,
                2,
                null,
                finalQueues(),
                averageQueues(),
                maximumQueues()));

        assertInvalid(
            () -> new SimulationResults(
                60,
                20,
                12,
                2,
                -1.0,
                finalQueues(),
                averageQueues(),
                maximumQueues()));

        assertInvalid(
            () -> new SimulationResults(
                60,
                20,
                12,
                2,
                Double.NaN,
                finalQueues(),
                averageQueues(),
                maximumQueues()));

        assertInvalid(
            () -> new SimulationResults(
                60,
                20,
                12,
                2,
                Double.POSITIVE_INFINITY,
                finalQueues(),
                averageQueues(),
                maximumQueues()));

        // An average is undefined with zero completions.
        assertInvalid(
            () -> new SimulationResults(
                60,
                8,
                0,
                2,
                0.0,
                finalQueues(),
                averageQueues(),
                maximumQueues()));
    }


    /**
     * Tests rejection of null statistics maps.
     */
    @Test
    public void testNullStatisticsMaps()
    {
        assertInvalid(
            () -> new SimulationResults(
                60,
                20,
                12,
                2,
                6.5,
                null,
                averageQueues(),
                maximumQueues()));

        assertInvalid(
            () -> new SimulationResults(
                60,
                20,
                12,
                2,
                6.5,
                finalQueues(),
                null,
                maximumQueues()));

        assertInvalid(
            () -> new SimulationResults(
                60,
                20,
                12,
                2,
                6.5,
                finalQueues(),
                averageQueues(),
                null));
    }


    /**
     * Tests that statistics maps must include all four directions.
     */
    @Test
    public void testIncompleteStatisticsMaps()
    {
        Map<Direction, Integer> finals = finalQueues();
        finals.remove(Direction.WEST);

        assertInvalid(
            () -> new SimulationResults(
                60,
                20,
                12,
                2,
                6.5,
                finals,
                averageQueues(),
                maximumQueues()));

        Map<Direction, Double> averages = averageQueues();
        averages.remove(Direction.WEST);

        assertInvalid(
            () -> new SimulationResults(
                60,
                20,
                12,
                2,
                6.5,
                finalQueues(),
                averages,
                maximumQueues()));

        Map<Direction, Integer> maxima = maximumQueues();
        maxima.remove(Direction.WEST);

        assertInvalid(
            () -> new SimulationResults(
                60,
                20,
                12,
                2,
                6.5,
                finalQueues(),
                averageQueues(),
                maxima));
    }


    /**
     * Tests rejection of null and negative values in the per-direction
     * statistics maps.
     */
    @Test
    public void testInvalidQueueValues()
    {
        Map<Direction, Integer> finals = finalQueues();

        finals.put(Direction.NORTH, null);

        assertInvalid(
            () -> new SimulationResults(
                60,
                20,
                12,
                2,
                6.5,
                finals,
                averageQueues(),
                maximumQueues()));

        finals.put(Direction.NORTH, -1);

        assertInvalid(
            () -> new SimulationResults(
                60,
                20,
                12,
                2,
                6.5,
                finals,
                averageQueues(),
                maximumQueues()));

        Map<Direction, Double> averages = averageQueues();

        averages.put(Direction.NORTH, null);

        assertInvalid(
            () -> new SimulationResults(
                60,
                20,
                12,
                2,
                6.5,
                finalQueues(),
                averages,
                maximumQueues()));

        averages.put(Direction.NORTH, -1.0);

        assertInvalid(
            () -> new SimulationResults(
                60,
                20,
                12,
                2,
                6.5,
                finalQueues(),
                averages,
                maximumQueues()));

        Map<Direction, Integer> maxima = maximumQueues();

        maxima.put(Direction.NORTH, null);

        assertInvalid(
            () -> new SimulationResults(
                60,
                20,
                12,
                2,
                6.5,
                finalQueues(),
                averageQueues(),
                maxima));

        maxima.put(Direction.NORTH, -1);

        assertInvalid(
            () -> new SimulationResults(
                60,
                20,
                12,
                2,
                6.5,
                finalQueues(),
                averageQueues(),
                maxima));
    }


    /**
     * Tests rejection of NaN and infinity in average queue-length statistics.
     */
    @Test
    public void testNonFiniteQueueAverage()
    {
        Map<Direction, Double> averages = averageQueues();

        averages.put(Direction.NORTH, Double.NaN);

        assertInvalid(
            () -> new SimulationResults(
                60,
                20,
                12,
                2,
                6.5,
                finalQueues(),
                averages,
                maximumQueues()));

        averages.put(Direction.NORTH, Double.POSITIVE_INFINITY);

        assertInvalid(
            () -> new SimulationResults(
                60,
                20,
                12,
                2,
                6.5,
                finalQueues(),
                averages,
                maximumQueues()));
    }


    /**
     * Tests that a maximum queue length cannot be smaller than the final queue
     * length.
     */
    @Test
    public void testMaximumBelowFinal()
    {
        Map<Direction, Integer> maxima = maximumQueues();
        maxima.put(Direction.NORTH, 1);

        assertInvalid(
            () -> new SimulationResults(
                60,
                20,
                12,
                2,
                6.5,
                finalQueues(),
                averageQueues(),
                maxima));
    }


    /**
     * Tests that an average queue length cannot exceed its maximum observed
     * length.
     */
    @Test
    public void testAverageAboveMaximum()
    {
        Map<Direction, Double> averages = averageQueues();
        averages.put(Direction.NORTH, 3.5);

        assertInvalid(
            () -> new SimulationResults(
                60,
                20,
                12,
                2,
                6.5,
                finalQueues(),
                averages,
                maximumQueues()));
    }
}
