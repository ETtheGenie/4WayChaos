
package defaultPackage;

import java.util.EnumMap;
import java.util.Map;

/**
 * Coordinates the V2 4WayChaos terminal application. Collects and validates
 * configuration input through TerminalView, constructs SimulationModel runs,
 * and sends immutable snapshots and results to the view. Traffic rules remain
 * entirely inside the model layer.
 */
public class SimulationController
{

    /** Maximum number of characters accepted in one input line. */
    private static final int MAX_INPUT_LENGTH = 80;

    /** Number of completed simulation seconds between periodic updates. */
    private static final int PROGRESS_INTERVAL_SECONDS = 60;

    private final TerminalView view;
    private SimulationModel activeModel;
    private SimulationConfig nextConfig;
    private SimulationResults latestResults;
    private SimulationResults previousResults;
    private boolean running;
    private boolean inputClosed;

    /**
     * Creates a controller for the supplied terminal presentation layer.
     *
     * @param view
     *            terminal view for input and output
     * @throws IllegalArgumentException
     *             if view is null
     */
    public SimulationController(TerminalView view)
    {
        if (view == null)
        {
            throw new IllegalArgumentException("View cannot be null.");
        }
        this.view = view;
        activeModel = null;
        nextConfig = null;
        latestResults = null;
        previousResults = null;
        running = false;
        inputClosed = false;
    }


    /**
     * Shows the five-option menu until the user selects Exit or reaches EOF.
     * Invalid commands are rejected without terminating the application. This
     * method does not close the view's streams.
     */
    public void launch()
    {
        while (!inputClosed)
        {
            view.showMenu();
            String raw = view.prompt("Select an option: ");
            if (raw == null)
            {
                inputClosed = true;
                return;
            }
            if (raw.length() > MAX_INPUT_LENGTH)
            {
                view.showError(
                    "Input must be at most " + MAX_INPUT_LENGTH
                        + " characters.");
                continue;
            }

            switch (raw.trim())
            {
                case "1":
                    configureSimulation();
                    break;
                case "2":
                    runSimulation();
                    break;
                case "3":
                    if (latestResults == null)
                    {
                        view.showError("No completed simulation to display.");
                    }
                    else
                    {
                        view.showResults(latestResults);
                    }
                    break;
                case "4":
                    compareRuns();
                    break;
                case "5":
                    return;
                default:
                    view.showError("Choose a menu option from 1 to 5.");
                    break;
            }
        }
    }


    /**
     * Interactively reads every V2 configuration field. Each numeric prompt
     * repeats until valid or EOF. The existing configuration remains intact
     * unless all fields have been entered successfully. Adaptive signals and
     * random turning are the only supported traffic modes.
     */
    public void configureSimulation()
    {
        if (inputClosed)
        {
            return;
        }
        if (running)
        {
            view.showError("Cannot configure interactively during a run.");
            return;
        }

        Map<Direction, Integer> rates = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values())
        {
            Integer rate = readInteger(
                direction + " arrival rate (vehicles/hour)",
                SimulationConfig.MIN_ARRIVAL_RATE,
                SimulationConfig.MAX_ARRIVAL_RATE);
            if (rate == null)
            {
                return;
            }
            rates.put(direction, rate);
        }

        Integer duration = readInteger(
            "Simulation duration (seconds)",
            SimulationConfig.MIN_DURATION_SECONDS,
            SimulationConfig.MAX_DURATION_SECONDS);
        if (duration == null)
        {
            return;
        }

        Integer left = readInteger(
            "Left-turn probability (%)",
            SimulationConfig.MIN_TURN_PERCENT,
            SimulationConfig.MAX_TURN_PERCENT);
        if (left == null)
        {
            return;
        }

        Integer uTurn = readInteger(
            "U-turn probability (%)",
            SimulationConfig.MIN_TURN_PERCENT,
            SimulationConfig.MAX_TURN_PERCENT - left);
        if (uTurn == null)
        {
            return;
        }

