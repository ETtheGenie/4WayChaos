
package defaultPackage;

/**
 * Immutable options controlling adaptive signals, emergency vehicles, random
 * traffic incidents, and reproducible random behavior in V2. Use
 * {@link #builder()} to create an instance.
 */
public final class SimulationOptions
{

    public static final int MIN_GREEN_SECONDS = 5;
    public static final int MAX_GREEN_SECONDS = 120;
    public static final int MIN_QUEUE_SWITCH_THRESHOLD = 1;
    public static final int MAX_QUEUE_SWITCH_THRESHOLD = 50;
    public static final int MIN_EVENT_CHECK_INTERVAL_SECONDS = 10;
    public static final int MAX_EVENT_CHECK_INTERVAL_SECONDS = 120;
    public static final int MIN_EVENT_DURATION_SECONDS = 5;
    public static final int MAX_EVENT_DURATION_SECONDS = 300;
    public static final int MIN_SLOWDOWN_SPEED_METERS_PER_SECOND = 1;
    public static final int MAX_SLOWDOWN_SPEED_METERS_PER_SECOND = 9;
    public static final int MIN_SLOWDOWN_HEADWAY_SECONDS = 2;
    public static final int MAX_SLOWDOWN_HEADWAY_SECONDS = 20;

    private final int minGreenSeconds;
    private final int maxGreenSeconds;
    private final int queueSwitchThreshold;
    private final int emergencyVehiclePercent;
    private final int eventCheckIntervalSeconds;
    private final int accidentChancePercent;
    private final int slowdownChancePercent;
    private final int accidentDurationSeconds;
    private final int slowdownDurationSeconds;
    private final int slowdownSpeedMetersPerSecond;
    private final int slowdownHeadwaySeconds;
    private final long randomSeed;

    /**
     * Validates and copies the settings from a builder.
     *
     * @param builder
     *            builder with candidate settings
     * @throws IllegalArgumentException
     *             if any setting is invalid
     */
    private SimulationOptions(Builder builder)
    {
        requireRange(
            "Minimum green",
            builder.minGreenSeconds,
            MIN_GREEN_SECONDS,
            MAX_GREEN_SECONDS);
        requireRange(
            "Maximum green",
            builder.maxGreenSeconds,
            MIN_GREEN_SECONDS,
            MAX_GREEN_SECONDS);

        if (builder.minGreenSeconds > builder.maxGreenSeconds)
        {
            throw new IllegalArgumentException(
                "Minimum green cannot exceed maximum green.");
        }

        requireRange(
            "Queue-switch threshold",
            builder.queueSwitchThreshold,
            MIN_QUEUE_SWITCH_THRESHOLD,
            MAX_QUEUE_SWITCH_THRESHOLD);
        requireRange(
            "Emergency-vehicle percentage",
            builder.emergencyVehiclePercent,
            0,
            100);
        requireRange(
            "Event-check interval",
            builder.eventCheckIntervalSeconds,
            MIN_EVENT_CHECK_INTERVAL_SECONDS,
            MAX_EVENT_CHECK_INTERVAL_SECONDS);
        requireRange(
            "Accident percentage",
            builder.accidentChancePercent,
            0,
            100);
        requireRange(
            "Slowdown percentage",
            builder.slowdownChancePercent,
            0,
            100);

        if (builder.accidentChancePercent + builder.slowdownChancePercent > 100)
        {
            throw new IllegalArgumentException(
                "Accident and slowdown percentages cannot exceed "
                    + "100 combined.");
        }

        requireRange(
            "Accident duration",
            builder.accidentDurationSeconds,
            MIN_EVENT_DURATION_SECONDS,
            MAX_EVENT_DURATION_SECONDS);
        requireRange(
            "Slowdown duration",
            builder.slowdownDurationSeconds,
            MIN_EVENT_DURATION_SECONDS,
            MAX_EVENT_DURATION_SECONDS);
        requireRange(
            "Slowdown speed",
            builder.slowdownSpeedMetersPerSecond,
            MIN_SLOWDOWN_SPEED_METERS_PER_SECOND,
            MAX_SLOWDOWN_SPEED_METERS_PER_SECOND);
        requireRange(
            "Slowdown headway",
            builder.slowdownHeadwaySeconds,
            MIN_SLOWDOWN_HEADWAY_SECONDS,
            MAX_SLOWDOWN_HEADWAY_SECONDS);

        minGreenSeconds = builder.minGreenSeconds;
        maxGreenSeconds = builder.maxGreenSeconds;
        queueSwitchThreshold = builder.queueSwitchThreshold;
        emergencyVehiclePercent = builder.emergencyVehiclePercent;
        eventCheckIntervalSeconds = builder.eventCheckIntervalSeconds;
        accidentChancePercent = builder.accidentChancePercent;
        slowdownChancePercent = builder.slowdownChancePercent;
        accidentDurationSeconds = builder.accidentDurationSeconds;
        slowdownDurationSeconds = builder.slowdownDurationSeconds;
        slowdownSpeedMetersPerSecond = builder.slowdownSpeedMetersPerSecond;
        slowdownHeadwaySeconds = builder.slowdownHeadwaySeconds;
        randomSeed = builder.randomSeed;
    }


