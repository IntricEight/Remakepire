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

    public VampireFallDamageListener(VampireManager vampireManager) {
        this.vampireManager = vampireManager;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            if (event.getCause() == DamageCause.FALL) {
                Player player = (Player)event.getEntity();
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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.vampireManager.removeProtection(event.getPlayer());
    }
}
