# 4WayChaos — Traffic Intersection Simulator

**CS 2114 | Group 95 | V1 Prototype**

**Team Members:** Ethan Gearhart, Xinchen, Makayla, Timothy Bungato

## Project Overview

4WayChaos is an offline, Java-based traffic simulator that models vehicle movement through a four-way intersection. Users configure traffic arrival rates, traffic signal durations, turning percentages, and simulation duration, then examine intersection performance through a terminal interface.

The V1 prototype uses a **Model-View-Controller (MVC)** architecture and a custom singly linked FIFO queue for each incoming approach. Every vehicle is represented by an individual object, allowing the model to track its arrival time, direction, intended turning movement, queue waiting time, and crossing progress.

### V1 Functionality

| Feature | Implementation Status |
|---|---|
| Four incoming traffic approaches | Implemented |
| Custom singly linked FIFO queues | Implemented |
| Individual vehicle objects | Implemented |
| Configurable deterministic arrivals | Implemented |
| Fixed-time North-South / East-West signals | Implemented |
| FIFO departures and constant-speed crossing | Implemented |
| Configurable left-turn and U-turn percentages | Movement assignment implemented; physical paths simplified |
| Vehicle and queue performance statistics | Implemented |
| Terminal configuration and results display | Implemented |
| Multi-run scenario comparison | Implemented |
| Input validation and recovery | Implemented for terminal MVP |
| Graphical interface and advanced traffic behavior | Not yet implemented |

### Architecture

**Model:** `SimulationModel`, `SimulationConfig`, `Lane`, `LinkedQueue<T>`, `Vehicle`, `TrafficSignal`, `StatisticsAccumulator`, `SimulationSnapshot`, `SimulationResults`, and supporting enums.

**View:** `TerminalView` — collects raw user input and formats model results.

**Controller:** `SimulationController` — processes commands, validates input, coordinates execution, and passes results to the view.

**Entry Point:** `Main`.

The architecture follows:

`TerminalView <-> SimulationController <-> SimulationModel`

The model does not depend on the controller or view. Simulation state is communicated through immutable snapshots and final results.

## Running the Program

### Interactive Application

1. Open the Java project in Eclipse.
2. Run `Main.java` as a Java Application.
3. Select **1 — Configure simulation** and enter the desired parameters.
4. Select **2 — Run simulation** to execute the model.
5. Select **3 — View latest results** or **4 — Compare last two runs**.
6. Select **5 — Exit** to close the application.

The application displays simulation progress and final statistics, including vehicle counts, average waiting time, and final, average, and maximum queue lengths.

### Standalone Model (No View Required)

To exercise the model independently, run `SimulationModelDriver.java` as a Java Application.

The driver constructs `SimulationConfig` and `SimulationModel` directly, executes simulation steps, and prints results without creating either `TerminalView` or `SimulationController`.

This demonstrates that the queue-backed simulation operates independently of the presentation layer.

### Running Unit Tests

The project uses **JUnit 4**.

In Eclipse, run individual `*Test.java` files using **Run As → JUnit Test**, or run the complete test suite.

Key tests:

- `LinkedQueueTest` — FIFO behavior, insertion, removal, empty queues, and exceptions.
- `LaneTest` — deterministic arrivals, turn assignment, green-light releases, and departure headway.
- `SimulationModelTest` — complete simulation execution, traffic statistics, signal behavior, and vehicle conservation.
- `SimulationControllerTest` — terminal workflow, validation, recovery, and scenario comparison.
- `TerminalViewTest` — view formatting and input handling independently of the simulation.

### Verified Example

A completed interactive run used the following configuration:

| Parameter | Value |
|---|---:|
| Arrival rate | 600 vehicles/hour per approach |
| North-South green | 30 seconds |
| East-West green | 30 seconds |
| Simulation duration | 120 seconds |
| Left turns | 20% |
| U-turns | 10% |

**Observed results:**

| Metric | Result |
|---|---:|
| Vehicles generated | 80 |
| Vehicles completed | 68 |
| Vehicles waiting | 10 |
| Vehicles crossing | 2 |
| Average waiting time | 8.03 seconds |

The simulation maintained the vehicle conservation relationship:

`Arrived = Waiting + Crossing + Completed`

`80 = 10 + 2 + 68`

The complete end-to-end interactive run finished without errors.

---

## Reflection

### 1. Specification vs. Actual Implementation

The implemented V1 closely follows the original specification. All major MVC components were retained, including individual vehicle objects, independent FIFO lane queues, fixed traffic signals, and immutable model outputs.

Several implementation details were refined while developing and testing individual classes.

