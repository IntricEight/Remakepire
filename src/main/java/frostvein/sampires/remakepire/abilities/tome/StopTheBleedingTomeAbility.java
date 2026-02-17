package frostvein.sampires.remakepire.abilities.tome;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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
import frostvein.sampires.remakepire.managers.VampireAbilityManager;

public class StopTheBleedingTomeAbility extends TomeAbility {
    private static final int HEALING_DURATION_TICKS = 1200;
    private static final double PROXIMITY_DISTANCE = (double)2.0F;
    private static final int PARTICLE_INTERVAL_TICKS = 20;
    private final Map<UUID, HealingSession> activeHealingSessions = new HashMap();
    private static final String ACTIVE_TAG = "stopthebleeding_active";

    public StopTheBleedingTomeAbility(RemakepirePlugin plugin) {
        super(plugin, "StopTheBleeding", new String[]{"You learn how to mend the wounds of death itself.", "Crouch within 2 blocks of another player for 1 minute", "to heal one heart for them, restoring their vitality."}, plugin.getConfigManager().getTomeStopTheBleedingCooldown());
    }

    protected boolean useAbility(Player player) {
        if (!this.canUse(player)) {
            this.sendCannotUseMessage(player, "Only humans can use tome abilities!");
            return false;
        } else {
            UUID playerId = player.getUniqueId();
            if (this.activeHealingSessions.containsKey(playerId)) {
                this.cancelHealing(player, "You stop focusing on healing.");
                return false;
            } else if (player.getScoreboardTags().contains("stopthebleeding_used_session")) {
                this.sendCannotUseMessage(player, "You have already used Stop the Bleeding this session!");
                return false;
            } else {
                Player target = this.findNearestPlayer(player, (double)2.0F);
                if (target == null) {
                    target = player;
                }

                int currentDeaths = this.getDeathScore(target);
                if (currentDeaths <= 0) {
                    if (target.equals(player)) {
                        this.sendCannotUseMessage(player, "You have no deaths to heal!");
                    } else {
                        this.sendCannotUseMessage(player, target.getName() + " has no deaths to heal!");
                    }

                    return false;
                } else {
                    this.startHealing(player, target);
                    return false;
                }
            }
        }
    }

    private void startHealing(Player healer, Player target) {
        UUID healerId = healer.getUniqueId();
        healer.addScoreboardTag("stopthebleeding_active");
        HealingSession session = new HealingSession(healer, target);
        this.activeHealingSessions.put(healerId, session);
        session.start();
        this.sendSuccessMessage(healer, "You begin focusing your healing energy on " + target.getName() + "...");
        if (!healer.equals(target)) {
            target.sendMessage("§a" + healer.getName() + " is focusing healing energy on you. Stay close.");
        } else {
            healer.sendMessage("§aYou focus healing energy on yourself...");
        }

    }

    private void cancelHealing(Player healer, String reason) {
        UUID healerId = healer.getUniqueId();
        HealingSession session = (HealingSession)this.activeHealingSessions.remove(healerId);
        if (session != null) {
            session.cancel();
            healer.removeScoreboardTag("stopthebleeding_active");
            healer.sendMessage("§c" + reason);
            Player target = session.getTarget();
            if (target != null && !target.equals(healer) && target.isOnline()) {
                target.sendMessage("§c" + healer.getName() + " stopped healing you.");
            }
        }

    }

