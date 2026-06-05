package frostvein.sampires.remakepire.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class StashFourthBookCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;

    public StashFourthBookCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /stash_fourth_book <x> <y> <z>");
            return true;

        } else {
            int x, y, z;

            try {
                x = Integer.parseInt(args[0]);
                y = Integer.parseInt(args[1]);
                z = Integer.parseInt(args[2]);

            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid coordinates. Use whole numbers.");
                return true;
            }

            World world = Bukkit.getWorld("world");

            if (world == null) {
                sender.sendMessage("§cWorld 'world' not found.");
                return true;

            } else {
                Location chestLocation = new Location(world, x, y, z);
                Block block = world.getBlockAt(chestLocation);

                if (!(block.getState() instanceof Chest chest)) {
                    sender.sendMessage("§cNo chest found at coordinates " + x + ", " + y + ", " + z + ".");
                    return true;

                } else {
                    Inventory chestInventory = chest.getInventory();
                    chestInventory.clear();
                    ItemStack book = this.plugin.getCureBookManager().getCureBook(4);
                    chestInventory.addItem(book);
                    sender.sendMessage("§aSuccessfully stashed '" + this.plugin.getCureBookManager().getCureBookName(4, true) + "' in the chest at " + x + ", " + y + ", " + z + ".");

                    this.plugin.logInfo(sender.getName() + " used /stash_fourth_book - placed " + this.plugin.getCureBookManager().getCureBookName(4, true) + " at " + x + ", " + y + ", " + z);
                    return true;
                }
            }
        }
    }
}
