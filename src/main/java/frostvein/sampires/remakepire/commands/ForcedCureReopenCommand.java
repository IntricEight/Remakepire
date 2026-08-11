package frostvein.sampires.remakepire.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class ForcedCureReopenCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;

    /**
     * Create an instance of the plugin's forced cure GUI reopener command handler.
     *
     * @param plugin the host plugin object.
     */
    public ForcedCureReopenCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle the command execution of reopening the forced cure choice GUI.
     *
     * @return {@code true} if the command didn't trigger a fatal error.
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;

        } else if (!this.plugin.getForcedCureChoiceManager().hasPendingCure(player)) {
            player.sendMessage("§cYou do not have a pending cure decision.");
            return true;

        } else {
            // Provide the cured with the choice between life and death.
            this.plugin.getForcedCureChoiceManager().reopenChoiceGUI(player);
            return true;
        }
    }
}
