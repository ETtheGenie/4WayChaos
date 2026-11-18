# 4WayChaos — Four-Way Intersection Traffic Simulator

**Course:** CS 2114 · **Group:** 95 · **Deliverable:** 4 (V2)  
**Team:** Ethan Gearhart, Xinchen, Makayla, Timothy Bungato  
**Interface:** Offline Java terminal application · **Tests:** JUnit 4

## Overview

4WayChaos simulates traffic approaching a four-way intersection from NORTH, SOUTH, EAST, and WEST. Vehicles enter per-approach, singly linked FIFO queues, wait for permission to cross, traverse a simplified 20-meter intersection, and contribute to throughput, waiting-time, and queue statistics. Users configure and rerun scenarios through a five-option terminal menu, then compare the two most recent runs.

Deliverable 4 extends the original fixed-time simulation with **adaptive signals, randomized turning, emergency vehicle priority requests, accidents, and slowdowns**. The simulation remains completely offline, text based, and independent of real-time clock delays. Each `SimulationModel.step()` represents one simulated second.

### Implemented behavior

- **Traffic:** Independent configurable arrival rates for four approaches (0–1,800 vehicles/hour each), deterministic arrival scheduling, distinct vehicle IDs, and FIFO release.
- **Turning:** Seeded randomized LEFT and U-turn selections; STRAIGHT receives the remaining probability. Turns are tracked as vehicle intentions but do not yet change paths or travel distances.
- **Adaptive signals:** Two mutually exclusive phases, `NS_GREEN` and `EW_GREEN`; configurable minimum and maximum green times and queue-difference threshold. A phase may switch after its minimum when opposing serviceable demand warrants it, and must switch at its maximum.
- **Emergency requests:** A configurable fraction of arrivals is tagged `EMERGENCY`. An eligible emergency waiting on the opposing axis can request a phase switch after minimum green, but cannot bypass FIFO order or override maximum-green fairness. Requests from accident-blocked approaches are ignored until unblocked.
- **Incidents:** At configurable simulated intervals, each approach without an active event independently draws ACCIDENT, SLOWDOWN, or NONE. Accidents prevent new departures on the affected approach. Slowdowns reduce crossing speed and increase departure headway. At most one active incident exists per approach; different approaches can have incidents simultaneously.
- **Metrics:** Total and emergency arrivals/completions, mean completed-vehicle queue wait, mean completed-emergency queue wait, waiting/crossing counts, average/maximum/final queues per approach, incident starts by type, and adaptive phase-switch count.
- **Repeatability:** One supplied `long` random seed deterministically seeds separate random streams for turn selection, emergency classification, and incidents.
- **Robustness:** Out-of-range and malformed terminal input is rejected and re-prompted; configuration is committed only after all fields pass validation; completed runs are immutable reports.

## How to Compile and Run

### Eclipse (recommended for course submission)

1. Open the **4WayChaos** Java project in Eclipse, or import it with **File → Import → Existing Projects into Workspace**.
2. Ensure all `.java` files belong to package `defaultPackage` and use the same project source folders. The project uses Java 8-compatible language features or newer.
3. Ensure the project's build path contains **JUnit 4** for the `*Test.java` classes (**Project → Properties → Java Build Path → Libraries → Add Library → JUnit → JUnit 4**). If Eclipse already has JUnit 4 configured, no additional setup is needed.
4. Eclipse automatically compiles Java source when **Project → Build Automatically** is enabled. Otherwise select **Project → Clean**, then **Project → Build Project**. Resolve build errors before running.
5. Right-click `Main.java` → **Run As → Java Application** to start the interactive simulator.
6. To demonstrate that the model works independently of the view/controller, right-click `SimulationModelDriver.java` → **Run As → Java Application**. The driver needs **no input** and should complete a reproducible 180-second simulation with conservation checks.

**Terminal menu:** `1` Configure simulation · `2` Run simulation · `3` View latest results · `4` Compare last two runs · `5` Exit.

