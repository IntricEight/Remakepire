package frostvein.sampires.remakepire.beacons;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import frostvein.sampires.remakepire.managers.SessionManager;

public class BeaconSite {
    private String name;
    private String worldName;
    private double x;
    private double y;
    private double z;
    private BeaconState state;
    private int captureRadius;
    private long lastStateChangeTime;
    private String lastChangedBy;
    private long conversionCooldownUntil;

    public BeaconSite() {
        this.state = BeaconSite.BeaconState.NEUTRAL;
        this.captureRadius = 10;
        this.lastStateChangeTime = System.currentTimeMillis();
        this.conversionCooldownUntil = 0L;
    }

    public BeaconSite(String name, Location location) {
        this.name = name;
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.state = BeaconSite.BeaconState.NEUTRAL;
        this.captureRadius = 10;
        this.lastStateChangeTime = System.currentTimeMillis();
        this.conversionCooldownUntil = 0L;
    }

    public Location getLocation() {
        World world = Bukkit.getWorld(this.worldName);
        return world == null ? null : new Location(world, this.x, this.y, this.z);
    }

    public boolean isWithinCaptureRadius(Location location) {
        Location beaconLoc = this.getLocation();
        if (beaconLoc != null && beaconLoc.getWorld().equals(location.getWorld())) {
            return beaconLoc.distance(location) <= (double)this.captureRadius;
        } else {
            return false;
        }
    }

    public void changeState(BeaconState newState, String changedBy, SessionManager sessionManager, long cooldownMs) {
        this.state = newState;
        this.lastChangedBy = changedBy;
        this.lastStateChangeTime = System.currentTimeMillis();
        if (newState == BeaconSite.BeaconState.HOLY || newState == BeaconSite.BeaconState.DESECRATED) {
            this.conversionCooldownUntil = sessionManager.getSessionTime() + cooldownMs;
        }

    }

    public void changeState(BeaconState newState, String changedBy, SessionManager sessionManager) {
        this.changeState(newState, changedBy, sessionManager, 3600000L);
    }

    public void changeState(BeaconState newState, String changedBy, long cooldownMs) {
        this.state = newState;
        this.lastChangedBy = changedBy;
        this.lastStateChangeTime = System.currentTimeMillis();
        if (newState == BeaconSite.BeaconState.HOLY || newState == BeaconSite.BeaconState.DESECRATED) {
            this.conversionCooldownUntil = System.currentTimeMillis() + cooldownMs;
        }

    }

    public void changeState(BeaconState newState, String changedBy) {
        this.changeState(newState, changedBy, 3600000L);
    }

    public boolean isOnConversionCooldown(SessionManager sessionManager) {
        return sessionManager.getSessionTime() < this.conversionCooldownUntil;
    }

    /** @deprecated */
    @Deprecated
    public boolean isOnConversionCooldown() {
        return System.currentTimeMillis() < this.conversionCooldownUntil;
    }

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

    public void reduceCooldown(long milliseconds) {
        if (this.conversionCooldownUntil > System.currentTimeMillis()) {
            this.conversionCooldownUntil = Math.max(System.currentTimeMillis(), this.conversionCooldownUntil - milliseconds);
        }

    }

    public String getStatusString(SessionManager sessionManager) {
        String stateColor = this.state.getColorCode();
        String timeAgo = this.getTimeSinceStateChange();
        String status = String.format("§e%s §7at §f(%.0f, %.0f, %.0f) §7in §f%s", this.name, this.x, this.y, this.z, this.worldName);
        status = status + String.format("\n  §7State: %s%s §7| Radius: §e%d blocks", stateColor, this.state.getDisplayName(), this.captureRadius);
        if (this.lastChangedBy != null) {
            status = status + String.format("\n  §7Last changed by: §f%s §7(%s ago)", this.lastChangedBy, timeAgo);
        }

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

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWorldName() {
        return this.worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public double getX() {
        return this.x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return this.y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return this.z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public BeaconState getState() {
        return this.state;
    }

    public void setState(BeaconState state) {
        this.state = state;
        this.lastStateChangeTime = System.currentTimeMillis();
    }

    public int getCaptureRadius() {
        return this.captureRadius;
    }

    public void setCaptureRadius(int captureRadius) {
        this.captureRadius = captureRadius;
    }

    public long getLastStateChangeTime() {
        return this.lastStateChangeTime;
    }

    public void setLastStateChangeTime(long lastStateChangeTime) {
        this.lastStateChangeTime = lastStateChangeTime;
    }

    public String getLastChangedBy() {
        return this.lastChangedBy;
    }

    public void setLastChangedBy(String lastChangedBy) {
        this.lastChangedBy = lastChangedBy;
    }

    public long getConversionCooldownUntil() {
        return this.conversionCooldownUntil;
    }

    public void setConversionCooldownUntil(long conversionCooldownUntil) {
        this.conversionCooldownUntil = conversionCooldownUntil;
    }

    public Location getParticleLocation() {
        Location loc = this.getLocation();
        return loc != null ? loc.add((double)0.0F, (double)1.5F, (double)0.0F) : null;
    }

    public boolean shouldShowParticles() {
        return this.state != BeaconSite.BeaconState.NEUTRAL;
    }

    public static enum BeaconState {
        NEUTRAL("Neutral", "§7"),
        HOLY("Holy", "§f"),
        DESECRATED("Desecrated", "§4"),
        PERMANENTLY_DESECRATED("Corrupted", "§8");

        private final String displayName;
        private final String colorCode;

        private BeaconState(String displayName, String colorCode) {
            this.displayName = displayName;
            this.colorCode = colorCode;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getColorCode() {
            return this.colorCode;
        }
    }
}