| Area | Original Specification | Actual V1 Implementation | Reason / What Prompted the Change |
|---|---|---|---|
| Class architecture | Separate model, view, and controller classes | Original MVC class structure retained, with a standalone model driver | Preserved the agreed architecture; driver added to demonstrate model independence for the prototype rubric |
| Lane generation | `generateArrivals(int): void` | `generateArrivals(int, long): int` | Integration design: SimulationModel supplies globally unique vehicle IDs and receives the number generated for statistics |
| Lane release | `releaseNext(int): Vehicle` | `releaseNext(int, boolean): Vehicle` | MVC/model responsibility review: Signal permission is supplied externally so Lane does not duplicate traffic-light logic |
| Arrival state | Per-lane arrival accumulator | Added `lastGeneratedSecond` to enforce sequential arrival updates | Unit-test and validation planning: Prevent duplicate or skipped generation steps |
| Turning behavior | Accumulated left/U-turn credits | Separate STRAIGHT, LEFT, and U_TURN credits | Implementation refinement: More evenly distribute deterministic turning assignments |
| Vehicle movement | Individual vehicle position and state | `startCrossing()`, `advance()`, and `finishCrossing()` enforce state transitions | Implementation design: Keep vehicle lifecycle logic inside Vehicle while SimulationModel determines crossing completion |
| Traffic signals | Two alternating fixed-duration phases | Stateless modulo-based timing in `phaseAt()` and permission checks through `allows()` | Resolved simulation-clock design question without additional timers or threads |
| Statistics | Arrival, completion, waiting-time, and queue counters | Added `queueSampleCount` and `completedVehicleIds` | Testing and data-integrity requirements: Calculate queue averages and prevent duplicate completions |
| Snapshot | Immutable current-state summary | Added derived `waitingCount` and vehicle conservation validation | Integration verification: Catch inconsistent vehicle accounting before it reaches the view |
| Results | Immutable final statistics | Derived total waiting count from four queues and validated queue-statistic consistency | Data-integrity refinement: Prevent inconsistent final reports |
| View | Terminal prompts and result presentation | Added injectable `Scanner`/`PrintStream` constructor and EOF handling | JUnit testability and robust terminal-input handling |
| Controller | Menu workflow, input validation, execution, comparison | Added atomic configuration updates, EOF recovery, and active-versus-next configuration handling | Bad-input and integration testing: Prevent incomplete edits from corrupting simulation state |
| Tests | Normal and bad-input tests for public model methods | Expanded JUnit 4 tests for boundary cases, timing, exceptions, immutability, conservation, and MVC integration | Prototype rubric and testing discoveries |

**Change attribution:** These refinements came primarily from GenAI-assisted implementation planning and subsequent integration/testing. The team's original scope provided the required behavior. The adjustments to Lane signatures, vehicle state handling, data validation, and controller input behavior arose from making those requirements work together as a testable Java program.

The team should document any additional changes resulting from its own code reviews or discussions.

### 2. Validation and Testing Changes

The original specification placed terminal input validation in the controller and model invariants within model classes.

**V1 preserves that separation:**

- `SimulationController.readInteger()` rejects blank, nonnumeric, overly long, and out-of-range input, then re-prompts.
- `SimulationConfig` independently rejects invalid arrival rates, signal durations, simulation durations, and turning percentages.
- `LinkedQueue` rejects null insertion and empty dequeue/peek operations.
- `Vehicle` rejects invalid construction, movement, and lifecycle transitions.
- `Lane` enforces valid arrival-step ordering and release conditions.
- `SimulationSnapshot` and `SimulationResults` reject inconsistent vehicle counts.

The controller does not silently replace invalid values with defaults.

Testing was expanded beyond the original minimum. In addition to normal and invalid-input tests for public methods, V1 includes integration tests for multi-lane operation, signal boundaries, vehicle conservation, deterministic repeatability, and maximum supported traffic loads.

### 3. Did the Model Stand Alone?

**Yes. The V1 model can operate without either the controller or the view.**

`SimulationModelDriver` constructs the configuration and model directly, runs the simulation, and retrieves statistics without creating `TerminalView` or `SimulationController`.

The model owns all vehicle generation, queues, traffic signal logic, vehicle movement, and statistics. It does not access `Scanner`, print to the terminal, or reference the view.

The controller is responsible for coordinating user commands and simulation execution. The view receives immutable `SimulationSnapshot` and `SimulationResults` objects.

**No simulation behavior had to be moved into the view or controller to make the program run.**

