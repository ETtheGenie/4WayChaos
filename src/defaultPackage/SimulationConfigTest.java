
package defaultPackage;

import static org.junit.Assert.*;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * JUnit 4 tests for V2 SimulationConfig validation, getters, immutable
 * configuration state, and integration with SimulationOptions.
 */
public class SimulationConfigTest
{

    /**
     * Creates a complete valid set of arrival rates.
     *
     * @return a mutable map with all four directions
     */
    private Map<Direction, Integer> makeRates()
    {
        Map<Direction, Integer> rates = new EnumMap<>(Direction.class);
        rates.put(Direction.NORTH, 600);
        rates.put(Direction.SOUTH, 450);
        rates.put(Direction.EAST, 300);
        rates.put(Direction.WEST, 150);
        return rates;
    }


    /**
     * Creates a configuration from supplied values for concise test setup.
     *
     * @param rates
     *            arrival-rate map
     * @param duration
     *            simulation duration
     * @param left
     *            left-turn percentage
     * @param uTurn
     *            U-turn percentage
     * @param options
     *            V2 options
     * @return validated configuration
     */
    private SimulationConfig makeConfig(
        Map<Direction, Integer> rates,
        int duration,
        int left,
        int uTurn,
        SimulationOptions options)
    {
        return new SimulationConfig(rates, duration, left, uTurn, options);
    }


    /**
     * Confirms that an invalid operation rejects its argument.
     *
     * @param operation
     *            operation expected to throw IllegalArgumentException
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
            // Invalid configuration was correctly rejected.
        }
    }


    /**
     * Checks construction and all public getters for a normal configuration.
     */
    @Test
    public void testValidConfigurationAndGetters()
    {
        SimulationOptions options = SimulationOptions.builder()
            .minGreenSeconds(15).randomSeed(99L).build();
        SimulationConfig config = makeConfig(makeRates(), 120, 20, 10, options);

        assertEquals(600, config.getArrivalRatePerHour(Direction.NORTH));
        assertEquals(450, config.getArrivalRatePerHour(Direction.SOUTH));
        assertEquals(300, config.getArrivalRatePerHour(Direction.EAST));
        assertEquals(150, config.getArrivalRatePerHour(Direction.WEST));
        assertEquals(4, config.getArrivalRatesPerHour().size());
        assertEquals(120, config.getDurationSeconds());
        assertEquals(20, config.getLeftTurnPercent());
        assertEquals(10, config.getUTurnPercent());
        assertSame(options, config.getOptions());
        assertEquals(15, config.getOptions().getMinGreenSeconds());
        assertEquals(99L, config.getOptions().getRandomSeed());
    }


    /**
     * Checks the documented static configuration limits.
     */
    @Test
    public void testConfigurationConstants()
    {
        assertEquals(0, SimulationConfig.MIN_ARRIVAL_RATE);
        assertEquals(1800, SimulationConfig.MAX_ARRIVAL_RATE);
        assertEquals(60, SimulationConfig.MIN_DURATION_SECONDS);
        assertEquals(7200, SimulationConfig.MAX_DURATION_SECONDS);
        assertEquals(0, SimulationConfig.MIN_TURN_PERCENT);
        assertEquals(100, SimulationConfig.MAX_TURN_PERCENT);
    }


    /**
     * Tests minimum and maximum allowed arrival rates across all approaches.
     */
    @Test
    public void testArrivalRateBoundaries()
    {
        Map<Direction, Integer> rates = makeRates();
        rates.put(Direction.NORTH, 0);
        rates.put(Direction.SOUTH, 1800);
        rates.put(Direction.EAST, 0);
        rates.put(Direction.WEST, 1800);

        SimulationConfig config =
            makeConfig(rates, 120, 20, 10, SimulationOptions.builder().build());
        assertEquals(0, config.getArrivalRatePerHour(Direction.NORTH));
        assertEquals(1800, config.getArrivalRatePerHour(Direction.SOUTH));
        assertEquals(0, config.getArrivalRatePerHour(Direction.EAST));
        assertEquals(1800, config.getArrivalRatePerHour(Direction.WEST));
    }


