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
            bookMeta.setTitle("§r§cOn the Nature of Sanguinity");
            bookMeta.setAuthor("§4§lTomas Orion");
            bookMeta.setPages("§4§lOn the Nature of Sanguinity\n" +
                            "§o2nd Ed§r\n" +
                            "§oBy Tomas Orion§r\n\n" +
                            "§8§o[You find a section that was not present in the edition you already read. It seems useful]\n",
                            "§4§lPart 4, Biology§r\n" +
                            "There are a few key differences in biology of a Vampire and an uncorrupted human.\n" +
                            "The most famous and obvious are, of course, their \"fangs\".\n" +
                            "The fangs are merely extended canine teeth, shaped to pierce flesh and gain access to a human artery or vein.",
                            "Vampires pale skin is cased by the other large difference between a human and someone who has been affected by Vampirism: Circulation. As a Vampire's heart does not beat, their blood does not flow around their body, Instead it flows primarily between the throat and the stomach,",
                            "where it is absorbed into the body through a means we do not yet understand. Something which has held my fascination for some time is that there is only one place in all of a vampires body where one can find blood, even when a vampire has not fed for some time. This location is the",
                            "Vertebral Artery. An artery which carries blood to the brain. Only here can blood be constantly found within a vampires body.\n" +
                            "Another change, present in vampires whose condition has progressed to a significant degree, is the presence of \"claws\". Claws are the elongated finger",
                            "bones that protrude through the end of their fingers in a way that should be extremely painful for the afflicted but is somehow painless. In addition to this, Vampire muscle mass seems to gain significant density at this level, which allows them to commit acts of great strength and",
                            "agility, jumping cliffs and speeding across the ground supernaturally quickly. In addition to this their nerves are dampened significantly, causing them to feel little pain outside of a few key sources. There are a few other minor differences worth mentioning...",
                            "§o[You figure you've absorbed what knowledge you actually need from this text by now]§r");
//            bookMeta.setPages("§8§o[The writing in this book is unlike the previous three, it is hurried and panicked, the ink is smeared and the smell of blood rests faintly on the pages]§r\n\n§0The spirits are too lenient... Too soft...", "§0These disgusting, vial, works of evil could never be convinced to come back to the light...\n\n§0They must be dragged back to humanity, kicking and screaming.", "§0They will have to choose. Accept the light, or face eternal darkness.\n\n§0I will give them this choice, with these holy words:", "§7/§4hoc-vinculum-tibi-dirumpo-mala-creatura §7<§4Players-Name§7>");

            book.setItemMeta(bookMeta);
        }

        return book;
    }
}
