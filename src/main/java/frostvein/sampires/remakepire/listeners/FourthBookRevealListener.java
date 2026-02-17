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

    public FourthBookRevealListener(RemakepirePlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.tomeChestLocations = configManager.getTomeChestLocations();
        if (plugin.getWorld() != null) {
            this.townChestLocation = new Location(plugin.getWorld(), 76.0, 80.0, 407.0);
            plugin.getLogger().info("FourthBookRevealListener: Loaded " + this.tomeChestLocations.size() + " tome chest locations from config");
        } else {
            this.townChestLocation = null;
            plugin.getLogger().warning("FourthBookRevealListener: World not found during initialization");
        }

    }

    @EventHandler
    public void onChestOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player)event.getPlayer();
            if (this.plugin.getSessionManager().isSessionActive()) {
                if (!this.plugin.getVampireManager().isVampire(player)) {
                    if (this.plugin.getConfig().getBoolean("fourth_book_spawn_enabled", false)) {
                        if (!this.hasBeenRevealed()) {
                            Inventory inventory = event.getInventory();
                            if (inventory.getHolder() != null && inventory.getHolder() instanceof Chest) {
                                Chest chest = (Chest)inventory.getHolder();
                                Location chestLocation = chest.getLocation();
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

    private boolean isTownChest(Location location) {
        if (this.townChestLocation == null) {
            return false;
        } else {
            return location.getBlockX() == this.townChestLocation.getBlockX() && location.getBlockY() == this.townChestLocation.getBlockY() && location.getBlockZ() == this.townChestLocation.getBlockZ() && location.getWorld().equals(this.townChestLocation.getWorld());
        }
    }

    private boolean isTomeChest(Location location) {
        for(Location tomeLocation : this.tomeChestLocations) {
            if (location.getBlockX() == tomeLocation.getBlockX() && location.getBlockY() == tomeLocation.getBlockY() && location.getBlockZ() == tomeLocation.getBlockZ() && location.getWorld().equals(tomeLocation.getWorld())) {
                return true;
            }
        }

        return false;
    }

    private boolean hasBeenRevealed() {
        return this.plugin.getConfig().getBoolean("fourth_book_has_spawned", false);
    }

    private void markAsRevealed() {
        this.plugin.getConfig().set("fourth_book_has_spawned", true);
        this.plugin.saveConfig();
        this.plugin.getLogger().info("FourthBookRevealListener: Marked fourth book as revealed in config");
    }

    private void revealFourthBook(Chest chest, Player player) {
        ItemStack fourthBook = this.createRetributionBook();
        Inventory chestInventory = chest.getInventory();
        chestInventory.addItem(new ItemStack[]{fourthBook});
        this.markAsRevealed();
        player.sendMessage("§8§o[As you open the chest, an unfamiliar book suddenly materializes within...]");
        player.sendMessage("§4§o[The smell of old blood emanates from its pages...]");
        this.plugin.getLogger().info("FOURTH BOOK REVEALED: " + player.getName() + " opened tome chest at " + chest.getLocation().getBlockX() + ", " + chest.getLocation().getBlockY() + ", " + chest.getLocation().getBlockZ());
    }

    private ItemStack createRetributionBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta)book.getItemMeta();
        if (bookMeta != null) {
            bookMeta.setTitle("The Retribution 4/3");
            bookMeta.setAuthor("§4A vengeful hand...");
            bookMeta.setPages(new String[]{"§8§o[The writing in this book is unlike the previous three, it's is hurried and panic'd, the ink is smeared and the smell of blood rests faintly on the pages]§r\n\n§4The spirits are too lenient... Too soft...", "§0These disgusting, vial, works of evil could never be convinced to come back to the light...\n\n§0They must be dragged back to humanity, even if it's by, kicking and screaming.", "§0Give them a choice. Accept the light, or face eternal darkness.\n\n§0I will give them this choice, with these holy words:", "§7/§4hoc-vinculum-tibi-dirumpo-mala-creatura §7<§4Players-Name§7>"});
            book.setItemMeta(bookMeta);
        }

        return book;
    }
}
