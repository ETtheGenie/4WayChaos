package defaultPackage;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.EnumMap;
import java.util.Map;
import org.junit.Test;

/**
 * JUnit 4 tests for SimulationConfig. Verifies configuration initialization,
 * parameter validation, boundary conditions, and immutability.
 */
public class SimulationConfigTest
{

    /**
     * Creates a valid set of arrival rates for testing.
     *
     * @return map containing all four directions
     */
    private Map<Direction, Integer> createValidRates()
    {
        Map<Direction, Integer> rates = new EnumMap<>(Direction.class);

        rates.put(Direction.NORTH, 600);
        rates.put(Direction.SOUTH, 400);
        rates.put(Direction.EAST, 800);
        rates.put(Direction.WEST, 500);

        return rates;
    }


    /**
     * Creates a valid default simulation configuration.
     *
     * @return valid SimulationConfig instance
     */
    private SimulationConfig createValidConfig()
    {
        return new SimulationConfig(createValidRates(), 30, 45, 600, 20, 10);
    }


    /**
     * Verifies that an operation throws IllegalArgumentException.
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
        catch (IllegalArgumentException e)
        {
            // Expected exception.
        }
    }


    /**
     * Tests correct initialization and retrieval of all configuration
     * parameters.
     */
    @Test
    public void testConstructorAndGetters()
    {
        SimulationConfig config = createValidConfig();

        assertEquals(600, config.getArrivalRatePerHour(Direction.NORTH));
        assertEquals(400, config.getArrivalRatePerHour(Direction.SOUTH));
        assertEquals(800, config.getArrivalRatePerHour(Direction.EAST));
        assertEquals(500, config.getArrivalRatePerHour(Direction.WEST));

        assertEquals(30, config.getNsGreenSeconds());
        assertEquals(45, config.getEwGreenSeconds());
        assertEquals(600, config.getDurationSeconds());
        assertEquals(20, config.getLeftTurnPercent());
        assertEquals(10, config.getUTurnPercent());
    }


    /**
     * Tests that the arrival-rate map getter returns all four configured
     * directions and their values.
     */
    @Test
    public void testArrivalRatesMapGetter()
    {
        SimulationConfig config = createValidConfig();

        Map<Direction, Integer> rates = config.getArrivalRatesPerHour();

        assertEquals(4, rates.size());

        assertEquals(Integer.valueOf(600), rates.get(Direction.NORTH));

        assertEquals(Integer.valueOf(400), rates.get(Direction.SOUTH));
    }


    /**
     * Tests that zero arrival rates are permitted for approaches without
     * incoming traffic.
     */
    @Test
    public void testZeroArrivalRates()
    {
        Map<Direction, Integer> rates = createValidRates();

        for (Direction direction : Direction.values())
        {
            rates.put(direction, 0);
        }

        SimulationConfig config = new SimulationConfig(rates, 30, 30, 60, 0, 0);

        for (Direction direction : Direction.values())
        {
            assertEquals(0, config.getArrivalRatePerHour(direction));
        }
    }


    /**
     * Tests acceptance of minimum and maximum valid configuration values.
     */
    @Test
    public void testValidBoundaries()
    {
        Map<Direction, Integer> rates = createValidRates();

        rates.put(Direction.NORTH, 0);
        rates.put(Direction.SOUTH, 1800);

        SimulationConfig minConfig =
            new SimulationConfig(rates, 5, 5, 60, 0, 0);

        assertEquals(5, minConfig.getNsGreenSeconds());
        assertEquals(60, minConfig.getDurationSeconds());
        assertEquals(0, minConfig.getLeftTurnPercent());

        SimulationConfig maxConfig =
            new SimulationConfig(rates, 120, 120, 7200, 100, 0);

        assertEquals(120, maxConfig.getNsGreenSeconds());
        assertEquals(7200, maxConfig.getDurationSeconds());
        assertEquals(100, maxConfig.getLeftTurnPercent());
        assertEquals(1800, maxConfig.getArrivalRatePerHour(Direction.SOUTH));
    }


    /**
     * Tests rejection of a null arrival-rate map.
     */
    @Test
    public void testNullArrivalRates()
    {
        assertInvalid(() -> new SimulationConfig(null, 30, 30, 600, 20, 10));
    }


