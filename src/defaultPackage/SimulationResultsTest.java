
package defaultPackage;

import static org.junit.Assert.*;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * JUnit 4 tests for V2 SimulationResults: vehicle conservation, emergency
 * subsets, incident and signal metrics, queue statistics, and immutability.
 */
public class SimulationResultsTest
{

    /**
     * Mutable test-only input fixture used to construct reports with one
     * modified setting at a time. Not part of the production model API.
     */
    private static final class ReportInput
    {
        int duration = 120;
        int arrived = 20;
        int completed = 11;
        int crossing = 2;
        Double averageWait = 6.5;
        Map<Direction, Integer> finalQueues = ints(3, 2, 1, 1);
        Map<Direction, Double> averageQueues = doubles(1.5, 1.0, 0.5, 0.5);
        Map<Direction, Integer> maximumQueues = ints(4, 3, 2, 2);
        int emergencyArrived = 6;
        int emergencyCompleted = 4;
        Double emergencyAverageWait = 5.25;
        int accidents = 2;
        int slowdowns = 3;
        int switches = 5;

        /**
         * Constructs the production report using the current test inputs.
         *
         * @return new immutable simulation results
         */
        SimulationResults build()
        {
            return new SimulationResults(
                duration,
                arrived,
                completed,
                crossing,
                averageWait,
                finalQueues,
                averageQueues,
                maximumQueues,
                emergencyArrived,
                emergencyCompleted,
                emergencyAverageWait,
                accidents,
                slowdowns,
                switches);
        }
    }

    /**
     * Builds a map of integer queue sizes in NORTH, SOUTH, EAST, WEST order.
     *
     * @param north
     *            north value
     * @param south
     *            south value
     * @param east
     *            east value
     * @param west
     *            west value
     * @return mutable map with all directions
     */
    private static
        Map<Direction, Integer>
        ints(int north, int south, int east, int west)
    {
        EnumMap<Direction, Integer> map = new EnumMap<>(Direction.class);
        map.put(Direction.NORTH, north);
        map.put(Direction.SOUTH, south);
        map.put(Direction.EAST, east);
        map.put(Direction.WEST, west);
        return map;
    }


    /**
     * Builds a map of average queue lengths in direction order.
     *
     * @param north
     *            north average
     * @param south
     *            south average
     * @param east
     *            east average
     * @param west
     *            west average
     * @return mutable map with all directions
     */
    private static
        Map<Direction, Double>
        doubles(double north, double south, double east, double west)
    {
        EnumMap<Direction, Double> map = new EnumMap<>(Direction.class);
        map.put(Direction.NORTH, north);
        map.put(Direction.SOUTH, south);
        map.put(Direction.EAST, east);
        map.put(Direction.WEST, west);
        return map;
    }


    /**
     * Constructs a valid scenario with queued and crossing vehicles, several
     * completed emergency vehicles, and some incidents and phase switches.
     *
     * @return populated report input fixture
     */
    private ReportInput populated()
    {
        return new ReportInput();
    }


    /**
     * Creates a valid zero-traffic report fixture.
     *
     * @return empty report input fixture
     */
    private ReportInput empty()
    {
        ReportInput input = populated();
        input.arrived = 0;
        input.completed = 0;
        input.crossing = 0;
        input.averageWait = null;
        input.finalQueues = ints(0, 0, 0, 0);
        input.averageQueues = doubles(0, 0, 0, 0);
        input.maximumQueues = ints(0, 0, 0, 0);
        input.emergencyArrived = 0;
        input.emergencyCompleted = 0;
        input.emergencyAverageWait = null;
        input.accidents = 0;
        input.slowdowns = 0;
        input.switches = 0;
        return input;
    }


    /**
     * Confirms that an operation rejects invalid report data.
     *
     * @param action
     *            operation expected to throw IllegalArgumentException
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
            // Expected validation failure.
        }
    }


    /**
     * Confirms a candidate report fixture is rejected by the constructor.
     *
     * @param input
     *            test values expected to violate a validation rule
     */
    private void assertInvalidBuild(ReportInput input)
    {
        assertInvalid(() -> input.build());
    }


