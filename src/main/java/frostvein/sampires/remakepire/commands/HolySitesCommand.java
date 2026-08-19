package frostvein.sampires.remakepire.commands;

import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.beacons.BeaconSite;
import frostvein.sampires.remakepire.beacons.BeaconSite.BeaconState;

public class HolySitesCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;

    /**
     * Create an instance of the plugin's global beacon alignment distribution command handler.
     *
     * @param plugin the host plugin object.
     */
    public HolySitesCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle the command execution of retrieving data on the current status and distribution of the world's beacons.
     *
     * @return {@code true} if the command didn't trigger a fatal error.
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;

        } else if (!this.plugin.getVampireManager().isHuman(player) && !this.plugin.getVampireManager().isVampire(player)) {
            player.sendMessage(Component.text("You sense nothing from the spiritual realm...", NamedTextColor.RED));
            return true;

        } else {
            Map<BeaconSite.BeaconState, Integer> stateStats = this.plugin.getBeaconManager().getStateStats();
            final int holyCount = stateStats.get(BeaconState.HOLY), desecratedCount = stateStats.get(BeaconState.DESECRATED), neutral = stateStats.get(BeaconState.NEUTRAL);
            int totalCount = holyCount + desecratedCount + neutral;

            // Modify the messages based on the player's alignment
            if (this.plugin.getVampireManager().isHuman(player)) {
                player.sendMessage(Component.text("=== BEACON STATUS ===", NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD));
                player.sendMessage(Component.text("Holy Beacons: ", NamedTextColor.GREEN)
                        .append(Component.text(holyCount, NamedTextColor.YELLOW)));
                player.sendMessage(Component.text("Desecrated Beacons: ", NamedTextColor.DARK_RED)
                        .append(Component.text(desecratedCount, NamedTextColor.RED)));
                player.sendMessage(Component.text("Neutral Beacons: ", NamedTextColor.GRAY)
                        .append(Component.text(neutral, NamedTextColor.WHITE)));
                player.sendMessage(Component.text("Total Beacons: ", NamedTextColor.GRAY)
                        .append(Component.text(totalCount, NamedTextColor.YELLOW)));

                if (holyCount == 0 && desecratedCount == 0) {
                    player.sendMessage(Component.text("Neither light nor shadow has claimed any sites...", NamedTextColor.GRAY));
                } else if (holyCount > 0 && desecratedCount == 0) {
                    player.sendMessage(Component.text("The light shines unopposed across the realm.", NamedTextColor.GREEN));
                } else if (holyCount > desecratedCount) {
                    player.sendMessage(Component.text("The light holds strong, but darkness encroaches.", NamedTextColor.YELLOW));
                } else if (holyCount == desecratedCount) {
                    player.sendMessage(Component.text("The balance of light and shadow is perfectly matched.", NamedTextColor.GOLD));
                } else if (holyCount > 0 && holyCount < desecratedCount) {
                    player.sendMessage(Component.text("Darkness spreads, but hope remains.", NamedTextColor.RED));
                } else {
                    player.sendMessage(Component.text("The realm has fallen into shadow... no sanctuaries remain.", NamedTextColor.DARK_RED));
                }
            } else {
                player.sendMessage(Component.text("=== BEACON STATUS ===", NamedTextColor.DARK_RED)
                        .decorate(TextDecoration.BOLD));
                player.sendMessage(Component.text("Desecrated Beacons: ", NamedTextColor.DARK_RED)
                        .append(Component.text(desecratedCount, NamedTextColor.RED)));
                player.sendMessage(Component.text("Holy Beacons: ", NamedTextColor.GREEN)
                        .append(Component.text(holyCount, NamedTextColor.YELLOW)));
                player.sendMessage(Component.text("Neutral Beacons: ", NamedTextColor.GRAY)
                        .append(Component.text(neutral, NamedTextColor.WHITE)));
                player.sendMessage(Component.text("Total Beacons: ", NamedTextColor.GRAY)
                        .append(Component.text(totalCount, NamedTextColor.YELLOW)));

                if (desecratedCount == 0 && holyCount == 0) {
                    player.sendMessage(Component.text("No sites of power have been claimed by either side...", NamedTextColor.GRAY));
                } else if (desecratedCount > 0 && holyCount == 0) {
                    player.sendMessage(Component.text("Darkness reigns supreme across the land.", NamedTextColor.DARK_RED));
                } else if (desecratedCount > holyCount) {
                    player.sendMessage(Component.text("The shadow grows strong, but light still resists.", NamedTextColor.DARK_PURPLE));
                } else if (desecratedCount == holyCount) {
                    player.sendMessage(Component.text("The forces of darkness and light are evenly matched.", NamedTextColor.GOLD));
                } else if (desecratedCount > 0 && desecratedCount < holyCount) {
                    player.sendMessage(Component.text("The cursed beacons spread their influence slowly...", NamedTextColor.RED));
                } else {
                    player.sendMessage(Component.text("The light burns too brightly... our sanctuaries are none.", NamedTextColor.RED));
                }
            }

            return true;
        }
    }
}
