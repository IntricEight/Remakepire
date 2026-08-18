package frostvein.sampires.remakepire.abilities.tome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.SessionManager;
import frostvein.sampires.remakepire.managers.VampireAbilityManager;

public class StopTheBleedingTomeAbility extends TomeAbility {
    // Controls how long the ability takes to conclude (in ticks)
    private static final int HEALING_DURATION_TICKS = 1200;
    // Controls how far the players can be while healing
    private static final int PROXIMITY_DISTANCE = 2;
    // Controls how frequently particle effects appear (in ticks)
    private static final int PARTICLE_INTERVAL_TICKS = 20;
    private static final String ACTIVE_TAG = "stopthebleeding_active";
    private final Map<UUID, HealingSession> activeHealingSessions = new HashMap<>();
    private final List<BukkitTask> cooldownTasks = new ArrayList<>();

    /**
     * Create an instance of the Stop the Bleeding tome ability.
     *
     * @param plugin the host plugin object.
     */
    public StopTheBleedingTomeAbility(RemakepirePlugin plugin) {
        super(plugin, "StopTheBleeding", new String[]{"You learn how to mend the wounds of death itself.", "Crouch within " + PROXIMITY_DISTANCE + " blocks of another player for " + (HEALING_DURATION_TICKS / 20 / 60) + " minute", "to heal one heart for them, restoring their vitality."}, plugin.getConfigManager().getTomeStopTheBleedingCooldown());
    }

    protected boolean useAbility(Player player) {
        if (!this.canUse(player)) {
            this.sendCannotUseMessage(player, "Only humans can use tome abilities!");

        } else {
            final UUID playerId = player.getUniqueId();

            if (this.activeHealingSessions.containsKey(playerId)) {
                this.cancelHealing(player, "You stop focusing on healing.");

            } else {
                Player target = this.findNearestPlayer(player, PROXIMITY_DISTANCE);

                // If the player is not healing someone, let them heal themselves
                if (target == null) {
                    target = player;
                }

                // Prevent a player from being healed too frequently
                if (target.getScoreboardTags().contains(SessionManager.STOPTHEBLEEDING_USED_SESSION)) {
                    // Prevent the player from receiving both messages if they are healing themselves
                    if (target != player) {
                        this.sendCannotUseMessage(player, target.getName() + " has been healed too recently, it would be dangerous to try again so soon!");
                    }

                    this.sendCannotUseMessage(target, "Your body still aches from the previous healing session!");

                    return false;
                }

                if (this.getDeathScore(target) <= 0) {
                    if (target.equals(player)) {
                        this.sendCannotUseMessage(player, "You have no deaths to heal!");
                    } else {
                        this.sendCannotUseMessage(player, target.getName() + " has no deaths to heal!");
                    }
                } else {
                    this.startHealing(player, target);
                }
            }
        }

        return false;
    }

    /**
     * Begin the healing process on a player.
     *
     * @param healer the player doing the healing.
     * @param target the player being healed.
     */
    private void startHealing(Player healer, Player target) {
        final UUID healerId = healer.getUniqueId();
        healer.addScoreboardTag(ACTIVE_TAG);

        HealingSession session = new HealingSession(healer, target);
        this.activeHealingSessions.put(healerId, session);
        session.start();

        if (!healer.equals(target)) {
            this.sendSuccessMessage(healer, "You begin focusing your healing energy on " + target.getName() + "...");
            this.sendSuccessMessage(target, healer.getName() + " is focusing healing energy on you. Stay close.");
        } else {
            this.sendSuccessMessage(healer, "You focus healing energy on yourself...");
        }
    }

    /**
     * Cancel the healing process on a player.
     * @param healer the player doing the healing.
     * @param reason why the healing was stopped.
     */
    private void cancelHealing(Player healer, String reason) {
        final UUID healerId = healer.getUniqueId();
        HealingSession session = this.activeHealingSessions.remove(healerId);

        if (session != null) {
            session.cancel();
            healer.removeScoreboardTag(ACTIVE_TAG);
            healer.sendMessage(Component.text(reason, NamedTextColor.RED));
            Player target = session.getTarget();

            if (target != null && !target.equals(healer) && target.isOnline()) {
                target.sendMessage(Component.text(healer.getName() + " stopped healing you.", NamedTextColor.RED));
            }
        }

    }

