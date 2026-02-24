package frostvein.sampires.remakepire.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class ForcedCureReopenCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;

    public ForcedCureReopenCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

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
