package defaultPackage;

import java.util.EnumMap;
import java.util.Map;

/**
 * Coordinates the 4WayChaos terminal application. Reads raw commands through
 * TerminalView, validates user input, creates configurations, runs
 * SimulationModel, and supplies immutable snapshots and results to the view. No
 * traffic or queue logic lives here.
 */
public class SimulationController
{

    /** Maximum number of characters accepted in one input line. */
    private static final int MAX_INPUT_LENGTH = 80;

    /** Number of completed simulation seconds between progress updates. */
    private static final int PROGRESS_INTERVAL_SECONDS = 60;

    private final TerminalView view;
    private SimulationModel activeModel;
    private SimulationConfig nextConfig;
    private SimulationResults latestResults;
    private SimulationResults previousResults;
    private boolean running;
    private boolean inputClosed;

    /**
     * Constructs a controller using the provided presentation layer.
     *
     * @param view
     *            terminal view for all application input and output
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
        this.activeModel = null;
        this.nextConfig = null;
        this.latestResults = null;
        this.previousResults = null;
        this.running = false;
        this.inputClosed = false;
    }


    /**
     * Shows the main menu and processes commands until Exit or EOF. Invalid
     * selections produce an error and allow another attempt. The controller
     * never closes the view's input/output streams.
     */
    public void launch()
    {
        while (!inputClosed)
        {
            view.showMenu();
            String command = view.prompt("Select an option: ");
            if (command == null)
            {
                inputClosed = true;
                break;
            }
            if (command.length() > MAX_INPUT_LENGTH)
            {
                view.showError(
                    "Input must be at most " + MAX_INPUT_LENGTH
                        + " characters.");
                continue;
            }

            switch (command.trim())
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
     * Collects a complete configuration through validated integer prompts.
     * Keeps the previous configuration until every field is valid. If input
     * ends during setup, leaves the previous configuration unchanged.
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

        Integer ns = readInteger(
            "North-South green duration (seconds)",
            SimulationConfig.MIN_GREEN_SECONDS,
            SimulationConfig.MAX_GREEN_SECONDS);
        if (ns == null)
        {
            return;
        }

        Integer ew = readInteger(
            "East-West green duration (seconds)",
            SimulationConfig.MIN_GREEN_SECONDS,
            SimulationConfig.MAX_GREEN_SECONDS);
        if (ew == null)
        {
            return;
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
            "Left-turn percentage",
            SimulationConfig.MIN_TURN_PERCENT,
            SimulationConfig.MAX_TURN_PERCENT);
        if (left == null)
        {
            return;
        }

        // Enforce the combined turning limit before creating the model object.
        Integer uTurn = readInteger(
            "U-turn percentage",
            SimulationConfig.MIN_TURN_PERCENT,
            SimulationConfig.MAX_TURN_PERCENT - left);
        if (uTurn == null)
        {
            return;
        }

        // Commit atomically only after the entire configuration is valid.
        setConfiguration(
            new SimulationConfig(rates, ns, ew, duration, left, uTurn));
    }


    /**
     * Selects a validated configuration for the next simulation run. A
     * currently executing model continues using its existing settings.
     *
     * @param config
     *            immutable configuration for the next run
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
     * Runs a fresh model from the selected configuration. Displays the initial
     * state, progress every 60 simulated seconds, and final results. A
     * completed run becomes latestResults and displaces the prior latest into
     * previousResults. Refuses unconfigured or overlapping runs.
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
            // Capture this run's configuration; subsequent changes apply later.
            SimulationConfig selected = nextConfig;
            activeModel = new SimulationModel(selected);
            view.showProgress(activeModel.getSnapshot());

            while (!activeModel.isComplete())
            {
                activeModel.step();
                SimulationSnapshot snapshot = activeModel.getSnapshot();
                int second = snapshot.getElapsedSeconds();
                if (second % PROGRESS_INTERVAL_SECONDS == 0
                    || activeModel.isComplete())
                {
                    view.showProgress(snapshot);
                }
            }

            // Publish new results only after successful model completion.
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
     * Displays the two most recently completed simulation results. If fewer
     * than two successful runs exist, displays an error instead.
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
     * Reads and validates one bounded integer. Rejects blank, nonnumeric,
     * excessively long, or out-of-range input and re-prompts until valid.
     * End-of-input is signaled by null, not by a fabricated default.
     *
     * @param label
     *            descriptive parameter name for the prompt
     * @param minimum
     *            minimum permitted integer (inclusive)
     * @param maximum
     *            maximum permitted integer (inclusive)
     * @return validated integer, or null if input reaches EOF
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
            catch (NumberFormatException e)
            {
                view.showError(
                    "Enter a whole number between " + minimum + " and "
                        + maximum + ".");
            }
        }
        return null;
    }
}
