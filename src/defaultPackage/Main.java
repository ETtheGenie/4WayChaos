package defaultPackage;

/**
 * Entry point for the 4WayChaos traffic simulator. Initializes the terminal
 * view and simulation controller, then launches the interactive application.
 * The simulation model is created and managed by the controller when the user
 * starts a simulation.
 */
public class Main
{

    /**
     * Starts the 4WayChaos application.
     *
     * @param args
     *            command-line arguments (unused)
     */
    public static void main(String[] args)
    {

        TerminalView view = new TerminalView();

        SimulationController controller = new SimulationController(view);

        controller.launch();
    }
}