    /**
     * Verifies all existing and newly introduced getters on a populated report.
     */
    @Test
    public void testPopulatedResultsAndAllGetters()
    {
        SimulationResults result = populated().build();
        assertEquals(120, result.getDurationSeconds());
        assertEquals(20, result.getTotalArrived());
        assertEquals(11, result.getTotalCompleted());
        assertEquals(7, result.getWaitingCount());
        assertEquals(2, result.getCrossingCount());
        assertEquals(6.5, result.getAverageWaitingTimeSeconds(), 0.000001);
        assertEquals(6, result.getTotalEmergencyArrived());
        assertEquals(4, result.getTotalEmergencyCompleted());
        assertEquals(2, result.getEmergencyUnfinishedCount());
        assertEquals(
            5.25,
            result.getAverageEmergencyWaitingTimeSeconds(),
            0.000001);
        assertEquals(2, result.getTotalAccidents());
        assertEquals(3, result.getTotalSlowdowns());
        assertEquals(5, result.getPhaseSwitchCount());
        assertEquals(4, result.getFinalQueueLengths().size());
        assertEquals(4, result.getAverageQueueLengths().size());
        assertEquals(4, result.getMaximumQueueLengths().size());
        assertEquals(3, result.getFinalQueueLength(Direction.NORTH));
        assertEquals(2, result.getFinalQueueLength(Direction.SOUTH));
        assertEquals(1, result.getFinalQueueLength(Direction.EAST));
        assertEquals(1, result.getFinalQueueLength(Direction.WEST));
        assertEquals(1.5, result.getAverageQueueLength(Direction.NORTH), 0.0);
        assertEquals(1.0, result.getAverageQueueLength(Direction.SOUTH), 0.0);
        assertEquals(0.5, result.getAverageQueueLength(Direction.EAST), 0.0);
        assertEquals(0.5, result.getAverageQueueLength(Direction.WEST), 0.0);
        assertEquals(4, result.getMaximumQueueLength(Direction.NORTH));
        assertEquals(3, result.getMaximumQueueLength(Direction.SOUTH));
        assertEquals(2, result.getMaximumQueueLength(Direction.EAST));
        assertEquals(2, result.getMaximumQueueLength(Direction.WEST));
    }


    /**
     * Tests an empty report with both waiting-time averages undefined.
     */
    @Test
    public void testEmptyResults()
    {
        SimulationResults result = empty().build();
        assertEquals(0, result.getTotalArrived());
        assertEquals(0, result.getTotalCompleted());
        assertEquals(0, result.getWaitingCount());
        assertEquals(0, result.getCrossingCount());
        assertEquals(0, result.getEmergencyUnfinishedCount());
        assertNull(result.getAverageWaitingTimeSeconds());
        assertNull(result.getAverageEmergencyWaitingTimeSeconds());
        assertEquals(0, result.getTotalAccidents());
        assertEquals(0, result.getTotalSlowdowns());
        assertEquals(0, result.getPhaseSwitchCount());
    }


    /**
     * Accepts a run in which all vehicles were emergency vehicles.
     */
    @Test
    public void testAllVehiclesEmergency()
    {
        ReportInput input = empty();
        input.arrived = 2;
        input.completed = 2;
        input.emergencyArrived = 2;
        input.emergencyCompleted = 2;
        input.averageWait = 4.0;
        input.emergencyAverageWait = 4.0;
        SimulationResults result = input.build();
        assertEquals(2, result.getTotalEmergencyCompleted());
        assertEquals(0, result.getEmergencyUnfinishedCount());
    }


    /**
     * Accepts emergency arrivals with no completed emergencies, while ordinary
     * vehicles may have completed.
     */
    @Test
    public void testUnfinishedEmergencyVehicles()
    {
        ReportInput input = populated();
        input.emergencyCompleted = 0;
        input.emergencyAverageWait = null;
        SimulationResults result = input.build();
        assertEquals(6, result.getEmergencyUnfinishedCount());
        assertNull(result.getAverageEmergencyWaitingTimeSeconds());
    }


    /**
     * Tests inclusive minimum and maximum permitted simulation durations.
     */
    @Test
    public void testDurationBoundariesAccepted()
    {
        ReportInput input = populated();
        input.duration = SimulationConfig.MIN_DURATION_SECONDS;
        assertEquals(60, input.build().getDurationSeconds());
        input.duration = SimulationConfig.MAX_DURATION_SECONDS;
        assertEquals(7200, input.build().getDurationSeconds());
    }


    /**
     * Rejects simulation durations outside the original validated range.
     */
    @Test
    public void testInvalidDurations()
    {
        ReportInput input = populated();
        input.duration = 59;
        assertInvalidBuild(input);
        input.duration = 7201;
        assertInvalidBuild(input);
        input.duration = -1;
        assertInvalidBuild(input);
    }