    /**
     * Rejects missing, null, and extra direction entries.
     */
    @Test
    public void testInvalidArrivalMaps()
    {
        SimulationOptions options = SimulationOptions.builder().build();
        assertInvalid(() -> makeConfig(null, 120, 20, 10, options));

        Map<Direction, Integer> missing = makeRates();
        missing.remove(Direction.EAST);
        assertInvalid(() -> makeConfig(missing, 120, 20, 10, options));

        Map<Direction, Integer> nullValue = makeRates();
        nullValue.put(Direction.NORTH, null);
        assertInvalid(() -> makeConfig(nullValue, 120, 20, 10, options));

        Map<Direction, Integer> extra = new HashMap<>(makeRates());
        extra.put(null, 10);
        assertInvalid(() -> makeConfig(extra, 120, 20, 10, options));
    }


    /**
     * Rejects arrival rates below zero and above 1800.
     */
    @Test
    public void testOutOfRangeArrivalRates()
    {
        SimulationOptions options = SimulationOptions.builder().build();
        for (Direction direction : Direction.values())
        {
            Map<Direction, Integer> negative = makeRates();
            negative.put(direction, -1);
            assertInvalid(() -> makeConfig(negative, 120, 20, 10, options));

            Map<Direction, Integer> excessive = makeRates();
            excessive.put(direction, 1801);
            assertInvalid(() -> makeConfig(excessive, 120, 20, 10, options));
        }
    }


    /**
     * Verifies inclusive minimum and maximum run duration boundaries.
     */
    @Test
    public void testDurationBoundaries()
    {
        SimulationOptions options = SimulationOptions.builder().build();
        assertEquals(
            60,
            makeConfig(makeRates(), 60, 20, 10, options).getDurationSeconds());
        assertEquals(
            7200,
            makeConfig(makeRates(), 7200, 20, 10, options)
                .getDurationSeconds());
    }


    /**
     * Rejects duration values below or above the permitted range.
     */
    @Test
    public void testInvalidDurations()
    {
        SimulationOptions options = SimulationOptions.builder().build();
        assertInvalid(() -> makeConfig(makeRates(), 59, 20, 10, options));
        assertInvalid(() -> makeConfig(makeRates(), 7201, 20, 10, options));
        assertInvalid(() -> makeConfig(makeRates(), -1, 20, 10, options));
    }


    /**
     * Accepts zero and full-scale probabilities when their sum is valid.
     */
    @Test
    public void testValidTurnPercentageBoundaries()
    {
        SimulationOptions options = SimulationOptions.builder().build();
        SimulationConfig allStraight =
            makeConfig(makeRates(), 120, 0, 0, options);
        assertEquals(0, allStraight.getLeftTurnPercent());
        assertEquals(0, allStraight.getUTurnPercent());

        SimulationConfig allLeft =
            makeConfig(makeRates(), 120, 100, 0, options);
        assertEquals(100, allLeft.getLeftTurnPercent());

        SimulationConfig allUTurn =
            makeConfig(makeRates(), 120, 0, 100, options);
        assertEquals(100, allUTurn.getUTurnPercent());

        SimulationConfig mixed = makeConfig(makeRates(), 120, 65, 35, options);
        assertEquals(65, mixed.getLeftTurnPercent());
        assertEquals(35, mixed.getUTurnPercent());
    }


    /**
     * Rejects negative and over-100 individual turning percentages.
     */
    @Test
    public void testInvalidIndividualTurnPercentages()
    {
        SimulationOptions options = SimulationOptions.builder().build();
        assertInvalid(() -> makeConfig(makeRates(), 120, -1, 0, options));
        assertInvalid(() -> makeConfig(makeRates(), 120, 101, 0, options));
        assertInvalid(() -> makeConfig(makeRates(), 120, 0, -1, options));
        assertInvalid(() -> makeConfig(makeRates(), 120, 0, 101, options));
    }


