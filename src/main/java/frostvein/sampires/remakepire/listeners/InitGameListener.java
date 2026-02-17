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

    public InitGameListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (this.plugin.getInitGameManager().isPlayerSelectionGUI(event.getView().getTitle())) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
                Player admin = (Player)event.getWhoClicked();
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

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (this.plugin.getInitGameManager().isPlayerSelectionGUI(event.getView().getTitle())) {
            if (event.getPlayer() instanceof Player) {
                Player admin = (Player)event.getPlayer();
                if (!this.plugin.getInitGameManager().isGUIRefreshInProgress(admin.getUniqueId())) {
                    if (this.plugin.getInitGameManager().getState(admin.getUniqueId()) == InitState.AWAITING_MODE_SELECTION) {
                        this.plugin.getInitGameManager().cancelInitialization(admin);
                    }

                }
            }
        }
    }

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

    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage();
        if (this.plugin.getInitGameManager().isInternalCommand(command)) {
            event.setCancelled(true);
            boolean handled = this.plugin.getInitGameManager().handleInternalCommand(player, command);
            if (!handled) {
                player.sendMessage("§cError: Invalid initialization command.");
            }
        }

    }

    private String extractPlayerName(String displayName) {
        String stripped = displayName.replaceAll("§[0-9a-fk-or]", "");
        String[] parts = stripped.split(" - ");
        return parts.length >= 1 ? parts[0].trim() : null;
    }
}