    /**
     * Rejects negative generated, completed, or crossing counts.
     */
    @Test
    public void testNegativeVehicleCounts()
    {
        ReportInput input = populated();
        input.arrived = -1;
        assertInvalidBuild(input);
        input = populated();
        input.completed = -1;
        assertInvalidBuild(input);
        input = populated();
        input.crossing = -1;
        assertInvalidBuild(input);
    }


    /**
     * Rejects missing vehicles or excessive completed/crossing counts under the
     * conservation rule.
     */
    @Test
    public void testVehicleConservationBothDirections()
    {
        ReportInput input = populated();
        input.arrived = 19;
        assertInvalidBuild(input);
        input.arrived = 21;
        assertInvalidBuild(input);
        input.arrived = 20;
        input.crossing = 3;
        assertInvalidBuild(input);
    }


    /**
     * Ensures the conservation check uses long arithmetic for large queues.
     */
    @Test
    public void testVehicleConservationOverflowSafe()
    {
        ReportInput input = empty();
        input.finalQueues = ints(Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0);
        input.maximumQueues = ints(Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0);
        assertInvalidBuild(input);
    }


    /**
     * Rejects absent overall average wait when some vehicles completed.
     */
    @Test
    public void testMissingOverallAverageRejected()
    {
        ReportInput input = populated();
        input.averageWait = null;
        assertInvalidBuild(input);
    }


    /**
     * Rejects an overall waiting-time average without completed vehicles.
     */
    @Test
    public void testUnexpectedOverallAverageRejected()
    {
        ReportInput input = empty();
        input.averageWait = 0.0;
        assertInvalidBuild(input);
    }


    /**
     * Rejects negative, NaN, and infinite overall waiting-time averages.
     */
    @Test
    public void testInvalidOverallAverages()
    {
        ReportInput input = populated();
        input.averageWait = -1.0;
        assertInvalidBuild(input);
        input.averageWait = Double.NaN;
        assertInvalidBuild(input);
        input.averageWait = Double.POSITIVE_INFINITY;
        assertInvalidBuild(input);
        input.averageWait = Double.NEGATIVE_INFINITY;
        assertInvalidBuild(input);
    }


    /**
     * Rejects negative emergency arrival and completion counts.
     */
    @Test
    public void testNegativeEmergencyCounts()
    {
        ReportInput input = populated();
        input.emergencyArrived = -1;
        assertInvalidBuild(input);
        input.emergencyArrived = 6;
        input.emergencyCompleted = -1;
        assertInvalidBuild(input);
    }


    /**
     * Rejects emergency arrivals exceeding all arrivals.
     */
    @Test
    public void testEmergencyArrivalsCannotExceedTotalArrivals()
    {
        ReportInput input = populated();
        input.emergencyArrived = 21;
        assertInvalidBuild(input);
    }


    /**
     * Rejects emergency completions exceeding recorded emergency arrivals.
     */
    @Test
    public void testEmergencyCompletionsCannotExceedEmergencyArrivals()
    {
        ReportInput input = populated();
        input.emergencyCompleted = 7;
        assertInvalidBuild(input);
    }


    /**
     * Rejects emergency completions exceeding all completed vehicles.
     */
    @Test
    public void testEmergencyCompletionsCannotExceedTotalCompletions()
    {
        ReportInput input = populated();
        input.emergencyCompleted = 6;
        input.completed = 5;
        input.finalQueues = ints(6, 4, 2, 1);
        input.maximumQueues = ints(6, 4, 2, 2);
        assertInvalidBuild(input);
    }


    /**
     * Rejects impossible normal-vehicle completion totals even when emergency
     * arrivals and completions each appear individually valid.
     */
    @Test
    public void testNormalCompletionsCannotExceedNormalArrivals()
    {
        ReportInput input = populated();
        input.emergencyArrived = 15;
        input.emergencyCompleted = 4;
        // Normal arrivals = 5, normal completed = 7.
        assertInvalidBuild(input);
    }


    /**
     * Rejects a missing average when at least one emergency completed.
     */
    @Test
    public void testMissingEmergencyAverageRejected()
    {
        ReportInput input = populated();
        input.emergencyAverageWait = null;
        assertInvalidBuild(input);
    }


    /**
     * Rejects a defined emergency average when none completed.
     */
    @Test
    public void testUnexpectedEmergencyAverageRejected()
    {
        ReportInput input = populated();
        input.emergencyCompleted = 0;
        input.emergencyAverageWait = 0.0;
        assertInvalidBuild(input);
    }


