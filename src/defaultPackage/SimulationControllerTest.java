package defaultPackage;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Scanner;
import org.junit.Test;

/**
 * JUnit 4 tests of MVC coordination, input validation, and scenario runs. Uses
 * an in-memory TerminalView with the actual SimulationModel classes.
 */
public class SimulationControllerTest
{

    /** Contains an in-memory view, controller, and captured terminal output. */
    private static class Fixture
    {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final TerminalView view;
        private final SimulationController controller;

        /**
         * Creates a fixture with simulated input for the controller.
         *
         * @param scriptedInput
         *            newline-separated user responses
         */
        Fixture(String scriptedInput)
        {
            view = new TerminalView(
                new Scanner(scriptedInput),
                new PrintStream(bytes));
            controller = new SimulationController(view);
        }


        /**
         * Returns all text printed by the view so far.
         *
         * @return captured terminal output
         */
        String output()
        {
            return bytes.toString();
        }
    }


    /**
     * Special view that invokes a callback once during its first progress
     * update, enabling tests of controller reentrancy and mid-run changes.
     */
    private static class CallbackView
        extends TerminalView
    {
        private Runnable callback;
        private boolean invoked;

        /**
         * Constructs a callback-enabled view that captures its output.
         *
         * @param bytes
         *            destination for rendered terminal text
         */
        CallbackView(ByteArrayOutputStream bytes)
        {
            super(new Scanner(""), new PrintStream(bytes));
        }


        /**
         * Assigns a callback to run during the first progress display.
         *
         * @param callback
         *            callback to invoke once
         */
        void whenProgress(Runnable callback)
        {
            this.callback = callback;
            invoked = false;
        }


        /**
         * Displays progress and invokes the test callback at most once.
         *
         * @param snapshot
         *            current immutable model snapshot
         */
        @Override
        public void showProgress(SimulationSnapshot snapshot)
        {
            super.showProgress(snapshot);
            if (!invoked && callback != null)
            {
                invoked = true;
                callback.run();
            }
        }
    }

