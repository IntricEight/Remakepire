package frostvein.sampires.remakepire.managers;

import java.util.Arrays;
import java.util.List;
import org.bukkit.entity.Bogged;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Husk;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Stray;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zoglin;
import org.bukkit.entity.Zombie;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class MobTeamManager {
    private final RemakepirePlugin plugin;
    private BukkitTask mobTeamTask;
    private final List<Class<? extends Entity>> vampireMobTypes = Arrays.asList(Zombie.class, Drowned.class, Husk.class, Skeleton.class, WitherSkeleton.class, Bogged.class, Stray.class, Creeper.class, Spider.class, Witch.class, Wither.class, Zoglin.class, Phantom.class);
    // Controls how frequently mobs are assigned to the vampire team
    private final long ASSIGNMENT_INTERVALS = 200L;

    /**
     * Create an instance of the Mob Team manager.
     *
     * @param plugin the host plugin object.
     */
    public MobTeamManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.startMobTeamTask();
    }

    /**
     * Begin routinely assigning certain mob types to the vampire's team.
     */
    public void startMobTeamTask() {
        if (this.mobTeamTask != null) {
            this.mobTeamTask.cancel();
        }

        this.mobTeamTask = (new BukkitRunnable() {
            public void run() {
                MobTeamManager.this.assignMobsToVampireTeam();
            }
        }).runTaskTimer(this.plugin, 0L, ASSIGNMENT_INTERVALS);

        this.plugin.logInfo("MobTeamManager: Started mob team assignment task (every 10 seconds)");
    }

    /**
     * Stop assigning the chosen mob types to the vampire team.
     */
    public void stopMobTeamTask() {
        if (this.mobTeamTask != null) {
            this.mobTeamTask.cancel();
            this.mobTeamTask = null;
            this.plugin.logInfo("MobTeamManager: Stopped mob team assignment task");
        }
    }

    /**
     * Assign certain mob types to the vampire team, which stops them from being hostile toward higher level vampires.
     */
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
                            this.plugin.getLogger().warning("Failed to add mob " + entity.getType() + " to VampireCastTeam: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    /**
     * Determine if an entity is in the list of mob types that will join the vampires' team.
     *
     * @param entity the entity being checked.
     * @return {@code true} if the entity is going to align with the vampire players.
     */
    private boolean isTargetMob(Entity entity) {
        for(Class<? extends Entity> mobType : this.vampireMobTypes) {
            if (mobType.isInstance(entity)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Assign existing mobs to the vampire team.
     */
    public void assignMobsNow() {
        this.assignMobsToVampireTeam();
    }

    /**
     * Retrieve the number of mobs on the vampire team.
     *
     * @return The number of players and mobs on the vampire team.
     */
    public int getVampireMobCount() {
        Team vampireTeam = this.plugin.getVampireCastTeam();
        return vampireTeam == null ? 0 : vampireTeam.getSize();
    }

    /**
     * Remove all entities from the vampire team.
     */
    public void clearVampireMobs() {
        Team vampireTeam = this.plugin.getVampireCastTeam();

        if (vampireTeam == null) {
            this.plugin.getLogger().warning("MobTeamManager: VampireCastTeam not found for clearing!");
        } else {
            int removedCount = vampireTeam.getSize();

            for(String entry : vampireTeam.getEntries().toArray(new String[0])) {
                vampireTeam.removeEntry(entry);
            }

            this.plugin.logInfo("MobTeamManager: Removed " + removedCount + " entries from VampireCastTeam");
        }
    }

    /**
     * Stop adding mobs to the vampires' team before shutting down the manager.
     */
    public void shutdown() {
        this.stopMobTeamTask();
    }
}
