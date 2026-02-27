package frostvein.sampires.remakepire.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.InitGameManager;
import frostvein.sampires.remakepire.managers.InitGameManager.InitState;

public class InitGameListener implements Listener {
    private final RemakepirePlugin plugin;

    /**
     * Create an instance of the Initialize Game listener.
     *
     * @param plugin the host plugin object.
     */
    public InitGameListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Control interactions with the starting vampires choice UI.
     *
     * @param event a player clicks inside an inventory menu.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (this.plugin.getInitGameManager().isPlayerSelectionGUI(event.getView().getTitle())) {
            event.setCancelled(true);

            if (event.getWhoClicked() instanceof Player admin) {
                ItemStack clickedItem = event.getCurrentItem();

                if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                    ItemMeta meta = clickedItem.getItemMeta();

                    if (meta != null && meta.hasDisplayName()) {
                        String displayName = meta.getDisplayName();

                        if (clickedItem.getType() == Material.LIME_CONCRETE) {
                            this.plugin.getInitGameManager().handleGUIConfirmation(admin);
                        } else {
                            if (clickedItem.getType() == Material.ARROW) {
                                if (displayName.contains("Previous Page")) {
                                    this.plugin.getInitGameManager().handlePageChange(admin, -1);
                                    return;
                                }

                                if (displayName.contains("Next Page")) {
                                    this.plugin.getInitGameManager().handlePageChange(admin, 1);
                                    return;
                                }
                            }

                            if (displayName.contains(" - Vampire") || displayName.contains(" - Human")) {
                                String playerName = this.extractPlayerName(displayName);
                                if (playerName != null) {
                                    this.plugin.getInitGameManager().handlePlayerToggle(admin, playerName);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Cancel the game initialization if the selection window is closed.
     *
     * @param event a player closes an inventory window.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (this.plugin.getInitGameManager().isPlayerSelectionGUI(event.getView().getTitle())) {
            if (event.getPlayer() instanceof Player admin) {
                if (!this.plugin.getInitGameManager().isGUIRefreshInProgress(admin.getUniqueId())) {
                    if (this.plugin.getInitGameManager().getState(admin.getUniqueId()) == InitState.AWAITING_MODE_SELECTION) {
                        this.plugin.getInitGameManager().cancelInitialization(admin);
                    }
                }
            }
        }
    }

    /**
     * Block messages from reaching the wider server when selecting the number of starting vampires.
     *
     * @param event a player sends a chat message.
     */
    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        InitGameManager.InitState state = this.plugin.getInitGameManager().getState(player.getUniqueId());

        if (state == InitState.AWAITING_MIN_VAMPIRES || state == InitState.AWAITING_MAX_VAMPIRES) {
            event.setCancelled(true);
            String message = event.getMessage();

            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                boolean handled = false;

                if (state == InitState.AWAITING_MIN_VAMPIRES) {
                    handled = this.plugin.getInitGameManager().handleMinVampiresInput(player, message);
                } else if (state == InitState.AWAITING_MAX_VAMPIRES) {
                    handled = this.plugin.getInitGameManager().handleMaxVampiresInput(player, message);
                }

                if (!handled) {
                    player.sendMessage("§cError processing input. Please try again.");
                }
            });
        }
    }

    /**
     * Cancel the game initialization process if another command interrupts the prompting sequence.
     *
     * @param event a command is entered in the chat.
     */
    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage();

        if (this.plugin.getInitGameManager().isInternalCommand(command)) {
            event.setCancelled(true);

            if (!this.plugin.getInitGameManager().handleInternalCommand(player, command)) {
                player.sendMessage("§cError: Invalid initialization command.");
            }
        }
    }

    /**
     * Extract the player's name from their expanded title within the starting vampire choice GUI menu.
     *
     * @param displayName the player's display name with a tag on their team alignment.
     * @return The player's extracted name.
     */
    private String extractPlayerName(String displayName) {
        String stripped = displayName.replaceAll("§[0-9a-fk-or]", "");
        String[] parts = stripped.split(" - ");
        return parts.length >= 1 ? parts[0].trim() : null;
    }
}