### Command line (optional)

If your project stores production sources under `src/defaultPackage/` and tests separately under `test/defaultPackage/`, you can compile the production sources from the project root. For example, on macOS/Linux:

```sh
mkdir -p out
find src/defaultPackage -maxdepth 1 -name '*.java' ! -name '*Test.java' -print0 | xargs -0 javac -d out
java -cp out defaultPackage.Main
# Or, independently:
java -cp out defaultPackage.SimulationModelDriver
```

This example assumes a `src/defaultPackage/` layout; adjust the source path if Eclipse uses a different folder. Do **not** compile test classes without adding JUnit 4 to the compiler classpath. On Windows, the simplest supported route is the Eclipse steps above. The runtime application itself needs no external Java libraries.

## Configuring a Run

The terminal prompts for **19 fields** in the following order. Percentages are independent probability weights unless a combined cap is listed.

| # | Configuration input | Allowed values | Meaning |
|---:|---|---|---|
| 1–4 | NORTH, SOUTH, EAST, WEST arrival rates | 0–1,800 veh/hour each | Scheduled arrivals for each incoming approach |
| 5 | Simulation duration | 60–7,200 s | Number of simulated one-second steps |
| 6 | Left-turn probability | 0–100% | Fraction of new vehicles expected to turn left |
| 7 | U-turn probability | 0–(100 − left%) | Fraction expected to U-turn; remaining fraction goes straight |
| 8 | Minimum adaptive green | 5–120 s | Phase cannot switch before this many seconds |
| 9 | Maximum adaptive green | minimum–120 s | Phase must switch when it reaches this duration |
| 10 | Queue-switch threshold | 1–50 vehicles | Required opposing minus current serviceable queue difference; idle-current-axis exception applies |
| 11 | Emergency vehicle probability | 0–100% | Chance an arrival is an emergency vehicle |
| 12 | Event-check interval | 10–120 s | How often eligible approaches draw an incident, starting after t=0 |
| 13 | Accident probability | 0–100% | Per-eligible-approach chance of an accident at a check |
| 14 | Slowdown probability | 0–(100 − accident%) | Per-eligible-approach chance of a slowdown at a check |
| 15 | Accident duration | 5–300 s | How long new departures are blocked |
| 16 | Slowdown duration | 5–300 s | How long reduced speed and larger headway apply |
| 17 | Slowdown speed | 1–9 m/s | Affected crossing speed (normal speed is 10 m/s) |
| 18 | Slowdown departure headway | 2–20 s | Minimum seconds between releases (normally 2 s) |
| 19 | Random seed | Any signed 64-bit integer | Reproduces random turns, emergencies, and incidents |

**Recommended live-demo inputs**, in prompt order:

```text
600
900
1200
1800
180
20
10
10
40
3
30
10
25
35
15
20
5
4
20271118
```

With this configuration, the seed gives a reproducible, nontrivial scenario. An incident check occurs at t=10, 20, 30, etc.; at an eligible check, accident/slowdown/no-event probabilities are 25%/35%/40%. An accident's interval is **start-inclusive and end-exclusive**: an event beginning at t=30 for 15 seconds applies during t=30 through t=44, and expires at t=45. Snapshots report incidents active at their displayed completed-step boundary.

The default rules have no yellow or all-red phase: only NS or EW is green. An emergency request does **not** make the vehicle bypass its own FIFO queue, and an accident affects new releases rather than stopping vehicles that were already crossing.

## Architecture and Model–View Separation

```text
Main
  ├── TerminalView (terminal input/output only)
  └── SimulationController (configuration, input parsing, run coordination)
        ├── uses TerminalView to prompt, display snapshots, and show results
        └── SimulationModel (simulated traffic rules/state)
              ├── Lane → LinkedQueue<Vehicle>
              ├── TrafficSignal (adaptive phase policy)
              ├── TrafficEventManager → TrafficEvent (incident lifecycle)
              ├── StatisticsAccumulator (statistics)
              ├── SimulationConfig → SimulationOptions (immutable settings)
              └── SimulationSnapshot / SimulationResults (immutable output)
                  (returned to controller, which asks TerminalView to display)

SimulationModelDriver → SimulationModel → SimulationSnapshot / SimulationResults
(standalone headless demonstration; does not construct TerminalView or SimulationController)
```

