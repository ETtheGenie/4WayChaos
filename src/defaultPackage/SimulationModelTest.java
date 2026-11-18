
package defaultPackage;

import static org.junit.Assert.*;

import java.util.EnumMap;
import java.util.Map;
import org.junit.Test;

/**
 * JUnit 4 integration tests for the V2 model: arrivals, adaptive switching,
 * emergency requests, accidents, slowdowns, seeded behavior, immutability,
 * completion metrics, and long-run vehicle conservation.
 */
public class SimulationModelTest
{

    /**
     * Creates a validated scenario with explicit rates and behavior options.
     *
     * @param duration
     *            simulation duration in seconds
     * @param north
     *            north arrival rate per hour
     * @param south
     *            south arrival rate per hour
     * @param east
     *            east arrival rate per hour
     * @param west
     *            west arrival rate per hour
     * @param options
     *            V2 behavior options
     * @return valid immutable simulation configuration
     */
    private SimulationConfig config(
        int duration,
        int north,
        int south,
        int east,
        int west,
        SimulationOptions options)
    {
        Map<Direction, Integer> rates = new EnumMap<>(Direction.class);
        rates.put(Direction.NORTH, north);
        rates.put(Direction.SOUTH, south);
        rates.put(Direction.EAST, east);
        rates.put(Direction.WEST, west);
        return new SimulationConfig(rates, duration, 20, 10, options);
    }


    /**
     * Returns settings with incidents and emergency arrivals disabled.
     *
     * @return valid deterministic-throughput behavior options
     */
    private SimulationOptions quietOptions()
    {
        return SimulationOptions.builder().emergencyVehiclePercent(0)
            .accidentChancePercent(0).slowdownChancePercent(0)
            .randomSeed(12345L).build();
    }


    /**
     * Processes simulation steps until the requested boundary is reached.
     *
     * @param model
     *            model to advance
     * @param elapsed
     *            target completed-step count
     */
    private void advanceTo(SimulationModel model, int elapsed)
    {
        while (model.getSnapshot().getElapsedSeconds() < elapsed)
        {
            model.step();
        }
    }


    /**
     * Completes a simulation and returns its final immutable results.
     *
     * @param model
     *            model to finish
     * @return final report
     */
    private SimulationResults finish(SimulationModel model)
    {
        while (!model.isComplete())
        {
            model.step();
        }
        return model.getResults();
    }


    /**
     * Verifies overall and emergency waiting-queue bounds and conservation.
     *
     * @param snapshot
     *            snapshot to validate
     */
    private void assertConserved(SimulationSnapshot snapshot)
    {
        assertEquals(
            snapshot.getTotalArrived(),
            snapshot.getWaitingCount() + snapshot.getCrossingCount()
                + snapshot.getTotalCompleted());
        int summedWaiting = 0;
        int summedEmergency = 0;
        for (Direction direction : Direction.values())
        {
            int queued = snapshot.getQueueLength(direction);
            int emergency = snapshot.getEmergencyQueueLength(direction);
            assertTrue(queued >= 0);
            assertTrue(emergency >= 0 && emergency <= queued);
            summedWaiting += queued;
            summedEmergency += emergency;
            TrafficEvent event = snapshot.getActiveEvent(direction);
            if (event == null)
            {
                assertEquals(
                    TrafficEventType.NONE,
                    snapshot.getActiveIncidentType(direction));
                assertEquals(
                    0,
                    snapshot.getRemainingIncidentSeconds(direction));
            }
            else
            {
                assertEquals(direction, event.getApproach());
                assertTrue(event.isActiveAt(snapshot.getElapsedSeconds()));
                assertTrue(snapshot.getRemainingIncidentSeconds(direction) > 0);
            }
        }
        assertEquals(summedWaiting, snapshot.getWaitingCount());
        assertEquals(summedEmergency, snapshot.getEmergencyWaitingCount());
    }


    /**
     * Compares all state exposed by two immutable model snapshots.
     *
     * @param expected
     *            first snapshot
     * @param actual
     *            second snapshot
     */
    private void assertSameSnapshot(
        SimulationSnapshot expected,
        SimulationSnapshot actual)
    {
        assertEquals(expected.getElapsedSeconds(), actual.getElapsedSeconds());
        assertEquals(expected.getSignalPhase(), actual.getSignalPhase());
        assertEquals(
            expected.getPhaseStartSecond(),
            actual.getPhaseStartSecond());
        assertEquals(
            expected.getPhaseSwitchCount(),
            actual.getPhaseSwitchCount());
        assertEquals(expected.getQueueLengths(), actual.getQueueLengths());
        assertEquals(
            expected.getEmergencyQueueLengths(),
            actual.getEmergencyQueueLengths());
        assertEquals(expected.getTotalArrived(), actual.getTotalArrived());
        assertEquals(expected.getTotalCompleted(), actual.getTotalCompleted());
        assertEquals(expected.getCrossingCount(), actual.getCrossingCount());
        assertEquals(
            expected.getActiveEvents().keySet(),
            actual.getActiveEvents().keySet());
        for (Direction direction : Direction.values())
        {
            assertEquals(
                expected.getActiveIncidentType(direction),
                actual.getActiveIncidentType(direction));
            assertEquals(
                expected.getRemainingIncidentSeconds(direction),
                actual.getRemainingIncidentSeconds(direction));
            TrafficEvent left = expected.getActiveEvent(direction);
            TrafficEvent right = actual.getActiveEvent(direction);
            if (left != null)
            {
                assertNotNull(right);
                assertEquals(left.getStartSecond(), right.getStartSecond());
                assertEquals(left.getEndSecond(), right.getEndSecond());
            }
        }
    }


