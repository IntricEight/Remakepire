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

    public TomeVampireRestrictionListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.tomeManager = plugin.getTomeManager();
        this.startTomeCheckTask();
    }

    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onTomePickup(EntityPickupItemEvent event) {
        LivingEntity var3 = event.getEntity();
        if (var3 instanceof Player player) {
            if (this.isRestrictedVampire(player)) {
                ItemStack item = event.getItem().getItemStack();
                if (this.isTome(item)) {
                    event.setCancelled(true);
                }

            }
        }
    }

    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity var3 = event.getWhoClicked();
        if (var3 instanceof Player player) {
            if (this.isRestrictedVampire(player)) {
                ItemStack currentItem = event.getCurrentItem();
                ItemStack cursorItem = event.getCursor();
                boolean currentIsTome = this.isTome(currentItem);
                boolean cursorIsTome = this.isTome(cursorItem);
                if (currentIsTome || cursorIsTome) {
                    event.setCancelled(true);
                    player.sendMessage("§4§lThe tome sears your flesh! You cannot touch such holy artifacts!");
                    Bukkit.getScheduler().runTask(this.plugin, () -> player.updateInventory());
                }

            }
        }
    }

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

    private void checkAndDropTomes(Player player) {
        boolean foundTome = false;

        for(int i = 0; i < player.getInventory().getSize(); ++i) {
            ItemStack item = player.getInventory().getItem(i);
            if (this.isTome(item)) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
                player.getInventory().setItem(i, (ItemStack)null);
                foundTome = true;
            }
        }

        ItemStack offhandItem = player.getInventory().getItemInOffHand();
        if (this.isTome(offhandItem)) {
            player.getWorld().dropItemNaturally(player.getLocation(), offhandItem);
            player.getInventory().setItemInOffHand((ItemStack)null);
            foundTome = true;
        }

        if (foundTome) {
            player.sendMessage("§cYour dark nature cannot bear to hold such holy knowledge...");
        }

    }

    private boolean isRestrictedVampire(Player player) {
        return this.vampireManager.isVampireStage2(player) || this.vampireManager.isVampireStage3(player);
    }

    public void forceDropTomesForPlayer(Player player) {
        if (this.isRestrictedVampire(player)) {
            this.checkAndDropTomes(player);
        }

    }

    private boolean isTome(ItemStack item) {
        if (item == null) {
            return false;
        } else if (item.getType() != Material.BOOK && item.getType() != Material.WRITTEN_BOOK) {
            return false;
        } else {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return false;
            } else {
                if (meta.hasDisplayName()) {
                    String displayName = meta.getDisplayName();
                    if (displayName.startsWith("§6Tome of ")) {
                        return true;
                    }
                }

                if (meta.hasLore()) {
                    List<String> lore = meta.getLore();
                    if (lore != null) {
                        for(String line : lore) {
                            if (line.contains("Tome Type: ")) {
                                return true;
                            }
                        }
                    }
                }

                if (item.getType() == Material.WRITTEN_BOOK && meta instanceof BookMeta) {
                    BookMeta bookMeta = (BookMeta)meta;
                    if (bookMeta.hasTitle()) {
                        String title = bookMeta.getTitle();
                        return this.tomeManager.isValidAbility(title);
                    }
                }

                return false;
            }
        }
    }
}
