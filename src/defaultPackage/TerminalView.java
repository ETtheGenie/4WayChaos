package defaultPackage;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Scanner;

/**
 * Text-only presentation layer for the 4WayChaos simulator. Collects raw lines
 * of input and formats immutable model summaries. It does not validate traffic
 * parameters, advance the simulation, or access mutable Lane, Vehicle, or queue
 * objects.
 */
public class TerminalView
{

    private final Scanner input;
    private final PrintStream output;

    /**
     * Creates a terminal view attached to standard input and output. The
     * application controller should construct this once on startup.
     */
    public TerminalView()
    {
        this(new Scanner(System.in), System.out);
    }


    /**
     * Creates a terminal view with supplied input/output resources. This
     * constructor allows unit testing without using the console.
     *
     * @param input
     *            source of user-entered lines
     * @param output
     *            destination for displayed text
     * @throws IllegalArgumentException
     *             if either argument is null
     */
    public TerminalView(Scanner input, PrintStream output)
    {
        if (input == null || output == null)
        {
            throw new IllegalArgumentException(
                "Input and output cannot be null.");
        }
        this.input = input;
        this.output = output;
    }


    /**
     * Displays a prompt and returns the next raw line of user input. The
     * controller is responsible for parsing and validating it. Blank lines and
     * surrounding whitespace are left unchanged.
     *
     * @param promptText
     *            text to display before reading input
     * @return raw input line, or null if input has ended (EOF)
     * @throws IllegalArgumentException
     *             if promptText is null
     */
    public String prompt(String promptText)
    {
        if (promptText == null)
        {
            throw new IllegalArgumentException("Prompt cannot be null.");
        }
        output.print(promptText);
        output.flush();
        return input.hasNextLine() ? input.nextLine() : null;
    }


    /**
     * Displays the five supported main-menu commands. Interpretation of menu
     * selections belongs to the controller.
     */
    public void showMenu()
    {
        output.println();
        output.println("=== 4WayChaos ===");
        output.println("1. Configure simulation");
        output.println("2. Run simulation");
        output.println("3. View latest results");
        output.println("4. Compare last two runs");
        output.println("5. Exit");
    }


    /**
     * Displays an error message supplied by the controller.
     *
     * @param message
     *            explanatory error text
     * @throws IllegalArgumentException
     *             if message is null
     */
    public void showError(String message)
    {
        if (message == null)
        {
            throw new IllegalArgumentException("Error message cannot be null.");
        }
        output.println("Error: " + message);
    }


    /**
     * Displays a read-only snapshot of simulation progress. A snapshot's phase
     * describes the next interval to be processed.
     *
     * @param snapshot
     *            current immutable simulation summary
     * @throws IllegalArgumentException
     *             if snapshot is null
     */
    public void showProgress(SimulationSnapshot snapshot)
    {
        if (snapshot == null)
        {
            throw new IllegalArgumentException("Snapshot cannot be null.");
        }
        output.println();
        output.println(
            "--- Simulation time: " + snapshot.getElapsedSeconds()
                + " seconds ---");
        output.println("Next signal phase: " + snapshot.getSignalPhase());
        output.println(
            "Arrived: " + snapshot.getTotalArrived() + " | Completed: "
                + snapshot.getTotalCompleted() + " | Waiting: "
                + snapshot.getWaitingCount() + " | Crossing: "
                + snapshot.getCrossingCount());
        output.println("Queue lengths:");
        for (Direction direction : Direction.values())
        {
            output.println(
                "  " + direction + ": " + snapshot.getQueueLength(direction));
        }
    }