    /**
     * Complete the healing process on a player.
     *
     * @param healer the player doing the healing.
     * @param target the player being healed.
     */
    private void completeHealing(Player healer, Player target) {
        final UUID healerId = healer.getUniqueId();
        HealingSession session = this.activeHealingSessions.remove(healerId);

        if (session != null) {
            session.cancel();
        }

        healer.removeScoreboardTag(ACTIVE_TAG);

        // Update the health and death counts of the player healed
        this.setDeathScore(target, Math.max(0, this.getDeathScore(target) - 1));
        this.updateMaxHealth(target);

        // Notify the players of the healing success
        if (!healer.equals(target)) {
            target.sendMessage(Component.text(healer.getName() + " has healed one of your wounds.", NamedTextColor.GREEN));
            healer.sendMessage(Component.text("You have successfully healed one of " + target.getName() + "'s wounds.", NamedTextColor.GREEN));
        } else {
            healer.sendMessage(Component.text("You have healed one of your own wounds.", NamedTextColor.GREEN));
        }

        healer.playSound(healer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0F, 1.5F);
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0F, 1.2F);

        // Begin a cooldown to allow the target to be healed again this session
        target.addScoreboardTag(SessionManager.STOPTHEBLEEDING_USED_SESSION);

        BukkitTask cooldownTask = Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (target.isOnline()) {
                target.removeScoreboardTag(SessionManager.STOPTHEBLEEDING_USED_SESSION);
            }

        }, (long)cooldownSeconds * 20L);

        this.cooldownTasks.add(cooldownTask);
    }

    /**
     * Find the nearest player to the caster.
     *
     * @param player the player who cast the ability.
     * @param maxDistance the maximum distance to search.
     * @return The {@code Player} who is closest to the {@code player}.
     */
    private Player findNearestPlayer(Player player, double maxDistance) {
        Player nearest = null;
        double nearestDistance = maxDistance, distance;

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(player) && other.getWorld().equals(player.getWorld())) {
                distance = player.getLocation().distance(other.getLocation());

                if (distance <= maxDistance && distance < nearestDistance) {
                    nearest = other;
                    nearestDistance = distance;
                }
            }
        }

        return nearest;
    }

    /**
     * Retrieve the number of times the player has died.
     *
     * @param player the player whose death counter we are retrieving.
     * @return The number of deaths {@code player} has experienced.
     */
    private int getDeathScore(Player player) {
        try {
            Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Objective deathObjective = mainScoreboard.getObjective("vsmp_death");

            if (deathObjective != null) {
                return deathObjective.getScore(player.getName()).getScore();
            }
        } catch (Exception e) {
            this.plugin.getLogger().warning("Failed to get death score for " + player.getName() + ": " + e.getMessage());
        }

        return 0;
    }

    /**
     * Update the number of times the player has died.
     *
     * @param player the player whose death counter we are updating.
     * @param score the number that the death counter will be updated to.
     */
    private void setDeathScore(Player player, int score) {
        try {
            Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Objective deathObjective = mainScoreboard.getObjective("vsmp_death");

            if (deathObjective != null) {
                deathObjective.getScore(player.getName()).setScore(score);
                this.plugin.logInfo("Set death score for " + player.getName() + " to " + score);
            }
        } catch (Exception e) {
            this.plugin.getLogger().warning("Failed to set death score for " + player.getName() + ": " + e.getMessage());
        }

    }

    /**
     * Update the maximum hearts the player can regenerate.
     *
     * @param player the player whose health is being updated.
     */
    private void updateMaxHealth(Player player) {
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            try {
                if (this.plugin.getBeaconMajorityManager() != null) {
                    this.plugin.getBeaconMajorityManager().updateBeaconMajorityBonuses();
                }

                this.plugin.logInfo("Updated max health for " + player.getName() + " to " + player.getAttribute(Attribute.MAX_HEALTH).getValue());
            } catch (Exception e) {
                this.plugin.getLogger().warning("Failed to update max health for " + player.getName() + ": " + e.getMessage());
            }
        });
    }

    /**
     * Clean up any healing and cooldown sessions when the plugin is turned off.
     */
    public void cleanup() {
        // Cancel the active healing sessions
        for (UUID healerId : (new HashMap<>(this.activeHealingSessions)).keySet()) {
            Player healer = Bukkit.getPlayer(healerId);

            if (healer != null) {
                this.cancelHealing(healer, "Plugin is shutting down.");
            }
        }

        this.activeHealingSessions.clear();

        // Cancel the cooldown removal sessions
        for (BukkitTask task : this.cooldownTasks) {
            task.cancel();
        }

        this.cooldownTasks.clear();
    }

    private class HealingSession {
        private final Player healer, target;
        private final UUID healerUUID, targetUUID;
        private final boolean isSelfHeal;
        private int ticksRemaining, particleCounter;
        private BukkitTask task;

        /**
         * Create an instance of the healing session.
         *
         * @param healer the player doing the healing.
         * @param target the player being healed.
         */
        public HealingSession(Player healer, Player target) {
            this.healer = healer;
            this.target = target;
            this.healerUUID = healer.getUniqueId();
            this.targetUUID = target.getUniqueId();
            this.isSelfHeal = this.healerUUID.equals(this.targetUUID);
            this.ticksRemaining = HEALING_DURATION_TICKS;
            this.particleCounter = 0;
        }

        /**
         * Retrieve the player being healed.
         *
         * @return The {@code Player} who is being healed.
         */
        public Player getTarget() {
            return this.target;
        }

        /**
         * Monitor the process conditions while the healing is ongoing, and generate the visual effects.
         */
        public void start() {
            this.task = (new BukkitRunnable() {
                public void run() {
                    Player currentHealer = Bukkit.getPlayer(HealingSession.this.healerUUID);
                    Player currentTarget = Bukkit.getPlayer(HealingSession.this.targetUUID);

                    if (currentHealer != null && currentHealer.isOnline() && currentHealer.getScoreboardTags().contains(ACTIVE_TAG)) {
                        if (currentTarget != null && currentTarget.isOnline()) {
                            if (!currentHealer.isSneaking()) {
                                StopTheBleedingTomeAbility.this.cancelHealing(currentHealer, "You stopped crouching - Your healing procedure is cancelled.");

                            } else if (HealingSession.this.isSelfHeal || currentHealer.getWorld().equals(currentTarget.getWorld()) && !(currentHealer.getLocation().distance(currentTarget.getLocation()) > PROXIMITY_DISTANCE)) {
                                if (HealingSession.this.particleCounter % PARTICLE_INTERVAL_TICKS == 0) {
                                    currentTarget.getWorld().spawnParticle(Particle.SCRAPE, currentTarget.getLocation().add(0.0, 1.0, 0.0), 3, 0.3, 0.5, 0.3, 0.02);
                                }

                                ++HealingSession.this.particleCounter;
                                int secondsRemaining = HealingSession.this.ticksRemaining / 20;
                                String timeDisplay = VampireAbilityManager.formatTime(secondsRemaining);

                                // Let the player(s) involved know what's going on
                                if (HealingSession.this.isSelfHeal) {
                                    // Let the player know they are healing themselves successfully
                                    currentHealer.sendActionBar(
                                            Component.text("Healing yourself... ", NamedTextColor.GREEN)
                                                    .append(Component.text(timeDisplay, NamedTextColor.YELLOW))
                                                    .append(Component.text(" remaining", NamedTextColor.GREEN))
                                    );
                                } else {
                                    // Let the healer know how much time remains
                                    currentHealer.sendActionBar(
                                            Component.text("Healing " + currentTarget.getName() + "... ", NamedTextColor.GREEN)
                                                    .append(Component.text(timeDisplay, NamedTextColor.YELLOW))
                                                    .append(Component.text(" remaining", NamedTextColor.GREEN))
                                    );

                                    // Let the receiving player know how much time remains
                                    currentTarget.sendActionBar(
                                            Component.text("Being healed by " + currentHealer.getName() + "... ", NamedTextColor.GREEN)
                                                    .append(Component.text(timeDisplay, NamedTextColor.YELLOW))
                                                    .append(Component.text(" remaining", NamedTextColor.GREEN))
                                    );
                                }

                                if (HealingSession.this.ticksRemaining % 200 == 0 && HealingSession.this.ticksRemaining > 0 && HealingSession.this.ticksRemaining < HEALING_DURATION_TICKS) {
                                    currentHealer.sendMessage(Component.text("[", NamedTextColor.GRAY)
                                            .append(Component.text("Stop the Bleeding", NamedTextColor.GREEN))
                                            .append(Component.text("] ", NamedTextColor.GRAY))
                                            .append(Component.text(secondsRemaining + " seconds remaining...", NamedTextColor.YELLOW))
                                    );
                                }

                                --HealingSession.this.ticksRemaining;
                                if (HealingSession.this.ticksRemaining <= 0) {
                                    StopTheBleedingTomeAbility.this.completeHealing(currentHealer, currentTarget);
                                }

                            } else {
                                StopTheBleedingTomeAbility.this.cancelHealing(currentHealer, "You moved too far away from " + currentTarget.getName() + "!");
                            }
                        } else {
                            StopTheBleedingTomeAbility.this.cancelHealing(currentHealer, "Target player logged off.");
                        }
                    } else {
                        if (currentHealer != null) {
                            StopTheBleedingTomeAbility.this.cancelHealing(currentHealer, "Healing interrupted.");
                        } else {
                            this.cancel();
                            StopTheBleedingTomeAbility.this.activeHealingSessions.remove(HealingSession.this.healerUUID);
                        }
                    }
                }
            }).runTaskTimer(StopTheBleedingTomeAbility.this.plugin, 0L, 1L);
        }

        /**
         * Cancel the ability cast.
         */
        public void cancel() {
            if (this.task != null && !this.task.isCancelled()) {
                this.task.cancel();
            }
        }
    }
}
