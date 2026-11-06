# 4WayChaos

**Group 95:** Ethan Gearhart, Xinchen, Makayla, Timothy Bungato  
**Course:** CS 2114  
**Target Platform:** Java 17 Terminal-Based Four-Way Traffic Simulator  

---

## Overview & Architecture

4WayChaos is an offline, terminal-based traffic simulator designed to model vehicle queueing, traffic signal state transitions, and intersection performance metrics at a four-way junction. 

The system strictly follows the **Model-View-Controller (MVC)** architectural pattern:
* **Model:** Manages custom queue state, discrete time-step transitions, and metrics computation without any UI dependencies.
* **Controller:** Intercepts and sanitizes user terminal input, updates model configuration, and executes simulation ticks.
* **View:** Formats menus, prompt messages, validation warnings, and statistical output.

---

## How to Run the System

### 1. Running the Standalone Model (Driver & Unit Tests)
The core model operates entirely independently of the presentation layer. You can exercise and verify the model using either the standalone headless driver or the automated JUnit test suite.

* **Run via Standalone Model Driver (No View/Controller):**
  ```bash
  javac -d bin src/model/*.java src/driver/ModelDriver.java
  java -cp bin driver.ModelDriver
  ```
  *Behavior:* Directly instantiates `IntersectionModel`, populates directional queues, ticks the simulation clock for 60 discrete steps, and prints the generated `SimulationMetrics` snapshot directly to `System.out`.

* **Run via JUnit 5 Test Suite:**
  ```bash
  mvn test
  ```
  *Or directly run test classes in your IDE:*
  * `SinglyLinkedQueueTest` — Validates custom generic FIFO queue operations (`enqueue`, `dequeue`, `peek`, `isEmpty`, null handling).
  * `IntersectionModelTest` — Validates discrete time-step execution, vehicle arrivals, traffic light toggles, and metrics collection.

---

### 2. Running the Thin / Stubbed View
The thin terminal view demonstrates end-to-end data flow mediated strictly through the controller layer.

* **Run the Full Application:**
  ```bash
  javac -d bin src/**/*.java
  java -cp bin Main
  ```
  *Behavior:* `Main.java` instantiates `IntersectionModel`, `ConsoleView`, and `SimulationController`, launching the interactive terminal menu where you can input arrival rates, set signal timing, run a simulation, and view output reports.

---

## Reflection

### What Changed From Your Specification?
* **Classes Added / Refactored:**
  * Added `SignalPhase` (Enum) inside `TrafficSignal.java` to replace hardcoded phase strings (`"GREEN"`, `"RED"`) with type-safe state representations. *Prompted by refactoring during initial Queue integration to eliminate string-parsing errors during light toggles.*
  * Refactored `SimulationMetrics` into an immutable snapshot record generated via `IntersectionModel.getMetrics()`. *Prompted by strict MVC decoupling to prevent the view layer from holding mutable references to active model statistics.*
* **Method Signatures Changed:**
  * Modified `SinglyLinkedQueue.enqueue(E item)` to return `void` instead of `boolean`, throwing an explicit `IllegalArgumentException` on `null` items. *Prompted by strict adherence to custom data structure invariant requirements.*
  * Updated `SimulationController.processArrivalRateInput(String rawInput)` to return `boolean` indicating validation success or failure. *Prompted by the need to support interactive user re-prompt loops in `ConsoleView` without crashing.*
* **Validation Location Changes:**
  * String parsing and input sanitization (regex string checks) were restricted exclusively to `SimulationController.processArrivalRateInput()` and `processDurationInput()`. However, defensive boundary checks (e.g., throwing `IllegalArgumentException` for negative arrival rates or duration $< 10$ seconds) were retained inside `IntersectionModel` and `TrafficSignal` constructors. *Prompted by a defense-in-depth design strategy ensuring the model remains corrupt-proof even when invoked programmatically outside the controller.*
* **Test Cases Adjusted:**
  * Expanded `SinglyLinkedQueueTest` to include explicit exception-throwing assertions (`assertThrows(NoSuchElementException.class, ...)` on empty `dequeue()`) that were omitted in the initial specification draft. *Prompted by edge-case test coverage gaps identified during JUnit execution.*

---

### Did the Model Really Stand Alone?
**Yes.** `IntersectionModel`, `SinglyLinkedQueue`, `Vehicle`, `TrafficSignal`, and `SimulationMetrics` contain zero imports, references, or calls to `ConsoleView`, `Scanner`, or `SimulationController`. 

