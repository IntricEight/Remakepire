package frostvein.sampires.remakepire.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.abilities.tome.TomeAbility;
import frostvein.sampires.remakepire.beacons.BeaconSite;
import frostvein.sampires.remakepire.beacons.BeaconSite.BeaconState;
import frostvein.sampires.remakepire.listeners.CureBookReadingListener;
import frostvein.sampires.remakepire.managers.BeaconManager;
import frostvein.sampires.remakepire.managers.ConfigManager;
import frostvein.sampires.remakepire.managers.SessionManager;
import frostvein.sampires.remakepire.managers.TomeManager;
import frostvein.sampires.remakepire.managers.VampireManager;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private final RemakepirePlugin plugin;
    private final ConfigManager configManager;
    private final SessionManager sessionManager;
    private final VampireManager vampireManager;
    private final BeaconManager beaconManager;
    private final TomeManager tomeManager;

    public CommandHandler(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.sessionManager = plugin.getSessionManager();
        this.vampireManager = plugin.getVampireManager();
        this.beaconManager = plugin.getBeaconManager();
        this.tomeManager = plugin.getTomeManager();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("vampiresmp.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;

        } else if (command.getName().equalsIgnoreCase("init")) {
            return this.handleInitCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("session")) {
            return this.handleSessionCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("vampire")) {
            return this.handleVampireCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("beacon")) {
            return this.handleBeaconCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("vampirecooldowns")) {
            return this.handleVampireCooldownCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("resettomecooldowns")) {
            return this.handleResetTomeCooldownCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("onehumanleft")) {
            return this.handleOneHumanLeftCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("vampirehealthcheck")) {
            return this.handleVampireHealthCheckCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("break_warning")) {
            return this.handleBreakWarningCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("givetome")) {
            return this.handleGiveTomeCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("select_tomes")) {
            return this.handleSelectTomesCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("give_cure_book")) {
            return this.handleGiveCureBookCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("distributetomes")) {
            return this.handleDistributeTomesCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("clearbloodmoonbuffs")) {
            return this.handleClearBloodMoonBuffsCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("fixattributes")) {
            return this.handleFixAttributesCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("removeendermen")) {
            return this.handleRemoveEndermenCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("damagesuppression")) {
            return this.handleDamageSuppressionCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("setupplayer")) {
            return this.handleSetupPlayerCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("spawnanimals")) {
            return this.handleSpawnAnimalsCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("addtomechest")) {
            return this.handleAddTomeChestCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("removetomechest")) {
            return this.handleRemoveTomeChestCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("listtomechests")) {
            return this.handleListTomeChestsCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("resetplayer")) {
            return this.handleResetPlayerCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("config")) {
            // Allow admins to change some elements within the configuration file without needing to restart the server
            return this.handleConfigCommand(sender, args);

        } else {
            return command.getName().equalsIgnoreCase("set_vampire_spawn") ? this.handleSetVampireSpawnCommand(sender, args) : false;
        }
    }

    private boolean handleResetPlayerCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /pow admin resetplayer <player> [true|false]");
            sender.sendMessage("§7  true/false = clear inventory");
            return true;

        } else {
            Player target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                sender.sendMessage("§cPlayer not found: " + args[0]);
                return true;

            } else {
                boolean clearInventory = args.length >= 2 && args[1].equalsIgnoreCase("true");
                if (target.getGameMode() == GameMode.SPECTATOR) {
                    target.setGameMode(GameMode.SURVIVAL);
                }

                this.vampireManager.setPlayerAsHuman(target);

                for(String tag : new HashSet<>(target.getScoreboardTags())) {
                    target.removeScoreboardTag(tag);
                }

                target.addScoreboardTag("human");

                try {
                    Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                    Objective deathObjective = mainScoreboard.getObjective("vsmp_death");

                    if (deathObjective != null) {
                        deathObjective.getScore(target.getName()).setScore(0);
                    }
                } catch (Exception e) {
                    this.plugin.getLogger().warning("Failed to reset death count for " + target.getName() + ": " + e.getMessage());
                }

                if (this.plugin.getTomeManager() != null) {
                    this.plugin.getTomeManager().removeAllAbilities(target);
                }

                if (this.plugin.getThirstManager() != null) {
                    target.removeScoreboardTag("ThirstImmunity");
                }

                if (this.plugin.getSireManager() != null) {
                    this.plugin.getSireManager().removeSire(target.getName());
                }

                if (this.plugin.getVampireAbilityManager() != null) {
                    this.plugin.getVampireAbilityManager().clearAllCooldowns(target);
                }

                target.getActivePotionEffects().forEach((effect) -> target.removePotionEffect(effect.getType()));
                AttributeInstance healthAttribute = target.getAttribute(Attribute.MAX_HEALTH);
                healthAttribute.getModifiers().forEach(arg_0 -> healthAttribute.removeModifier(arg_0));
                healthAttribute.setBaseValue(20.0);
                target.setHealth(healthAttribute.getValue());
                target.setFoodLevel(20);
                target.setSaturation(5.0F);
                target.setExp(0.0F);
                target.setLevel(0);

                if (this.plugin.getBeaconMajorityManager() != null) {
                    this.plugin.getBeaconMajorityManager().updateBeaconMajorityBonuses();
                }

                target.setInvulnerable(false);

                if (clearInventory) {
                    target.getInventory().clear();
                    target.getEnderChest().clear();
                    sender.sendMessage("§7Cleared " + target.getName() + "'s inventory and ender chest.");
                }

                sender.sendMessage("§aPlayer " + target.getName() + " has been fully reset to a fresh state.");
                target.sendMessage("§aYou have been reset to a fresh state by an administrator.");
                target.sendMessage("§All vampire status, abilities, cooldowns, and death count have been cleared." + (clearInventory ? " Your inventory has also been cleared." : ""));
                this.plugin.logInfo("Admin " + sender.getName() + " reset player " + target.getName() + " to fresh state");

                return true;
            }
        }
    }

    /**
     * Execute commands sent using the config admin options.
     *
     * @param sender the admin who entered the command.
     * @param args the command arguments.
     * @return {@code true}
     */
    private boolean handleConfigCommand(CommandSender sender, String[] args) {
        /* Configuration Commands design
        * If only 1 argument is passed:
        * * Retrieve the current config value and give it to the sender.
        *
        * If 2 or more arguments are passed:
        * * Execute a change command, updating the value inside the config.yml file.
        */

        if (args.length < 2) {
            switch (args[0].toLowerCase()) {
                case "help":
                    sender.sendMessage("§aUsage: /pow admin config <configuration> <new setting>");
                    sender.sendMessage("§7  alert_on_quit [true|false] - Alert admins when a player leaves");
                    sender.sendMessage("§7  holy_water_cap [true|false] - Limit holy water creation");
                    sender.sendMessage("§7  tome_cap [true|false] - Limit new tome abilities absorbed");
                    sender.sendMessage("§7  vampire_level_cap [true|false] - Prevent vampires from returning to lost levels");
                    sender.sendMessage("§7  new_vampire_tracking [true|false] - Allow vampires to track down newly created vampires");
                    sender.sendMessage("§7  allow_vampire_mounts [true|false] - Allow vampires to ride living mounts");
                    sender.sendMessage("§7  cure_requires_dead_sire [true|false] - Require a sire's permadeath before their spawn can be cured");
                    sender.sendMessage("§7  enable_npc_mobs [true|false] - Allow NPC mobs to naturally spawn");
                    sender.sendMessage("§7  stake_permadeath_stage [1|2|3] - Set stage that vampires can permadie on");
                    sender.sendMessage("§7  human_life_limit [true|false] - Humans always die on their sixth death");
                    break;

                case "alert_on_quit":
                    sender.sendMessage("§6alert-on-player-leave§r is currently: " + configManager.shouldAlertOnPlayerQuit());
                    break;

                case "holy_water_cap":
                    sender.sendMessage("§6holy-water-session-capped§r is currently: " + configManager.isHolyWaterSessionCapped());
                    break;

                case "tome_cap":
                    sender.sendMessage("§6tome-absorption-capping§r is currently: " + configManager.isTomeAbsorptionCapped());
                    break;

                case "vampire_level_cap":
                    sender.sendMessage("§6vampire-level-capping§r is currently: " + configManager.isVampireLevelingCapped());
                    break;

                case "new_vampire_tracking":
                    sender.sendMessage("§6new_vampire_tracking§r is currently: " + configManager.canTrackNewVampires());
                    break;

                case "allow_vampire_mounts":
                    sender.sendMessage("§6allow_vampire_mounts§r is currently: " + configManager.canVampiresRideLivingMounts());
                    break;

                case "cure_requires_dead_sire":
                    sender.sendMessage("§6cure_requires_dead_sire§r is currently: " + configManager.doCuresRequireSireDeath());
                    break;

                case "enable_npc_mobs":
                    sender.sendMessage("§6enable_npc_mobs§r is currently: " + configManager.areNpcMobsEnabled());
                    break;

                case "stake_permadeath_stage":
                    sender.sendMessage("§6permadeath-minimum-stage§r is currently: " + configManager.getPermadeathMinimumStage());
                    break;

                case "human_life_limit":
                    sender.sendMessage("§6enforce-life-limit§r is currently: " + configManager.isLifeLimitEnforced());
                    break;

                default:
                    sender.sendMessage("§cInvalid configuration. Use \"/pow admin config help\" for a list of config command options.");
            }
        } else {
            // Store and template the update message to send to the player
            String senderMessage = "Config for §6";

            switch (args[0].toLowerCase()) {
                case "alert_on_quit":
                    sessionManager.setAlertOnPlayerQuit(Boolean.parseBoolean(args[1]));
                    senderMessage += "alert-on-player-leave§r set to: " + Boolean.parseBoolean(args[1]);
                    break;

                case "holy_water_cap":
                    sessionManager.setHolyWaterCapping(Boolean.parseBoolean(args[1]));
                    senderMessage += "holy-water-session-capped§r set to: " + Boolean.parseBoolean(args[1]);
                    break;

                case "tome_cap":
                    sessionManager.setTomeAbsorptionCapping(Boolean.parseBoolean(args[1]));
                    senderMessage += "tome-absorption-capping§r set to: " + Boolean.parseBoolean(args[1]);
                    break;

                case "vampire_level_cap":
                    sessionManager.setVampireLevelCapping(Boolean.parseBoolean(args[1]));

                    // Clear existing promotion and stage bans if capping is being disabled
                    if (!Boolean.parseBoolean(args[1])) {
                        plugin.getVampireManager().clearAllPromotionBans();
                        plugin.getVampireManager().clearAllStageCaps();
                    }

                    senderMessage += "vampire-level-capping§r set to: " + Boolean.parseBoolean(args[1]);
                    break;

                case "new_vampire_tracking":
                    sessionManager.setTrackingNewVampires(Boolean.parseBoolean(args[1]));
                    senderMessage += "new-vampire-tracking§r set to: " + Boolean.parseBoolean(args[1]);
                    break;

                case "allow_vampire_mounts":
                    sessionManager.setVampiresRideLivingMounts(Boolean.parseBoolean(args[1]));
                    senderMessage += "allow-vampire-mounts§r set to: " + Boolean.parseBoolean(args[1]);
                    break;

                case "cure_requires_dead_sire":
                    sessionManager.setCureRequiresSireDeath(Boolean.parseBoolean(args[1]));
                    senderMessage += "sire-death-requirement§r set to: " + Boolean.parseBoolean(args[1]);
                    break;

                case "enable_npc_mobs":
                    sessionManager.setNpcSpawningGamerules(Boolean.parseBoolean(args[1]));
                    senderMessage += "enable-npc-mobs§r set to: " + Boolean.parseBoolean(args[1]);
                    break;

                case "stake_permadeath_stage":
                    if (Integer.parseInt(args[1]) >= 1 && Integer.parseInt(args[1]) <= 3) {
                        sessionManager.setStakePermadeathMinimumStage(Integer.parseInt(args[1]));
                        senderMessage += "permadeath-minimum-stage§r set to: " + Integer.parseInt(args[1]);
                    } else {
                        senderMessage = "§cInvalid stage! Use 1, 2, or 3";
                    }

                    break;

                case "human_life_limit":
                    sessionManager.setHumanLivesEnforced(Boolean.parseBoolean(args[1]));
                    senderMessage += "enforce-life-limit§r set to: " + Boolean.parseBoolean(args[1]);
                    break;

                default:
                    senderMessage = "§cInvalid configuration. Use \"/pow admin config help\" for a list of config command options.";
            }

            sender.sendMessage(senderMessage);
        }

        return true;
    }

    private boolean handleSetVampireSpawnCommand(CommandSender sender, String[] args) {
        double x, y, z;

        if (args.length >= 3) {
            try {
                x = Double.parseDouble(args[0]);
                y = Double.parseDouble(args[1]);
                z = Double.parseDouble(args[2]);

            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid coordinates. Usage: /pow admin set_vampire_spawn [x y z]");
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cConsole must provide coordinates: /pow admin set_vampire_spawn <x> <y> <z>");
                return true;
            }

            Player player = (Player)sender;
            Location loc = player.getLocation();
            x = loc.getX();
            y = loc.getY();
            z = loc.getZ();
        }

        String locationStr = (int)x + "," + (int)y + "," + (int)z;
        this.plugin.getConfigManager().setVampireRespawnLocation(locationStr);
        this.plugin.reloadVampireRespawnLocation();

        sender.sendMessage("§aVampire spawn location set to: §e" + locationStr);
        this.plugin.logInfo("Admin " + sender.getName() + " set vampire spawn to " + locationStr);

        return true;
    }

    private boolean handleInitCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;

        } else if (args.length > 0 && args[0].equalsIgnoreCase("cancel")) {
            this.plugin.getInitGameManager().cancelInitialization(admin);
            return true;

        } else {
            this.plugin.getInitGameManager().startInitialization(admin);
            return true;
        }
    }

    private boolean handleVampireCooldownCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /pow admin vampirecooldowns <reset|clear> [player]");
            sender.sendMessage("§7- /pow admin vampirecooldowns reset §8- Reset all cooldowns for all online players");
            sender.sendMessage("§7- /pow admin vampirecooldowns reset <player> §8- Reset cooldowns for a specific player");
            sender.sendMessage("§7- /pow admin vampirecooldowns clear §8- Same as reset");
            return true;

        } else {
            String action = args[0].toLowerCase();
            if (!action.equals("reset") && !action.equals("clear")) {
                sender.sendMessage("§cInvalid action. Use 'reset' or 'clear'.");
                return true;

            } else if (args.length >= 2) {
                Player target = Bukkit.getPlayer(args[1]);

                if (target == null) {
                    sender.sendMessage("§cPlayer '" + args[1] + "' not found.");
                    return true;

                } else {
                    this.resetPlayerCooldowns(target);
                    sender.sendMessage("§aReset all cooldowns for player: §e" + target.getName());
                    target.sendMessage("§aYour vampire ability cooldowns have been reset by an administrator.");
                    return true;
                }
            } else {
                int playersAffected = 0;

                for(Player player : Bukkit.getOnlinePlayers()) {
                    this.resetPlayerCooldowns(player);
                    player.sendMessage("§aYour vampire ability cooldowns have been reset by an administrator.");
                    ++playersAffected;
                }

                sender.sendMessage("§aReset all vampire ability cooldowns for §e" + playersAffected + " §aonline players.");
                this.plugin.logInfo("Admin " + sender.getName() + " reset cooldowns for " + playersAffected + " players");

                return true;
            }
        }
    }

    private void resetPlayerCooldowns(Player player) {
        this.plugin.getVampireAbilityManager().clearAllCooldowns(player);
        this.plugin.getVampireAbilityManager().clearGlobalCooldowns();
    }

    private boolean handleResetTomeCooldownCommand(CommandSender sender, String[] args) {
        int humansAffected = 0;

        // Handle the cooldown command for individual players
        if (args.length == 0) {
            for(Player player : Bukkit.getOnlinePlayers()) {
                if (this.plugin.getVampireManager().isHuman(player)) {
                    TomeAbility.clearAllCooldowns(player);
                    player.removeScoreboardTag("blessing_used_session");
                    player.removeScoreboardTag("stopthebleeding_used_session");
                    player.sendMessage("§aYour tome ability cooldowns have been reset by an administrator.");
                    ++humansAffected;
                }
            }
        } else {
            // Handle the cooldown command for a single player
            Player target = Bukkit.getPlayer(args[0]);

            if (target != null && this.plugin.getVampireManager().isHuman(target)) {
                TomeAbility.clearAllCooldowns(target);
                target.removeScoreboardTag("blessing_used_session");
                target.removeScoreboardTag("stopthebleeding_used_session");
                target.sendMessage("§aYour tome ability cooldowns have been reset by an administrator.");
                ++humansAffected;
            }
        }

        if (humansAffected == 0) {
            sender.sendMessage("§eNo humans online to reset tome cooldowns for.");
        } else {
            sender.sendMessage("§aReset all tome ability cooldowns for §e" + humansAffected + " §ahuman players.");
        }

        this.plugin.logInfo("Admin " + sender.getName() + " reset tome cooldowns for " + humansAffected + " human players");
        return true;
    }

    private boolean handleOneHumanLeftCommand(CommandSender sender, String[] args) {
        boolean newState = !this.sessionManager.isOneHumanLeftActive();
        this.sessionManager.setOneHumanLeftActive(newState);

        if (newState) {
            sender.sendMessage("§aOne Human Left mode ACTIVATED: Humans no longer have beacon cooldowns");
        } else {
            sender.sendMessage("§cOne Human Left mode DEACTIVATED: Normal beacon cooldowns restored for all players");
        }

        return true;
    }

    private boolean handleVampireHealthCheckCommand(CommandSender sender, String[] args) {
        if (args.length != 0 && !args[0].equalsIgnoreCase("get")) {
            if (args[0].equalsIgnoreCase("set") && args.length >= 2) {
                try {
                    int newTicks = Integer.parseInt(args[1]);

                    if (newTicks < 1) {
                        sender.sendMessage("§cInterval must be at least 1 tick.");
                    } else {
                        this.sessionManager.setVampireHealthCheckTicks(newTicks);
                        double seconds = newTicks / 20.0;
                        sender.sendMessage("§aVampire Health Check Interval set to: §e" + newTicks + " ticks §7(" + seconds + " seconds)");
                        sender.sendMessage("§7Changes saved to config and take effect immediately");
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid number: " + args[1]);
                }
            } else {
                sender.sendMessage("§cUsage: /pow admin vampirehealthcheck [get|set] [ticks]");
                sender.sendMessage("§7- /pow admin vampirehealthcheck get §8- Show current interval");
                sender.sendMessage("§7- /pow admin vampirehealthcheck set <ticks> §8- Set new interval");
                sender.sendMessage("§7Current: §e" + this.sessionManager.getVampireHealthCheckTicks() + " ticks");
            }
        } else {
            int currentTicks = this.sessionManager.getVampireHealthCheckTicks();
            sender.sendMessage("§aVampire Health Check Interval: §e" + currentTicks + " ticks §7(" + (currentTicks / 20) + " seconds)");
        }

        return true;
    }

    private boolean handleBreakWarningCommand(CommandSender sender, String[] args) {
        sender.sendMessage("§ePlaying first warning sound for all players...");

        // Send the first warning noise (crows)
        for(Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), "crimson:crimson.sound.crimson_warning_1", SoundCategory.MASTER, 1.0F, 1.0F);
        }

        sender.sendMessage("§aFirst warning sound played. Second warning will play in 5 minutes.");

        // Send the second warning noise (bells) after 5 minutes
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            for(Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), "crimson:crimson.sound.crimson_warning_2", SoundCategory.MASTER, 1.0F, 1.0F);
            }

            this.plugin.logInfo("Second warning sound played for all online players");
        }, 6000L);

        return true;
    }

    private boolean handleSessionCommand(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /pow admin session <start|pause|end|prime|resume|building>");
            return true;

        } else {
            switch (args[0].toLowerCase()) {
                case "start":
                    int currentState = this.sessionManager.getSessionState();
                    if (currentState == 3) {
                        sender.sendMessage("§eSession ended - auto-priming for new session...");
                        this.sessionManager.primeNewSession();
                        currentState = this.sessionManager.getSessionState();
                    }

                    if (currentState != 0 && currentState != 4) {
                        sender.sendMessage("§cCannot start session. Session must be primed or in building mode first.");
                        return true;
                    }

                    this.sessionManager.startSession();
                    sender.sendMessage("§aSession started.");
                    break;
                case "pause":
                    if (this.sessionManager.getSessionState() != 1) {
                        sender.sendMessage("§cCannot pause session. No session is currently running.");
                        return true;
                    }

                    this.sessionManager.pauseSession();
                    sender.sendMessage("§eSession paused.");
                    break;
                case "end":
                    if (this.sessionManager.getSessionState() != 1 && this.sessionManager.getSessionState() != 2) {
                        sender.sendMessage("§cCannot end session. No session is currently running or paused.");
                        return true;
                    }

                    this.sessionManager.endSession();
                    sender.sendMessage("§cSession ended.");
                    break;
                case "prime":
                    if (this.sessionManager.getSessionState() == 0) {
                        sender.sendMessage("§cCannot prime session. A session is already primed. Start it with '/pow admin session start'.");
                        return true;
                    }

                    if (this.sessionManager.getSessionState() == 1) {
                        sender.sendMessage("§cCannot prime session. A session is currently running. End it first with '/pow admin session end'.");
                        return true;
                    }

                    if (this.sessionManager.getSessionState() == 2) {
                        sender.sendMessage("§cCannot prime session. A session is currently paused. Resume it first with '/pow admin session resume'.");
                        return true;
                    }

                    this.sessionManager.primeNewSession();
                    sender.sendMessage("§aSession primed. Use '/pow admin session start' to begin.");
                    break;
                case "resume":
                    if (this.sessionManager.getSessionState() != 2) {
                        sender.sendMessage("§cCannot resume session. No session is currently paused.");
                        return true;
                    }

                    this.sessionManager.resumeSession();
                    sender.sendMessage("§aSession resumed.");
                    break;
                case "building":
                    if (this.sessionManager.getSessionState() != 0) {
                        sender.sendMessage("§cCannot enter building mode. Session must be primed first. Use '/pow admin session prime' to prepare a new session.");
                        return true;
                    }

                    this.sessionManager.preStartSession();
                    break;
                default:
                    sender.sendMessage("§cInvalid action. Use: start, pause, end, prime, resume, or building.");
                    return true;
            }

            return true;
        }
    }

    private boolean handleVampireCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /pow admin vampire <player> <human|1|2|3|turn|clearcap|clearban>");
            sender.sendMessage("§7  clearcap - Remove stage cap (allows vampire to level up after thirst demotion)");
            sender.sendMessage("§7  clearban - Remove promotion ban (allows vampire to level up again)");
            return true;

        } else {
            Player target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;

            } else {
                switch (args[1].toLowerCase()) {
                    case "human":
                        this.vampireManager.setPlayerAsHuman(target);
                        target.getActivePotionEffects().forEach((effect) -> target.removePotionEffect(effect.getType()));

                        if (this.plugin.getBeaconMajorityManager() != null) {
                            this.plugin.getBeaconMajorityManager().updateBeaconMajorityBonuses();
                        }

                        target.setHealth(target.getAttribute(Attribute.MAX_HEALTH).getValue());
                        sender.sendMessage("§a" + target.getName() + " is now human.");
                        target.sendMessage("§aYou have been set as human.");
                        break;
                    case "1":
                        this.vampireManager.setPlayerAsVampire(target, 1, true);

                        if (this.plugin.getBeaconMajorityManager() != null) {
                            this.plugin.getBeaconMajorityManager().removeBonusesFromPlayer(target);
                            this.plugin.getBeaconMajorityManager().applyBonusesToPlayer(target);
                        }

                        target.setHealth(target.getAttribute(Attribute.MAX_HEALTH).getValue());
                        this.applyVampireNightVision(target);
                        target.setExp(0.5F);

                        sender.sendMessage("§5" + target.getName() + " is now a Stage 1 vampire.");
                        target.sendMessage("§5You have been set as a Stage 1 vampire.");
                        this.sendVampireTexturePackPrompt(target);
                        break;
                    case "2":
                        this.vampireManager.setPlayerAsVampire(target, 2, true);

                        if (this.plugin.getBeaconMajorityManager() != null) {
                            this.plugin.getBeaconMajorityManager().removeBonusesFromPlayer(target);
                            this.plugin.getBeaconMajorityManager().applyBonusesToPlayer(target);
                        }

                        target.setHealth(target.getAttribute(Attribute.MAX_HEALTH).getValue());
                        this.applyVampireNightVision(target);
                        target.setExp(0.5F);

                        sender.sendMessage("§5" + target.getName() + " is now a Stage 2 vampire.");
                        target.sendMessage("§5You have been set as a Stage 2 vampire.");
                        this.sendVampireTexturePackPrompt(target);
                        break;
                    case "3":
                        this.vampireManager.setPlayerAsVampire(target, 3, true);

                        if (this.plugin.getBeaconMajorityManager() != null) {
                            this.plugin.getBeaconMajorityManager().removeBonusesFromPlayer(target);
                            this.plugin.getBeaconMajorityManager().applyBonusesToPlayer(target);
                        }

                        target.setHealth(target.getAttribute(Attribute.MAX_HEALTH).getValue());
                        this.applyVampireNightVision(target);
                        target.setExp(0.5F);

                        sender.sendMessage("§5" + target.getName() + " is now a Stage 3 vampire.");
                        target.sendMessage("§5You have been set as a Stage 3 vampire.");
                        this.sendVampireTexturePackPrompt(target);
                        break;
                    case "turn":
                        return this.handleTurnCommand(sender, target, args);
                    case "clearcap":
                    case "clear_stage_cap":
                        if (this.vampireManager.hasStageCap(target)) {
                            int cap = this.vampireManager.getStageCap(target);
                            this.vampireManager.clearStageCap(target);

                            sender.sendMessage("§aCleared stage cap for " + target.getName() + " (was capped at stage " + cap + ")");
                            target.sendMessage("§aYour stage cap has been removed by an administrator. You can now level up freely.");

                        } else {
                            sender.sendMessage("§c" + target.getName() + " does not have a stage cap.");
                        }
                        break;
                    case "clearban":
                    case "clear_promotion_ban":
                        if (this.vampireManager.hasPromotionBan(target)) {
                            this.vampireManager.clearPromotionBan(target);
                            sender.sendMessage("§aCleared promotion ban for " + target.getName());
                            target.sendMessage("§aYour promotion ban has been removed by an administrator. You can now level up.");

                        } else {
                            sender.sendMessage("§c" + target.getName() + " does not have a promotion ban.");
                        }
                        break;
                    default:
                        sender.sendMessage("§cInvalid type. Use: human, 1, 2, 3, turn, clearcap, or clearban.");
                        return true;
                }

                return true;
            }
        }
    }

    private boolean handleBeaconCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            this.sendBeaconHelp(sender);
            return true;

        } else {
            switch (args[0].toLowerCase()) {
                case "add":
                    return this.handleBeaconAdd(sender, args);
                case "remove":
                case "delete":
                    return this.handleBeaconRemove(sender, args);
                case "list":
                    return this.handleBeaconList(sender, args);
                case "reload":
                    return this.handleBeaconReload(sender, args);
                case "stats":
                    return this.handleBeaconStats(sender, args);
                case "info":
                    return this.handleBeaconInfo(sender, args);
                case "validate":
                    return this.handleBeaconValidate(sender, args);
                case "holy":
                    return this.handleBeaconHoly(sender, args);
                case "desecrated":
                case "desecrate":
                    return this.handleBeaconDesecrated(sender, args);
                case "neutral":
                    return this.handleBeaconNeutral(sender, args);
                case "fix":
                case "repair":
                    return this.handleBeaconFix(sender, args);
                case "debug":
                    return this.handleBeaconDebug(sender, args);
                case "refresh":
                    return this.handleBeaconRefresh(sender, args);
                case "cleanup":
                    return this.handleBeaconCleanup(sender, args);
                case "clearcooldowns":
                    return this.handleBeaconClearCooldowns(sender, args);
                default:
                    sender.sendMessage("§cUnknown beacon command: " + args[0].toLowerCase());
                    this.sendBeaconHelp(sender);
                    return true;
            }
        }
    }

    private boolean handleBeaconFix(CommandSender sender, String[] args) {
        sender.sendMessage("§eValidating and repairing beacon displays...");
        this.beaconManager.validateDisplays();
        sender.sendMessage("§aBeacon display repair complete. Check console for details.");
        return true;
    }

    private boolean handleBeaconDebug(CommandSender sender, String[] args) {
        if (args.length != 1) {
            if (args.length == 2) {
                String beaconName = args[1];
                String debugInfo = this.beaconManager.getBeaconDisplayDebugInfo(beaconName);

                if (debugInfo != null) {
                    sender.sendMessage("§6=== BEACON DISPLAY DEBUG: " + beaconName.toUpperCase() + " ===");
                    sender.sendMessage(debugInfo);
                } else {
                    sender.sendMessage("§cBeacon '" + beaconName + "' not found.");
                }

                return true;
            } else {
                sender.sendMessage("§cUsage: /beacon debug [beacon_name]");
                return true;
            }
        } else {
            sender.sendMessage("§6=== BEACON DISPLAY DEBUG INFO ===");

            for(BeaconSite beacon : this.beaconManager.getAllBeacons()) {
                sender.sendMessage(this.beaconManager.getBeaconDisplayDebugInfo(beacon.getName()));
            }

            return true;
        }
    }

    private boolean handleBeaconRefresh(CommandSender sender, String[] args) {
        sender.sendMessage("§eForce refreshing all beacon displays...");
        this.beaconManager.forceRefreshAllDisplays();
        sender.sendMessage("§aBeacon display refresh complete. All displays should now match their beacon states.");
        return true;
    }

    private boolean handleBeaconCleanup(CommandSender sender, String[] args) {
        sender.sendMessage("§c§lWARNING: §ePerforming aggressive cleanup of ALL item displays at beacon locations...");
        this.beaconManager.cleanupAllDisplays();
        sender.sendMessage("§aAggressive beacon cleanup complete. All item displays at beacon locations have been removed and recreated.");
        return true;
    }

    private boolean handleBeaconClearCooldowns(CommandSender sender, String[] args) {
        sender.sendMessage("§eClearing all beacon conversion cooldowns...");
        this.beaconManager.clearAllBeaconCooldownsForNewSession();
        sender.sendMessage("§aAll beacon conversion cooldowns have been cleared. Beacons can now be converted immediately.");
        return true;
    }

    private boolean handleBeaconAdd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /pow admin beacon add <name> [radius]");
            return true;

        } else if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can add beacons.");
            return true;

        } else {
            Player player = (Player)sender;
            String name = args[1];

            if (this.beaconManager.getBeacon(name) != null) {
                sender.sendMessage("§cA beacon named '" + name + "' already exists.");
                return true;

            } else {
                Location playerLocation = player.getLocation();
                Location blockLocation = new Location(playerLocation.getWorld(), playerLocation.getBlockX() + 0.5, playerLocation.getBlockY(), playerLocation.getBlockZ() + 0.5, 0.0F, 0.0F);
                blockLocation.getBlock().setType(Material.BARRIER);

                if (this.beaconManager.addBeacon(name, blockLocation)) {
                    sender.sendMessage("§aBeacon '" + name + "' added at your location.");
                    sender.sendMessage("§7Location: §f" + blockLocation.getWorld().getName() + " (" + blockLocation.getBlockX() + ", " + blockLocation.getBlockY() + ", " + blockLocation.getBlockZ() + ")");
                    sender.sendMessage("§7An item display beacon has been created at this location.");
                    player.playSound(blockLocation, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0F, 1.0F);

                    if (args.length >= 3) {
                        try {
                            int radius = Integer.parseInt(args[2]);

                            if (radius > 0 && radius <= 100) {
                                BeaconSite beacon = this.beaconManager.getBeacon(name);
                                beacon.setCaptureRadius(radius);
                                this.beaconManager.saveBeacons();
                                sender.sendMessage("§7Capture radius set to: §e" + radius + " blocks");

                            } else {
                                sender.sendMessage("§cRadius must be between 1 and 100. Using default radius of 10.");
                            }
                        } catch (NumberFormatException e) {
                            sender.sendMessage("§cInvalid radius number. Using default radius of 10.");
                        }
                    }
                } else {
                    sender.sendMessage("§cFailed to add beacon.");
                    blockLocation.getBlock().setType(Material.AIR);
                }

                return true;
            }
        }
    }

    private boolean handleBeaconRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /pow admin beacon remove <name>");
            return true;

        } else {
            String name = args[1];
            BeaconSite beacon = this.beaconManager.getBeacon(name);

            if (beacon == null) {
                sender.sendMessage("§cBeacon '" + name + "' not found.");
                return true;

            } else {
                if (sender instanceof Player player) {
                    Location beaconLoc = beacon.getLocation();

                    if (beaconLoc != null && beaconLoc.getWorld() != null) {
                        Location originalLoc = player.getLocation().clone();
                        player.teleport(beaconLoc);
                        sender.sendMessage("§7Teleported to beacon location to ensure chunk is loaded...");

                        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                            if (this.beaconManager.removeBeacon(name)) {
                                sender.sendMessage("§aBeacon '" + name + "' removed.");
                            } else {
                                sender.sendMessage("§cFailed to remove beacon '" + name + "'.");
                            }

                            player.teleport(originalLoc);
                            sender.sendMessage("§7Teleported back to original location.");
                        }, 5L);

                        return true;
                    }
                }

                if (this.beaconManager.removeBeacon(name)) {
                    sender.sendMessage("§aBeacon '" + name + "' removed.");
                } else {
                    sender.sendMessage("§cFailed to remove beacon '" + name + "'.");
                }

                return true;
            }
        }
    }

    private boolean handleBeaconList(CommandSender sender, String[] args) {
        for(String line : this.beaconManager.getBeaconList()) {
            sender.sendMessage(line);
        }

        return true;
    }

    private boolean handleBeaconReload(CommandSender sender, String[] args) {
        this.beaconManager.reloadBeacons();
        sender.sendMessage("§aBeacons reloaded from file.");
        return true;
    }

    private boolean handleBeaconStats(CommandSender sender, String[] args) {
        Map<BeaconSite.BeaconState, Integer> stateStats = this.beaconManager.getStateStats();
        sender.sendMessage("§6§l=== BEACON STATISTICS ===");
        int total = stateStats.values().stream().mapToInt(Integer::intValue).sum();

        sender.sendMessage("§7Total Beacons: §e" + total);
        sender.sendMessage("");
        sender.sendMessage("§f§l=== SPIRITUAL INFLUENCE ===");

        for(BeaconSite.BeaconState state : BeaconState.values()) {
            int count = stateStats.get(state);
            double percentage = total > 0 ? count * 100.0 / (double)total : 0;
            String icon = "";

            switch (state) {
                case HOLY -> icon = "✦ ";
                case DESECRATED -> icon = "☠ ";
                case NEUTRAL -> icon = "◯ ";
            }

            sender.sendMessage(String.format("%s%s%s: §e%d §7(%.1f%%)", state.getColorCode(), icon, state.getDisplayName(), count, percentage));
        }

        return true;
    }

    private boolean handleBeaconInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /pow admin beacon info <name>");
            return true;

        } else {
            String name = args[1];
            BeaconSite beacon = this.beaconManager.getBeacon(name);

            if (beacon == null) {
                sender.sendMessage("§cBeacon '" + name + "' not found.");
                return true;

            } else {
                sender.sendMessage("§6§l=== BEACON INFO ===");
                sender.sendMessage(beacon.getStatusString());
                return true;
            }
        }
    }

    private boolean handleBeaconValidate(CommandSender sender, String[] args) {
        this.beaconManager.validateBeacons();
        sender.sendMessage("§aBeacon validation complete. Check console for any issues.");
        return true;
    }

    private boolean handleBeaconHoly(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /pow admin beacon holy <name>");
            return true;

        } else {
            String name = args[1];

            if (this.beaconManager.setBeaconHoly(name)) {
                sender.sendMessage("§aBeacon '" + name + "' has been consecrated as holy.");
                sender.sendMessage("§7The beacon now emanates divine light and protection.");

            } else {
                sender.sendMessage("§cBeacon '" + name + "' not found.");
            }

            return true;
        }
    }

    private boolean handleBeaconDesecrated(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /pow admin beacon desecrated <name>");
            return true;

        } else {
            String name = args[1];

            if (this.beaconManager.setBeaconDesecrated(name)) {
                sender.sendMessage("§4Beacon '" + name + "' has been desecrated by dark forces.");
                sender.sendMessage("§7The beacon now radiates malevolent energy and shadow.");

            } else {
                sender.sendMessage("§cBeacon '" + name + "' not found.");
            }

            return true;
        }
    }

    private boolean handleBeaconNeutral(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /pow admin beacon neutral <name>");
            return true;

        } else {
            String name = args[1];

            if (this.beaconManager.setBeaconNeutral(name)) {
                sender.sendMessage("§7Beacon '" + name + "' has been set to neutral.");
                sender.sendMessage("§7The beacon texture has changed. Players will receive notification in 60 seconds.");

            } else {
                sender.sendMessage("§cBeacon '" + name + "' not found.");
            }

            return true;
        }
    }

    private void sendBeaconHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== BEACON COMMANDS ===");
        sender.sendMessage("§e/pow admin beacon add <name> [radius] §7- Add beacon at your location");
        sender.sendMessage("§e/pow admin beacon remove <name> §7- Remove a beacon");
        sender.sendMessage("§e/pow admin beacon list §7- List all beacons");
        sender.sendMessage("§e/pow admin beacon info <name> §7- Get detailed beacon info");
        sender.sendMessage("§e/pow admin beacon stats §7- Show spiritual influence statistics");
        sender.sendMessage("§e/pow admin beacon reload §7- Reload beacons from file");
        sender.sendMessage("§e/pow admin beacon validate §7- Check for invalid beacons");
        sender.sendMessage("§e/pow admin beacon fix §7- Repair missing item displays");
        sender.sendMessage("§e/pow admin beacon refresh §7- Force refresh all beacon displays");
        sender.sendMessage("§e/pow admin beacon cleanup §7- AGGRESSIVE cleanup of all item displays at beacons");
        sender.sendMessage("§e/pow admin beacon clearcooldowns §7- Clear all beacon conversion cooldowns");
        sender.sendMessage("§e/pow admin beacon debug [name] §7- Debug beacon display info");
    }

    private boolean handleTurnCommand(CommandSender sender, Player target, String[] args) {
        if (!this.vampireManager.isHuman(target)) {
            sender.sendMessage("§c" + target.getName() + " is not human. Only humans can be turned into vampires.");
            return true;
        } else {
            Player turner = null;

            if (args.length >= 3) {
                turner = Bukkit.getPlayer(args[2]);

                if (turner == null) {
                    sender.sendMessage("§cTurner player '" + args[2] + "' not found.");
                    return true;
                }

                if (!this.vampireManager.isVampire(turner)) {
                    sender.sendMessage("§c" + turner.getName() + " is not a vampire. Only vampires can turn humans.");
                    return true;
                }
            }

            this.plugin.getVampireManager().performVampireTurning(target, turner);
            return true;
        }
    }

    private boolean handleGiveTomeCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /givetome <player> <ability> [amount]");
            return true;

        } else {
            Player target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                sender.sendMessage("§cPlayer '" + args[0] + "' not found.");
                return true;

            } else {
                String abilityName = args[1];

                if (!this.tomeManager.isValidAbility(abilityName)) {
                    sender.sendMessage("§cUnknown tome ability: '" + abilityName + "'");
                    sender.sendMessage("§7Available abilities: " + String.join(", ", this.tomeManager.getAllAbilityNames()));
                    return true;

                } else {
                    int amount = 1;

                    if (args.length >= 3) {
                        try {
                            amount = Integer.parseInt(args[2]);

                            if (amount < 1 || amount > 64) {
                                sender.sendMessage("§cAmount must be between 1 and 64.");
                                return true;
                            }

                        } catch (NumberFormatException e) {
                            sender.sendMessage("§cInvalid amount: '" + args[2] + "'. Must be a number between 1 and 64.");
                            return true;
                        }
                    }

                    TomeAbility ability = this.tomeManager.getAbility(abilityName);
                    ItemStack tome = new ItemStack(Material.WRITTEN_BOOK, amount);
                    BookMeta bookMeta = (BookMeta)tome.getItemMeta();

                    if (bookMeta != null) {
                        String canonicalName = ability != null ? ability.getName() : abilityName;
                        bookMeta.setTitle(canonicalName);
                        bookMeta.setAuthor("§6A source unknown...");

                        if (ability != null) {
                            List<String> lore = new ArrayList<>();
                            String[] descriptionLines = ability.getDescriptionLines();

                            for(String line : descriptionLines) {
                                lore.add("§7" + line);
                            }

                            lore.add("");
                            lore.add("§eRight-click with this tome in hand to learn its secrets");
                            bookMeta.setLore(lore);
                        }

                        List<String> pages = new ArrayList<>();
                        StringBuilder pageContent = new StringBuilder();
                        pageContent.append("§5§lANCIENT KNOWLEDGE§r\n\n");
                        pageContent.append("§8The secrets of ").append(abilityName).append(" are contained within these pages.\n\n");

                        if (ability != null) {
                            String[] descriptionLines = ability.getDescriptionLines();

                            for(String line : descriptionLines) {
                                pageContent.append("§7").append(line).append("\n");
                            }
                        } else {
                            pageContent.append("§7No description available\n");
                        }

                        pageContent.append("\n§6Use this knowledge wisely, for it comes with great responsibility.");
                        pages.add(pageContent.toString());
                        bookMeta.setPages(pages);
                        tome.setItemMeta(bookMeta);
                    }

                    if (target.getInventory().firstEmpty() == -1) {
                        target.getWorld().dropItemNaturally(target.getLocation(), tome);
                        target.sendMessage("§eA mysterious tome appears at your feet...");

                    } else {
                        target.getInventory().addItem(tome);
                        target.sendMessage("§eA mysterious tome has appeared in your inventory...");
                    }

                    sender.sendMessage("§aGave " + amount + "x Tome of " + abilityName + " to " + target.getName() + ".");
                    return true;
                }
            }
        }
    }

    private boolean handleSelectTomesCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;

        } else if (args.length < 1) {
            sender.sendMessage("§cUsage: /pow admin select_tomes <player>");
            return true;

        } else {
            Player target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                sender.sendMessage("§cPlayer '" + args[0] + "' not found.");
            } else {
                this.tomeManager.openTomeSelectionGUI(admin, target);
            }

            return true;
        }
    }

    private boolean handleGiveCureBookCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /pow admin give_cure_book <player> <1|2|3|4>");
            return true;

        } else {
            Player target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                sender.sendMessage("§cPlayer '" + args[0] + "' not found.");
                return true;

            } else {
                int bookNum;

                try {
                    bookNum = Integer.parseInt(args[1]);
                    if (bookNum < 1 || bookNum > 4) {
                        sender.sendMessage("§cBook number must be 1, 2, 3, or 4.");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid book number: '" + args[1] + "'. Must be 1, 2, 3, or 4.");
                    return true;
                }

                ItemStack book = this.createCureBook(bookNum);

                if (target.getInventory().firstEmpty() == -1) {
                    target.getWorld().dropItemNaturally(target.getLocation(), book);
                    target.sendMessage("§5An ancient tome appears at your feet...");

                } else {
                    target.getInventory().addItem(book);
                    target.sendMessage("§5An ancient tome has appeared in your inventory...");
                }

                sender.sendMessage("§aGave Cure Book " + bookNum + " to §e" + target.getName());
                return true;
            }
        }
    }

    private ItemStack createCureBook(int bookNum) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta)book.getItemMeta();

        if (meta != null) {
            switch (bookNum) {
                case 1:
                    meta.setTitle("The Remedy 1/3");
                    meta.setAuthor("§5An ancient scholar");
                    meta.setPages("§5§lTHE REMEDY§r\n§8Part I of III\n\n§7In the darkest hours, when the cursed blood burns within your veins, know that salvation exists.\n\n§7The ancients spoke of a trinity of knowledge...", "§7...that when combined, can sever the unholy bond between mortal and monster.\n\n§7This is the first piece of that forbidden wisdom.\n\n§8Read on, seeker of the light...");
                    break;
                case 2:
                    meta.setTitle("The Cure 2/3");
                    meta.setAuthor("§5An ancient scholar");
                    meta.setPages("§5§lTHE CURE§r\n§8Part II of III\n\n§7The second fragment reveals the nature of the curse itself.\n\n§7Born of darkness, sustained by blood, the vampire's existence is a perversion of nature's order...", "§7...yet within this perversion lies the key to its undoing.\n\n§7Holy water, blessed by the righteous, weakens the bond.\n\n§8Continue your search, truth-seeker...");
                    break;
                case 3:
                    meta.setTitle("The Absolution 3/3");
                    meta.setAuthor("§5An ancient scholar");
                    meta.setPages("§5§lTHE ABSOLUTION§r\n§8Part III of III\n\n§7The final piece completes the trinity.\n\n§7With all three fragments of knowledge, the words of power are revealed:\n\n§6voluntate-mea-hoc-nefandum-vinculum-abicio", "§7Stand near a holy beacon, with holy water upon your person, beneath the light of day.\n\n§7Speak the words, and be free of the curse forevermore.\n\n§8May the light guide your path.");
                    break;
                case 4:
                    meta.setTitle("The Retribution 4/3");
                    meta.setAuthor("§4A vengeful spirit");
                    meta.setPages("§4§lTHE RETRIBUTION§r\n§8The Fourth Tome\n\n§7This knowledge was never meant to be found.\n\n§7While the trinity speaks of self-salvation, this tome reveals darker words - words of forced redemption...", "§7...or forced damnation.\n\n§4hoc-vinculum-tibi-dirumpo-mala-creatura\n\n§7With these words, you may force the choice upon another creature of the night.\n\n§8Use this power wisely, for it carries great consequence.");
            }

            List<String> lore = new ArrayList<>();
            lore.add("§5An ancient tome of forbidden knowledge");
            lore.add("§7Part " + bookNum + " of the cure series");
            lore.add("");
            lore.add("§eRead this book to absorb its wisdom");

            meta.setLore(lore);
            CureBookReadingListener.markAsAuthenticCureBook(meta, bookNum, this.plugin);
            book.setItemMeta(meta);
        }

        return book;
    }

    private boolean handleDistributeTomesCommand(CommandSender sender, String[] args) {
        if (this.plugin.getTomeDistributionManager().getTomeLocations().isEmpty()) {
            sender.sendMessage("§c§lWarning: §cNo tome chest locations are configured!");
            sender.sendMessage("§7Use §e/pow admin addtomechest §7to add tome chest locations first.");
            return true;

        } else {
            sender.sendMessage("§eTriggering tome distribution...");
            this.plugin.getTomeDistributionManager().triggerDistribution();
            sender.sendMessage("§aTome distribution complete. Check the tome locations for new tomes.");
            return true;
        }
    }

    private boolean handleAddTomeChestCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;

        } else {
            Location location = player.getLocation();

            if (this.plugin.getTomeDistributionManager().addTomeLocation(location)) {
                location.getBlock().setType(Material.CHEST);
                sender.sendMessage("§a✔ Successfully added tome chest location.");
                sender.sendMessage("§7Location: §e" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
                sender.sendMessage("§7A chest has been placed at this location.");
                sender.sendMessage("§7Total tome chest locations: §e" + this.plugin.getTomeDistributionManager().getTomeLocations().size());

            } else {
                sender.sendMessage("§c✖ This location already exists in the tome chest list.");
                sender.sendMessage("§7Location: §e" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
            }

            return true;
        }
    }

    private boolean handleRemoveTomeChestCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;

        } else {
            Location playerLocation = player.getLocation(), nearestLocation = null;
            List<Location> tomeLocations = this.plugin.getTomeDistributionManager().getTomeLocations();
            double nearestDistance = Double.MAX_VALUE;

            for(Location loc : tomeLocations) {
                if (loc.getWorld() != null && loc.getWorld().equals(playerLocation.getWorld())) {
                    double distance = playerLocation.distance(loc);

                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestLocation = loc;
                    }
                }
            }

            if (nearestLocation != null && !(nearestDistance > 10)) {
                if (nearestLocation.getBlock().getType() == Material.CHEST) {
                    nearestLocation.getBlock().setType(Material.AIR);
                    sender.sendMessage("§7Removed physical chest at location.");
                }

                if (this.plugin.getTomeDistributionManager().removeTomeLocation(nearestLocation)) {
                    sender.sendMessage("§a✔ Successfully removed tome chest location.");
                    sender.sendMessage("§7Location: §e" + nearestLocation.getBlockX() + ", " + nearestLocation.getBlockY() + ", " + nearestLocation.getBlockZ());
                    sender.sendMessage("§7Distance: §e" + String.format("%.1f", nearestDistance) + " blocks");
                    sender.sendMessage("§7Total tome chest locations: §e" + this.plugin.getTomeDistributionManager().getTomeLocations().size());

                } else {
                    sender.sendMessage("§c✖ Failed to remove tome chest location from config.");
                }
            } else {
                sender.sendMessage("§c✖ No tome chest found within 10 blocks.");
                sender.sendMessage("§7Move closer to a tome chest location and try again.");
            }

            return true;
        }
    }

    private boolean handleListTomeChestsCommand(CommandSender sender, String[] args) {
        List<Location> tomeLocations = this.plugin.getTomeDistributionManager().getTomeLocations();

        sender.sendMessage("§6§l=== TOME CHEST LOCATIONS ===");
        sender.sendMessage("§7Total: §e" + tomeLocations.size() + " locations");
        sender.sendMessage("");

        if (tomeLocations.isEmpty()) {
            sender.sendMessage("§7No tome chest locations configured.");
            sender.sendMessage("§7Use §e/pow admin addtomechest §7to add a location.");
        } else {
            int index = 1;

            for(Location loc : tomeLocations) {
                boolean hasChest = loc.getWorld() != null && loc.getBlock().getType() == Material.CHEST;
                String chestStatus = hasChest ? "§a✔" : "§c✖";
                String tpCommand = String.format("/tp %d %d %d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

                TextComponent indexPart = new TextComponent(String.format("§7%d. ", index));
                TextComponent coordsPart = new TextComponent(String.format("§e%d, %d, %d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
                coordsPart.setClickEvent(new ClickEvent(Action.RUN_COMMAND, tpCommand));
                coordsPart.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder("§aClick to teleport to this location")).create()));
                TextComponent statusPart = new TextComponent(String.format(" §7(chest: %s§7)", chestStatus));

                if (sender instanceof Player player) {
                    player.spigot().sendMessage(indexPart, coordsPart, statusPart);

                } else {
                    sender.sendMessage(String.format("§7%d. §e%d, %d, %d §7(chest: %s§7)", index, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), chestStatus));
                }

                ++index;
            }

            sender.sendMessage("");
            sender.sendMessage("§7Click a location to teleport, or use §e/pow admin removetomechest §7nearby to remove.");
        }

        return true;
    }

    private boolean handleClearBloodMoonBuffsCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /clearbloodmoonbuffs <player|all>");
            sender.sendMessage("§7This command removes stacked blood moon attribute modifiers");
            return true;

        } else {
            String target = args[0].toLowerCase();

            if (target.equals("all")) {
                int playersAffected = 0;

                for(Player player : Bukkit.getOnlinePlayers()) {
                    this.plugin.getBloodMoonAttributeListener().forceRemoveBloodMoonAttributes(player);
                    ++playersAffected;
                }

                sender.sendMessage("§aCleared blood moon buffs for §e" + playersAffected + " §aonline players.");
                this.plugin.logInfo("Admin " + sender.getName() + " cleared blood moon buffs for all players");

            } else {
                Player targetPlayer = Bukkit.getPlayer(target);

                if (targetPlayer == null) {
                    sender.sendMessage("§cPlayer '" + target + "' not found or not online.");
                    return true;
                }

                this.plugin.getBloodMoonAttributeListener().forceRemoveBloodMoonAttributes(targetPlayer);
                sender.sendMessage("§aCleared blood moon buffs for §e" + targetPlayer.getName() + "§a.");
                targetPlayer.sendMessage("§aAn admin has cleared your blood moon buffs.");
                this.plugin.logInfo("Admin " + sender.getName() + " cleared blood moon buffs for " + targetPlayer.getName());
            }

            return true;
        }
    }

    private boolean handleFixAttributesCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                this.plugin.getBloodMoonAttributeListener().forceCleanupOnJoin((Player)sender);
                sender.sendMessage("§aAggressively cleaned your attribute modifiers.");

            } else {
                sender.sendMessage("§cUsage: /fixattributes <player|all>");
            }

            return true;
        } else {
            String target = args[0].toLowerCase();

            if (target.equals("all")) {
                int playersAffected = 0;

                for(Player player : Bukkit.getOnlinePlayers()) {
                    this.plugin.getBloodMoonAttributeListener().forceCleanupOnJoin(player);
                    ++playersAffected;
                }

                sender.sendMessage("§aAggressively cleaned attribute modifiers for §e" + playersAffected + " §aonline players.");
                this.plugin.logInfo("Admin " + sender.getName() + " fixed attributes for all players");

            } else {
                Player targetPlayer = Bukkit.getPlayer(target);

                if (targetPlayer == null) {
                    sender.sendMessage("§cPlayer '" + target + "' not found or not online.");
                    return true;
                }

                this.plugin.getBloodMoonAttributeListener().forceCleanupOnJoin(targetPlayer);
                sender.sendMessage("§aAggressively cleaned attribute modifiers for §e" + targetPlayer.getName() + "§a.");
                this.plugin.logInfo("Admin " + sender.getName() + " fixed attributes for " + targetPlayer.getName());
            }

            return true;
        }
    }

    private boolean handleRemoveEndermenCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /removeendermen <all|toggle|status>");
            sender.sendMessage("§7- /removeendermen all §8- Remove all existing endermen from loaded chunks");
            sender.sendMessage("§7- /removeendermen toggle §8- Toggle enderman spawn prevention on/off");
            sender.sendMessage("§7- /removeendermen status §8- Check if enderman removal is enabled");
            return true;

        } else {
            switch (args[0].toLowerCase()) {
                case "all":
                    int removedCount = this.plugin.getEndermanRemovalListener().removeAllEndermen();
                    sender.sendMessage("§aRemoved §e" + removedCount + " §aendermen from all loaded chunks.");
                    this.plugin.logInfo("Admin " + sender.getName() + " removed " + removedCount + " endermen");
                    break;
                case "toggle":
                    boolean currentStatus = this.plugin.getEndermanRemovalListener().isEndermanRemovalEnabled();
                    this.plugin.getEndermanRemovalListener().setEndermanRemovalEnabled(!currentStatus);
                    String newStatus = !currentStatus ? "ENABLED" : "DISABLED";
                    sender.sendMessage("§aEnderman removal is now §e" + newStatus + "§a.");
                    this.plugin.logInfo("Admin " + sender.getName() + " toggled enderman removal to " + newStatus);
                    break;
                case "status":
                    String statusMessage = this.plugin.getEndermanRemovalListener().isEndermanRemovalEnabled() ? "§aENABLED" : "§cDISABLED";
                    sender.sendMessage("§7Enderman removal is currently: " + statusMessage);
                    break;
                default:
                    sender.sendMessage("§cInvalid action. Use 'all', 'toggle', or 'status'.");
            }

            return true;
        }
    }

    private boolean handleDamageSuppressionCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /pow admin damagesuppression <set|get> [percentage]");
            sender.sendMessage("§7- /pow admin damagesuppression get §8- Show current damage suppression percentage");
            sender.sendMessage("§7- /pow admin damagesuppression set <percentage> §8- Set damage suppression (0-100)");
            return true;

        } else {
            switch (args[0].toLowerCase()) {
                case "get":
                    try {
                        int suppressionScore = this.plugin.getConfig().getInt("damage_suppression", 0);
                        sender.sendMessage("§7Current damage suppression: §e" + suppressionScore + "%");
                    } catch (Exception e) {
                        sender.sendMessage("§cError reading damage suppression: " + e.getMessage());
                    }
                    break;
                case "set":
                    if (args.length < 2) {
                        sender.sendMessage("§cUsage: /pow admin damagesuppression set <percentage>");
                        sender.sendMessage("§7Example: /pow admin damagesuppression set 50 (for 50% damage reduction)");
                        return true;
                    }

                    try {
                        int percentage = Integer.parseInt(args[1]);
                        if (percentage >= 0 && percentage <= 100) {
                            this.plugin.getConfig().set("damage_suppression", percentage);
                            this.plugin.saveConfig();

                            sender.sendMessage("§aDamage suppression set to §e" + percentage + "%");
                            sender.sendMessage("§7Changes are active immediately (no server restart required)");
                            this.plugin.logInfo("Admin " + sender.getName() + " set damage suppression to " + percentage + "%");
                            break;
                        }

                        sender.sendMessage("§cPercentage must be between 0 and 100.");
                        return true;
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cInvalid number format. Use whole numbers (e.g., 50).");
                    } catch (Exception e) {
                        sender.sendMessage("§cError setting damage suppression: " + e.getMessage());
                    }
                    break;
                default:
                    sender.sendMessage("§cInvalid action. Use 'set' or 'get'.");
            }

            return true;
        }
    }

    private boolean handleSetupPlayerCommand(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /setupplayer <playername>");
            return true;

        } else {
            Player target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                sender.sendMessage("§cPlayer '" + args[0] + "' not found.");
                return true;

            } else {
                Random random = new Random();

                List<ItemStack> starterItems = new ArrayList<>();
                starterItems.add(new ItemStack(Material.COOKED_CHICKEN, 1 + random.nextInt(4)));
                starterItems.add(new ItemStack(Material.COOKED_SALMON, 1 + random.nextInt(4)));
                starterItems.add(new ItemStack(Material.BREAD, 1 + random.nextInt(4)));
                starterItems.add(new ItemStack(Material.BAKED_POTATO, 1 + random.nextInt(4)));
                starterItems.add(new ItemStack(Material.STONE_SWORD, 1));
                starterItems.add(new ItemStack(Material.STONE_PICKAXE, 1));
                starterItems.add(new ItemStack(Material.STONE_AXE, 1));

                int itemsGiven = 0, itemsDropped = 0;

                for(ItemStack item : starterItems) {
                    if (target.getInventory().firstEmpty() != -1) {
                        target.getInventory().addItem(item);
                        ++itemsGiven;
                    } else {
                        target.getWorld().dropItemNaturally(target.getLocation(), item);
                        ++itemsDropped;
                    }
                }

                if (itemsGiven > 0) {
                    target.sendMessage("§aYou have received starter items. (" + itemsGiven + " items added to inventory)");
                }

                if (itemsDropped > 0) {
                    target.sendMessage("§eYour inventory was full. " + itemsDropped + " items were dropped at your feet.");
                }

                sender.sendMessage("§aGave starter items to " + target.getName() + ". (" + itemsGiven + " in inventory, " + itemsDropped + " dropped)");
                return true;
            }
        }
    }

    private boolean handleSpawnAnimalsCommand(CommandSender sender, String[] args) {
        sender.sendMessage("§eManually spawning passive animals across the world...");
        this.plugin.getPassiveMobSpawningManager().triggerSpawning();
        sender.sendMessage("§aPassive mob spawning complete. Check console for spawn details.");
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("vampiresmp.admin")) {
            return new ArrayList<>();

        } else {
            List<String> completions = new ArrayList<>();

            if (command.getName().equalsIgnoreCase("session")) {
                if (args.length == 1) {
                    completions.addAll(Arrays.asList("start", "pause", "end", "prime", "resume", "building"));
                }

            } else if (command.getName().equalsIgnoreCase("vampire")) {
                if (args.length == 1) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                } else if (args.length == 2) {
                    completions.addAll(Arrays.asList("human", "1", "2", "3", "turn", "clearcap", "clearban"));

                } else if (args.length == 3 && args[1].equalsIgnoreCase("turn")) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (this.vampireManager.isVampire(player)) {
                            completions.add(player.getName());
                        }
                    }
                }

            } else if (command.getName().equalsIgnoreCase("config")) {
                if (args.length == 1) {
                    completions.addAll(Arrays.asList("help", "alert_on_quit", "holy_water_cap", "tome_cap", "vampire_level_cap", "new_vampire_tracking", "allow_vampire_mounts", "cure_requires_dead_sire", "enable_npc_mobs", "stake_permadeath_stage", "human_life_limit"));

                } else if (args.length == 2) {
                    if (args[0].equals("stake_permadeath_stage")) {
                        completions.addAll(Arrays.asList("1", "2", "3"));
                    } else {
                        completions.addAll(Arrays.asList("true", "false"));
                    }
                }

            } else if (command.getName().equalsIgnoreCase("beacon")) {
                if (args.length == 1) {
                    completions.addAll(Arrays.asList("add", "remove", "list", "info", "stats", "reload", "validate", "holy", "desecrated", "neutral", "fix", "refresh", "cleanup", "clearcooldowns", "debug"));

                } else if (args.length == 2) {
                    String subCommand = args[0].toLowerCase();

                    if (!subCommand.equals("remove") && !subCommand.equals("delete") && !subCommand.equals("info") && !subCommand.equals("holy") && !subCommand.equals("desecrated") && !subCommand.equals("desecrate") && !subCommand.equals("neutral")) {
                        if (subCommand.equals("add")) {
                            completions.add("<beacon_name>");
                        }
                    } else {
                        completions.addAll(this.beaconManager.getBeaconNames());
                    }
                } else if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
                    completions.addAll(Arrays.asList("5", "10", "15", "20", "25"));
                }
            } else if (command.getName().equalsIgnoreCase("cooldowns")) {
                if (args.length == 1) {
                    completions.addAll(Arrays.asList("reset", "clear"));

                } else if (args.length == 2 && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("clear"))) {
                    for(Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                }
            } else if (command.getName().equalsIgnoreCase("break_warning")) {
                completions.clear();

            } else if (command.getName().equalsIgnoreCase("givetome")) {
                if (args.length == 1) {
                    for(Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                } else if (args.length == 2) {
                    completions.addAll(this.tomeManager.getAllAbilityNames());

                } else if (args.length == 3) {
                    completions.addAll(Arrays.asList("1", "5", "10", "16", "32", "64"));
                }
            } else if (command.getName().equalsIgnoreCase("distributetomes")) {
                completions.clear();

            } else if (command.getName().equalsIgnoreCase("clearbloodmoonbuffs")) {
                if (args.length == 1) {
                    completions.add("all");

                    for(Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                }
            } else if (command.getName().equalsIgnoreCase("fixattributes")) {
                if (args.length == 1) {
                    completions.add("all");

                    for(Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                }
            } else if (command.getName().equalsIgnoreCase("removeendermen")) {
                if (args.length == 1) {
                    completions.addAll(Arrays.asList("all", "toggle", "status"));
                }
            } else if (command.getName().equalsIgnoreCase("setupplayer")) {
                if (args.length == 1) {
                    for(Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                }
            } else if (command.getName().equalsIgnoreCase("spawnanimals")) {
                completions.clear();
            }

            if (args.length > 0) {
                String input = args[args.length - 1].toLowerCase();
                completions.removeIf((s) -> !s.toLowerCase().startsWith(input));
            }

            return completions;
        }
    }

    private void applyVampireNightVision(Player player) {
        PotionEffect nightVision = new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 0, false, false, false);
        player.addPotionEffect(nightVision);
    }

    private void sendVampireTexturePackPrompt(Player player) {
        TextComponent textureMessage = new TextComponent("§7Apply the vampire texture pack: ");
        TextComponent clickableText = new TextComponent("§c§n[CLICK HERE]");

        clickableText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/pow texture vampire"));
        clickableText.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder("§7Click to apply the vampire texture pack")).create()));

        textureMessage.addExtra(clickableText);
        player.spigot().sendMessage(textureMessage);
    }
}
