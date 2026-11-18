
package defaultPackage;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable configuration for one V2 traffic simulation. Stores the four
 * approach arrival rates, run duration, turning probabilities, and validated
 * adaptive signal, emergency-vehicle, and incident options. Simulation behavior
 * and randomness are managed by other model classes.
 */
public final class SimulationConfig
{

    public static final int MIN_ARRIVAL_RATE = 0;
    public static final int MAX_ARRIVAL_RATE = 1800;
    public static final int MIN_DURATION_SECONDS = 60;
    public static final int MAX_DURATION_SECONDS = 7200;
    public static final int MIN_TURN_PERCENT = 0;
    public static final int MAX_TURN_PERCENT = 100;

    private final Map<Direction, Integer> arrivalRatesPerHour;
    private final int durationSeconds;
    private final int leftTurnPercent;
    private final int uTurnPercent;
    private final SimulationOptions options;

    /**
     * Creates a validated V2 configuration. Turning percentages are independent
     * probabilities for each new vehicle; straight-through probability is 100 -
     * leftTurnPercent - uTurnPercent.
     *
     * @param arrivalRatesPerHour
     *            rates for all four directions, in vehicles/hour
     * @param durationSeconds
     *            duration of the simulation, in simulated seconds
     * @param leftTurnPercent
     *            probability of a left turn, from 0 to 100
     * @param uTurnPercent
     *            probability of a U-turn, from 0 to 100
     * @param options
     *            non-null immutable V2 simulation options
     * @throws IllegalArgumentException
     *             if any argument is invalid
     */
    public SimulationConfig(
        Map<Direction, Integer> arrivalRatesPerHour,
        int durationSeconds,
        int leftTurnPercent,
        int uTurnPercent,
        SimulationOptions options)
    {

        if (arrivalRatesPerHour == null
            || arrivalRatesPerHour.size() != Direction.values().length)
        {
            throw new IllegalArgumentException(
                "Arrival rates must contain exactly four directions.");
        }

        // Validate the entire map before retaining any settings.
        EnumMap<Direction, Integer> copy = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values())
        {
            Integer rate = arrivalRatesPerHour.get(direction);
            if (rate == null || rate < MIN_ARRIVAL_RATE
                || rate > MAX_ARRIVAL_RATE)
            {
                throw new IllegalArgumentException(
                    "Invalid arrival rate for " + direction + ".");
            }
            copy.put(direction, rate);
        }

        if (durationSeconds < MIN_DURATION_SECONDS
            || durationSeconds > MAX_DURATION_SECONDS)
        {
            throw new IllegalArgumentException(
                "Duration must be between " + MIN_DURATION_SECONDS + " and "
                    + MAX_DURATION_SECONDS + " seconds.");
        }

        if (leftTurnPercent < MIN_TURN_PERCENT
            || leftTurnPercent > MAX_TURN_PERCENT)
        {
            throw new IllegalArgumentException(
                "Left-turn percentage must be between 0 and 100.");
        }

        if (uTurnPercent < MIN_TURN_PERCENT || uTurnPercent > MAX_TURN_PERCENT)
        {
            throw new IllegalArgumentException(
                "U-turn percentage must be between 0 and 100.");
        }

        if (leftTurnPercent + uTurnPercent > MAX_TURN_PERCENT)
        {
            throw new IllegalArgumentException(
                "Combined left-turn and U-turn percentages cannot exceed 100.");
        }

        if (options == null)
        {
            throw new IllegalArgumentException(
                "Simulation options cannot be null.");
        }

        this.arrivalRatesPerHour = Collections.unmodifiableMap(copy);
        this.durationSeconds = durationSeconds;
        this.leftTurnPercent = leftTurnPercent;
        this.uTurnPercent = uTurnPercent;
        this.options = options;
    }


    /**
     * Returns the arrival rate for an incoming direction.
     *
     * @param direction
     *            incoming direction
     * @return vehicles per hour for that direction
     * @throws IllegalArgumentException
     *             if direction is null
     */
    public int getArrivalRatePerHour(Direction direction)
    {
        if (direction == null)
        {
            throw new IllegalArgumentException("Direction cannot be null.");
        }
        return arrivalRatesPerHour.get(direction);
    }


    /**
     * Returns an unmodifiable map of the four configured arrival rates.
     *
     * @return direction-indexed arrival rates
     */
    public Map<Direction, Integer> getArrivalRatesPerHour()
    {
        return arrivalRatesPerHour;
    }


    /**
     * Returns total simulation duration.
     *
     * @return duration in seconds
     */
    public int getDurationSeconds()
    {
        return durationSeconds;
    }


    /**
     * Returns the probability of a left turn for each generated vehicle.
     *
     * @return left-turn percentage
     */
    public int getLeftTurnPercent()
    {
        return leftTurnPercent;
    }


    /**
     * Returns the probability of a U-turn for each generated vehicle.
     *
     * @return U-turn percentage
     */
    public int getUTurnPercent()
    {
        return uTurnPercent;
    }


    /**
     * Returns immutable V2 behavior settings.
     *
     * @return non-null options object
     */
    public SimulationOptions getOptions()
    {
        return options;
    }
}