The most important architectural refinement was explicitly separating signal permission from queue removal: `TrafficSignal` determines whether a direction has green, while `Lane` manages the actual FIFO departure. `SimulationModel` coordinates both.

This separation should allow future graphical interfaces and traffic-control algorithms to reuse the existing model with limited changes.

### 4. Original Unknowns: Resolved vs. Open

The original scope identified several implementation questions.

| Original Unknown | Status | What We Learned / Implemented |
|---|---|---|
| Simulation clock and internal timing | Resolved for MVP | One-second discrete simulation steps; no real-time waiting |
| Vehicle representation and movement | Resolved for simplified MVP | Individual Vehicle objects with states, timestamps, turning movements, and crossing progress |
| FIFO queue releases and waiting-time calculations | Resolved | Four independent LinkedQueue-backed lanes, traffic-signal permissions, departure headway, and recorded crossing-start times |
| Running traffic statistics | Resolved | StatisticsAccumulator maintains totals and queue-length samples |
| Vehicle cleanup and memory management | Resolved for MVP | Completed vehicles are removed from the active crossing list and become eligible for garbage collection |
| GUI event synchronization | Still open | No GUI in V1; controller-driven snapshots provide a future integration point |
| Smooth graphical animation | Still open | Position updates are discrete; rendering and interpolation have not been implemented |

The completed-vehicle ID set retains IDs for duplicate-detection purposes. It does not retain entire Vehicle objects, although its memory usage increases with the number of completed vehicles.

### 5. What Does Not Work Yet?

The terminal MVP is functional, but several features remain simplified or unimplemented.

**Current limitations:**

- Turning movements are assigned to vehicles but do not yet affect their physical crossing paths or movement duration.
- Vehicles travel at constant speed; acceleration, braking, and realistic queue spacing are not modeled.
- There are no separate yellow-light or all-red transition periods.
- Opposing left-turn conflicts, collision avoidance, and detailed intersection geometry are not modeled.
- Vehicle generation is deterministic rather than probabilistic.
- No graphical interface or vehicle animation is implemented.
- Traffic signals cannot adapt to congestion.
- Pedestrians, emergency vehicles, accidents, and traffic slowdowns are not implemented.

**Bad-input handling:** The terminal implementation rejects invalid numeric and text input. Configuration changes are staged for the next simulation, and overlapping runs are rejected. GUI button-spam behavior has not been implemented because V1 has no GUI buttons.

**Testing limitations:** No failing tests were reported in the completed component verification and demonstrated run. However, the complete project still requires a final regression check after all files are integrated, and future features will require additional tests.

The current model is therefore a functional traffic-flow prototype, not a fully realistic traffic-engineering simulation.

---

## Next Steps

### 1. Remaining MVP Work — Ordered Plan for Deliverable 4

V1 establishes the main simulation pipeline. Deliverable 4 will focus on improving completeness, validating behavior, and adding a non-MVP feature.

| Priority | Remaining Work | Specific Classes / Methods | Proposed Owner |
|---|---|---|---|
| 1 | Complete full-project regression testing and correct any integration defects | All test suites; `SimulationModel.step()`, `SimulationController.launch()` | Ethan, with component owners |
| 2 | Improve representation of turning movements beyond their current labels | `Vehicle`, `Lane.assignTurn()`, `SimulationModel.step()` | Xinchen + Ethan |
| 3 | Review signal transitions and crossing behavior at phase boundaries | `TrafficSignal.phaseAt()`, `allows()`, `SimulationModel.step()` | Ethan |
| 4 | Verify all original bad-input cases through the full interactive workflow | `SimulationController.configureSimulation()`, `readInteger()`, `runSimulation()` | Timothy |
| 5 | Refine terminal usability and results formatting | `TerminalView.showProgress()`, `showResults()`, `showComparison()` | Makayla |
| 6 | Implement and test the required non-MVP change feature | Adaptive signal methods described below | Ethan + Timothy |
| 7 | Run full regression tests and update documentation | All affected tests, `README.md` | All team members |

The proposed ownership follows the division of work in our specification; final assignments require team confirmation.

### 2. Non-MVP Change Feature: Adaptive Traffic Signals

**Selected feature:** Queue-responsive adaptive traffic signal control.

This feature was identified as a stretch goal in the original scope and is not part of the V1 MVP.

Currently, signal phases alternate at fixed user-defined intervals. Deliverable 4 will introduce an optional adaptive mode where the duration of a green phase can change based on the number of vehicles waiting in each direction.

**Proposed behavior:**