    /**
     * Builds an immutable valid configuration with equal rates in all lanes.
     *
     * @param hourlyRate
     *            vehicles per hour per lane
     * @return valid configuration for a 60-second run
     */
    private SimulationConfig config(int hourlyRate)
    {
        Map<Direction, Integer> rates = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values())
        {
            rates.put(direction, hourlyRate);
        }
        return new SimulationConfig(rates, 30, 30, 60, 20, 10);
    }


    /**
     * Returns valid terminal configuration responses, without menu commands.
     *
     * @return nine newline-terminated input values
     */
    private String validInput()
    {
        return "600\n600\n600\n600\n30\n30\n60\n20\n10\n";
    }


    /**
     * Checks that the controller constructor rejects a null view.
     */
    @Test
    public void testConstructorRejectsNullView()
    {
        try
        {
            new SimulationController(null);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected)
        {
            // Expected.
        }
    }


    /**
     * Checks that running before configuration reports an error, not a crash.
     */
    @Test
    public void testRunWithoutConfiguration()
    {
        Fixture fixture = new Fixture("");
        fixture.controller.runSimulation();
        assertTrue(
            fixture.output()
                .contains("Configure the simulation before running it."));
        assertFalse(fixture.output().contains("Final Simulation Results"));
    }


    /**
     * Checks that requesting comparison before two runs reports an error.
     */
    @Test
    public void testComparisonWithoutEnoughRuns()
    {
        Fixture fixture = new Fixture("");
        fixture.controller.compareRuns();
        assertTrue(
            fixture.output()
                .contains("Run at least two simulations before comparing."));
    }


    /**
     * Tests rejection of null programmatic configurations.
     */
    @Test
    public void testSetConfigurationRejectsNull()
    {
        Fixture fixture = new Fixture("");
        try
        {
            fixture.controller.setConfiguration(null);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected)
        {
            // Expected.
        }
        fixture.controller.runSimulation();
        assertTrue(fixture.output().contains("Configure the simulation"));
    }


    /**
     * Tests a valid interactive configuration and a full 60-second run.
     */
    @Test
    public void testConfigureAndRun()
    {
        Fixture fixture = new Fixture(validInput());
        fixture.controller.configureSimulation();
        fixture.controller.runSimulation();
        assertTrue(fixture.output().contains("Final Simulation Results"));
        assertTrue(fixture.output().contains("Arrived: 40"));
        assertTrue(fixture.output().contains("Simulation time: 60 seconds"));
    }


    /**
     * Tests the complete menu workflow, including result viewing and handling
     * an unavailable comparison before exiting.
     */
    @Test
    public void testLaunchEndToEnd()
    {
        Fixture fixture = new Fixture("1\n" + validInput() + "2\n3\n4\n5\n");
        fixture.controller.launch();
        String text = fixture.output();
        assertTrue(text.contains("=== 4WayChaos ==="));
        assertTrue(text.contains("Arrived: 40"));
        assertTrue(text.contains("Simulation time: 60 seconds"));
        assertTrue(text.contains("Run at least two simulations"));
        assertEquals(2, countMatches(text, "=== Final Simulation Results ==="));
    }


    /**
     * Counts exact substring occurrences in captured output.
     *
     * @param value
     *            text to search
     * @param needle
     *            substring being counted
     * @return number of appearances
     */
    private int countMatches(String value, String needle)
    {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0)
        {
            count++;
            index += needle.length();
        }
        return count;
    }


    /**
     * Tests invalid menu inputs, including blanks, unknown commands, nonnumeric
     * text, and strings exceeding the length limit.
     */
    @Test
    public void testMenuInvalidInputs()
    {
        String tooLong = new String(new char[81]).replace('\0', 'a');
        Fixture fixture = new Fixture("\nhello\n9\n" + tooLong + "\n5\n");
        fixture.controller.launch();
        String text = fixture.output();
        assertEquals(
            3,
            countMatches(text, "Choose a menu option from 1 to 5."));
        assertTrue(text.contains("Input must be at most 80 characters."));
    }


    /**
     * Checks that menu commands for running/viewing/comparing before
     * configuration produce errors rather than exceptions.
     */
    @Test
    public void testMenuCommandsWithoutResults()
    {
        Fixture fixture = new Fixture("2\n3\n4\n5\n");
        fixture.controller.launch();
        String text = fixture.output();
        assertTrue(text.contains("Configure the simulation"));
        assertTrue(text.contains("No completed simulation to display"));
        assertTrue(text.contains("Run at least two simulations"));
    }


    /**
     * Tests graceful EOF at the menu without hanging or running a model.
     */
    @Test
    public void testMenuEndOfInput()
    {
        Fixture fixture = new Fixture("");
        fixture.controller.launch();
        assertEquals(1, countMatches(fixture.output(), "=== 4WayChaos ==="));
        assertFalse(fixture.output().contains("Final Simulation Results"));
    }


    /**
     * Tests rejection and re-prompting of text, blanks, overlong lines, integer
     * overflow, extreme traffic rates, invalid timing/duration, and combined
     * turning percentages above 100 percent.
     */
    @Test
    public void testBadConfigurationValuesRecover()
    {
        String tooLong = new String(new char[81]).replace('\0', '7');
        String input = "abc\n\n" + tooLong + "\n-1\n1000000\n"
            + "2147483648\n600\n600\n600\n600\n" + "0\n4\n121\n30\n"
            + "oops\n30\n" + "0\n59\n7201\n60\n" + "-1\n101\n80\n" + "30\n20\n";
        Fixture fixture = new Fixture(input);
        fixture.controller.configureSimulation();
        fixture.controller.runSimulation();
        String text = fixture.output();
        assertTrue(text.contains("field cannot be blank"));
        assertTrue(text.contains("at most 80 characters"));
        assertTrue(text.contains("Enter a whole number"));
        assertTrue(text.contains("Enter a value between"));
        assertTrue(text.contains("U-turn percentage [0-20]"));
        assertTrue(text.contains("Final Simulation Results"));
        assertTrue(text.contains("Arrived: 40"));
    }


    /**
     * Tests that a partial configuration ending at EOF does not replace an
     * existing valid configuration; the previous config can still run.
     */
    @Test
    public void testPartialConfigurationPreservesPrevious()
    {
        Fixture fixture = new Fixture("600\n600\n");
        fixture.controller.setConfiguration(config(0));
        fixture.controller.configureSimulation();
        fixture.controller.runSimulation();
        assertTrue(fixture.output().contains("Final Simulation Results"));
        assertTrue(fixture.output().contains("Arrived: 0"));
    }


    /**
     * Tests EOF halfway through the menu's configuration workflow. Launch must
     * return without committing an incomplete config.
     */
    @Test
    public void testMenuEOFInConfiguration()
    {
        Fixture fixture = new Fixture("1\n600\n");
        fixture.controller.launch();
        assertFalse(fixture.output().contains("Final Simulation Results"));
        assertEquals(1, countMatches(fixture.output(), "=== 4WayChaos ==="));
    }


    /**
     * Tests zero arrival rates, valid turning percentages at their limit, and
     * correct handling of N/A waiting time.
     */
    @Test
    public void testZeroTrafficAndTurnBoundaries()
    {
        Fixture fixture = new Fixture("0\n0\n0\n0\n5\n120\n60\n100\n0\n");
        fixture.controller.configureSimulation();
        fixture.controller.runSimulation();
        assertTrue(fixture.output().contains("Arrived: 0"));
        assertTrue(fixture.output().contains("Average waiting time: N/A"));
    }


    /**
     * Tests comparison after two successful runs with different configs.
     */
    @Test
    public void testTwoRunsAndComparison()
    {
        Fixture fixture = new Fixture("");
        fixture.controller.setConfiguration(config(0));
        fixture.controller.runSimulation();
        fixture.controller.setConfiguration(config(600));
        fixture.controller.runSimulation();
        fixture.controller.compareRuns();
        String text = fixture.output();
        assertEquals(2, countMatches(text, "=== Final Simulation Results ==="));
        assertTrue(text.contains("Arrived: 0"));
        assertTrue(text.contains("Arrived: 40"));
        assertTrue(text.contains("Simulation Comparison"));
        assertTrue(text.contains("Previous"));
        assertTrue(text.contains("Current"));
        assertTrue(text.contains("NORTH avg queue"));
    }


    /**
     * Tests that updates made while the first run is active are used on the
     * next run, not retroactively on the ongoing model.
     */
    @Test
    public void testMidRunConfigurationAppliesNextRun()
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CallbackView view = new CallbackView(bytes);
        SimulationController controller = new SimulationController(view);
        controller.setConfiguration(config(600));
        view.whenProgress(() -> controller.setConfiguration(config(0)));
        controller.runSimulation();
        controller.runSimulation();
        String text = bytes.toString();
        assertEquals(2, countMatches(text, "=== Final Simulation Results ==="));
        assertTrue(text.contains("Arrived: 40"));
        assertTrue(text.contains("Arrived: 0"));
    }


    /**
     * Tests that reentrant attempts to start a second run while one is active
     * are rejected, allowing the first run to complete.
     */
    @Test
    public void testOverlappingRunRejected()
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CallbackView view = new CallbackView(bytes);
        SimulationController controller = new SimulationController(view);
        controller.setConfiguration(config(600));
        view.whenProgress(() -> controller.runSimulation());
        controller.runSimulation();
        String text = bytes.toString();
        assertTrue(text.contains("A simulation is already running."));
        assertEquals(1, countMatches(text, "=== Final Simulation Results ==="));
    }


    /**
     * Tests that progress is shown at t=0, t=60, and at completion for a run
     * whose duration is not divisible by the progress interval.
     */
    @Test
    public void testProgressOnFinalPartialInterval()
    {
        Map<Direction, Integer> rates = new EnumMap<>(Direction.class);
        for (Direction d : Direction.values())
        {
            rates.put(d, 0);
        }
        Fixture fixture = new Fixture("");
        fixture.controller
            .setConfiguration(new SimulationConfig(rates, 30, 30, 61, 0, 0));
        fixture.controller.runSimulation();
        String text = fixture.output();
        assertEquals(3, countMatches(text, "--- Simulation time:"));
        assertTrue(text.contains("Simulation time: 0 seconds"));
        assertTrue(text.contains("Simulation time: 60 seconds"));
        assertTrue(text.contains("Simulation time: 61 seconds"));
    }
}
