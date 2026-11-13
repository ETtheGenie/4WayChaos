package defaultPackage;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Stores the configuration parameters for a traffic intersection simulation.
 * All configuration values are validated during construction. Once created, a
 * SimulationConfig object cannot be modified.
 */
public class SimulationConfig
{

    // Allowed configuration limits
    public static final int MIN_ARRIVAL_RATE = 0;
    public static final int MAX_ARRIVAL_RATE = 1800;

    public static final int MIN_GREEN_SECONDS = 5;
    public static final int MAX_GREEN_SECONDS = 120;

    public static final int MIN_DURATION_SECONDS = 60;
    public static final int MAX_DURATION_SECONDS = 7200;

    public static final int MIN_TURN_PERCENT = 0;
    public static final int MAX_TURN_PERCENT = 100;

    // Simulation configuration
    private final Map<Direction, Integer> arrivalRatesPerHour;
    private final int nsGreenSeconds;
    private final int ewGreenSeconds;
    private final int durationSeconds;
    private final int leftTurnPercent;
    private final int uTurnPercent;

    /**
     * Constructs a validated simulation configuration. Arrival rates must be
     * provided for all four approaches. Turning percentages apply to all
     * incoming vehicles.
     *
     * @param arrivalRatesPerHour
     *            arrival rates by direction
     * @param nsGreenSeconds
     *            north-south green duration
     * @param ewGreenSeconds
     *            east-west green duration
     * @param durationSeconds
     *            total simulation duration
     * @param leftTurnPercent
     *            percentage turning left
     * @param uTurnPercent
     *            percentage making U-turns
     * @throws IllegalArgumentException
     *             if any configuration parameter is invalid
     */
    public SimulationConfig(
        Map<Direction, Integer> arrivalRatesPerHour,
        int nsGreenSeconds,
        int ewGreenSeconds,
        int durationSeconds,
        int leftTurnPercent,
        int uTurnPercent)
    {

        // Validate arrival-rate map
        if (arrivalRatesPerHour == null)
        {
            throw new IllegalArgumentException("Arrival rates cannot be null.");
        }

        if (arrivalRatesPerHour.size() != Direction.values().length)
        {
            throw new IllegalArgumentException(
                "Arrival rates must include all four directions.");
        }

        // Copy and validate each arrival rate
        EnumMap<Direction, Integer> rates = new EnumMap<>(Direction.class);

        for (Direction direction : Direction.values())
        {

            Integer rate = arrivalRatesPerHour.get(direction);

            if (rate == null)
            {
                throw new IllegalArgumentException(
                    "Missing arrival rate for " + direction);
            }

            if (rate < MIN_ARRIVAL_RATE || rate > MAX_ARRIVAL_RATE)
            {
                throw new IllegalArgumentException(
                    "Invalid arrival rate for " + direction
                        + ". Must be between " + MIN_ARRIVAL_RATE + " and "
                        + MAX_ARRIVAL_RATE + ".");
            }

            rates.put(direction, rate);
        }

        // Validate signal durations
        if (nsGreenSeconds < MIN_GREEN_SECONDS
            || nsGreenSeconds > MAX_GREEN_SECONDS)
        {
            throw new IllegalArgumentException(
                "Invalid north-south green duration.");
        }

        if (ewGreenSeconds < MIN_GREEN_SECONDS
            || ewGreenSeconds > MAX_GREEN_SECONDS)
        {
            throw new IllegalArgumentException(
                "Invalid east-west green duration.");
        }

        // Validate simulation duration
        if (durationSeconds < MIN_DURATION_SECONDS
            || durationSeconds > MAX_DURATION_SECONDS)
        {
            throw new IllegalArgumentException("Invalid simulation duration.");
        }

        // Validate turning percentages
        if (leftTurnPercent < MIN_TURN_PERCENT
            || leftTurnPercent > MAX_TURN_PERCENT)
        {
            throw new IllegalArgumentException("Invalid left-turn percentage.");
        }

        if (uTurnPercent < MIN_TURN_PERCENT || uTurnPercent > MAX_TURN_PERCENT)
        {
            throw new IllegalArgumentException("Invalid U-turn percentage.");
        }

        if (leftTurnPercent + uTurnPercent > 100)
        {
            throw new IllegalArgumentException(
                "Combined turning percentages cannot exceed 100.");
        }

        // Store validated configuration
        this.arrivalRatesPerHour = Collections.unmodifiableMap(rates);

        this.nsGreenSeconds = nsGreenSeconds;
        this.ewGreenSeconds = ewGreenSeconds;
        this.durationSeconds = durationSeconds;
        this.leftTurnPercent = leftTurnPercent;
        this.uTurnPercent = uTurnPercent;
    }


    /**
     * Returns the configured arrival rate for an approach.
     *
     * @param direction
     *            incoming traffic direction
     * @return arrival rate in vehicles per hour
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
     * Returns an unmodifiable map of all arrival rates.
     *
     * @return arrival rates indexed by direction
     */
    public Map<Direction, Integer> getArrivalRatesPerHour()
    {
        return arrivalRatesPerHour;
    }


    /**
     * Returns the north-south green duration.
     *
     * @return green duration in seconds
     */
    public int getNsGreenSeconds()
    {
        return nsGreenSeconds;
    }


    /**
     * Returns the east-west green duration.
     *
     * @return green duration in seconds
     */
    public int getEwGreenSeconds()
    {
        return ewGreenSeconds;
    }


    /**
     * Returns the simulation duration.
     *
     * @return simulation duration in seconds
     */
    public int getDurationSeconds()
    {
        return durationSeconds;
    }


    /**
     * Returns the left-turn percentage.
     *
     * @return percentage of vehicles turning left
     */
    public int getLeftTurnPercent()
    {
        return leftTurnPercent;
    }


    /**
     * Returns the U-turn percentage.
     *
     * @return percentage of vehicles making U-turns
     */
    public int getUTurnPercent()
    {
        return uTurnPercent;
    }
}
