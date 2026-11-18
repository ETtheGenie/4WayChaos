
package defaultPackage;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Scanner;

/**
 * Text-only presentation layer for 4WayChaos V2. Reads raw input and displays
 * immutable simulation snapshots and reports. The controller validates input;
 * the model owns traffic behavior and statistics.
 */
public class TerminalView
{

    private final Scanner input;
    private final PrintStream output;

    /**
     * Creates a view connected to standard input and output.
     */
    public TerminalView()
    {
        this(new Scanner(System.in), System.out);
    }


    /**
     * Creates a view with injectable input and output for testing.
     *
     * @param input
     *            source of raw input lines
     * @param output
     *            destination for display text
     * @throws IllegalArgumentException
     *             if either resource is null
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
     * Displays a prompt and returns the next unmodified input line. EOF is
     * represented by null; parsing is the controller's responsibility.
     *
     * @param promptText
     *            text to display before reading
     * @return raw line including whitespace, or null at EOF
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
     * Displays the five main-menu commands. The controller interprets them.
     */
    public void showMenu()
    {
        output.println();
        output.println("=== 4WayChaos V2 ===");
        output.println("1. Configure simulation");
        output.println("2. Run simulation");
        output.println("3. View latest results");
        output.println("4. Compare last two runs");
        output.println("5. Exit");
    }


