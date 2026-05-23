package frostvein.sampires.remakepire.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class NoSleepListener implements Listener {
    RemakepirePlugin plugin;

    /**
     * Create an instance of the No Sleep (sleep prevention) listener.
     *
     * @param plugin the host plugin object.
     */
    public NoSleepListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Kick the player out of bed when they attempt to sleep.
     *
     * @param event a player enters a bed.
     */
    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();

        if (this.plugin.getVampireManager().isVampire(player)) {
            player.sendMessage("§cYou close your eyes... And are left disappointed. Once again, you find it impossible to fall asleep.");
        } else if (this.plugin.getVampireManager().isHuman(player)) {
            player.sendMessage("§cYou close your eyes... And are left disappointed. You feel too uneasy to sleep.");
        }

        event.setCancelled(true);
    }
}
