package frostvein.sampires.remakepire.managers;

import io.papermc.paper.event.entity.WaterBottleSplashEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitTask;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class HolyWaterEffectManager implements Listener {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    // Controls how long vampiric abilities are disabled (in seconds)
    private static final int DISABLE_DURATION = 120;
    private final Map<UUID, BukkitTask> disabledVampires = new HashMap<>();

    /**
     * Create an instance of the Holy Water Effects manager.
     *
     * @param plugin the host plugin object.
     */
    public HolyWaterEffectManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("HolyWaterEffectManager initialized and event listener registered!");
    }

    /**
     * Apply the effects of holy water to players who are hit by the potion.
     *
     * @param event a splash potion hits a surface.
     */
    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        if (event instanceof WaterBottleSplashEvent waterEvent) {
            Location splashLocation = waterEvent.getPotion().getLocation();
            double splashRadius = 4.0;

            for(Entity nearby : splashLocation.getWorld().getNearbyEntities(splashLocation, splashRadius, splashRadius, splashRadius)) {
                if (nearby instanceof Player player) {
                    double distance = nearby.getLocation().distance(splashLocation);

                    if (distance <= splashRadius) {
                        this.processHolyWaterHit(player);
                    }
                }
            }

        } else {
            ThrownPotion potion = event.getPotion();
            ItemStack potionItem = potion.getItem();

            if (this.isWaterSplashBottle(potionItem)) {
                for(LivingEntity entity : event.getAffectedEntities()) {
                    this.processHolyWaterHit(entity);
                }
            }
        }
    }

    /**
     * Determine if the entity should be effected by holy water.
     *
     * @param entity the entity hit by the holy water.
     */
    private void processHolyWaterHit(LivingEntity entity) {
        if (entity instanceof Player player) {
            if (this.vampireManager.isVampire(player) && (this.vampireManager.isVampireStage2(player) || this.vampireManager.isVampireStage3(player))) {
                this.applyHolyWaterEffect(player);
            }
        }
    }

    /**
     * Determine if a potion is a splash bottle of water.
     *
     * @param item the item being checked.
     * @return {@code true} if the item does not have potion metadata or is an effectless potion.
     */
    private boolean isWaterSplashBottle(ItemStack item) {
        if (item == null) {
            return false;
        } else if (item.getType() != Material.SPLASH_POTION) {
            return false;
        } else if (!item.hasItemMeta()) {
            return true;
        } else if (!(item.getItemMeta() instanceof PotionMeta)) {
            return true;
        } else {
            PotionMeta potionMeta = (PotionMeta)item.getItemMeta();

            if (potionMeta.hasCustomEffects()) {
                return false;
            } else {
                PotionType baseType = potionMeta.getBasePotionType();

                if (baseType != null && baseType != PotionType.WATER) {
                    return baseType == PotionType.AWKWARD || baseType == PotionType.MUNDANE || baseType == PotionType.THICK;
                } else {
                    return true;
                }
            }
        }
    }

    /**
     * Disable the vampire's powers and regeneration and play the notification effect.
     *
     * @param vampire the player whose powers will be suppressed.
     */
    public void applyHolyWaterEffect(Player vampire) {
        UUID vampireId = vampire.getUniqueId();
        BukkitTask existingTask = this.disabledVampires.get(vampireId);

        if (existingTask != null && !existingTask.isCancelled()) {
            existingTask.cancel();
        }

        this.disabledVampires.put(vampireId, null);
        vampire.getWorld().playSound(vampire.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.MASTER, 1.0F, 1.0F);

        this.notifyVampireDisabled(vampire);
        BukkitTask enableTask = Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.removeHolyWaterEffect(vampire),  (long)(DISABLE_DURATION * 20));

        this.disabledVampires.put(vampireId, enableTask);
        this.plugin.getLogger().info("Applied holy water effect to vampire: " + vampire.getName());
    }

    /**
     * Remove the effects of holy water from the vampire.
     *
     * @param vampire the player who had been suppressed by holy water.
     */
    private void removeHolyWaterEffect(Player vampire) {
        this.removeHolyWaterEffect(vampire, true);
    }

    /**
     * Remove the effects of holy water from the vampire.
     *
     * @param vampire the player who had been suppressed by holy water.
     * @param notify {@code true} if the vampire should be notified that the effect has worn off.
     */
    public void removeHolyWaterEffect(Player vampire, boolean notify) {
        UUID vampireId = vampire.getUniqueId();
        BukkitTask task = this.disabledVampires.remove(vampireId);

        if (task != null && !task.isCancelled()) {
            task.cancel();
        }

        if (notify && vampire.isOnline()) {
            this.notifyVampireEnabled(vampire);
        }

        this.plugin.getLogger().info("Removed holy water effect from vampire: " + vampire.getName());
    }

    /**
     * Retrieve if the player currently has their abilities disabled by holy water.
     *
     * @param vampire the player being checked.
     * @return {@code true} if the vampire's abilities are disabled.
     */
    public boolean isAbilitiesDisabled(Player vampire) {
        return this.disabledVampires.containsKey(vampire.getUniqueId());
    }

    /**
     * Retrieve the time remaining until the holy water suppression wears off.
     *
     * @param vampire the player whose remaining time is being checked.
     * @return the milliseconds until the vampire's abilities are enabled.
     */
    public long getRemainingDisableTime(Player vampire) {
        BukkitTask task = this.disabledVampires.get(vampire.getUniqueId());
        return task == null ? 0L : Math.max(0L, (long)DISABLE_DURATION - System.currentTimeMillis() / 1000L % (long)DISABLE_DURATION);
    }

    /**
     * Inform the vampire that their abilities have been disabled by holy water.
     *
     * @param vampire the player whose abilities are disabled.
     */
    private void notifyVampireDisabled(Player vampire) {
        vampire.sendMessage("§cThe holy water sears your vampiric essence!");
        vampire.sendMessage("§cYour abilities and blood regeneration have been disabled for " + (int)(DISABLE_DURATION / 60.0) + " minutes.");

        vampire.playSound(vampire, Sound.ENTITY_GENERIC_HURT, SoundCategory.MASTER, 1.0F, 0.8F);
        vampire.playSound(vampire, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.MASTER, 1.0F, 1.2F);
        vampire.playSound(vampire, Sound.ENTITY_WITCH_HURT, SoundCategory.MASTER, 0.8F, 1.5F);
    }

    /**
     * Inform the vampire that the holy water has worn off and their abilities have been enabled.
     *
     * @param vampire the player whose abilities are enable.
     */
    private void notifyVampireEnabled(Player vampire) {
        vampire.sendMessage("§cYou feel your dark powers flowing through you once more.");
        vampire.playSound(vampire, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 0.5F, 0.8F);
    }

    /**
     * Retrieve the number of vampires affected by holy water.
     *
     * @return The number of disabled vampires.
     */
    public int getDisabledVampireCount() {
        return this.disabledVampires.size();
    }

    /**
     * Clear the holy water ability suppression from all vampires.
     */
    public void clearAllEffects() {
        for(Map.Entry<UUID, BukkitTask> entry : this.disabledVampires.entrySet()) {
            UUID vampireId = entry.getKey();
            BukkitTask task = entry.getValue();

            if (task != null && !task.isCancelled()) {
                task.cancel();
            }

            Player vampire = Bukkit.getPlayer(vampireId);

            if (vampire != null && vampire.isOnline()) {
                vampire.sendMessage("§aAn admin has restored your vampiric abilities.");
            }
        }

        int cleared = this.disabledVampires.size();
        this.disabledVampires.clear();
        this.plugin.getLogger().info("Cleared holy water effects from " + cleared + " vampires");
    }

    /**
     * Remove the holy water ability suppression from all vampires before shutting down the manager.
     */
    public void shutdown() {
        for(BukkitTask task : this.disabledVampires.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }

        this.disabledVampires.clear();
        this.plugin.getLogger().info("HolyWaterEffectManager shutdown complete");
    }
}
