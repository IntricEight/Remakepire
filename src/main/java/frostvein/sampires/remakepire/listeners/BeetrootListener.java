package frostvein.sampires.remakepire.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class BeetrootListener implements Listener {
    private final RemakepirePlugin plugin;

    /**
     * Create an instance of the Beetroot "garlic" listener.
     *
     * @param plugin the host plugin object.
     */
    public BeetrootListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle the effects of eating garlic when beetroot is eaten.
     *
     * @param event a player consumes an item.
     */
    @EventHandler(
            priority = EventPriority.NORMAL
    )
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (this.plugin.getSessionManager().isSessionActive()) {
            if (item.getType() == Material.BEETROOT) {
                this.plugin.getBeetrootManager().handleBeetrootConsumption(player);
            }
        }
    }
}