- Keep the existing fixed-time mode available.
- Introduce a minimum and maximum green duration.
- After the minimum green duration, evaluate queue lengths for the active and opposing directions.
- Extend or end the current phase based on relative congestion.
- Enforce a maximum green duration to prevent one direction from being indefinitely blocked.

The exact decision threshold and timing rules will be finalized before implementation.

#### Predicted Class and Method Changes

This prediction is being recorded **before** the feature is implemented. Deliverable 4 will compare these expected changes against the actual modifications.

| Class | Predicted Method-Level Changes | Why |
|---|---|---|
| `SimulationConfig` | Extend constructor/configuration to include signal mode, minimum green, and maximum green; add corresponding getters and validation | Store adaptive settings |
| New `SignalMode` enum | Define `FIXED` and `ADAPTIVE` | Represent selected strategy |
| `TrafficSignal` | Add an adaptive phase-update method, such as `updatePhase(int elapsedSecond, int nsQueue, int ewQueue)`; add a current-phase getter; update phase-permission handling | Implement congestion-responsive timing |
| `SimulationModel` | Modify `step()` to provide queue counts before release decisions; adjust `getSnapshot()` to report the appropriate current adaptive phase | Integrate adaptive control with the existing simulation |
| `SimulationController` | Modify `configureSimulation()` to accept signal mode and adaptive parameters | Allow users to select adaptive mode |
| `TerminalView` | No changes expected; existing `prompt()`, `showProgress()`, and `showResults()` can present adaptive configurations and outcomes | Existing presentation API should be reusable |

**Classes predicted not to change:**

| Class | Why No Change Should Be Necessary |
|---|---|
| `LinkedQueue<T>` | FIFO insertion/removal is independent of signal strategy |
| `Vehicle` | Vehicle state and movement behavior do not depend on how green durations are selected |
| `Lane` | Already accepts external green-light permission through `releaseNext()` |
| `StatisticsAccumulator` | Existing arrival, completion, wait, and queue metrics apply to both strategies |
| `SimulationSnapshot` | Already stores the current signal phase and queue lengths |
| `SimulationResults` | Existing metrics can compare adaptive and fixed-time scenarios |
| `Main` | Application entry point remains unchanged |
| `SimulationModelDriver` | Existing fixed-time demonstration should remain functional |

The main architectural challenge is that fixed-time signals calculate phase directly from elapsed time, whereas adaptive timing requires retaining information about previous phase decisions. This may require replacing the purely time-based phase calculation with additional internal signal state.

**Success criterion:** Run identical traffic-arrival scenarios under fixed and adaptive timing and compare waiting times, throughput, and queue lengths.

### 3. Stretch Goal Commitments

For Deliverable 4, the proposed feature priorities are:

| Original Stretch Goal | Decision |
|---|---|
| Adaptive traffic lights | **Committed / selected change feature** |
| Randomized turning | Deferred; deterministic turning retained |
| Graphical simulation and animations | Not committed for Deliverable 4 |
| Emergency vehicles, accidents, and slowdowns | Dropped from Deliverable 4 scope |
| Pedestrian crossing simulation | Dropped from Deliverable 4 scope |
| Expanded lanes and dedicated turning lanes | Dropped from Deliverable 4 scope |

These decisions prioritize a working adaptive signal feature over developing several incomplete extensions. Features dropped from Deliverable 4 may be reconsidered for V_Final, depending on the time available.

### 4. New Tests Required

All new tests will use JUnit 4 and will supplement existing regression tests.

| Test Class | New Test Cases | Expected Behavior |
|---|---|---|
| `SimulationConfigTest` | Valid adaptive configuration; invalid mode; negative/zero timing; maximum below minimum | Accept valid settings; reject invalid configurations |
| `TrafficSignalTest` | Unequal queues; equal queues; empty queues; minimum/maximum green boundaries | Adaptive phase changes obey timing and congestion rules |
| `SimulationModelTest` | Fixed versus adaptive timing; phase switching; multi-lane congestion; vehicle conservation | Adaptive control affects signal decisions without corrupting traffic state |
| `SimulationControllerTest` | Adaptive-mode menu selection; invalid settings; repeated configuration changes | Invalid input produces re-prompts without affecting active simulation state |
| `VehicleTest` / `LaneTest` | Updated turning-path behavior and boundary cases, if turning logic is expanded | Correct movement and FIFO handling |
| Existing regression tests | Re-run all tests after integration | Fixed-time behavior and existing MVC functionality remain unchanged |

Additional adaptive-control edge cases should include traffic present in only one direction, both directions empty, rapidly changing queue lengths, and protection against a direction remaining red indefinitely.

### 5. Division of Work — Deliverable 4

