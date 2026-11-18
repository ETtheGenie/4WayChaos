
package defaultPackage;

import java.io.PrintStream;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Standalone V2 demonstration of SimulationModel without TerminalView or
 * SimulationController. Uses a fixed, reproducible configuration, steps the
 * model directly, verifies vehicle conservation at every step, and formats
 * immutable snapshots/results to a supplied PrintStream. This class is a
 * diagnostic client of the model, not the application's view or controller.
 * Main remains the entry point for the interactive application.
 */
public final class SimulationModelDriver
{

    private static final int PROGRESS_INTERVAL_SECONDS = 30;
    private static final int DEMO_DURATION_SECONDS = 180;

    /**
     * Prevents construction of a utility class with only static operations.
     */
    private SimulationModelDriver()
    {
        // No instances needed.
    }


    /**
     * Runs the standalone model demonstration using standard output.
     * Command-line arguments do not alter the reproducible demonstration.
     *
     * @param args
     *            unused command-line arguments
     */
    public static void main(String[] args)
    {
        runDemonstration(System.out);
    }


    /**
     * Runs the V2 simulation independently of the interactive MVC interface.
     * Prints the initial state; snapshots at 30-second intervals, signal
     * switches, and incident transitions; and final results. Verifies vehicle
     * accounting at the initial state and after every simulated second.
     *
     * @param output
     *            non-null destination for diagnostic text
     * @return immutable final report, useful for independent verification
     * @throws IllegalArgumentException
     *             if output is null
     * @throws IllegalStateException
     *             if vehicle accounting ever fails
     */
    public static SimulationResults runDemonstration(PrintStream output)
    {
        if (output == null)
        {
            throw new IllegalArgumentException("Output stream cannot be null.");
        }

        SimulationConfig config = createDemoConfig();
        SimulationModel model = new SimulationModel(config);

        printConfiguration(output, config);
        SimulationSnapshot previous = model.getSnapshot();
        verifyConservation(previous);
        int verifiedSnapshots = 1;
        printSnapshot(output, previous);

        while (!model.isComplete())
        {
            model.step();
            SimulationSnapshot current = model.getSnapshot();
            verifyConservation(current);
            verifiedSnapshots++;

            boolean phaseChanged =
                current.getPhaseSwitchCount() != previous.getPhaseSwitchCount();
            boolean eventsChanged =
                !current.getActiveEvents().equals(previous.getActiveEvents());

            if (phaseChanged || eventsChanged)
            {
                printTransitions(output, previous, current);
            }

            if (phaseChanged || eventsChanged || model.isComplete()
                || current.getElapsedSeconds() % PROGRESS_INTERVAL_SECONDS == 0)
            {
                printSnapshot(output, current);
            }
            previous = current;
        }

        SimulationResults results = model.getResults();
        verifyFinalConservation(results);
        printResults(output, results);
        output.printf(
            Locale.US,
            "%nVehicle conservation: PASS (%d arrived = %d accounted)%n",
            results.getTotalArrived(),
            (long)results.getWaitingCount() + results.getCrossingCount()
                + results.getTotalCompleted());
        output.printf(
            Locale.US,
            "Conservation checks passed: %d snapshots (initial + %d steps)%n",
            verifiedSnapshots,
            config.getDurationSeconds());
        output
            .println("Standalone model demonstration completed successfully.");
        return results;
    }


    /**
     * Creates a nontrivial V2 scenario that exercises adaptive timing,
     * emergency vehicle generation, and stochastic accidents and slowdowns. The
     * same seed produces identical results on repeated executions.
     *
     * @return immutable V2 simulation configuration
     */
    private static SimulationConfig createDemoConfig()
    {
        Map<Direction, Integer> rates = new EnumMap<>(Direction.class);
        rates.put(Direction.NORTH, 600);
        rates.put(Direction.SOUTH, 900);
        rates.put(Direction.EAST, 1200);
        rates.put(Direction.WEST, 1800);

        SimulationOptions options =
            SimulationOptions.builder().minGreenSeconds(10).maxGreenSeconds(40)
                .queueSwitchThreshold(3).emergencyVehiclePercent(30)
                .eventCheckIntervalSeconds(10).accidentChancePercent(25)
                .slowdownChancePercent(35).accidentDurationSeconds(15)
                .slowdownDurationSeconds(20).slowdownSpeedMetersPerSecond(5)
                .slowdownHeadwaySeconds(4).randomSeed(20271118L).build();

        return new SimulationConfig(
            rates,
            DEMO_DURATION_SECONDS,
            20,
            10,
            options);
    }


