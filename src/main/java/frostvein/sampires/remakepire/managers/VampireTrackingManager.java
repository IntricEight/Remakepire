package frostvein.sampires.remakepire.managers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class VampireTrackingManager {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final Map<UUID, BukkitTask> activeTrackingSessions = new ConcurrentHashMap<>();
    private UUID mostRecentVampireId = null;
    private static final int TRACKING_DURATION_SECONDS = 120;
    private static final int UPDATE_INTERVAL_TICKS = 4;

    public VampireTrackingManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
    }

    public void startTrackingNewVampire(Player newVampire) {
        if (newVampire != null && newVampire.isOnline()) {
            final UUID newVampireId = newVampire.getUniqueId();
            this.stopTracking(newVampireId);
            this.mostRecentVampireId = newVampireId;

            BukkitTask trackingTask = (new BukkitRunnable() {
                int ticksRemaining = TRACKING_DURATION_SECONDS * 20;

                public void run() {
                    if (this.ticksRemaining <= 0) {
                        VampireTrackingManager.this.stopTracking(newVampireId);
                    } else {
                        Player trackedPlayer = Bukkit.getPlayer(newVampireId);

                        if (trackedPlayer != null && trackedPlayer.isOnline()) {
                            VampireTrackingManager.this.updateTrackingForAllVampires(trackedPlayer);
                            this.ticksRemaining -= UPDATE_INTERVAL_TICKS;
                        } else {
                            VampireTrackingManager.this.stopTracking(newVampireId);
                        }
                    }
                }
            }).runTaskTimer(this.plugin, 0L, 4L);

            this.activeTrackingSessions.put(newVampireId, trackingTask);
            this.plugin.getLogger().info("Started vampire tracking for " + newVampire.getName() + " (120s)");
        }
    }

    private void updateTrackingForAllVampires(Player trackedVampire) {
        if (this.mostRecentVampireId == null || trackedVampire.getUniqueId().equals(this.mostRecentVampireId)) {
            Location trackedLocation = trackedVampire.getLocation();

            for(Player vampire : Bukkit.getOnlinePlayers()) {
                if (this.vampireManager.isVampire(vampire) && !vampire.getUniqueId().equals(trackedVampire.getUniqueId()) && vampire.getWorld().equals(trackedVampire.getWorld()) && (this.plugin.getVampireFeedingManager() == null || !this.plugin.getVampireFeedingManager().isFeeding(vampire))) {
                    Location vampireLocation = vampire.getLocation();

                    double deltaX = trackedLocation.getX() - vampireLocation.getX();
                    double deltaZ = trackedLocation.getZ() - vampireLocation.getZ();
                    double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

                    String direction = this.getRelativeDirection(deltaX, deltaZ, vampireLocation.getYaw());
                    String message = String.format("§3New Mimic: §7(§f%.0f blocks§7)", distance);

                    this.plugin.getSessionManager().sendActionBar(vampire, message);
                }
            }
        }
    }

    private String getRelativeDirection(double deltaX, double deltaZ, float playerYaw) {
        double targetAngle = Math.atan2(deltaX, -deltaZ);
        double targetDegrees = Math.toDegrees(targetAngle);

        if (targetDegrees < 0) {
            targetDegrees += 360.0;
        }

        double playerFacing = (double)((playerYaw + 180.0F) % 360.0F);
        if (playerFacing < 0) {
            playerFacing += 360.0;
        }

        double relativeAngle = (targetDegrees - playerFacing + 360.0) % 360.0;
        if (!(relativeAngle >= 337.5) && !(relativeAngle < 22.5)) {
            if (relativeAngle >= 22.5 && relativeAngle < 67.5) {
                return "\ue00b";
            } else if (relativeAngle >= 67.5 && relativeAngle < 112.5) {
                return "\ue00c";
            } else if (relativeAngle >= 112.5 && relativeAngle < 157.5) {
                return "\ue00d";
            } else if (relativeAngle >= 157.5 && relativeAngle < 202.5) {
                return "\ue00e";
            } else if (relativeAngle >= 202.5 && relativeAngle < 247.5) {
                return "\ue00f";
            } else if (relativeAngle >= 247.5 && relativeAngle < 292.5) {
                return "\ue010";
            } else {
                return relativeAngle >= 292.5 && relativeAngle < 337.5 ? "\ue011" : "\ue00a";
            }
        } else {
            return "\ue00a";
        }
    }

    public void stopTracking(UUID vampireId) {
        BukkitTask task = (BukkitTask)this.activeTrackingSessions.remove(vampireId);
        if (task != null) {
            task.cancel();
            this.plugin.getLogger().info("Stopped vampire tracking for " + String.valueOf(vampireId));
        }
    }

    public void stopAllTracking() {
        for(BukkitTask task : this.activeTrackingSessions.values()) {
            task.cancel();
        }

        this.activeTrackingSessions.clear();
        this.plugin.getLogger().info("Stopped all vampire tracking sessions");
    }

    public int getActiveTrackingCount() {
        return this.activeTrackingSessions.size();
    }

    public boolean isBeingTracked(UUID vampireId) {
        return this.activeTrackingSessions.containsKey(vampireId);
    }

    public void shutdown() {
        this.stopAllTracking();
        this.plugin.getLogger().info("VampireTrackingManager shutdown complete");
    }
}
