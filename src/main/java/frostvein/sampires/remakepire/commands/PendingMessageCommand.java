package frostvein.sampires.remakepire.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class PendingMessageCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;

    /**
     * Create an instance of the plugin's accidental chat prevention command handler.
     *
     * @param plugin the host plugin object.
     */
    public PendingMessageCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle the command execution of stopping the player from messaging and allowing them to bypass it.
     *
     * @return {@code true} if the command didn't trigger a fatal error, and the command sender is a player.
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return false;

        } else {
            this.plugin.getPlayerChatManager().handleSendPendingMessage(player);

            // Stop the chat prevention from happening more than once to each player
            player.addScoreboardTag("ChatPrevented");

            return true;
        }
    }
}
