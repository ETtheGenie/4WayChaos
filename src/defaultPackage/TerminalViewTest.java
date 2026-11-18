
package defaultPackage;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import org.junit.Test;

/**
 * JUnit 4 tests for V2 TerminalView input, progress snapshots, final reports,
 * comparisons, US-locale formatting, and invalid arguments.
 */
public class TerminalViewTest
{

    /**
     * Holds a view with in-memory input/output for one independent test.
     */
    private static final class Fixture
    {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final TerminalView view;

        /**
         * Creates a view using supplied text as its entire input stream.
         *
         * @param input
         *            simulated raw terminal input
         */
        Fixture(String input)
        {
            view = new TerminalView(
                new Scanner(input),
                new PrintStream(output, true));
        }


        /**
         * Returns all output printed by the view so far.
         *
         * @return captured terminal output
         */
        String text()
        {
            return output.toString();
        }
    }

    /**
     * Creates a four-direction integer map in compass order.
     *
     * @param north
     *            north value
     * @param south
     *            south value
     * @param east
     *            east value
     * @param west
     *            west value
     * @return mutable complete map
     */
    private
        Map<Direction, Integer>
        counts(int north, int south, int east, int west)
    {
        Map<Direction, Integer> map = new EnumMap<>(Direction.class);
        map.put(Direction.NORTH, north);
        map.put(Direction.SOUTH, south);
        map.put(Direction.EAST, east);
        map.put(Direction.WEST, west);
        return map;
    }


    /**
     * Creates a complete map of average queue lengths.
     *
     * @param north
     *            north mean
     * @param south
     *            south mean
     * @param east
     *            east mean
     * @param west
     *            west mean
     * @return mutable complete map
     */
    private
        Map<Direction, Double>
        averages(double north, double south, double east, double west)
    {
        Map<Direction, Double> map = new EnumMap<>(Direction.class);
        map.put(Direction.NORTH, north);
        map.put(Direction.SOUTH, south);
        map.put(Direction.EAST, east);
        map.put(Direction.WEST, west);
        return map;
    }


    /**
     * Creates the zero-traffic, initial signal snapshot.
     *
     * @return a valid initial snapshot
     */
    private SimulationSnapshot initialSnapshot()
    {
        return new SimulationSnapshot(
            0,
            SignalPhase.NS_GREEN,
            0,
            0,
            counts(0, 0, 0, 0),
            counts(0, 0, 0, 0),
            new EnumMap<Direction, TrafficEvent>(Direction.class),
            0,
            0,
            0);
    }


    /**
     * Creates a populated V2 snapshot at second 70, including incidents.
     *
     * @return valid populated snapshot with 10 waiting, 2 crossing, 5 done
     */
    private SimulationSnapshot populatedSnapshot()
    {
        Map<Direction, TrafficEvent> incidents = new EnumMap<>(Direction.class);
        incidents.put(
            Direction.NORTH,
            new TrafficEvent(
                TrafficEventType.ACCIDENT,
                Direction.NORTH,
                60,
                45));
        incidents.put(
            Direction.EAST,
            new TrafficEvent(
                TrafficEventType.SLOWDOWN,
                Direction.EAST,
                65,
                60));
        return new SimulationSnapshot(
            70,
            SignalPhase.EW_GREEN,
            60,
            3,
            counts(4, 3, 2, 1),
            counts(2, 1, 0, 0),
            incidents,
            17,
            5,
            2);
    }


    /**
     * Constructs a report with 7 queued and 2 crossing vehicles, with a
     * configurable completion count and waiting averages.
     *
     * @param completed
     *            all completed vehicles
     * @param mean
     *            overall average, or null if no completions
     * @param emergencyCompleted
     *            emergency vehicles completed
     * @param emergencyMean
     *            emergency average, or null if none completed
     * @param accidents
     *            number of accidents started
     * @param slowdowns
     *            number of slowdowns started
     * @param switches
     *            number of signal switches
     * @return immutable, valid completed report
     */
    private SimulationResults report(
        int completed,
        Double mean,
        int emergencyCompleted,
        Double emergencyMean,
        int accidents,
        int slowdowns,
        int switches)
    {
        return new SimulationResults(
            120,
            completed + 9,
            completed,
            2,
            mean,
            counts(3, 2, 1, 1),
            averages(1.5, 1.0, 0.5, 0.25),
            counts(4, 3, 2, 2),
            6,
            emergencyCompleted,
            emergencyMean,
            accidents,
            slowdowns,
            switches);
    }