The **model** implements arrival scheduling, random outcomes, signal changes, FIFO queue management, movement, events, and statistics. The **controller** turns raw menu entries into validated typed configuration, advances the model, and decides when the view should display progress. The **view** formats data and reads input but does not retain mutable model state or inspect live queues/vehicles. `SimulationSnapshot` and `SimulationResults` are immutable and copy their maps defensively.

The interface is **pull-based**: the controller calls `getSnapshot()` after steps and `getResults()` at completion. The earlier specification proposed an Observer/callback approach, but this simpler implementation does not require model-to-view observers. `SimulationModelDriver` independently demonstrates that the model can run without the terminal MVC application.

### Simulation-step order and invariants

For each simulated second `t`:

1. Expire old incidents and start any newly scheduled incidents for `t`.
2. Generate arrivals and classify randomized turns/emergencies.
3. Compute serviceable queue lengths and eligible emergency requests, excluding accident-blocked approaches.
4. Let the adaptive signal select the phase for interval `t`.
5. Release at most one vehicle per eligible approach, subject to FIFO, green permission, accident blocking, and headway.
6. Advance all crossing vehicles (including new releases) at normal or incident-reduced speed; complete vehicles that reach 20 meters.
7. Sample queue statistics and advance the completed-step counter.

The central conservation check is:

`totalArrived = totalCompleted + totalWaiting + totalCrossing`

Waiting-time averages are calculated **only for completed vehicles**. Emergency vehicles are already included in overall totals, so their counts must never be added again. Incident statistics count **event starts**, not active event-seconds.

## Testing and Bad-Input Recovery

### Running JUnit 4 tests

1. In Eclipse, confirm **JUnit 4**, not JUnit 5, is on the Java Build Path.
2. Right-click the project's test source folder, package `defaultPackage`, or an individual `*Test.java` file → **Run As → JUnit Test**.
3. Run the **whole suite after the latest changes**, not only individual test classes. The JUnit result panel should show zero failures and errors.
4. Separately run `Main` with representative normal and invalid input, and run `SimulationModelDriver` without a view.

`LinkedQueueTest` specifically tests FIFO ordering after **interleaved enqueues and dequeues** (`A`, `B`, dequeue `A`, enqueue `C`, dequeue `B`, etc.), plus draining/reusing the queue and 10,000-operation stress tests. Other tests target core model behavior directly (including normal and invalid cases), rather than relying solely on UI output.

**Verification status for this README draft:** The team has reported passing individual JUnit tests in Eclipse throughout the V2 implementation; an earlier local JUnit 4-compatible harness reported 345 passing tests across the V2 suites, and a separate local run reported 25 passing `LinkedQueueTest` methods. These are **not** a claim that the complete latest source tree has just been re-run as a single genuine JUnit 4 suite. A final clean, integrated JUnit 4 run remains on the final-build checklist.

### Scope-specific bad inputs and expected recovery

| Input or action | Required behavior / current mechanism |
|---|---|
| Letters, decimal text, blank fields, or overly long values | Show an error and re-prompt without committing partial configuration |
| Negative arrival rate or arrival rate of 1,000,000 vehicles/hour | Reject outside 0–1,800 veh/hour; re-prompt |
| Zero or too-short duration | Reject duration below 60 s; re-prompt |
| Zero-second minimum/maximum green | Reject green time below 5 s; re-prompt |
| Inverted min/max green | Enforce `maxGreen >= minGreen` in prompt and options validation |
| Left + U-turn probabilities exceeding 100% | Restrict U-turn to the remainder and re-prompt |
| Accident + slowdown probabilities exceeding 100% | Restrict slowdown to the remainder and re-prompt |
| Nonnumeric or overflowing random seed | Reject and re-prompt for a signed 64-bit integer |
| EOF during configuration | Leave the previously committed configuration intact |
| Run without configuring | Display an error; do not start the model |
| Concurrent/reentrant run request | Running guard prevents a second overlapping simulation |
| Configuration changed programmatically while a run is active | Current model retains its immutable configuration; change applies to the next run |
| Repeated keystrokes during a synchronous terminal run | No new menu command is processed until the run returns; buffered input may be processed afterward |
| Empty `dequeue`/`peek`, null `enqueue` | `LinkedQueue` throws its documented exception without corrupting queue state |

