package frostvein.sampires.remakepire.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class PendingMessageCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;

    public PendingMessageCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return false;
        } else {
            this.plugin.getPlayerChatManager().handleSendPendingMessage(player);
            player.addScoreboardTag("ChatPrevented");
            return true;
        }
    }
}