        Integer minGreen = readInteger(
            "Minimum adaptive green (seconds)",
            SimulationOptions.MIN_GREEN_SECONDS,
            SimulationOptions.MAX_GREEN_SECONDS);
        if (minGreen == null)
        {
            return;
        }

        Integer maxGreen = readInteger(
            "Maximum adaptive green (seconds)",
            minGreen,
            SimulationOptions.MAX_GREEN_SECONDS);
        if (maxGreen == null)
        {
            return;
        }

        Integer threshold =
            readInteger("Queue-switch threshold (vehicles)", 1, 50);
        if (threshold == null)
        {
            return;
        }

        Integer emergency =
            readInteger("Emergency vehicle probability (%)", 0, 100);
        if (emergency == null)
        {
            return;
        }

        Integer interval =
            readInteger("Traffic-event check interval (seconds)", 10, 120);
        if (interval == null)
        {
            return;
        }

        Integer accident =
            readInteger("Accident probability per check (%)", 0, 100);
        if (accident == null)
        {
            return;
        }

        Integer slowdown = readInteger(
            "Slowdown probability per check (%)",
            0,
            100 - accident);
        if (slowdown == null)
        {
            return;
        }

        Integer accidentDuration = readInteger(
            "Accident duration (seconds)",
            SimulationOptions.MIN_EVENT_DURATION_SECONDS,
            SimulationOptions.MAX_EVENT_DURATION_SECONDS);
        if (accidentDuration == null)
        {
            return;
        }

        Integer slowdownDuration = readInteger(
            "Slowdown duration (seconds)",
            SimulationOptions.MIN_EVENT_DURATION_SECONDS,
            SimulationOptions.MAX_EVENT_DURATION_SECONDS);
        if (slowdownDuration == null)
        {
            return;
        }

        Integer slowdownSpeed =
            readInteger("Slowdown speed (meters/second)", 1, 9);
        if (slowdownSpeed == null)
        {
            return;
        }

        Integer slowdownHeadway =
            readInteger("Slowdown headway (seconds)", 2, 20);
        if (slowdownHeadway == null)
        {
            return;
        }

        Long seed = readLong("Random seed");
        if (seed == null)
        {
            return;
        }

        // Construct and commit only after ALL inputs have been validated.
        SimulationOptions options = SimulationOptions.builder()
            .minGreenSeconds(minGreen).maxGreenSeconds(maxGreen)
            .queueSwitchThreshold(threshold).emergencyVehiclePercent(emergency)
            .eventCheckIntervalSeconds(interval).accidentChancePercent(accident)
            .slowdownChancePercent(slowdown)
            .accidentDurationSeconds(accidentDuration)
            .slowdownDurationSeconds(slowdownDuration)
            .slowdownSpeedMetersPerSecond(slowdownSpeed)
            .slowdownHeadwaySeconds(slowdownHeadway).randomSeed(seed).build();

