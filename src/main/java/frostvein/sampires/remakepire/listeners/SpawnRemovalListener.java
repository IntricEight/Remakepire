package frostvein.sampires.remakepire.listeners;

import org.bukkit.Chunk;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class SpawnRemovalListener implements Listener {
    private final RemakepirePlugin plugin;
    private boolean endermanRemovalEnabled, creeperRemovalEnabled;

    /**
     * Create an instance of the Mob Spawn Removal listener.
     *
     * @param plugin the host plugin object.
     */
    public SpawnRemovalListener(RemakepirePlugin plugin) {
        this.plugin = plugin;

        this.endermanRemovalEnabled = this.plugin.getConfig().getBoolean("hostile-mob-spawning.enderman-removal", true);
        this.creeperRemovalEnabled = this.plugin.getConfig().getBoolean("hostile-mob-spawning.creeper-removal", false);

        plugin.logInfo("SpawnRemovalListener initialized");
    }

    /**
     * Prevent certain entities from spawning in the world:
     * Setting controlled: Creepers, Endermen.<br/>
     * Prevent chickens from spawning if animal breeding is disabled, and we are out of session.
     *
     * @param event a non-player entity spawns.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!this.plugin.getConfigManager().canBreedAnimalsOutOfSession() && !this.plugin.getSessionManager().isSessionActive()) {
            event.setCancelled(event.getEntityType() == EntityType.CHICKEN && event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.EGG);

        } else if (this.endermanRemovalEnabled && event.getEntityType() == EntityType.ENDERMAN) {
            event.setCancelled(true);
            this.plugin.logInfo("Prevented Enderman spawn at " + event.getLocation().getBlockX() + ", " + event.getLocation().getBlockY() + ", " + event.getLocation().getBlockZ() + " (Reason: " + event.getSpawnReason() + ")");

        } else if (this.creeperRemovalEnabled && event.getEntityType() == EntityType.CREEPER) {
            event.setCancelled(true);
            this.plugin.logInfo("Prevented Creeper spawn at " + event.getLocation().getBlockX() + ", " + event.getLocation().getBlockY() + ", " + event.getLocation().getBlockZ() + " (Reason: " + event.getSpawnReason() + ")");
        }
    }

    /**
     * Prevent chickens from spawning when a session is not active and animal breeding is disabled.
     *
     * @param event an egg is thrown.
     */
    @EventHandler
    public void onEggThrow(PlayerEggThrowEvent event) {
        if (!this.plugin.getConfigManager().canBreedAnimalsOutOfSession() && !this.plugin.getSessionManager().isSessionActive()) {
            event.setHatching(false);
        }
    }

    /**
     * Prevent entities from spawning in new chunks.
     *
     * @param event a chunk loads.
     */
    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onChunkLoad(ChunkLoadEvent event) {
        if (this.endermanRemovalEnabled) {
            Entity[] entities = event.getChunk().getEntities();

            for (Entity entity : entities) {
                if (entity instanceof Enderman) {
                    entity.remove();
                }
            }
        }

        if (this.creeperRemovalEnabled) {
            Entity[] entities = event.getChunk().getEntities();

            for (Entity entity : entities) {
                if (entity instanceof Creeper) {
                    entity.remove();
                }
            }
        }
    }

    /**
     * Retrieve if Endermen should be removed from the game world.
     *
     * @return {@code true} if Endermen spawning is prevented.
     */
    public boolean isEndermanRemovalEnabled() {
        return this.endermanRemovalEnabled;
    }

    /**
     * Choose if Endermen should be removed from the game world.
     *
     * @param enabled should Endermen spawning be prevented.
     */
    public void setEndermanRemovalEnabled(boolean enabled) {
        this.endermanRemovalEnabled = enabled;

        this.plugin.getConfig().set("hostile-mob-spawning.enderman-removal", endermanRemovalEnabled);
        this.plugin.saveConfig();

        this.plugin.logInfo("Enderman removal " + (enabled ? "ENABLED" : "DISABLED"));
    }

    /**
     * Retrieve if Creepers should be removed from the game world.
     *
     * @return {@code true} if Creepers spawning is prevented.
     */
    public boolean isCreeperRemovalEnabled() {
        return this.creeperRemovalEnabled;
    }

    /**
     * Choose if Creepers should be removed from the game world.
     *
     * @param enabled should Creepers spawning be prevented.
     */
    public void setCreeperRemovalEnabled(boolean enabled) {
        this.creeperRemovalEnabled = enabled;

        this.plugin.getConfig().set("hostile-mob-spawning.creeper-removal", creeperRemovalEnabled);
        this.plugin.saveConfig();

        this.plugin.logInfo("Creeper removal " + (enabled ? "ENABLED" : "DISABLED"));
    }

    /**
     * Remove all Endermen from the game world.
     *
     * @return the number of Endermen removed.
     */
    public int removeAllEndermen() {
        int totalRemoved = 0;

        for (Chunk chunk : this.plugin.getWorld().getLoadedChunks()) {
            Entity[] entities = chunk.getEntities();

            for (Entity entity : entities) {
                if (entity instanceof Enderman) {
                    entity.remove();
                    ++totalRemoved;
                }
            }
        }

        this.plugin.logInfo("Removed " + totalRemoved + " Endermen from all loaded chunks");
        return totalRemoved;
    }

    /**
     * Remove all Creepers from the game world.
     *
     * @return the number of Creepers removed.
     */
    public int removeAllCreepers() {
        int totalRemoved = 0;

        for (Chunk chunk : this.plugin.getWorld().getLoadedChunks()) {
            Entity[] entities = chunk.getEntities();

            for (Entity entity : entities) {
                if (entity instanceof Creeper) {
                    entity.remove();
                    ++totalRemoved;
                }
            }
        }

        this.plugin.logInfo("Removed " + totalRemoved + " Creepers from all loaded chunks");
        return totalRemoved;
    }

    /**
     * Notify the log that this listener is shut down.
     */
    public void shutdown() {
        this.plugin.logInfo("SpawnRemovalListener shutdown");
    }
}