    /**
     * Confirms that the supplied operation rejects an invalid model state.
     *
     * @param action
     *            operation expected to throw IllegalStateException
     */
    private void assertIllegalState(Runnable action)
    {
        try
        {
            action.run();
            fail("Expected IllegalStateException.");
        }
        catch (IllegalStateException expected)
        {
            // Expected invalid model state.
        }
    }


    /**
     * Rejects null configuration rather than constructing a partial model.
     */
    @Test
    public void testNullConfigurationRejected()
    {
        try
        {
            new SimulationModel(null);
            fail("Expected IllegalArgumentException.");
        }
        catch (IllegalArgumentException expected)
        {
            // Expected null-configuration rejection.
        }
    }


    /**
     * Confirms modeled crossing length and normal speed are preserved.
     */
    @Test
    public void testMovementConstants()
    {
        assertEquals(20, SimulationModel.CROSSING_DISTANCE_METERS);
        assertEquals(10, SimulationModel.VEHICLE_SPEED_METERS_PER_SECOND);
    }


    /**
     * Validates the pristine state before any simulation steps run.
     */
    @Test
    public void testInitialSnapshotAndCompletionState()
    {
        SimulationModel model =
            new SimulationModel(config(60, 600, 600, 600, 600, quietOptions()));
        SimulationSnapshot snapshot = model.getSnapshot();
        assertEquals(0, snapshot.getElapsedSeconds());
        assertEquals(SignalPhase.NS_GREEN, snapshot.getSignalPhase());
        assertEquals(0, snapshot.getPhaseStartSecond());
        assertEquals(0, snapshot.getPhaseSwitchCount());
        assertEquals(0, snapshot.getTotalArrived());
        assertEquals(0, snapshot.getWaitingCount());
        assertEquals(0, snapshot.getCrossingCount());
        assertTrue(snapshot.getActiveEvents().isEmpty());
        assertFalse(model.isComplete());
        assertConserved(snapshot);
    }


    /**
     * Refuses to return final statistics before the configured duration ends.
     */
    @Test
    public void testResultsUnavailableBeforeCompletion()
    {
        SimulationModel model =
            new SimulationModel(config(60, 0, 0, 0, 0, quietOptions()));
        assertIllegalState(() -> model.getResults());
        advanceTo(model, 59);
        assertIllegalState(() -> model.getResults());
        assertFalse(model.isComplete());
    }


    /**
     * Verifies each step advances the clock by exactly one simulated second.
     */
    @Test
    public void testEachStepAdvancesExactlyOneSecond()
    {
        SimulationModel model =
            new SimulationModel(config(60, 0, 0, 0, 0, quietOptions()));
        for (int elapsed = 1; elapsed <= 60; elapsed++)
        {
            model.step();
            assertEquals(elapsed, model.getSnapshot().getElapsedSeconds());
            assertEquals(elapsed == 60, model.isComplete());
        }
    }


    /**
     * Checks that completed models cannot step again and retain final state.
     */
    @Test
    public void testStepAfterCompletionRejectedWithoutMutation()
    {
        SimulationModel model =
            new SimulationModel(config(60, 600, 0, 0, 0, quietOptions()));
        SimulationResults before = finish(model);
        SimulationSnapshot snapshot = model.getSnapshot();
        assertIllegalState(() -> model.step());
        assertSameSnapshot(snapshot, model.getSnapshot());
        assertEquals(
            before.getTotalArrived(),
            model.getResults().getTotalArrived());
    }


    /**
     * Verifies a zero-vehicle run still advances its adaptive signal.
     */
    @Test
    public void testZeroTrafficCompletesWithZeroVehicleStatistics()
    {
        SimulationModel model =
            new SimulationModel(config(60, 0, 0, 0, 0, quietOptions()));
        SimulationResults result = finish(model);
        assertEquals(0, result.getTotalArrived());
        assertEquals(0, result.getTotalCompleted());
        assertEquals(0, result.getWaitingCount());
        assertEquals(0, result.getCrossingCount());
        assertNull(result.getAverageWaitingTimeSeconds());
        assertNull(result.getAverageEmergencyWaitingTimeSeconds());
        assertEquals(0, result.getTotalAccidents());
        assertEquals(0, result.getTotalSlowdowns());
        assertConserved(model.getSnapshot());
    }


