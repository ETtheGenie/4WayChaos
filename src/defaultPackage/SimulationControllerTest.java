
package defaultPackage;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.junit.Test;

/**
 * JUnit 4 tests for the V2 controller's complete configuration workflow,
 * numeric validation, EOF behavior, run history, progress notifications,
 * reentrancy guards, and view/model integration.
 */
public class SimulationControllerTest
{

    /**
     * Test view that records interactions while using the production
     * TerminalView's Scanner and output formatting.
     */
    private static final class RecordingView
        extends TerminalView
    {
        final List<String> prompts = new ArrayList<>();
        final List<String> errors = new ArrayList<>();
        final List<SimulationSnapshot> progress = new ArrayList<>();
        final List<SimulationResults> results = new ArrayList<>();
        int menus;
        int comparisons;
        SimulationResults comparedPrevious;
        SimulationResults comparedCurrent;
        Runnable firstProgressHook;

        /**
         * Creates a recording view with specified input and output.
         *
         * @param scanner
         *            input source
         * @param output
         *            output destination
         */
        RecordingView(Scanner scanner, PrintStream output)
        {
            super(scanner, output);
        }


        /**
         * Records and delegates a user-input prompt.
         *
         * @param message
         *            prompt displayed to user
         * @return next raw input line or null on EOF
         */
        @Override
        public String prompt(String message)
        {
            prompts.add(message);
            return super.prompt(message);
        }


        /**
         * Records how many main menus were displayed.
         */
        @Override
        public void showMenu()
        {
            menus++;
            super.showMenu();
        }


        /**
         * Records error messages while preserving ordinary terminal output.
         *
         * @param message
         *            error description
         */
        @Override
        public void showError(String message)
        {
            errors.add(message);
            super.showError(message);
        }


        /**
         * Records progress and invokes a one-shot optional test callback.
         *
         * @param snapshot
         *            immutable model snapshot
         */
        @Override
        public void showProgress(SimulationSnapshot snapshot)
        {
            progress.add(snapshot);
            Runnable hook = firstProgressHook;
            firstProgressHook = null;
            if (hook != null)
            {
                hook.run();
            }
        }


        /**
         * Records each completed result delivered to the presentation layer.
         *
         * @param result
         *            final simulation report
         */
        @Override
        public void showResults(SimulationResults result)
        {
            results.add(result);
        }


        /**
         * Records the two reports passed to the comparison view.
         *
         * @param previous
         *            earlier report
         * @param current
         *            more recent report
         */
        @Override
        public void showComparison(
            SimulationResults previous,
            SimulationResults current)
        {
            comparisons++;
            comparedPrevious = previous;
            comparedCurrent = current;
        }
    }


    /**
     * Test fixture holding a controller, its recording view, and output bytes.
     */
    private static final class Fixture
    {
        final ByteArrayOutputStream output;
        final RecordingView view;
        final SimulationController controller;

        /**
         * Creates the fixture from newline-delimited terminal input.
         *
         * @param input
         *            user-entered lines
         */
        Fixture(String input)
        {
            output = new ByteArrayOutputStream();
            Scanner scanner = new Scanner(
                new ByteArrayInputStream(
                    input.getBytes(StandardCharsets.UTF_8)),
                "UTF-8");
            view = new RecordingView(scanner, new PrintStream(output, true));
            controller = new SimulationController(view);
        }
    }

    /**
     * Supplies all nineteen V2 configuration answers in their prompt order.
     * Rates are N=600, S=900, E=1200, W=1800; duration 120; left=20, U-turn=10;
     * green=10..30; threshold=3; emergency=25%; interval=10; accident=25%;
     * slowdown=35%; durations=15/20; speed=5; headway=4; seed=123456.
     *
     * @return newline-terminated valid input sequence
     */
    private String defaultAnswers()
    {
        return answers(
            "600",
            "900",
            "1200",
            "1800",
            "120",
            "20",
            "10",
            "10",
            "30",
            "3",
            "25",
            "10",
            "25",
            "35",
            "15",
            "20",
            "5",
            "4",
            "123456");
    }


    /**
     * Converts individual answers into newline-delimited user input.
     *
     * @param values
     *            one or more prompt answers
     * @return newline-terminated input
     */
    private String answers(String... values)
    {
        StringBuilder text = new StringBuilder();
        for (String value : values)
        {
            text.append(value).append('\n');
        }
        return text.toString();
    }


