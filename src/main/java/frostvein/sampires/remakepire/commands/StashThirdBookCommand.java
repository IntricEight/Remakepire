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
        World world = Bukkit.getWorld("world");
        if (world == null) {
            sender.sendMessage("§cWorld 'world' not found.");
            return true;

        } else {
            Location chestLocation = new Location(world, 76.0, 80.0, 407.0);
            Block block = world.getBlockAt(chestLocation);

            if (!(block.getState() instanceof Chest)) {
                sender.sendMessage("§cNo chest found at coordinates 76, 80, 407.");
                return true;

            } else {
                Chest chest = (Chest)block.getState();
                Inventory chestInventory = chest.getInventory();
                chestInventory.clear();
                ItemStack book = this.createAbsolutionBook();
                chestInventory.addItem(new ItemStack[]{book});
                sender.sendMessage("§aSuccessfully stashed 'The Absolution 3/3' in the chest at 76, 80, 407.");

                this.plugin.getLogger().info(sender.getName() + " used /stash_third_book - placed The Absolution 3/3 at 76, 80, 407");
                return true;
            }
        }
    }

    private ItemStack createAbsolutionBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta)book.getItemMeta();
        if (bookMeta != null) {
            bookMeta.setTitle("The Absolution 3/3");
            bookMeta.setAuthor("§6A holy source unknown...");
            bookMeta.setPages(new String[]{"§0Know, seeker, that this sacred knowledge forms but the third pillar of the Trinity of Restoration.", "§0As the ancients decreed, no single tome holds the complete path to salvation.", "§0Absolution can only be found where divine power lies, let the seeker's voice break the bonds of darkness before a beacon of holy light...", "§0Speak the ancient words of renunciation:", "§7/§6voluntate-mea-hoc-nefandum-vinculum-abicio", "§0§lBeware: The price of such liberation burns away the very sanctity that enables it forever, and a child of the night may not return to their corrupted ways once liberated, only death waits for them now."});
            book.setItemMeta(bookMeta);
        }

        return book;
    }
}