    /**
     * Verifies exactly ten arrivals per approach at 600 vehicles/hour in 60 s.
     */
    @Test
    public void testExactArrivalTotalsAtSixHundredEach()
    {
        SimulationModel model =
            new SimulationModel(config(60, 600, 600, 600, 600, quietOptions()));
        SimulationResults result = finish(model);
        assertEquals(40, result.getTotalArrived());
        assertEquals(0, result.getTotalEmergencyArrived());
        assertEquals(
            40,
            result.getTotalCompleted() + result.getCrossingCount()
                + result.getWaitingCount());
    }


    /**
     * Verifies the maximum permitted arrival rate produces 30 per direction.
     */
    @Test
    public void testExactMaximumArrivalTotals()
    {
        SimulationModel model = new SimulationModel(
            config(60, 1800, 1800, 1800, 1800, quietOptions()));
        SimulationResults result = finish(model);
        assertEquals(120, result.getTotalArrived());
        assertEquals(
            120,
            result.getTotalCompleted() + result.getCrossingCount()
                + result.getWaitingCount());
    }


    /**
     * Checks independent deterministic arrival rates in one mixed scenario.
     */
    @Test
    public void testMixedArrivalRatesAreExact()
    {
        SimulationModel model =
            new SimulationModel(config(120, 1800, 600, 900, 0, quietOptions()));
        SimulationResults result = finish(model);
        assertEquals(60 + 20 + 30, result.getTotalArrived());
    }


    /**
     * Verifies north-south remains green until a maximum-green boundary in an
     * empty intersection, then the opposite phase receives green.
     */
    @Test
    public void testMaximumGreenSwitchesWithNoTraffic()
    {
        SimulationOptions options = SimulationOptions.builder()
            .minGreenSeconds(10).maxGreenSeconds(20).emergencyVehiclePercent(0)
            .accidentChancePercent(0).slowdownChancePercent(0).build();
        SimulationModel model =
            new SimulationModel(config(60, 0, 0, 0, 0, options));
        advanceTo(model, 20);
        assertEquals(
            SignalPhase.NS_GREEN,
            model.getSnapshot().getSignalPhase());
        assertEquals(0, model.getSnapshot().getPhaseSwitchCount());
        model.step();
        assertEquals(
            SignalPhase.EW_GREEN,
            model.getSnapshot().getSignalPhase());
        assertEquals(20, model.getSnapshot().getPhaseStartSecond());
        assertEquals(1, model.getSnapshot().getPhaseSwitchCount());
        SimulationResults result = finish(model);
        assertEquals(2, result.getPhaseSwitchCount());
    }


    /**
     * Verifies adaptive switching based on actual queued east-west demand.
     */
    @Test
    public void testAdaptiveQueueDemandSwitchesAtMinimumGreen()
    {
        SimulationOptions options =
            SimulationOptions.builder().minGreenSeconds(5).maxGreenSeconds(60)
                .queueSwitchThreshold(50).emergencyVehiclePercent(0)
                .accidentChancePercent(0).slowdownChancePercent(0).build();
        SimulationModel model =
            new SimulationModel(config(60, 0, 0, 1800, 0, options));
        advanceTo(model, 5);
        assertEquals(
            SignalPhase.NS_GREEN,
            model.getSnapshot().getSignalPhase());
        assertEquals(2, model.getSnapshot().getQueueLength(Direction.EAST));
        model.step();
        assertEquals(
            SignalPhase.EW_GREEN,
            model.getSnapshot().getSignalPhase());
        assertEquals(5, model.getSnapshot().getPhaseStartSecond());
        assertEquals(1, model.getSnapshot().getPhaseSwitchCount());
        assertConserved(model.getSnapshot());
    }


    /**
     * Confirms the snapshot displays the phase just used, not the upcoming
     * phase that will be decided when the next interval begins.
     */
    @Test
    public void testSnapshotNeverPredictsFutureSignalSwitches()
    {
        SimulationOptions options = SimulationOptions.builder()
            .minGreenSeconds(10).maxGreenSeconds(60).emergencyVehiclePercent(0)
            .accidentChancePercent(0).slowdownChancePercent(0).build();
        SimulationModel model =
            new SimulationModel(config(60, 0, 0, 1800, 0, options));
        advanceTo(model, 10);
        assertEquals(
            SignalPhase.NS_GREEN,
            model.getSnapshot().getSignalPhase());
        model.step();
        assertEquals(
            SignalPhase.EW_GREEN,
            model.getSnapshot().getSignalPhase());
        assertEquals(10, model.getSnapshot().getPhaseStartSecond());
        assertEquals(1, model.getSnapshot().getPhaseElapsedSeconds());
    }


    /**
     * Confirms a red-light emergency remains in the FIFO queue until its
     * direction receives green and never bypasses the queue.
     */
    @Test
    public void testEmergencyQueuesWhileRedAndThenDeparts()
    {
        SimulationOptions options =
            SimulationOptions.builder().emergencyVehiclePercent(100)
                .accidentChancePercent(0).slowdownChancePercent(0)
                .minGreenSeconds(10).maxGreenSeconds(60).build();
        SimulationModel model =
            new SimulationModel(config(60, 0, 0, 1800, 0, options));
        advanceTo(model, 10);
        assertEquals(5, model.getSnapshot().getQueueLength(Direction.EAST));
        assertEquals(
            5,
            model.getSnapshot().getEmergencyQueueLength(Direction.EAST));
        assertEquals(0, model.getSnapshot().getTotalCompleted());
        model.step();
        assertEquals(
            SignalPhase.EW_GREEN,
            model.getSnapshot().getSignalPhase());
        assertEquals(4, model.getSnapshot().getQueueLength(Direction.EAST));
        assertEquals(
            4,
            model.getSnapshot().getEmergencyQueueLength(Direction.EAST));
        assertEquals(1, model.getSnapshot().getCrossingCount());
    }