    /**
     * Creates a report containing no vehicles or incidents.
     *
     * @return zero-traffic report with undefined waiting averages
     */
    private SimulationResults emptyReport()
    {
        return new SimulationResults(
            60,
            0,
            0,
            0,
            null,
            counts(0, 0, 0, 0),
            averages(0, 0, 0, 0),
            counts(0, 0, 0, 0),
            0,
            0,
            null,
            0,
            0,
            0);
    }


    /**
     * Asserts that an action rejects invalid input.
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
            // Invalid input was rejected as designed.
        }
    }


    /**
     * Confirms that custom input and output resources cannot be null.
     */
    @Test
    public void testConstructorRejectsNullResources()
    {
        assertInvalid(
            () -> new TerminalView(
                null,
                new PrintStream(new ByteArrayOutputStream())));
        assertInvalid(() -> new TerminalView(new Scanner(""), null));
    }


    /**
     * Confirms raw prompt input retains spaces, blank lines, and mixed text.
     */
    @Test
    public void testPromptPreservesRawInput()
    {
        Fixture f = new Fixture(" 600 \n\nabc 123\n");
        assertEquals(" 600 ", f.view.prompt("Rate: "));
        assertEquals("", f.view.prompt("Again: "));
        assertEquals("abc 123", f.view.prompt("Value: "));
        assertEquals("Rate: Again: Value: ", f.text());
    }


    /**
     * Confirms end-of-file returns null while still showing the prompt.
     */
    @Test
    public void testPromptAtEndOfFile()
    {
        Fixture f = new Fixture("one\n");
        assertEquals("one", f.view.prompt("First: "));
        assertNull(f.view.prompt("Second: "));
        assertNull(f.view.prompt("Third: "));
        assertEquals("First: Second: Third: ", f.text());
    }


    /**
     * Confirms a null prompt is rejected without consuming input.
     */
    @Test
    public void testNullPromptDoesNotConsumeInput()
    {
        Fixture f = new Fixture("5\n");
        assertInvalid(() -> f.view.prompt(null));
        assertEquals("5", f.view.prompt("Choice: "));
    }


    /**
     * Confirms the original five menu options remain available in V2.
     */
    @Test
    public void testMenuDisplaysAllFiveOptions()
    {
        Fixture f = new Fixture("");
        f.view.showMenu();
        String text = f.text();
        assertTrue(text.contains("4WayChaos V2"));
        assertTrue(text.contains("1. Configure simulation"));
        assertTrue(text.contains("2. Run simulation"));
        assertTrue(text.contains("3. View latest results"));
        assertTrue(text.contains("4. Compare last two runs"));
        assertTrue(text.contains("5. Exit"));
    }


    /**
     * Confirms repeated menu rendering is independent of user input.
     */
    @Test
    public void testMenuCanBeDisplayedRepeatedly()
    {
        Fixture f = new Fixture("5\n");
        f.view.showMenu();
        f.view.showMenu();
        assertEquals("5", f.view.prompt("Choice: "));
        assertTrue(
            f.text().indexOf("4WayChaos V2") != f.text()
                .lastIndexOf("4WayChaos V2"));
    }


    /**
     * Confirms the controller's error message receives the Error prefix.
     */
    @Test
    public void testShowError()
    {
        Fixture f = new Fixture("");
        f.view.showError("Invalid probability");
        assertTrue(f.text().contains("Error: Invalid probability"));
    }


    /**
     * Confirms even an empty non-null message is valid error text.
     */
    @Test
    public void testShowEmptyErrorMessage()
    {
        Fixture f = new Fixture("");
        f.view.showError("");
        assertTrue(f.text().contains("Error: "));
    }


    /**
     * Confirms an absent error message is rejected.
     */
    @Test
    public void testNullErrorRejected()
    {
        Fixture f = new Fixture("");
        assertInvalid(() -> f.view.showError(null));
    }


    /**
     * Confirms initial progress uses the existing NS phase and zero metrics.
     */
    @Test
    public void testInitialProgressShowsZeroState()
    {
        Fixture f = new Fixture("");
        f.view.showProgress(initialSnapshot());
        String text = f.text();
        assertTrue(text.contains("Simulation time: 0 seconds"));
        assertTrue(text.contains("Signal phase: NS_GREEN"));
        assertTrue(text.contains("Phase elapsed: 0 s | Phase switches: 0"));
        assertTrue(
            text.contains(
                "Arrived: 0 | Completed: 0" + " | Waiting: 0 | Crossing: 0"));
        assertTrue(text.contains("Emergency vehicles waiting: 0"));
        assertFalse(text.contains("Next signal phase"));
    }


