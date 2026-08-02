package frostvein.sampires.remakepire.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.PermadeathManager;
import frostvein.sampires.remakepire.managers.VampireManager;
import frostvein.sampires.remakepire.managers.PermadeathManager.PermadeathMode;

public class PermadeathCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;
    private final PermadeathManager permadeathManager;
    private final VampireManager vampireManager;

    /**
     * Create an instance of the plugin's permadeath setting command handler.
     *
     * @param plugin the host plugin object.
     */
    public PermadeathCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.permadeathManager = plugin.getPermadeathManager();
        this.vampireManager = plugin.getVampireManager();
    }

    /**
     * Handle the command execution of setting a human's permadeath setting.
     *
     * @return {@code true}
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;

        } else if (!this.vampireManager.isHuman(player)) {
            player.sendMessage("§cOnly humans can use the permadeath setting.");
            return true;

        } else if (args.length == 0) {
            this.showCurrentStatus(player);
            return true;

        } else {
            switch (args[0].toLowerCase()) {
                case "on":
                    this.permadeathManager.setPermadeathMode(player, PermadeathMode.ON);
                    player.sendMessage("§c§lPERMADEATH: ON");
                    player.sendMessage("§7You have chosen the path of sacrifice.");
                    player.sendMessage("§7If a vampire with turning enabled kills you,");
                    player.sendMessage("§7you will die permanently instead of becoming a vampire.");
                    break;

                case "off":
                    this.permadeathManager.setPermadeathMode(player, PermadeathMode.OFF);
                    player.sendMessage("§a§lPERMADEATH: OFF");
                    player.sendMessage("§7You will become a vampire if turned by one,");
                    player.sendMessage("§7following the normal vampire conversion rules.");
                    break;

                case "absolute":
                    this.permadeathManager.setPermadeathMode(player, PermadeathMode.ABSOLUTE);
                    player.sendMessage("§4§lPERMADEATH: ABSOLUTE");
                    player.sendMessage("§cYou have chosen the path of ultimate sacrifice.");
                    player.sendMessage("§7If you are killed by ANY means, you will die permanently");
                    player.sendMessage("§4This is the most extreme setting - use with caution.");
                    break;

                default:
                    player.sendMessage("§cInvalid option. Use: §e/pow permadeath <on | off | absolute>");
                    player.sendMessage("§7  on §8- Die permanently if vampire tries to turn you");
                    player.sendMessage("§7  off §8- Become a vampire if turned (default)");
                    player.sendMessage("§7  absolute §8- Die permanently from ANY death");
                    return true;
            }

            return true;
        }
    }

    /**
     * Inform the player of their current permadeath setting.
     *
     * @param player the human checking the setting.
     */
    private void showCurrentStatus(Player player) {
        PermadeathManager.PermadeathMode currentMode = this.permadeathManager.getPermadeathMode(player);
        player.sendMessage("§6§l=== PERMADEATH STATUS ===");

        switch (currentMode) {
            case OFF:
                player.sendMessage("§7Current setting: §aOFF");
                player.sendMessage("§7You will become a vampire if turned by one.");
                break;
            case ON:
                player.sendMessage("§7Current setting: §cON");
                player.sendMessage("§7You will die permanently if a vampire tries to turn you.");
                break;
            case ABSOLUTE:
                player.sendMessage("§7Current setting: §4ABSOLUTE");
                player.sendMessage("§7You will die permanently from ANY cause of death.");
        }

        player.sendMessage("");
        player.sendMessage("§7Change with: §e/pow permadeath <on | off | absolute>");
    }
}
