package frostvein.sampires.remakepire.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.beacons.BeaconSite.BeaconState;
import frostvein.sampires.remakepire.managers.VampireManager;

public class DeathHandler implements Listener {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final Map<UUID, Material> lastWeaponUsed = new HashMap<>();
    private final Map<UUID, UUID> woodenStakeKills = new HashMap<>();

    // TODO: Remove wooden axes from having special effects on vampires. It's really inconsistent where they are applied and where they are not, so it doesn't seem intentional.

    /**
     * Create an instance of the Death Handler listener.
     *
     * @param plugin the host plugin object.
     * @param vampireManager the manager for generic vampire traits.
     */
    public DeathHandler(RemakepirePlugin plugin, VampireManager vampireManager) {
        this.plugin = plugin;
        this.vampireManager = vampireManager;
    }

    /**
     * Log the victim within the register of staked individuals.
     *
     * @param victim the player who has been killed.
     * @param killer the player who staked the victim.
     */
    public void registerWoodenStakeKill(Player victim, Player killer) {
        this.woodenStakeKills.put(victim.getUniqueId(), killer.getUniqueId());
        this.plugin.getLogger().info("DEBUG: Registered wooden stake kill - Victim: " + victim.getName() + ", Killer: " + killer.getName());
    }

    /**
     * Update the player tags and update the death counters when players respawn.
     *
     * @param event a player has respawned.
     */
    @EventHandler
    public void onPlayerPostRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        boolean wasVampire = this.vampireManager.isVampire(player);
        boolean wasHuman = this.vampireManager.isHuman(player);

        if (this.vampireManager.isVampire(player) && player.getScoreboardTags().contains("PermaKilled")) {
            this.vampireManager.killVampirePermanently(player);
            player.removeScoreboardTag("PermaKilled");

        } else if (this.vampireManager.isHuman(player) && player.getScoreboardTags().contains("PermadeathChosen")) {
            this.vampireManager.killVampirePermanently(player);
            player.removeScoreboardTag("PermadeathChosen");

        } else if (this.vampireManager.isVampire(player) && player.getScoreboardTags().contains("PromotionBanPending")) {
            this.vampireManager.applyPromotionBan(player);
            player.removeScoreboardTag("PromotionBanPending");
        }