    /**
     * Confirms the latest applied phase and its timing are shown correctly.
     */
    @Test
    public void testProgressDisplaysAdaptiveSignalTiming()
    {
        Fixture f = new Fixture("");
        f.view.showProgress(populatedSnapshot());
        assertTrue(f.text().contains("Signal phase: EW_GREEN"));
        assertTrue(
            f.text().contains("Phase elapsed: 10 s | Phase switches: 3"));
        assertFalse(f.text().contains("Next signal phase"));
    }


    /**
     * Confirms vehicle conservation totals and emergency waiting count display.
     */
    @Test
    public void testProgressDisplaysAllVehicleCounts()
    {
        Fixture f = new Fixture("");
        f.view.showProgress(populatedSnapshot());
        assertTrue(
            f.text().contains(
                "Arrived: 17 | Completed: 5" + " | Waiting: 10 | Crossing: 2"));
        assertTrue(f.text().contains("Emergency vehicles waiting: 3"));
    }


    /**
     * Confirms every direction appears in the waiting/emergency/incident table.
     */
    @Test
    public void testProgressContainsFourApproachRows()
    {
        Fixture f = new Fixture("");
        f.view.showProgress(populatedSnapshot());
        String text = f.text();
        assertTrue(text.contains("Approach status:"));
        assertTrue(text.contains("Waiting"));
        assertTrue(text.contains("Emergency"));
        assertTrue(text.contains("Incident"));
        assertTrue(text.contains("Remaining"));
        for (Direction direction : Direction.values())
        {
            assertTrue(text.contains(direction.toString()));
        }
    }


    /**
     * Confirms accidents show their type, affected direction, and remaining
     * time.
     */
    @Test
    public void testProgressShowsAccidentRemainingTime()
    {
        Fixture f = new Fixture("");
        f.view.showProgress(populatedSnapshot());
        String text = f.text();
        assertTrue(text.matches("(?s).*NORTH\\s+4\\s+2\\s+ACCIDENT\\s+35s.*"));
    }


    /**
     * Confirms slowdowns and unaffected approaches are reported distinctly.
     */
    @Test
    public void testProgressShowsSlowdownAndNone()
    {
        Fixture f = new Fixture("");
        f.view.showProgress(populatedSnapshot());
        String text = f.text();
        assertTrue(text.matches("(?s).*EAST\\s+2\\s+0\\s+SLOWDOWN\\s+55s.*"));
        assertTrue(text.matches("(?s).*SOUTH\\s+3\\s+1\\s+NONE\\s+-.*"));
        assertTrue(text.matches("(?s).*WEST\\s+1\\s+0\\s+NONE\\s+-.*"));
    }


    /**
     * Confirms zero-traffic progress shows no incidents or emergency cars.
     */
    @Test
    public void testProgressWithoutIncidents()
    {
        Fixture f = new Fixture("");
        f.view.showProgress(initialSnapshot());
        String text = f.text();
        for (Direction direction : Direction.values())
        {
            assertTrue(
                text.matches(
                    "(?s).*" + direction + "\\s+0\\s+0\\s+NONE\\s+-.*"));
        }
    }


    /**
     * Confirms progress rendering does not alter the provided snapshot.
     */
    @Test
    public void testProgressDoesNotMutateSnapshot()
    {
        Fixture f = new Fixture("");
        SimulationSnapshot snapshot = populatedSnapshot();
        f.view.showProgress(snapshot);
        f.view.showProgress(snapshot);
        assertEquals(17, snapshot.getTotalArrived());
        assertEquals(3, snapshot.getEmergencyWaitingCount());
        assertEquals(2, snapshot.getActiveEvents().size());
    }


    /**
     * Confirms null progress input is rejected.
     */
    @Test
    public void testNullProgressRejected()
    {
        Fixture f = new Fixture("");
        assertInvalid(() -> f.view.showProgress(null));
    }


    /**
     * Confirms ordinary totals and overall mean wait in the final report.
     */
    @Test
    public void testResultsDisplayOverallMetrics()
    {
        Fixture f = new Fixture("");
        f.view.showResults(report(11, 6.5, 4, 5.25, 2, 3, 5));
        String text = f.text();
        assertTrue(text.contains("Final Simulation Results"));
        assertTrue(text.contains("Duration: 120 seconds"));
        assertTrue(text.contains("Arrived: 20"));
        assertTrue(text.contains("Completed: 11"));
        assertTrue(text.contains("Waiting: 7"));
        assertTrue(text.contains("Crossing: 2"));
        assertTrue(text.contains("Average waiting time: 6.50 seconds"));
    }


