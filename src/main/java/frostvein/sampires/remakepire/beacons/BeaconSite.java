package frostvein.sampires.remakepire.beacons;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import frostvein.sampires.remakepire.managers.SessionManager;

public class BeaconSite {
    // The name of the beacon
    private String name;
    private String worldName;
    // The coordinates of the beacon
    private double x, y, z;
    // The alignment of the beacon. Defaults to neutral.
    private BeaconState state = BeaconSite.BeaconState.NEUTRAL;
    // Controls the distance a player can be from a beacon while effecting it
    private int captureRadius = 10;
    private long lastStateChangeTime;
    private String lastChangedBy;
    // The timestamp when the beacon is convertible again.
    private long conversionCooldownUntil = 0L;

    /**
     * Create an instance of a neutral beacon.
     */
    public BeaconSite() {
        this.lastStateChangeTime = System.currentTimeMillis();
    }

    /**
     * Create a defined beacon instance at a set location.
     *
     * @param name the title of the beacon.
     * @param location the location of the beacon.
     */
    public BeaconSite(String name, Location location) {
        this.name = name;
        this.worldName = location.getWorld().getName();

        // Retrieve the world coordinates of the beacon.
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();

        this.lastStateChangeTime = System.currentTimeMillis();
    }

    /**
     * Retrieve the beacon's location.
     *
     * @return The {@code Location} of the beacon's coordinates.
     */
    public Location getLocation() {
        World world = Bukkit.getWorld(this.worldName);
        return world == null ? null : new Location(world, this.x, this.y, this.z);
    }

    /**
     * Determine if a location is within the beacon's capture range.
     *
     * @param location A location within the world.
     * @return {@code true}
     */
    public boolean isWithinCaptureRadius(Location location) {
        Location beaconLoc = this.getLocation();

        if (beaconLoc != null && beaconLoc.getWorld().equals(location.getWorld())) {
            return beaconLoc.distance(location) <= this.captureRadius;

        } else {
            return false;
        }
    }

    /**
     * Change the alignment of the beacon.
     *
     * @param newState the new alignment of the beacon.
     * @param changedBy the method that changed the beacon.
     * @param sessionManager the manager for the session states.
     * @param cooldownMs how long the beacon will remain inconvertible.
     */
    public void changeState(BeaconState newState, String changedBy, SessionManager sessionManager, long cooldownMs) {
        this.state = newState;
        this.lastChangedBy = changedBy;
        this.lastStateChangeTime = System.currentTimeMillis();

        if (newState == BeaconSite.BeaconState.HOLY || newState == BeaconSite.BeaconState.DESECRATED) {
            this.conversionCooldownUntil = sessionManager.getSessionTime() + cooldownMs;
        }
    }

    /**
     * Change the alignment of the beacon.
     *
     * @param newState the new alignment of the beacon.
     * @param changedBy the method that changed the beacon.
     * @param sessionManager the manager for the session states.
     */
    public void changeState(BeaconState newState, String changedBy, SessionManager sessionManager) {
        this.changeState(newState, changedBy, sessionManager, 3600000L);
    }

    /**
     * Change the alignment of the beacon.
     *
     * @param newState the new alignment of the beacon.
     * @param changedBy the method that changed the beacon.
     * @param cooldownMs how long the beacon will remain inconvertible.
     */
    public void changeState(BeaconState newState, String changedBy, long cooldownMs) {
        this.state = newState;
        this.lastChangedBy = changedBy;
        this.lastStateChangeTime = System.currentTimeMillis();

        if (newState == BeaconSite.BeaconState.HOLY || newState == BeaconSite.BeaconState.DESECRATED) {
            this.conversionCooldownUntil = System.currentTimeMillis() + cooldownMs;
        }
    }

    /**
     * Change the alignment of the beacon.
     *
     * @param newState the new alignment of the beacon.
     * @param changedBy the method that changed the beacon.
     */
    public void changeState(BeaconState newState, String changedBy) {
        this.changeState(newState, changedBy, 3600000L);
    }

    /**
     * Determine if the beacon's conversion cooldown has elapsed.
     *
     * @param sessionManager the manager for the session states.
     * @return {@code true} if the beacon's conversation cooldown is active.
     */
    public boolean isOnConversionCooldown(SessionManager sessionManager) {
        return sessionManager.getSessionTime() < this.conversionCooldownUntil;
    }

    /** @deprecated */
    @Deprecated
    public boolean isOnConversionCooldown() {
        return System.currentTimeMillis() < this.conversionCooldownUntil;
    }

    /**
     * Retrieve the remaining milliseconds on the beacon's conversion cooldown.
     *
     * @param sessionManager the manager for the session states.
     * @return A {@code long} of the remaining time on the beacon's conversation cooldown.
     */
    public long getRemainingCooldownMs(SessionManager sessionManager) {
        long remaining = this.conversionCooldownUntil - sessionManager.getSessionTime();
        return Math.max(0L, remaining);
    }

