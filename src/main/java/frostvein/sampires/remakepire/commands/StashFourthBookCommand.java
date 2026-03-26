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
                    ItemStack book = this.createRetributionBook();
                    chestInventory.addItem(book);
                    sender.sendMessage("§aSuccessfully stashed 'The Retribution 4/3' in the chest at " + x + ", " + y + ", " + z + ".");
                    this.plugin.logInfo(sender.getName() + " used /stash_fourth_book - placed The Retribution 4/3 at " + x + ", " + y + ", " + z);
                    return true;
                }
            }
        }
    }

    private ItemStack createRetributionBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta)book.getItemMeta();

        if (bookMeta != null) {
            bookMeta.setTitle("The Retribution 4/3");
            bookMeta.setAuthor("§4A vengeful hand...");
            bookMeta.setPages("§8§o[The writing in this book is unlike the previous three, it is hurried and panicked, the ink is smeared and the smell of blood rests faintly on the pages]§r\n\n§0The spirits are too lenient... Too soft...", "§0These disgusting, vial, works of evil could never be convinced to come back to the light...\n\n§0They must be dragged back to humanity, kicking and screaming.", "§0They will have to choose. Accept the light, or face eternal darkness.\n\n§0I will give them this choice, with these holy words:", "§7/§4hoc-vinculum-tibi-dirumpo-mala-creatura §7<§4Players-Name§7>");
            book.setItemMeta(bookMeta);
        }

        return book;
    }
}
