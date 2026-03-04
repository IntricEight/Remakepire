package frostvein.sampires.remakepire.listeners;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.TomeManager;
import frostvein.sampires.remakepire.managers.VampireManager;

public class TomeVampireRestrictionListener implements Listener {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final TomeManager tomeManager;

    /**
     * Create an instance of the Tome Vampire Restriction listener.
     *
     * @param plugin the host plugin object.
     */
    public TomeVampireRestrictionListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.tomeManager = plugin.getTomeManager();
        this.startTomeCheckTask();
    }

    /**
     * Prevent higher vampires from picking up holy tomes.
     *
     * @param event an entity picks up an item.
     */
    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onTomePickup(EntityPickupItemEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity instanceof Player player) {
            if (this.isRestrictedVampire(player)) {
                ItemStack item = event.getItem().getItemStack();

                if (this.isTome(item)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    /**
     * Prevent higher vampires from taking holy tomes from other inventories.
     *
     * @param event a player clicks inside an inventory menu.
     */
    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity entity = event.getWhoClicked();

        if (entity instanceof Player player) {
            if (this.isRestrictedVampire(player)) {
                ItemStack currentItem = event.getCurrentItem();
                ItemStack cursorItem = event.getCursor();

                if (this.isTome(currentItem) || this.isTome(cursorItem)) {
                    event.setCancelled(true);
                    player.sendMessage("§3§lThe tome sears your flesh! You cannot touch such holy artifacts!");
                    Bukkit.getScheduler().runTask(this.plugin, () -> player.updateInventory());
                }
            }
        }
    }

    /**
     * Begin regularly checking if a vampire has tomes in their inventory.
     */
    private void startTomeCheckTask() {
        (new BukkitRunnable() {
            public void run() {
                for(Player player : Bukkit.getOnlinePlayers()) {
                    if (TomeVampireRestrictionListener.this.isRestrictedVampire(player)) {
                        TomeVampireRestrictionListener.this.checkAndDropTomes(player);
                    }
                }
            }
        }).runTaskTimer(this.plugin, 20L, 20L);
    }

    /**
     * Drop any holy tomes within the vampire's inventory.
     *
     * @param player the vampire whose inventory is being checked.
     */
    private void checkAndDropTomes(Player player) {
        boolean foundTome = false;

        for(int i = 0; i < player.getInventory().getSize(); ++i) {
            ItemStack item = player.getInventory().getItem(i);

            if (this.isTome(item)) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
                player.getInventory().setItem(i, null);
                foundTome = true;
            }
        }

        ItemStack offhandItem = player.getInventory().getItemInOffHand();

        if (this.isTome(offhandItem)) {
            player.getWorld().dropItemNaturally(player.getLocation(), offhandItem);
            player.getInventory().setItemInOffHand(null);
            foundTome = true;
        }

        if (foundTome) {
            player.sendMessage("§cYour dark nature cannot bear to hold such holy knowledge...");
        }
    }

    /**
     * Determine if the player should be prevented from interacting with tomes.
     *
     * @param player the player attempting to interact with a tome.
     * @return {@code true} if the player is a higher vampire.
     */
    private boolean isRestrictedVampire(Player player) {
        return this.vampireManager.isVampireStage2(player) || this.vampireManager.isVampireStage3(player);
    }

    /**
     * Force higher vampires to drop any tomes that they are holding.
     *
     * @param player the player being checked.
     */
    public void forceDropTomesForPlayer(Player player) {
        if (this.isRestrictedVampire(player)) {
            this.checkAndDropTomes(player);
        }
    }

    /**
     * Determine if an item is a holy tome that grants abilities.
     *
     * @param item the item being checked.
     * @return {@code true} if the item is a tome book.
     */
    private boolean isTome(ItemStack item) {
        if (item == null) {
            return false;

        } else if (item.getType() != Material.BOOK && item.getType() != Material.WRITTEN_BOOK) {
            return false;

        } else {
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                if (meta.hasDisplayName()) {
                    String displayName = meta.getDisplayName();

                    if (displayName.startsWith("§6Tome of ")) {
                        return true;
                    }
                }

                if (meta.hasLore()) {
                    List<String> lore = meta.getLore();

                    if (lore != null) {
                        for (String line : lore) {
                            if (line.contains("Tome Type: ")) {
                                return true;
                            }
                        }
                    }
                }

                if (item.getType() == Material.WRITTEN_BOOK && meta instanceof BookMeta bookMeta) {
                    if (bookMeta.hasTitle()) {
                        String title = bookMeta.getTitle();
                        return this.tomeManager.isValidAbility(title);
                    }
                }
            }

            return false;
        }
    }
}
