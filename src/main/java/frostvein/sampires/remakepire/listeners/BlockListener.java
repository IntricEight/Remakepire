package frostvein.sampires.remakepire.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.SessionManager;

public class BlockListener implements Listener {
    private final RemakepirePlugin plugin;
    private final SessionManager sessionManager;

    public BlockListener(RemakepirePlugin plugin, SessionManager sessionManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!this.sessionManager.isSessionActive() && !this.sessionManager.isPreSession()) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot break blocks while the session is inactive.");
        } else {
            if (this.sessionManager.isPreSession()) {
                Material blockType = event.getBlock().getType();
                if (blockType == Material.IRON_ORE || blockType == Material.DEEPSLATE_IRON_ORE || blockType == Material.IRON_BLOCK || blockType == Material.RAW_IRON_BLOCK) {
                    event.setCancelled(true);
                    player.sendMessage("§cYou cannot mine iron while in Building Mode.");
                    return;
                }
            }

            if (this.plugin.getBatTransformationManager().isInBatForm(player)) {
                event.setCancelled(true);
                player.sendMessage("§cYou cannot break blocks while in bat form");
            } else if (event.getBlock().getType() == Material.BEACON) {
                event.setCancelled(true);
                player.sendMessage("§cYou are not allowed to break beacons");
            } else {
                if (event.getBlock().getType() == Material.NETHERITE_BLOCK) {
                    event.setDropItems(false);
                }

                if (event.getBlock().getType() == Material.NETHERITE_BLOCK) {
                    player.sendMessage("§7The block of silver breaks apart as you mine it, becoming useless.");
                }

            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!this.sessionManager.isSessionActive() && !this.sessionManager.isPreSession()) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot place blocks while the session is inactive.");
        } else if (this.plugin.getBatTransformationManager().isInBatForm(player)) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot place blocks while in bat form");
        } else {
            if (event.getBlock().getLocation().distance(this.plugin.getVampireRespawnLocation()) < 3.0) {
                if (!this.plugin.getVampireManager().isVampire(player)) {
                    player.sendMessage("§cThis is desecrated ground, you find yourself unable to place blocks here.");
                    event.setCancelled(true);
                    return;
                }

                player.sendMessage("§cWarning: This is the vampire spawn point, placing blocks here may cause issues for you and your fellow thralls.");
            }

            if (event.getBlock().getType() == Material.IRON_BLOCK) {
                event.getBlock().setType(Material.NETHERITE_BLOCK);
            }

        }
    }
}
