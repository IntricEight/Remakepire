package frostvein.sampires.remakepire.listeners;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.ConfigManager;

public class FourthBookRevealListener implements Listener {
    private final RemakepirePlugin plugin;
    private final ConfigManager configManager;
    private final List<Location> tomeChestLocations;
    private final Location townChestLocation;

    /**
     * Create an instance of the Fourth Cure Book Reveal listener.
     *
     * @param plugin the host plugin object.
     */
    public FourthBookRevealListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.tomeChestLocations = configManager.getTomeChestLocations();

        if (plugin.getWorld() != null) {
            this.townChestLocation = new Location(plugin.getWorld(), 76.0, 80.0, 407.0);
            plugin.logInfo("FourthBookRevealListener: Loaded " + this.tomeChestLocations.size() + " tome chest locations from config");
        } else {
            this.townChestLocation = null;
            plugin.getLogger().warning("FourthBookRevealListener: World not found during initialization");
        }
    }

    /**
     * Reveal the fourth cure book in a tome chest once the config setting is enabled.
     *
     * @param event a player opens a chest.
     */
    @EventHandler
    public void onChestOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            if (this.plugin.getSessionManager().isSessionActive()) {
                if (!this.plugin.getVampireManager().isVampire(player)) {
                    if (this.plugin.getConfig().getBoolean("fourth_book_spawn_enabled", false)) {
                        if (!this.hasBeenRevealed()) {
                            Inventory inventory = event.getInventory();

                            if (inventory.getHolder() != null && inventory.getHolder() instanceof Chest chest) {
                                Location chestLocation = chest.getLocation();

                                // Prevent the fourth cure book from loading in the Town's tome chest
                                if (!this.isTownChest(chestLocation)) {
                                    if (this.isTomeChest(chestLocation)) {
                                        this.revealFourthBook(chest, player);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Determine if a chest location is that of the town's tome chest.
     *
     * @param location a location that might contain a chest.
     * @return {@code true} if
     */
    private boolean isTownChest(Location location) {
        if (this.townChestLocation == null) {
            return false;
        } else {
            return location.getBlockX() == this.townChestLocation.getBlockX() && location.getBlockY() == this.townChestLocation.getBlockY() && location.getBlockZ() == this.townChestLocation.getBlockZ() && location.getWorld().equals(this.townChestLocation.getWorld());
        }
    }

    /**
     * Determine if a chest location is a chest where holy tomes can spawn.
     *
     * @param location a location that might contain a chest.
     * @return {@code true} if the location matches that of a tome chest.
     */
    private boolean isTomeChest(Location location) {
        for(Location tomeLocation : this.tomeChestLocations) {
            if (location.getBlockX() == tomeLocation.getBlockX() && location.getBlockY() == tomeLocation.getBlockY() && location.getBlockZ() == tomeLocation.getBlockZ() && location.getWorld().equals(tomeLocation.getWorld())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Retrieve whether the fourth cure book been found in a tome chest.
     *
     * @return {@code true} if cure book 4/3 has spawned.
     */
    private boolean hasBeenRevealed() {
        return this.plugin.getConfig().getBoolean("fourth_book_has_spawned", false);
    }

    /**
     * Log that the fourth cure book has been found in a tome chest.
     */
    private void markAsRevealed() {
        this.plugin.getConfig().set("fourth_book_has_spawned", true);
        this.plugin.saveConfig();
        this.plugin.logInfo("FourthBookRevealListener: Marked fourth book as revealed in config");
    }

    /**
     * Reveal the fourth cure book as a player opens the tome chest.
     *
     * @param chest the chest where cure book 4/3 will be placed.
     * @param player the player who discovered the fourth cure book.
     */
    private void revealFourthBook(Chest chest, Player player) {
        ItemStack fourthBook = this.createRetributionBook();
        Inventory chestInventory = chest.getInventory();
        chestInventory.addItem(fourthBook);
        this.markAsRevealed();

        player.sendMessage("§8§o[As you open the chest, an unfamiliar book suddenly materializes within...]");
        player.sendMessage("§4§o[The smell of old blood emanates from its pages...]");

        this.plugin.logInfo("FOURTH BOOK REVEALED: " + player.getName() + " opened tome chest at " + chest.getLocation().getBlockX() + ", " + chest.getLocation().getBlockY() + ", " + chest.getLocation().getBlockZ());
    }

    /**
     * Create the fourth cure book, "the Retribution".
     *
     * @return the written cure book 4/3.
     */
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
//            bookMeta.setPages("§8§o[The writing in this book is unlike the previous three, it's is hurried and panicked, the ink is smeared and the smell of blood rests faintly on the pages]§r\n\n§4The spirits are too lenient... Too soft...", "§0These disgusting, vile, works of evil could never be convinced to come back to the light...\n\n§0They must be dragged back to humanity, even if it's by, kicking and screaming.", "§0Give them a choice. Accept the light, or face eternal darkness.\n\n§0I will give them this choice, with these holy words:", "§7/§4hoc-vinculum-tibi-dirumpo-mala-creatura §7<§4Players-Name§7>");
            book.setItemMeta(bookMeta);
        }

        return book;
    }
}
