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
 * JUnit 4 tests for TerminalView, using in-memory input and output. Verifies
 * formatting, raw input handling, and error cases without accessing System.in
 * or System.out or running SimulationModel.
 */
public class TerminalViewTest
{

    /**
     * Creates a view with controlled input and captured output.
     *
     * @param inputText
     *            simulated input lines
     * @param outputBytes
     *            destination for rendered output
     * @return an independent TerminalView for testing
     */
    private
        TerminalView
        makeView(String inputText, ByteArrayOutputStream outputBytes)
    {
        return new TerminalView(
            new Scanner(inputText),
            new PrintStream(outputBytes));
    }


    /**
     * Creates a valid snapshot with four waiting, two crossing, and three
     * completed vehicles.
     *
     * @return immutable simulation snapshot
     */
    private SimulationSnapshot makeSnapshot()
    {
        Map<Direction, Integer> queues = new EnumMap<>(Direction.class);
        queues.put(Direction.NORTH, 2);
        queues.put(Direction.SOUTH, 1);
        queues.put(Direction.EAST, 0);
        queues.put(Direction.WEST, 1);
        return new SimulationSnapshot(
            40,
            SignalPhase.EW_GREEN,
            queues,
            9,
            3,
            2);
    }


    /**
     * Creates final results with six queued vehicles and a supplied completion
     * count and average waiting time.
     *
     * @param completed
     *            number of completed vehicles
     * @param meanWait
     *            average waiting time, or null when none completed
     * @return validated immutable simulation results
     */
    private SimulationResults makeResults(int completed, Double meanWait)
    {
        Map<Direction, Integer> finals = new EnumMap<>(Direction.class);
        Map<Direction, Double> averages = new EnumMap<>(Direction.class);
        Map<Direction, Integer> maximums = new EnumMap<>(Direction.class);
        finals.put(Direction.NORTH, 2);
        finals.put(Direction.SOUTH, 1);
        finals.put(Direction.EAST, 2);
        finals.put(Direction.WEST, 1);
        averages.put(Direction.NORTH, 1.5);
        averages.put(Direction.SOUTH, 0.5);
        averages.put(Direction.EAST, 1.0);
        averages.put(Direction.WEST, 0.25);
        maximums.put(Direction.NORTH, 3);
        maximums.put(Direction.SOUTH, 2);
        maximums.put(Direction.EAST, 4);
        maximums.put(Direction.WEST, 1);
        return new SimulationResults(
            60,
            completed + 8,
            completed,
            2,
            meanWait,
            finals,
            averages,
            maximums);
    }


