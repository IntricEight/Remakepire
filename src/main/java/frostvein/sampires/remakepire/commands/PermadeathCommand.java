package frostvein.sampires.remakepire.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.PermadeathManager;
import frostvein.sampires.remakepire.managers.PermadeathManager.PermadeathMode;

public class PermadeathCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;
    private final PermadeathManager permadeathManager;

    /**
     * Create an instance of the plugin's permadeath setting command handler.
     *
     * @param plugin the host plugin object.
     */
    public PermadeathCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.permadeathManager = plugin.getPermadeathManager();
    }

    /**
     * Handle the command execution of setting a human's permadeath setting.
     *
     * @return {@code true}
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;

        } else if (!this.plugin.getVampireManager().isHuman(player)) {
            player.sendMessage(Component.text("Only humans can use the permadeath setting.", NamedTextColor.RED));
            return true;

        } else if (args.length == 0) {
            this.showCurrentStatus(player);
            return true;

        } else {
            switch (args[0].toLowerCase()) {
                case "on":
                    this.permadeathManager.setPermadeathMode(player, PermadeathMode.ON);
                    player.sendMessage(Component.text("PERMADEATH: ON", NamedTextColor.RED)
                            .decorate(TextDecoration.BOLD));
                    player.sendMessage(Component.text("You have chosen the path of sacrifice.", NamedTextColor.GRAY));
                    player.sendMessage(Component.text("If a vampire with turning enabled kills you,", NamedTextColor.GRAY));
                    player.sendMessage(Component.text("you will die permanently instead of becoming a vampire.", NamedTextColor.GRAY));
                    break;

                case "off":
                    this.permadeathManager.setPermadeathMode(player, PermadeathMode.OFF);
                    player.sendMessage(Component.text("PERMADEATH: OFF", NamedTextColor.GREEN)
                            .decorate(TextDecoration.BOLD));
                    player.sendMessage(Component.text("You will become a vampire if turned by one,", NamedTextColor.GRAY));
                    player.sendMessage(Component.text("following the normal vampire conversion rules.", NamedTextColor.GRAY));
                    break;

                case "absolute":
                    this.permadeathManager.setPermadeathMode(player, PermadeathMode.ABSOLUTE);
                    player.sendMessage(Component.text("PERMADEATH: ABSOLUTE", NamedTextColor.DARK_RED)
                            .decorate(TextDecoration.BOLD));
                    player.sendMessage(Component.text("You have chosen the path of ultimate sacrifice.", NamedTextColor.RED));
                    player.sendMessage(Component.text("If you are killed by ANY means, you will die permanently", NamedTextColor.GRAY));
                    player.sendMessage(Component.text("This is the most extreme setting - use with caution.", NamedTextColor.DARK_RED));
                    break;

                default:
                    player.sendMessage(Component.text("Invalid option. Use: ", NamedTextColor.RED)
                            .append(Component.text("/pow permadeath <on | off | absolute>", NamedTextColor.YELLOW)));
                    CommandHandler.sendCommandCorrection(player, "  on", "Die permanently if vampire tries to turn you");
                    CommandHandler.sendCommandCorrection(player, "  off", "Become a vampire if turned (default)");
                    CommandHandler.sendCommandCorrection(player, "  absolute", "Die permanently from ANY death");
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
        player.sendMessage(Component.text("=== PERMADEATH STATUS ===", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));

        switch (currentMode) {
            case OFF:
                player.sendMessage(Component.text("Current setting: ", NamedTextColor.GRAY)
                        .append(Component.text("OFF", NamedTextColor.GREEN)));
                player.sendMessage(Component.text("You will become a vampire if turned by one.", NamedTextColor.GRAY));
                break;

            case ON:
                player.sendMessage(Component.text("Current setting: ", NamedTextColor.GRAY)
                        .append(Component.text("ON", NamedTextColor.RED)));
                player.sendMessage(Component.text("You will die permanently if a vampire tries to turn you.", NamedTextColor.GRAY));
                break;

            case ABSOLUTE:
                player.sendMessage(Component.text("Current setting: ", NamedTextColor.GRAY)
                        .append(Component.text("ABSOLUTE", NamedTextColor.DARK_RED)));
                player.sendMessage(Component.text("You will die permanently from ANY cause of death.", NamedTextColor.GRAY));
        }

        player.sendMessage("");
        player.sendMessage(Component.text("Change with: ", NamedTextColor.GRAY)
                .append(Component.text("/pow permadeath <on | off | absolute>", NamedTextColor.YELLOW)));
    }
}