    /**
     * Displays a controller-provided error message.
     *
     * @param message
     *            error description
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
     * Displays a read-only V2 progress snapshot. The signal phase is the one
     * most recently applied (or the initial phase at elapsed time zero).
     * Incidents are shown at the elapsed-time boundary with their remaining
     * duration; emergency queue counts are subsets of waiting vehicles.
     *
     * @param snapshot
     *            immutable progress information
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
        output.println("Signal phase: " + snapshot.getSignalPhase());
        output.println(
            "Phase elapsed: " + snapshot.getPhaseElapsedSeconds()
                + " s | Phase switches: " + snapshot.getPhaseSwitchCount());
        output.println(
            "Arrived: " + snapshot.getTotalArrived() + " | Completed: "
                + snapshot.getTotalCompleted() + " | Waiting: "
                + snapshot.getWaitingCount() + " | Crossing: "
                + snapshot.getCrossingCount());
        output.println(
            "Emergency vehicles waiting: "
                + snapshot.getEmergencyWaitingCount());
        output.println("Approach status:");
        output.printf(
            Locale.US,
            "%-9s %8s %10s %-10s %12s%n",
            "Approach",
            "Waiting",
            "Emergency",
            "Incident",
            "Remaining");

        for (Direction direction : Direction.values())
        {
            TrafficEventType type = snapshot.getActiveIncidentType(direction);
            String remaining = type == TrafficEventType.NONE
                ? "-"
                : snapshot.getRemainingIncidentSeconds(direction) + "s";
            output.printf(
                Locale.US,
                "%-9s %8d %10d %-10s %12s%n",
                direction,
                snapshot.getQueueLength(direction),
                snapshot.getEmergencyQueueLength(direction),
                type,
                remaining);
        }
    }


    /**
     * Displays an immutable completed V2 report: traffic totals, overall and
     * emergency waiting times, incident counts, signal switches, and queue
     * statistics. Undefined averages are displayed as N/A.
     *
     * @param results
     *            final simulation results
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
        output.println(
            "Average waiting time: " + formatAverageWithUnit(
                results.getAverageWaitingTimeSeconds()));

        output.println();
        output.println("Emergency vehicle statistics:");
        output.println(
            "Emergency arrived: " + results.getTotalEmergencyArrived());
        output.println(
            "Emergency completed: " + results.getTotalEmergencyCompleted());
        output.println(
            "Emergency unfinished: " + results.getEmergencyUnfinishedCount());
        output.println(
            "Average emergency waiting time: " + formatAverageWithUnit(
                results.getAverageEmergencyWaitingTimeSeconds()));

        output.println();
        output.println("Incidents and adaptive signals:");
        output.println("Accidents: " + results.getTotalAccidents());
        output.println("Slowdowns: " + results.getTotalSlowdowns());
        output
            .println("Signal phase switches: " + results.getPhaseSwitchCount());

        output.println();
        output.println("Queue statistics (waiting vehicles):");
        output.printf(
            Locale.US,
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
     * Displays the last two completed runs side by side. This comparison is
     * descriptive and does not assume that both runs used identical traffic
     * configurations. All values come from immutable result objects.
     *
     * @param previous
     *            earlier completed report
     * @param current
     *            more recent completed report
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
        output.printf(
            Locale.US,
            "%-28s %13s %13s%n",
            "Metric",
            "Previous",
            "Current");

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

        printComparisonRow(
            "Emergency arrived",
            Integer.toString(previous.getTotalEmergencyArrived()),
            Integer.toString(current.getTotalEmergencyArrived()));
        printComparisonRow(
            "Emergency completed",
            Integer.toString(previous.getTotalEmergencyCompleted()),
            Integer.toString(current.getTotalEmergencyCompleted()));
        printComparisonRow(
            "Emergency unfinished",
            Integer.toString(previous.getEmergencyUnfinishedCount()),
            Integer.toString(current.getEmergencyUnfinishedCount()));
        printComparisonRow(
            "Emergency mean wait (s)",
            formatAverage(previous.getAverageEmergencyWaitingTimeSeconds()),
            formatAverage(current.getAverageEmergencyWaitingTimeSeconds()));
        printComparisonRow(
            "Accidents",
            Integer.toString(previous.getTotalAccidents()),
            Integer.toString(current.getTotalAccidents()));
        printComparisonRow(
            "Slowdowns",
            Integer.toString(previous.getTotalSlowdowns()),
            Integer.toString(current.getTotalSlowdowns()));
        printComparisonRow(
            "Signal phase switches",
            Integer.toString(previous.getPhaseSwitchCount()),
            Integer.toString(current.getPhaseSwitchCount()));

        output.println();
        output.println("Queue comparison (waiting vehicles):");
        output.printf(
            Locale.US,
            "%-28s %13s %13s%n",
            "Metric",
            "Previous",
            "Current");
        for (Direction direction : Direction.values())
        {
            String name = direction.toString();
            printComparisonRow(
                name + " final queue",
                Integer.toString(previous.getFinalQueueLength(direction)),
                Integer.toString(current.getFinalQueueLength(direction)));
            printComparisonRow(
                name + " avg queue",
                formatNumber(previous.getAverageQueueLength(direction)),
                formatNumber(current.getAverageQueueLength(direction)));
            printComparisonRow(
                name + " max queue",
                Integer.toString(previous.getMaximumQueueLength(direction)),
                Integer.toString(current.getMaximumQueueLength(direction)));
        }
    }


    /**
     * Formats a nullable mean as a two-decimal number or N/A.
     *
     * @param value
     *            nullable mean
     * @return formatted number or N/A
     */
    private String formatAverage(Double value)
    {
        return value == null ? "N/A" : formatNumber(value);
    }


    /**
     * Formats an average with its unit, except when the value is unavailable.
     *
     * @param value
     *            nullable mean waiting time
     * @return human-readable waiting time or N/A
     */
    private String formatAverageWithUnit(Double value)
    {
        return value == null ? "N/A" : formatNumber(value) + " seconds";
    }


    /**
     * Formats a real-valued metric with two digits and a fixed US decimal
     * point.
     *
     * @param value
     *            finite metric supplied by a validated report
     * @return two-decimal numeric text
     */
    private String formatNumber(double value)
    {
        return String.format(Locale.US, "%.2f", value);
    }


    /**
     * Writes one row of a two-column report comparison.
     *
     * @param label
     *            descriptive metric name
     * @param previous
     *            formatted earlier value
     * @param current
     *            formatted newer value
     */
    private
        void
        printComparisonRow(String label, String previous, String current)
    {
        output.printf(Locale.US, "%-28s %13s %13s%n", label, previous, current);
    }
}
