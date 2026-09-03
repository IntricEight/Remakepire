package frostvein.sampires.remakepire.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class PowCommand implements CommandExecutor, TabCompleter {
    private final RemakepirePlugin plugin;
    private final CommandHandler adminHandler;
    private final VampireAbilityCommand abilityCommand;
    private final TomeAbilityCommand tomeCommand;
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
        this.abilityCommand = new VampireAbilityCommand(plugin);
        this.tomeCommand = new TomeAbilityCommand(plugin);
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
                    return this.abilityCommand.onCommand(sender, command, label, subArgs);

                case "tome":
                    // Access the tome ability commands
                    return this.tomeCommand.onCommand(sender, command, label, subArgs);

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
            return true;

        } else {
            String adminSubCommand = args[0].toLowerCase();
            String[] adminArgs = Arrays.copyOfRange(args, 1, args.length);

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

    /**
     * Create the list of autocorrecting options for pow commands as they are written out in the command line.
     *
     * @param command the previous word in the argument list.
     * @return A {@code List} of options for the autocomplete to suggest.
     */
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Stores the autocomplete options that will be displayed
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("vability", "tome", "checklives", "beaconstatus", "permadeath", "toggle-turning", "help"));

            if (sender instanceof Player player && this.plugin.getVampireManager().isVampire(player)) {
                subCommands.add("stake-myself");
            }

            if (sender.hasPermission("vampiresmp.admin")) {
                subCommands.addFirst("admin");
            }

            return subCommands.stream().filter((s) -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());

        } else if (args.length == 2 && args[0].equalsIgnoreCase("permadeath")) {
            List<String> permadeathOptions = Arrays.asList("on", "off", "absolute");
            return permadeathOptions.stream().filter((s) -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());

        } else {
            if (args.length >= 2 && args[0].equalsIgnoreCase("admin")) {
                if (!sender.hasPermission("vampiresmp.admin")) {
                    return new ArrayList<>();
                }

                if (args.length == 2) {
                    List<String> adminCommands = Arrays.asList("help", "init", "session", "vampire", "beacon", "vampirecooldowns", "cooldownvampires", "resettomecooldowns", "cooldownresettomes", "break_warning", "givetome", "select_tomes", "give_cure_book", "stash_cure_book", "distributetomes", "clearbloodmoonbuffs", "make_incurable", "fixattributes", "removeendermen", "removecreepers", "setupplayer", "spawnanimals", "addtomechest", "removetomechest", "listtomechests", "resetplayer", "set_vampire_spawn", "config");
                    return adminCommands.stream().filter((s) -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 3 && args[1].equalsIgnoreCase("session")) {
                    List<String> sessionOptions = Arrays.asList("start", "pause", "end", "prime", "resume", "building");
                    return sessionOptions.stream().filter((s) -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                }

                // Set the player autofill options for 'vampire'
                if (args.length == 3 && args[1].equalsIgnoreCase("vampire")) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter((s) -> s.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 4 && args[1].equalsIgnoreCase("vampire")) {
                    List<String> vampireOptions = Arrays.asList("human", "1", "2", "3", "turn", "clearcap", "clearban");
                    return vampireOptions.stream().filter((s) -> s.startsWith(args[3].toLowerCase())).collect(Collectors.toList());
                }

                // Set the player autofill for 'resettomecooldowns'
                if (args.length == 3 && (args[1].equalsIgnoreCase("resettomecooldowns") || args[1].equalsIgnoreCase("cooldownresettomes"))) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter((s) -> s.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 4 && args[1].equalsIgnoreCase("config")) {
                    String configName = args[2].toLowerCase();

                    if (configName.equals("stake_permadeath_stage")) {
                        // Handle vampire permadeath setting with the valid numbers 1, 2 and 3
                        List<String> stageOptions = Arrays.asList("1", "2", "3");
                        return stageOptions.stream().filter((s) -> s.startsWith(args[3].toLowerCase())).collect(Collectors.toList());

                    } else {
                        // Handle config changing commands that accept a boolean
                        List<String> booleanOptions = Arrays.asList("true", "false");
                        return booleanOptions.stream().filter((s) -> s.startsWith(args[3].toLowerCase())).collect(Collectors.toList());
                    }
                }

                if (args.length == 3 && args[1].equalsIgnoreCase("beacon")) {
                    List<String> beaconOptions = Arrays.asList("add", "remove", "list", "info", "stats", "reload", "holy", "desecrated", "corrupted", "neutral", "validate", "fix", "refresh", "cleanup", "clearcooldowns", "debug");
                    return beaconOptions.stream().filter((s) -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 4 && args[1].equalsIgnoreCase("beacon")) {
                    String subCommand = args[2].toLowerCase();
                    if (subCommand.equals("remove") || subCommand.equals("delete") || subCommand.equals("info") || subCommand.equals("holy") || subCommand.equals("desecrated") || subCommand.equals("corrupted") || subCommand.equals("neutral")) {
                        return this.plugin.getBeaconManager().getAllBeacons().stream().map((beacon) -> beacon.getName()).filter((s) -> s.toLowerCase().startsWith(args[3].toLowerCase())).collect(Collectors.toList());
                    }

                    if (subCommand.equals("add")) {
                        return List.of("[name]");
                    }
                }

                if (args.length == 5 && args[1].equalsIgnoreCase("beacon") && args[2].equalsIgnoreCase("add")) {
                    return Arrays.asList("5", "10", "15", "20", "25", "50", "100");
                }

                if (args.length == 3 && (args[1].equalsIgnoreCase("vampirecooldowns") || args[1].equalsIgnoreCase("cooldownvampires"))) {
                    return Stream.of("reset", "clear").filter((s) -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 4 && (args[1].equalsIgnoreCase("vampirecooldowns") || args[1].equalsIgnoreCase("cooldownvampires")) && (args[2].equalsIgnoreCase("reset") || args[2].equalsIgnoreCase("clear"))) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter((s) -> s.toLowerCase().startsWith(args[3].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 3 && args[1].equalsIgnoreCase("givetome")) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter((s) -> s.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 4 && args[1].equalsIgnoreCase("givetome")) {
                    List<String> tomeAbilities = BrigadierCommands.TOME_ABILITIES;
                    return tomeAbilities.stream().filter((s) -> s.toLowerCase().startsWith(args[3].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 5 && args[1].equalsIgnoreCase("givetome")) {
                    return Arrays.asList("1", "5", "10", "16", "32", "64");
                }

                if (args.length == 3 && args[1].equalsIgnoreCase("select_tomes")) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter((s) -> s.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 3 && args[1].equalsIgnoreCase("give_cure_book")) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter((s) -> s.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 4 && args[1].equalsIgnoreCase("give_cure_book")) {
                    return Stream.of("1", "2", "3", "4").filter((s) -> s.startsWith(args[3])).collect(Collectors.toList());
                }

                if (args.length == 3 && args[1].equalsIgnoreCase("stash_cure_book")) {
                    return Stream.of("1", "2", "3", "4").filter((s) -> s.startsWith(args[2])).collect(Collectors.toList());
                }

                if (args.length == 3 && args[1].equalsIgnoreCase("clearbloodmoonbuffs")) {
                    List<String> options = new ArrayList<>();
                    options.add("all");
                    Bukkit.getOnlinePlayers().forEach((p) -> options.add(p.getName()));

                    return options.stream().filter((s) -> s.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 3 && args[1].equalsIgnoreCase("make_incurable")) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter((s) -> s.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 3 && args[1].equalsIgnoreCase("resetplayer")) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter((s) -> s.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 4 && args[1].equalsIgnoreCase("resetplayer")) {
                    return Stream.of("true", "false").filter((s) -> s.startsWith(args[3].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 3 && args[1].equalsIgnoreCase("fixattributes")) {
                    List<String> options = new ArrayList<>();
                    options.add("all");
                    Bukkit.getOnlinePlayers().forEach((p) -> options.add(p.getName()));
                    return options.stream().filter((s) -> s.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 3 && ( args[1].equalsIgnoreCase("removeendermen") || args[1].equalsIgnoreCase("removecreepers") )) {
                    return Stream.of("all", "toggle", "status").filter((s) -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 3 && args[1].equalsIgnoreCase("setupplayer")) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter((s) -> s.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 3 && args[1].equalsIgnoreCase("init")) {
                    return Stream.of("cancel").filter((s) -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                }
            }

            if (args.length == 2 && args[0].equalsIgnoreCase("vability")) {
                List<String> abilities = Arrays.asList("list", "all");
                abilities.addAll(BrigadierCommands.VAMPIRE_ABILITIES);

                return abilities.stream().filter((s) -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());

            } else if (args.length == 2 && args[0].equalsIgnoreCase("tome")) {
                if (!(sender instanceof Player player)) {
                    return new ArrayList<>();

                } else {
                    completions = new ArrayList<>();
                    completions.add("list");
                    completions.addAll(this.plugin.getTomeManager().getPlayerAbilities(player));

                    return completions.stream().filter((s) -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                }
            } else {
                return completions;
            }
        }
    }
}