    /**
     * Confirms 100% emergency classification includes every generated vehicle
     * in both the overall and emergency totals without double-counting.
     */
    @Test
    public void testAllEmergencyTrafficStats()
    {
        SimulationOptions options =
            SimulationOptions.builder().emergencyVehiclePercent(100)
                .accidentChancePercent(0).slowdownChancePercent(0).build();
        SimulationModel model =
            new SimulationModel(config(120, 600, 900, 1200, 1800, options));
        SimulationResults result = finish(model);
        assertEquals(
            result.getTotalArrived(),
            result.getTotalEmergencyArrived());
        assertEquals(
            result.getTotalCompleted(),
            result.getTotalEmergencyCompleted());
        assertEquals(
            result.getTotalEmergencyArrived()
                - result.getTotalEmergencyCompleted(),
            result.getEmergencyUnfinishedCount());
        for (Direction direction : Direction.values())
        {
            assertEquals(
                model.getSnapshot().getQueueLength(direction),
                model.getSnapshot().getEmergencyQueueLength(direction));
        }
    }


    /**
     * Verifies a zero emergency probability never generates emergency cars.
     */
    @Test
    public void testZeroEmergencyProbabilityProducesNoEmergencyStats()
    {
        SimulationModel model =
            new SimulationModel(config(60, 1800, 0, 1800, 0, quietOptions()));
        SimulationResults result = finish(model);
        assertEquals(0, result.getTotalEmergencyArrived());
        assertEquals(0, result.getTotalEmergencyCompleted());
        assertEquals(0, result.getEmergencyUnfinishedCount());
        assertNull(result.getAverageEmergencyWaitingTimeSeconds());
        assertEquals(0, model.getSnapshot().getEmergencyWaitingCount());
    }


    /**
     * Tests incidents are absent at elapsed boundary 10 before interval t=10
     * begins, then accidents appear after step t=10 is processed.
     */
    @Test
    public void testAccidentsBeginAtScheduledInterval()
    {
        SimulationOptions options = SimulationOptions.builder()
            .eventCheckIntervalSeconds(10).accidentChancePercent(100)
            .slowdownChancePercent(0).accidentDurationSeconds(5).build();
        SimulationModel model =
            new SimulationModel(config(60, 0, 0, 0, 0, options));
        advanceTo(model, 10);
        assertTrue(model.getSnapshot().getActiveEvents().isEmpty());
        model.step();
        SimulationSnapshot snapshot = model.getSnapshot();
        assertEquals(4, snapshot.getActiveEvents().size());
        for (Direction direction : Direction.values())
        {
            assertEquals(
                TrafficEventType.ACCIDENT,
                snapshot.getActiveIncidentType(direction));
            assertEquals(4, snapshot.getRemainingIncidentSeconds(direction));
            assertEquals(
                10,
                snapshot.getActiveEvent(direction).getStartSecond());
        }
    }


    /**
     * Verifies expiration is evaluated at the exact event end, not one step
     * later, and counts incidents once each time they start.
     */
    @Test
    public void testAccidentExpirationAndNewCheckAtTwenty()
    {
        SimulationOptions options = SimulationOptions.builder()
            .eventCheckIntervalSeconds(10).accidentChancePercent(100)
            .slowdownChancePercent(0).accidentDurationSeconds(5).build();
        SimulationModel model =
            new SimulationModel(config(60, 0, 0, 0, 0, options));
        advanceTo(model, 14);
        assertEquals(
            1,
            model.getSnapshot().getRemainingIncidentSeconds(Direction.NORTH));
        advanceTo(model, 15);
        assertTrue(model.getSnapshot().getActiveEvents().isEmpty());
        advanceTo(model, 20);
        assertTrue(model.getSnapshot().getActiveEvents().isEmpty());
        model.step();
        assertEquals(
            20,
            model.getSnapshot().getActiveEvent(Direction.NORTH)
                .getStartSecond());
        SimulationResults result = finish(model);
        assertEquals(20, result.getTotalAccidents());
        assertEquals(0, result.getTotalSlowdowns());
    }