    /**
     * Confirms all emergency totals and emergency waiting time appear.
     */
    @Test
    public void testResultsDisplayEmergencyMetrics()
    {
        Fixture f = new Fixture("");
        f.view.showResults(report(11, 6.5, 4, 5.25, 2, 3, 5));
        String text = f.text();
        assertTrue(text.contains("Emergency vehicle statistics:"));
        assertTrue(text.contains("Emergency arrived: 6"));
        assertTrue(text.contains("Emergency completed: 4"));
        assertTrue(text.contains("Emergency unfinished: 2"));
        assertTrue(
            text.contains("Average emergency waiting time: 5.25 seconds"));
    }


    /**
     * Confirms accident/slowdown totals and phase switches are displayed.
     */
    @Test
    public void testResultsDisplayIncidentsAndSignalStats()
    {
        Fixture f = new Fixture("");
        f.view.showResults(report(11, 6.5, 4, 5.25, 2, 3, 5));
        String text = f.text();
        assertTrue(text.contains("Incidents and adaptive signals:"));
        assertTrue(text.contains("Accidents: 2"));
        assertTrue(text.contains("Slowdowns: 3"));
        assertTrue(text.contains("Signal phase switches: 5"));
    }


    /**
     * Confirms final, average, and maximum queues for all directions.
     */
    @Test
    public void testResultsDisplayQueueStatisticsTable()
    {
        Fixture f = new Fixture("");
        f.view.showResults(report(11, 6.5, 4, 5.25, 2, 3, 5));
        String text = f.text();
        assertTrue(text.contains("Queue statistics (waiting vehicles):"));
        assertTrue(text.contains("Final"));
        assertTrue(text.contains("Average"));
        assertTrue(text.contains("Maximum"));
        assertTrue(text.matches("(?s).*NORTH\\s+3\\s+1\\.50\\s+4.*"));
        assertTrue(text.matches("(?s).*SOUTH\\s+2\\s+1\\.00\\s+3.*"));
        assertTrue(text.matches("(?s).*EAST\\s+1\\s+0\\.50\\s+2.*"));
        assertTrue(text.matches("(?s).*WEST\\s+1\\s+0\\.25\\s+2.*"));
    }


    /**
     * Confirms both waiting-time means show N/A when no cars completed.
     */
    @Test
    public void testResultsWithoutAnyCompletedVehicles()
    {
        Fixture f = new Fixture("");
        f.view.showResults(emptyReport());
        String text = f.text();
        assertTrue(text.contains("Average waiting time: N/A"));
        assertTrue(text.contains("Average emergency waiting time: N/A"));
        assertTrue(text.contains("Emergency unfinished: 0"));
    }


    /**
     * Confirms emergency waiting average is undefined independently of the
     * overall waiting average when no emergency vehicles have completed.
     */
    @Test
    public void testResultsWithOnlyNormalCompletions()
    {
        Fixture f = new Fixture("");
        f.view.showResults(report(11, 6.5, 0, null, 2, 3, 5));
        assertTrue(f.text().contains("Average waiting time: 6.50 seconds"));
        assertTrue(f.text().contains("Average emergency waiting time: N/A"));
        assertTrue(f.text().contains("Emergency unfinished: 6"));
    }


    /**
     * Confirms a report with no events still labels each count correctly.
     */
    @Test
    public void testResultsWithZeroIncidents()
    {
        Fixture f = new Fixture("");
        f.view.showResults(emptyReport());
        assertTrue(f.text().contains("Accidents: 0"));
        assertTrue(f.text().contains("Slowdowns: 0"));
        assertTrue(f.text().contains("Signal phase switches: 0"));
    }


    /**
     * Confirms numeric formatting retains decimal points under another locale.
     */
    @Test
    public void testResultsFormattingIsLocaleIndependent()
    {
        Locale previous = Locale.getDefault();
        try
        {
            Locale.setDefault(Locale.FRANCE);
            Fixture f = new Fixture("");
            f.view.showResults(report(11, 6.5, 4, 5.25, 2, 3, 5));
            assertTrue(f.text().contains("6.50 seconds"));
            assertTrue(f.text().contains("5.25 seconds"));
            assertTrue(f.text().contains("1.50"));
            assertFalse(f.text().contains("6,50 seconds"));
        }
        finally
        {
            Locale.setDefault(previous);
        }
    }


    /**
     * Confirms the final report cannot be mutated by displaying it.
     */
    @Test
    public void testResultsAreNotModifiedByDisplay()
    {
        Fixture f = new Fixture("");
        SimulationResults results = report(11, 6.5, 4, 5.25, 2, 3, 5);
        f.view.showResults(results);
        f.view.showResults(results);
        assertEquals(11, results.getTotalCompleted());
        assertEquals(6, results.getTotalEmergencyArrived());
        assertEquals(3, results.getTotalSlowdowns());
        assertEquals(3, results.getFinalQueueLength(Direction.NORTH));
    }


