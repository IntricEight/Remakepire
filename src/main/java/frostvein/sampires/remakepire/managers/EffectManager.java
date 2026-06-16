package frostvein.sampires.remakepire.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class EffectManager {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private BukkitTask effectTask;
    private final NamespacedKey SUN_WEAKNESS_SPEED_KEY;
    private final NamespacedKey VAMPIRE_SAFE_FALL_KEY;
    private final Map<UUID, Long> lastTrialOmenApplied = new HashMap<>();
    // Controls how much health the vampires have during the holy beacon last stand
    private final double LAST_STAND_VAMPIRE_HEALTH = 6.0;

    /**
     * Create an instance of the Vampire Effects manager.
     *
     * @param plugin the host plugin object.
     */
    public EffectManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();

        this.SUN_WEAKNESS_SPEED_KEY = new NamespacedKey(plugin, "sun_weakness_speed");
        this.VAMPIRE_SAFE_FALL_KEY = new NamespacedKey(plugin, "vampire_safe_fall");
    }

    /**
     * Begin regularly applying the passive effects on players from their team.
     */
    public void startEffectTask() {
        this.effectTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::applyVampireEffects, 20L, 20L);
    }

    /**
     * Stop applying passive effects on players from their team.
     */
    public void stopEffectTask() {
        if (this.effectTask != null) {
            this.effectTask.cancel();
        }
    }

    /**
     * Apply the passive effects on players based on their team and the state of the game. This includes final stand and day cycle effects.
     */
    private void applyVampireEffects() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            if (this.vampireManager.isVampire(player)) {
                this.applyWaterBreathing(player);
                this.applyDaylightEffects(player);
                this.applyVampireSafeFall(player);
                this.applyHumansFinalStandHealthReduction(player);

            } else {
                this.removeVampireSafeFall(player);

                if (this.vampireManager.isHuman(player) && this.plugin.getSessionManager().isVampiresEternalNightActive() && player.getGameMode() == GameMode.SURVIVAL) {
                    this.applyEternalNightDarkness(player);
                }
            }
        }
    }

    /**
     * Apply water breathing to the player.
     *
     * @param player the player gaining the status.
     */
    private void applyWaterBreathing(Player player) {
        PotionEffect waterBreathing = new PotionEffect(PotionEffectType.WATER_BREATHING, 1000, 0, false, false, false);
        player.addPotionEffect(waterBreathing);
    }

    /**
     * Apply the effects of sun weakness to the player if they are a higher vampire.
     *
     * @param player the player gaining the status.
     */
    private void applyDaylightEffects(Player player) {
        if (this.vampireManager.isVampireStage1(player)) {
            this.removeSunWeaknessEffects(player);
            this.lastTrialOmenApplied.remove(player.getUniqueId());

        } else {
            if (this.canPlayerSeeSky(player) && this.isDaytime(player.getWorld()) && this.isClearWeather(player.getWorld())) {
                int stage = this.vampireManager.getVampireStage(player);
                long currentTime = System.currentTimeMillis();
                UUID playerUUID = player.getUniqueId();
                Long lastApplied = this.lastTrialOmenApplied.get(playerUUID);
                boolean shouldApplyTrialOmen = lastApplied == null || currentTime - lastApplied >= 300000L;

                // Apply the visual indicator of sun weakness
                if (shouldApplyTrialOmen && !player.hasPotionEffect(PotionEffectType.INVISIBILITY) && player.getGameMode() == GameMode.SURVIVAL) {
                    if (stage == 2) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.RAID_OMEN, 6000, 0, false, false, true));
                        this.lastTrialOmenApplied.put(playerUUID, currentTime);
                    } else if (stage == 3) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.RAID_OMEN, 6000, 1, false, false, true));
                        this.lastTrialOmenApplied.put(playerUUID, currentTime);
                    }
                }

                this.applySunWeaknessSpeed(player);

            } else {
                this.removeSunWeaknessEffects(player);

                if (player.hasPotionEffect(PotionEffectType.RAID_OMEN)) {
                    player.removePotionEffect(PotionEffectType.RAID_OMEN);
                }

                this.lastTrialOmenApplied.remove(player.getUniqueId());
            }
        }
    }

    /**
     * Slow the vampire down slightly.
     *
     * @param player the vampire gaining the status.
     */
    private void applySunWeaknessSpeed(Player player) {
        AttributeInstance speedAttribute = player.getAttribute(Attribute.MOVEMENT_SPEED);

        if (speedAttribute != null) {
            boolean hasModifier = speedAttribute.getModifiers().stream().anyMatch(modifier -> SUN_WEAKNESS_SPEED_KEY.equals(modifier.getKey()));

            if (!hasModifier) {
                AttributeModifier speedReduction = new AttributeModifier(SUN_WEAKNESS_SPEED_KEY, -0.03, AttributeModifier.Operation.ADD_NUMBER);

                speedAttribute.addModifier(speedReduction);
            }
        }
    }

    /**
     * Return the vampire to their normal speed.
     *
     * @param player the vampire losing the status.
     */
    private void removeSunWeaknessEffects(Player player) {
        AttributeInstance speedAttribute = player.getAttribute(Attribute.MOVEMENT_SPEED);

        if (speedAttribute != null) {
            speedAttribute.getModifiers().stream()
                    .filter(modifier -> SUN_WEAKNESS_SPEED_KEY.equals(modifier.getKey()))
                    .findFirst()
                    .ifPresent(speedAttribute::removeModifier);
        }
    }

    /**
     * Increase the distance the vampire can fall before taking fall damage.
     *
     * @param player the vampire gaining the status.
     */
    private void applyVampireSafeFall(Player player) {
        AttributeInstance safeFallAttribute = player.getAttribute(Attribute.SAFE_FALL_DISTANCE);

        if (safeFallAttribute != null) {
            boolean hasModifier = safeFallAttribute.getModifiers().stream().anyMatch(modifier -> VAMPIRE_SAFE_FALL_KEY.equals(modifier.getKey()));

            if (!hasModifier) {
                AttributeModifier safeFallIncrease = new AttributeModifier(VAMPIRE_SAFE_FALL_KEY, 5.0, AttributeModifier.Operation.ADD_NUMBER);
                safeFallAttribute.addModifier(safeFallIncrease);
            }
        }
    }

    /**
     * Remove the vampire's fall distance resistance.
     *
     * @param player the vampire losing the status.
     */
    private void removeVampireSafeFall(Player player) {
        AttributeInstance safeFallAttribute = player.getAttribute(Attribute.SAFE_FALL_DISTANCE);

        if (safeFallAttribute != null) {
            safeFallAttribute.getModifiers().stream()
                    .filter(modifier -> VAMPIRE_SAFE_FALL_KEY.equals(modifier.getKey()))
                    .findFirst()
                    .ifPresent(safeFallAttribute::removeModifier);
        }
    }

    /**
     * Determine if the player has open sky access above their head.
     *
     * @param player the player who is being checked.
     * @return {@code true} if there are no blocks above the player.
     */
    public boolean canPlayerSeeSky(Player player) {
        Block highestBlock = player.getWorld().getHighestBlockAt(player.getLocation());
        return player.getLocation().getBlockY() >= highestBlock.getY();
    }

    /**
     * Determine if it is day time.
     *
     * @param world the world hosting the plugin interactions.
     * @return {@code true} if it is daytime.
     */
    public boolean isDaytime(World world) {
        long time = world.getTime();
        return time >= 0L && time < 12000L;
    }

    /**
     * Determine if the weather is clear skies.
     *
     * @param world the world hosting the plugin interactions.
     * @return {@code true} if the weather is clear.
     */
    public boolean isClearWeather(World world) {
        return !world.hasStorm() && !world.isThundering();
    }

    /**
     * Blind the player from the desecrated beacon's final stand, if they are a living human.
     *
     * @param player the human gaining the status.
     */
    public void applyEternalNightDarkness(Player player) {
        if (this.vampireManager.isHuman(player) && player.getGameMode() == GameMode.SURVIVAL && this.plugin.getSessionManager().isVampiresEternalNightActive()) {
            PotionEffect darkness = new PotionEffect(PotionEffectType.DARKNESS, -1, 0, false, false, true);
            player.addPotionEffect(darkness);
        }
    }

    /**
     * Remove the blinding effects of vampire total beacon control.
     *
     * @param player the player losing the effect.
     */
    public void removeEternalNightDarkness(Player player) {
        player.removePotionEffect(PotionEffectType.DARKNESS);
    }

    /**
     * Reduce the maximum health of vampires if the holy control final stand is active.
     *
     * @param player the vampire losing their hearts.
     */
    public void applyHumansFinalStandHealthReduction(Player player) {
        if (this.vampireManager.isVampire(player) && player.getGameMode() == GameMode.SURVIVAL && this.plugin.getSessionManager().isHumansFinalStandActive()) {
            AttributeInstance healthAttribute = player.getAttribute(Attribute.MAX_HEALTH);

            if (healthAttribute != null && healthAttribute.getBaseValue() > LAST_STAND_VAMPIRE_HEALTH) {
                healthAttribute.setBaseValue(LAST_STAND_VAMPIRE_HEALTH);

                if (player.getHealth() > LAST_STAND_VAMPIRE_HEALTH) {
                    player.setHealth(LAST_STAND_VAMPIRE_HEALTH);
                }
            }
        }
    }

    /**
     * Restore the maximum health of vampires.
     *
     * @param player the vampire restoring their hearts.
     */
    public void removeHumansFinalStandHealthReduction(Player player) {
        if (player.getGameMode() == GameMode.SURVIVAL) {
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20.0);
        }
    }

    /**
     * Apply the passive effects to the player when they join the game.
     *
     * @param player the player joining the game.
     */
    public void applyJoinEffects(Player player) {
        if (this.vampireManager.isVampire(player)) {
            this.applyWaterBreathing(player);
            this.applyVampireSafeFall(player);
            this.applyHumansFinalStandHealthReduction(player);
            this.plugin.getVampireTurningManager().applyLuckEffectIfEnabled(player);
            this.removeSunWeaknessEffects(player);

        } else {
            this.removeVampireSafeFall(player);

            if (this.vampireManager.isHuman(player) && this.plugin.getSessionManager().isVampiresEternalNightActive() && player.getGameMode() == GameMode.SURVIVAL) {
                this.applyEternalNightDarkness(player);
            }
        }

    }

    /**
     * Clean up the passive effects before shutting down the manager.
     */
    public void shutdown() {
        if (this.effectTask != null) {
            this.effectTask.cancel();
            this.effectTask = null;
        }

        for(Player player : Bukkit.getOnlinePlayers()) {
            this.removeSunWeaknessEffects(player);
            this.removeVampireSafeFall(player);
        }

        this.lastTrialOmenApplied.clear();
        this.plugin.logInfo("EffectManager shutdown - cleaned up vampire modifiers");
    }
}