    /**
     * Prints all V2 configuration settings for a reproducible demonstration.
     *
     * @param output
     *            diagnostic output destination
     * @param config
     *            validated model configuration
     */
    private static
        void
        printConfiguration(PrintStream output, SimulationConfig config)
    {
        SimulationOptions options = config.getOptions();
        output.println("=== 4WayChaos V2: Standalone Model Demonstration ===");
        output.println("No TerminalView or SimulationController required.");
        output.printf(
            Locale.US,
            "Duration: %d seconds%n",
            config.getDurationSeconds());
        for (Direction direction : Direction.values())
        {
            output.printf(
                Locale.US,
                "%s arrivals: %d vehicles/hour%n",
                direction,
                config.getArrivalRatePerHour(direction));
        }
        output.printf(
            Locale.US,
            "Random turns: LEFT %d%%, U_TURN %d%%, STRAIGHT %d%%%n",
            config.getLeftTurnPercent(),
            config.getUTurnPercent(),
            100 - config.getLeftTurnPercent() - config.getUTurnPercent());
        output.printf(
            Locale.US,
            "Adaptive green: min %d s, max %d s, queue threshold %d%n",
            options.getMinGreenSeconds(),
            options.getMaxGreenSeconds(),
            options.getQueueSwitchThreshold());
        output.printf(
            Locale.US,
            "Emergency arrivals: %d%%%n",
            options.getEmergencyVehiclePercent());
        output.printf(
            Locale.US,
            "Incident checks: every %d s | accident %d%% | slowdown %d%%%n",
            options.getEventCheckIntervalSeconds(),
            options.getAccidentChancePercent(),
            options.getSlowdownChancePercent());
        output.printf(
            Locale.US,
            "Accident duration: %d s | slowdown duration: %d s%n",
            options.getAccidentDurationSeconds(),
            options.getSlowdownDurationSeconds());
        output.printf(
            Locale.US,
            "Slowdown speed: %d m/s | slowdown departure headway: %d s%n",
            options.getSlowdownSpeedMetersPerSecond(),
            options.getSlowdownHeadwaySeconds());
        output.printf(Locale.US, "Random seed: %d%n", options.getRandomSeed());
    }


    /**
     * Reports signal changes and incident starts/ends between two consecutive
     * snapshots. Event times come from TrafficEvent, not from the post-step
     * snapshot timestamp (which is one second after the interval processed).
     *
     * @param output
     *            diagnostic output destination
     * @param previous
     *            snapshot before the latest step
     * @param current
     *            snapshot after the latest step
     */
    private static void printTransitions(
        PrintStream output,
        SimulationSnapshot previous,
        SimulationSnapshot current)
    {
        if (current.getPhaseSwitchCount() != previous.getPhaseSwitchCount())
        {
            output.printf(
                Locale.US,
                "%nSIGNAL SWITCH at t=%d: %s%n",
                current.getPhaseStartSecond(),
                current.getSignalPhase());
        }
        for (Direction direction : Direction.values())
        {
            TrafficEvent oldEvent = previous.getActiveEvent(direction);
            TrafficEvent newEvent = current.getActiveEvent(direction);
            if (oldEvent != null && oldEvent != newEvent)
            {
                output.printf(
                    Locale.US,
                    "INCIDENT END at t=%d: %s %s%n",
                    oldEvent.getEndSecond(),
                    direction,
                    oldEvent.getType());
            }
            if (newEvent != null && newEvent != oldEvent)
            {
                output.printf(
                    Locale.US,
                    "INCIDENT START at t=%d: %s %s (ends at t=%d)%n",
                    newEvent.getStartSecond(),
                    direction,
                    newEvent.getType(),
                    newEvent.getEndSecond());
            }
        }
    }


