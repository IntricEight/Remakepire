package frostvein.sampires.remakepire.listeners;

import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Chunk;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class EndermanRemovalListener implements Listener {
    private final RemakepirePlugin plugin;
    private final AtomicBoolean endermanRemovalEnabled;

    /**
     * Create an instance of the Enderman Removal listener.
     *
     * @param plugin the host plugin object.
     */
    public EndermanRemovalListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.endermanRemovalEnabled = new AtomicBoolean(true);
        plugin.logInfo("EndermanRemovalListener initialized - Enderman removal is ENABLED");
    }

    /**
     * Prevent an enderman from spawning in the world.
     *
     * @param event a non-player entity spawns.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (this.endermanRemovalEnabled.get()) {
            if (event.getEntityType() == EntityType.ENDERMAN) {
                event.setCancelled(true);
                this.plugin.logInfo("Prevented Enderman spawn at " + event.getLocation().getBlockX() + ", " + event.getLocation().getBlockY() + ", " + event.getLocation().getBlockZ() + " (Reason: " + String.valueOf(event.getSpawnReason()) + ")");
            }
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
        if (this.endermanRemovalEnabled.get()) {
            Chunk chunk = event.getChunk();
            Entity[] entities = chunk.getEntities();
            int removedCount = 0;

            for(Entity entity : entities) {
                if (entity instanceof Enderman) {
                    entity.remove();
                    ++removedCount;
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
        return this.endermanRemovalEnabled.get();
    }

    /**
     * Choose if Endermen should be removed from the game world.
     *
     * @param enabled should Endermen spawning be prevented.
     */
    public void setEndermanRemovalEnabled(boolean enabled) {
        this.endermanRemovalEnabled.set(enabled);
        this.plugin.logInfo("Enderman removal " + (enabled ? "ENABLED" : "DISABLED"));
    }

    /**
     * Remove all Endermen from the game world.
     *
     * @return the number of Endermen removed.
     */
    public int removeAllEndermen() {
        int totalRemoved = 0;

        for(Chunk chunk : this.plugin.getWorld().getLoadedChunks()) {
            Entity[] entities = chunk.getEntities();

            for(Entity entity : entities) {
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
     * Notify the log that this listener is shut down.
     */
    public void shutdown() {
        this.plugin.logInfo("EndermanRemovalListener shutdown");
    }
}
