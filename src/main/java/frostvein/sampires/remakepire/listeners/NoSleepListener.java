package frostvein.sampires.remakepire.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class NoSleepListener implements Listener {
    RemakepirePlugin plugin;

    public NoSleepListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        if (this.plugin.getVampireManager().isVampire(player)) {
            player.sendMessage("§cYou close your eyes... And are left disappointed. Once again, you find it impossible to fall asleep.");
        } else if (!this.plugin.getVampireManager().isVampire(player)) {
            player.sendMessage("§cYou close your eyes... And are left disappointed. You feel too uneasy to sleep.");
        }

        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            if (player.isSleeping()) {
                player.teleport(player.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F));
            }

        }, 2L);
    }
}