    /**
     * Rejects negative, NaN, and infinite emergency waiting-time averages.
     */
    @Test
    public void testInvalidEmergencyAverages()
    {
        ReportInput input = populated();
        input.emergencyAverageWait = -0.1;
        assertInvalidBuild(input);
        input.emergencyAverageWait = Double.NaN;
        assertInvalidBuild(input);
        input.emergencyAverageWait = Double.POSITIVE_INFINITY;
        assertInvalidBuild(input);
        input.emergencyAverageWait = Double.NEGATIVE_INFINITY;
        assertInvalidBuild(input);
    }


    /**
     * Accepts a zero-second waiting average for completed vehicles.
     */
    @Test
    public void testZeroWaitingAveragesAccepted()
    {
        ReportInput input = populated();
        input.averageWait = 0.0;
        input.emergencyAverageWait = 0.0;
        assertEquals(0.0, input.build().getAverageWaitingTimeSeconds(), 0.0);
        assertEquals(
            0.0,
            input.build().getAverageEmergencyWaitingTimeSeconds(),
            0.0);
    }


    /**
     * Rejects negative accident and slowdown counts independently.
     */
    @Test
    public void testNegativeIncidentCounts()
    {
        ReportInput input = populated();
        input.accidents = -1;
        assertInvalidBuild(input);
        input.accidents = 2;
        input.slowdowns = -1;
        assertInvalidBuild(input);
    }


    /**
     * Rejects negative or impossibly large numbers of phase switches.
     */
    @Test
    public void testInvalidPhaseSwitchCounts()
    {
        ReportInput input = populated();
        input.switches = -1;
        assertInvalidBuild(input);
        input.switches = 121;
        assertInvalidBuild(input);
    }


    /**
     * Accepts both zero and the conservative upper bound on phase switches.
     */
    @Test
    public void testPhaseSwitchCountBoundaries()
    {
        ReportInput input = populated();
        input.switches = 0;
        assertEquals(0, input.build().getPhaseSwitchCount());
        input.switches = 120;
        assertEquals(120, input.build().getPhaseSwitchCount());
    }


    /**
     * Rejects a null value for each required statistics map.
     */
    @Test
    public void testNullQueueMaps()
    {
        ReportInput input = populated();
        input.finalQueues = null;
        assertInvalidBuild(input);
        input = populated();
        input.averageQueues = null;
        assertInvalidBuild(input);
        input = populated();
        input.maximumQueues = null;
        assertInvalidBuild(input);
    }


    /**
     * Rejects missing direction entries in any of the queue maps.
     */
    @Test
    public void testMissingQueueMapDirections()
    {
        ReportInput input = populated();
        input.finalQueues.remove(Direction.WEST);
        assertInvalidBuild(input);
        input = populated();
        input.averageQueues.remove(Direction.WEST);
        assertInvalidBuild(input);
        input = populated();
        input.maximumQueues.remove(Direction.WEST);
        assertInvalidBuild(input);
    }


    /**
     * Rejects extra keys in all queue maps, including null HashMap keys.
     */
    @Test
    public void testExtraQueueMapKeys()
    {
        ReportInput input = populated();
        input.finalQueues = new HashMap<>(input.finalQueues);
        input.finalQueues.put(null, 0);
        assertInvalidBuild(input);
        input = populated();
        input.averageQueues = new HashMap<>(input.averageQueues);
        input.averageQueues.put(null, 0.0);
        assertInvalidBuild(input);
        input = populated();
        input.maximumQueues = new HashMap<>(input.maximumQueues);
        input.maximumQueues.put(null, 0);
        assertInvalidBuild(input);
    }


    /**
     * Rejects missing numeric entries in otherwise complete maps.
     */
    @Test
    public void testNullQueueValues()
    {
        ReportInput input = populated();
        input.finalQueues.put(Direction.NORTH, null);
        assertInvalidBuild(input);
        input = populated();
        input.averageQueues.put(Direction.NORTH, null);
        assertInvalidBuild(input);
        input = populated();
        input.maximumQueues.put(Direction.NORTH, null);
        assertInvalidBuild(input);
    }