The final regression should explicitly exercise the scope's **exact** 0-second green and 1,000,000-vehicles/hour values, in addition to the equivalent out-of-range unit-test cases. A buffered-input stress test should also be recorded for the “button/key spam” scope case.

## Reflection

### How did the design absorb the change? D3 predictions versus actual edits

Our D3 Next Steps identified the adaptive-signal feature as a primary D4 target. The main predicted change points were `TrafficSignal`, `SimulationModel`, `SimulationConfig`, `SimulationController`, and terminal presentation. Those prediction areas were correct: the signal changed from fixed-cycle calculation to a stateful adaptive policy, the model had to provide queue demand each step, configuration gained adaptive parameters, and the controller/view needed the new settings and status fields.

D3 anticipated that reusable structures such as `LinkedQueue<T>`, `Vehicle`, and `StatisticsAccumulator` could remain unchanged. **Only the `LinkedQueue` production implementation remained untouched.** `Vehicle` needed a type field and crossing behavior; `StatisticsAccumulator` needed emergency and incident metrics. Additional class changes expanded beyond the original adaptive-only plan because D4 also implemented random turning, emergency requests, accidents, and slowdowns. This is a real difference from the prediction, not a retrospective claim that D3 anticipated all those edits.

| Class or group | D3 expectation / original role | What D4 actually required |
|---|---|---|
| `TrafficSignal` | Change for adaptive signals | Replaced fixed-cycle logic with min/max green, serviceable demand, emergency priority, switch tracking |
| `SimulationModel` | Change for adaptive integration | Rewrote step order; integrated RNG, incidents, emergency requests, movement, statistics, and snapshots |
| `SimulationConfig`, `SimulationController` | Change for new configuration | Added immutable `SimulationOptions`, 19-field validation, seed handling, and change-triggered progress |
| `TerminalView` | Likely presentation changes | Added signal timing, emergency/incident tables, richer results and comparisons |
| `LinkedQueue` | Remain unchanged | **Unchanged production class**; added stronger interleaved-order JUnit tests |
| `Vehicle`, `Lane` | Mostly reusable / limited anticipated change | Added emergency type, seeded random turns, queue emergency counts, new release/headway logic |
| `StatisticsAccumulator` | Expected to remain stable in narrow adaptive plan | Added type-specific arrivals/completions/wait times and incident counters |
| `SimulationSnapshot`, `SimulationResults` | Existing reporting DTOs | Expanded immutable payloads and conservation checks for all new V2 fields |
| `SimulationModelDriver` | Existing model-only demonstrator | Updated configuration/output to exercise full V2 independently |
| New V2 classes | Not in original minimal adaptive prediction | Added `VehicleType`, `TrafficEventType`, `SimulationOptions`, `TrafficEvent`, `TrafficEventManager` |
| `Main`, original basic enums | Remain unchanged | Retained as-is |

**What surprised us:** The most significant ripple was through the data contracts, not the signal algorithm itself. To report emergency wait times correctly, the vehicle type had to survive from arrival generation through completion statistics. To show incident countdowns without exposing model internals, both immutable reporting classes needed new fields. These were legitimate consequences of expanded functionality, rather than unnecessary coupling.

### Did the change cross the model/view boundary?