        setConfiguration(
            new SimulationConfig(rates, duration, left, uTurn, options));
    }


    /**
     * Selects a configuration for the next run. Updating it during an active
     * run does not change the already constructed model for that run.
     *
     * @param config
     *            validated immutable configuration
     * @throws IllegalArgumentException
     *             if config is null
     */
    public void setConfiguration(SimulationConfig config)
    {
        if (config == null)
        {
            throw new IllegalArgumentException("Configuration cannot be null.");
        }
        nextConfig = config;
    }


    /**
     * Runs a fresh model and publishes results only after successful
     * completion. Shows the initial snapshot, each 60-second checkpoint, the
     * final snapshot, and any intervening signal switch or incident
     * start/expiration. This method refuses unconfigured or overlapping runs
     * and resets the running guard even if output or model execution raises an
     * exception.
     */
    public void runSimulation()
    {
        if (running)
        {
            view.showError("A simulation is already running.");
            return;
        }
        if (nextConfig == null)
        {
            view.showError("Configure the simulation before running it.");
            return;
        }

        running = true;
        try
        {
            SimulationConfig selected = nextConfig;
            activeModel = new SimulationModel(selected);
            SimulationSnapshot previous = activeModel.getSnapshot();
            view.showProgress(previous);

            while (!activeModel.isComplete())
            {
                activeModel.step();
                SimulationSnapshot current = activeModel.getSnapshot();
                int second = current.getElapsedSeconds();
                boolean phaseChanged = current.getPhaseSwitchCount() != previous
                    .getPhaseSwitchCount();
                boolean incidentsChanged = !current.getActiveEvents()
                    .equals(previous.getActiveEvents());

                if (second % PROGRESS_INTERVAL_SECONDS == 0
                    || activeModel.isComplete() || phaseChanged
                    || incidentsChanged)
                {
                    view.showProgress(current);
                }
                previous = current;
            }

            SimulationResults finished = activeModel.getResults();
            previousResults = latestResults;
            latestResults = finished;
            view.showResults(finished);
        }
        finally
        {
            running = false;
        }
    }


    /**
     * Displays the two most recently completed results, or explains why a
     * comparison cannot yet be performed.
     */
    public void compareRuns()
    {
        if (previousResults == null || latestResults == null)
        {
            view.showError("Run at least two simulations before comparing.");
            return;
        }
        view.showComparison(previousResults, latestResults);
    }


    /**
     * Reads an inclusive-range integer, re-prompting on blank, oversized,
     * nonnumeric, or out-of-range input. EOF closes further input and leaves
     * the caller's in-progress configuration uncommitted.
     *
     * @param label
     *            descriptive field name
     * @param minimum
     *            inclusive lower bound
     * @param maximum
     *            inclusive upper bound
     * @return validated integer, or null on EOF
     */
    private Integer readInteger(String label, int minimum, int maximum)
    {
        while (!inputClosed)
        {
            String raw =
                view.prompt(label + " [" + minimum + "-" + maximum + "]: ");
            if (raw == null)
            {
                inputClosed = true;
                return null;
            }
            if (raw.length() > MAX_INPUT_LENGTH)
            {
                view.showError(
                    "Input must be at most " + MAX_INPUT_LENGTH
                        + " characters.");
                continue;
            }
            String trimmed = raw.trim();
            if (trimmed.isEmpty())
            {
                view.showError(
                    "An integer is required; the field cannot be blank.");
                continue;
            }
            try
            {
                int value = Integer.parseInt(trimmed);
                if (value < minimum || value > maximum)
                {
                    view.showError(
                        "Enter a value between " + minimum + " and " + maximum
                            + ".");
                    continue;
                }
                return value;
            }
            catch (NumberFormatException exception)
            {
                view.showError(
                    "Enter a whole number between " + minimum + " and "
                        + maximum + ".");
            }
        }
        return null;
    }


    /**
     * Reads a signed 64-bit random seed. Accepts negative and positive values,
     * including Long.MIN_VALUE and Long.MAX_VALUE. Rejects blank, oversized,
     * noninteger, and overflowing input, re-prompting until valid or EOF.
     *
     * @param label
     *            descriptive field name
     * @return validated seed, or null on EOF
     */
    private Long readLong(String label)
    {
        while (!inputClosed)
        {
            String raw = view.prompt(label + " [signed 64-bit integer]: ");
            if (raw == null)
            {
                inputClosed = true;
                return null;
            }
            if (raw.length() > MAX_INPUT_LENGTH)
            {
                view.showError(
                    "Input must be at most " + MAX_INPUT_LENGTH
                        + " characters.");
                continue;
            }
            String trimmed = raw.trim();
            if (trimmed.isEmpty())
            {
                view.showError(
                    "A random seed is required; the field cannot be blank.");
                continue;
            }
            try
            {
                return Long.parseLong(trimmed);
            }
            catch (NumberFormatException exception)
            {
                view.showError("Enter a signed 64-bit whole number.");
            }
        }
        return null;
    }
}
