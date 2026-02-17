package frostvein.sampires.remakepire.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.BeetrootManager;
import frostvein.sampires.remakepire.managers.SessionManager;

public class BeetrootListener implements Listener {
    private final RemakepirePlugin plugin;
    private final BeetrootManager beetrootManager;
    private final SessionManager sessionManager;

    public BeetrootListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.beetrootManager = plugin.getBeetrootManager();
        this.sessionManager = plugin.getSessionManager();
    }

    @EventHandler(
            priority = EventPriority.NORMAL
    )
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (this.sessionManager.isSessionActive()) {
            if (item.getType() == Material.BEETROOT) {
                this.beetrootManager.handleBeetrootConsumption(player);
            }
        }
    }
}