    /**
     * Prints a complete immutable model snapshot without inspecting live
     * queues, accessing signal internals, or calling the interactive view.
     *
     * @param output
     *            diagnostic output destination
     * @param snapshot
     *            immutable model state to display
     */
    private static
        void
        printSnapshot(PrintStream output, SimulationSnapshot snapshot)
    {
        output.printf(
            Locale.US,
            "%n--- Model snapshot at t=%d s ---%n",
            snapshot.getElapsedSeconds());
        output.printf(
            Locale.US,
            "Signal: %s | phase started t=%d | phase elapsed %d s | switches %d%n",
            snapshot.getSignalPhase(),
            snapshot.getPhaseStartSecond(),
            snapshot.getPhaseElapsedSeconds(),
            snapshot.getPhaseSwitchCount());
        output.printf(
            Locale.US,
            "Arrived: %d | Completed: %d | Waiting: %d | Crossing: %d"
                + " | Emergency waiting: %d%n",
            snapshot.getTotalArrived(),
            snapshot.getTotalCompleted(),
            snapshot.getWaitingCount(),
            snapshot.getCrossingCount(),
            snapshot.getEmergencyWaitingCount());
        output.printf(
            Locale.US,
            "%-9s %8s %10s %-10s %10s%n",
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
                "%-9s %8d %10d %-10s %10s%n",
                direction,
                snapshot.getQueueLength(direction),
                snapshot.getEmergencyQueueLength(direction),
                type,
                remaining);
        }
    }


    /**
     * Displays the finished immutable model report, including emergency
     * throughput, incident counts, adaptive switches, and queue statistics.
     *
     * @param output
     *            diagnostic output destination
     * @param results
     *            completed simulation report
     */
    private static
        void
        printResults(PrintStream output, SimulationResults results)
    {
        output.println("\n=== FINAL V2 MODEL RESULTS ===");
        output.printf(
            Locale.US,
            "Duration: %d seconds%n",
            results.getDurationSeconds());
        output.printf(
            Locale.US,
            "Arrived: %d | Completed: %d | Waiting: %d | Crossing: %d%n",
            results.getTotalArrived(),
            results.getTotalCompleted(),
            results.getWaitingCount(),
            results.getCrossingCount());
        output.printf(
            Locale.US,
            "Average waiting time: %s%n",
            formatWait(results.getAverageWaitingTimeSeconds()));
        output.printf(
            Locale.US,
            "Emergency arrived: %d | Completed: %d | Unfinished: %d%n",
            results.getTotalEmergencyArrived(),
            results.getTotalEmergencyCompleted(),
            results.getEmergencyUnfinishedCount());
        output.printf(
            Locale.US,
            "Average emergency waiting time: %s%n",
            formatWait(results.getAverageEmergencyWaitingTimeSeconds()));
        output.printf(
            Locale.US,
            "Accidents: %d | Slowdowns: %d | Signal switches: %d%n",
            results.getTotalAccidents(),
            results.getTotalSlowdowns(),
            results.getPhaseSwitchCount());
        output.printf(
            Locale.US,
            "%-9s %8s %10s %8s%n",
            "Approach",
            "Final",
            "Average",
            "Maximum");
        for (Direction direction : Direction.values())
        {
            output.printf(
                Locale.US,
                "%-9s %8d %10.2f %8d%n",
                direction,
                results.getFinalQueueLength(direction),
                results.getAverageQueueLength(direction),
                results.getMaximumQueueLength(direction));
        }
    }


    /**
     * Converts nullable completed-vehicle waiting-time statistics into text.
     *
     * @param seconds
     *            mean wait, or null if no qualifying vehicles finished
     * @return "N/A" or a two-decimal waiting time with seconds unit
     */
    private static String formatWait(Double seconds)
    {
        return seconds == null
            ? "N/A"
            : String.format(Locale.US, "%.2f seconds", seconds);
    }


    /**
     * Verifies vehicle conservation for a current immutable snapshot. Long
     * arithmetic protects addition against intermediate int overflow.
     *
     * @param snapshot
     *            current immutable simulation state
     * @throws IllegalStateException
     *             if arrived and accounted counts differ
     */
    private static void verifyConservation(SimulationSnapshot snapshot)
    {
        long accounted = (long)snapshot.getWaitingCount()
            + snapshot.getCrossingCount() + snapshot.getTotalCompleted();
        if (snapshot.getTotalArrived() != accounted)
        {
            throw new IllegalStateException(
                "Vehicle conservation failed at t="
                    + snapshot.getElapsedSeconds());
        }
    }


    /**
     * Verifies that final report totals account for all generated vehicles.
     *
     * @param results
     *            immutable final simulation report
     * @throws IllegalStateException
     *             if arrived and accounted counts differ
     */
    private static void verifyFinalConservation(SimulationResults results)
    {
        long accounted = (long)results.getWaitingCount()
            + results.getCrossingCount() + results.getTotalCompleted();
        if (results.getTotalArrived() != accounted)
        {
            throw new IllegalStateException(
                "Final vehicle conservation failed.");
        }
    }
}