**No traffic rules moved into the view.** `TerminalView` learned about *new immutable output fields* (emergency queue lengths, active event type and remaining time, phase elapsed/switch counts), but never learned how to compute a signal decision, generate an event, access a linked queue, or modify a vehicle. The controller learned how to prompt for configuration and when to show a changed snapshot; it does not evaluate traffic rules.

The change necessarily **rippled across layer interfaces**: `SimulationModel` produces richer snapshots/results; the controller passes these to the view; and the view formats them. This is an intentional data-contract expansion, not a violation of MVC. The separate `SimulationModelDriver` runs the entire model without constructing `TerminalView` or `SimulationController`, demonstrating the separation. The view also keeps no copy of model state between display calls.

The earlier D3 design work discussed an Observer arrangement. V2 instead uses synchronous steps and controller-initiated snapshot reads. This avoided extra callback state for a synchronous terminal program while retaining the ability to replace the presentation layer later.

### What changed from the D3 plan, and why?

- **Feature scope:** D3 planned adaptive lights as the immediate follow-on; D4 additionally implemented randomized turning, emergency classification/requests, accidents, and slowdowns. These make adaptive behavior testable under changing demand and disruptions.
- **No fixed-mode compatibility:** The final V2 signal always uses adaptive timing. We removed obsolete fixed-cycle constructors and APIs rather than maintaining two control systems. Consequently, the original scope's fixed-versus-adaptive *side-by-side mode comparison* is not yet available.
- **Signatures/configuration:** Added `SimulationOptions` builder and new `SimulationConfig` constructor; revised `Vehicle`, `Lane`, `TrafficSignal`, `StatisticsAccumulator`, `SimulationSnapshot`, and `SimulationResults` APIs. The controller now accepts 19 inputs rather than the V1 configuration flow.
- **Emergency policy:** Instead of forcing all signals red (one original stretch concept), emergencies request earlier service after minimum green; vehicles remain FIFO, and simultaneous requests defer to maximum-green fairness.
- **Incident policy:** A `TrafficEventManager` owns reproducible one-per-approach incident lifecycles. `SimulationModel` applies effects instead of generating events inside the controller or view.
- **Validation/tests:** Added dependent probability constraints, signed-long seed parsing, sequential simulation updates, overflow/immutability checks, stress tests, and interleaved FIFO verification. Tests moved beyond basic happy-path function checks to integrated seeded runs and every-step vehicle conservation.

### What does not work yet or remains simplified?

The core terminal flow works and the individually run V2 suites have passed, but final all-at-once JUnit 4 regression is still required before submission. The remaining scope gaps and explicit simplifications are:

- Only **one incoming FIFO queue per approach** is modeled; physical multi-lane geometry and dedicated turn lanes are not implemented.
- RIGHT-turn selection and route-specific crossing paths are not implemented. LEFT and U-turn intent is randomized, but all vehicles use the same simplified 20-meter crossing distance.
- No collision/conflict zones, pedestrians/crosswalks, yellow/all-red clearance state, red-light violations, or GUI animation.
- Emergency vehicles cannot pass blocked cars or preempt the minimum green, and emergencies do not trigger a real-world all-red clearance interval.
- Accident blocking applies to *new departures*; it does not model crashes occurring inside the intersection.
- The terminal runs synchronously. There is no live pause/resume or editable prompt during an active run; programmatic changes to the selected next configuration do not alter the running model.
- Although V2 uses adaptive signals exclusively, a switchable fixed-timing baseline for controlled fixed-versus-adaptive experimentation remains deferred.
- Exact scope examples (zero-second green, 1,000,000 vehicles/hour, rapid buffered keystrokes) should be explicitly recorded in the final integrated regression, even though corresponding bounds and guards exist.

### Design change that would make the next feature cheaper (Exceeds)

