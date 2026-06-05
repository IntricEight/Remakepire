package frostvein.sampires.remakepire.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.ConfigGuiManager;

public class ConfigGuiListener  implements Listener {
    private final RemakepirePlugin plugin;

    public ConfigGuiListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Control interactions with the config modification GUI.
     *
     * @param event a player clicks inside an inventory menu.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ConfigGuiManager.CONFIG_GUI_TITLE)) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();

            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                // Retrieve the command that the admin is interacting with
                String commandName = ConfigGuiManager.getCommandNameFromItem(clickedItem.getType());

                // Execute the different commands as the admin interacts with their GUI item
                this.plugin.getConfigGuiManager().runConfigCommand(commandName);

                // Refresh the GUI item to reflect the new change.
                this.plugin.getConfigGuiManager().refreshConfigGuiItem(commandName);
            }
        }
    }
}