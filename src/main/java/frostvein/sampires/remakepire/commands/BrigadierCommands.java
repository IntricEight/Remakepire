package frostvein.sampires.remakepire.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.beacons.BeaconSite;

@SuppressWarnings("unchecked")
public class BrigadierCommands {
    private final RemakepirePlugin plugin;
    private final PowCommand powCommand;
    private final VampireCureCommand cureCommand;
    private final ForcedVampireCureCommand forcedCureCommand;
    private static final List<String> VAMPIRE_ABILITIES = Arrays.asList("bat", "lunge", "vanish", "stormcall", "beacontravel", "vision");
    private static final List<String> TOME_ABILITIES = Arrays.asList("blessing", "banishundead", "holyword", "enlightenedeye", "lanternthrash", "prayeroffaith", "rallyingcry", "shoulderbarge", "turnundead", "uncannydirection", "unnaturalhaste", "wayoftheland", "wayofthelumberjack", "wayoftheprospector", "stopthebleeding");

    public BrigadierCommands(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.powCommand = new PowCommand(plugin);
        this.cureCommand = new VampireCureCommand(plugin);
        this.forcedCureCommand = new ForcedVampireCureCommand(plugin);
    }

    public void registerAll() {
        this.plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, (event) -> {
            Commands commands = event.registrar();
            this.registerPowCommand(commands);

            // Register the unique cure commands
            this.registerLatinCureCommand(commands);
            this.registerLatinForcedCureCommand(commands);

            this.plugin.logInfo("All Brigadier commands registered successfully!");
        });
    }

    private void registerPowCommand(Commands commands) {
        commands.register(((LiteralArgumentBuilder) ((LiteralArgumentBuilder) Commands.literal("pow")
                .then(Commands.literal("help").executes((ctx) -> this.executePowCommand(ctx, "help"))))

                .then(this.buildVampireAbilitySubcommand())
                .then(this.buildTomeAbilitySubcommand())

                .then(Commands.literal("beaconstatus").executes((ctx) -> this.executePowCommand(ctx, "beaconstatus")))
                .then(Commands.literal("holysites").executes((ctx) -> this.executePowCommand(ctx, "beaconstatus")))
                .then(Commands.literal("holy").executes((ctx) -> this.executePowCommand(ctx, "beaconstatus")))

                .then(this.buildTextureSubcommand())
                .then(Commands.literal("texturepack").executes((ctx) -> this.executePowCommand(ctx, "texture")))
                .then(Commands.literal("resourcepack").executes((ctx) -> this.executePowCommand(ctx, "texture")))

                .then(((LiteralArgumentBuilder) Commands.literal("permadeath")
                        .then(Commands.literal("on").executes((ctx) -> this.executePowCommand(ctx, "permadeath", "on"))))
                        .then(Commands.literal("off").executes((ctx) -> this.executePowCommand(ctx, "permadeath", "off")))
                        .then(Commands.literal("absolute").executes((ctx) -> this.executePowCommand(ctx, "permadeath", "absolute"))))
                .then(((LiteralArgumentBuilder) Commands.literal("toggle-permadeath")
                        .then(Commands.literal("on").executes((ctx) -> this.executePowCommand(ctx, "permadeath", "on"))))
                        .then(Commands.literal("off").executes((ctx) -> this.executePowCommand(ctx, "permadeath", "off")))
                        .then(Commands.literal("absolute").executes((ctx) -> this.executePowCommand(ctx, "permadeath", "absolute"))))

                .then(Commands.literal("toggle-turning").executes((ctx) -> this.executePowCommand(ctx, "toggle-turning")))
                .then(Commands.literal("turning").executes((ctx) -> this.executePowCommand(ctx, "toggle-turning")))

                .then(Commands.literal("sendmessage").executes((ctx) -> this.executePowCommand(ctx, "sendmessage")))
                .then(Commands.literal("sendpendingmessage").executes((ctx) -> this.executePowCommand(ctx, "sendmessage")))

                .then(Commands.literal("reopen").executes((ctx) -> this.executePowCommand(ctx, "reopen")))
                .then(Commands.literal("forcedcure-reopen").executes((ctx) -> this.executePowCommand(ctx, "reopen")))

                .then(((LiteralArgumentBuilder) Commands.literal("admin").requires((source) -> source.getSender().hasPermission("vampiresmp.admin")))
                        .then(((LiteralArgumentBuilder) Commands.literal("init").executes((ctx) -> this.executePowCommand(ctx, "admin", "init")))
                                .then(Commands.literal("cancel").executes((ctx) -> this.executePowCommand(ctx, "admin", "init", "cancel"))))
                        .then(this.buildSessionSubcommand())
                        .then(this.buildVampireSubcommand())
                        .then(this.buildConfigSubcommand())
                        .then(this.buildBeaconSubcommand())
                        .then(this.buildVampireCooldownSubcommand())
                        .then(this.buildTomeCooldownSubcommand())

                        .then(Commands.literal("onehumanleft").executes((ctx) -> this.executePowCommand(ctx, "admin", "onehumanleft")))

                        .then(((LiteralArgumentBuilder) Commands.literal("vampirehealthcheck")
                                .then(Commands.literal("get").executes((ctx) -> this.executePowCommand(ctx, "admin", "vampirehealthcheck", "get"))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 1000)).executes((ctx) -> {
                                            int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                                            return this.executePowCommand(ctx, "admin", "vampirehealthcheck", "set", String.valueOf(ticks));
                                        }))))

                        .then(Commands.literal("break_warning").executes((ctx) -> this.executePowCommand(ctx, "admin", "break_warning")))

                        .then(Commands.literal("givetome")
                                .then(Commands.argument("player", StringArgumentType.word()).suggests((ctx, builder) -> this.suggestOnlinePlayers(builder))
                                        .then(((RequiredArgumentBuilder) Commands.argument("ability", StringArgumentType.word()).suggests((ctx, builder) -> this.suggestAllTomeAbilities(builder)).executes((ctx) -> {
                                            String player = StringArgumentType.getString(ctx, "player");
                                            String ability = StringArgumentType.getString(ctx, "ability");
                                            return this.executePowCommand(ctx, "admin", "givetome", player, ability);
                                        }))
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64)).executes((ctx) -> {
                                                    String player = StringArgumentType.getString(ctx, "player");
                                                    String ability = StringArgumentType.getString(ctx, "ability");
                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                                    return this.executePowCommand(ctx, "admin", "givetome", player, ability, String.valueOf(amount));
                                                })))))
                        .then(Commands.literal("select_tomes").executes((ctx) -> {
                                    String senderName = ctx.getSource().getSender().getName();
                                    return this.executePowCommand(ctx, "admin", "select_tomes", senderName);
                                })
                                .then(Commands.argument("player", StringArgumentType.word()).suggests((ctx, builder) -> this.suggestOnlinePlayers(builder)).executes((ctx) -> {
                                    String player = StringArgumentType.getString(ctx, "player");
                                    return this.executePowCommand(ctx, "admin", "select_tomes", player);
                                })))
                        .then(Commands.literal("give_cure_book")
                                .then(Commands.argument("player", StringArgumentType.word()).suggests((ctx, builder) -> this.suggestOnlinePlayers(builder))
                                        .then(Commands.argument("book_number", IntegerArgumentType.integer(1, 4)).executes((ctx) -> {
                                            String player = StringArgumentType.getString(ctx, "player");
                                            int bookNum = IntegerArgumentType.getInteger(ctx, "book_number");
                                            return this.executePowCommand(ctx, "admin", "give_cure_book", player, String.valueOf(bookNum));
                                        }))))
                        .then(Commands.literal("distributetomes").executes((ctx) -> this.executePowCommand(ctx, "admin", "distributetomes")))

                        .then(((LiteralArgumentBuilder) Commands.literal("clearbloodmoonbuffs")
                                .then(Commands.literal("all").executes((ctx) -> this.executePowCommand(ctx, "admin", "clearbloodmoonbuffs", "all"))))
                                .then(Commands.argument("player", StringArgumentType.word()).suggests((ctx, builder) -> this.suggestOnlinePlayers(builder)).executes((ctx) -> {
                                    String player = StringArgumentType.getString(ctx, "player");
                                    return this.executePowCommand(ctx, "admin", "clearbloodmoonbuffs", player);
                                })))
                        .then(((LiteralArgumentBuilder) Commands.literal("fixattributes").executes((ctx) -> this.executePowCommand(ctx, "admin", "fixattributes")))
                                .then(Commands.literal("all").executes((ctx) -> this.executePowCommand(ctx, "admin", "fixattributes", "all")))
                                .then(Commands.argument("player", StringArgumentType.word()).suggests((ctx, builder) -> this.suggestOnlinePlayers(builder)).executes((ctx) -> {
                                    String player = StringArgumentType.getString(ctx, "player");
                                    return this.executePowCommand(ctx, "admin", "fixattributes", player);
                                })))
                        .then(((LiteralArgumentBuilder) Commands.literal("removeendermen")
                                .then(Commands.literal("all").executes((ctx) -> this.executePowCommand(ctx, "admin", "removeendermen", "all"))))
                                .then(Commands.literal("toggle").executes((ctx) -> this.executePowCommand(ctx, "admin", "removeendermen", "toggle")))
                                .then(Commands.literal("status").executes((ctx) -> this.executePowCommand(ctx, "admin", "removeendermen", "status"))))
                        .then(((LiteralArgumentBuilder) Commands.literal("damagesuppression")
                                .then(Commands.literal("get").executes((ctx) -> this.executePowCommand(ctx, "admin", "damagesuppression", "get"))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("percentage", IntegerArgumentType.integer(0, 100)).executes((ctx) -> {
                                            int percentage = IntegerArgumentType.getInteger(ctx, "percentage");
                                            return this.executePowCommand(ctx, "admin", "damagesuppression", "set", String.valueOf(percentage));
                                        }))))
                        .then(Commands.literal("setupplayer")
                                .then(Commands.argument("player", StringArgumentType.word()).suggests((ctx, builder) -> this.suggestOnlinePlayers(builder)).executes((ctx) -> {
                                    String player = StringArgumentType.getString(ctx, "player");
                                    return this.executePowCommand(ctx, "admin", "setupplayer", player);
                                })))
                        .then(Commands.literal("spawnanimals").executes((ctx) -> this.executePowCommand(ctx, "admin", "spawnanimals")))
                        .then(Commands.literal("addtomechest").executes((ctx) -> this.executePowCommand(ctx, "admin", "addtomechest")))
                        .then(Commands.literal("removetomechest").executes((ctx) -> this.executePowCommand(ctx, "admin", "removetomechest")))
                        .then(Commands.literal("listtomechests").executes((ctx) -> this.executePowCommand(ctx, "admin", "listtomechests")))
                        .then(Commands.literal("resetplayer")
                                .then(((RequiredArgumentBuilder) Commands.argument("player", StringArgumentType.word()).suggests((ctx, builder) -> this.suggestOnlinePlayers(builder)).executes((ctx) -> {
                                    String player = StringArgumentType.getString(ctx, "player");
                                    return this.executePowCommand(ctx, "admin", "resetplayer", player);
                                }))
                                        .then(Commands.argument("clearInventory", BoolArgumentType.bool()).executes((ctx) -> {
                                            String player = StringArgumentType.getString(ctx, "player");
                                            boolean clearInv = BoolArgumentType.getBool(ctx, "clearInventory");
                                            return this.executePowCommand(ctx, "admin", "resetplayer", player, String.valueOf(clearInv));
                                        }))))
                        .then(((LiteralArgumentBuilder) Commands.literal("set_vampire_spawn").executes((ctx) -> this.executePowCommand(ctx, "admin", "set_vampire_spawn")))
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                                .then(Commands.argument("z", IntegerArgumentType.integer()).executes((ctx) -> {
                                                    int x = IntegerArgumentType.getInteger(ctx, "x");
                                                    int y = IntegerArgumentType.getInteger(ctx, "y");
                                                    int z = IntegerArgumentType.getInteger(ctx, "z");
                                                    return this.executePowCommand(ctx, "admin", "set_vampire_spawn", String.valueOf(x), String.valueOf(y), String.valueOf(z));
                                                }))))
                        )
                ).executes((ctx) -> this.executePowCommand(ctx, "help"))).build(), "VampireSMP main command", List.of("vampiresmp", "vsmp"));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildVampireAbilitySubcommand() {
        return Commands.literal("vability")
                    .then(Commands.literal("list").executes((ctx) -> this.executePowCommand(ctx, "vability", "list")))
                    .then(Commands.literal("all").executes((ctx) -> this.executePowCommand(ctx, "vability", "all")))
                    .then(Commands.argument("ability", StringArgumentType.word()).suggests((ctx, builder) -> this.suggestVampireAbilities(builder)).executes((ctx) -> {
                        String ability = StringArgumentType.getString(ctx, "ability");
                        return this.executePowCommand(ctx, "vability", ability);
                    }));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildTomeAbilitySubcommand() {
        return Commands.literal("tome")
                    .then(Commands.literal("list").executes((ctx) -> this.executePowCommand(ctx, "tome", "list")))
                    .then(Commands.argument("ability", StringArgumentType.word()).suggests((ctx, builder) -> this.suggestTomeAbilities(ctx, builder)).executes((ctx) -> {
                        String ability = StringArgumentType.getString(ctx, "ability");
                        return this.executePowCommand(ctx, "tome", ability);
                    }));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildTextureSubcommand() {
        return Commands.literal("texture")
                .executes((ctx) -> this.executePowCommand(ctx, "texture"))
                .then(Commands.literal("all").executes((ctx) -> this.executePowCommand(ctx, "texture", "all")))
                .then(Commands.literal("force").executes((ctx) -> this.executePowCommand(ctx, "texture", "force")))
                .then(Commands.literal("vampire").executes((ctx) -> this.executePowCommand(ctx, "texture", "vampire")))
                .then(Commands.literal("human").executes((ctx) -> this.executePowCommand(ctx, "texture", "human")));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildAdminSubcommand() {
        // TODO: If we do this at all. Otherwise, remove.
        return null;
    }







    private LiteralArgumentBuilder<CommandSourceStack> buildSessionSubcommand() {
        return Commands.literal("session")
                .then(Commands.literal("start").executes((ctx) -> this.executePowCommand(ctx, "admin", "session", "start")))
                .then(Commands.literal("pause").executes((ctx) -> this.executePowCommand(ctx, "admin", "session", "pause")))
                .then(Commands.literal("end").executes((ctx) -> this.executePowCommand(ctx, "admin", "session", "end")))
                .then(Commands.literal("prime").executes((ctx) -> this.executePowCommand(ctx, "admin", "session", "prime")))
                .then(Commands.literal("resume").executes((ctx) -> this.executePowCommand(ctx, "admin", "session", "resume")))
                .then(Commands.literal("building").executes((ctx) -> this.executePowCommand(ctx, "admin", "session", "building")));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildVampireSubcommand() {
        return (LiteralArgumentBuilder) Commands.literal("vampire")
                .then(((RequiredArgumentBuilder) Commands.argument("player", StringArgumentType.word()).suggests((ctx, builder) -> this.suggestOnlinePlayers(builder))
                        .then(Commands.literal("human").executes((ctx) -> {
                            String player = StringArgumentType.getString(ctx, "player");
                            return this.executePowCommand(ctx, "admin", "vampire", player, "human");
                        })))
                        .then(Commands.literal("1").executes((ctx) -> {
                            String player = StringArgumentType.getString(ctx, "player");
                            return this.executePowCommand(ctx, "admin", "vampire", player, "1");
                        }))
                        .then(Commands.literal("2").executes((ctx) -> {
                            String player = StringArgumentType.getString(ctx, "player");
                            return this.executePowCommand(ctx, "admin", "vampire", player, "2");
                        }))
                        .then(Commands.literal("3").executes((ctx) -> {
                            String player = StringArgumentType.getString(ctx, "player");
                            return this.executePowCommand(ctx, "admin", "vampire", player, "3");
                        }))
                        .then(((LiteralArgumentBuilder) Commands.literal("turn").executes((ctx) -> {
                            String player = StringArgumentType.getString(ctx, "player");
                            return this.executePowCommand(ctx, "admin", "vampire", player, "turn");
                        }))
                                .then(Commands.argument("turner", StringArgumentType.word()).suggests((ctx, builder) -> this.suggestOnlinePlayers(builder)).executes((ctx) -> {
                                    String player = StringArgumentType.getString(ctx, "player");
                                    String turner = StringArgumentType.getString(ctx, "turner");
                                    return this.executePowCommand(ctx, "admin", "vampire", player, "turn", turner);
                                })))
                        .then(Commands.literal("clearcap").executes((ctx) -> {
                            String player = StringArgumentType.getString(ctx, "player");
                            return this.executePowCommand(ctx, "admin", "vampire", player, "clearcap");
                        }))
                        .then(Commands.literal("clearban").executes((ctx) -> {
                            String player = StringArgumentType.getString(ctx, "player");
                            return this.executePowCommand(ctx, "admin", "vampire", player, "clearban");
                        })));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildConfigSubcommand() {
        return Commands.literal("config")
                .then(Commands.literal("help").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "help")))

                .then(Commands.literal("alert_on_quit").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "alert_on_quit"))
                        .then(Commands.literal("true").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "alert_on_quit", "true")))
                        .then(Commands.literal("false").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "alert_on_quit", "false")))
                )

                .then(Commands.literal("holy_water_cap").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "holy_water_cap"))
                        .then(Commands.literal("true").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "holy_water_cap", "true")))
                        .then(Commands.literal("false").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "holy_water_cap", "false")))
                )

                .then(Commands.literal("tome_cap").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "tome_cap"))
                        .then(Commands.literal("true").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "tome_cap", "true")))
                        .then(Commands.literal("false").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "tome_cap", "false")))
                )

                .then(Commands.literal("vampire_level_cap").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "vampire_level_cap"))
                        .then(Commands.literal("true").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "vampire_level_cap", "true")))
                        .then(Commands.literal("false").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "vampire_level_cap", "false")))
                )

                .then(Commands.literal("new_vampire_tracking").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "new_vampire_tracking"))
                        .then(Commands.literal("true").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "new_vampire_tracking", "true")))
                        .then(Commands.literal("false").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "new_vampire_tracking", "false")))
                )

                .then(Commands.literal("allow_vampire_mounts").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "allow_vampire_mounts"))
                        .then(Commands.literal("true").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "allow_vampire_mounts", "true")))
                        .then(Commands.literal("false").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "allow_vampire_mounts", "false")))
                )

                .then(Commands.literal("cure_requires_dead_sire").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "cure_requires_dead_sire"))
                        .then(Commands.literal("true").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "cure_requires_dead_sire", "true")))
                        .then(Commands.literal("false").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "cure_requires_dead_sire", "false")))
                )

                .then(Commands.literal("enable_npc_mobs").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "enable_npc_mobs"))
                        .then(Commands.literal("true").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "enable_npc_mobs", "true")))
                        .then(Commands.literal("false").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "enable_npc_mobs", "false")))
                )

                .then(Commands.literal("stake_permadeath_stage").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "stake_permadeath_stage"))
                        .then(Commands.literal("1").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "stake_permadeath_stage", "1")))
                        .then(Commands.literal("2").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "stake_permadeath_stage", "2")))
                        .then(Commands.literal("3").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "stake_permadeath_stage", "3")))
                )

                .then(Commands.literal("human_life_limit").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "human_life_limit"))
                        .then(Commands.literal("true").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "human_life_limit", "true")))
                        .then(Commands.literal("false").executes(ctx -> this.executePowCommand(ctx, "admin", "config", "human_life_limit", "false")))
                );
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildBeaconSubcommand() {
        return (LiteralArgumentBuilder) Commands.literal("beacon")
                .then(Commands.literal("add")
                        .then(((RequiredArgumentBuilder) Commands.argument("name", StringArgumentType.word()).executes((ctx) -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            return this.executePowCommand(ctx, "admin", "beacon", "add", name);
                        }))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 100)).executes((ctx) -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    int radius = IntegerArgumentType.getInteger(ctx, "radius");
                                    return this.executePowCommand(ctx, "admin", "beacon", "add", name, String.valueOf(radius));
                                }))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.word()).suggests((ctx, builder) -> this.suggestBeaconNames(builder)).executes((ctx) -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            return this.executePowCommand(ctx, "admin", "beacon", "remove", name);
                        })))
                .then(Commands.literal("list").executes((ctx) -> this.executePowCommand(ctx, "admin", "beacon", "list")))
                .then(Commands.literal("info")
                        .then(Commands.argument("name", StringArgumentType.word()).suggests((ctx, builder) -> this.suggestBeaconNames(builder)).executes((ctx) -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            return this.executePowCommand(ctx, "admin", "beacon", "info", name);
                        })))
                .then(Commands.literal("stats").executes((ctx) -> this.executePowCommand(ctx, "admin", "beacon", "stats")))
                .then(Commands.literal("reload").executes((ctx) -> this.executePowCommand(ctx, "admin", "beacon", "reload")))
                .then(Commands.literal("validate").executes((ctx) -> this.executePowCommand(ctx, "admin", "beacon", "validate")))
                .then(Commands.literal("holy")
                        .then(Commands.argument("name", StringArgumentType.word()).suggests((ctx, builder) -> this.suggestBeaconNames(builder)).executes((ctx) -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            return this.executePowCommand(ctx, "admin", "beacon", "holy", name);
                        })))
                .then(Commands.literal("desecrated")
                        .then(Commands.argument("name", StringArgumentType.word()).suggests((ctx, builder) -> this.suggestBeaconNames(builder)).executes((ctx) -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            return this.executePowCommand(ctx, "admin", "beacon", "desecrated", name);
                        })))
                .then(Commands.literal("neutral")
                        .then(Commands.argument("name", StringArgumentType.word()).suggests((ctx, builder) -> this.suggestBeaconNames(builder)).executes((ctx) -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            return this.executePowCommand(ctx, "admin", "beacon", "neutral", name);
                        })))
                .then(Commands.literal("fix").executes((ctx) -> this.executePowCommand(ctx, "admin", "beacon", "fix")))
                .then(Commands.literal("refresh").executes((ctx) -> this.executePowCommand(ctx, "admin", "beacon", "refresh")))
                .then(Commands.literal("cleanup").executes((ctx) -> this.executePowCommand(ctx, "admin", "beacon", "cleanup")))
                .then(Commands.literal("clearcooldowns").executes((ctx) -> this.executePowCommand(ctx, "admin", "beacon", "clearcooldowns")))
                .then(((LiteralArgumentBuilder) Commands.literal("debug").executes((ctx) -> this.executePowCommand(ctx, "admin", "beacon", "debug")))
                        .then(Commands.argument("name", StringArgumentType.word()).suggests((ctx, builder) -> this.suggestBeaconNames(builder)).executes((ctx) -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            return this.executePowCommand(ctx, "admin", "beacon", "debug", name);
                        })));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildVampireCooldownSubcommand() {
        return (LiteralArgumentBuilder) Commands.literal("vampirecooldowns")
                .then(((LiteralArgumentBuilder) Commands.literal("reset").executes((ctx) -> this.executePowCommand(ctx, "admin", "vampirecooldowns", "reset")))
                        .then(Commands.argument("player", StringArgumentType.word()).suggests((ctx, builder) -> this.suggestOnlinePlayers(builder)).executes((ctx) -> {
                            String player = StringArgumentType.getString(ctx, "player");
                            return this.executePowCommand(ctx, "admin", "vampirecooldowns", "reset", player);
                        })))
                .then(((LiteralArgumentBuilder) Commands.literal("clear").executes((ctx) -> this.executePowCommand(ctx, "admin", "vampirecooldowns", "clear")))
                        .then(Commands.argument("player", StringArgumentType.word()).suggests((ctx, builder) -> this.suggestOnlinePlayers(builder)).executes((ctx) -> {
                            String player = StringArgumentType.getString(ctx, "player");
                            return this.executePowCommand(ctx, "admin", "vampirecooldowns", "clear", player);
                        })));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildTomeCooldownSubcommand() {
        return Commands.literal("resettomecooldowns").executes((ctx) -> this.executePowCommand(ctx, "admin", "resettomecooldowns"))
                .then(Commands.argument("player", StringArgumentType.word()).suggests((ctx, builder) -> this.suggestOnlinePlayers(builder)).executes((ctx) -> {
                    String player = StringArgumentType.getString(ctx, "player");
                    return this.executePowCommand(ctx, "admin", "resettomecooldowns", player);
                }));
    }

    private void registerLatinCureCommand(Commands commands) {
        commands.register(((LiteralArgumentBuilder) Commands.literal("voluntate-mea-hoc-nefandum-vinculum-abicio").executes((ctx) -> {
            CommandSender sender = (ctx.getSource()).getSender();
            this.cureCommand.onCommand(sender, null, "voluntate-mea-hoc-nefandum-vinculum-abicio", new String[0]);
            return 1;
        })).build(), "Cure yourself from vampirism");
    }

    private void registerLatinForcedCureCommand(Commands commands) {
        commands.register(((LiteralArgumentBuilder) Commands.literal("hoc-vinculum-tibi-dirumpo-mala-creatura")
                .then(Commands.argument("player", StringArgumentType.word()).suggests((ctx, builder) -> this.suggestOnlinePlayers(builder)).executes((ctx) -> {
                    CommandSender sender = (ctx.getSource()).getSender();
                    String playerName = StringArgumentType.getString(ctx, "player");
                    this.forcedCureCommand.onCommand(sender, null, "hoc-vinculum-tibi-dirumpo-mala-creatura", new String[]{playerName});
                    return 1;
                }))).build(), "Force a vampire back to humanity");
    }

    private int executePowCommand(CommandContext<CommandSourceStack> ctx, String... args) {
        CommandSender sender = (ctx.getSource()).getSender();

        Command dummyCommand = new BukkitCommand("pow") {
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                return false;
            }
        };

        this.powCommand.onCommand(sender, dummyCommand, "pow", args);
        return 1;
    }

    private CompletableFuture<Suggestions> suggestOnlinePlayers(SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(remaining)) {
                builder.suggest(player.getName());
            }
        }

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestVampireAbilities(SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();

        for (String ability : VAMPIRE_ABILITIES) {
            if (ability.startsWith(remaining)) {
                builder.suggest(ability);
            }
        }

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestTomeAbilities(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        CommandSender sender = (ctx.getSource()).getSender();

        if (sender instanceof Player player) {
            for (String ability : this.plugin.getTomeManager().getPlayerAbilities(player)) {
                if (ability.toLowerCase().startsWith(remaining)) {
                    builder.suggest(ability);
                }
            }
        }

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestAllTomeAbilities(SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();

        for (String ability : TOME_ABILITIES) {
            if (ability.startsWith(remaining)) {
                builder.suggest(ability);
            }
        }

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestBeaconNames(SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();

        for (BeaconSite beacon : this.plugin.getBeaconManager().getAllBeacons()) {
            if (beacon.getName().toLowerCase().startsWith(remaining)) {
                builder.suggest(beacon.getName());
            }
        }

        return builder.buildFuture();
    }
}