    /**
     * Rejects a combined left-turn and U-turn probability above 100%.
     */
    @Test
    public void testCombinedTurnPercentages()
    {
        SimulationOptions options = SimulationOptions.builder().build();
        assertInvalid(() -> makeConfig(makeRates(), 120, 80, 21, options));
        assertInvalid(() -> makeConfig(makeRates(), 120, 100, 1, options));
    }


    /**
     * Rejects null options rather than creating a partially configured run.
     */
    @Test
    public void testNullOptionsRejected()
    {
        assertInvalid(() -> makeConfig(makeRates(), 120, 20, 10, null));
    }


    /**
     * Rejects a null direction when reading an arrival rate.
     */
    @Test
    public void testNullDirectionRejected()
    {
        SimulationConfig config = makeConfig(
            makeRates(),
            120,
            20,
            10,
            SimulationOptions.builder().build());
        assertInvalid(() -> config.getArrivalRatePerHour(null));
    }


    /**
     * Tests defensive copying of caller-owned maps after construction.
     */
    @Test
    public void testInputMapIsDefensivelyCopied()
    {
        Map<Direction, Integer> input = makeRates();
        SimulationConfig config =
            makeConfig(input, 120, 20, 10, SimulationOptions.builder().build());

        input.put(Direction.NORTH, 1200);
        input.remove(Direction.EAST);
        assertEquals(600, config.getArrivalRatePerHour(Direction.NORTH));
        assertEquals(300, config.getArrivalRatePerHour(Direction.EAST));
        assertEquals(4, config.getArrivalRatesPerHour().size());
    }


    /**
     * Tests that callers cannot edit the returned arrival-rate map.
     */
    @Test
    public void testReturnedMapCannotBeModified()
    {
        SimulationConfig config = makeConfig(
            makeRates(),
            120,
            20,
            10,
            SimulationOptions.builder().build());
        try
        {
            config.getArrivalRatesPerHour().put(Direction.WEST, 1700);
            fail("Expected UnsupportedOperationException");
        }
        catch (UnsupportedOperationException expected)
        {
            // Read-only map correctly rejected mutation.
        }
        assertEquals(150, config.getArrivalRatePerHour(Direction.WEST));
    }


    /**
     * Tests that builder changes cannot mutate the saved immutable options.
     */
    @Test
    public void testOptionsRemainImmutable()
    {
        SimulationOptions.Builder builder =
            SimulationOptions.builder().minGreenSeconds(15).randomSeed(123L);
        SimulationOptions options = builder.build();
        SimulationConfig config = makeConfig(makeRates(), 120, 20, 10, options);

        builder.minGreenSeconds(25).randomSeed(999L);
        SimulationOptions modifiedOptions = builder.build();
        assertEquals(15, config.getOptions().getMinGreenSeconds());
        assertEquals(123L, config.getOptions().getRandomSeed());
        assertEquals(25, modifiedOptions.getMinGreenSeconds());
        assertEquals(999L, modifiedOptions.getRandomSeed());
    }


    /**
     * Tests that two independent configurations retain their own settings.
     */
    @Test
    public void testIndependentConfigurations()
    {
        SimulationConfig first = makeConfig(
            makeRates(),
            120,
            20,
            10,
            SimulationOptions.builder().randomSeed(7L).build());
        Map<Direction, Integer> changedRates = makeRates();
        changedRates.put(Direction.NORTH, 900);
        SimulationConfig second = makeConfig(
            changedRates,
            300,
            40,
            5,
            SimulationOptions.builder().randomSeed(8L).build());

        assertEquals(600, first.getArrivalRatePerHour(Direction.NORTH));
        assertEquals(120, first.getDurationSeconds());
        assertEquals(7L, first.getOptions().getRandomSeed());
        assertEquals(900, second.getArrivalRatePerHour(Direction.NORTH));
        assertEquals(300, second.getDurationSeconds());
        assertEquals(8L, second.getOptions().getRandomSeed());
    }
}
