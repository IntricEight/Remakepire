package frostvein.sampires.remakepire.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class DamageSuppressionListener implements Listener {
    private final RemakepirePlugin plugin;

    /**
     * Create an instance of the Damage Suppression listener.
     *
     * @param plugin the host plugin object.
     */
    public DamageSuppressionListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Decrease the damage dealt to an entity with the plugin damage suppression value.
     *
     * @param event an entity receives damage.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            try {
                int suppressionScore = this.plugin.getConfigManager().getDamageSuppression();

                if (suppressionScore > 0) {
                    double suppressionPercentage = suppressionScore / 100.0;
                    double originalDamage = event.getDamage();
                    event.setDamage(originalDamage * (1.0 - suppressionPercentage));
                }
            } catch (Exception e) {
                this.plugin.getLogger().warning("Failed to read damage suppression from config: " + e.getMessage());
            }
        }
    }
}
