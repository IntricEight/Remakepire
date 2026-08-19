package frostvein.sampires.remakepire.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.SessionManager;

public class BlockListener implements Listener {
    private final RemakepirePlugin plugin;
    private final SessionManager sessionManager;

    /**
     * Create an instance of the Block listener.
     *
     * @param plugin the host plugin object.
     */
    public BlockListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.sessionManager = plugin.getSessionManager();
    }

    /**
     * Monitor the blocks being broken to check if it should be prevented, or if any special effects have come from the block's breaking.
     *
     * @param event a block being broken.
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (!this.sessionManager.isSessionActive() && !this.sessionManager.isPreSession()) {
            event.setCancelled(true);
            player.sendMessage(Component.text("You cannot break blocks while the session is inactive.", NamedTextColor.RED));

        } else {
            if (this.sessionManager.isPreSession()) {
                Material blockType = event.getBlock().getType();

                if (blockType == Material.IRON_ORE || blockType == Material.DEEPSLATE_IRON_ORE || blockType == Material.IRON_BLOCK || blockType == Material.RAW_IRON_BLOCK) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("You cannot mine iron while in Building Mode.", NamedTextColor.RED));
                    return;
                }
            }

            if (this.plugin.getBatTransformationManager().isInBatForm(player)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("You cannot break blocks while in bat form", NamedTextColor.RED));

            } else if (event.getBlock().getType() == Material.BEACON) {
                event.setCancelled(true);
                player.sendMessage(Component.text("You are not allowed to break beacons", NamedTextColor.RED));

            } else {
                // Stop placed silver blocks from being moved elsewhere
                if (event.getBlock().getType() == Material.NETHERITE_BLOCK) {
                    event.setDropItems(false);
                }

                if (event.getBlock().getType() == Material.NETHERITE_BLOCK) {
                    player.sendMessage(Component.text("The block of silver breaks apart as you mine it, becoming useless.", NamedTextColor.GRAY));
                }
            }
        }
    }

    /**
     * Monitor the blocks being placed to check if it should be prevented, or if any special effects have come from the block's placement.
     *
     * @param event a block being placed.
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (!this.sessionManager.isSessionActive() && !this.sessionManager.isPreSession()) {
            event.setCancelled(true);
            player.sendMessage(Component.text("You cannot place blocks while the session is inactive.", NamedTextColor.RED));

        } else if (this.plugin.getBatTransformationManager().isInBatForm(player)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("You cannot place blocks while in bat form", NamedTextColor.RED));

        } else {
            if (event.getBlock().getLocation().distance(this.plugin.getVampireRespawnLocation()) < 3) {
                // Prevent humans from messing with the vampire respawn location
                if (!this.plugin.getVampireManager().isVampire(player)) {
                    player.sendMessage(Component.text("This is desecrated ground, you find yourself unable to place blocks here.", NamedTextColor.RED));
                    event.setCancelled(true);
                    return;
                }

                player.sendMessage(Component.text("Warning: This is the vampire spawn point, placing blocks here may cause issues for you and your fellow thralls.", NamedTextColor.RED));
            }

            // Only replace iron blocks with the more resistant alternative if vampires are affected by it
            if (event.getBlock().getType() == Material.IRON_BLOCK && plugin.getConfigManager().doSilverBlocksWeakenVampires()) {
                event.getBlock().setType(Material.NETHERITE_BLOCK);
            }
        }
    }

    /**
     * Stop creeper explosions from destroying blocks.
     *
     * @param event a creeper explodes.
     */
    @EventHandler
    public void onCreeperExplode(EntityExplodeEvent event) {
        // Only stop the block destruction if it is from a Creeper
        if (!(event.getEntity() instanceof Creeper)) {
            return;
        }

        // Stop blocks from being destroyed in the explosion
        event.blockList().clear();
    }
}
