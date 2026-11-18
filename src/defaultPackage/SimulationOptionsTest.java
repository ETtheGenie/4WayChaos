
package defaultPackage;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * JUnit 4 tests for SimulationOptions defaults, custom settings, all validation
 * boundaries, and immutable builder results.
 */
public class SimulationOptionsTest
{

    /**
     * Verifies that an operation rejects an invalid configuration.
     *
     * @param operation
     *            action expected to throw IllegalArgumentException
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
            // Expected.
        }
    }


    /**
     * Verifies every documented V2 default value.
     */
    @Test
    public void testDefaults()
    {
        SimulationOptions options = SimulationOptions.builder().build();

        assertEquals(10, options.getMinGreenSeconds());
        assertEquals(60, options.getMaxGreenSeconds());
        assertEquals(3, options.getQueueSwitchThreshold());
        assertEquals(5, options.getEmergencyVehiclePercent());
        assertEquals(30, options.getEventCheckIntervalSeconds());
        assertEquals(5, options.getAccidentChancePercent());
        assertEquals(10, options.getSlowdownChancePercent());
        assertEquals(45, options.getAccidentDurationSeconds());
        assertEquals(60, options.getSlowdownDurationSeconds());
        assertEquals(5, options.getSlowdownSpeedMetersPerSecond());
        assertEquals(4, options.getSlowdownHeadwaySeconds());
        assertEquals(1L, options.getRandomSeed());
    }


    /**
     * Verifies that all builder setters store valid custom values.
     */
    @Test
    public void testCustomConfiguration()
    {
        SimulationOptions options =
            SimulationOptions.builder().minGreenSeconds(15).maxGreenSeconds(90)
                .queueSwitchThreshold(7).emergencyVehiclePercent(20)
                .eventCheckIntervalSeconds(60).accidentChancePercent(25)
                .slowdownChancePercent(35).accidentDurationSeconds(120)
                .slowdownDurationSeconds(150).slowdownSpeedMetersPerSecond(3)
                .slowdownHeadwaySeconds(6).randomSeed(987654321L).build();

        assertEquals(15, options.getMinGreenSeconds());
        assertEquals(90, options.getMaxGreenSeconds());
        assertEquals(7, options.getQueueSwitchThreshold());
        assertEquals(20, options.getEmergencyVehiclePercent());
        assertEquals(60, options.getEventCheckIntervalSeconds());
        assertEquals(25, options.getAccidentChancePercent());
        assertEquals(35, options.getSlowdownChancePercent());
        assertEquals(120, options.getAccidentDurationSeconds());
        assertEquals(150, options.getSlowdownDurationSeconds());
        assertEquals(3, options.getSlowdownSpeedMetersPerSecond());
        assertEquals(6, options.getSlowdownHeadwaySeconds());
        assertEquals(987654321L, options.getRandomSeed());
    }


    /**
     * Tests inclusive green boundaries and rejection outside them.
     */
    @Test
    public void testGreenDurationBoundaries()
    {
        SimulationOptions valid = SimulationOptions.builder().minGreenSeconds(5)
            .maxGreenSeconds(120).build();

        assertEquals(5, valid.getMinGreenSeconds());
        assertEquals(120, valid.getMaxGreenSeconds());

        assertInvalid(
            () -> SimulationOptions.builder().minGreenSeconds(4).build());
        assertInvalid(
            () -> SimulationOptions.builder().minGreenSeconds(121).build());
        assertInvalid(
            () -> SimulationOptions.builder().maxGreenSeconds(4).build());
        assertInvalid(
            () -> SimulationOptions.builder().maxGreenSeconds(121).build());
    }


    /**
     * Tests rejection of a minimum green exceeding the maximum.
     */
    @Test
    public void testInvertedGreenDurations()
    {
        assertInvalid(
            () -> SimulationOptions.builder().minGreenSeconds(61)
                .maxGreenSeconds(60).build());

        SimulationOptions equal = SimulationOptions.builder()
            .minGreenSeconds(30).maxGreenSeconds(30).build();

        assertEquals(30, equal.getMinGreenSeconds());
        assertEquals(30, equal.getMaxGreenSeconds());
    }


