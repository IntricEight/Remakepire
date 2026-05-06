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
    private final Map<UUID, Boolean> playersWithBloodMoonAttributes = new HashMap<>();
    private final Map<UUID, AttributeModifier> speedModifiers = new HashMap<>();
    private final Map<UUID, AttributeModifier> strengthModifiers = new HashMap<>();
    // Controls the additional speed vampires receive.
    private static final double SPEED_MODIFIER = 0.3;
    // Controls the additional strength vampires receive.
    private static final double STRENGTH_MODIFIER = 0.15;

    /**
     * Create an instance of the Blood Moon Attribute listener.
     *
     * @param plugin the host plugin object.
     */
    public BloodMoonAttributeListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.logInfo("BloodMoonAttributeListener initialized");
    }

    /**
     *
     *
     * @param event a potion effect changes.
     */
    @EventHandler
    public void onPotionEffectChange(EntityPotionEffectEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Player player) {
            if (this.vampireManager.isVampireStage2OrHigher(player)) {
                if (event.getNewEffect() != null && event.getNewEffect().getType() == PotionEffectType.UNLUCK) {
                    this.applyBloodMoonAttributes(player);
                } else if (event.getOldEffect() != null && event.getOldEffect().getType() == PotionEffectType.UNLUCK) {
                    this.removeBloodMoonAttributes(player);
                }

                // Remove the blood moon attributes from the player if they don't qualify for it
                if (!this.vampireManager.isVampireStage2OrHigher(player) && this.playersWithBloodMoonAttributes.getOrDefault(player.getUniqueId(), false)) {
                    this.removeBloodMoonAttributes(player);
                }
            }
        }
    }

    /**
     * Apply the bonuses to vampires during a blood moon.
     *
     * @param player the player being given bonuses from the blood moon.
     */
    private void applyBloodMoonAttributes(Player player) {
        UUID playerId = player.getUniqueId();

        if (!this.playersWithBloodMoonAttributes.getOrDefault(playerId, false)) {
            AttributeInstance speedAttribute = player.getAttribute(Attribute.MOVEMENT_SPEED);

            if (speedAttribute != null) {
                AttributeModifier speedModifier = new AttributeModifier("BloodMoon_Speed", SPEED_MODIFIER, Operation.MULTIPLY_SCALAR_1);
                speedAttribute.addModifier(speedModifier);
                this.speedModifiers.put(playerId, speedModifier);
            }

            AttributeInstance strengthAttribute = player.getAttribute(Attribute.ATTACK_DAMAGE);

            if (strengthAttribute != null) {
                AttributeModifier strengthModifier = new AttributeModifier("BloodMoon_Strength", STRENGTH_MODIFIER, Operation.MULTIPLY_SCALAR_1);
                strengthAttribute.addModifier(strengthModifier);
                this.strengthModifiers.put(playerId, strengthModifier);
            }

            this.playersWithBloodMoonAttributes.put(playerId, true);
            this.plugin.logInfo("Applied blood moon attributes to vampire: " + player.getName());
        }
    }

    /**
     * Remove the bonuses from vampires after a blood moon.
     *
     * @param player the player who had bonuses from the blood moon.
     */
    private void removeBloodMoonAttributes(Player player) {
        UUID playerId = player.getUniqueId();

        if (this.playersWithBloodMoonAttributes.getOrDefault(playerId, false)) {
            AttributeInstance speedAttribute = player.getAttribute(Attribute.MOVEMENT_SPEED);
            AttributeModifier speedModifier = this.speedModifiers.remove(playerId);

            if (speedAttribute != null && speedModifier != null) {
                speedAttribute.removeModifier(speedModifier);
            }

            AttributeInstance strengthAttribute = player.getAttribute(Attribute.ATTACK_DAMAGE);
            AttributeModifier strengthModifier = this.strengthModifiers.remove(playerId);

            if (strengthAttribute != null && strengthModifier != null) {
                strengthAttribute.removeModifier(strengthModifier);
            }

            this.playersWithBloodMoonAttributes.put(playerId, false);
            this.plugin.logInfo("Removed blood moon attributes from vampire: " + player.getName());
        }
    }

    /**
     * Remove the bonuses from vampires when they quit the game during a blood moon.
     *
     * @param event a player leaving the world.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (this.playersWithBloodMoonAttributes.getOrDefault(playerId, false)) {
            this.removeBloodMoonAttributes(player);
            this.plugin.logInfo("Cleaned up blood moon attributes for quitting player: " + player.getName());
        }
    }

    /**
     * Remove the blood moon's bonuses from vampires.
     *
     * @param player the player who had bonuses from the blood moon.
     */
    public void forceRemoveBloodMoonAttributes(Player player) {
        this.removeBloodMoonAttributes(player);
    }

    /**
     * Clean up the effects of the blood moon when the server is shut down.
     *
     * @param player the player who joined the game.
     */
    public void forceCleanupOnJoin(Player player) {
        try {
            UUID playerId = player.getUniqueId();
            AttributeInstance speedAttribute = player.getAttribute(Attribute.MOVEMENT_SPEED);
            AttributeInstance strengthAttribute = player.getAttribute(Attribute.ATTACK_DAMAGE);
            int removedCount = 0;

            if (speedAttribute != null) {
                for(AttributeModifier mod : speedAttribute.getModifiers().stream().filter((modx) -> modx.getAmount() > 0.05 || modx.getName().contains("BloodMoon") || modx.getName().contains("Vampire")).toList()) {
                    speedAttribute.removeModifier(mod);
                    this.plugin.logInfo("Removed speed modifier: " + mod.getName() + " (" + mod.getAmount() + ")");
                    ++removedCount;
                }
            }

            if (strengthAttribute != null) {
                for(AttributeModifier mod : strengthAttribute.getModifiers().stream().filter((modx) -> modx.getName().contains("BloodMoon") || modx.getName().contains("Vampire")).toList()) {
                    strengthAttribute.removeModifier(mod);
                    this.plugin.logInfo("Removed strength modifier: " + mod.getName() + " (" + mod.getAmount() + ")");
                    ++removedCount;
                }
            }

            this.playersWithBloodMoonAttributes.put(playerId, false);
            this.speedModifiers.remove(playerId);
            this.strengthModifiers.remove(playerId);

            if (removedCount > 0) {
                this.plugin.logInfo("AGGRESSIVE cleanup removed " + removedCount + " suspicious attribute modifiers from: " + player.getName());
                player.sendMessage("§aRemoved " + removedCount + " attribute modifiers that were causing speed/jump issues.");
            } else {
                this.plugin.logInfo("No suspicious attribute modifiers found for: " + player.getName());
            }
        } catch (Exception e) {
            this.plugin.getLogger().warning("Error during aggressive cleanup of attributes for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Remove the blood moon effects before shutting down the listener.
     */
    public void shutdown() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            if (this.playersWithBloodMoonAttributes.getOrDefault(player.getUniqueId(), false)) {
                this.removeBloodMoonAttributes(player);
            }
        }

        this.playersWithBloodMoonAttributes.clear();
        this.speedModifiers.clear();
        this.strengthModifiers.clear();
        this.plugin.logInfo("BloodMoonAttributeListener shutdown complete");
    }
}
