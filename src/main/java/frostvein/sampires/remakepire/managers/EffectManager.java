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
    private final Map<UUID, Long> lastTrialOmenApplied = new HashMap();

    public EffectManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();

        this.SUN_WEAKNESS_SPEED_KEY = new NamespacedKey(plugin, "sun_weakness_speed");
        this.VAMPIRE_SAFE_FALL_KEY = new NamespacedKey(plugin, "vampire_safe_fall");
    }

    public void startEffectTask() {
        this.effectTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::applyVampireEffects, 20L, 20L);
    }

    public void stopEffectTask() {
        if (this.effectTask != null) {
            this.effectTask.cancel();
        }

    }

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

    private void applyWaterBreathing(Player player) {
        PotionEffect waterBreathing = new PotionEffect(PotionEffectType.WATER_BREATHING, 1000, 0, false, false, false);
        player.addPotionEffect(waterBreathing);
    }

    private void applyDaylightEffects(Player player) {
        if (this.vampireManager.isVampireStage1(player)) {
            this.removeSunWeaknessEffects(player);
            this.lastTrialOmenApplied.remove(player.getUniqueId());
        } else {
            if (this.canPlayerSeeSky(player) && this.isDaytime(player.getWorld()) && this.isClearWeather(player.getWorld())) {
                int stage = this.vampireManager.getVampireStage(player);
                long currentTime = System.currentTimeMillis();
                UUID playerUUID = player.getUniqueId();
                Long lastApplied = (Long)this.lastTrialOmenApplied.get(playerUUID);
                boolean shouldApplyTrialOmen = lastApplied == null || currentTime - lastApplied >= 300000L;
                if (shouldApplyTrialOmen && !player.hasPotionEffect(PotionEffectType.INVISIBILITY) && player.getGameMode() == GameMode.SURVIVAL) {
                    if (stage == 2) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.TRIAL_OMEN, 6000, 0, false, false, true));
                        this.lastTrialOmenApplied.put(playerUUID, currentTime);
                    } else if (stage == 3) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.TRIAL_OMEN, 6000, 1, false, false, true));
                        this.lastTrialOmenApplied.put(playerUUID, currentTime);
                    }
                }

                this.applySunWeaknessSpeed(player);
            } else {
                this.removeSunWeaknessEffects(player);

                if (player.hasPotionEffect(PotionEffectType.TRIAL_OMEN)) {
                    player.removePotionEffect(PotionEffectType.TRIAL_OMEN);
                }

                this.lastTrialOmenApplied.remove(player.getUniqueId());
            }
        }
    }

    private void applySunWeaknessSpeed(Player player) {
        AttributeInstance speedAttribute = player.getAttribute(Attribute.MOVEMENT_SPEED);

        if (speedAttribute != null) {
            boolean hasModifier = speedAttribute.getModifiers().stream().anyMatch(modifier -> SUN_WEAKNESS_SPEED_KEY.equals(modifier.getKey()));

            if (!hasModifier) {
                AttributeModifier speedReduction = new AttributeModifier(SUN_WEAKNESS_SPEED_KEY, -0.03, AttributeModifier.Operation.ADD_NUMBER);

                speedAttribute.addModifier(speedReduction);
            }

            // TODO: Decompilation Fixing, remove comments once success is confirmed

//            boolean hasModifier = speedAttribute.getModifiers().stream().anyMatch((modifier) -> modifier.getUniqueId().equals(SUN_WEAKNESS_SPEED_UUID));
//
//            if (!hasModifier) {
//                AttributeModifier speedReduction = new AttributeModifier(SUN_WEAKNESS_SPEED_UUID, "Sun Weakness Speed Reduction", -0.03, Operation.ADD_NUMBER);
//                speedAttribute.addModifier(speedReduction);
//            }
        }
    }

    private void removeSunWeaknessEffects(Player player) {
        AttributeInstance speedAttribute = player.getAttribute(Attribute.MOVEMENT_SPEED);

        if (speedAttribute != null) {
            speedAttribute.getModifiers().stream()
                    .filter(modifier -> SUN_WEAKNESS_SPEED_KEY.equals(modifier.getKey()))
                    .findFirst()
                    .ifPresent(speedAttribute::removeModifier);

            // TODO: Decompilation Fixing, remove comments once success is confirmed

//            AttributeModifier toRemove = (AttributeModifier)speedAttribute.getModifiers().stream().filter((modifier) -> modifier.getUniqueId().equals(SUN_WEAKNESS_SPEED_UUID)).findFirst().orElse((Object)null);
//
//            if (toRemove != null) {
//                speedAttribute.removeModifier(toRemove);
//            }
        }
    }

    private void applyVampireSafeFall(Player player) {
        AttributeInstance safeFallAttribute = player.getAttribute(Attribute.SAFE_FALL_DISTANCE);

        if (safeFallAttribute != null) {
            boolean hasModifier = safeFallAttribute.getModifiers().stream().anyMatch(modifier -> VAMPIRE_SAFE_FALL_KEY.equals(modifier.getKey()));

            if (!hasModifier) {
                AttributeModifier safeFallIncrease = new AttributeModifier(VAMPIRE_SAFE_FALL_KEY, 5.0, AttributeModifier.Operation.ADD_NUMBER);
                safeFallAttribute.addModifier(safeFallIncrease);
            }


            // TODO: Decompilation Fixing, remove comments once success is confirmed

//            boolean hasModifier = safeFallAttribute.getModifiers().stream().anyMatch((modifier) -> modifier.getUniqueId().equals(VAMPIRE_SAFE_FALL_UUID));
//
//            if (!hasModifier) {
//                AttributeModifier safeFallIncrease = new AttributeModifier(VAMPIRE_SAFE_FALL_UUID, "Vampire Safe Fall Distance", (double)5.0F, Operation.ADD_NUMBER);
//                safeFallAttribute.addModifier(safeFallIncrease);
//            }
        }
    }

    private void removeVampireSafeFall(Player player) {
        AttributeInstance safeFallAttribute = player.getAttribute(Attribute.SAFE_FALL_DISTANCE);

        if (safeFallAttribute != null) {

            safeFallAttribute.getModifiers().stream()
                    .filter(modifier -> VAMPIRE_SAFE_FALL_KEY.equals(modifier.getKey()))
                    .findFirst()
                    .ifPresent(safeFallAttribute::removeModifier);


            // TODO: Decompilation Fixing, remove comments once success is confirmed

//            AttributeModifier toRemove = (AttributeModifier)safeFallAttribute.getModifiers().stream().filter((modifier) -> modifier.getUniqueId().equals(VAMPIRE_SAFE_FALL_UUID)).findFirst().orElse((Object)null);
//
//            if (toRemove != null) {
//                safeFallAttribute.removeModifier(toRemove);
//            }
        }
    }

    private boolean canPlayerSeeSky(Player player) {
        Block highestBlock = player.getWorld().getHighestBlockAt(player.getLocation());
        return player.getLocation().getBlockY() >= highestBlock.getY();
    }

    private boolean isDaytime(World world) {
        long time = world.getTime();
        return time >= 0L && time < 12000L;
    }

    private boolean isClearWeather(World world) {
        return !world.hasStorm() && !world.isThundering();
    }

    private void applyEternalNightDarkness(Player player) {
        PotionEffect darkness = new PotionEffect(PotionEffectType.DARKNESS, 100, 0, false, false, true);
        player.addPotionEffect(darkness);
    }

    private void applyHumansFinalStandHealthReduction(Player player) {
        if (this.plugin.getSessionManager().isHumansFinalStandActive()) {
            AttributeInstance healthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
            if (healthAttribute != null && healthAttribute.getBaseValue() > (double)6.0F) {
                healthAttribute.setBaseValue((double)6.0F);
                if (player.getHealth() > (double)6.0F) {
                    player.setHealth((double)6.0F);
                }
            }
        }

    }

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
        this.plugin.getLogger().info("EffectManager shutdown - cleaned up vampire modifiers");
    }
}