    /**
     * Displays final vehicle counts, mean waiting time, and queue statistics.
     * All values have already been calculated by the model.
     *
     * @param results
     *            immutable report for a completed run
     * @throws IllegalArgumentException
     *             if results is null
     */
    public void showResults(SimulationResults results)
    {
        if (results == null)
        {
            throw new IllegalArgumentException("Results cannot be null.");
        }
        output.println();
        output.println("=== Final Simulation Results ===");
        output
            .println("Duration: " + results.getDurationSeconds() + " seconds");
        output.println("Arrived: " + results.getTotalArrived());
        output.println("Completed: " + results.getTotalCompleted());
        output.println("Waiting: " + results.getWaitingCount());
        output.println("Crossing: " + results.getCrossingCount());
        Double averageWait = results.getAverageWaitingTimeSeconds();
        if (averageWait == null)
        {
            output.println("Average waiting time: N/A");
        }
        else
        {
            output.printf(
                Locale.US,
                "Average waiting time: %.2f seconds%n",
                averageWait);
        }
        output.println();
        output.printf(
            "%-9s %8s %9s %9s%n",
            "Approach",
            "Final",
            "Average",
            "Maximum");
        for (Direction direction : Direction.values())
        {
            output.printf(
                Locale.US,
                "%-9s %8d %9.2f %9d%n",
                direction,
                results.getFinalQueueLength(direction),
                results.getAverageQueueLength(direction),
                results.getMaximumQueueLength(direction));
        }
    }


    /**
     * Displays two completed runs side by side for comparison. The view only
     * formats supplied values; it does not calculate differences or modify
     * either report.
     *
     * @param previous
     *            earlier completed simulation report
     * @param current
     *            more recent completed simulation report
     * @throws IllegalArgumentException
     *             if either report is null
     */
    public
        void
        showComparison(SimulationResults previous, SimulationResults current)
    {
        if (previous == null || current == null)
        {
            throw new IllegalArgumentException(
                "Both comparison reports are required.");
        }
        output.println();
        output.println("=== Simulation Comparison ===");
        output.printf("%-23s %13s %13s%n", "Metric", "Previous", "Current");
        printComparisonRow(
            "Duration (s)",
            Integer.toString(previous.getDurationSeconds()),
            Integer.toString(current.getDurationSeconds()));
        printComparisonRow(
            "Arrived",
            Integer.toString(previous.getTotalArrived()),
            Integer.toString(current.getTotalArrived()));
        printComparisonRow(
            "Completed",
            Integer.toString(previous.getTotalCompleted()),
            Integer.toString(current.getTotalCompleted()));
        printComparisonRow(
            "Waiting",
            Integer.toString(previous.getWaitingCount()),
            Integer.toString(current.getWaitingCount()));
        printComparisonRow(
            "Crossing",
            Integer.toString(previous.getCrossingCount()),
            Integer.toString(current.getCrossingCount()));
        printComparisonRow(
            "Mean wait (s)",
            formatAverage(previous.getAverageWaitingTimeSeconds()),
            formatAverage(current.getAverageWaitingTimeSeconds()));

        for (Direction direction : Direction.values())
        {
            String prefix = direction.toString();
            printComparisonRow(
                prefix + " final queue",
                Integer.toString(previous.getFinalQueueLength(direction)),
                Integer.toString(current.getFinalQueueLength(direction)));
            printComparisonRow(
                prefix + " avg queue",
                formatNumber(previous.getAverageQueueLength(direction)),
                formatNumber(current.getAverageQueueLength(direction)));
            printComparisonRow(
                prefix + " max queue",
                Integer.toString(previous.getMaximumQueueLength(direction)),
                Integer.toString(current.getMaximumQueueLength(direction)));
        }
    }


    /**
     * Formats an optional average for a report column.
     *
     * @param value
     *            average value or null if undefined
     * @return two-decimal text or N/A
     */
    private String formatAverage(Double value)
    {
        return value == null ? "N/A" : formatNumber(value);
    }


    /**
     * Formats a real-valued statistic with two decimal places.
     *
     * @param value
     *            statistic to display
     * @return localized-independent, two-decimal string
     */
    private String formatNumber(double value)
    {
        return String.format(Locale.US, "%.2f", value);
    }


    /**
     * Writes a side-by-side row containing already computed values.
     *
     * @param label
     *            metric name
     * @param previous
     *            value from prior run
     * @param current
     *            value from current run
     */
    private
        void
        printComparisonRow(String label, String previous, String current)
    {
        output.printf("%-23s %13s %13s%n", label, previous, current);
    }
}