    /**
     * Checks a live accident prevents releases from a green approach and
     * increases its queue relative to an incident-free scenario.
     */
    @Test
    public void testAccidentBlocksGreenDepartures()
    {
        SimulationOptions accidents =
            SimulationOptions.builder().eventCheckIntervalSeconds(10)
                .accidentChancePercent(100).slowdownChancePercent(0)
                .accidentDurationSeconds(5).emergencyVehiclePercent(0).build();
        SimulationModel blocked =
            new SimulationModel(config(60, 1800, 0, 0, 0, accidents));
        SimulationModel normal =
            new SimulationModel(config(60, 1800, 0, 0, 0, quietOptions()));
        advanceTo(blocked, 14);
        advanceTo(normal, 14);
        assertEquals(
            TrafficEventType.ACCIDENT,
            blocked.getSnapshot().getActiveIncidentType(Direction.NORTH));
        assertTrue(
            blocked.getSnapshot().getQueueLength(Direction.NORTH) > normal
                .getSnapshot().getQueueLength(Direction.NORTH));
        assertEquals(
            blocked.getSnapshot().getTotalArrived(),
            normal.getSnapshot().getTotalArrived());
        assertConserved(blocked.getSnapshot());
    }


    /**
     * Verifies an accident does not stop a vehicle that was already inside the
     * intersection before the incident began.
     */
    @Test
    public void testAccidentDoesNotStopAlreadyCrossingVehicles()
    {
        SimulationOptions accidents =
            SimulationOptions.builder().eventCheckIntervalSeconds(10)
                .accidentChancePercent(100).slowdownChancePercent(0)
                .accidentDurationSeconds(5).emergencyVehiclePercent(0).build();
        SimulationModel model =
            new SimulationModel(config(60, 1800, 0, 0, 0, accidents));
        advanceTo(model, 10);
        assertEquals(4, model.getSnapshot().getTotalCompleted());
        assertEquals(1, model.getSnapshot().getCrossingCount());
        model.step();
        assertEquals(
            TrafficEventType.ACCIDENT,
            model.getSnapshot().getActiveIncidentType(Direction.NORTH));
        assertEquals(5, model.getSnapshot().getTotalCompleted());
        assertEquals(0, model.getSnapshot().getCrossingCount());
    }


    /**
     * Checks accident-blocked emergencies cannot request service, so a red
     * phase does not switch solely for those trapped vehicles.
     */
    @Test
    public void testAccidentBlocksEmergencyPriorityRequests()
    {
        SimulationOptions accidents =
            SimulationOptions.builder().eventCheckIntervalSeconds(10)
                .accidentChancePercent(100).slowdownChancePercent(0)
                .accidentDurationSeconds(5).emergencyVehiclePercent(100)
                .minGreenSeconds(10).maxGreenSeconds(60).build();
        SimulationModel blocked =
            new SimulationModel(config(60, 0, 0, 1800, 0, accidents));
        advanceTo(blocked, 11);
        assertEquals(
            SignalPhase.NS_GREEN,
            blocked.getSnapshot().getSignalPhase());
        assertEquals(0, blocked.getSnapshot().getPhaseSwitchCount());
        assertTrue(blocked.getSnapshot().getEmergencyWaitingCount() > 0);
    }


    /**
     * Verifies slowdown changes both crossing speed and the minimum headway,
     * rather than merely changing an incident label in the snapshot.
     */
    @Test
    public void testSlowdownChangesMovementAndHeadway()
    {
        SimulationOptions slow =
            SimulationOptions.builder().eventCheckIntervalSeconds(10)
                .accidentChancePercent(0).slowdownChancePercent(100)
                .slowdownDurationSeconds(5).slowdownSpeedMetersPerSecond(5)
                .slowdownHeadwaySeconds(4).emergencyVehiclePercent(0).build();
        SimulationModel slowed =
            new SimulationModel(config(60, 1800, 0, 0, 0, slow));
        SimulationModel normal =
            new SimulationModel(config(60, 1800, 0, 0, 0, quietOptions()));
        advanceTo(slowed, 11);
        advanceTo(normal, 11);
        assertEquals(
            TrafficEventType.SLOWDOWN,
            slowed.getSnapshot().getActiveIncidentType(Direction.NORTH));
        assertEquals(1, slowed.getSnapshot().getCrossingCount());
        assertEquals(0, normal.getSnapshot().getCrossingCount());
        assertEquals(4, slowed.getSnapshot().getTotalCompleted());
        assertEquals(5, normal.getSnapshot().getTotalCompleted());
        advanceTo(slowed, 12);
        advanceTo(normal, 12);
        assertEquals(1, slowed.getSnapshot().getQueueLength(Direction.NORTH));
        assertEquals(0, normal.getSnapshot().getQueueLength(Direction.NORTH));
        assertConserved(slowed.getSnapshot());
    }


    /**
     * Confirms a slowdown's headway restriction disappears after expiration.
     */
    @Test
    public void testNormalThroughputResumesAfterSlowdown()
    {
        SimulationOptions slow =
            SimulationOptions.builder().eventCheckIntervalSeconds(10)
                .accidentChancePercent(0).slowdownChancePercent(100)
                .slowdownDurationSeconds(5).slowdownSpeedMetersPerSecond(5)
                .slowdownHeadwaySeconds(4).emergencyVehiclePercent(0).build();
        SimulationModel model =
            new SimulationModel(config(60, 1800, 0, 0, 0, slow));
        advanceTo(model, 15);
        assertEquals(
            TrafficEventType.NONE,
            model.getSnapshot().getActiveIncidentType(Direction.NORTH));
        int waitingBefore = model.getSnapshot().getQueueLength(Direction.NORTH);
        model.step();
        assertEquals(
            TrafficEventType.NONE,
            model.getSnapshot().getActiveIncidentType(Direction.NORTH));
        assertEquals(
            waitingBefore,
            model.getSnapshot().getQueueLength(Direction.NORTH));
        assertEquals(1, model.getSnapshot().getCrossingCount());
    }


