/*
 * Decompiled with CFR 0.153-SNAPSHOT (d6f6758-dirty).
 * 
 * Could not load the following classes:
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.md_5.bungee.api.chat.ClickEvent
 *  net.md_5.bungee.api.chat.ClickEvent$Action
 *  net.md_5.bungee.api.chat.ComponentBuilder
 *  net.md_5.bungee.api.chat.HoverEvent
 *  net.md_5.bungee.api.chat.HoverEvent$Action
 *  net.md_5.bungee.api.chat.TextComponent
 *  org.bukkit.Bukkit
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.attribute.Attribute
 *  org.bukkit.attribute.AttributeInstance
 *  org.bukkit.attribute.AttributeModifier
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scoreboard.Objective
 *  org.bukkit.scoreboard.Scoreboard
 */
package frostvein.sampires.remakepire.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.beacons.BeaconSite;

public class InitGameManager {
    private final RemakepirePlugin plugin;
    private static final double BORDER_BUFFER = 50.0;
    private static final String COMMAND_PREFIX = "/pow_init_internal_";
    private final Map<UUID, InitState> adminStates = new HashMap<>();
    private final Map<UUID, InitData> adminData = new HashMap<>();
    private final Map<UUID, Boolean> guiRefreshInProgress = new HashMap<>();
    private static final int PLAYERS_PER_PAGE = 45, INVENTORY_SIZE = 54;

