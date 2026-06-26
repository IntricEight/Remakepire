package frostvein.sampires.remakepire.commands;

import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.beacons.BeaconSite;
import frostvein.sampires.remakepire.beacons.BeaconSite.BeaconState;
import frostvein.sampires.remakepire.managers.BeaconManager;
import frostvein.sampires.remakepire.managers.VampireManager;

public class HolySitesCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;
    private final BeaconManager beaconManager;
    private final VampireManager vampireManager;

    /**
     * Create an instance of the plugin's global beacon alignment distribution command handler.
     *
     * @param plugin the host plugin object.
     */
    public HolySitesCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.beaconManager = plugin.getBeaconManager();
        this.vampireManager = plugin.getVampireManager();
    }

    /**
     * Handle the command execution of retrieving data on the current status and distribution of the world's beacons.
     *
     * @return {@code true} if the command didn't trigger a fatal error.
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;

        } else if (!this.vampireManager.isHuman(player) && !this.vampireManager.isVampire(player)) {
            player.sendMessage("§cYou sense nothing from the spiritual realm...");
            return true;

        } else {
            Map<BeaconSite.BeaconState, Integer> stateStats = this.beaconManager.getStateStats();
            final int holyCount = stateStats.get(BeaconState.HOLY), desecratedCount = stateStats.get(BeaconState.DESECRATED), neutral = stateStats.get(BeaconState.NEUTRAL);
            int totalCount = holyCount + desecratedCount + neutral;

            // Modify the messages based on the player's alignment
            if (this.vampireManager.isHuman(player)) {
                player.sendMessage("§6§l=== BEACON STATUS ===");
                player.sendMessage("§aHoly Beacons: §e" + holyCount);
                player.sendMessage("§4Desecrated Beacons: §c" + desecratedCount);
                player.sendMessage("§7Neutral Beacons: §f" + neutral);
                player.sendMessage("§7Total Beacons: §e" + totalCount);

                if (holyCount == 0 && desecratedCount == 0) {
                    player.sendMessage("§7Neither light nor shadow has claimed any sites...");
                } else if (holyCount > 0 && desecratedCount == 0) {
                    player.sendMessage("§aThe light shines unopposed across the realm.");
                } else if (holyCount > desecratedCount) {
                    player.sendMessage("§eThe light holds strong, but darkness encroaches.");
                } else if (holyCount == desecratedCount) {
                    player.sendMessage("§6The balance of light and shadow is perfectly matched.");
                } else if (holyCount > 0 && holyCount < desecratedCount) {
                    player.sendMessage("§cDarkness spreads, but hope remains.");
                } else {
                    player.sendMessage("§4The realm has fallen into shadow... no sanctuaries remain.");
                }
            } else {
                player.sendMessage("§4§l=== BEACON STATUS ===");
                player.sendMessage("§4Desecrated Beacons: §c" + desecratedCount);
                player.sendMessage("§aHoly Beacons: §e" + holyCount);
                player.sendMessage("§7Neutral Beacons: §f" + neutral);
                player.sendMessage("§7Total Beacons: §e" + totalCount);

                if (desecratedCount == 0 && holyCount == 0) {
                    player.sendMessage("§7No sites of power have been claimed by either side...");
                } else if (desecratedCount > 0 && holyCount == 0) {
                    player.sendMessage("§4Darkness reigns supreme across the land.");
                } else if (desecratedCount > holyCount) {
                    player.sendMessage("§5The shadow grows strong, but light still resists.");
                } else if (desecratedCount == holyCount) {
                    player.sendMessage("§6The forces of darkness and light are evenly matched.");
                } else if (desecratedCount > 0 && desecratedCount < holyCount) {
                    player.sendMessage("§cThe cursed beacons spread their influence slowly...");
                } else {
                    player.sendMessage("§cThe light burns too brightly... our sanctuaries are none.");
                }
            }

            return true;
        }
    }
}
