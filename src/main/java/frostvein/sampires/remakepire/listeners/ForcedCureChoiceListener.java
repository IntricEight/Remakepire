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
import frostvein.sampires.remakepire.managers.ForcedCureChoiceManager;

public class ForcedCureChoiceListener implements Listener {
    private final RemakepirePlugin plugin;

    /**
     * Create an instance of the Forced Cure Choice listener.
     *
     * @param plugin the host plugin object.
     */
    public ForcedCureChoiceListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Control interactions with the cure choice GUI.
     *
     * @param event a player clicks inside an inventory menu.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ForcedCureChoiceManager.CURE_CHOICE_TITLE)) {
            event.setCancelled(true);

            if (event.getWhoClicked() instanceof Player player) {
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

    /**
     * Give the player the option to reopen the forced cure choice window.
     *
     * @param event a player closes an inventory window.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(ForcedCureChoiceManager.CURE_CHOICE_TITLE)) {
            if (event.getPlayer() instanceof Player player) {
                if (this.plugin.getForcedCureChoiceManager().hasPendingCure(player)) {
                    player.sendMessage("");
                    player.sendMessage("§4This is a decision you cannot run from, monster.");
                    player.sendMessage("§7The spirits have come knocking, and they are joined by death.");
                    player.sendMessage("§7Say your piece, and when you ready to make your decision,");

                    TextComponent message = new TextComponent("§e§l[CLICK HERE]");
                    message.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/pow reopen"));
                    message.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder("§6Click to reopen the choice menu")).create()));

                    player.spigot().sendMessage(message);
                    player.sendMessage("");
                }
            }
        }
    }

    /**
     * Give the player the option to reopen the forced cure choice window when they rejoin the game.
     *
     * @param event a player joining the world.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (this.plugin.getForcedCureChoiceManager().hasPendingCure(player)) {
            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                if (player.isOnline() && this.plugin.getForcedCureChoiceManager().hasPendingCure(player)) {
                    player.sendMessage("");
                    player.sendMessage("§4This is a decision you cannot run from, monster.");
                    player.sendMessage("§7The spirits have come knocking, and they are joined by death.");
                    player.sendMessage("§7Say your piece, and when you ready to make your decision,");

                    TextComponent message = new TextComponent("§e§l[CLICK HERE]");
                    message.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/pow reopen"));
                    message.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder("§6Click to reopen the choice menu")).create()));
                    player.spigot().sendMessage(message);
                    player.sendMessage("");
                }
            }, 20L);
        }
    }

    /**
     * Prevent the player from moving once a force cure has been used on them until a choice is maded.
     *
     * @param event a player moving.
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (this.plugin.getForcedCureChoiceManager().hasPendingCure(player) && (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getY() != event.getTo().getY() || event.getFrom().getZ() != event.getTo().getZ())) {
            event.setCancelled(true);
        }
    }
}
