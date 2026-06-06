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

public class StashThirdBookCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;

    public StashThirdBookCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        World world = Bukkit.getWorld(RemakepirePlugin.WORLD_NAME);

        if (world == null) {
            sender.sendMessage("§cWorld 'world' not found.");
            return true;

        } else {
            Location chestLocation = new Location(world, 76.0, 80.0, 407.0);
            Block block = world.getBlockAt(chestLocation);

            if (!(block.getState() instanceof Chest chest)) {
                sender.sendMessage("§cNo chest found at coordinates 76, 80, 407.");
                return true;

            } else {
                Inventory chestInventory = chest.getInventory();
                chestInventory.clear();
                ItemStack book = this.plugin.getCureBookManager().getCureBook(3);
                chestInventory.addItem(book);
                sender.sendMessage("§aSuccessfully stashed '" + this.plugin.getCureBookManager().getCureBookName(3, true) + "' in the chest at 76, 80, 407.");

                this.plugin.logInfo(sender.getName() + " used /stash_third_book - placed " + this.plugin.getCureBookManager().getCureBookName(3, true) + " at 76, 80, 407");
                return true;
            }
        }
    }
}