    /**
     * Create an instance of the Initialize Game manager.
     *
     * @param plugin the host plugin object.
     */
    public InitGameManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Begin a new game of Vampires.
     *
     * @param admin the player running the initialization command.
     */
    public void startInitialization(Player admin) {
        UUID adminId = admin.getUniqueId();
        this.adminStates.put(adminId, InitGameManager.InitState.AWAITING_FIRST_CONFIRM);
        this.adminData.put(adminId, new InitData());

        admin.sendMessage("§c§l========================================");
        admin.sendMessage("§c§lWARNING: GAME INITIALIZATION");
        admin.sendMessage("§c§l========================================");
        admin.sendMessage("");
        admin.sendMessage("§7You are about to start a §lbrand new game§7 of Vampires.");
        admin.sendMessage("§7This will:");
        admin.sendMessage("§7  • Reset all player tags and inventories");
        admin.sendMessage("§7  • Neutralize all beacons");
        admin.sendMessage("§7  • Reset the session");
        admin.sendMessage("§7  • Teleport all online players");
        admin.sendMessage("§7  • Assign new vampires");
        admin.sendMessage("");

        TextComponent confirmMessage = new TextComponent("§7Are you sure? ");
        TextComponent clickableText = new TextComponent("§e§n[CLICK HERE TO CONTINUE]");
        clickableText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, COMMAND_PREFIX + "confirm1"));
        clickableText.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder("§7Click to proceed with initialization")).create()));
        confirmMessage.addExtra(clickableText);

        admin.spigot().sendMessage(confirmMessage);
        admin.sendMessage("");
        admin.sendMessage("§7Type §e/pow admin init cancel §7at any time to cancel.");
        admin.sendMessage("§c§l========================================");
    }

    /**
     * Prompt the admin on the method of choosing vampires.
     *
     * @param admin the player running the initialization command.
     */
    public void handleFirstConfirmation(Player admin) {
        UUID adminId = admin.getUniqueId();

        if (this.adminStates.get(adminId) != InitGameManager.InitState.AWAITING_FIRST_CONFIRM) {
            admin.sendMessage("§cError: Invalid initialization state.");

        } else {
            this.adminStates.put(adminId, InitGameManager.InitState.AWAITING_MODE_SELECTION);

            admin.sendMessage("");
            admin.sendMessage("§6§l========================================");
            admin.sendMessage("§6How would you like to assign vampires?");
            admin.sendMessage("§6§l========================================");
            admin.sendMessage("");
            admin.sendMessage("§7Type §e/pow admin init cancel §7to cancel.");
            admin.sendMessage("");

            TextComponent randomButton = new TextComponent("§a§l[RANDOM] ");
            randomButton.setClickEvent(new ClickEvent(Action.RUN_COMMAND, COMMAND_PREFIX + "mode_random"));
            randomButton.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder("§7Randomly select vampires from online players")).create()));

            TextComponent selectedButton = new TextComponent("§b§l[SELECTED]");
            selectedButton.setClickEvent(new ClickEvent(Action.RUN_COMMAND, COMMAND_PREFIX + "mode_selected"));
            selectedButton.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder("§7Manually choose which players become vampires")).create()));

            TextComponent buttonMessage = new TextComponent("");
            buttonMessage.addExtra(randomButton);
            buttonMessage.addExtra(selectedButton);

            admin.spigot().sendMessage(buttonMessage);
            admin.sendMessage("");
        }
    }

    /**
     * Set the game initialization to randomize the vampire selection and prompt the admin for the minimum number of starting vampires.
     *
     * @param admin the player running the initialization command.
     */
    public void handleRandomMode(Player admin) {
        UUID adminId = admin.getUniqueId();

        if (this.adminStates.get(adminId) != InitGameManager.InitState.AWAITING_MODE_SELECTION) {
            admin.sendMessage("§cError: Invalid initialization state.");

        } else {
            InitData data = this.adminData.get(adminId);
            data.mode = InitGameManager.InitData.VampireMode.RANDOM;
            this.adminStates.put(adminId, InitGameManager.InitState.AWAITING_MIN_VAMPIRES);

            admin.sendMessage("");
            admin.sendMessage("§e§l========================================");
            admin.sendMessage("§eWhat should the §lminimum§e number of starting vampires be?");
            admin.sendMessage("§7Please type a number in chat (must be 0 or more).");
            admin.sendMessage("§7Type §e/pow admin init cancel §7to cancel.");
            admin.sendMessage("§e§l========================================");
            admin.sendMessage("");
        }
    }

    /**
     * Set the game initialization to allow the admin to choose the starting vampire.
     *
     * @param admin the player running the initialization command.
     */
    public void handleSelectedMode(Player admin) {
        UUID adminId = admin.getUniqueId();

        if (this.adminStates.get(adminId) != InitGameManager.InitState.AWAITING_MODE_SELECTION) {
            admin.sendMessage("§cError: Invalid initialization state.");

        } else {
            InitData data = this.adminData.get(adminId);
            data.mode = InitGameManager.InitData.VampireMode.SELECTED;
            this.openPlayerSelectionGUI(admin);
        }
    }

    /**
     * Create the inventory menu for the admin to select the starting vampires from.
     *
     * @param admin the player running the initialization command.
     */
    public void openPlayerSelectionGUI(Player admin) {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        int playerCount = onlinePlayers.size();

        if (playerCount == 0) {
            admin.sendMessage("§cNo players are online to select.");
            this.cancelInitialization(admin);

        } else {
            InitData data = this.adminData.get(admin.getUniqueId());
            int totalPages = (int)Math.ceil((double) playerCount / PLAYERS_PER_PAGE), currentPage = Math.min(data.currentPage, totalPages - 1);
            data.currentPage = currentPage;
            int slot = 0, startIndex = currentPage * PLAYERS_PER_PAGE, endIndex = Math.min(startIndex + PLAYERS_PER_PAGE, playerCount);
            Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, "§4§lSelect Vampires");

            for(int i = startIndex; i < endIndex; ++i) {
                Player player = onlinePlayers.get(i);
                boolean isVampire = data.selectedVampires.contains(player.getUniqueId());
                ItemStack item = new ItemStack(isVampire ? Material.EXPERIENCE_BOTTLE : Material.GLASS_BOTTLE);
                ItemMeta meta = item.getItemMeta();

                if (isVampire) {
                    meta.setDisplayName("§4" + player.getName() + " - Vampire");
                } else {
                    meta.setDisplayName("§a" + player.getName() + " - Human");
                }

                List<String> lore = new ArrayList<>();
                lore.add("§7Click to toggle");
                meta.setLore(lore);
                item.setItemMeta(meta);
                inventory.setItem(slot, item);
                ++slot;
            }

            if (currentPage > 0) {
                ItemStack prevButton = new ItemStack(Material.ARROW);
                ItemMeta prevMeta = prevButton.getItemMeta();
                prevMeta.setDisplayName("§e« Previous Page");
                List<String> prevLore = new ArrayList<>();
                prevLore.add("§7Go to page " + currentPage);
                prevMeta.setLore(prevLore);
                prevButton.setItemMeta(prevMeta);
                inventory.setItem(45, prevButton);
            }

            ItemStack pageIndicator = new ItemStack(Material.PAPER);
            ItemMeta pageMeta = pageIndicator.getItemMeta();
            pageMeta.setDisplayName("§fPage " + (currentPage + 1) + " of " + totalPages);
            List<String> pageLore = new ArrayList<>();
            pageLore.add("§7" + playerCount + " players total");
            pageLore.add("§7" + data.selectedVampires.size() + " selected as vampires");
            pageMeta.setLore(pageLore);
            pageIndicator.setItemMeta(pageMeta);
            inventory.setItem(49, pageIndicator);

            if (currentPage < totalPages - 1) {
                ItemStack nextButton = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = nextButton.getItemMeta();
                nextMeta.setDisplayName("§eNext Page »");
                List<String> nextLore = new ArrayList<>();
                nextLore.add("§7Go to page " + (currentPage + 2));
                nextMeta.setLore(nextLore);
                nextButton.setItemMeta(nextMeta);
                inventory.setItem(50, nextButton);
            }

            ItemStack confirmButton = new ItemStack(Material.LIME_CONCRETE);
            ItemMeta confirmMeta = confirmButton.getItemMeta();
            confirmMeta.setDisplayName("§a§lCONFIRM SELECTION");
            List<String> confirmLore = new ArrayList<>();
            confirmLore.add("§7Click to proceed with these selections");
            confirmLore.add("§7Selected: §e" + data.selectedVampires.size() + " vampires");
            confirmMeta.setLore(confirmLore);
            confirmButton.setItemMeta(confirmMeta);
            inventory.setItem(53, confirmButton);
            admin.openInventory(inventory);
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.guiRefreshInProgress.remove(admin.getUniqueId()), 1L);
        }
    }

    /**
     * Refresh the selection GUI and navigate the admin between pages.
     *
     * @param admin the player running the initialization command.
     * @param delta the change in page index.
     */
    public void handlePageChange(Player admin, int delta) {
        UUID adminId = admin.getUniqueId();
        InitData data = this.adminData.get(adminId);

        if (data != null) {
            data.currentPage += delta;
            this.guiRefreshInProgress.put(adminId, true);
            this.openPlayerSelectionGUI(admin);
        }
    }

    /**
     * Add or remove the selected player from the list of starting vampires.
     *
     * @param admin the player running the initialization command.
     * @param playerName the player being added or removed from the list.
     */
    public void handlePlayerToggle(Player admin, String playerName) {
        UUID adminId = admin.getUniqueId();
        InitData data = this.adminData.get(adminId);

        if (data != null && data.mode == InitGameManager.InitData.VampireMode.SELECTED) {
            Player targetPlayer = Bukkit.getPlayer(playerName);

            if (targetPlayer != null) {
                UUID targetId = targetPlayer.getUniqueId();

                if (data.selectedVampires.contains(targetId)) {
                    data.selectedVampires.remove(targetId);
                } else {
                    data.selectedVampires.add(targetId);
                }

                this.guiRefreshInProgress.put(adminId, true);
                this.openPlayerSelectionGUI(admin);
            }
        }
    }

    /**
     * Prompt the admin with a confirmation once the vampires are selected.
     *
     * @param admin the player running the initialization command.
     */
    public void handleGUIConfirmation(Player admin) {
        UUID adminId = admin.getUniqueId();
        InitData data = this.adminData.get(adminId);

        if (data != null && data.mode == InitGameManager.InitData.VampireMode.SELECTED) {
            this.adminStates.put(adminId, InitGameManager.InitState.AWAITING_FINAL_CONFIRM);
            admin.closeInventory();
            this.showFinalConfirmation(admin);
        }
    }

    /**
     * Set the minimum number of initialization vampires and prompt the admin for the maximum number of starting vampires.
     *
     * @param admin the player running the initialization command.
     * @param input the minimum number of vampires.
     * @return {@code true} if the initialization command state was waiting for the minimum vampires input.
     */
    public boolean handleMinVampiresInput(Player admin, String input) {
        UUID adminId = admin.getUniqueId();

        if (this.adminStates.get(adminId) != InitGameManager.InitState.AWAITING_MIN_VAMPIRES) {
            return false;
        } else {
            try {
                int min = Integer.parseInt(input.trim());

                if (min < 0) {
                    admin.sendMessage("§cThe minimum must be 0 or more. Please try again:");
                    return true;

                } else {
                    InitData data = this.adminData.get(adminId);
                    data.minVampires = min;
                    this.adminStates.put(adminId, InitGameManager.InitState.AWAITING_MAX_VAMPIRES);
                    admin.sendMessage("§a✓ Minimum vampires set to: §e" + min);
                    admin.sendMessage("");
                    admin.sendMessage("§e§l========================================");
                    admin.sendMessage("§eWhat should the §lmaximum§e number of vampires be?");
                    admin.sendMessage("§7Please type a number in chat (must be " + min + " or more).");
                    admin.sendMessage("§7Type §e/pow admin init cancel §7to cancel.");
                    admin.sendMessage("§e§l========================================");
                    admin.sendMessage("");
                    return true;
                }
            } catch (NumberFormatException e) {
                admin.sendMessage("§c'" + input + "' is not a valid number. Please try again:");
                return true;
            }
        }
    }

    /**
     * Set the maximum number of initialization vampires.
     *
     * @param admin the player running the initialization command.
     * @param input the maximum number of vampires.
     * @return {@code true} if the initialization command state was waiting for the maximum vampires input.
     */
    public boolean handleMaxVampiresInput(Player admin, String input) {
        UUID adminId = admin.getUniqueId();

        if (this.adminStates.get(adminId) != InitGameManager.InitState.AWAITING_MAX_VAMPIRES) {
            return false;

        } else {
            InitData data = this.adminData.get(adminId);

            try {
                int max = Integer.parseInt(input.trim());

                if (max < data.minVampires) {
                    admin.sendMessage("§cThe maximum must be " + data.minVampires + " or more. Please try again:");
                    return true;

                } else {
                    data.maxVampires = max;
                    this.adminStates.put(adminId, InitGameManager.InitState.AWAITING_FINAL_CONFIRM);
                    admin.sendMessage("§a✓ Maximum vampires set to: §e" + max);
                    admin.sendMessage("");
                    this.showFinalConfirmation(admin);
                    return true;
                }
            } catch (NumberFormatException e) {
                admin.sendMessage("§c'" + input + "' is not a valid number. Please try again:");
                return true;
            }
        }
    }

    /**
     * Show the admin their starting choices and prompt them to begin the game.
     *
     * @param admin the player running the initialization command.
     */
    private void showFinalConfirmation(Player admin) {
        UUID adminId = admin.getUniqueId();
        InitData data = this.adminData.get(adminId);

        admin.sendMessage("");
        admin.sendMessage("§a§l========================================");
        admin.sendMessage("§a§lFINAL CONFIRMATION");
        admin.sendMessage("§a§l========================================");
        admin.sendMessage("");
        admin.sendMessage("§7Configuration:");

        if (data.mode == InitGameManager.InitData.VampireMode.RANDOM) {
            admin.sendMessage("§7  • Mode: §eRandom");
            admin.sendMessage("§7  • Vampires: §e" + data.minVampires + "-" + data.maxVampires);

        } else {
            admin.sendMessage("§7  • Mode: §eManually Selected");
            admin.sendMessage("§7  • Vampires: §e" + data.selectedVampires.size() + " players");
        }

        admin.sendMessage("");
        admin.sendMessage("§7This will reset the entire game state.");
        admin.sendMessage("");

        TextComponent confirmMessage = new TextComponent("§7Ready to begin? ");
        TextComponent clickableText = new TextComponent("§a§l§n[START GAME]");
        clickableText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, COMMAND_PREFIX + "execute"));
        clickableText.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder("§7Click to initialize the game")).create()));
        confirmMessage.addExtra(clickableText);

        admin.spigot().sendMessage(confirmMessage);
        admin.sendMessage("§a§l========================================");
    }

    /**
     * Activate the new Vampires game and reset the game state.
     *
     * @param admin the player running the initialization command.
     */
    public void executeInitialization(Player admin) {
        UUID adminId = admin.getUniqueId();

        if (this.adminStates.get(adminId) != InitGameManager.InitState.AWAITING_FINAL_CONFIRM) {
            admin.sendMessage("§cError: Invalid initialization state.");

        } else {
            InitData data = this.adminData.get(adminId);
            admin.sendMessage("");
            admin.sendMessage("§6§l========================================");
            admin.sendMessage("§6§lINITIALIZING GAME...");
            admin.sendMessage("§6§l========================================");
            World world = this.plugin.getServer().getWorld("world");

            if (world == null) {
                admin.sendMessage("§cError: World 'world' not found.");
                this.cancelInitialization(admin);

            } else {
                admin.sendMessage("§7[1/9] Neutralizing beacons...");

                for (BeaconSite beacon : this.plugin.getBeaconManager().getAllBeacons()) {
                    this.plugin.getBeaconManager().setBeaconNeutral(beacon.getName(), true);
                    Location beaconLoc = beacon.getLocation();

                    if (beaconLoc != null && beaconLoc.getWorld() != null) {
                        beaconLoc.getBlock().setType(Material.BARRIER);
                    }
                }

                if (this.plugin.getBeaconManager().getBeacon("castle") != null) {
                    this.plugin.getBeaconManager().setBeaconDesecrated("castle");
                    admin.sendMessage("§7  → Castle beacon set to desecrated");
                }

                admin.sendMessage("§7[2/9] Clearing beacon cooldowns...");
                this.plugin.getBeaconManager().clearAllBeaconCooldownsForNewSession();
                admin.sendMessage("§7[3/9] Resetting player data...");

                Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();

                for(Player player : onlinePlayers) {
                    for(String tag : new HashSet<>(player.getScoreboardTags())) {
                        player.removeScoreboardTag(tag);
                    }

                    player.getInventory().clear();
                }

                admin.sendMessage("§7[3.5/9] Resetting scoreboard objectives...");
                Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

                for(Objective obj : new HashSet<>(mainScoreboard.getObjectives())) {
                    if (obj.getName().startsWith("vsmp_")) {
                        String name = obj.getName();
                        String displayName = obj.getDisplayName();
                        String criteria = obj.getCriteria();
                        obj.unregister();
                        mainScoreboard.registerNewObjective(name, criteria, displayName);
                    }
                }

                for(Player player : onlinePlayers) {
                    AttributeInstance healthAttr = player.getAttribute(Attribute.MAX_HEALTH);

                    for(AttributeModifier modifier : healthAttr.getModifiers()) {
                        healthAttr.removeModifier(modifier);
                    }

                    healthAttr.setBaseValue(20.0);
                    player.setHealth(20.0);
                    player.setLevel(0);
                    player.setExp(0.0F);
                    player.setTotalExperience(0);
                }

                try {
                    Objective deathObjective = mainScoreboard.getObjective("vsmp_death");

                    if (deathObjective != null) {
                        for(Player player : onlinePlayers) {
                            deathObjective.getScore(player.getName()).setScore(0);
                        }

                        admin.sendMessage("§7  → Reset death counts for all players");
                    }
                } catch (Exception e) {
                    this.plugin.getLogger().warning("Failed to reset death scoreboard: " + e.getMessage());
                }

                admin.sendMessage("§7[4/9] Priming new session and incrementing game ID...");
                this.plugin.getSessionManager().primeNewSession();
                this.plugin.getSessionManager().incrementGameID();

                admin.sendMessage("§7[4.5/9] Resetting game state flags...");
                this.plugin.getConfig().set("first_beacon_converted", false);
                this.plugin.getConfig().set("humans_own_all_beacons", false);
                this.plugin.getConfig().set("vampires_own_all_beacons", false);
                this.plugin.getConfig().set("one_human_left", false);
                this.plugin.getConfig().set("fourth_book_has_spawned", false);
                this.plugin.getConfig().set("fourth_book_spawn_enabled", false);
                this.plugin.saveConfig();

                admin.sendMessage("§7[4.6/9] Clearing sire mappings...");
                this.plugin.getSireManager().clearAllSireMappings();
                admin.sendMessage("§7[4.7/9] Stopping vampire tracking...");

                if (this.plugin.getVampireTrackingManager() != null) {
                    this.plugin.getVampireTrackingManager().stopAllTracking();
                }

                admin.sendMessage("§7[4.8/9] Clearing permadeath preferences...");
                this.plugin.getPermadeathManager().clearAllPermadeathModes();
                admin.sendMessage("§7[5/9] Setting world time and border...");

                world.setFullTime(1L);
                world.getWorldBorder().setSize(900000.0);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule playersNetherPortalDefaultDelay 80");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule locatorBar false");
                admin.sendMessage("§7[6/9] Applying saturation effect...");

                for(Player player : onlinePlayers) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 200, 9));
                }

                admin.sendMessage("§7[7/9] Teleporting players...");

                for(Player player : onlinePlayers) {
                    if (player.getGameMode() != GameMode.SURVIVAL) {
                        GameMode oldMode = player.getGameMode();
                        player.setGameMode(GameMode.SURVIVAL);
                        admin.sendMessage("§7  → Reset " + player.getName() + " from " + oldMode.name().toLowerCase() + " to survival");
                    }

                    Location teleportLoc = this.getRandomTeleportLocation(world);

                    if (teleportLoc != null) {
                        player.teleport(teleportLoc);
                    } else {
                        admin.sendMessage("§cWarning: Could not find valid teleport location for " + player.getName());
                    }
                }

                admin.sendMessage("§7[8/9] Assigning vampires...");

                List<Player> playersToConvert = new ArrayList<>();

                if (data.mode == InitGameManager.InitData.VampireMode.RANDOM) {
                    int vampireCount = ThreadLocalRandom.current().nextInt(data.minVampires, data.maxVampires + 1);
                    List<Player> availablePlayers = new ArrayList<>(onlinePlayers);
                    Collections.shuffle(availablePlayers);
                    vampireCount = Math.min(vampireCount, availablePlayers.size());
                    playersToConvert = availablePlayers.subList(0, vampireCount);

                } else {
                    for(Player player : onlinePlayers) {
                        if (data.selectedVampires.contains(player.getUniqueId())) {
                            playersToConvert.add(player);
                        }
                    }
                }

                Set<UUID> vampireIds = new HashSet<>();

                for(Player player : playersToConvert) {
                    this.plugin.getVampireManager().setPlayerAsVampire(player, 1);
                    vampireIds.add(player.getUniqueId());

                    player.setExp(0.5F);
                    player.sendTitle("§4§lVampire", "", 10, 100, 20);
                    player.sendMessage("");
                    player.sendMessage("§4§l========================================");
                    player.sendMessage("§cYou are a creature of the night, and it is time to feed.");
                    player.sendMessage("");
                    player.sendMessage("§7What to do: Turn other humans by 'killing' them when no one is looking. As a level 1 vampire, there are very few ways you can be found out, but still be cautious. You cannot help turn beacons, eating food is bad but stomachable for now, only attack during the night. Press \"k\" to customize your vampire ability keybinds.");
                    player.sendMessage("§4§l========================================");
                    player.sendMessage("");

                    TextComponent textureMessage = new TextComponent("§7Apply the vampire texture pack: ");
                    TextComponent clickableText = new TextComponent("§c§n[CLICK HERE]");
                    clickableText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/pow texture vampire"));
                    clickableText.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder("§7Click to apply the vampire texture pack")).create()));
                    textureMessage.addExtra(clickableText);

                    player.spigot().sendMessage(textureMessage);
                }

                admin.sendMessage("§7  → Converted " + playersToConvert.size() + " players to vampires");

                for(Player player : onlinePlayers) {
                    if (!vampireIds.contains(player.getUniqueId())) {
                        player.addScoreboardTag("human");
                        player.sendTitle("§e§lHuman", "", 10, 100, 20);
                        player.sendMessage("");
                        player.sendMessage("§e§l========================================");
                        player.sendMessage("§7Welcome to " + plugin.getConfigManager().getTownName() + ". Survive, consecrate beacons, find tomes, and above all: Fear the night.");
                        player.sendMessage("§e§l========================================");
                        player.sendMessage("");
                    }
                }

                admin.sendMessage("§7[9/11] Starting session...");
                this.plugin.getSessionManager().startSession();
                admin.sendMessage("§7[10/11] Distributing tomes to chests...");

                if (this.plugin.getTomeDistributionManager().getTomeLocations().isEmpty()) {
                    admin.sendMessage("§e  → No tome chest locations configured, skipping tome distribution");
                } else {
                    this.plugin.getTomeDistributionManager().triggerDistribution();
                    admin.sendMessage("§7  → Tomes distributed to " + this.plugin.getTomeDistributionManager().getTomeLocations().size() + " chest locations");
                }

                admin.sendMessage("§7[11/11] Clearing potion effects...");

                for(Player player : onlinePlayers) {
                    for(PotionEffect effect : player.getActivePotionEffects()) {
                        player.removePotionEffect(effect.getType());
                    }
                }

                this.plugin.getVampireTurningManager().enableAllVampireTurning();
                admin.sendMessage("");
                admin.sendMessage("§a§l========================================");
                admin.sendMessage("§a§lGAME INITIALIZED SUCCESSFULLY.");
                admin.sendMessage("§a§l========================================");
                admin.sendMessage("§7Players: §e" + onlinePlayers.size());
                admin.sendMessage("§7Vampires: §c" + playersToConvert.size());
                admin.sendMessage("§7Humans: §a" + (onlinePlayers.size() - playersToConvert.size()));
                admin.sendMessage("§a§l========================================");
                this.adminStates.remove(adminId);
                this.adminData.remove(adminId);
            }
        }
    }

    /**
     * Determine a random location with the config boundaries where players can spawn when a game begins.
     *
     * @param world the world hosting the plugin interactions.
     * @return A location to spawn a player at.
     */
    private Location getRandomTeleportLocation(World world) {
        int maxAttempts = 50;
        ConfigManager config = this.plugin.getConfigManager();

        double townCenterX = config.getTownCenterX();
        double townCenterZ = config.getTownCenterZ();
        double teleportRadius = config.getTeleportRadius();

        double minX = config.getBorderMinX();
        double maxX = config.getBorderMaxX();
        double minZ = config.getBorderMinZ();
        double maxZ = config.getBorderMaxZ();

        for(int attempt = 0; attempt < maxAttempts; ++attempt) {
            double angle = ThreadLocalRandom.current().nextDouble() * 2.0 * Math.PI;
            double distance = Math.sqrt(ThreadLocalRandom.current().nextDouble()) * teleportRadius;
            double x = townCenterX + distance * Math.cos(angle);
            double z = townCenterZ + distance * Math.sin(angle);

            if (!(x < minX + BORDER_BUFFER) && !(x > maxX - BORDER_BUFFER) && !(z < minZ + BORDER_BUFFER) && !(z > maxZ - BORDER_BUFFER)) {
                Location loc = new Location(world, x, (double)(world.getHighestBlockYAt((int)x, (int)z) + 1), z);

                if (loc.getY() > 0 && loc.getY() < world.getMaxHeight()) {
                    return loc;
                }
            }
        }

        return new Location(world, townCenterX, world.getHighestBlockYAt((int)townCenterX, (int)townCenterZ) + 1, townCenterZ);
    }

    /**
     * Cancel the new game initialization process.
     *
     * @param admin the player running the initialization command.
     */
    public void cancelInitialization(Player admin) {
        UUID adminId = admin.getUniqueId();
        this.adminStates.remove(adminId);
        this.adminData.remove(adminId);
        admin.sendMessage("§cGame initialization cancelled.");
    }

    /**
     * Retrieve if a command is an internal init command from this plugin.
     *
     * @param command the command being checked.
     * @return {@code true} if the command starts with "/pow_init_internal_"
     */
    public boolean isInternalCommand(String command) {
        return command.startsWith(COMMAND_PREFIX);
    }

    /**
     * Execute the internal initialization commands from the plugin as the game initialization process progresses.
     *
     * @param admin the player running the initialization command.
     * @param command the command being run.
     * @return {@code true} if a command was executed.
     */
    public boolean handleInternalCommand(Player admin, String command) {
        if (!command.startsWith(COMMAND_PREFIX)) {
            return false;

        } else {
            switch (command.substring(COMMAND_PREFIX.length())) {
                case "confirm1":
                    this.handleFirstConfirmation(admin);
                    return true;
                case "mode_random":
                    this.handleRandomMode(admin);
                    return true;
                case "mode_selected":
                    this.handleSelectedMode(admin);
                    return true;
                case "execute":
                    this.executeInitialization(admin);
                    return true;
                default:
                    return false;
            }
        }
    }

    /**
     * Retrieve if the admin is currently initializing a new Vampires game.
     *
     * @param adminId the id of the player running the initialization command.
     * @return {@code true} if the admin is in the process of initializing a game.
     */
    public boolean isInInitialization(UUID adminId) {
        return this.adminStates.containsKey(adminId);
    }

    /**
     * Retrieve the current progress of the initializing process.
     *
     * @param adminId the id of the player running the initialization command.
     * @return The current state of the initiation process.
     */
    public InitState getState(UUID adminId) {
        return this.adminStates.getOrDefault(adminId, InitGameManager.InitState.IDLE);
    }

    /**
     * Retrieve if the player is manually selecting the starting vampires.
     *
     * @param title the name of the current event.
     * @return {@code true} if the current event has the admin manually selecting vampires.
     */
    public boolean isPlayerSelectionGUI(String title) {
        return title.equals("§4§lSelect Vampires");
    }

    /**
     * Retrieve if the selection GUI is currently refreshing pages.
     *
     * @param adminId the id of the player running the initialization command.
     * @return {@code true} if the selection menu is reloading.
     */
    public boolean isGUIRefreshInProgress(UUID adminId) {
        return this.guiRefreshInProgress.getOrDefault(adminId, false);
    }

    public static enum InitState {
        IDLE,
        AWAITING_FIRST_CONFIRM,
        AWAITING_MODE_SELECTION,
        AWAITING_MIN_VAMPIRES,
        AWAITING_MAX_VAMPIRES,
        AWAITING_FINAL_CONFIRM
    }

    public static class InitData {
        VampireMode mode;
        int minVampires;
        int maxVampires;
        Set<UUID> selectedVampires = new HashSet<>();
        int currentPage = 0;

        public static enum VampireMode {
            RANDOM,
            SELECTED
        }
    }
}