    /**
     * Returns an independent modifiable array of default answers.
     *
     * @return nineteen answers in controller prompt order
     */
    private String[] answerArray()
    {
        return new String[] { "600", "900", "1200", "1800", "120", "20", "10",
            "10", "30", "3", "25", "10", "25", "35", "15", "20", "5", "4",
            "123456" };
    }


    /**
     * Builds a valid programmatic configuration independent of terminal input.
     *
     * @param rate
     *            same hourly arrival rate for all four directions
     * @param options
     *            scenario behavior settings
     * @return validated V2 simulation configuration for a 60-second run
     */
    private SimulationConfig config(int rate, SimulationOptions options)
    {
        Map<Direction, Integer> rates = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values())
        {
            rates.put(direction, rate);
        }
        return new SimulationConfig(rates, 60, 20, 10, options);
    }


    /**
     * Returns quiet, no-emergency V2 settings for controlled tests.
     *
     * @return immutable options without incidents or emergency arrivals
     */
    private SimulationOptions quietOptions()
    {
        return SimulationOptions.builder().emergencyVehiclePercent(0)
            .accidentChancePercent(0).slowdownChancePercent(0).build();
    }


    /**
     * Reads the selected immutable configuration using reflection solely to
     * verify that every interactive input was stored correctly. This avoids
     * exposing testing-only accessors in the production controller.
     *
     * @param controller
     *            instance whose selected configuration is inspected
     * @return configuration selected for the next run, possibly null
     */
    private SimulationConfig selectedConfig(SimulationController controller)
    {
        try
        {
            Field field =
                SimulationController.class.getDeclaredField("nextConfig");
            field.setAccessible(true);
            return (SimulationConfig)field.get(controller);
        }
        catch (ReflectiveOperationException exception)
        {
            throw new AssertionError(
                "Unable to inspect configuration",
                exception);
        }
    }


    /**
     * Confirms that an operation rejects a null or otherwise invalid argument.
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
     * Verifies controller rejects a null view.
     */
    @Test
    public void testConstructorRejectsNullView()
    {
        assertInvalid(() -> new SimulationController(null));
    }


    /**
     * Verifies a selected configuration cannot be null.
     */
    @Test
    public void testSetConfigurationRejectsNull()
    {
        Fixture f = new Fixture("");
        assertInvalid(() -> f.controller.setConfiguration(null));
        assertNull(selectedConfig(f.controller));
    }


    /**
     * Verifies running without a configuration reports an error, not a crash.
     */
    @Test
    public void testRunRequiresConfiguration()
    {
        Fixture f = new Fixture("");
        f.controller.runSimulation();
        assertEquals(1, f.view.errors.size());
        assertTrue(f.view.errors.get(0).contains("Configure"));
        assertEquals(0, f.view.results.size());
        assertEquals(0, f.view.progress.size());
    }


    /**
     * Verifies comparing without two completed runs gives an error.
     */
    @Test
    public void testCompareRequiresTwoRuns()
    {
        Fixture f = new Fixture("");
        f.controller.compareRuns();
        assertEquals(1, f.view.errors.size());
        assertTrue(f.view.errors.get(0).contains("at least two"));
    }


    /**
     * Verifies the exit command stops after displaying one menu.
     */
    @Test
    public void testMenuExit()
    {
        Fixture f = new Fixture("5\n");
        f.controller.launch();
        assertEquals(1, f.view.menus);
        assertEquals(1, f.view.prompts.size());
        assertEquals(0, f.view.errors.size());
    }


    /**
     * Verifies EOF at the menu exits cleanly without creating a configuration.
     */
    @Test
    public void testMenuEof()
    {
        Fixture f = new Fixture("");
        f.controller.launch();
        assertEquals(1, f.view.menus);
        assertNull(selectedConfig(f.controller));
    }


    /**
     * Verifies invalid menu commands, blank lines, and overlong lines
     * re-prompt.
     */
    @Test
    public void testInvalidMenuInputReprompts()
    {
        String tooLong = new String(new char[81]).replace('\0', '9');
        Fixture f = new Fixture(answers("", "abc", "6", tooLong, " 5 "));
        f.controller.launch();
        assertEquals(5, f.view.menus);
        assertEquals(4, f.view.errors.size());
        assertTrue(f.view.errors.get(3).contains("80"));
    }


    /**
     * Verifies viewing results before any completed run reports an error.
     */
    @Test
    public void testMenuViewResultsBeforeRun()
    {
        Fixture f = new Fixture("3\n5\n");
        f.controller.launch();
        assertEquals(1, f.view.errors.size());
        assertEquals(0, f.view.results.size());
    }


    /**
     * Verifies menu comparison before two runs reports an error.
     */
    @Test
    public void testMenuCompareBeforeRuns()
    {
        Fixture f = new Fixture("4\n5\n");
        f.controller.launch();
        assertEquals(1, f.view.errors.size());
        assertEquals(0, f.view.comparisons);
    }


    /**
     * Confirms all nineteen input values reach the intended configuration and
     * all twelve SimulationOptions settings are preserved correctly.
     */
    @Test
    public void testCompleteCustomConfigurationMapsEveryField()
    {
        Fixture f = new Fixture(defaultAnswers());
        f.controller.configureSimulation();
        SimulationConfig c = selectedConfig(f.controller);
        assertNotNull(c);
        assertEquals(600, c.getArrivalRatePerHour(Direction.NORTH));
        assertEquals(900, c.getArrivalRatePerHour(Direction.SOUTH));
        assertEquals(1200, c.getArrivalRatePerHour(Direction.EAST));
        assertEquals(1800, c.getArrivalRatePerHour(Direction.WEST));
        assertEquals(120, c.getDurationSeconds());
        assertEquals(20, c.getLeftTurnPercent());
        assertEquals(10, c.getUTurnPercent());
        SimulationOptions o = c.getOptions();
        assertEquals(10, o.getMinGreenSeconds());
        assertEquals(30, o.getMaxGreenSeconds());
        assertEquals(3, o.getQueueSwitchThreshold());
        assertEquals(25, o.getEmergencyVehiclePercent());
        assertEquals(10, o.getEventCheckIntervalSeconds());
        assertEquals(25, o.getAccidentChancePercent());
        assertEquals(35, o.getSlowdownChancePercent());
        assertEquals(15, o.getAccidentDurationSeconds());
        assertEquals(20, o.getSlowdownDurationSeconds());
        assertEquals(5, o.getSlowdownSpeedMetersPerSecond());
        assertEquals(4, o.getSlowdownHeadwaySeconds());
        assertEquals(123456L, o.getRandomSeed());
        assertEquals(19, f.view.prompts.size());
        assertTrue(f.view.errors.isEmpty());
    }


    /**
     * Verifies inclusive boundary values for every category of configuration.
     */
    @Test
    public void testValidExtremeConfiguration()
    {
        Fixture f = new Fixture(
            answers(
                "0",
                "1800",
                "0",
                "1800",
                "60",
                "100",
                "0",
                "120",
                "120",
                "50",
                "100",
                "120",
                "100",
                "0",
                "300",
                "300",
                "9",
                "20",
                Long.MIN_VALUE + ""));
        f.controller.configureSimulation();
        SimulationConfig c = selectedConfig(f.controller);
        assertNotNull(c);
        assertEquals(100, c.getLeftTurnPercent());
        assertEquals(0, c.getUTurnPercent());
        assertEquals(120, c.getOptions().getMinGreenSeconds());
        assertEquals(120, c.getOptions().getMaxGreenSeconds());
        assertEquals(100, c.getOptions().getAccidentChancePercent());
        assertEquals(0, c.getOptions().getSlowdownChancePercent());
        assertEquals(Long.MIN_VALUE, c.getOptions().getRandomSeed());
        assertTrue(f.view.errors.isEmpty());
    }


    /**
     * Verifies blanks, text, oversized input, decimal values, and integer
     * overflow are rejected before accepting the first arrival rate.
     */
    @Test
    public void testInvalidIntegerFormatsRepromptWithoutLosingData()
    {
        String tooLong = new String(new char[81]).replace('\0', '8');
        Fixture f = new Fixture(
            answers("", "abc", "1.5", tooLong, "999999999999999999999", "600")
                + answers(
                    "900",
                    "1200",
                    "1800",
                    "120",
                    "20",
                    "10",
                    "10",
                    "30",
                    "3",
                    "25",
                    "10",
                    "25",
                    "35",
                    "15",
                    "20",
                    "5",
                    "4",
                    "123456"));
        f.controller.configureSimulation();
        assertEquals(5, f.view.errors.size());
        assertEquals(
            600,
            selectedConfig(f.controller)
                .getArrivalRatePerHour(Direction.NORTH));
        assertEquals(24, f.view.prompts.size());
    }


    /**
     * Verifies all four arrival-rate prompts reject negative and excessive
     * rates.
     */
    @Test
    public void testArrivalRateOutOfRangeRepromptsEachDirection()
    {
        String[] values = answerArray();
        Fixture f = new Fixture(
            answers(
                "-1",
                values[0],
                "1801",
                values[1],
                "-5",
                values[2],
                "99999",
                values[3])
                + answers(
                    values[4],
                    values[5],
                    values[6],
                    values[7],
                    values[8],
                    values[9],
                    values[10],
                    values[11],
                    values[12],
                    values[13],
                    values[14],
                    values[15],
                    values[16],
                    values[17],
                    values[18]));
        f.controller.configureSimulation();
        assertNotNull(selectedConfig(f.controller));
        assertEquals(4, f.view.errors.size());
    }


    /**
     * Verifies duration limits are enforced by the terminal configuration flow.
     */
    @Test
    public void testInvalidDurationReprompts()
    {
        Fixture f = new Fixture(
            answers(
                "600",
                "900",
                "1200",
                "1800",
                "59",
                "7201",
                "120",
                "20",
                "10",
                "10",
                "30",
                "3",
                "25",
                "10",
                "25",
                "35",
                "15",
                "20",
                "5",
                "4",
                "123456"));
        f.controller.configureSimulation();
        assertEquals(2, f.view.errors.size());
        assertEquals(120, selectedConfig(f.controller).getDurationSeconds());
    }


    /**
     * Verifies U-turn probability cannot exceed the remainder after left turns.
     */
    @Test
    public void testCombinedTurnLimitReprompts()
    {
        Fixture f = new Fixture(
            answers(
                "600",
                "900",
                "1200",
                "1800",
                "120",
                "80",
                "21",
                "20",
                "10",
                "30",
                "3",
                "25",
                "10",
                "25",
                "35",
                "15",
                "20",
                "5",
                "4",
                "123456"));
        f.controller.configureSimulation();
        assertEquals(1, f.view.errors.size());
        assertTrue(f.view.prompts.get(7).contains("0-20"));
        assertEquals(80, selectedConfig(f.controller).getLeftTurnPercent());
        assertEquals(20, selectedConfig(f.controller).getUTurnPercent());
    }


    /**
     * Verifies maximum green cannot be less than entered minimum green.
     */
    @Test
    public void testInvertedAdaptiveGreenLimitsReprompt()
    {
        Fixture f = new Fixture(
            answers(
                "600",
                "900",
                "1200",
                "1800",
                "120",
                "20",
                "10",
                "20",
                "19",
                "20",
                "3",
                "25",
                "10",
                "25",
                "35",
                "15",
                "20",
                "5",
                "4",
                "123456"));
        f.controller.configureSimulation();
        assertEquals(1, f.view.errors.size());
        assertEquals(
            20,
            selectedConfig(f.controller).getOptions().getMaxGreenSeconds());
    }


    /**
     * Verifies incident probabilities cannot sum above 100 percent.
     */
    @Test
    public void testIncidentProbabilitySumReprompts()
    {
        Fixture f = new Fixture(
            answers(
                "600",
                "900",
                "1200",
                "1800",
                "120",
                "20",
                "10",
                "10",
                "30",
                "3",
                "25",
                "10",
                "75",
                "26",
                "25",
                "15",
                "20",
                "5",
                "4",
                "123456"));
        f.controller.configureSimulation();
        assertEquals(1, f.view.errors.size());
        assertTrue(f.view.prompts.get(13).contains("0-25"));
        assertEquals(
            75,
            selectedConfig(f.controller).getOptions()
                .getAccidentChancePercent());
        assertEquals(
            25,
            selectedConfig(f.controller).getOptions()
                .getSlowdownChancePercent());
    }


    /**
     * Verifies each bounded V2 options field rejects a value below the minimum
     * and above the maximum before accepting the original default.
     */
    @Test
    public void testEveryOptionsBoundReprompts()
    {
        int[] indices = { 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17 };
        String[] invalid =
            { "4", "121", "0", "101", "9", "101", "76", "4", "301", "0", "21" };
        for (int i = 0; i < indices.length; i++)
        {
            String[] values = answerArray();
            StringBuilder raw = new StringBuilder();
            for (int j = 0; j < values.length; j++)
            {
                if (j == indices[i])
                {
                    raw.append(invalid[i]).append('\n');
                }
                raw.append(values[j]).append('\n');
            }
            Fixture f = new Fixture(raw.toString());
            f.controller.configureSimulation();
            assertEquals("Field index " + indices[i], 1, f.view.errors.size());
            assertNotNull(selectedConfig(f.controller));
        }
    }


    /**
     * Verifies empty, nonnumeric, decimal, overlength, and overflowing seeds
     * reprompt until Long.MAX_VALUE is entered.
     */
    @Test
    public void testInvalidLongSeedReprompts()
    {
        String[] values = answerArray();
        values[18] = "";
        String tooLong = new String(new char[81]).replace('\0', '1');
        Fixture f = new Fixture(
            answers(values) + answers(
                "banana",
                "1.5",
                tooLong,
                "9223372036854775808",
                Long.MAX_VALUE + ""));
        f.controller.configureSimulation();
        assertEquals(5, f.view.errors.size());
        assertEquals(
            Long.MAX_VALUE,
            selectedConfig(f.controller).getOptions().getRandomSeed());
    }


    /**
     * Verifies EOF during a first interactive configuration leaves it unset.
     */
    @Test
    public void testEofDuringFirstConfigurationDoesNotCommit()
    {
        Fixture f = new Fixture(answers("600", "900"));
        f.controller.configureSimulation();
        assertNull(selectedConfig(f.controller));
        assertEquals(3, f.view.prompts.size());
        assertTrue(f.view.errors.isEmpty());
        f.controller.configureSimulation();
        assertEquals(3, f.view.prompts.size());
    }


    /**
     * Verifies EOF late in setup preserves an earlier programmatic selection.
     */
    @Test
    public void testEofDuringReconfigurationPreservesPrevious()
    {
        Fixture f = new Fixture(
            answers(
                "600",
                "900",
                "1200",
                "1800",
                "120",
                "20",
                "10",
                "10",
                "30",
                "3",
                "25",
                "10",
                "25"));
        SimulationConfig previous = config(600, quietOptions());
        f.controller.setConfiguration(previous);
        f.controller.configureSimulation();
        assertSame(previous, selectedConfig(f.controller));
        f.controller.runSimulation();
        assertEquals(40, f.view.results.get(0).getTotalArrived());
    }


    /**
     * Verifies a successful reconfiguration replaces rather than mutates the
     * previously selected configuration.
     */
    @Test
    public void testSuccessfulReconfigurationReplacesConfiguration()
    {
        Fixture f = new Fixture(defaultAnswers());
        SimulationConfig old = config(0, quietOptions());
        f.controller.setConfiguration(old);
        f.controller.configureSimulation();
        assertNotNull(selectedConfig(f.controller));
        assertTrue(selectedConfig(f.controller) != old);
        assertEquals(0, old.getArrivalRatePerHour(Direction.NORTH));
    }


    /**
     * Verifies first and final snapshots plus results on a quiet 60-second run.
     */
    @Test
    public void testQuietRunShowsInitialAndFinalSnapshots()
    {
        Fixture f = new Fixture("");
        f.controller.setConfiguration(config(0, quietOptions()));
        f.controller.runSimulation();
        assertEquals(2, f.view.progress.size());
        assertEquals(0, f.view.progress.get(0).getElapsedSeconds());
        assertEquals(60, f.view.progress.get(1).getElapsedSeconds());
        assertEquals(1, f.view.results.size());
        assertEquals(0, f.view.results.get(0).getTotalArrived());
    }


    /**
     * Verifies periodic snapshots appear at 60 and 120 seconds, without
     * duplicates at the final boundary.
     */
    @Test
    public void testPeriodicProgressAndFinalAreNotDuplicated()
    {
        Fixture f = new Fixture("");
        Map<Direction, Integer> rates = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values())
        {
            rates.put(direction, 0);
        }
        SimulationOptions options =
            SimulationOptions.builder().minGreenSeconds(120)
                .maxGreenSeconds(120).emergencyVehiclePercent(0)
                .accidentChancePercent(0).slowdownChancePercent(0).build();
        f.controller.setConfiguration(
            new SimulationConfig(rates, 120, 20, 10, options));
        f.controller.runSimulation();
        assertEquals(3, f.view.progress.size());
        assertEquals(0, f.view.progress.get(0).getElapsedSeconds());
        assertEquals(60, f.view.progress.get(1).getElapsedSeconds());
        assertEquals(120, f.view.progress.get(2).getElapsedSeconds());
    }


    /**
     * Verifies each adaptive phase switch is displayed without waiting until
     * the next periodic 60-second checkpoint.
     */
    @Test
    public void testAdaptiveSignalSwitchTriggersProgress()
    {
        Fixture f = new Fixture("");
        SimulationOptions options = SimulationOptions.builder()
            .minGreenSeconds(5).maxGreenSeconds(10).emergencyVehiclePercent(0)
            .accidentChancePercent(0).slowdownChancePercent(0).build();
        f.controller.setConfiguration(config(0, options));
        f.controller.runSimulation();
        int[] expected = { 0, 11, 21, 31, 41, 51, 60 };
        assertEquals(expected.length, f.view.progress.size());
        for (int i = 0; i < expected.length; i++)
        {
            assertEquals(
                expected[i],
                f.view.progress.get(i).getElapsedSeconds());
        }
        assertEquals(5, f.view.results.get(0).getPhaseSwitchCount());
    }


    /**
     * Verifies incident starts and exact expiration boundaries both trigger
     * progress notifications, including incident details.
     */
    @Test
    public void testIncidentStartsAndEndsTriggerProgress()
    {
        Fixture f = new Fixture("");
        SimulationOptions options =
            SimulationOptions.builder().eventCheckIntervalSeconds(10)
                .accidentChancePercent(100).slowdownChancePercent(0)
                .accidentDurationSeconds(5).emergencyVehiclePercent(0).build();
        f.controller.setConfiguration(config(0, options));
        f.controller.runSimulation();
        boolean sawStart = false;
        boolean sawExpiry = false;
        for (SimulationSnapshot s : f.view.progress)
        {
            if (s.getElapsedSeconds() == 11)
            {
                sawStart = true;
                assertEquals(4, s.getActiveEvents().size());
            }
            if (s.getElapsedSeconds() == 15)
            {
                sawExpiry = true;
                assertTrue(s.getActiveEvents().isEmpty());
            }
        }
        assertTrue(sawStart);
        assertTrue(sawExpiry);
        assertEquals(20, f.view.results.get(0).getTotalAccidents());
    }


    /**
     * Verifies a single successful run is insufficient for comparison.
     */
    @Test
    public void testCompareAfterOneRunStillRejected()
    {
        Fixture f = new Fixture("");
        f.controller.setConfiguration(config(0, quietOptions()));
        f.controller.runSimulation();
        f.controller.compareRuns();
        assertEquals(1, f.view.errors.size());
        assertEquals(0, f.view.comparisons);
    }


    /**
     * Verifies the two most recent successful run results are delivered to the
     * comparison method in chronological order.
     */
    @Test
    public void testTwoRunsCompareInCorrectOrder()
    {
        Fixture f = new Fixture("");
        f.controller.setConfiguration(config(0, quietOptions()));
        f.controller.runSimulation();
        f.controller.setConfiguration(config(600, quietOptions()));
        f.controller.runSimulation();
        f.controller.compareRuns();
        assertEquals(1, f.view.comparisons);
        assertSame(f.view.results.get(0), f.view.comparedPrevious);
        assertSame(f.view.results.get(1), f.view.comparedCurrent);
        assertEquals(0, f.view.comparedPrevious.getTotalArrived());
        assertEquals(40, f.view.comparedCurrent.getTotalArrived());
    }


    /**
     * Verifies only the latest two of three successful runs are compared.
     */
    @Test
    public void testThirdRunDisplacesOldestResult()
    {
        Fixture f = new Fixture("");
        f.controller.setConfiguration(config(0, quietOptions()));
        f.controller.runSimulation();
        f.controller.setConfiguration(config(600, quietOptions()));
        f.controller.runSimulation();
        f.controller.setConfiguration(config(900, quietOptions()));
        f.controller.runSimulation();
        f.controller.compareRuns();
        assertSame(f.view.results.get(1), f.view.comparedPrevious);
        assertSame(f.view.results.get(2), f.view.comparedCurrent);
        assertEquals(60, f.view.comparedCurrent.getTotalArrived());
    }


    /**
     * Verifies all five menu options work together, including a completed
     * configuration and two runs followed by comparison.
     */
    @Test
    public void testCompleteMenuWorkflow()
    {
        Fixture f = new Fixture("1\n" + defaultAnswers() + "2\n3\n2\n4\n5\n");
        f.controller.launch();
        assertEquals(3, f.view.results.size());
        assertEquals(1, f.view.comparisons);
        assertTrue(f.view.errors.isEmpty());
        assertEquals(6, f.view.menus);
        assertEquals(
            f.view.results.get(0).getTotalArrived(),
            f.view.results.get(1).getTotalArrived());
    }


    /**
     * Verifies a configuration changed during a run applies to the NEXT run and
     * does not alter the active model.
     */
    @Test
    public void testConfigurationUpdateDuringRunAppliesNextTime()
    {
        Fixture f = new Fixture("");
        f.controller.setConfiguration(config(600, quietOptions()));
        f.view.firstProgressHook =
            () -> f.controller.setConfiguration(config(0, quietOptions()));
        f.controller.runSimulation();
        f.controller.runSimulation();
        assertEquals(40, f.view.results.get(0).getTotalArrived());
        assertEquals(0, f.view.results.get(1).getTotalArrived());
    }


    /**
     * Verifies a reentrant run requested by a view callback is refused.
     */
    @Test
    public void testOverlappingRunIsRejected()
    {
        Fixture f = new Fixture("");
        f.controller.setConfiguration(config(600, quietOptions()));
        f.view.firstProgressHook = () -> f.controller.runSimulation();
        f.controller.runSimulation();
        assertEquals(1, f.view.errors.size());
        assertTrue(f.view.errors.get(0).contains("already running"));
        assertEquals(1, f.view.results.size());
    }


    /**
     * Verifies interactive configuration cannot begin during an active run.
     */
    @Test
    public void testInteractiveConfigureDuringRunIsRejected()
    {
        Fixture f = new Fixture("");
        f.controller.setConfiguration(config(600, quietOptions()));
        f.view.firstProgressHook = () -> f.controller.configureSimulation();
        f.controller.runSimulation();
        assertEquals(1, f.view.errors.size());
        assertTrue(f.view.errors.get(0).contains("Cannot configure"));
        assertTrue(f.view.prompts.isEmpty());
        assertEquals(1, f.view.results.size());
    }


    /**
     * Verifies the running flag is cleared after a view callback throws,
     * allowing a later simulation to finish successfully.
     */
    @Test
    public void testRunningGuardResetsAfterViewException()
    {
        Fixture f = new Fixture("");
        f.controller.setConfiguration(config(600, quietOptions()));
        f.view.firstProgressHook = () -> {
            throw new IllegalStateException("Test display failure");
        };
        try
        {
            f.controller.runSimulation();
            fail("Expected the test view's exception.");
        }
        catch (IllegalStateException expected)
        {
            assertEquals("Test display failure", expected.getMessage());
        }
        assertEquals(0, f.view.results.size());
        f.controller.runSimulation();
        assertEquals(1, f.view.results.size());
    }


    /**
     * Verifies terminal-entered 100% emergencies and accidents are reflected in
     * actual model results, not merely stored configuration fields.
     */
    @Test
    public void testCustomInputFeaturesAffectResults()
    {
        Fixture f = new Fixture(
            answers(
                "600",
                "600",
                "600",
                "600",
                "60",
                "20",
                "10",
                "5",
                "30",
                "3",
                "100",
                "10",
                "100",
                "0",
                "5",
                "5",
                "5",
                "4",
                "-9"));
        f.controller.configureSimulation();
        f.controller.runSimulation();
        SimulationResults result = f.view.results.get(0);
        assertEquals(40, result.getTotalArrived());
        assertEquals(40, result.getTotalEmergencyArrived());
        assertEquals(20, result.getTotalAccidents());
        assertEquals(0, result.getTotalSlowdowns());
        assertTrue(result.getPhaseSwitchCount() > 0);
    }
}
