package frostvein.sampires.remakepire.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffectType;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.VampireManager;

public class BloodMoonAttributeListener implements Listener {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final Map<UUID, Boolean> playersWithBloodMoonAttributes = new HashMap();
    private final Map<UUID, AttributeModifier> speedModifiers = new HashMap();
    private final Map<UUID, AttributeModifier> strengthModifiers = new HashMap();
    private static final double SPEED_MODIFIER = 0.3;
    private static final double STRENGTH_MODIFIER = 0.15;

    public BloodMoonAttributeListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("BloodMoonAttributeListener initialized");
    }

    @EventHandler
    public void onPotionEffectChange(EntityPotionEffectEvent event) {
        Entity var3 = event.getEntity();
        if (var3 instanceof Player player) {
            if (this.vampireManager.isVampireStage2(player) || this.vampireManager.isVampireStage3(player)) {
                if (event.getNewEffect() != null && event.getNewEffect().getType() == PotionEffectType.UNLUCK) {
                    this.applyBloodMoonAttributes(player);
                } else if (event.getOldEffect() != null && event.getOldEffect().getType() == PotionEffectType.UNLUCK) {
                    this.removeBloodMoonAttributes(player);
                }

                if (!this.vampireManager.isVampireStage2(player) && !this.vampireManager.isVampireStage3(player) && (Boolean)this.playersWithBloodMoonAttributes.getOrDefault(player.getUniqueId(), false)) {
                    this.removeBloodMoonAttributes(player);
                }

            }
        }
    }

    private void applyBloodMoonAttributes(Player player) {
        UUID playerId = player.getUniqueId();
        if (!(Boolean)this.playersWithBloodMoonAttributes.getOrDefault(playerId, false)) {
            AttributeInstance speedAttribute = player.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speedAttribute != null) {
                AttributeModifier speedModifier = new AttributeModifier("BloodMoon_Speed", 0.3, Operation.MULTIPLY_SCALAR_1);
                speedAttribute.addModifier(speedModifier);
                this.speedModifiers.put(playerId, speedModifier);
            }

            AttributeInstance strengthAttribute = player.getAttribute(Attribute.ATTACK_DAMAGE);
            if (strengthAttribute != null) {
                AttributeModifier strengthModifier = new AttributeModifier("BloodMoon_Strength", 0.15, Operation.MULTIPLY_SCALAR_1);
                strengthAttribute.addModifier(strengthModifier);
                this.strengthModifiers.put(playerId, strengthModifier);
            }

            this.playersWithBloodMoonAttributes.put(playerId, true);
            this.plugin.getLogger().info("Applied blood moon attributes to vampire: " + player.getName());
        }
    }

    private void removeBloodMoonAttributes(Player player) {
        UUID playerId = player.getUniqueId();
        if ((Boolean)this.playersWithBloodMoonAttributes.getOrDefault(playerId, false)) {
            AttributeInstance speedAttribute = player.getAttribute(Attribute.MOVEMENT_SPEED);
            AttributeModifier speedModifier = (AttributeModifier)this.speedModifiers.remove(playerId);
            if (speedAttribute != null && speedModifier != null) {
                speedAttribute.removeModifier(speedModifier);
            }

            AttributeInstance strengthAttribute = player.getAttribute(Attribute.ATTACK_DAMAGE);
            AttributeModifier strengthModifier = (AttributeModifier)this.strengthModifiers.remove(playerId);
            if (strengthAttribute != null && strengthModifier != null) {
                strengthAttribute.removeModifier(strengthModifier);
            }

            this.playersWithBloodMoonAttributes.put(playerId, false);
            this.plugin.getLogger().info("Removed blood moon attributes from vampire: " + player.getName());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if ((Boolean)this.playersWithBloodMoonAttributes.getOrDefault(playerId, false)) {
            this.removeBloodMoonAttributes(player);
            this.plugin.getLogger().info("Cleaned up blood moon attributes for quitting player: " + player.getName());
        }

    }

    public void forceRemoveBloodMoonAttributes(Player player) {
        this.removeBloodMoonAttributes(player);
    }

    public void forceCleanupOnJoin(Player player) {
        try {
            UUID playerId = player.getUniqueId();
            int removedCount = 0;
            AttributeInstance speedAttribute = player.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speedAttribute != null) {
                double baseSpeed = speedAttribute.getBaseValue();

                for(AttributeModifier mod : speedAttribute.getModifiers().stream().filter((modx) -> modx.getAmount() > 0.05 || modx.getName().contains("BloodMoon") || modx.getName().contains("Vampire")).toList()) {
                    speedAttribute.removeModifier(mod);
                    this.plugin.getLogger().info("Removed speed modifier: " + mod.getName() + " (" + mod.getAmount() + ")");
                    ++removedCount;
                }
            }

            AttributeInstance strengthAttribute = player.getAttribute(Attribute.ATTACK_DAMAGE);
            if (strengthAttribute != null) {
                for(AttributeModifier mod : strengthAttribute.getModifiers().stream().filter((modx) -> modx.getName().contains("BloodMoon") || modx.getName().contains("Vampire")).toList()) {
                    strengthAttribute.removeModifier(mod);
                    this.plugin.getLogger().info("Removed strength modifier: " + mod.getName() + " (" + mod.getAmount() + ")");
                    ++removedCount;
                }
            }

            this.playersWithBloodMoonAttributes.put(playerId, false);
            this.speedModifiers.remove(playerId);
            this.strengthModifiers.remove(playerId);
            if (removedCount > 0) {
                this.plugin.getLogger().info("AGGRESSIVE cleanup removed " + removedCount + " suspicious attribute modifiers from: " + player.getName());
                player.sendMessage("§aRemoved " + removedCount + " attribute modifiers that were causing speed/jump issues.");
            } else {
                this.plugin.getLogger().info("No suspicious attribute modifiers found for: " + player.getName());
            }
        } catch (Exception e) {
            this.plugin.getLogger().warning("Error during aggressive cleanup of attributes for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }

    }

    public void shutdown() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            if ((Boolean)this.playersWithBloodMoonAttributes.getOrDefault(player.getUniqueId(), false)) {
                this.removeBloodMoonAttributes(player);
            }
        }

        this.playersWithBloodMoonAttributes.clear();
        this.speedModifiers.clear();
        this.strengthModifiers.clear();
        this.plugin.getLogger().info("BloodMoonAttributeListener shutdown complete");
    }
}