    /**
     * Checks whether a numeric option falls inside its permitted range.
     *
     * @param name
     *            option name used in error messages
     * @param value
     *            candidate value
     * @param minimum
     *            inclusive minimum
     * @param maximum
     *            inclusive maximum
     * @throws IllegalArgumentException
     *             if the value is outside the range
     */
    private static
        void
        requireRange(String name, int value, int minimum, int maximum)
    {

        if (value < minimum || value > maximum)
        {
            throw new IllegalArgumentException(
                name + " must be between " + minimum + " and " + maximum + ".");
        }
    }


    /**
     * Starts a builder initialized with V2 default settings.
     *
     * @return new mutable builder
     */
    public static Builder builder()
    {
        return new Builder();
    }


    /**
     * @return minimum green duration in seconds
     */
    public int getMinGreenSeconds()
    {
        return minGreenSeconds;
    }


    /**
     * @return maximum green duration in seconds
     */
    public int getMaxGreenSeconds()
    {
        return maxGreenSeconds;
    }


    /**
     * @return queue difference required for congestion-driven switching
     */
    public int getQueueSwitchThreshold()
    {
        return queueSwitchThreshold;
    }


    /**
     * @return percentage probability that an arriving vehicle is emergency
     */
    public int getEmergencyVehiclePercent()
    {
        return emergencyVehiclePercent;
    }


    /**
     * @return seconds between incident-generation checks
     */
    public int getEventCheckIntervalSeconds()
    {
        return eventCheckIntervalSeconds;
    }


    /**
     * @return accident probability per eligible incident check, in percent
     */
    public int getAccidentChancePercent()
    {
        return accidentChancePercent;
    }


    /**
     * @return slowdown probability per eligible incident check, in percent
     */
    public int getSlowdownChancePercent()
    {
        return slowdownChancePercent;
    }


    /**
     * @return accident duration in seconds
     */
    public int getAccidentDurationSeconds()
    {
        return accidentDurationSeconds;
    }


    /**
     * @return slowdown duration in seconds
     */
    public int getSlowdownDurationSeconds()
    {
        return slowdownDurationSeconds;
    }


    /**
     * @return vehicle crossing speed during a slowdown, in meters per second
     */
    public int getSlowdownSpeedMetersPerSecond()
    {
        return slowdownSpeedMetersPerSecond;
    }


    /**
     * @return minimum lane departure headway during a slowdown, in seconds
     */
    public int getSlowdownHeadwaySeconds()
    {
        return slowdownHeadwaySeconds;
    }


    /**
     * @return seed used to derive reproducible random-number streams
     */
    public long getRandomSeed()
    {
        return randomSeed;
    }

    /**
     * Mutable builder for an immutable SimulationOptions instance. Setters may
     * be called in any order; build() validates final values.
     */
    public static final class Builder
    {