    /**
     * Tests valid and invalid queue-switch thresholds.
     */
    @Test
    public void testQueueSwitchThresholdBoundaries()
    {
        assertEquals(
            1,
            SimulationOptions.builder().queueSwitchThreshold(1).build()
                .getQueueSwitchThreshold());

        assertEquals(
            50,
            SimulationOptions.builder().queueSwitchThreshold(50).build()
                .getQueueSwitchThreshold());

        assertInvalid(
            () -> SimulationOptions.builder().queueSwitchThreshold(0).build());
        assertInvalid(
            () -> SimulationOptions.builder().queueSwitchThreshold(51).build());
    }


    /**
     * Tests emergency-vehicle probabilities from zero through 100 percent.
     */
    @Test
    public void testEmergencyVehiclePercentBoundaries()
    {
        assertEquals(
            0,
            SimulationOptions.builder().emergencyVehiclePercent(0).build()
                .getEmergencyVehiclePercent());

        assertEquals(
            100,
            SimulationOptions.builder().emergencyVehiclePercent(100).build()
                .getEmergencyVehiclePercent());

        assertInvalid(
            () -> SimulationOptions.builder().emergencyVehiclePercent(-1)
                .build());
        assertInvalid(
            () -> SimulationOptions.builder().emergencyVehiclePercent(101)
                .build());
    }


    /**
     * Tests inclusive event-check interval limits and invalid values.
     */
    @Test
    public void testEventCheckIntervalBoundaries()
    {
        assertEquals(
            10,
            SimulationOptions.builder().eventCheckIntervalSeconds(10).build()
                .getEventCheckIntervalSeconds());

        assertEquals(
            120,
            SimulationOptions.builder().eventCheckIntervalSeconds(120).build()
                .getEventCheckIntervalSeconds());

        assertInvalid(
            () -> SimulationOptions.builder().eventCheckIntervalSeconds(9)
                .build());
        assertInvalid(
            () -> SimulationOptions.builder().eventCheckIntervalSeconds(121)
                .build());
    }


    /**
     * Tests invalid and valid accident probabilities.
     */
    @Test
    public void testAccidentChanceBoundaries()
    {
        assertEquals(
            100,
            SimulationOptions.builder().accidentChancePercent(100)
                .slowdownChancePercent(0).build().getAccidentChancePercent());

        assertInvalid(
            () -> SimulationOptions.builder().accidentChancePercent(-1)
                .build());
        assertInvalid(
            () -> SimulationOptions.builder().accidentChancePercent(101)
                .build());
    }


    /**
     * Tests invalid and valid slowdown probabilities.
     */
    @Test
    public void testSlowdownChanceBoundaries()
    {
        assertEquals(
            100,
            SimulationOptions.builder().accidentChancePercent(0)
                .slowdownChancePercent(100).build().getSlowdownChancePercent());

        assertInvalid(
            () -> SimulationOptions.builder().slowdownChancePercent(-1)
                .build());
        assertInvalid(
            () -> SimulationOptions.builder().slowdownChancePercent(101)
                .build());
    }


    /**
     * Tests rejection of combined incident probabilities exceeding 100%.
     */
    @Test
    public void testCombinedIncidentProbabilities()
    {
        SimulationOptions valid = SimulationOptions.builder()
            .accidentChancePercent(40).slowdownChancePercent(60).build();

        assertEquals(40, valid.getAccidentChancePercent());
        assertEquals(60, valid.getSlowdownChancePercent());

        assertInvalid(
            () -> SimulationOptions.builder().accidentChancePercent(40)
                .slowdownChancePercent(61).build());
    }


    /**
     * Tests minimum, maximum and invalid accident durations.
     */
    @Test
    public void testAccidentDurationBoundaries()
    {
        assertEquals(
            5,
            SimulationOptions.builder().accidentDurationSeconds(5).build()
                .getAccidentDurationSeconds());

        assertEquals(
            300,
            SimulationOptions.builder().accidentDurationSeconds(300).build()
                .getAccidentDurationSeconds());

        assertInvalid(
            () -> SimulationOptions.builder().accidentDurationSeconds(4)
                .build());
        assertInvalid(
            () -> SimulationOptions.builder().accidentDurationSeconds(301)
                .build());
    }


