package frostvein.sampires.remakepire.commands;

import java.util.Arrays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class PowCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;
    private final CommandHandler adminHandler;
    private final VampireAbilityCommand vampireAbilityCommand;
    private final TomeAbilityCommand tomeAbilityCommand;
    private final CheckLivesCommand checkLivesCommand;
    private final ForcedCureReopenCommand forceCureReopenCommand;
    private final HolySitesCommand beaconStatusCommand;
    private final TexturePackCommand texturePackCommand;
    private final StakeSelfCommand stakeSelfCommand;
    private final PermadeathCommand permadeathCommand;
    private final ToggleTurningCommand turningCommand;
    private final PendingMessageCommand sendMessageCommand;

    /**
     * Create an instance of the plugin's custom command heading manager.
     *
     * @param plugin the host plugin object.
     */
    public PowCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.adminHandler = new CommandHandler(plugin);
        this.vampireAbilityCommand = new VampireAbilityCommand(plugin);
        this.tomeAbilityCommand = new TomeAbilityCommand(plugin);
        this.checkLivesCommand = new CheckLivesCommand(plugin);
        this.forceCureReopenCommand = new ForcedCureReopenCommand(plugin);
        this.beaconStatusCommand = new HolySitesCommand(plugin);
        this.texturePackCommand = new TexturePackCommand(plugin);
        this.stakeSelfCommand = new StakeSelfCommand(plugin);
        this.permadeathCommand = new PermadeathCommand(plugin);
        this.turningCommand = new ToggleTurningCommand(plugin);
        this.sendMessageCommand = new PendingMessageCommand(plugin);
    }

    /**
     * Handle the command execution of the custom plugin commands.
     *
     * @return {@code true} if the command didn't trigger a fatal error.
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            this.sendHelp(sender);
            return true;

        } else {
            String subCommand = args[0].toLowerCase();
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

            switch (subCommand) {
                case "admin":
                    // Access the admin commands
                    return this.handleAdminCommand(sender, subArgs);

                case "vability":
                    // Access the vampire ability commands
                    return this.vampireAbilityCommand.onCommand(sender, command, label, subArgs);

                case "tome":
                    // Access the tome ability commands
                    return this.tomeAbilityCommand.onCommand(sender, command, label, subArgs);

                case "checklives":
                    // Check how many lives this player has remaining
                    return this.checkLivesCommand.onCommand(sender, command, label, subArgs);

                case "beaconstatus":
                case "holysites":
                case "holy":
                    // Check the status of the beacons in the world
                    return this.beaconStatusCommand.onCommand(sender, command, label, subArgs);

                case "texture":
                case "texturepack":
                case "resourcepack":
                    // Force the application of a plugin texture pack
                    return this.texturePackCommand.onCommand(sender, command, label, subArgs);

                case "toggle_permadeath":
                case "toggle-permadeath":
                case "togglepermadeath":
                case "permadeath":
                    // Change or check the permadeath setting of this player
                    return this.permadeathCommand.onCommand(sender, command, label, subArgs);

                case "toggle-turning":
                case "turning":
                    // Toggle whether vampire killings will attempt to turn the victim into a vampire
                    return this.turningCommand.onCommand(sender, command, label, subArgs);

                case "sendmessage":
                case "sendpendingmessage":
                    // Force a message through the message prevention system
                    return this.sendMessageCommand.onCommand(sender, command, label, subArgs);

                case "reopen":
                case "forcedcure-reopen":
                    // Reopen the forced cure choice menu
                    return this.forceCureReopenCommand.onCommand(sender, command, label, subArgs);

                case "stake-myself":
                    // Allow a vampire to stake themselves
                    return this.stakeSelfCommand.onCommand(sender, command, label, subArgs);

                case "help":
                    // Print out a descriptive list of the commands available to this player
                    this.sendHelp(sender);
                    return true;

                default:
                    sender.sendMessage(Component.text("Unknown subcommand: " + subCommand, NamedTextColor.RED));
                    sender.sendMessage(Component.text("Use ", NamedTextColor.GRAY)
                            .append(Component.text("/pow help", NamedTextColor.YELLOW))
                            .append(Component.text(" for a list of commands", NamedTextColor.GRAY))
                    );
                    return true;
            }
        }
    }

    /**
     * Determine if the sender has admin permissions and can use admin commands.
     *
     * @param sender the player sending the command.
     * @param args the arguments attached to the command.
     * @return {@code true} if the command was processed without failure.
     */
    private boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vampiresmp.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use admin commands.", NamedTextColor.RED));
            return true;

        } else if (args.length == 0) {
            this.adminHandler.sendAdminHelp(sender);
            return true;

        } else {
            final String adminSubCommand = args[0].toLowerCase();
            final String[] adminArgs = Arrays.copyOfRange(args, 1, args.length);

            Command dummyCommand = new BukkitCommand(adminSubCommand) {
                public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                    return false;
                }
            };

            return this.adminHandler.onCommand(sender, dummyCommand, adminSubCommand, adminArgs);
        }
    }

    /**
     * Print to the sender a list of available commands they can run using the pow command.
     *
     * @param sender the player sending the command.
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(Component.text("=== VampireSMP Commands ===", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));

        // Only let the sender know about the admin option if they have access to it
        if (sender.hasPermission("vampiresmp.admin")) {
            sender.sendMessage(Component.text("/pow admin", NamedTextColor.YELLOW)
                    .append(Component.text(" - Admin commands (requires permission)", NamedTextColor.GRAY)));
        }

        CommandHandler.sendCommandInstruction(sender, "/pow vability <name>", "Use vampire abilities");
        CommandHandler.sendCommandInstruction(sender, "/pow tome <name>", "Use tome abilities (humans)");
        CommandHandler.sendCommandInstruction(sender, "/voluntate-mea-hoc-nefandum-vinculum-abicio", "Cure yourself from vampirism");
        CommandHandler.sendCommandInstruction(sender, "/hoc-vinculum-tibi-dirumpo-mala-creatura <player>", "Force cure a vampire");
        CommandHandler.sendCommandInstruction(sender, "/pow checklives", "Check how many lives this player has remaining");
        CommandHandler.sendCommandInstruction(sender, "/pow beaconstatus", "Check beacon spiritual influence");
        CommandHandler.sendCommandInstruction(sender, "/pow texture", "Apply VampireSMP texture pack");
        CommandHandler.sendCommandInstruction(sender, "/pow permadeath <on | off | absolute>", "Set permadeath preference");
        CommandHandler.sendCommandInstruction(sender, "/pow toggle-turning", "Toggle vampire turning ability");
        CommandHandler.sendCommandInstruction(sender, "/pow sendmessage", "Send pending chat message");
    }
}