    /** @deprecated */
    @Deprecated
    public long getRemainingCooldownMs() {
        long remaining = this.conversionCooldownUntil - System.currentTimeMillis();
        return Math.max(0L, remaining);
    }

    /**
     * Retrieve the remaining time on the beacon's conversion cooldown.
     *
     * @param sessionManager the manager for the session states.
     * @return A {@code String} of the remaining time on the beacon's conversation cooldown.
     */
    public String getRemainingCooldownString(SessionManager sessionManager) {
        long remaining = this.getRemainingCooldownMs(sessionManager);

        if (remaining <= 0L) {
            return "Ready";

        } else {
            long minutes = remaining / 60000L;
            long seconds = remaining % 60000L / 1000L;
            return minutes > 0L ? minutes + "m " + seconds + "s" : seconds + "s";
        }
    }

    /** @deprecated */
    @Deprecated
    public String getRemainingCooldownString() {
        long remaining = this.getRemainingCooldownMs();
        if (remaining <= 0L) {
            return "Ready";
        } else {
            long minutes = remaining / 60000L;
            long seconds = remaining % 60000L / 1000L;
            return minutes > 0L ? minutes + "m " + seconds + "s" : seconds + "s";
        }
    }

    /**
     * Reduce the beacon's conversion cooldown by the provided time.
     *
     * @param milliseconds the time to be removed from the cooldown.
     */
    public void reduceCooldown(long milliseconds) {
        if (this.conversionCooldownUntil > System.currentTimeMillis()) {
            this.conversionCooldownUntil = Math.max(System.currentTimeMillis(), this.conversionCooldownUntil - milliseconds);
        }
    }

    /**
     * Retrieve the current status of this beacon.
     *
     * @param sessionManager the manager for the session states.
     * @return A {@code String} of the beacon's current details.
     */
    public String getStatusString(SessionManager sessionManager) {
        String stateColor = this.state.getColorCode();
        String timeAgo = this.getTimeSinceStateChange();
        String status = String.format("§e%s §7at §f(%.0f, %.0f, %.0f) §7in §f%s", this.name, this.x, this.y, this.z, this.worldName);
        status = status + String.format("\n  §7State: %s%s §7| Radius: §e%d blocks", stateColor, this.state.getDisplayName(), this.captureRadius);

        if (this.lastChangedBy != null) {
            status = status + String.format("\n  §7Last changed by: §f%s §7(%s ago)", this.lastChangedBy, timeAgo);
        }

        // Display the cooldown status
        if (this.isOnConversionCooldown(sessionManager)) {
            status = status + String.format("\n  §c⏰ Conversion cooldown: %s remaining (session time)", this.getRemainingCooldownString(sessionManager));
        } else if (this.state != BeaconSite.BeaconState.NEUTRAL) {
            status = status + "\n  §a✓ Ready for conversion";
        }

        return status;
    }

    /** @deprecated */
    @Deprecated
    public String getStatusString() {
        String stateColor = this.state.getColorCode();
        String timeAgo = this.getTimeSinceStateChange();
        String status = String.format("§e%s §7at §f(%.0f, %.0f, %.0f) §7in §f%s", this.name, this.x, this.y, this.z, this.worldName);
        status = status + String.format("\n  §7State: %s%s §7| Radius: §e%d blocks", stateColor, this.state.getDisplayName(), this.captureRadius);
        if (this.lastChangedBy != null) {
            status = status + String.format("\n  §7Last changed by: §f%s §7(%s ago)", this.lastChangedBy, timeAgo);
        }

        if (this.isOnConversionCooldown()) {
            status = status + String.format("\n  §c⏰ Conversion cooldown: %s remaining", this.getRemainingCooldownString());
        } else if (this.state != BeaconSite.BeaconState.NEUTRAL) {
            status = status + "\n  §a✓ Ready for conversion";
        }

        return status;
    }

    /**
     * Retrieve the time since the beacon was last converted.
     *
     * @return A {@code String} of the time since the beacon last changed alignment.
     */
    private String getTimeSinceStateChange() {
        long timeDiff = System.currentTimeMillis() - this.lastStateChangeTime;
        long seconds = timeDiff / 1000L;

        if (seconds < 60L) {
            return seconds + "s";
        } else if (seconds < 3600L) {
            return seconds / 60L + "m";
        } else {
            return seconds < 86400L ? seconds / 3600L + "h" : seconds / 86400L + "d";
        }
    }

    /**
     * Retrieve the beacon's name.
     *
     * @return A {@code String} of the beacon's name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Rename the beacon.
     *
     * @param name the new name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Retrieve the world's name.
     *
     * @return A {@code String} of the world's name.
     */
    public String getWorldName() {
        return this.worldName;
    }