Before adding pedestrians or dedicated turning lanes, **extract movement permission and crossing conflict rules from `SimulationModel` into a `MovementPolicy` / intersection-conflict component**. Right now, `SimulationModel` decides which queued vehicles may depart and advances their crossing progress; it assumes all allowed vehicles can traverse the same 20-meter segment without collision conflicts. Pedestrian crossings and realistic turns would otherwise require more special cases inside the main step loop. A dedicated policy could answer whether a vehicle/pedestrian movement is permitted, its path/distance, and whether its conflict zone is occupied, while the model continues orchestrating the step. This would centralize safety constraints, keep tests smaller, and allow later GUI work without changing core movement rules.

## Next Steps

### Remaining final-build work, in order

The feature implementation is substantially complete. Remaining work is **verification, packaging, and a rehearsed demonstration**, not an untested last-minute redesign. The assignments below reflect the team's D3 planning roles and are **proposed for confirmation**, not claims that a member has already performed an unverified task.

| Order | Remaining task and pass condition | Proposed owner |
|---:|---|---|
| 1 | Clean-build all production and test sources together in Eclipse; run **all JUnit 4 tests** with zero failures; verify `Main` and standalone `SimulationModelDriver` launch | **Timothy** (integration) + **Xinchen** (test review); **Ethan** (model verification) |
| 2 | Add/record exact scope bad-input tests: 0-second green/duration, 1,000,000 veh/hour, malformed/blank/overlong entries, buffered rapid commands, mid-run selected-configuration changes | **Makayla** (terminal cases) + **Xinchen** (JUnit cases) |
| 3 | Check event start/expiry boundary reporting, emergency-priority fairness, deterministic seed replay, and accounting under congestion; fix only confirmed regressions | **Ethan** (model) + **Xinchen** (tests) |
| 4 | Verify README commands, D3-vs-D4 reflection, source/test attribution with each contributor, and accurate AI disclosure; remove any unverified authorship claims | **Ethan** (README) + **all four members** (attribution approval) |
| 5 | Rehearse the three-minute live demo, confirm the terminal scroll output is readable, and freeze the version used for submission | **Timothy** (demo coordination) + **Makayla** (presentation) + **Ethan** (driver/model) |

### Stretch goals: committed versus dropped/deferred

| Goal | Decision | Rationale |
|---|---|---|
| Adaptive traffic signals | **Implemented; committed to keep** | Central D4 change; validated min/max green and demand selection |
| Emergency requests | **Implemented; committed to keep** | Demonstrates priority control while retaining FIFO fairness |
| Random accidents and slowdowns | **Implemented; committed to keep** | Seeded checks, realistic disruptions at simplified lane level |
| Randomized turning | **Implemented; committed to keep** | Seeded LEFT/U-turn/STRAIGHT choices; route geometry remains simplified |
| More edge-case tests, input recovery, deterministic replay | **Committed for final build** | Increases reliability without architectural disruption |
| Fixed versus adaptive mode toggle | **Deferred, not committed** | Would require maintaining a second signal policy; adaptive replaced fixed mode in V2 |
| Pedestrians/crosswalk signal | **Deferred, not committed** | Needs conflict rules and pedestrian states not present in this model |
| Extra physical lanes and dedicated turning lanes | **Dropped from current final build** | One FIFO queue per incoming approach is the stable model |
| RIGHT turns, turn-specific paths, collision zones | **Deferred, not committed** | Requires movement/conflict abstraction described in the Reflection |
| GUI animation | **Dropped from current final build** | Deliverable uses an offline terminal view; MVC separation permits future view replacement |
| Red-light running / all-red emergency preemption | **Dropped from current final build** | Would require a safety/clearance model beyond current two-phase signal logic |

### Timed three-minute demonstration path (Exceeds)

The live demo uses the recommended 19-field configuration above and follows this **180-second presentation budget**. Practice once with the actual terminal output; cut commentary, not validation, if the clock runs long.