    /**
     * Confirms missing final results are rejected.
     */
    @Test
    public void testNullResultsRejected()
    {
        Fixture f = new Fixture("");
        assertInvalid(() -> f.view.showResults(null));
    }


    /**
     * Confirms the side-by-side comparison contains all ordinary metrics.
     */
    @Test
    public void testComparisonContainsOverallMetrics()
    {
        Fixture f = new Fixture("");
        f.view.showComparison(
            report(11, 6.5, 4, 5.25, 2, 3, 5),
            report(14, 4.0, 5, 3.75, 5, 1, 10));
        String text = f.text();
        assertTrue(text.contains("Simulation Comparison"));
        assertTrue(text.contains("Previous"));
        assertTrue(text.contains("Current"));
        assertTrue(text.contains("Duration (s)"));
        assertTrue(text.contains("Mean wait (s)"));
        assertTrue(text.contains("6.50"));
        assertTrue(text.contains("4.00"));
        assertTrue(text.contains("Arrived"));
        assertTrue(text.contains("Completed"));
        assertTrue(text.contains("Waiting"));
        assertTrue(text.contains("Crossing"));
    }


    /**
     * Confirms comparison includes every emergency, event, and signal metric.
     */
    @Test
    public void testComparisonContainsNewV2Metrics()
    {
        Fixture f = new Fixture("");
        f.view.showComparison(
            report(11, 6.5, 4, 5.25, 2, 3, 5),
            report(14, 4.0, 5, 3.75, 5, 1, 10));
        String text = f.text();
        assertTrue(text.contains("Emergency arrived"));
        assertTrue(text.contains("Emergency completed"));
        assertTrue(text.contains("Emergency unfinished"));
        assertTrue(text.contains("Emergency mean wait (s)"));
        assertTrue(text.contains("Accidents"));
        assertTrue(text.contains("Slowdowns"));
        assertTrue(text.contains("Signal phase switches"));
        assertTrue(text.contains("5.25"));
        assertTrue(text.contains("3.75"));
    }


    /**
     * Confirms the comparison includes final, mean, and peak queues in all
     * lanes.
     */
    @Test
    public void testComparisonContainsAllQueueMetrics()
    {
        Fixture f = new Fixture("");
        f.view.showComparison(
            report(11, 6.5, 4, 5.25, 2, 3, 5),
            report(14, 4.0, 5, 3.75, 5, 1, 10));
        String text = f.text();
        for (Direction direction : Direction.values())
        {
            assertTrue(text.contains(direction + " final queue"));
            assertTrue(text.contains(direction + " avg queue"));
            assertTrue(text.contains(direction + " max queue"));
        }
    }


    /**
     * Confirms an undefined mean appears as N/A in the correct report column.
     */
    @Test
    public void testComparisonHandlesUnavailableAverages()
    {
        Fixture f = new Fixture("");
        f.view.showComparison(emptyReport(), report(11, 6.5, 0, null, 2, 3, 5));
        String text = f.text();
        assertTrue(text.matches("(?s).*Mean wait \\(s\\)\\s+N/A\\s+6\\.50.*"));
        assertTrue(
            text.matches("(?s).*Emergency mean wait \\(s\\)\\s+N/A\\s+N/A.*"));
    }


    /**
     * Confirms the comparison does not mutate either input report.
     */
    @Test
    public void testComparisonDoesNotMutateReports()
    {
        Fixture f = new Fixture("");
        SimulationResults first = report(11, 6.5, 4, 5.25, 2, 3, 5);
        SimulationResults second = report(14, 4.0, 5, 3.75, 5, 1, 10);
        f.view.showComparison(first, second);
        assertEquals(11, first.getTotalCompleted());
        assertEquals(14, second.getTotalCompleted());
        assertEquals(2, first.getTotalAccidents());
        assertEquals(5, second.getTotalAccidents());
        assertEquals(5, first.getPhaseSwitchCount());
        assertEquals(10, second.getPhaseSwitchCount());
    }


    /**
     * Confirms both comparison argument positions are validated.
     */
    @Test
    public void testComparisonRejectsNullReports()
    {
        Fixture f = new Fixture("");
        SimulationResults valid = emptyReport();
        assertInvalid(() -> f.view.showComparison(null, valid));
        assertInvalid(() -> f.view.showComparison(valid, null));
        assertInvalid(() -> f.view.showComparison(null, null));
    }
}