    private void completeHealing(Player healer, Player target) {
        UUID healerId = healer.getUniqueId();
        HealingSession session = (HealingSession)this.activeHealingSessions.remove(healerId);
        if (session != null) {
            session.cancel();
        }

        healer.removeScoreboardTag("stopthebleeding_active");
        int currentDeaths = this.getDeathScore(target);
        int newDeaths = Math.max(0, currentDeaths - 1);
        this.setDeathScore(target, newDeaths);
        this.updateMaxHealth(target);
        if (!healer.equals(target)) {
            target.sendMessage("§a" + healer.getName() + " has healed one of your wounds.");
            healer.sendMessage("§aYou have successfully healed one of " + target.getName() + "'s wounds.");
        } else {
            healer.sendMessage("§aYou have healed one of your own wounds.");
        }

        healer.playSound(healer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0F, 1.5F);
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0F, 1.2F);
        healer.addScoreboardTag("stopthebleeding_used_session");
    }

    private Player findNearestPlayer(Player player, double maxDistance) {
        Player nearest = null;
        double nearestDistance = maxDistance;

        for(Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(player) && other.getWorld().equals(player.getWorld())) {
                double distance = player.getLocation().distance(other.getLocation());
                if (distance <= maxDistance && distance < nearestDistance) {
                    nearest = other;
                    nearestDistance = distance;
                }
            }
        }

        return nearest;
    }

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

    private void setDeathScore(Player player, int score) {
        try {
            Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Objective deathObjective = mainScoreboard.getObjective("vsmp_death");

            if (deathObjective != null) {
                deathObjective.getScore(player.getName()).setScore(score);
                this.plugin.getLogger().info("Set death score for " + player.getName() + " to " + score);
            }
        } catch (Exception e) {
            this.plugin.getLogger().warning("Failed to set death score for " + player.getName() + ": " + e.getMessage());
        }

    }

    private void updateMaxHealth(Player player) {
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            try {
                if (this.plugin.getBeaconMajorityManager() != null) {
                    this.plugin.getBeaconMajorityManager().updateBeaconMajorityBonuses();
                }

                double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                this.plugin.getLogger().info("Updated max health for " + player.getName() + " to " + maxHealth);
            } catch (Exception e) {
                this.plugin.getLogger().warning("Failed to update max health for " + player.getName() + ": " + e.getMessage());
            }
        });
    }

    public void cleanup() {
        for(UUID healerId : (new HashMap<>(this.activeHealingSessions)).keySet()) {
            Player healer = Bukkit.getPlayer(healerId);
            if (healer != null) {
                this.cancelHealing(healer, "Plugin is shutting down.");
            }
        }

        this.activeHealingSessions.clear();
    }

    private class HealingSession {
        private final Player healer;
        private final Player target;
        private final UUID healerUUID;
        private final UUID targetUUID;
        private final boolean isSelfHeal;
        private int ticksRemaining;
        private BukkitTask task;
        private int particleCounter;

        public HealingSession(Player healer, Player target) {
            this.healer = healer;
            this.target = target;
            this.healerUUID = healer.getUniqueId();
            this.targetUUID = target.getUniqueId();
            this.isSelfHeal = this.healerUUID.equals(this.targetUUID);
            this.ticksRemaining = 1200;
            this.particleCounter = 0;
        }

        public Player getTarget() {
            return this.target;
        }

        public void start() {
            this.task = (new BukkitRunnable() {
                public void run() {
                    Player currentHealer = Bukkit.getPlayer(HealingSession.this.healerUUID);
                    Player currentTarget = Bukkit.getPlayer(HealingSession.this.targetUUID);
                    if (currentHealer != null && currentHealer.isOnline() && currentHealer.getScoreboardTags().contains("stopthebleeding_active")) {
                        if (currentTarget != null && currentTarget.isOnline()) {
                            if (!currentHealer.isSneaking()) {
                                StopTheBleedingTomeAbility.this.cancelHealing(currentHealer, "You stopped crouching - Your healing procedure is cancelled.");
                            } else if (HealingSession.this.isSelfHeal || currentHealer.getWorld().equals(currentTarget.getWorld()) && !(currentHealer.getLocation().distance(currentTarget.getLocation()) > (double)2.0F)) {
                                if (HealingSession.this.particleCounter % 20 == 0) {
                                    currentTarget.getWorld().spawnParticle(Particle.SCRAPE, currentTarget.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F), 3, 0.3, (double)0.5F, 0.3, 0.02);
                                }

                                ++HealingSession.this.particleCounter;
                                int secondsRemaining = HealingSession.this.ticksRemaining / 20;
                                String timeDisplay = VampireAbilityManager.formatTime((long)secondsRemaining);

                                if (HealingSession.this.isSelfHeal) {
                                    currentHealer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§aHealing yourself... §e" + timeDisplay + " §aremaining"));
                                } else {
                                    // Let the healer know how much time remains
                                    currentHealer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§aHealing " + currentTarget.getName() + "... §e" + timeDisplay + " §aremaining"));
                                    // Let the healing player know how much time remains
                                    currentTarget.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§aBeing healed by " + currentHealer.getName() + "... §e" + timeDisplay + " §aremaining"));
                                }

                                if (HealingSession.this.ticksRemaining % 200 == 0 && HealingSession.this.ticksRemaining > 0 && HealingSession.this.ticksRemaining < 1200) {
                                    currentHealer.sendMessage("§7[§aStop the Bleeding§7] §e" + secondsRemaining + " seconds remaining...");
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

        public void cancel() {
            if (this.task != null && !this.task.isCancelled()) {
                this.task.cancel();
            }
        }
    }
}
