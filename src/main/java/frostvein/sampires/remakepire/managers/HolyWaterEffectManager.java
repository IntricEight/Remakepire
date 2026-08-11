package frostvein.sampires.remakepire.managers;

import io.papermc.paper.event.entity.WaterBottleSplashEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.utils.ItemTypeChecking;

public class HolyWaterEffectManager implements Listener {
    private final RemakepirePlugin plugin;
    private final ConfigManager configManager;
    private final Map<UUID, BukkitTask> disabledVampires = new HashMap<>();

    /**
     * Create an instance of the Holy Water Effects manager.
     *
     * @param plugin the host plugin object.
     */
    public HolyWaterEffectManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();

        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.logInfo("HolyWaterEffectManager initialized and event listener registered!");
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
            final double splashRadius = 4.0;

            for (Entity nearby : splashLocation.getWorld().getNearbyEntities(splashLocation, splashRadius, splashRadius, splashRadius)) {
                if (nearby instanceof Player player) {
                    final double distance = nearby.getLocation().distance(splashLocation);

                    if (distance <= splashRadius) {
                        this.processHolyWaterHit(player);
                    }
                }
            }

        } else {
            ThrownPotion potion = event.getPotion();
            ItemStack potionItem = potion.getItem();

            if (ItemTypeChecking.isHolyWater(potionItem)) {
                for (LivingEntity entity : event.getAffectedEntities()) {
                    this.processHolyWaterHit(entity);
                }
            }
        }
    }

    /**
     * Prevent higher vampires from throwing bottles of holy water.
     *
     * @param event a projectile is released or thrown.
     */
    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof ThrownPotion potion) {
            if (potion.getShooter() instanceof Player player) {
                if (this.plugin.getVampireManager().isVampireStage2OrHigher(player)) {
                    ItemStack potionItem = potion.getItem();

                    if (ItemTypeChecking.isHolyWater(potionItem)) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    /**
     * Prevent higher vampires from throwing bottles of holy water.
     *
     * @param event a player interacts with an object.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        try {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (this.plugin.getVampireManager().isVampireStage2OrHigher(player)) {
                    // Check if the player is attempting to throw a bottle of holy water
                    if (ItemTypeChecking.isHolyWater(event.getItem())) {
                        event.setCancelled(true);
                        player.sendMessage(Component.text("The Holy Water burns your hand as you try to throw it! You feel unable to bring yourself to use this item...", NamedTextColor.RED));
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Determine if the entity should be effected by holy water.
     *
     * @param entity the entity hit by the holy water.
     */
    private void processHolyWaterHit(LivingEntity entity) {
        if (entity instanceof Player player) {
            if (player.getGameMode() != GameMode.SPECTATOR && this.plugin.getVampireManager().isVampire(player) && this.plugin.getVampireManager().isVampireStage2OrHigher(player)) {
                this.applyHolyWaterEffect(player);
            }
        }
    }

    /**
     * Search the player's inventory to find a bottle of holy water.
     *
     * @param player the player being searched.
     * @return The bottle of holy water, or {@code null} if none is found.
     */
    public @Nullable ItemStack findHolyWater(Player player) {
        for (ItemStack item : player.getInventory()) {
            if (ItemTypeChecking.isHolyWater(item)) {
                return item;
            }
        }

        return null;
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
        BukkitTask enableTask = Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.removeHolyWaterEffect(vampire),  this.configManager.getHolyWaterDisableDurationSeconds() * 20L);

        this.disabledVampires.put(vampireId, enableTask);
        this.plugin.logInfo("Applied holy water effect to vampire: " + vampire.getName());
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

        this.plugin.logInfo("Removed holy water effect from vampire: " + vampire.getName());
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
        final long duration = this.configManager.getHolyWaterDisableDurationSeconds();

        return task == null ? 0L : Math.max(0L, duration - System.currentTimeMillis() / 1000L % duration);
    }

    /**
     * Inform the vampire that their abilities have been disabled by holy water.
     *
     * @param vampire the player whose abilities are disabled.
     */
    private void notifyVampireDisabled(Player vampire) {
        int duration = this.configManager.getHolyWaterDisableDurationSeconds();

        vampire.sendMessage(Component.text("The holy water sears your vampiric essence!", NamedTextColor.RED));

        if (duration >= 60) {
            vampire.sendMessage(Component.text("Your abilities and blood regeneration have been disabled for " + duration / 60 + " minute" + (duration / 60 != 1 ? "s" : "") + ".", NamedTextColor.RED));
        } else {
            vampire.sendMessage(Component.text("Your abilities and blood regeneration have been disabled for " + duration + " second" + (duration != 1 ? "s" : "") + ".", NamedTextColor.RED));
        }

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
        vampire.sendMessage(Component.text("You feel your dark powers flowing through you once more.", NamedTextColor.RED));
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
        for (Map.Entry<UUID, BukkitTask> entry : this.disabledVampires.entrySet()) {
            UUID vampireId = entry.getKey();
            BukkitTask task = entry.getValue();

            if (task != null && !task.isCancelled()) {
                task.cancel();
            }

            Player vampire = Bukkit.getPlayer(vampireId);

            if (vampire != null && vampire.isOnline()) {
                vampire.sendMessage(Component.text("An admin has restored your vampiric abilities.", NamedTextColor.RED));
            }
        }

        int cleared = this.getDisabledVampireCount();
        this.disabledVampires.clear();
        this.plugin.logInfo("Cleared holy water effects from " + cleared + " vampires");
    }

    /**
     * Remove the holy water ability suppression from all vampires before shutting down the manager.
     */
    public void shutdown() {
        for (BukkitTask task : this.disabledVampires.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }

        this.disabledVampires.clear();
        this.plugin.logInfo("HolyWaterEffectManager shutdown complete");
    }
}