    /**
     * Rename the beacon's world name. Does not change the name of the world itself, just what the beacon records the name as.
     *
     * @param worldName the world's new name.
     */
    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    /**
     * Retrieve the beacon's X coordinate.
     *
     * @return The beacon's X coordinate.
     */
    public double getX() {
        return this.x;
    }

    /**
     * Set the beacon's new X coordinate.
     *
     * @param x the new X coordinate.
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * Retrieve the beacon's Y coordinate.
     *
     * @return The beacon's Y coordinate.
     */
    public double getY() {
        return this.y;
    }

    /**
     * Set the beacon's new Y coordinate.
     *
     * @param y the new Y coordinate.
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * Retrieve the beacon's Z coordinate.
     *
     * @return The beacon's Z coordinate.
     */
    public double getZ() {
        return this.z;
    }

    /**
     * Set the beacon's new Z coordinate.
     *
     * @param z the new Z coordinate.
     */
    public void setZ(double z) {
        this.z = z;
    }

    /**
     * Retrieve the current alignment of the beacon.
     *
     * @return The beacon's current alignment.
     */
    public BeaconState getState() {
        return this.state;
    }

    /**
     * Set the alignment of the beacon.
     *
     * @param state the alignment of the beacon.
     */
    public void setState(BeaconState state) {
        this.state = state;
        this.lastStateChangeTime = System.currentTimeMillis();
    }

    /**
     * Retrieve the capture range of the beacon.
     *
     * @return The distance a player can stand from the beacon.
     */
    public int getCaptureRadius() {
        return this.captureRadius;
    }

    /**
     * Set the distance a player can capture the beacon from.
     *
     * @param captureRadius the new capture distance (in blocks).
     */
    public void setCaptureRadius(int captureRadius) {
        this.captureRadius = captureRadius;
    }

    /**
     * Retrieve when the beacon last changed alignment.
     *
     * @return A {@code long} timestamp of when the beacon was last converted.
     */
    public long getLastStateChangeTime() {
        return this.lastStateChangeTime;
    }

    /**
     * Set when the beacon last changed alignment.
     *
     * @param lastStateChangeTime the time when the beacon was last converted.
     */
    public void setLastStateChangeTime(long lastStateChangeTime) {
        this.lastStateChangeTime = lastStateChangeTime;
    }

    /**
     * Retrieve the cause of the beacon's last conversion.
     *
     * @return the method that changed the beacon.
     */
    public String getLastChangedBy() {
        return this.lastChangedBy;
    }

    /**
     * Set the cause of the beacon's last conversion.
     *
     * @param lastChangedBy the method that changed the beacon.
     */
    public void setLastChangedBy(String lastChangedBy) {
        this.lastChangedBy = lastChangedBy;
    }

    /**
     * Retrieve the timestamp when the beacon will become convertible.
     *
     * @return A {@code long} timestamp of when the beacon can change alignments again.
     */
    public long getConversionCooldownUntil() {
        return this.conversionCooldownUntil;
    }

    /**
     * Set the timestamp when the beacon will become convertible.
     *
     * @param conversionCooldownUntil A {@code long} timestamp of when the beacon can change alignments again.
     */
    public void setConversionCooldownUntil(long conversionCooldownUntil) {
        this.conversionCooldownUntil = conversionCooldownUntil;
    }

    /**
     * Retrieve the location for the beacon's particles.
     *
     * @return A {@code Location} coordinate for particles to appear.
     */
    public Location getParticleLocation() {
        Location loc = this.getLocation();
        return loc != null ? loc.add(0.0, 1.5, 0.0) : null;
    }

    /**
     * Determine if the beacon should be showing one of the side's alignment particles.
     *
     * @return {@code true} if the beacon is not neutral.
     */
    public boolean shouldShowParticles() {
        return this.state != BeaconSite.BeaconState.NEUTRAL;
    }

    public enum BeaconState {
        NEUTRAL("Neutral", "§7"),
        HOLY("Holy", "§f"),
        DESECRATED("Desecrated", "§4"),
        PERMANENTLY_DESECRATED("Corrupted", "§8");

        private final String displayName;
        private final String colorCode;

        /**
         * Create an instance of the beacon alignment.
         *
         * @param displayName the team's name that the alignment represents.
         * @param colorCode the color of the team's alignment.
         */
        BeaconState(String displayName, String colorCode) {
            this.displayName = displayName;
            this.colorCode = colorCode;
        }

        /**
         * Retrieve the name of the beacon state.
         *
         * @return The team name of the beacon state.
         */
        public String getDisplayName() {
            return this.displayName;
        }

        /**
         * Retrieve the color of the beacon state.
         *
         * @return the beacon's color code sequence.
         */
        public String getColorCode() {
            return this.colorCode;
        }
    }
}