To prove complete independence, `ModelDriver.java` instantiates and ticks `IntersectionModel` entirely in memory without ever initializing a view or controller. Nothing was moved across the Model/View boundary to force execution, and state data crosses the boundary strictly as an immutable `SimulationMetrics` snapshot object passed by the controller.

---

### What Happened to Your Unknowns?
* **Figured Out:**
  * *Discrete Time Clock:* Resolved by implementing a step-based clock incremented inside `IntersectionModel.step()` rather than using real-time `Thread.sleep()` delays. This allows 3,600 simulated seconds to execute deterministically in milliseconds.
  * *Queue Buffer & Object Dereferencing:* Verified that vehicles dequeued upon crossing the intersection are naturally garbage collected once removed from `SinglyLinkedQueue`, preventing memory leaks during long runs.
  * *FIFO Directional Buffers:* Proved that maintaining four distinct `SinglyLinkedQueue<Vehicle>` buffers (North, South, East, West) correctly prevents cross-traffic blocking while enforcing arrival-order processing per lane.
* **Still Open:**
  * *Stochastic vs. Fixed Arrival Scheduling:* Fine-tuning Poisson-distributed arrival algorithms versus deterministic arrival accumulators to ensure realistic traffic spikes during short 60-second test runs.

---

### What Doesn't Work Yet?
* **Mid-Run Configuration Updates:** Editing signal durations while the simulation tick loop is actively running is currently blocked; changes are queued and only apply on the subsequent simulation run.
* **Multi-Lane Expansion:** Current MVP implementation models exactly one inbound queue per cardinal direction; dedicated turning lanes (left-turn pockets) remain unintegrated in the current slice.
* **Surviving Invalid Strings in Deep Sub-Menus:** While standard integer parsing errors are caught on the main setup menu, typing non-numeric strings into nested prompt loops currently outputs a generic warning and returns to the main menu rather than inline re-prompting.

---

## Next Steps

### 1. MVP Completion Plan (In Execution Order)
1. **Refine Sub-Menu Re-prompt Loops:** Enhance `SimulationController` to keep users inside invalid input prompts until a valid integer is supplied, rather than resetting to the top-level menu.
2. **Dynamic Queue Length Tracking:** Enhance `IntersectionModel` to track peak max queue length continuously across all individual ticks rather than sampling at snapshot intervals.
3. **Scenario Comparison Output:** Update `ConsoleView` to format side-by-side metric comparisons between sequential simulation runs.

---

### 2. Change Feature: Emergency Vehicle Priority Override (EVPO)
We are adding **Emergency Vehicle Priority Override (EVPO)** as our non-trivial feature for Deliverable 4. 

* **Feature Description:** Users can trigger an emergency vehicle arrival (e.g., ambulance or fire truck) on a specific approach lane. When triggered, the model immediately overrides standard signal timing, forces the target approach signal to `GREEN`, sets all conflicting directions to `RED`, enqueues a priority `Vehicle` object, and releases it through the intersection before resuming normal signal timing cycles.

* **Predicted Impact on Codebase:**
  * **Classes/Methods Touch Points (Will Change):**
    * `IntersectionModel`: Add `triggerEmergencyOverride(Direction direction)` to set override flags, interrupt active phase timers, and enqueue an emergency vehicle.
    * `TrafficSignal`: Add `forcePhaseOverride(SignalPhase phase)` to immediately mutate light state regardless of remaining phase duration.
    * `Vehicle`: Add `boolean isEmergency` attribute and `public boolean isEmergency()` getter.
    * `SimulationController`: Add `processEmergencyCommand(String rawInput)` to parse user override requests during setup/execution.
    * `ConsoleView`: Add `displayEmergencyAlert(Direction direction)` to format visual override alerts.
  * **Classes Predicted NOT to Change:**
    * `SinglyLinkedQueue`: **Will not change.** The custom queue data structure operates identically using existing `enqueue()` and `dequeue()` methods regardless of whether the vehicle holds an emergency flag.
    * `SimulationMetrics`: **Will not change.** Priority vehicles are tracked within standard arrival, departure, and wait time calculation logic.

---

### 3. Stretch Goals Commitment
* **Committed Stretch Goals:**
  1. *Emergency Vehicle Priority Override (Change Feature)*
  2. *Adaptive Traffic Signals:* Signal green phase duration dynamically extends if the active queue length exceeds a threshold.