    /**
     * Tests that incidents are disabled by zero probabilities even when the run
     * crosses many configured event-check intervals.
     */
    @Test
    public void testZeroIncidentProbabilitiesNeverGenerateEvents()
    {
        SimulationOptions options =
            SimulationOptions.builder().eventCheckIntervalSeconds(10)
                .accidentChancePercent(0).slowdownChancePercent(0).build();
        SimulationModel model =
            new SimulationModel(config(120, 600, 600, 600, 600, options));
        while (!model.isComplete())
        {
            model.step();
            assertTrue(model.getSnapshot().getActiveEvents().isEmpty());
        }
        assertEquals(0, model.getResults().getTotalAccidents());
        assertEquals(0, model.getResults().getTotalSlowdowns());
    }


    /**
     * Confirms incidents can occur on empty approaches but never create phantom
     * arrivals or violate conservation.
     */
    @Test
    public void testIncidentsWithZeroTrafficStillCount()
    {
        SimulationOptions options = SimulationOptions.builder()
            .eventCheckIntervalSeconds(10).accidentChancePercent(0)
            .slowdownChancePercent(100).slowdownDurationSeconds(5).build();
        SimulationModel model =
            new SimulationModel(config(60, 0, 0, 0, 0, options));
        SimulationResults results = finish(model);
        assertEquals(0, results.getTotalArrived());
        assertEquals(20, results.getTotalSlowdowns());
        assertEquals(0, results.getTotalAccidents());
        assertEquals(0, results.getTotalCompleted());
    }


    /**
     * Confirms exact incident count and expiration when event duration equals
     * the check interval, allowing immediate replacement at the boundary.
     */
    @Test
    public void testIncidentsCanRestartOnExpirationBoundary()
    {
        SimulationOptions options = SimulationOptions.builder()
            .eventCheckIntervalSeconds(10).accidentChancePercent(100)
            .slowdownChancePercent(0).accidentDurationSeconds(10).build();
        SimulationModel model =
            new SimulationModel(config(60, 0, 0, 0, 0, options));
        advanceTo(model, 21);
        assertEquals(
            20,
            model.getSnapshot().getActiveEvent(Direction.WEST)
                .getStartSecond());
        assertEquals(
            9,
            model.getSnapshot().getRemainingIncidentSeconds(Direction.WEST));
        SimulationResults result = finish(model);
        assertEquals(20, result.getTotalAccidents());
    }


    /**
     * Verifies an event whose exclusive end is the final simulation boundary is
     * absent from the final snapshot, without running an extra step.
     */
    @Test
    public void testEventExpiredAtFinalSnapshotBoundary()
    {
        SimulationOptions options = SimulationOptions.builder()
            .eventCheckIntervalSeconds(10).accidentChancePercent(100)
            .slowdownChancePercent(0).accidentDurationSeconds(10).build();
        SimulationModel model =
            new SimulationModel(config(60, 0, 0, 0, 0, options));
        finish(model);
        assertEquals(0, model.getSnapshot().getActiveEvents().size());
    }


    /**
     * Verifies the model preserves the exact final number of crossing cars
     * rather than incorrectly marking all cars complete at the time limit.
     */
    @Test
    public void testCarsCrossingAtTimeLimitRemainUnfinished()
    {
        SimulationOptions options = SimulationOptions.builder()
            .minGreenSeconds(10).maxGreenSeconds(120).emergencyVehiclePercent(0)
            .accidentChancePercent(0).slowdownChancePercent(0).build();
        SimulationModel model =
            new SimulationModel(config(60, 1800, 0, 0, 0, options));
        SimulationResults result = finish(model);
        assertEquals(30, result.getTotalArrived());
        assertEquals(29, result.getTotalCompleted());
        assertEquals(1, result.getCrossingCount());
        assertEquals(0, result.getWaitingCount());
    }


    /**
     * Tests immediate normal departures and correct average queue waiting time
     * under an uncongested northbound approach.
     */
    @Test
    public void testNormalDeparturesYieldZeroAverageWaitingTime()
    {
        SimulationOptions options = SimulationOptions.builder()
            .minGreenSeconds(10).maxGreenSeconds(120).emergencyVehiclePercent(0)
            .accidentChancePercent(0).slowdownChancePercent(0).build();
        SimulationModel model =
            new SimulationModel(config(60, 1800, 0, 0, 0, options));
        SimulationResults result = finish(model);
        assertEquals(0.0, result.getAverageWaitingTimeSeconds(), 0.000001);
        assertEquals(0, result.getFinalQueueLength(Direction.NORTH));
        assertEquals(0, result.getMaximumQueueLength(Direction.NORTH));
    }