    /**
     * Rejects negative final, mean, and maximum queue measurements.
     */
    @Test
    public void testNegativeQueueStatistics()
    {
        ReportInput input = populated();
        input.finalQueues.put(Direction.NORTH, -1);
        assertInvalidBuild(input);
        input = populated();
        input.averageQueues.put(Direction.NORTH, -0.1);
        assertInvalidBuild(input);
        input = populated();
        input.maximumQueues.put(Direction.NORTH, -1);
        assertInvalidBuild(input);
    }


    /**
     * Rejects NaN or infinite mean queue lengths on any approach.
     */
    @Test
    public void testNonfiniteQueueAverages()
    {
        ReportInput input = populated();
        input.averageQueues.put(Direction.EAST, Double.NaN);
        assertInvalidBuild(input);
        input.averageQueues.put(Direction.EAST, Double.POSITIVE_INFINITY);
        assertInvalidBuild(input);
        input.averageQueues.put(Direction.EAST, Double.NEGATIVE_INFINITY);
        assertInvalidBuild(input);
    }


    /**
     * Rejects a final queue length larger than the reported maximum.
     */
    @Test
    public void testFinalQueueCannotExceedMaximum()
    {
        ReportInput input = populated();
        input.maximumQueues.put(Direction.NORTH, 2);
        assertInvalidBuild(input);
    }


    /**
     * Rejects average queue lengths larger than the observed maximum.
     */
    @Test
    public void testAverageQueueCannotExceedMaximum()
    {
        ReportInput input = populated();
        input.averageQueues.put(Direction.NORTH, 4.1);
        assertInvalidBuild(input);
    }


    /**
     * Tests exact agreement between averages and maximum queue lengths.
     */
    @Test
    public void testQueueAverageEqualToMaximumAllowed()
    {
        ReportInput input = populated();
        input.averageQueues.put(Direction.NORTH, 4.0);
        assertEquals(
            4.0,
            input.build().getAverageQueueLength(Direction.NORTH),
            0.0);
    }


    /**
     * Confirms that modifications to caller-owned maps cannot alter results.
     */
    @Test
    public void testInputMapsDefensivelyCopied()
    {
        ReportInput input = populated();
        SimulationResults result = input.build();
        input.finalQueues.put(Direction.NORTH, 999);
        input.averageQueues.put(Direction.NORTH, 99.0);
        input.maximumQueues.put(Direction.NORTH, 999);
        assertEquals(3, result.getFinalQueueLength(Direction.NORTH));
        assertEquals(1.5, result.getAverageQueueLength(Direction.NORTH), 0.0);
        assertEquals(4, result.getMaximumQueueLength(Direction.NORTH));
    }


    /**
     * Confirms all three returned queue maps reject mutation.
     */
    @Test
    public void testReturnedMapsUnmodifiable()
    {
        SimulationResults result = populated().build();
        try
        {
            result.getFinalQueueLengths().clear();
            fail("Expected unmodifiable final queue map.");
        }
        catch (UnsupportedOperationException expected)
        {
            // Unmodifiable as required.
        }
        try
        {
            result.getAverageQueueLengths().put(Direction.NORTH, 9.0);
            fail("Expected unmodifiable average queue map.");
        }
        catch (UnsupportedOperationException expected)
        {
            // Unmodifiable as required.
        }
        try
        {
            result.getMaximumQueueLengths().remove(Direction.WEST);
            fail("Expected unmodifiable maximum queue map.");
        }
        catch (UnsupportedOperationException expected)
        {
            // Unmodifiable as required.
        }
        assertEquals(3, result.getFinalQueueLength(Direction.NORTH));
    }


    /**
     * Ensures all direction-specific query methods reject null directions.
     */
    @Test
    public void testNullDirectionQueries()
    {
        SimulationResults result = populated().build();
        assertInvalid(() -> result.getFinalQueueLength(null));
        assertInvalid(() -> result.getAverageQueueLength(null));
        assertInvalid(() -> result.getMaximumQueueLength(null));
    }


    /**
     * Confirms two reports retain independent counts, means, and queues.
     */
    @Test
    public void testIndependentResults()
    {
        SimulationResults populatedResult = populated().build();
        SimulationResults emptyResult = empty().build();
        assertEquals(6, populatedResult.getTotalEmergencyArrived());
        assertEquals(0, emptyResult.getTotalEmergencyArrived());
        assertEquals(2, populatedResult.getTotalAccidents());
        assertEquals(0, emptyResult.getTotalAccidents());
        assertEquals(3, populatedResult.getFinalQueueLength(Direction.NORTH));
        assertEquals(0, emptyResult.getFinalQueueLength(Direction.NORTH));
    }
}