    /**
     * Tests rejection of missing directions and null arrival-rate values.
     */
    @Test
    public void testIncompleteArrivalRates()
    {
        Map<Direction, Integer> missing = createValidRates();
        missing.remove(Direction.WEST);

        assertInvalid(() -> new SimulationConfig(missing, 30, 30, 600, 20, 10));

        Map<Direction, Integer> nullRate = createValidRates();
        nullRate.put(Direction.NORTH, null);

        assertInvalid(
            () -> new SimulationConfig(nullRate, 30, 30, 600, 20, 10));
    }


    /**
     * Tests rejection of negative and excessively large vehicle arrival rates.
     */
    @Test
    public void testInvalidArrivalRates()
    {
        Map<Direction, Integer> rates = createValidRates();

        rates.put(Direction.NORTH, -1);

        assertInvalid(() -> new SimulationConfig(rates, 30, 30, 600, 20, 10));

        rates.put(Direction.NORTH, 1000000);

        assertInvalid(() -> new SimulationConfig(rates, 30, 30, 600, 20, 10));

        rates.put(Direction.NORTH, 1801);

        assertInvalid(() -> new SimulationConfig(rates, 30, 30, 600, 20, 10));
    }


    /**
     * Tests that signal phases outside the permitted duration limits are
     * rejected.
     */
    @Test
    public void testInvalidGreenDurations()
    {
        Map<Direction, Integer> rates = createValidRates();

        assertInvalid(() -> new SimulationConfig(rates, 0, 30, 600, 20, 10));

        assertInvalid(() -> new SimulationConfig(rates, 4, 30, 600, 20, 10));

        assertInvalid(() -> new SimulationConfig(rates, 30, 121, 600, 20, 10));
    }


    /**
     * Tests rejection of simulation durations below the minimum or above the
     * maximum.
     */
    @Test
    public void testInvalidSimulationDuration()
    {
        Map<Direction, Integer> rates = createValidRates();

        assertInvalid(() -> new SimulationConfig(rates, 30, 30, 0, 20, 10));

        assertInvalid(() -> new SimulationConfig(rates, 30, 30, 59, 20, 10));

        assertInvalid(() -> new SimulationConfig(rates, 30, 30, 7201, 20, 10));
    }


    /**
     * Tests rejection of negative or excessive individual turning percentages.
     */
    @Test
    public void testInvalidTurnPercentages()
    {
        Map<Direction, Integer> rates = createValidRates();

        assertInvalid(() -> new SimulationConfig(rates, 30, 30, 600, -1, 10));

        assertInvalid(() -> new SimulationConfig(rates, 30, 30, 600, 20, -1));

        assertInvalid(() -> new SimulationConfig(rates, 30, 30, 600, 101, 0));

        assertInvalid(() -> new SimulationConfig(rates, 30, 30, 600, 0, 101));
    }


    /**
     * Tests rejection of combined turning percentages exceeding 100 percent.
     */
    @Test
    public void testCombinedTurnPercentage()
    {
        Map<Direction, Integer> rates = createValidRates();

        assertInvalid(() -> new SimulationConfig(rates, 30, 30, 600, 70, 40));

        SimulationConfig valid =
            new SimulationConfig(rates, 30, 30, 600, 70, 30);

        assertEquals(70, valid.getLeftTurnPercent());
        assertEquals(30, valid.getUTurnPercent());
    }


    /**
     * Tests that modifying the original arrival-rate map does not change an
     * existing configuration.
     */
    @Test
    public void testDefensiveCopy()
    {
        Map<Direction, Integer> rates = createValidRates();

        SimulationConfig config =
            new SimulationConfig(rates, 30, 30, 600, 20, 10);

        rates.put(Direction.NORTH, 1200);
        rates.remove(Direction.WEST);

        assertEquals(600, config.getArrivalRatePerHour(Direction.NORTH));

        assertEquals(500, config.getArrivalRatePerHour(Direction.WEST));
    }


    /**
     * Tests that the returned arrival-rate map cannot be modified.
     */
    @Test
    public void testUnmodifiableArrivalRates()
    {
        SimulationConfig config = createValidConfig();

        Map<Direction, Integer> rates = config.getArrivalRatesPerHour();

        try
        {
            rates.put(Direction.NORTH, 1200);
            fail("Expected UnsupportedOperationException");
        }
        catch (UnsupportedOperationException e)
        {
            // Expected exception.
        }

        assertEquals(600, config.getArrivalRatePerHour(Direction.NORTH));
    }


    /**
     * Tests rejection of a null direction when requesting an individual arrival
     * rate.
     */
    @Test
    public void testNullDirectionGetter()
    {
        SimulationConfig config = createValidConfig();

        assertInvalid(() -> config.getArrivalRatePerHour(null));
    }
}
