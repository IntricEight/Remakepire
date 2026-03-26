package frostvein.sampires.remakepire.managers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import frostvein.sampires.remakepire.utils.ConversionAssistant;
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

    /**
     * Create an instance of the New Vampire Tracking manager.
     *
     * @param plugin the host plugin object.
     */
    public VampireTrackingManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
    }

    /**
     * Begin directing the other vampires in the direction of the new fledgling.
     *
     * @param newVampire the newly turned player.
     */
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
            this.plugin.logInfo("Started vampire tracking for " + newVampire.getName() + " (120s)");
        }
    }

    /**
     * Direct the other vampires in the direction of the new fledgling.
     *
     * @param trackedVampire the newly turned player.
     */
    private void updateTrackingForAllVampires(Player trackedVampire) {
        if (this.mostRecentVampireId == null || trackedVampire.getUniqueId().equals(this.mostRecentVampireId)) {
            Location trackedLocation = trackedVampire.getLocation();
            final ConversionAssistant conversionAssistant = new ConversionAssistant();

            for(Player vampire : Bukkit.getOnlinePlayers()) {
                if (this.vampireManager.isVampire(vampire) && !vampire.getUniqueId().equals(trackedVampire.getUniqueId()) && vampire.getWorld().equals(trackedVampire.getWorld()) && (this.plugin.getVampireFeedingManager() == null || !this.plugin.getVampireFeedingManager().isFeeding(vampire))) {
                    Location vampireLocation = vampire.getLocation();

                    double deltaX = trackedLocation.getX() - vampireLocation.getX();
                    double deltaZ = trackedLocation.getZ() - vampireLocation.getZ();
                    double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

                    String direction = conversionAssistant.getRelativeDirection(deltaX, deltaZ, vampireLocation.getYaw());
                    String message = String.format("§4New Vampire: §f%s §7(§f%.0f blocks§7)", direction, distance);

                    this.plugin.getSessionManager().sendActionBar(vampire, message);
                }
            }
        }
    }

    /**
     * Stop tracking the newly turned vampire.
     *
     * @param vampireId the UUID of the new vampire.
     */
    public void stopTracking(UUID vampireId) {
        BukkitTask task = this.activeTrackingSessions.remove(vampireId);

        if (task != null) {
            task.cancel();
            this.plugin.logInfo("Stopped vampire tracking for " + String.valueOf(vampireId));
        }
    }

    /**
     * Stop tracking all newly turned vampires.
     */
    public void stopAllTracking() {
        for(BukkitTask task : this.activeTrackingSessions.values()) {
            task.cancel();
        }

        this.activeTrackingSessions.clear();
        this.plugin.logInfo("Stopped all vampire tracking sessions");
    }

    /**
     * Retrieve the number of vampires being tracked.
     *
     * @return The number of ongoing trackers.
     */
    public int getActiveTrackingCount() {
        return this.activeTrackingSessions.size();
    }

    /**
     * Retrieve whether the player is being tracked by other vampires.
     *
     * @param vampireId the UUID of a player.
     * @return {@code true} if the vampire is being tracked.
     */
    public boolean isBeingTracked(UUID vampireId) {
        return this.activeTrackingSessions.containsKey(vampireId);
    }

    /**
     * Stop tracking any new vampires before shutting down the manager.
     */
    public void shutdown() {
        this.stopAllTracking();
        this.plugin.logInfo("VampireTrackingManager shutdown complete");
    }
}