| Presentation time | Action | What the audience can verify |
|---|---|---|
| **0:00–0:20** (20 s) | Introduce four directional FIFO queues, V2 features, and MVC boundary | Purpose and architecture |
| **0:20–1:20** (60 s) | Launch `Main`, choose **1 Configure**, enter the prepared 19 values, choose **2 Run** | User input flows through controller into model; adaptive/emergency/incident settings |
| **1:20–2:05** (45 s) | Highlight signal changes and incident countdowns in progress; scroll to emergency, incident, mean-wait, and queue statistics | New features affect real model state and appear in the view |
| **2:05–2:25** (20 s) | Choose **2 Run** again with the same seed, then **4 Compare** | Repeatability and comparison of completed reports |
| **2:25–2:55** (30 s) | Run `SimulationModelDriver` separately and point out model-only conservation verification | Model runs without `TerminalView` or `SimulationController` |
| **2:55–3:00** (5 s) | Summarize known simplifications and point to JUnit 4 tests/README | Limitations and verification |

**If the demo requires visibly different results rather than reproducibility:** prepare a second configuration with a changed seed or incident probability before the demo, but do not attempt to type another 19-field configuration within the timed path above. The deterministic-repeat comparison is the reliable three-minute default.

## Attribution

This table covers **each production class and test file in the D4 V2 class inventory**. The implementation record for this version shows **Ethan directing changes, reviewing behavior, and reporting Eclipse test results**, while **ChatGPT supplied many of the D4 code and test drafts**. Preexisting D3 files were retained where indicated; the original author's individual identity is not established by the available implementation record. Therefore, the table does **not** invent independent code contributions for other team members. The team must confirm original-file authorship and any subsequent edits before submission.

**Key:** `Ethan + ChatGPT` means Ethan specified/reviewed the change and ran or arranged tests; ChatGPT generated or revised the source/test draft, which required human inspection. `D3 retained` means there was no D4 production rewrite (not a claim about D3 authorship).

