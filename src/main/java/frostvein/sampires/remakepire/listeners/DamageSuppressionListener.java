package frostvein.sampires.remakepire.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class DamageSuppressionListener implements Listener {
    private final RemakepirePlugin plugin;

    public DamageSuppressionListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            try {
                int suppressionScore = this.plugin.getConfig().getInt("damage_suppression", 50);
                if (suppressionScore > 0) {
                    double suppressionPercentage = (double)suppressionScore / (double)100.0F;
                    double originalDamage = event.getDamage();
                    double suppressedDamage = originalDamage * ((double)1.0F - suppressionPercentage);
                    event.setDamage(suppressedDamage);
                }
            } catch (Exception e) {
                this.plugin.getLogger().warning("Failed to read damage suppression from config: " + e.getMessage());
            }
        }
    }
}