        private int minGreenSeconds = 10;
        private int maxGreenSeconds = 60;
        private int queueSwitchThreshold = 3;
        private int emergencyVehiclePercent = 5;
        private int eventCheckIntervalSeconds = 30;
        private int accidentChancePercent = 5;
        private int slowdownChancePercent = 10;
        private int accidentDurationSeconds = 45;
        private int slowdownDurationSeconds = 60;
        private int slowdownSpeedMetersPerSecond = 5;
        private int slowdownHeadwaySeconds = 4;
        private long randomSeed = 1L;

        /**
         * Creates a builder with the documented V2 defaults.
         */
        private Builder()
        {
        }


        /**
         * Sets the shortest allowed green interval.
         *
         * @param seconds
         *            minimum duration in seconds
         * @return this builder
         */
        public Builder minGreenSeconds(int seconds)
        {
            minGreenSeconds = seconds;
            return this;
        }


        /**
         * Sets the longest allowed green interval.
         *
         * @param seconds
         *            maximum duration in seconds
         * @return this builder
         */
        public Builder maxGreenSeconds(int seconds)
        {
            maxGreenSeconds = seconds;
            return this;
        }


        /**
         * Sets the queue imbalance needed for a congestion-driven switch.
         *
         * @param vehicles
         *            positive queue-length difference
         * @return this builder
         */
        public Builder queueSwitchThreshold(int vehicles)
        {
            queueSwitchThreshold = vehicles;
            return this;
        }


        /**
         * Sets the independent emergency-classification probability.
         *
         * @param percent
         *            probability in percent
         * @return this builder
         */
        public Builder emergencyVehiclePercent(int percent)
        {
            emergencyVehiclePercent = percent;
            return this;
        }


        /**
         * Sets how often the simulator considers starting random incidents.
         *
         * @param seconds
         *            interval in seconds
         * @return this builder
         */
        public Builder eventCheckIntervalSeconds(int seconds)
        {
            eventCheckIntervalSeconds = seconds;
            return this;
        }


        /**
         * Sets the accident probability per eligible approach/check.
         *
         * @param percent
         *            accident probability in percent
         * @return this builder
         */
        public Builder accidentChancePercent(int percent)
        {
            accidentChancePercent = percent;
            return this;
        }


        /**
         * Sets the slowdown probability per eligible approach/check.
         *
         * @param percent
         *            slowdown probability in percent
         * @return this builder
         */
        public Builder slowdownChancePercent(int percent)
        {
            slowdownChancePercent = percent;
            return this;
        }


        /**
         * Sets how long an accident blocks new lane departures.
         *
         * @param seconds
         *            accident duration in seconds
         * @return this builder
         */
        public Builder accidentDurationSeconds(int seconds)
        {
            accidentDurationSeconds = seconds;
            return this;
        }


        /**
         * Sets how long a slowdown reduces throughput.
         *
         * @param seconds
         *            slowdown duration in seconds
         * @return this builder
         */
        public Builder slowdownDurationSeconds(int seconds)
        {
            slowdownDurationSeconds = seconds;
            return this;
        }


        /**
         * Sets vehicle speed during an active slowdown.
         *
         * @param metersPerSecond
         *            speed in meters per second
         * @return this builder
         */
        public Builder slowdownSpeedMetersPerSecond(int metersPerSecond)
        {
            slowdownSpeedMetersPerSecond = metersPerSecond;
            return this;
        }


        /**
         * Sets minimum departure headway during an active slowdown.
         *
         * @param seconds
         *            departure headway in seconds
         * @return this builder
         */
        public Builder slowdownHeadwaySeconds(int seconds)
        {
            slowdownHeadwaySeconds = seconds;
            return this;
        }


        /**
         * Sets the base seed used to create independent random streams.
         *
         * @param seed
         *            any long seed value
         * @return this builder
         */
        public Builder randomSeed(long seed)
        {
            randomSeed = seed;
            return this;
        }


        /**
         * Validates all fields and creates a new immutable options object.
         *
         * @return validated options
         * @throws IllegalArgumentException
         *             if any setting is invalid
         */
        public SimulationOptions build()
        {
            return new SimulationOptions(this);
        }
    }
}
