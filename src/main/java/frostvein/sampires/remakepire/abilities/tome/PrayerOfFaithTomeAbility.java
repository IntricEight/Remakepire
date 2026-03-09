package frostvein.sampires.remakepire.abilities.tome;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.VampireAbilityManager;

public class PrayerOfFaithTomeAbility extends TomeAbility {
    // Controls how long the ability takes to conclude (in seconds)
    private static final int PRAYER_DURATION = 60;
    // Controls how long the ability effects last (in ticks)
    private static final int ABSORPTION_DURATION = 12000;
    // Controls the intensity of the ability
    private static final int ABSORPTION_AMPLIFIER = 2;
    private static final Map<UUID, PrayerSession> activePrayers = new HashMap<>();

    /**
     * Create an instance of the Prayer of Faith tome ability.
     *
     * @param plugin the host plugin object.
     */
    public PrayerOfFaithTomeAbility(RemakepirePlugin plugin) {
        super(plugin, "PrayerOfFaith", new String[]{"You pray to whatever God you think might be listening,", "you must remain motionless for " + PRAYER_DURATION + " seconds after using this ability,", "after which you will receive absorption for " + (ABSORPTION_DURATION / 20 / 60) + " minutes."}, plugin.getConfigManager().getTomePrayerOfFaithCooldown());
    }

    protected boolean useAbility(Player player) {
        if (!this.canUse(player)) {
            this.sendCannotUseMessage(player, "Only humans can use tome abilities!");
            return false;

        } else if (activePrayers.containsKey(player.getUniqueId())) {
            this.sendCannotUseMessage(player, "you are already in prayer!");
            return false;

        } else {
            Location prayerLocation = player.getLocation().clone();
            PrayerSession session = new PrayerSession(player, prayerLocation);
            activePrayers.put(player.getUniqueId(), session);
            player.playSound(player.getLocation(), "minecraft:block.bell.use", 1.0F, 0.8F);
            this.sendSuccessMessage(player, "You begin your prayer... Remain motionless for " + PRAYER_DURATION + " seconds.");
            player.sendMessage("§7You can look around, but do not move from this spot.");
            session.startMonitoring();
            return true;
        }
    }

    /**
     * Cancel the player's prayer.
     *
     * @param player the player who cast the ability.
     */
    public static void cancelPrayer(Player player) {
        PrayerSession session = activePrayers.remove(player.getUniqueId());

        if (session != null) {
            session.cancel();
        }
    }

    /**
     * Determine if the player is casting the ability.
     *
     * @param player a player who might have cast the ability.
     * @return {@code true} if the {@code player} is in the list of active prayers.
     */
    public static boolean isPraying(Player player) {
        return activePrayers.containsKey(player.getUniqueId());
    }

    /**
     * Clean up any prayers from players who disconnected while praying.
     *
     * @param player the player who cast the ability.
     */
    public static void cleanupOfflinePlayer(Player player) {
        cancelPrayer(player);
    }

    private class PrayerSession {
        private final Player player;
        private final Location originalLocation;
        private final long startTime;
        private BukkitTask monitoringTask;
        private int secondsRemaining;

        /**
         * Create an instance of the prayer session.
         *
         * @param player the player who cast the ability.
         * @param originalLocation the player's original location.
         */
        public PrayerSession(Player player, Location originalLocation) {
            this.player = player;
            this.originalLocation = originalLocation;
            this.startTime = System.currentTimeMillis();
            this.secondsRemaining = PRAYER_DURATION;
        }

        /**
         * Monitor the player's condition while the prayer is ongoing.
         */
        public void startMonitoring() {
            this.monitoringTask = (new BukkitRunnable() {
                public void run() {
                    if (!PrayerSession.this.player.isOnline()) {
                        TomeAbility.clearCooldown(PrayerSession.this.player, PrayerOfFaithTomeAbility.this.getName());

                        this.cancel();

                        PrayerOfFaithTomeAbility.activePrayers.remove(PrayerSession.this.player.getUniqueId());

                    } else {
                        Location currentLocation = PrayerSession.this.player.getLocation();

                        if (PrayerSession.this.hasPlayerMoved(PrayerSession.this.originalLocation, currentLocation)) {
                            PrayerSession.this.player.sendMessage("§cYour prayer is interrupted. You moved from your position.");
                            PrayerSession.this.player.playSound(PrayerSession.this.player.getLocation(), "minecraft:block.glass.break", 1.0F, 0.5F);

                            TomeAbility.clearCooldown(PrayerSession.this.player, PrayerOfFaithTomeAbility.this.getName());
                            this.cancel();

                            PrayerOfFaithTomeAbility.activePrayers.remove(PrayerSession.this.player.getUniqueId());

                        } else {
                            --PrayerSession.this.secondsRemaining;

                            if (PrayerSession.this.secondsRemaining != 45 && PrayerSession.this.secondsRemaining != 30 && PrayerSession.this.secondsRemaining != 15) {
                                if (PrayerSession.this.secondsRemaining <= 10 && PrayerSession.this.secondsRemaining > 0) {
                                    PrayerOfFaithTomeAbility.this.plugin.getSessionManager().sendActionBar(PrayerSession.this.player, "§6Prayer: §e" + VampireAbilityManager.formatTime((long)PrayerSession.this.secondsRemaining) + "...");
                                }

                            } else {
                                PrayerOfFaithTomeAbility.this.plugin.getSessionManager().sendActionBar(PrayerSession.this.player, "§6Prayer: §e" + VampireAbilityManager.formatTime((long)PrayerSession.this.secondsRemaining) + " remaining...");
                            }

                            if (PrayerSession.this.secondsRemaining <= 0) {
                                PrayerSession.this.completePrayer();
                                this.cancel();
                                PrayerOfFaithTomeAbility.activePrayers.remove(PrayerSession.this.player.getUniqueId());
                            }
                        }
                    }
                }
            }).runTaskTimer(PrayerOfFaithTomeAbility.this.plugin, 0L, 20L);
        }

        /**
         * Determine if the player has moved since beginning the prayer.
         *
         * @param original the player's original location.
         * @param current the player's current location.
         * @return {@code true} if the {@code original} and {@code current} locations are not the same.
         */
        private boolean hasPlayerMoved(Location original, Location current) {
            return Math.abs(original.getX() - current.getX()) > 0.1 || Math.abs(original.getY() - current.getY()) > 0.1 || Math.abs(original.getZ() - current.getZ()) > 0.1;
        }

        /**
         * Provide the player with the ability's benefits.
         */
        private void completePrayer() {
            this.player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, ABSORPTION_DURATION, ABSORPTION_AMPLIFIER, false, false));
            this.player.playSound(this.player.getLocation(), "minecraft:block.beacon.activate", 1.0F, 1.5F);
            this.player.sendMessage("§7You feel divinely protected with absorption for " + (ABSORPTION_DURATION / 20 / 60) + " minutes.");
            PrayerOfFaithTomeAbility.this.plugin.getSessionManager().sendActionBar(this.player, "§a✦ Prayer Complete ✦");
        }

        /**
         * Cancel the ability cast.
         */
        public void cancel() {
            if (this.monitoringTask != null) {
                this.monitoringTask.cancel();
                this.monitoringTask = null;
            }
        }
    }
}