    /**
     * Verifies the same seed and settings reproduce every observable step,
     * including incident types, emergency queue counts, and switching times.
     */
    @Test
    public void testSeededSimulationsReproduceAllSnapshots()
    {
        SimulationOptions options = SimulationOptions.builder()
            .minGreenSeconds(7).maxGreenSeconds(45).accidentChancePercent(25)
            .slowdownChancePercent(35).eventCheckIntervalSeconds(10)
            .accidentDurationSeconds(17).slowdownDurationSeconds(19)
            .emergencyVehiclePercent(40).randomSeed(9234567L).build();
        SimulationConfig scenario = config(300, 600, 900, 1200, 1800, options);
        SimulationModel a = new SimulationModel(scenario);
        SimulationModel b = new SimulationModel(scenario);
        assertSameSnapshot(a.getSnapshot(), b.getSnapshot());
        for (int i = 0; i < 300; i++)
        {
            a.step();
            b.step();
            assertSameSnapshot(a.getSnapshot(), b.getSnapshot());
        }
        assertEquals(
            a.getResults().getTotalAccidents(),
            b.getResults().getTotalAccidents());
        assertEquals(
            a.getResults().getTotalSlowdowns(),
            b.getResults().getTotalSlowdowns());
        assertEquals(
            a.getResults().getTotalEmergencyArrived(),
            b.getResults().getTotalEmergencyArrived());
        assertEquals(
            a.getResults().getTotalEmergencyCompleted(),
            b.getResults().getTotalEmergencyCompleted());
        assertEquals(
            a.getResults().getAverageWaitingTimeSeconds(),
            b.getResults().getAverageWaitingTimeSeconds());
        assertEquals(
            a.getResults().getAverageEmergencyWaitingTimeSeconds(),
            b.getResults().getAverageEmergencyWaitingTimeSeconds());
    }


    /**
     * Confirms different random seeds can affect random incident or emergency
     * outcomes, while each remains a valid simulation.
     */
    @Test
    public void testDifferentSeedsCanChangeOutcomes()
    {
        SimulationOptions aOptions =
            SimulationOptions.builder().eventCheckIntervalSeconds(10)
                .accidentChancePercent(25).slowdownChancePercent(35)
                .emergencyVehiclePercent(40).randomSeed(1L).build();
        SimulationOptions bOptions =
            SimulationOptions.builder().eventCheckIntervalSeconds(10)
                .accidentChancePercent(25).slowdownChancePercent(35)
                .emergencyVehiclePercent(40).randomSeed(2L).build();
        SimulationModel a =
            new SimulationModel(config(180, 1800, 1800, 1800, 1800, aOptions));
        SimulationModel b =
            new SimulationModel(config(180, 1800, 1800, 1800, 1800, bOptions));
        boolean different = false;
        for (int i = 0; i < 180; i++)
        {
            a.step();
            b.step();
            SimulationSnapshot x = a.getSnapshot();
            SimulationSnapshot y = b.getSnapshot();
            if (!x.getQueueLengths().equals(y.getQueueLengths())
                || !x.getEmergencyQueueLengths()
                    .equals(y.getEmergencyQueueLengths())
                || !x.getActiveEvents().keySet()
                    .equals(y.getActiveEvents().keySet()))
            {
                different = true;
            }
        }
        assertTrue(
            "Different seeds should produce distinguishable traces",
            different);
        assertConserved(a.getSnapshot());
        assertConserved(b.getSnapshot());
    }


    /**
     * Checks an earlier immutable snapshot remains unchanged when the model
     * advances through future incidents and vehicle arrivals.
     */
    @Test
    public void testSnapshotsRemainIndependentOfFutureSteps()
    {
        SimulationOptions options =
            SimulationOptions.builder().eventCheckIntervalSeconds(10)
                .accidentChancePercent(100).slowdownChancePercent(0).build();
        SimulationModel model =
            new SimulationModel(config(60, 1800, 0, 1800, 0, options));
        SimulationSnapshot original = model.getSnapshot();
        advanceTo(model, 30);
        assertEquals(0, original.getElapsedSeconds());
        assertEquals(0, original.getTotalArrived());
        assertTrue(original.getActiveEvents().isEmpty());
        assertEquals(0, original.getWaitingCount());
        assertTrue(model.getSnapshot().getTotalArrived() > 0);
    }


    /**
     * Checks the snapshot cannot be used to modify the model's live queues.
     */
    @Test
    public void testSnapshotQueuesAreReadOnly()
    {
        SimulationModel model =
            new SimulationModel(config(60, 1800, 0, 1800, 0, quietOptions()));
        advanceTo(model, 10);
        SimulationSnapshot snapshot = model.getSnapshot();
        try
        {
            snapshot.getQueueLengths().put(Direction.EAST, 999);
            fail("Expected an unmodifiable snapshot map.");
        }
        catch (UnsupportedOperationException expected)
        {
            // Snapshot does not expose mutable model internals.
        }
        assertEquals(
            snapshot.getQueueLength(Direction.EAST),
            model.getSnapshot().getQueueLength(Direction.EAST));
    }


