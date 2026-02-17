package frostvein.sampires.remakepire.managers;

import java.util.Arrays;
import java.util.List;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Husk;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Zombie;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class MobTeamManager {
    private final RemakepirePlugin plugin;
    private BukkitTask mobTeamTask;
    private final List<Class<? extends Entity>> vampireMobTypes = Arrays.asList(Zombie.class, Skeleton.class, Creeper.class, Drowned.class, Husk.class, Spider.class, Witch.class);

    public MobTeamManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.startMobTeamTask();
    }

    public void startMobTeamTask() {
        if (this.mobTeamTask != null) {
            this.mobTeamTask.cancel();
        }

        this.mobTeamTask = (new BukkitRunnable() {
            public void run() {
                MobTeamManager.this.assignMobsToVampireTeam();
            }
        }).runTaskTimer(this.plugin, 0L, 200L);
        this.plugin.getLogger().info("MobTeamManager: Started mob team assignment task (every 10 seconds)");
    }

    public void stopMobTeamTask() {
        if (this.mobTeamTask != null) {
            this.mobTeamTask.cancel();
            this.mobTeamTask = null;
            this.plugin.getLogger().info("MobTeamManager: Stopped mob team assignment task");
        }

    }

    private void assignMobsToVampireTeam() {
        Team vampireTeam = this.plugin.getVampireCastTeam();
        if (vampireTeam == null) {
            this.plugin.getLogger().warning("MobTeamManager: VampireCastTeam not found!");
        } else if (this.plugin.getWorld() == null) {
            this.plugin.getLogger().warning("MobTeamManager: World not found!");
        } else {
            int mobsAdded = 0;

            for(Entity entity : this.plugin.getWorld().getEntities()) {
                if (this.isTargetMob(entity)) {
                    String entityName = entity.getUniqueId().toString();
                    if (!vampireTeam.hasEntry(entityName)) {
                        try {
                            vampireTeam.addEntry(entityName);
                            ++mobsAdded;
                        } catch (Exception e) {
                            this.plugin.getLogger().warning("Failed to add mob " + String.valueOf(entity.getType()) + " to VampireCastTeam: " + e.getMessage());
                        }
                    }
                }
            }

        }
    }

    private boolean isTargetMob(Entity entity) {
        for(Class<? extends Entity> mobType : this.vampireMobTypes) {
            if (mobType.isInstance(entity)) {
                return true;
            }
        }

        return false;
    }

    public void assignMobsNow() {
        this.assignMobsToVampireTeam();
    }

    public int getVampireMobCount() {
        Team vampireTeam = this.plugin.getVampireCastTeam();
        return vampireTeam == null ? 0 : vampireTeam.getSize();
    }

    public void clearVampireMobs() {
        Team vampireTeam = this.plugin.getVampireCastTeam();
        if (vampireTeam == null) {
            this.plugin.getLogger().warning("MobTeamManager: VampireCastTeam not found for clearing!");
        } else {
            int removedCount = vampireTeam.getSize();

            for(String entry : (String[])vampireTeam.getEntries().toArray(new String[0])) {
                vampireTeam.removeEntry(entry);
            }

            this.plugin.getLogger().info("MobTeamManager: Removed " + removedCount + " entries from VampireCastTeam");
        }
    }

    public void shutdown() {
        this.stopMobTeamTask();
    }
}
