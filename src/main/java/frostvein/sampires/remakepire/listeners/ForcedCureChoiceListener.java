package frostvein.sampires.remakepire.listeners;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class ForcedCureChoiceListener implements Listener {
    private final RemakepirePlugin plugin;

    public ForcedCureChoiceListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle() != null && event.getView().getTitle().equals("§4§lYour Fate Awaits...")) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player)event.getWhoClicked();
                if (!this.plugin.getForcedCureChoiceManager().hasPendingCure(player)) {
                    player.closeInventory();
                } else {
                    ItemStack clickedItem = event.getCurrentItem();
                    if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                        if (clickedItem.getType() == Material.PLAYER_HEAD) {
                            this.plugin.getForcedCureChoiceManager().handleHumanityChoice(player);
                        } else if (clickedItem.getType() == Material.SKELETON_SKULL) {
                            this.plugin.getForcedCureChoiceManager().handleDeathChoice(player);
                        }

                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle() != null && event.getView().getTitle().equals("§4§lYour Fate Awaits...")) {
            if (event.getPlayer() instanceof Player) {
                Player player = (Player)event.getPlayer();
                if (this.plugin.getForcedCureChoiceManager().hasPendingCure(player)) {
                    player.sendMessage("");
                    player.sendMessage("§4This is a decision you cannot run from monster.");
                    player.sendMessage("§7The spirits have come knocking, and they are joined by death.");
                    player.sendMessage("§7Say your peace, and when you ready to make your decision,");
                    TextComponent message = new TextComponent("§e§l[CLICK HERE]");
                    message.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/pow reopen"));
                    message.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder("§6Click to reopen the choice menu")).create()));
                    player.spigot().sendMessage(message);
                    player.sendMessage("");
                }

            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (this.plugin.getForcedCureChoiceManager().hasPendingCure(player)) {
            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                if (player.isOnline() && this.plugin.getForcedCureChoiceManager().hasPendingCure(player)) {
                    player.sendMessage("");
                    player.sendMessage("§4This is a decision you cannot run from monster.");
                    player.sendMessage("§7The spirits have come knocking, and they are joined by death.");
                    player.sendMessage("§7Say your peace, and when you ready to make your decision,");
                    TextComponent message = new TextComponent("§e§l[CLICK HERE]");
                    message.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/pow reopen"));
                    message.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder("§6Click to reopen the choice menu")).create()));
                    player.spigot().sendMessage(message);
                    player.sendMessage("");
                }

            }, 20L);
        }

    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (this.plugin.getForcedCureChoiceManager().hasPendingCure(player) && (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getY() != event.getTo().getY() || event.getFrom().getZ() != event.getTo().getZ())) {
            event.setCancelled(true);
        }

    }
}