    /**
     * Tests overall accounting, emergency subsets, incident boundaries, and
     * timing limits on each step of a multi-feature scenario.
     */
    @Test
    public void testMultiFeatureConservationAtEveryStep()
    {
        SimulationOptions options =
            SimulationOptions.builder().minGreenSeconds(5).maxGreenSeconds(37)
                .eventCheckIntervalSeconds(10).accidentChancePercent(30)
                .slowdownChancePercent(40).accidentDurationSeconds(21)
                .slowdownDurationSeconds(27).emergencyVehiclePercent(33)
                .randomSeed(887766L).build();
        SimulationModel model =
            new SimulationModel(config(360, 600, 900, 1200, 1800, options));
        int lastPhaseStart = 0;
        for (int i = 0; i < 360; i++)
        {
            model.step();
            SimulationSnapshot snapshot = model.getSnapshot();
            assertConserved(snapshot);
            if (snapshot.getPhaseStartSecond() != lastPhaseStart)
            {
                int interval = snapshot.getPhaseStartSecond() - lastPhaseStart;
                assertTrue(interval >= 5);
                assertTrue(interval <= 37);
                lastPhaseStart = snapshot.getPhaseStartSecond();
            }
            assertTrue(
                snapshot.getElapsedSeconds()
                    - snapshot.getPhaseStartSecond() <= 37);
        }
        SimulationResults result = model.getResults();
        assertEquals(360, result.getDurationSeconds());
        assertEquals(60 + 90 + 120 + 180, result.getTotalArrived());
        assertEquals(
            result.getTotalArrived(),
            result.getTotalCompleted() + result.getCrossingCount()
                + result.getWaitingCount());
        assertTrue(result.getTotalAccidents() > 0);
        assertTrue(result.getTotalSlowdowns() > 0);
        assertTrue(result.getTotalEmergencyArrived() > 0);
        assertTrue(result.getTotalEmergencyCompleted() > 0);
        assertEquals(
            model.getSnapshot().getPhaseSwitchCount(),
            result.getPhaseSwitchCount());
    }


    /**
     * Stress-tests the maximum configured duration and arrival rates across all
     * directions with fully emergency-classified traffic.
     */
    @Test
    public void testMaximumLoadSevenThousandTwoHundredSeconds()
    {
        SimulationOptions options =
            SimulationOptions.builder().minGreenSeconds(5).maxGreenSeconds(30)
                .emergencyVehiclePercent(100).accidentChancePercent(0)
                .slowdownChancePercent(0).randomSeed(Long.MIN_VALUE).build();
        SimulationModel model =
            new SimulationModel(config(7200, 1800, 1800, 1800, 1800, options));
        for (int t = 0; t < 7200; t++)
        {
            model.step();
            if (t % 500 == 0 || t == 7199)
            {
                assertConserved(model.getSnapshot());
            }
        }
        SimulationResults result = model.getResults();
        assertEquals(14400, result.getTotalArrived());
        assertEquals(14400, result.getTotalEmergencyArrived());
        assertEquals(
            result.getTotalArrived(),
            result.getTotalCompleted() + result.getWaitingCount()
                + result.getCrossingCount());
        assertEquals(
            result.getTotalCompleted(),
            result.getTotalEmergencyCompleted());
        assertTrue(result.getPhaseSwitchCount() > 0);
        for (Direction direction : Direction.values())
        {
            assertTrue(
                result.getFinalQueueLength(direction) <= result
                    .getMaximumQueueLength(direction));
            assertTrue(
                result.getAverageQueueLength(direction) <= result
                    .getMaximumQueueLength(direction));
        }
    }


    /**
     * Confirms legal extreme seed values are accepted and remain reproducible.
     */
    @Test
    public void testExtremeRandomSeedValues()
    {
        for (long seed : new long[] { Long.MIN_VALUE, Long.MAX_VALUE })
        {
            SimulationOptions options = SimulationOptions.builder()
                .randomSeed(seed).emergencyVehiclePercent(40)
                .accidentChancePercent(20).slowdownChancePercent(20).build();
            SimulationConfig settings = config(60, 600, 600, 600, 600, options);
            SimulationModel first = new SimulationModel(settings);
            SimulationModel second = new SimulationModel(settings);
            for (int t = 0; t < 60; t++)
            {
                first.step();
                second.step();
            }
            assertSameSnapshot(first.getSnapshot(), second.getSnapshot());
        }
    }


    /**
     * Verifies final reporting does not mutate a completed model or its state.
     */
    @Test
    public void testRepeatedResultsAndSnapshotsStayConsistent()
    {
        SimulationModel model =
            new SimulationModel(config(60, 600, 600, 600, 600, quietOptions()));
        finish(model);
        SimulationSnapshot before = model.getSnapshot();
        SimulationResults first = model.getResults();
        SimulationResults second = model.getResults();
        assertEquals(first.getTotalArrived(), second.getTotalArrived());
        assertEquals(first.getTotalCompleted(), second.getTotalCompleted());
        assertEquals(
            first.getFinalQueueLengths(),
            second.getFinalQueueLengths());
        assertEquals(
            first.getAverageQueueLengths(),
            second.getAverageQueueLengths());
        assertSameSnapshot(before, model.getSnapshot());
    }
}
