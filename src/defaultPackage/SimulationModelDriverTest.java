
package defaultPackage;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

/**
 * JUnit 4 tests for the standalone V2 model driver. Exercises its public
 * demonstration entry points and verifies diagnostic output against the
 * immutable SimulationResults returned by the model-only run.
 */
public class SimulationModelDriverTest
{

    /**
     * Captures the output and immutable report of one independent driver run.
     */
    private static final class RunCapture
    {
        private final ByteArrayOutputStream bytes;
        private final SimulationResults result;

        /**
         * Runs one fully independent seeded model demonstration.
         */
        RunCapture()
        {
            bytes = new ByteArrayOutputStream();
            PrintStream output = new PrintStream(bytes, true);
            result = SimulationModelDriver.runDemonstration(output);
            output.flush();
        }


        /**
         * Returns the captured demonstration output.
         *
         * @return complete UTF-8 diagnostic text
         */
        String text()
        {
            return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        }


        /**
         * Returns the model report produced by this demonstration.
         *
         * @return immutable completed result
         */
        SimulationResults result()
        {
            return result;
        }
    }

    /**
     * Counts nonoverlapping occurrences of a literal in diagnostic text.
     *
     * @param text
     *            full output to inspect
     * @param needle
     *            literal marker to count
     * @return number of occurrences
     */
    private int countOccurrences(String text, String needle)
    {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(needle, offset)) >= 0)
        {
            count++;
            offset += needle.length();
        }
        return count;
    }


    /**
     * Checks whether a line matching a regular expression occurs in the output.
     *
     * @param text
     *            full demonstration output
     * @param regex
     *            regular expression for one line
     * @return true if at least one matching line exists
     */
    private boolean containsLine(String text, String regex)
    {
        return Pattern.compile(regex, Pattern.MULTILINE).matcher(text).find();
    }


    /**
     * Confirms the driver rejects a null output stream.
     */
    @Test
    public void testNullPrintStreamRejected()
    {
        try
        {
            SimulationModelDriver.runDemonstration(null);
            fail("Expected IllegalArgumentException.");
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("Output"));
        }
    }


    /**
     * Verifies the main entry point runs without interactive input or a view.
     */
    @Test
    public void testMainRunsStandaloneWithoutInput()
    {
        PrintStream previous = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try
        {
            System.setOut(new PrintStream(bytes, true));
            SimulationModelDriver.main(new String[] { "unused" });
        }
        finally
        {
            System.setOut(previous);
        }
        String output = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(output.contains("Standalone model demonstration completed"));
        assertTrue(output.contains("Conservation checks passed: 181"));
    }


    /**
     * Verifies the driver completes a configured 180-second simulation.
     */
    @Test
    public void testDriverReturnsCompletedReport()
    {
        RunCapture run = new RunCapture();
        assertNotNull(run.result());
        assertEquals(180, run.result().getDurationSeconds());
        assertTrue(run.text().contains("FINAL V2 MODEL RESULTS"));
    }


    /**
     * Verifies that four different arrival rates produce exactly 225 cars.
     */
    @Test
    public void testExactConfiguredVehicleArrivals()
    {
        RunCapture run = new RunCapture();
        // 180 seconds * (600 + 900 + 1200 + 1800) / 3600 = 225.
        assertEquals(225, run.result().getTotalArrived());
        assertTrue(run.text().contains("Arrived: 225"));
    }


    /**
     * Verifies final accounting and explicit per-second conservation checks.
     */
    @Test
    public void testConservationAtEveryStepAndAtCompletion()
    {
        RunCapture run = new RunCapture();
        SimulationResults result = run.result();
        long accounted = (long)result.getWaitingCount()
            + result.getCrossingCount() + result.getTotalCompleted();
        assertEquals((long)result.getTotalArrived(), accounted);
        assertTrue(run.text().contains("Vehicle conservation: PASS"));
        assertTrue(
            run.text().contains(
                "Conservation checks passed: 181 snapshots (initial + 180 steps)"));
    }


    /**
     * Verifies the configured nonzero emergency probability affects results.
     */
    @Test
    public void testEmergencyVehicleMetricsAreProduced()
    {
        SimulationResults results = new RunCapture().result();
        assertTrue(results.getTotalEmergencyArrived() > 0);
        assertTrue(results.getTotalEmergencyCompleted() > 0);
        assertTrue(
            results.getTotalEmergencyArrived() <= results.getTotalArrived());
        assertTrue(
            results.getTotalEmergencyCompleted() <= results
                .getTotalCompleted());
        assertEquals(
            results.getTotalEmergencyArrived()
                - results.getTotalEmergencyCompleted(),
            results.getEmergencyUnfinishedCount());
        assertNotNull(results.getAverageEmergencyWaitingTimeSeconds());
    }


    /**
     * Verifies the seeded demonstration produces both accident and slowdown
     * events.
     */
    @Test
    public void testBothIncidentTypesActuallyOccur()
    {
        RunCapture run = new RunCapture();
        assertTrue(run.result().getTotalAccidents() > 0);
        assertTrue(run.result().getTotalSlowdowns() > 0);
        assertTrue(run.text().contains("ACCIDENT"));
        assertTrue(run.text().contains("SLOWDOWN"));
    }


    /**
     * Confirms the adaptive phase is switched multiple times in the scenario.
     */
    @Test
    public void testAdaptiveSignalSwitchesActuallyOccur()
    {
        RunCapture run = new RunCapture();
        assertTrue(run.result().getPhaseSwitchCount() > 0);
        assertTrue(run.text().contains("SIGNAL SWITCH at t="));
        assertTrue(run.text().contains("Signal: EW_GREEN"));
        assertTrue(run.text().contains("Signal: NS_GREEN"));
    }


    /**
     * Verifies configuration output includes every V2 feature and the seed.
     */
    @Test
    public void testCompleteV2ConfigurationPrinted()
    {
        String output = new RunCapture().text();
        assertTrue(output.contains("4WayChaos V2"));
        assertTrue(output.contains("No TerminalView or SimulationController"));
        assertTrue(output.contains("Random turns: LEFT 20%, U_TURN 10%"));
        assertTrue(output.contains("Adaptive green: min 10 s, max 40 s"));
        assertTrue(output.contains("Emergency arrivals: 30%"));
        assertTrue(output.contains("Incident checks: every 10 s"));
        assertTrue(output.contains("accident 25% | slowdown 35%"));
        assertTrue(output.contains("Accident duration: 15 s"));
        assertTrue(output.contains("slowdown duration: 20 s"));
        assertTrue(output.contains("Slowdown speed: 5 m/s"));
        assertTrue(output.contains("slowdown departure headway: 4 s"));
        assertTrue(output.contains("Random seed: 20271118"));
    }


    /**
     * Checks the individual arrival-rate values appear in the demonstration.
     */
    @Test
    public void testAllFourConfiguredArrivalRatesPrinted()
    {
        String output = new RunCapture().text();
        assertTrue(output.contains("NORTH arrivals: 600 vehicles/hour"));
        assertTrue(output.contains("SOUTH arrivals: 900 vehicles/hour"));
        assertTrue(output.contains("EAST arrivals: 1200 vehicles/hour"));
        assertTrue(output.contains("WEST arrivals: 1800 vehicles/hour"));
    }


    /**
     * Checks the pristine initial snapshot and final boundary are both printed.
     */
    @Test
    public void testInitialAndFinalSnapshotsDisplayed()
    {
        String output = new RunCapture().text();
        assertTrue(output.contains("Model snapshot at t=0 s"));
        assertTrue(output.contains("Model snapshot at t=180 s"));
        assertTrue(
            containsLine(
                output,
                "Arrived: 0 \\| Completed: 0 \\| Waiting: 0 \\| Crossing: 0.*"));
    }


    /**
     * Verifies every periodic 30-second progress checkpoint is displayed.
     */
    @Test
    public void testPeriodicThirtySecondSnapshotsDisplayed()
    {
        String output = new RunCapture().text();
        for (int second = 30; second <= 180; second += 30)
        {
            assertTrue(
                "Missing checkpoint t=" + second,
                output.contains("Model snapshot at t=" + second + " s"));
        }
    }


    /**
     * Verifies transitions trigger additional output without printing all
     * steps.
     */
    @Test
    public void testChangeTriggeredSnapshotsWithoutExcessiveOutput()
    {
        String output = new RunCapture().text();
        int snapshotCount =
            countOccurrences(output, "--- Model snapshot at t=");
        assertTrue(
            "Expected event/signal-triggered progress",
            snapshotCount > 7);
        assertTrue("Should not print all 181 snapshots", snapshotCount < 181);
        assertTrue(output.contains("Model snapshot at t=11 s"));
    }


    /**
     * Verifies incident starts and ends are both reported to the console.
     */
    @Test
    public void testIncidentTransitionAnnouncements()
    {
        String output = new RunCapture().text();
        assertTrue(output.contains("INCIDENT START at t="));
        assertTrue(output.contains("INCIDENT END at t="));
        assertTrue(
            containsLine(
                output,
                "INCIDENT START at t=10: NORTH ACCIDENT \\(ends at t=25\\)"));
        assertTrue(
            containsLine(output, "INCIDENT END at t=25: NORTH ACCIDENT"));
    }


    /**
     * Checks that every newly reported incident starts on the chosen ten-second
     * check interval, and that its end matches the configured type duration.
     */
    @Test
    public void testIncidentAnnouncementsRespectSchedulingAndDuration()
    {
        RunCapture run = new RunCapture();
        String output = run.text();
        Pattern pattern = Pattern.compile(
            "INCIDENT START at t=(\\d+): (NORTH|SOUTH|EAST|WEST) "
                + "(ACCIDENT|SLOWDOWN) \\(ends at t=(\\d+)\\)");
        Matcher matcher = pattern.matcher(output);
        int starts = 0;
        while (matcher.find())
        {
            int start = Integer.parseInt(matcher.group(1));
            int end = Integer.parseInt(matcher.group(4));
            assertTrue(start > 0 && start % 10 == 0);
            assertEquals(
                "ACCIDENT".equals(matcher.group(3)) ? 15 : 20,
                end - start);
            starts++;
        }
        assertEquals(
            run.result().getTotalAccidents() + run.result().getTotalSlowdowns(),
            starts);
    }


    /**
     * Checks adaptive change announcements use phase-start seconds rather than
     * the one-second-later snapshot timestamps.
     */
    @Test
    public void testSignalTransitionUsesActualPhaseStartTime()
    {
        String output = new RunCapture().text();
        assertTrue(output.contains("SIGNAL SWITCH at t=10: EW_GREEN"));
        assertTrue(output.contains("Model snapshot at t=11 s"));
        assertTrue(output.contains("phase started t=10 | phase elapsed 1 s"));
    }


    /**
     * Verifies each progress table includes both emergency queues and
     * incidents.
     */
    @Test
    public void testSnapshotTablesShowFourApproachesAndIncidentRemainingTime()
    {
        String output = new RunCapture().text();
        assertTrue(output.contains("Approach   Waiting  Emergency Incident"));
        assertTrue(
            containsLine(output, "NORTH\\s+\\d+\\s+\\d+\\s+ACCIDENT\\s+\\d+s"));
        for (Direction direction : Direction.values())
        {
            assertTrue(
                containsLine(
                    output,
                    direction + "\\s+\\d+\\s+\\d+\\s+"
                        + "(NONE|ACCIDENT|SLOWDOWN)\\s+.*"));
        }
    }


    /**
     * Verifies final printed totals reflect the returned immutable result.
     */
    @Test
    public void testFinalTotalsReflectReturnedReport()
    {
        RunCapture run = new RunCapture();
        SimulationResults result = run.result();
        String output = run.text();
        assertTrue(
            output.contains(
                String.format(
                    Locale.US,
                    "Arrived: %d | Completed: %d | Waiting: %d | Crossing: %d",
                    result.getTotalArrived(),
                    result.getTotalCompleted(),
                    result.getWaitingCount(),
                    result.getCrossingCount())));
        assertTrue(
            output.contains(
                String.format(
                    Locale.US,
                    "Emergency arrived: %d | Completed: %d | Unfinished: %d",
                    result.getTotalEmergencyArrived(),
                    result.getTotalEmergencyCompleted(),
                    result.getEmergencyUnfinishedCount())));
        assertTrue(
            output.contains(
                String.format(
                    Locale.US,
                    "Accidents: %d | Slowdowns: %d | Signal switches: %d",
                    result.getTotalAccidents(),
                    result.getTotalSlowdowns(),
                    result.getPhaseSwitchCount())));
    }


    /**
     * Verifies final output includes waiting means and complete queue
     * statistics.
     */
    @Test
    public void testFinalReportIncludesAveragesAndAllQueueMetrics()
    {
        RunCapture run = new RunCapture();
        String output = run.text();
        assertTrue(output.contains("Average waiting time:"));
        assertTrue(output.contains("Average emergency waiting time:"));
        assertTrue(output.contains("Approach     Final    Average  Maximum"));
        for (Direction direction : Direction.values())
        {
            String row = String.format(
                Locale.US,
                "%-9s %8d %10.2f %8d",
                direction,
                run.result().getFinalQueueLength(direction),
                run.result().getAverageQueueLength(direction),
                run.result().getMaximumQueueLength(direction));
            assertTrue(
                "Missing or incorrect row for " + direction,
                output.contains(row));
        }
    }


    /**
     * Verifies identical seeded runs generate identical reports and text
     * output.
     */
    @Test
    public void testReproducibleReportAndOutput()
    {
        RunCapture first = new RunCapture();
        RunCapture second = new RunCapture();
        assertEquals(first.text(), second.text());
        assertEquals(
            first.result().getTotalArrived(),
            second.result().getTotalArrived());
        assertEquals(
            first.result().getTotalCompleted(),
            second.result().getTotalCompleted());
        assertEquals(
            first.result().getTotalEmergencyArrived(),
            second.result().getTotalEmergencyArrived());
        assertEquals(
            first.result().getTotalAccidents(),
            second.result().getTotalAccidents());
        assertEquals(
            first.result().getTotalSlowdowns(),
            second.result().getTotalSlowdowns());
    }


    /**
     * Verifies numeric output uses decimal points regardless of default locale.
     */
    @Test
    public void testOutputUsesUsNumberFormatting()
    {
        Locale before = Locale.getDefault();
        try
        {
            Locale.setDefault(Locale.FRANCE);
            RunCapture run = new RunCapture();
            assertTrue(
                containsLine(
                    run.text(),
                    "Average waiting time: \\d+\\.\\d{2} seconds"));
            assertFalse(
                containsLine(
                    run.text(),
                    "Average waiting time: \\d+,\\d{2} seconds"));
        }
        finally
        {
            Locale.setDefault(before);
        }
    }


    /**
     * Verifies repeated demonstrations can share an output stream and remain
     * independent, with no simulator state retained between runs.
     */
    @Test
    public void testSequentialRunsSharingOutputRemainIndependent()
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, true);
        SimulationResults first = SimulationModelDriver.runDemonstration(out);
        SimulationResults second = SimulationModelDriver.runDemonstration(out);
        String text = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        assertEquals(
            2,
            countOccurrences(
                text,
                "Standalone model demonstration completed successfully."));
        assertEquals(first.getTotalArrived(), second.getTotalArrived());
        assertEquals(first.getTotalAccidents(), second.getTotalAccidents());
        assertEquals(first.getPhaseSwitchCount(), second.getPhaseSwitchCount());
    }
}
