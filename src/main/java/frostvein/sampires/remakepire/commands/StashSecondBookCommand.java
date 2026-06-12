package frostvein.sampires.remakepire.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class StashSecondBookCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;

    /**
     * Create an instance of the command manager to stash the second cure book.
     *
     * @param plugin the host plugin object.
     */
    public StashSecondBookCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Stash the second cure book at the location provided through the command.
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /stash_second_book <x> <y> <z>");
            return true;
        }

        boolean stashSucccess;
        int x, y, z;

        try {
            x = Integer.parseInt(args[0]);
            y = Integer.parseInt(args[1]);
            z = Integer.parseInt(args[2]);

        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid coordinates. Use whole numbers.");
            return true;
        }

        // Stash the third cure book inside the chest at the location, if it exists
        if (sender instanceof Player admin) {
            stashSucccess = this.plugin.getCureBookManager().stashCureBook(admin, 2, x, y, z);
        } else {
            stashSucccess = this.plugin.getCureBookManager().stashCureBook(2, x, y, z);
        }

        if (stashSucccess) {
            this.plugin.logInfo(sender.getName() + " used /stash_second_book - placed " + this.plugin.getCureBookManager().getCureBookName(2, true) + " at " + x + ", " + y + ", " + z);
        }

        return true;
    }
}