    /**
     * Confirms that an operation rejects null arguments.
     *
     * @param action
     *            operation that should throw IllegalArgumentException
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
     * Tests that prompts return exactly the input entered, including
     * leading/trailing whitespace and empty lines, without parsing.
     */
    @Test
    public void testPromptPreservesRawInput()
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        TerminalView view = makeView(" 600 \n\n", bytes);
        assertEquals(" 600 ", view.prompt("Rate: "));
        assertEquals("", view.prompt("Again: "));
        assertTrue(bytes.toString().contains("Rate: Again: "));
    }


    /**
     * Tests that an exhausted input source returns null rather than crashing or
     * indefinitely attempting to read a new line.
     */
    @Test
    public void testPromptAtEndOfInput()
    {
        TerminalView view = makeView("", new ByteArrayOutputStream());
        assertNull(view.prompt("Enter command: "));
    }


    /**
     * Tests that the view displays all five agreed menu options.
     */
    @Test
    public void testShowMenu()
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        TerminalView view = makeView("", bytes);
        view.showMenu();
        String text = bytes.toString();
        assertTrue(text.contains("4WayChaos"));
        assertTrue(text.contains("1. Configure simulation"));
        assertTrue(text.contains("2. Run simulation"));
        assertTrue(text.contains("3. View latest results"));
        assertTrue(text.contains("4. Compare last two runs"));
        assertTrue(text.contains("5. Exit"));
    }


    /**
     * Tests that controller-supplied error messages are displayed.
     */
    @Test
    public void testShowError()
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        TerminalView view = makeView("", bytes);
        view.showError("Arrival rate must be an integer.");
        assertTrue(
            bytes.toString()
                .contains("Error: Arrival rate must be an integer."));
    }


    /**
     * Tests that all snapshot counts, the next phase, and four queue lengths
     * appear without exposing any mutable model objects.
     */
    @Test
    public void testShowProgress()
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        TerminalView view = makeView("", bytes);
        view.showProgress(makeSnapshot());
        String text = bytes.toString();
        assertTrue(text.contains("Simulation time: 40 seconds"));
        assertTrue(text.contains("Next signal phase: EW_GREEN"));
        assertTrue(
            text.contains(
                "Arrived: 9 | Completed: 3 | Waiting: 4 | Crossing: 2"));
        assertTrue(text.contains("NORTH: 2"));
        assertTrue(text.contains("SOUTH: 1"));
        assertTrue(text.contains("EAST: 0"));
        assertTrue(text.contains("WEST: 1"));
    }


    /**
     * Tests that a completed run displays the correct statistics including a
     * formatted average waiting time and per-lane values.
     */
    @Test
    public void testShowResults()
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        TerminalView view = makeView("", bytes);
        view.showResults(makeResults(12, 6.5));
        String text = bytes.toString();
        assertTrue(text.contains("Final Simulation Results"));
        assertTrue(text.contains("Duration: 60 seconds"));
        assertTrue(text.contains("Arrived: 20"));
        assertTrue(text.contains("Completed: 12"));
        assertTrue(text.contains("Waiting: 6"));
        assertTrue(text.contains("Crossing: 2"));
        assertTrue(text.contains("Average waiting time: 6.50 seconds"));
        assertTrue(text.contains("NORTH"));
        assertTrue(text.contains("Maximum"));
    }


    /**
     * Tests that undefined average waiting time is shown as N/A when a run has
     * no completed vehicles.
     */
    @Test
    public void testShowResultsWithoutCompletions()
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        TerminalView view = makeView("", bytes);
        view.showResults(makeResults(0, null));
        assertTrue(bytes.toString().contains("Average waiting time: N/A"));
    }


    /**
     * Tests side-by-side display of results from two runs. The display receives
     * precomputed results and does not change them.
     */
    @Test
    public void testShowComparison()
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        TerminalView view = makeView("", bytes);
        SimulationResults previous = makeResults(12, 6.5);
        SimulationResults current = makeResults(14, 4.0);
        view.showComparison(previous, current);
        String text = bytes.toString();
        assertTrue(text.contains("Simulation Comparison"));
        assertTrue(text.contains("Previous"));
        assertTrue(text.contains("Current"));
        assertTrue(text.contains("6.50"));
        assertTrue(text.contains("4.00"));
        assertTrue(text.contains("NORTH final queue"));
        assertTrue(text.contains("WEST max queue"));
        assertEquals(12, previous.getTotalCompleted());
        assertEquals(14, current.getTotalCompleted());
    }


    /**
     * Tests that null I/O resources and null arguments to public display
     * methods fail clearly instead of producing NPEs.
     */
    @Test
    public void testNullArgumentRejection()
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(bytes);
        assertInvalid(() -> new TerminalView(null, stream));
        assertInvalid(() -> new TerminalView(new Scanner(""), null));
        TerminalView view = makeView("", bytes);
        assertInvalid(() -> view.prompt(null));
        assertInvalid(() -> view.showError(null));
        assertInvalid(() -> view.showProgress(null));
        assertInvalid(() -> view.showResults(null));
        assertInvalid(() -> view.showComparison(null, makeResults(12, 6.5)));
        assertInvalid(() -> view.showComparison(makeResults(12, 6.5), null));
    }
}
