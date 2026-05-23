package frostvein.sampires.remakepire.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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
    private final ForcedCureReopenCommand forceCureReopenCommand;
    private final HolySitesCommand beaconStatusCommand;
    private final TexturePackCommand texturePackCommand;
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
        this.forceCureReopenCommand = new ForcedCureReopenCommand(plugin);
        this.beaconStatusCommand = new HolySitesCommand(plugin);
        this.texturePackCommand = new TexturePackCommand(plugin);
        this.permadeathCommand = new PermadeathCommand(plugin);
        this.turningCommand = new ToggleTurningCommand(plugin);
        this.sendMessageCommand = new PendingMessageCommand(plugin);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            this.sendHelp(sender);
            return true;

        } else {
            String subCommand = args[0].toLowerCase();
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

            switch (subCommand) {
                case "admin":
                    return this.handleAdminCommand(sender, subArgs);
                case "vability":
                    return this.abilityCommand.onCommand(sender, command, label, subArgs);
                case "tome":
                    return this.tomeCommand.onCommand(sender, command, label, subArgs);
                case "beaconstatus":
                case "holysites":
                case "holy":
                    return this.beaconStatusCommand.onCommand(sender, command, label, subArgs);
                case "texture":
                case "texturepack":
                case "resourcepack":
                    return this.texturePackCommand.onCommand(sender, command, label, subArgs);
                case "toggle_permadeath":
                case "toggle-permadeath":
                case "togglepermadeath":
                case "permadeath":
                    return this.permadeathCommand.onCommand(sender, command, label, subArgs);
                case "toggle-turning":
                case "turning":
                    return this.turningCommand.onCommand(sender, command, label, subArgs);
                case "sendmessage":
                case "sendpendingmessage":
                    return this.sendMessageCommand.onCommand(sender, command, label, subArgs);
                case "reopen":
                case "forcedcure-reopen":
                    return this.forceCureReopenCommand.onCommand(sender, command, label, subArgs);
                case "help":
                    this.sendHelp(sender);
                    return true;
                default:
                    sender.sendMessage("§cUnknown subcommand: " + subCommand);
                    sender.sendMessage("§7Use §e/pow help §7for a list of commands");
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
            sender.sendMessage("§cYou don't have permission to use admin commands.");
            return true;

        } else if (args.length == 0) {
            this.sendAdminHelp(sender);
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
     * Print to the sender a list of available commands they can run using the pow heading command.
     *
     * @param sender the player sending the command.
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== VampireSMP Commands ===");

        // Only let the sender know about the admin option if they have access to it
        if (sender.hasPermission("vampiresmp.admin")) {
            sender.sendMessage("§e/pow admin §7- Admin commands (requires permission)");
        }

        sender.sendMessage("§e/pow vability <name> §7- Use vampire abilities");
        sender.sendMessage("§e/pow tome <name> §7- Use tome abilities (humans)");
        sender.sendMessage("§e/voluntate-mea-hoc-nefandum-vinculum-abicio §7- Cure yourself from vampirism");
        sender.sendMessage("§e/hoc-vinculum-tibi-dirumpo-mala-creatura <player> §7- Force cure a vampire");
        sender.sendMessage("§e/pow beaconstatus §7- Check beacon spiritual influence");
        sender.sendMessage("§e/pow texture §7- Apply VampireSMP texture pack");
        sender.sendMessage("§e/pow permadeath <on | off | absolute> §7- Set permadeath preference");
        sender.sendMessage("§e/pow toggle-turning §7- Toggle vampire turning ability");
        sender.sendMessage("§e/pow sendmessage §7- Send pending chat message");
    }

    /**
     * Print to the sender a list of available admin commands they can run using the pow admin command.
     *
     * @param sender the admin sending the command.
     */
    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== VampireSMP Admin Commands ===");
        sender.sendMessage("§e/pow admin init §7- Initialize a new game (full reset)");
        sender.sendMessage("§e/pow admin session <start | pause | end | prime | resume | building> §7- Manage session state");
        sender.sendMessage("§e/pow admin vampire <player> <human | 1 | 2 | 3 | turn> §7- Manage vampire status");
        sender.sendMessage("§e/pow admin beacon <subcommand> §7- Manage beacon sites (use tab for options)");
        sender.sendMessage("§e/pow admin config <configuration> §7- Configure values in the configuration file live and in-game");
        sender.sendMessage("§e/pow admin vampirecooldowns <reset | clear> [player] §7- Reset vampire ability cooldowns");
        sender.sendMessage("§e/pow admin resettomecooldowns [player] §7- Reset tome ability cooldowns");
        sender.sendMessage("§e/pow admin break_warning §7- Play break warning sounds");
        sender.sendMessage("§e/pow admin givetome <player> <ability> [amount] §7- Give tome to player");
        sender.sendMessage("§e/pow admin select_tomes <player> §7- Open GUI to grant tome abilities");
        sender.sendMessage("§e/pow admin give_cure_book <player> <1 | 2 | 3 | 4> §7- Give cure book item to player");
        sender.sendMessage("§e/pow admin distributetomes §7- Manually trigger tome distribution");
        sender.sendMessage("§e/pow admin clearbloodmoonbuffs <all | player> §7- Clear blood moon buffs");
        sender.sendMessage("§e/pow admin fixattributes <all | player> §7- Fix stuck attribute modifiers (health/speed)");
        sender.sendMessage("§e/pow admin make_incurable <player> §7- Makes the player incapable of being cured.");
        sender.sendMessage("§e/pow admin removeendermen <all | toggle | status> §7- Manage enderman removal");
        sender.sendMessage("§e/pow admin setupplayer <player> §7- Give starter items to player");
        sender.sendMessage("§e/pow admin spawnanimals §7- Manually trigger passive mob spawning");
        sender.sendMessage("§e/pow admin addtomechest §7- Add current location as tome chest spawn");
        sender.sendMessage("§e/pow admin removetomechest §7- Remove nearest tome chest within 10 blocks");
        sender.sendMessage("§e/pow admin listtomechests §7- List all tome chest locations");
        sender.sendMessage("§e/pow admin resetplayer <player> §7- Fully reset player to fresh state");
        sender.sendMessage("§e/pow admin set_vampire_spawn [x y z] §7- Set vampire respawn location");
    }

    /**
     * Create the list of autocorrecting options for pow commands as they are written out in the command line.
     *
     * @param sender the player sending the command.
     * @param command the command that is being created and executed.
     * @param alias the chosen head for the command.
     * @param args the arguments attached to the command.
     * @return A {@code List} of options for the autocomplete to suggest.
     */
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("vability", "tome", "beaconstatus", "permadeath", "toggle-turning", "help"));
            if (sender.hasPermission("vampiresmp.admin")) {
                subCommands.add(0, "admin");
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
                    List<String> adminCommands = Arrays.asList("init", "session", "vampire", "beacon", "vampirecooldowns", "resettomecooldowns", "break_warning", "givetome", "select_tomes", "give_cure_book", "distributetomes", "clearbloodmoonbuffs", "make_incurable", "fixattributes", "removeendermen", "setupplayer", "spawnanimals", "addtomechest", "removetomechest", "listtomechests", "resetplayer", "set_vampire_spawn", "config");
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
                if (args.length == 3 && args[1].equalsIgnoreCase("resettomecooldowns")) {
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
                    List<String> beaconOptions = Arrays.asList("add", "remove", "list", "info", "stats", "reload", "holy", "desecrated", "neutral", "validate", "fix", "refresh", "cleanup", "clearcooldowns", "debug");
                    return beaconOptions.stream().filter((s) -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 4 && args[1].equalsIgnoreCase("beacon")) {
                    String subCommand = args[2].toLowerCase();
                    if (subCommand.equals("remove") || subCommand.equals("delete") || subCommand.equals("info") || subCommand.equals("holy") || subCommand.equals("desecrated") || subCommand.equals("neutral")) {
                        return this.plugin.getBeaconManager().getAllBeacons().stream().map((beacon) -> beacon.getName()).filter((s) -> s.toLowerCase().startsWith(args[3].toLowerCase())).collect(Collectors.toList());
                    }

                    if (subCommand.equals("add")) {
                        return Arrays.asList("[name]");
                    }
                }

                if (args.length == 5 && args[1].equalsIgnoreCase("beacon") && args[2].equalsIgnoreCase("add")) {
                    return Arrays.asList("5", "10", "15", "20", "25", "50", "100");
                }

                if (args.length == 3 && args[1].equalsIgnoreCase("vampirecooldowns")) {
                    return Arrays.asList("reset", "clear").stream().filter((s) -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 4 && args[1].equalsIgnoreCase("vampirecooldowns") && (args[2].equalsIgnoreCase("reset") || args[2].equalsIgnoreCase("clear"))) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter((s) -> s.toLowerCase().startsWith(args[3].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 3 && args[1].equalsIgnoreCase("givetome")) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter((s) -> s.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 4 && args[1].equalsIgnoreCase("givetome")) {
                    List<String> tomeAbilities = Arrays.asList("blessing", "banishundead", "holyword", "enlightenedeye", "lanternthrash", "prayeroffaith", "rallyingcry", "shoulderbarge", "turnundead", "uncannydirection", "unnaturalhaste", "wayoftheland", "wayofthelumberjack", "wayoftheprospector", "stopthebleeding");
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
                    return Arrays.asList("1", "2", "3", "4").stream().filter((s) -> s.startsWith(args[3])).collect(Collectors.toList());
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
                    return Arrays.asList("true", "false").stream().filter((s) -> s.startsWith(args[3].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 3 && args[1].equalsIgnoreCase("fixattributes")) {
                    List<String> options = new ArrayList<>();
                    options.add("all");
                    Bukkit.getOnlinePlayers().forEach((p) -> options.add(p.getName()));
                    return options.stream().filter((s) -> s.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 3 && args[1].equalsIgnoreCase("removeendermen")) {
                    return Arrays.asList("all", "toggle", "status").stream().filter((s) -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 3 && args[1].equalsIgnoreCase("setupplayer")) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter((s) -> s.toLowerCase().startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                }

                if (args.length == 3 && args[1].equalsIgnoreCase("init")) {
                    return Arrays.asList("cancel").stream().filter((s) -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
                }


            }

            if (args.length == 2 && args[0].equalsIgnoreCase("vability")) {
                List<String> abilities = Arrays.asList("list", "all", "bat", "lunge", "vanish", "stormcall", "beacontravel", "vision");
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