    /**
     * Tests minimum, maximum and invalid slowdown durations.
     */
    @Test
    public void testSlowdownDurationBoundaries()
    {
        assertEquals(
            5,
            SimulationOptions.builder().slowdownDurationSeconds(5).build()
                .getSlowdownDurationSeconds());

        assertEquals(
            300,
            SimulationOptions.builder().slowdownDurationSeconds(300).build()
                .getSlowdownDurationSeconds());

        assertInvalid(
            () -> SimulationOptions.builder().slowdownDurationSeconds(4)
                .build());
        assertInvalid(
            () -> SimulationOptions.builder().slowdownDurationSeconds(301)
                .build());
    }


    /**
     * Tests valid slowdown speeds and rejection of zero/normal speed.
     */
    @Test
    public void testSlowdownSpeedBoundaries()
    {
        assertEquals(
            1,
            SimulationOptions.builder().slowdownSpeedMetersPerSecond(1).build()
                .getSlowdownSpeedMetersPerSecond());

        assertEquals(
            9,
            SimulationOptions.builder().slowdownSpeedMetersPerSecond(9).build()
                .getSlowdownSpeedMetersPerSecond());

        assertInvalid(
            () -> SimulationOptions.builder().slowdownSpeedMetersPerSecond(0)
                .build());
        assertInvalid(
            () -> SimulationOptions.builder().slowdownSpeedMetersPerSecond(10)
                .build());
    }


    /**
     * Tests valid slowdown headways and invalid headway values.
     */
    @Test
    public void testSlowdownHeadwayBoundaries()
    {
        assertEquals(
            2,
            SimulationOptions.builder().slowdownHeadwaySeconds(2).build()
                .getSlowdownHeadwaySeconds());

        assertEquals(
            20,
            SimulationOptions.builder().slowdownHeadwaySeconds(20).build()
                .getSlowdownHeadwaySeconds());

        assertInvalid(
            () -> SimulationOptions.builder().slowdownHeadwaySeconds(1)
                .build());
        assertInvalid(
            () -> SimulationOptions.builder().slowdownHeadwaySeconds(21)
                .build());
    }


    /**
     * Tests that all long seed values are accepted unchanged.
     */
    @Test
    public void testRandomSeedBoundaries()
    {
        assertEquals(
            Long.MIN_VALUE,
            SimulationOptions.builder().randomSeed(Long.MIN_VALUE).build()
                .getRandomSeed());

        assertEquals(
            Long.MAX_VALUE,
            SimulationOptions.builder().randomSeed(Long.MAX_VALUE).build()
                .getRandomSeed());
    }


    /**
     * Tests that editing a builder does not mutate an existing options object.
     */
    @Test
    public void testBuilderReuseDoesNotMutateBuiltOptions()
    {
        SimulationOptions.Builder builder = SimulationOptions.builder();
        SimulationOptions first = builder.build();

        builder.minGreenSeconds(20).randomSeed(42L);
        SimulationOptions second = builder.build();

        assertEquals(10, first.getMinGreenSeconds());
        assertEquals(1L, first.getRandomSeed());
        assertEquals(20, second.getMinGreenSeconds());
        assertEquals(42L, second.getRandomSeed());
    }


    /**
     * Tests that a builder can recover after a rejected build.
     */
    @Test
    public void testRecoverAfterInvalidBuild()
    {
        SimulationOptions.Builder builder = SimulationOptions.builder()
            .accidentChancePercent(80).slowdownChancePercent(30);

        assertInvalid(() -> builder.build());

        builder.slowdownChancePercent(20);
        SimulationOptions options = builder.build();

        assertEquals(80, options.getAccidentChancePercent());
        assertEquals(20, options.getSlowdownChancePercent());
    }


    /**
     * Tests that disabling both incident types with zero probabilities is
     * valid.
     */
    @Test
    public void testZeroIncidentsAllowed()
    {
        SimulationOptions options =
            SimulationOptions.builder().accidentChancePercent(0)
                .slowdownChancePercent(0).emergencyVehiclePercent(0).build();

        assertEquals(0, options.getAccidentChancePercent());
        assertEquals(0, options.getSlowdownChancePercent());
        assertEquals(0, options.getEmergencyVehiclePercent());
    }
}
