package defaultPackage;

import java.util.EnumMap;
import java.util.Map;

/**
 * Standalone demonstration driver for SimulationModel. Runs a complete traffic
 * simulation without requiring TerminalView or SimulationController.
 * Demonstrates vehicle generation, queue-backed traffic flow, signal operation,
 * and performance statistics.
 */
public class SimulationModelDriver
{

    /**
     * Creates and runs a demonstration simulation.
     *
     * @param args
     *            command-line arguments (unused)
     */
    public static void main(String[] args)
    {

        // Configure vehicle arrival rates.
        Map<Direction, Integer> arrivalRates = new EnumMap<>(Direction.class);

        arrivalRates.put(Direction.NORTH, 600);
        arrivalRates.put(Direction.SOUTH, 600);
        arrivalRates.put(Direction.EAST, 600);
        arrivalRates.put(Direction.WEST, 600);

        // Create the simulation configuration.
        SimulationConfig config = new SimulationConfig(
            arrivalRates,
            30,     // North-South green duration
            30,     // East-West green duration
            120,    // Total simulation duration
            20,     // Left-turn percentage
            10      // U-turn percentage
        );

        // Initialize the simulation model.
        SimulationModel model = new SimulationModel(config);

        System.out.println("=================================");
        System.out.println("       4WayChaos - Model V1      ");
        System.out.println("=================================");

        System.out.println("\nSimulation Configuration:");
        System.out
            .println("Duration: " + config.getDurationSeconds() + " seconds");

        System.out
            .println("NS Green: " + config.getNsGreenSeconds() + " seconds");

        System.out
            .println("EW Green: " + config.getEwGreenSeconds() + " seconds");

        System.out.println("Left Turns: " + config.getLeftTurnPercent() + "%");

        System.out.println("U-Turns: " + config.getUTurnPercent() + "%");

        for (Direction direction : Direction.values())
        {
            System.out.println(
                direction + " Arrivals: "
                    + config.getArrivalRatePerHour(direction)
                    + " vehicles/hour");
        }

        System.out.println("\nStarting simulation...");

        // Run the model one simulated second at a time.
        while (!model.isComplete())
        {

            model.step();

            SimulationSnapshot snapshot = model.getSnapshot();

            // Verify vehicle conservation after each step.
            verifyConservation(snapshot);

            // Display progress every 30 simulated seconds.
            if (snapshot.getElapsedSeconds() % 30 == 0)
            {
                printSnapshot(snapshot);
            }
        }

        // Retrieve and display final results.
        SimulationResults results = model.getResults();

        printResults(results);

        System.out.println("\nSimulation completed successfully.");
    }


    /**
     * Checks that all generated vehicles are accounted for in the current
     * simulation state.
     *
     * @param snapshot
     *            current simulation state
     * @throws IllegalStateException
     *             if counts are inconsistent
     */
    private static void verifyConservation(SimulationSnapshot snapshot)
    {

        int accounted = snapshot.getWaitingCount() + snapshot.getCrossingCount()
            + snapshot.getTotalCompleted();

        if (snapshot.getTotalArrived() != accounted)
        {
            throw new IllegalStateException(
                "Vehicle conservation failed at t="
                    + snapshot.getElapsedSeconds());
        }
    }


    /**
     * Displays the current simulation state, including signal phase, vehicle
     * counts, and queue lengths.
     *
     * @param snapshot
     *            current simulation state
     */
    private static void printSnapshot(SimulationSnapshot snapshot)
    {

        System.out.println("\n-------------------------------");
        System.out
            .println("Time: " + snapshot.getElapsedSeconds() + " seconds");

        System.out.println("Signal Phase: " + snapshot.getSignalPhase());

        System.out.println("Total Arrived: " + snapshot.getTotalArrived());

        System.out.println("Total Completed: " + snapshot.getTotalCompleted());

        System.out
            .println("Currently Crossing: " + snapshot.getCrossingCount());

        System.out.println("Currently Waiting: " + snapshot.getWaitingCount());

        System.out.println("Queue Lengths:");

        for (Direction direction : Direction.values())
        {
            System.out.println(
                "  " + direction + ": " + snapshot.getQueueLength(direction));
        }
    }


    /**
     * Displays the final traffic performance report, including average waiting
     * time and per-direction final, average, and maximum queue lengths.
     *
     * @param results
     *            completed simulation results
     */
    private static void printResults(SimulationResults results)
    {

        System.out.println("\n=================================");
        System.out.println("       FINAL SIMULATION RESULTS   ");
        System.out.println("=================================");

        System.out
            .println("Duration: " + results.getDurationSeconds() + " seconds");

        System.out.println("Total Arrived: " + results.getTotalArrived());

        System.out.println("Total Completed: " + results.getTotalCompleted());

        System.out.println("Vehicles Waiting: " + results.getWaitingCount());

        System.out.println("Vehicles Crossing: " + results.getCrossingCount());

        Double averageWait = results.getAverageWaitingTimeSeconds();

        if (averageWait == null)
        {
            System.out.println("Average Waiting Time: N/A");
        }
        else
        {
            System.out
                .printf("Average Waiting Time: %.2f seconds%n", averageWait);
        }

        System.out.println("\nQueue Statistics:");

        for (Direction direction : Direction.values())
        {

            System.out.println("\n" + direction + ":");

            System.out.println(
                "  Final Queue Length: "
                    + results.getFinalQueueLength(direction));

            System.out.printf(
                "  Average Queue Length: %.2f%n",
                results.getAverageQueueLength(direction));

            System.out.println(
                "  Maximum Queue Length: "
                    + results.getMaximumQueueLength(direction));
        }

        // Final vehicle conservation check.
        int accounted = results.getWaitingCount() + results.getCrossingCount()
            + results.getTotalCompleted();

        System.out.println(
            "\nVehicle Conservation: " + results.getTotalArrived()
                + " arrived = " + accounted + " accounted for");

        if (results.getTotalArrived() != accounted)
        {
            throw new IllegalStateException(
                "Final vehicle conservation failed.");
        }
    }
}