| Class or test file | Person(s) / assistance | Work performed for Deliverable 4 |
|---|---|---|
| `Direction.java` | D3 retained; Ethan checked compatibility | Kept four-direction enum used throughout the V2 model |
| `Turn.java` | D3 retained; Ethan checked compatibility | Retained existing turn enum for randomized turn choices |
| `SignalPhase.java` | D3 retained; Ethan checked compatibility | Retained NS/EW phase labels for adaptive controller |
| `VehicleState.java` | D3 retained; Ethan checked compatibility | Retained WAITING/CROSSING/DONE lifecycle labels |
| `VehicleType.java` | Ethan + ChatGPT | Introduced NORMAL/EMERGENCY classification |
| `TrafficEventType.java` | Ethan + ChatGPT | Introduced NONE/ACCIDENT/SLOWDOWN classifications |
| `LinkedQueue.java` | D3 retained; Ethan checked compatibility | Original singly linked FIFO queue unchanged |
| `LinkedQueueTest.java` | Ethan + ChatGPT | Reworked JUnit 4 suite with invalid operations, interleaved FIFO order, reuse, and stress cases |
| `Vehicle.java` | Ethan + ChatGPT | Extended type, crossing progress, movement, and state transitions |
| `VehicleTest.java` | Ethan + ChatGPT | Added vehicle-type, lifecycle, boundary, and invalid-input tests |
| `SimulationOptions.java` | Ethan + ChatGPT | Added immutable V2 options/builder, bounds, probabilities, durations, and seed |
| `SimulationOptionsTest.java` | Ethan + ChatGPT | Verified options defaults, builder validation, and dependent bounds |
| `SimulationConfig.java` | Ethan + ChatGPT | Replaced fixed-signal config with validated rates, turns, duration, and V2 options |
| `SimulationConfigTest.java` | Ethan + ChatGPT | Verified complete configurations, null/invalid values, and defensive copies |
| `Lane.java` | Ethan + ChatGPT | Integrated FIFO arrivals, random turns/emergency classification, headways, and release tracking |
| `LaneTest.java` | Ethan + ChatGPT | Verified arrivals, FIFO releases, headway, random choices, and invalid cases |
| `TrafficEvent.java` | Ethan + ChatGPT | Added immutable incident intervals and overflow-safe timing |
| `TrafficEventTest.java` | Ethan + ChatGPT | Verified event classification, time boundaries, invalid input, and immutability |
| `TrafficEventManager.java` | Ethan + ChatGPT | Added seeded, per-approach event scheduling, active-state and expiration tracking |
| `TrafficEventManagerTest.java` | Ethan + ChatGPT | Tested event odds, scheduled starts, exact expiry, overlap prevention, and reproducibility |
| `TrafficSignal.java` | Ethan + ChatGPT | Replaced fixed-time cycle with adaptive queues, min/max green, emergency requests, and counters |
| `TrafficSignalTest.java` | Ethan + ChatGPT | Tested queue thresholds, minimum/maximum, emergencies, bad input, and long-run timing |
| `StatisticsAccumulator.java` | Ethan + ChatGPT | Added emergency arrivals/completions/wait times and incident-start counts |
| `StatisticsAccumulatorTest.java` | Ethan + ChatGPT | Tested total/emergency accounting, averages, incidents, queues, and invalid input |
| `SimulationSnapshot.java` | Ethan + ChatGPT | Expanded immutable step-boundary status with emergencies, incidents, signal timing |
| `SimulationSnapshotTest.java` | Ethan + ChatGPT | Tested conservation, event countdowns, queue subsets, validation, and immutability |
| `SimulationResults.java` | Ethan + ChatGPT | Expanded final immutable report with emergency/event/phase metrics |
| `SimulationResultsTest.java` | Ethan + ChatGPT | Tested report consistency, queue limits, emergency subsets, and invalid values |
| `SimulationModel.java` | Ethan + ChatGPT | Integrated all V2 subsystems in one deterministic second-by-second engine |
| `SimulationModelTest.java` | Ethan + ChatGPT | Tested end-to-end traffic lifecycle, incident effects, seed replay, and max-load conservation |
| `SimulationController.java` | Ethan + ChatGPT | Added V2 prompts, safe parsing/commit, rerun comparison, progress triggers, and running guard |
| `SimulationControllerTest.java` | Ethan + ChatGPT | Tested all inputs, menu paths, error recovery, reruns, callbacks, and concurrency guard |
| `TerminalView.java` | Ethan + ChatGPT | Added formatted V2 progress, emergency/incident tables, results, and comparisons |
| `TerminalViewTest.java` | Ethan + ChatGPT | Verified output, null cases, incident/queue tables, comparisons, and locale formatting |
| `SimulationModelDriver.java` | Ethan + ChatGPT | Updated standalone model demonstration, seeded scenario, and conservation checks |
| `SimulationModelDriverTest.java` | Ethan + ChatGPT | Tested standalone execution, exact output, seeded reproducibility, and conservation |
| `Main.java` | D3 retained; Ethan checked compatibility | Existing interactive application entry point unchanged |

**Earlier team responsibility planning (not independent proof of D4 file authorship):** Ethan — simulation engine/signals and integration; Xinchen — queue structures and model tests; Makayla — terminal presentation and bad-input cases; Timothy — controller and final integration. These proposed responsibilities inform the Next Steps owners above; each teammate should review the final attribution table and credit any code/review work actually performed.

### GenAI Attribution

GenAI was used as an **engineering assistant** during the specification, implementation, testing, and documentation of 4WayChaos. Earlier project work used Gemini to probe model/view boundaries and adversarial input cases. During D4, ChatGPT was used to propose the V2 architecture, draft/revise Java classes and JUnit 4 test cases, investigate edge conditions and validation strategies, explain configuration behavior, and draft this README. Ethan directed the requested behavior, reviewed the generated changes, compiled and ran the supplied tests in Eclipse, and reported results that informed subsequent revisions. Generated code and documentation were not treated as automatically correct; behavior was checked through unit tests, seeded integration scenarios, and the standalone model driver. This disclosure does not assign unverified contributions to teammates or imply that AI independently performed the user's Eclipse verification.