* **Dropped Stretch Goals:**
  1. *Real-Time 2D Graphical GUI:* Dropped to focus strictly on algorithm correctness, custom data structure performance, and robust terminal execution.
  2. *Pedestrian Crosswalk Buttons:* Dropped due to scope constraints.

---

### 4. New Tests Needed
The added functionality in Deliverable 4 will be covered by the following new automated tests:
* `IntersectionModelTest.testEmergencyOverrideForcesGreenSignal()` — Verifies that invoking `triggerEmergencyOverride(Direction.NORTH)` immediately sets North-South signal phase to `GREEN`.
* `IntersectionModelTest.testEmergencyOverrideInvalidDirectionThrowsException()` — Verifies that passing a `null` direction to `triggerEmergencyOverride()` throws `IllegalArgumentException` (**Bad Input Case**).
* `TrafficSignalTest.testForcePhaseOverrideResetsPhaseTimer()` — Verifies internal phase timer resets to 0 when forced into an override phase.
* `VehicleTest.testEmergencyVehicleFlagInitialization()` — Verifies constructor correctly flags priority status.

---

### 5. Remaining Task Assignments

| Task / Feature | Team Member Owner | Target Completion Milestone |
| :--- | :--- | :--- |
| **Emergency Priority Logic** (`IntersectionModel`, `TrafficSignal`) | Xinchen | Deliverable 4 — Phase 1 |
| **Controller Integration & Re-prompt Loops** (`SimulationController`) | Ethan Gearhart | Deliverable 4 — Phase 1 |
| **View Emergency Banners & Reports** (`ConsoleView`) | Makayla | Deliverable 4 — Phase 2 |
| **JUnit Suite Expansion & Smoke Tests** (Test Files, `Main`) | Timothy Bungato | Deliverable 4 — Phase 2 |

---

## Attribution & Generative AI Note

### Section & File Attribution Table

| File / Class Name | Primary Contributors | Specific Contributions |
| :--- | :--- | :--- |
| `SinglyLinkedQueue.java` | Xinchen | Wrote custom singly-linked FIFO queue implementation, node references, and pointer operations. |
| `Vehicle.java` | Xinchen | Wrote vehicle state container, timestamp tracking, and wait time calculation methods. |
| `TrafficSignal.java` | Xinchen & Ethan Gearhart | Wrote phase state machine, timer tick increments, and green phase reconfigurations. |
| `SimulationMetrics.java` | Xinchen & Timothy Bungato | Wrote immutable statistical record container and aggregate metrics getters. |
| `IntersectionModel.java` | Xinchen | Wrote four-lane queue owner, discrete time step tick logic, and vehicle release algorithms. |
| `ConsoleView.java` | Makayla | Wrote terminal prompt formatting, menu rendering, error alerts, and summary output views. |
| `SimulationController.java` | Ethan Gearhart | Wrote raw input sanitization, regex range parsers, error recovery loops, and step orchestration. |
| `Main.java` | Timothy Bungato | Wrote application entry point, bound Model-View-Controller references, and managed build scripts. |
| `ModelDriver.java` | Timothy Bungato | Wrote standalone headless driver demonstrating model execution independent of view. |
| `SinglyLinkedQueueTest.java` | Timothy Bungato & Xinchen | Wrote JUnit tests covering `enqueue`, `dequeue`, `peek`, `isEmpty`, null handling, and empty queue exceptions. |
| `IntersectionModelTest.java` | Timothy Bungato | Wrote JUnit tests covering discrete time step generation, signal light changes, and metrics calculations. |

---

### Generative AI Disclosure Note
Generative AI tools (Gemini and ChatGPT) were used as adaptive thought partners during the development of Deliverable 3. Specifically, Generative AI was utilized for:
1. **Edge-Case Brainstorming:** Identifying non-obvious bad-input test cases (e.g., negative arrival rates, mid-run edits, and empty queue dequeues).
2. **README Structure & Rubric Formatting:** Formatting markdown tables, structuring reflection sections, and auditing compliance against assignment rubric criteria.
3. **Refactoring Guidance:** Recommending type-safe enum replacements (`SignalPhase`) for string-based phase flags.

All AI-generated recommendations were manually reviewed, tested, and integrated by Group 95 members to ensure complete technical accuracy and alignment with course guidelines.
