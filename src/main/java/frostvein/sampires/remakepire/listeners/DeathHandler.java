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
import org.bukkit.configuration.file.FileConfiguration;
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
    public static final String PERMAKILLED_TAG = "perma_dead", PERMAKILL_PROCESSING_TAG = "PermaKilled", PERMADEATH_CHOSEN_TAG = "PermadeathChosen", PROMOTION_BAN_PENDING_TAG = "PromotionBanPending";
    private final FileConfiguration textConfig;
    private final boolean CUSTOM_DEATH_MESSAGES;

    /**
     * Create an instance of the Death Handler listener.
     *
     * @param plugin the host plugin object.
     */
    public DeathHandler(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.textConfig = this.plugin.getTextConfig();

        this.CUSTOM_DEATH_MESSAGES = this.textConfig.getBoolean("custom-global-announcements", false);
    }

    /**
     * Log the victim within the register of staked individuals.
     *
     * @param victim the player who has been killed.
     * @param killer the player who staked the victim.
     */
    public void registerWoodenStakeKill(Player victim, Player killer) {
        this.woodenStakeKills.put(victim.getUniqueId(), killer.getUniqueId());
        this.plugin.logInfo("DEBUG: Registered wooden stake kill - Victim: " + victim.getName() + ", Killer: " + killer.getName());
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

        if (wasVampire && player.getScoreboardTags().contains(PERMAKILL_PROCESSING_TAG)) {
            this.vampireManager.killPlayerPermanently(player);
            player.removeScoreboardTag(PERMAKILL_PROCESSING_TAG);

        } else if (wasHuman && player.getScoreboardTags().contains(PERMADEATH_CHOSEN_TAG)) {
            this.vampireManager.killPlayerPermanently(player);
            player.removeScoreboardTag(PERMADEATH_CHOSEN_TAG);

        } else if (wasVampire && player.getScoreboardTags().contains(PROMOTION_BAN_PENDING_TAG)) {
            this.vampireManager.applyPromotionBan(player);
            player.removeScoreboardTag(PROMOTION_BAN_PENDING_TAG);
        }

        if (wasHuman) {
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                try {
                    Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                    Objective deathObjective = mainScoreboard.getObjective("vsmp_death");

                    // Make sure the player doesn't respawn with an illegal number of lives
                    if (deathObjective != null) {
                        int currentDeaths = deathObjective.getScore(player.getName()).getScore();
                        int maxDeaths = this.plugin.getConfigManager().getHumanLifeCount();

                        if (currentDeaths > maxDeaths) {
                            deathObjective.getScore(player.getName()).setScore(maxDeaths);
                            this.plugin.logInfo("Capped death count for " + player.getName() + " at " + maxDeaths + " (was " + currentDeaths + ")");
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
            if (!event.isBedSpawn() && wasVampire) {
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
            int aliveHumans = 0, aliveVampires = 0;

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
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
        plugin.logInfo("ALL HUMANS ELIMINATED");

        int totalBeacons = plugin.getBeaconManager().getAllBeacons().size();
        int evilBeacons = plugin.getBeaconManager().getAllEvilBeacons().size();
        boolean allBeaconsDesecrated = totalBeacons > 0 && evilBeacons == totalBeacons;
        String townName = plugin.getConfigManager().getTownName();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle("§cThe last human has fallen.", "", 20, 100, 40);
            player.sendMessage("");
            player.sendMessage("§cThe last defender of humanity has fallen...");

            if (allBeaconsDesecrated) {
                player.sendMessage("§cDarkness reigns supreme over " + townName + ". You are free.");
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
        plugin.logInfo("ALL VAMPIRES ELIMINATED");

        int totalBeacons = plugin.getBeaconManager().getAllBeacons().size();
        int holyBeacons = plugin.getBeaconManager().getHolyBeacons().size();
        boolean allBeaconsHoly = totalBeacons > 0 && holyBeacons == totalBeacons;
        boolean anyPermanentlyCorrupted = plugin.getBeaconManager().getAllBeacons().stream().anyMatch((beacon) -> beacon.getState() == BeaconState.PERMANENTLY_DESECRATED);
        boolean trappedWhenPermanentlyCorrupted = plugin.getConfigManager().doCorruptedBeaconsTrapHumans();
        String townName = plugin.getConfigManager().getTownName();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle("§aThe last vampire has fallen.", "", 20, 100, 40);
            player.sendMessage("");
            player.sendMessage("§aThe last creature of darkness has fallen...");

            if (anyPermanentlyCorrupted) {
                player.sendMessage("§7But a beacon of light has been permanently corrupted.");

                if (trappedWhenPermanentlyCorrupted) {
                    player.sendMessage("§7The creatures of the night have been vanquished, but you are stuck in " + townName + ", forever.");
                } else {
                    player.sendMessage("§7The creatures of the night have been vanquished, but does freedom await you?");
                }
            } else if (allBeaconsHoly) {
                player.sendMessage("§aLight reigns supreme over " + townName + ". You are free.");
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
                this.plugin.logInfo("DEBUG: Retrieved tracked wooden stake killer: " + killer.getName() + " for victim: " + victim.getName());
            }
        }

        if (this.vampireManager.isHuman(victim)) {
            try {
                Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                Objective deathObjective = mainScoreboard.getObjective("vsmp_death");

                if (deathObjective != null) {
                    int currentDeaths = deathObjective.getScore(victim.getName()).getScore();
                    deathObjective.getScore(victim.getName()).setScore(currentDeaths + 1);
                    this.plugin.logInfo("Incremented death count for " + victim.getName() + " to " + (currentDeaths + 1));
                }
            } catch (Exception e) {
                this.plugin.getLogger().warning("Failed to increment death count for " + victim.getName() + ": " + e.getMessage());
            }
        }

        if (killer != null) {
            this.handlePvPDeath(victim, killer, event);
        } else if (this.vampireManager.isVampire(victim)) {
            victim.addScoreboardTag(PROMOTION_BAN_PENDING_TAG);
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
                this.plugin.logInfo("DEBUG: Using tracked last weapon: " + lastWeapon + " (current weapon broke/dropped)");
            }
        }

        this.plugin.logInfo("DEBUG: PvP Death - Victim: " + victim.getName() + ", CurrentWeapon: " + (weapon != null ? weapon.getType() : "null") + ", LastTrackedWeapon: " + lastWeapon + ", IsWoodenWeapon: " + killedWithWoodenWeapon + ", IsVampire: " + this.vampireManager.isVampire(victim) + ", IsStage1: " + this.vampireManager.isVampireStage1(victim) + ", VictimTags: " + victim.getScoreboardTags());
        this.lastWeaponUsed.remove(victim.getUniqueId());
        this.woodenStakeKills.remove(victim.getUniqueId());

        if (this.vampireManager.isVampire(victim)) {
            int woodenStakeThreshold = this.plugin.getConfigManager().getPermadeathMinimumStage();
            int victimStage = this.vampireManager.getVampireStage(victim);

            if (victimStage <= woodenStakeThreshold && killedWithWoodenWeapon) {
                victim.addScoreboardTag(PERMAKILL_PROCESSING_TAG);
                killer.sendMessage("§4You have permanently killed the vampire " + victim.getName() + "!");
                this.createVampireDeathEffects(victim.getLocation());
                this.plugin.logInfo("PERMA-KILL: Applied " + PERMAKILL_PROCESSING_TAG + " tag to " + victim.getName() + " (Stage " + victimStage + ", Threshold: " + woodenStakeThreshold + ")");

            } else {
                victim.addScoreboardTag(PROMOTION_BAN_PENDING_TAG);
                this.plugin.logInfo("PROMOTION BAN: Applied " + PROMOTION_BAN_PENDING_TAG + " tag to " + victim.getName() + " (Stage " + victimStage + ", Threshold: " + woodenStakeThreshold + ")");
            }
        }
    }

    /**
     * Alert the server that a player has been permanently killed.
     *
     * @param victim the player who was killed.
     */
    public void broadcastPermaKill(Player victim) {
        // Default messages for players who have avoided being placed on a team
        String vampireMessage = "A player has met their final death.", humanMessage = "A player has been slain.";

        // Broadcast a death message depending on what team the slain player was on
        if (vampireManager.isHuman(victim)) {
            if (CUSTOM_DEATH_MESSAGES) {
                humanMessage = this.textConfig.getString("combat-death-announcement.human-death-to-humans", "Custom death message failed to load: human-death-to-humans");
                vampireMessage = this.textConfig.getString("combat-death-announcement.human-death-to-vampires", "Custom death message failed to load: human-death-to-vampires");
            } else {
                humanMessage = "§cA shiver runs down your spine, as a human soul is torn from this realm...";
                vampireMessage = "§2You sense the fading light of a soul, unnoticed until its disappearance... One of your prey has been slain.";
            }
        } else if (vampireManager.isVampire(victim)) {
            if (CUSTOM_DEATH_MESSAGES) {
                humanMessage = this.textConfig.getString("combat-death-announcement.vampire-death-to-humans", "Custom death message failed to load: vampire-death-to-humans");
                vampireMessage = this.textConfig.getString("combat-death-announcement.vampire-death-to-vampires", "Custom death message failed to load: vampire-death-to-vampires");
            } else {
                humanMessage = "§aYou feel the realm has been purged of an evil spirit... Someone has successfully killed a vampire. Permanently.";
                vampireMessage = "§4You feel a dark soul ripped from its human coil, somebody has slain a member of your monstrous family...";
            }
        }

        // Send the curated death message to all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Don't send the message to the player who died
            if (!player.getUniqueId().equals(victim.getUniqueId())) {
                if (this.vampireManager.isVampire(player)) {
                    player.sendMessage(vampireMessage);
                } else {
                    player.sendMessage(humanMessage);
                }
            }
        }

        this.plugin.logInfo("PERMA-KILL: " + victim.getName() + " was permanently killed");
    }

    /**
     * Determine if the item is a wooden weapon.
     *
     * @param item the item to check.
     * @return {@code true} if the item is a wooden sword or axe.
     */
    private boolean isWoodenWeapon(ItemStack item) {
        if (item == null) {
            this.plugin.logInfo("DEBUG: Weapon is null");
            return false;

        } else {
            Material type = item.getType();
            boolean isWooden = type == Material.WOODEN_SWORD || type == Material.WOODEN_AXE;
            this.plugin.logInfo("DEBUG: Weapon type: " + type + ", Is wooden: " + isWooden);
            return isWooden;
        }
    }

    /**
     * Create the particle and sound effects around a vampire's final death.
     *
     * @param deathLocation the location where the vampire was staked.
     */
    public void createVampireDeathEffects(Location deathLocation) {
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