| Member | Primary Responsibilities |
|---|---|
| Ethan Gearhart | Adaptive signal algorithm, SimulationModel integration, signal/model tests, final integration |
| Xinchen | Vehicle and Lane improvements, turning behavior, FIFO regression tests |
| Makayla | Terminal interface improvements, usability checks, display tests |
| Timothy Bungato | Controller configuration changes, invalid-input recovery, controller tests |
| All members | Code review, integration testing, documentation updates, and final verification |

**Integration approach:** Agree on changed method signatures before implementation; develop and test components separately; merge the adaptive feature incrementally; then run the original tests to confirm that fixed-time operation is unaffected.

---

# Attribution

**Attribution Convention:** Ethan Gearhart served as the primary contributor for the V1 prototype, overseeing implementation, integration, testing, and verification. GenAI assisted with code generation, software design, documentation, and unit test development. All code was incorporated into the project and reviewed or tested during development.

## Classes and Application Files

| File | Primary Contributor | Nature of Work |
|---|---|---|
| `Direction.java` | Timothy | GenAI-assisted enum definition and implementation |
| `Turn.java` | Ethan | GenAI-assisted enum definition and implementation |
| `SignalPhase.java` | Ethan | GenAI-assisted enum definition and implementation |
| `VehicleState.java` | Xinchen | GenAI-assisted enum definition and implementation |
| `LinkedQueue.java` | Ethan | GenAI-assisted FIFO queue implementation, Javadocs, and integration |
| `Vehicle.java` | Makayla | GenAI-assisted object modeling, state transitions, validation, and testing |
| `SimulationConfig.java` | Makayla | GenAI-assisted configuration management, validation, and testing |
| `Lane.java` | Ethan | GenAI-assisted queue management, vehicle arrivals, turning logic, and testing |
| `TrafficSignal.java` | Timothy | GenAI-assisted signal timing, phase control, validation, and testing |
| `StatisticsAccumulator.java` | Xinchen | GenAI-assisted statistical calculations, queue sampling, and testing |
| `SimulationSnapshot.java` | Ethan | GenAI-assisted immutable state representation, validation, and testing |
| `SimulationResults.java` | Timothy | GenAI-assisted final statistics reporting, validation, and testing |
| `SimulationModel.java` | Xinchen | GenAI-assisted simulation engine development, integration, and testing |
| `TerminalView.java` | Ethan | GenAI-assisted terminal interface development, formatting, and testing |
| `SimulationController.java` | Timothy | GenAI-assisted application control flow, validation, and integration testing |
| `Main.java` | Ethan | GenAI-assisted application initialization and execution |
| `SimulationModelDriver.java` | Xinchen | GenAI-assisted standalone model demonstration and verification |

## Unit Test Files

| File | Primary Contributor | Nature of Work |
|---|---|---|
| `LinkedQueueTest.java` | Xinchen | GenAI-assisted JUnit 4 test development for FIFO operations and exceptions |
| `VehicleTest.java` | Ethan | GenAI-assisted testing of vehicle construction, movement, state transitions, and validation |
| `SimulationConfigTest.java` | Timothy | GenAI-assisted configuration validation, boundary testing, and immutability verification |
| `LaneTest.java` | Ethan | GenAI-assisted testing of arrivals, queues, turning distributions, and departures |
| `TrafficSignalTest.java` | Xinchen | GenAI-assisted signal timing and validation tests; debugging and correction of test expectations |
| `StatisticsAccumulatorTest.java` | Timothy | GenAI-assisted statistics, completion tracking, and queue sampling tests |
| `SimulationSnapshotTest.java` | Xinchen | GenAI-assisted state validation, vehicle conservation, and immutability tests |
| `SimulationResultsTest.java` | Ethan | GenAI-assisted final report validation, statistical consistency, and boundary tests |
| `SimulationModelTest.java` | Xinchen | GenAI-assisted simulation integration tests, traffic behavior verification, and edge-case testing |
| `TerminalViewTest.java` | Ethan | GenAI-assisted input/output testing, formatting verification, and exception handling |
| `SimulationControllerTest.java` | Timothy | GenAI-assisted application workflow, input validation, error recovery, and integration testing |


## V1 Summary

V1 provides a working, queue-backed, four-way traffic intersection simulator with individual vehicle objects, configurable traffic parameters, fixed-time signals, deterministic arrivals, and performance statistics.

The complete interactive simulation was demonstrated successfully, and the core model remains independently executable without the terminal interface.

Deliverable 4 will focus on improving simulation fidelity and extending traffic signal control while preserving the MVC boundaries established in V1.