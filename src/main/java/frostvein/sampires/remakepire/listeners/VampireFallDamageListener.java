package frostvein.sampires.remakepire.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerQuitEvent;
import frostvein.sampires.remakepire.managers.VampireManager;

public class VampireFallDamageListener implements Listener {
    private final VampireManager vampireManager;

    /**
     * Create an instance of the Vampire Fall Damage listener.
     *
     * @param vampireManager the manager for generic vampire traits.
     */
    public VampireFallDamageListener(VampireManager vampireManager) {
        this.vampireManager = vampireManager;
    }

    /**
     * Reduce or remove the damage dealt by fall damage.
     *
     * @param event an entity receives damage.
     */
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getCause() == DamageCause.FALL) {
                if (this.vampireManager.shouldPreventFallDamage(player)) {
                    event.setCancelled(true);
                } else {
                    if (this.vampireManager.isVampire(player)) {
                        event.setDamage(event.getDamage() * 0.5);
                    }
                }
            }
        }
    }

    /**
     * Remove fall damage protection when a player quits the game.
     *
     * @param event a player leaving the world.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.vampireManager.removeProtection(event.getPlayer());
    }
}