        if (this.vampireManager.isHuman(player)) {
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                try {
                    Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                    Objective deathObjective = mainScoreboard.getObjective("vsmp_death");

                    if (deathObjective != null) {
                        int currentDeaths = deathObjective.getScore(player.getName()).getScore();

                        if (currentDeaths > 5) {
                            deathObjective.getScore(player.getName()).setScore(5);
                            this.plugin.getLogger().info("Capped death count for " + player.getName() + " at 5 (was " + currentDeaths + ")");
                        }
                    }
                } catch (Exception e) {
                    this.plugin.getLogger().warning("Failed to cap death count for " + player.getName() + ": " + e.getMessage());
                }
            });
        }

        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            if (this.plugin.getBeaconMajorityManager() != null) {
                this.plugin.getBeaconMajorityManager().updateBeaconMajorityBonuses();
            }

            double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            player.setHealth(maxHealth);
            this.plugin.getLogger().fine(player.getName() + " respawned with " + maxHealth + " HP (full health)");
        }, 5L);

        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            if (!event.isBedSpawn() && this.vampireManager.isVampire(player)) {
                player.setRespawnLocation(this.plugin.getVampireRespawnLocation());
                player.teleport(this.plugin.getVampireRespawnLocation());
            }
        }, 7L);

        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> checkAndAnnounceTeamElimination(this.plugin, wasHuman, wasVampire), 10L);
    }

    /**
     * Determine if a team has been eliminated from the game.
     *
     * @param plugin the host plugin object.
     * @param affectedWasHuman {@code true} if the killed player was a human.
     * @param affectedWasVampire {@code true} if the killed player was a vampire.
     */
    public static void checkAndAnnounceTeamElimination(RemakepirePlugin plugin, boolean affectedWasHuman, boolean affectedWasVampire) {
        if (plugin.getSessionManager().isSessionActive()) {
            VampireManager vampireManager = plugin.getVampireManager();
            int aliveHumans = 0;
            int aliveVampires = 0;

            for(Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getGameMode() == GameMode.SURVIVAL) {
                    if (vampireManager.isHuman(onlinePlayer)) {
                        ++aliveHumans;
                    } else if (vampireManager.isVampire(onlinePlayer)) {
                        ++aliveVampires;
                    }
                }
            }

            if (aliveHumans == 0 && affectedWasHuman) {
                announceAllHumansDeadStatic(plugin);
            } else if (aliveVampires == 0 && affectedWasVampire) {
                announceHumansWinStatic(plugin);
            }
        }
    }

    /**
     * Announce the elimination of the human team.
     *
     * @param plugin the host plugin object.
     */
    private static void announceAllHumansDeadStatic(RemakepirePlugin plugin) {
        plugin.getLogger().info("ALL HUMANS ELIMINATED");

        int totalBeacons = plugin.getBeaconManager().getAllBeacons().size();
        int evilBeacons = plugin.getBeaconManager().getAllEvilBeacons().size();
        boolean allBeaconsDesecrated = totalBeacons > 0 && evilBeacons == totalBeacons;

        for(Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle("§cThe last human has fallen.", "", 20, 100, 40);
            player.sendMessage("");
            player.sendMessage("§cThe last defender of humanity has fallen...");

            if (allBeaconsDesecrated) {
                player.sendMessage("§cDarkness reigns supreme over Oakhurst. You are free.");
            } else {
                player.sendMessage("§cNow only the beacons lie between you and freedom.");
            }

            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.MASTER, 1.0F, 0.5F);
        }
    }

    /**
     * Announce the elimination of the vampire team.
     *
     * @param plugin the host plugin object.
     */
    private static void announceHumansWinStatic(RemakepirePlugin plugin) {
        plugin.getLogger().info("ALL VAMPIRES ELIMINATED");

        int totalBeacons = plugin.getBeaconManager().getAllBeacons().size();
        int holyBeacons = plugin.getBeaconManager().getHolyBeacons().size();
        boolean allBeaconsHoly = totalBeacons > 0 && holyBeacons == totalBeacons;
        boolean anyPermanentlyCorrupted = plugin.getBeaconManager().getAllBeacons().stream().anyMatch((beacon) -> beacon.getState() == BeaconState.PERMANENTLY_DESECRATED);

        for(Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle("§aThe last vampire has fallen.", "", 20, 100, 40);
            player.sendMessage("");
            player.sendMessage("§aThe last creature of darkness has fallen...");

            if (anyPermanentlyCorrupted) {
                player.sendMessage("§7But a beacon of light has been permanently corrupted.");
                player.sendMessage("§7The creatures of the night have been vanquished, but you are stuck in Oakhurst, forever.");

            } else if (allBeaconsHoly) {
                player.sendMessage("§aLight reigns supreme over Oakhurst. You are free.");
            } else {
                player.sendMessage("§7Now only the beacons lie between you and freedom.");
            }

            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 1.0F, 1.0F);
        }
    }

    /**
     * Record the last weapon used on a player by another player.
     *
     * @param event an entity being damaged by another entity.
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player attacker) {
            ItemStack weapon = attacker.getInventory().getItemInMainHand();

            if (weapon != null) {
                this.lastWeaponUsed.put(victim.getUniqueId(), weapon.getType());
            }
        }
    }

    /**
     * Handle the death counter and apply the promotion ban on a player's death.
     *
     * @param event a player dies.
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        UUID trackedKillerUUID = this.woodenStakeKills.remove(victim.getUniqueId());

        if (trackedKillerUUID != null && killer == null) {
            Player trackedKiller = this.plugin.getServer().getPlayer(trackedKillerUUID);

            if (trackedKiller != null) {
                killer = trackedKiller;
                this.plugin.getLogger().info("DEBUG: Retrieved tracked wooden stake killer: " + killer.getName() + " for victim: " + victim.getName());
            }
        }

        if (this.vampireManager.isHuman(victim)) {
            try {
                Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                Objective deathObjective = mainScoreboard.getObjective("vsmp_death");

                if (deathObjective != null) {
                    int currentDeaths = deathObjective.getScore(victim.getName()).getScore();
                    deathObjective.getScore(victim.getName()).setScore(currentDeaths + 1);
                    this.plugin.getLogger().info("Incremented death count for " + victim.getName() + " to " + (currentDeaths + 1));
                }
            } catch (Exception e) {
                this.plugin.getLogger().warning("Failed to increment death count for " + victim.getName() + ": " + e.getMessage());
            }
        }

        if (killer != null) {
            this.handlePvPDeath(victim, killer, event);
        } else if (this.vampireManager.isVampire(victim)) {
            victim.addScoreboardTag("PromotionBanPending");
        }
    }

    /**
     * Check if the weapon used a stake, and whether it would have a permanent effect on the victim.
     *
     * @param victim the player who was killed.
     * @param killer the player who killed the victim.
     * @param event a player dies.
     */
    private void handlePvPDeath(Player victim, Player killer, PlayerDeathEvent event) {
        ItemStack weapon = killer.getInventory().getItemInMainHand();
        boolean killedWithWoodenWeapon = this.isWoodenWeapon(weapon);
        Material lastWeapon = this.lastWeaponUsed.get(victim.getUniqueId());

        if (!killedWithWoodenWeapon && lastWeapon != null) {
            killedWithWoodenWeapon = lastWeapon == Material.WOODEN_SWORD || lastWeapon == Material.WOODEN_AXE;

            if (killedWithWoodenWeapon) {
                this.plugin.getLogger().info("DEBUG: Using tracked last weapon: " + String.valueOf(lastWeapon) + " (current weapon broke/dropped)");
            }
        }

        this.plugin.getLogger().info("DEBUG: PvP Death - Victim: " + victim.getName() + ", CurrentWeapon: " + String.valueOf(weapon != null ? weapon.getType() : "null") + ", LastTrackedWeapon: " + String.valueOf(lastWeapon) + ", IsWoodenWeapon: " + killedWithWoodenWeapon + ", IsVampire: " + this.vampireManager.isVampire(victim) + ", IsStage1: " + this.vampireManager.isVampireStage1(victim) + ", VictimTags: " + String.valueOf(victim.getScoreboardTags()));
        this.lastWeaponUsed.remove(victim.getUniqueId());
        this.woodenStakeKills.remove(victim.getUniqueId());

        if (this.vampireManager.isVampire(victim)) {
            int woodenStakeThreshold = this.plugin.getConfigManager().getPermadeathMinimumStage();
            int victimStage = this.vampireManager.getVampireStage(victim);

            if (victimStage <= woodenStakeThreshold && killedWithWoodenWeapon) {
                victim.addScoreboardTag("PermaKilled");
                killer.sendMessage("§4You have permanently killed the vampire " + victim.getName() + "!");
                this.createVampireDeathEffects(victim.getLocation());
                this.broadcastPermaKill(victim, killer);
                this.plugin.getLogger().info("PERMA-KILL: Applied PermaKilled tag to " + victim.getName() + " (Stage " + victimStage + ", Threshold: " + woodenStakeThreshold + ")");

            } else {
                victim.addScoreboardTag("PromotionBanPending");
                this.plugin.getLogger().info("PROMOTION BAN: Applied PromotionBanPending tag to " + victim.getName() + " (Stage " + victimStage + ", Threshold: " + woodenStakeThreshold + ")");
            }
        }
    }

    /**
     * Alert the server that a vampire has been permanently.
     *
     * @param victim the player who was killed.
     * @param killer the player who killed the victim.
     */
    private void broadcastPermaKill(Player victim, Player killer) {
        String vampireMessage = "§4You feel a dark soul ripped from its human coil, somebody has slayed a member of your monsterous family...";
        String humanMessage = "§aYou feel the realm has been purged of an evil spirit... Someone has successfully killed a vampire. Permanently.";

        for(Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getUniqueId().equals(victim.getUniqueId())) {
                if (this.vampireManager.isVampire(player)) {
                    player.sendMessage(vampireMessage);
                } else {
                    player.sendMessage(humanMessage);
                }
            }
        }

        String killerType = this.vampireManager.isVampire(killer) ? "vampire" : "human";
        this.plugin.getLogger().info("PERMA-KILL: " + victim.getName() + " was permanently killed by " + killerType + " " + killer.getName());
    }

    /**
     * Determine if the item is a wooden weapon.
     *
     * @param item the item to check.
     * @return {@code true} if the item is a wooden sword or axe.
     */
    private boolean isWoodenWeapon(ItemStack item) {
        if (item == null) {
            this.plugin.getLogger().info("DEBUG: Weapon is null");
            return false;

        } else {
            Material type = item.getType();
            boolean isWooden = type == Material.WOODEN_SWORD || type == Material.WOODEN_AXE;
            this.plugin.getLogger().info("DEBUG: Weapon type: " + String.valueOf(type) + ", Is wooden: " + isWooden);
            return isWooden;
        }
    }

    /**
     * Create the particle and sound effects around a vampire's final death.
     *
     * @param deathLocation the location where the vampire was staked.
     */
    private void createVampireDeathEffects(Location deathLocation) {
        if (deathLocation.getWorld() != null) {
            Location centerLoc = deathLocation.clone().add(0.0, 1.0, 0.0);
            deathLocation.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, centerLoc, 60, 1.5, 1.0, 1.5, 0.1);
            deathLocation.getWorld().spawnParticle(Particle.FLAME, centerLoc, 40, 1.2, 0.8, 1.2, 0.08);
            deathLocation.getWorld().spawnParticle(Particle.WHITE_ASH, centerLoc, 50, 1.0, 1.5, 1.0, 0.05);
            deathLocation.getWorld().spawnParticle(Particle.LARGE_SMOKE, centerLoc, 30, 1.8, 1.2, 1.8, 0.02);
            deathLocation.getWorld().playSound(deathLocation, Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.5F, 0.8F);

            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                deathLocation.getWorld().spawnParticle(Particle.WHITE_ASH, centerLoc, 30, 1.5, 2.0, 1.5, 0.03);
                deathLocation.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, centerLoc, 20, 1.0, 0.5, 1.0, 0.02);
                deathLocation.getWorld().playSound(deathLocation, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 1.0F, 1.2F);
            }, 20L);

            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                deathLocation.getWorld().spawnParticle(Particle.LARGE_SMOKE, centerLoc, 15, 2.0, 1.8, 2.0, 0.01);
                deathLocation.getWorld().spawnParticle(Particle.WHITE_ASH, centerLoc, 10, 1.8, 2.5, 1.8, 0.02);
            }, 40L);
        }
    }
}
