package frostvein.sampires.remakepire.managers;

import java.time.Duration;
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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
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
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.beacons.BeaconSite;
import frostvein.sampires.remakepire.utils.ItemTypeChecking;

public class InitGameManager {
    private final RemakepirePlugin plugin;
    private static final double BORDER_BUFFER = 50.0;
    private static final String COMMAND_PREFIX = "/pow_init_internal_";
    private final Map<UUID, InitState> adminStates = new HashMap<>();
    private final Map<UUID, InitData> adminData = new HashMap<>();
    private final Map<UUID, Boolean> guiRefreshInProgress = new HashMap<>();
    private static final int PLAYERS_PER_PAGE = 45, INVENTORY_SIZE = 54;
    public static final Component SELECT_VAMPIRES_GUI_TITLE = Component.text("Select Vampires", NamedTextColor.DARK_RED)
            .decorate(TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false);

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

        admin.sendMessage(Component.text("========================================", NamedTextColor.RED).decorate(TextDecoration.BOLD).append(Component.newline())
                .append(Component.text("WARNING: GAME INITIALIZATION")).append(Component.newline())
                .append(Component.text("========================================"))
        );

        admin.sendMessage("");
        admin.sendMessage(Component.text("You are about to start a ", NamedTextColor.GRAY)
                .append(Component.text("brand new game", NamedTextColor.GRAY)
                        .decorate(TextDecoration.BOLD))
                .append(Component.text(" of Vampires - Remakepire Edition.")
                        .decoration(TextDecoration.BOLD, false))
        );
        admin.sendMessage(Component.text("This will:", NamedTextColor.GRAY).append(Component.newline())
                .append(Component.text("  • Reset all player tags and inventories")).append(Component.newline())
                .append(Component.text("  • Neutralize all beacons")).append(Component.newline())
                .append(Component.text("  • Reset the session")).append(Component.newline())
                .append(Component.text("  • Teleport all online players")).append(Component.newline())
                .append(Component.text("  • Assign new vampires"))
        );
        admin.sendMessage("");

        admin.sendMessage(Component.text("Are you sure? ", NamedTextColor.GRAY)
                .append(Component.text("[CLICK HERE TO CONTINUE]", NamedTextColor.YELLOW)
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.runCommand(COMMAND_PREFIX + "confirm1"))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to proceed with initialization", NamedTextColor.GRAY))))
        );

        admin.sendMessage("");
        admin.sendMessage(Component.text("Type ", NamedTextColor.GRAY)
                .append(Component.text("/pow admin init cancel", NamedTextColor.YELLOW))
                .append(Component.text(" at any time to cancel.", NamedTextColor.GRAY))
        );
        admin.sendMessage(Component.text("========================================", NamedTextColor.RED)
                .decorate(TextDecoration.BOLD));
    }

    /**
     * Prompt the admin on the method of choosing vampires.
     *
     * @param admin the player running the initialization command.
     */
    public void handleFirstConfirmation(Player admin) {
        UUID adminId = admin.getUniqueId();

        if (this.adminStates.get(adminId) != InitGameManager.InitState.AWAITING_FIRST_CONFIRM) {
            admin.sendMessage(Component.text("Error: Invalid initialization state.", NamedTextColor.RED));

        } else {
            this.adminStates.put(adminId, InitGameManager.InitState.AWAITING_MODE_SELECTION);

            admin.sendMessage("");
            admin.sendMessage(Component.text("========================================", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).append(Component.newline())
                    .append(Component.text("How would you like to assign vampires?").decoration(TextDecoration.BOLD, false)).append(Component.newline())
                    .append(Component.text("========================================").decorate(TextDecoration.BOLD))
            );
            admin.sendMessage("");
            admin.sendMessage(Component.text("Type ", NamedTextColor.GRAY)
                    .append(Component.text("/pow admin init cancel", NamedTextColor.YELLOW))
                    .append(Component.text(" to cancel.", NamedTextColor.GRAY))
            );
            admin.sendMessage("");
            admin.sendMessage(Component.text("[RANDOM] ", NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand(COMMAND_PREFIX + "mode_random"))
                    .hoverEvent(HoverEvent.showText(Component.text("Randomly select vampires from online players", NamedTextColor.GRAY)))
                    .append(Component.text("[SELECTED]", NamedTextColor.AQUA)
                            .decorate(TextDecoration.BOLD)
                            .clickEvent(ClickEvent.runCommand(COMMAND_PREFIX + "mode_selected"))
                            .hoverEvent(HoverEvent.showText(Component.text("Manually choose which players become vampires", NamedTextColor.GRAY)))
                    )
            );
            admin.sendMessage("");
        }
    }

    /**
     * Set the game initialization to randomize the vampire selection and prompt the admin for the minimum number of starting vampires.
     *
     * @param admin the player running the initialization command.
     */
    public void handleRandomMode(Player admin) {
        final UUID adminId = admin.getUniqueId();

        if (this.adminStates.get(adminId) != InitGameManager.InitState.AWAITING_MODE_SELECTION) {
            admin.sendMessage(Component.text("Error: Invalid initialization state.", NamedTextColor.RED));

        } else {
            InitData data = this.adminData.get(adminId);
            data.mode = InitGameManager.InitData.VampireMode.RANDOM;
            this.adminStates.put(adminId, InitGameManager.InitState.AWAITING_MIN_VAMPIRES);

            admin.sendMessage(Component.text("========================================", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD).append(Component.newline())
                    .append(Component.text("What should the ", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.BOLD, false)
                            .append(Component.text("minimum", NamedTextColor.YELLOW)
                                    .decorate(TextDecoration.BOLD))
                            .append(Component.text(" number of starting vampires be?", NamedTextColor.YELLOW)
                                    .decoration(TextDecoration.BOLD, false))
                    )
                    .append(Component.newline())
                    .append(Component.text("Please type a number in chat (must be 0 or more).", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)).append(Component.newline())
                    .append(Component.text("Type ", NamedTextColor.GRAY)
                            .decoration(TextDecoration.BOLD, false)
                            .append(Component.text("/pow admin init cancel", NamedTextColor.YELLOW))
                            .append(Component.text(" to cancel.", NamedTextColor.GRAY))
                    )
                    .append(Component.newline())
                    .append(Component.text("========================================", NamedTextColor.YELLOW)
                            .decorate(TextDecoration.BOLD))
            );

            admin.sendMessage("");
        }
    }

    /**
     * Set the game initialization to allow the admin to choose the starting vampire.
     *
     * @param admin the player running the initialization command.
     */
    public void handleSelectedMode(Player admin) {
        final UUID adminId = admin.getUniqueId();

        if (this.adminStates.get(adminId) != InitGameManager.InitState.AWAITING_MODE_SELECTION) {
            admin.sendMessage(Component.text("Error: Invalid initialization state.", NamedTextColor.RED));

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
        final int playerCount = onlinePlayers.size();

        if (playerCount == 0) {
            admin.sendMessage(Component.text("No players are online to select.", NamedTextColor.RED));
            this.cancelInitialization(admin);

        } else {
            InitData data = this.adminData.get(admin.getUniqueId());
            final int totalPages = (int)Math.ceil((double) playerCount / PLAYERS_PER_PAGE), currentPage = Math.min(data.currentPage, totalPages - 1);
            data.currentPage = currentPage;
            int slot = 0, startIndex = currentPage * PLAYERS_PER_PAGE, endIndex = Math.min(startIndex + PLAYERS_PER_PAGE, playerCount);
            Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, SELECT_VAMPIRES_GUI_TITLE);

            for (int i = startIndex; i < endIndex; ++i) {
                Player player = onlinePlayers.get(i);
                final boolean isVampire = data.selectedVampires.contains(player.getUniqueId());
                ItemStack item = new ItemStack(isVampire ? ItemTypeChecking.getBloodBottleType() : Material.GLASS_BOTTLE);
                ItemMeta meta = item.getItemMeta();

                if (isVampire) {
                    meta.customName(Component.text(player.getName() + " - Vampire", NamedTextColor.DARK_RED)
                            .decoration(TextDecoration.ITALIC, false));
                } else {
                    meta.customName(Component.text(player.getName() + " - Human", NamedTextColor.GREEN)
                            .decoration(TextDecoration.ITALIC, false));
                }

                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Click to toggle", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                item.setItemMeta(meta);
                inventory.setItem(slot, item);
                ++slot;
            }

            if (currentPage > 0) {
                // Create the button to return to the previous page
                ItemStack prevButton = new ItemStack(Material.ARROW);
                ItemMeta prevMeta = prevButton.getItemMeta();
                prevMeta.customName(Component.text("« Previous Page", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));

                // Inform the reader of what page number the previous button will take them to
                List<Component> prevLore = new ArrayList<>();
                prevLore.add(Component.text("Go to page " + currentPage, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                prevMeta.lore(prevLore);

                prevButton.setItemMeta(prevMeta);
                inventory.setItem(45, prevButton);
            }

            // Create a current page number item
            ItemStack pageIndicator = new ItemStack(Material.PAPER);
            ItemMeta pageMeta = pageIndicator.getItemMeta();
            pageMeta.customName(Component.text("Page " + (currentPage + 1) + " of " + totalPages, NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));

            // Note how many players have been chosen as vampires currently
            List<Component> pageLore = new ArrayList<>();
            pageLore.add(Component.text(playerCount, NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(" players total", NamedTextColor.GRAY))
            );
            pageLore.add(Component.text(data.selectedVampires.size(), NamedTextColor.DARK_RED)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(" selected as vampires", NamedTextColor.GRAY))
            );
            pageMeta.lore(pageLore);

            pageIndicator.setItemMeta(pageMeta);
            inventory.setItem(49, pageIndicator);

            if (currentPage < totalPages - 1) {
                // Create the button to progress to the next page
                ItemStack nextButton = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = nextButton.getItemMeta();
                nextMeta.customName(Component.text("Next Page »", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));

                // Inform the reader of what page number the next button will take them to
                List<Component> nextLore = new ArrayList<>();
                nextLore.add(Component.text("Go to page " + (currentPage + 2), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                nextMeta.lore(nextLore);

                nextButton.setItemMeta(nextMeta);
                inventory.setItem(50, nextButton);
            }

            // Create a confirmation button to move forward
            ItemStack confirmButton = new ItemStack(Material.LIME_CONCRETE);
            ItemMeta confirmMeta = confirmButton.getItemMeta();
            confirmMeta.customName(Component.text("CONFIRM SELECTION", NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false)
            );

            // Let the admin know how many vampires they will proceed with
            List<Component> confirmLore = new ArrayList<>();
            confirmLore.add(Component.text("Click to proceed with these selections", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            confirmLore.add(Component.text("Selected: ", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(data.selectedVampires.size() + " vampires", NamedTextColor.YELLOW)));
            confirmMeta.lore(confirmLore);

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
        final UUID adminId = admin.getUniqueId();
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
        final UUID adminId = admin.getUniqueId();
        InitData data = this.adminData.get(adminId);

        if (data != null && data.mode == InitGameManager.InitData.VampireMode.SELECTED) {
            Player targetPlayer = Bukkit.getPlayerExact(playerName);

            if (targetPlayer != null) {
                final UUID targetId = targetPlayer.getUniqueId();

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
        final UUID adminId = admin.getUniqueId();
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
        final UUID adminId = admin.getUniqueId();

        if (this.adminStates.get(adminId) != InitGameManager.InitState.AWAITING_MIN_VAMPIRES) {
            return false;
        } else {
            try {
                int min = Integer.parseInt(input.trim());

                if (min < 0) {
                    admin.sendMessage(Component.text("The minimum must be 0 or more. Please try again:", NamedTextColor.RED));

                } else {
                    InitData data = this.adminData.get(adminId);
                    data.minVampires = min;
                    this.adminStates.put(adminId, InitGameManager.InitState.AWAITING_MAX_VAMPIRES);

                    admin.sendMessage(Component.text("✓ Minimum vampires set to: ", NamedTextColor.GREEN)
                            .append(Component.text(min, NamedTextColor.YELLOW)));
                    admin.sendMessage("");
                    admin.sendMessage(Component.text("========================================", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD).append(Component.newline())
                            .append(Component.text("What should the ", NamedTextColor.YELLOW)
                                    .decoration(TextDecoration.BOLD, false)
                                    .append(Component.text("maximum", NamedTextColor.YELLOW)
                                                    .decorate(TextDecoration.BOLD))
                                    .append(Component.text(" number of vampires be?", NamedTextColor.YELLOW)
                                                    .decoration(TextDecoration.BOLD, false))
                            )
                            .append(Component.newline())
                            .append(Component.text("Please type a number in chat (must be " + min + " or more).", NamedTextColor.GRAY)
                                    .decoration(TextDecoration.BOLD, false))
                            .append(Component.newline())
                            .append(Component.text("Type ", NamedTextColor.GRAY)
                                    .decoration(TextDecoration.BOLD, false)
                                    .append(Component.text("/pow admin init cancel", NamedTextColor.YELLOW))
                                    .append(Component.text(" to cancel.", NamedTextColor.GRAY))
                            )
                            .append(Component.newline())
                            .append(Component.text("========================================", NamedTextColor.YELLOW)
                                    .decorate(TextDecoration.BOLD))
                    );
                    admin.sendMessage("");
                }

                return true;

            } catch (NumberFormatException e) {
                admin.sendMessage(Component.text("'" + input + "' is not a valid number. Please try again:", NamedTextColor.RED));
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
        final UUID adminId = admin.getUniqueId();

        if (this.adminStates.get(adminId) != InitGameManager.InitState.AWAITING_MAX_VAMPIRES) {
            return false;

        } else {
            InitData data = this.adminData.get(adminId);

            try {
                final int max = Integer.parseInt(input.trim());

                if (max < data.minVampires) {
                    admin.sendMessage(Component.text("The maximum must be " + data.minVampires + " or more. Please try again:", NamedTextColor.RED));

                } else {
                    data.maxVampires = max;
                    this.adminStates.put(adminId, InitGameManager.InitState.AWAITING_FINAL_CONFIRM);
                    admin.sendMessage(Component.text("✓ Maximum vampires set to: ", NamedTextColor.GREEN)
                            .append(Component.text(max, NamedTextColor.YELLOW)));
                    admin.sendMessage("");
                    this.showFinalConfirmation(admin);
                }

                return true;

            } catch (NumberFormatException e) {
                admin.sendMessage(Component.text("'" + input + "' is not a valid number. Please try again:", NamedTextColor.RED));
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
        final UUID adminId = admin.getUniqueId();
        InitData data = this.adminData.get(adminId);

        admin.sendMessage("");
        admin.sendMessage(Component.text("========================================", NamedTextColor.GREEN).decorate(TextDecoration.BOLD).append(Component.newline())
                .append(Component.text("FINAL CONFIRMATION")).append(Component.newline())
                .append(Component.text("========================================"))
        );

        admin.sendMessage("");
        admin.sendMessage(Component.text("Configuration:", NamedTextColor.GRAY));

        if (data.mode == InitGameManager.InitData.VampireMode.RANDOM) {
            admin.sendMessage(Component.text("  • Mode: ", NamedTextColor.GRAY)
                    .append(Component.text("Random", NamedTextColor.YELLOW))
                    .append(Component.newline())
                    .append(Component.text("  • Vampires: ", NamedTextColor.GRAY))
                    .append(Component.text(data.minVampires + "-" + data.maxVampires, NamedTextColor.YELLOW))
            );

        } else {
            admin.sendMessage(Component.text("  • Mode: ", NamedTextColor.GRAY)
                    .append(Component.text("Manually Selected", NamedTextColor.YELLOW))
                    .append(Component.newline())
                    .append(Component.text("  • Vampires: ", NamedTextColor.GRAY))
                    .append(Component.text(data.selectedVampires.size() + " players", NamedTextColor.YELLOW))
            );
        }

        admin.sendMessage("");
        admin.sendMessage(Component.text("This will reset the entire game state.", NamedTextColor.GRAY));
        admin.sendMessage("");

        admin.sendMessage(Component.text("Ready to begin? ", NamedTextColor.GRAY)
                .append(Component.text("[START GAME]", NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD)
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.runCommand(COMMAND_PREFIX + "execute"))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to initialize the game", NamedTextColor.GRAY)))
                )
        );
        admin.sendMessage(Component.text("========================================", NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD));
    }

    /**
     * Activate the new Vampires game and reset the game state.
     *
     * @param admin the player running the initialization command.
     */
    public void executeInitialization(Player admin) {
        final UUID adminId = admin.getUniqueId();

        if (this.adminStates.get(adminId) != InitGameManager.InitState.AWAITING_FINAL_CONFIRM) {
            admin.sendMessage(Component.text("Error: Invalid initialization state.", NamedTextColor.RED));

        } else {
            InitData data = this.adminData.get(adminId);
            admin.sendMessage("");
            admin.sendMessage(Component.text("========================================", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).append(Component.newline())
                    .append(Component.text("INITIALIZING GAME...")).append(Component.newline())
                    .append(Component.text("========================================"))
            );
            World world = this.plugin.getServer().getWorld(RemakepirePlugin.WORLD_NAME);

            if (world == null) {
                admin.sendMessage(Component.text("Error: World '" + RemakepirePlugin.WORLD_NAME + "' not found.", NamedTextColor.RED));
                this.cancelInitialization(admin);

            } else {
                admin.sendMessage(Component.text("[1/9] Neutralizing beacons...", NamedTextColor.GRAY));

                for (BeaconSite beacon : this.plugin.getBeaconManager().getAllBeacons()) {
                    this.plugin.getBeaconManager().setBeaconNeutral(beacon.getName(), true);
                    Location beaconLoc = beacon.getLocation();

                    if (beaconLoc != null && beaconLoc.getWorld() != null) {
                        beaconLoc.getBlock().setType(Material.BARRIER);
                    }
                }

                if (this.plugin.getBeaconManager().getBeacon("castle") != null) {
                    this.plugin.getBeaconManager().setBeaconDesecrated("castle");
                    admin.sendMessage(Component.text("  → Castle beacon set to desecrated", NamedTextColor.GRAY));
                }

                admin.sendMessage(Component.text("[2/9] Clearing beacon cooldowns...", NamedTextColor.GRAY));
                this.plugin.getBeaconManager().clearAllBeaconCooldownsForNewSession();
                admin.sendMessage(Component.text("[3/9] Resetting player data...", NamedTextColor.GRAY));

                Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();

                // Clear all tags from all online players
                for (Player player : onlinePlayers) {
                    for (String tag : new HashSet<>(player.getScoreboardTags())) {
                        player.removeScoreboardTag(tag);
                    }

                    player.getInventory().clear();
                }

                admin.sendMessage(Component.text("[3.5/9] Resetting scoreboard objectives...", NamedTextColor.GRAY));
                Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

                for (Objective obj : new HashSet<>(mainScoreboard.getObjectives())) {
                    if (obj.getName().startsWith("vsmp_")) {
                        final String name = obj.getName();
                        final Component displayName = obj.displayName();

                        Criteria criteria = obj.getTrackedCriteria();
                        obj.unregister();
                        mainScoreboard.registerNewObjective(name,criteria, displayName);
                    }
                }

                for (Player player : onlinePlayers) {
                    AttributeInstance healthAttr = player.getAttribute(Attribute.MAX_HEALTH);

                    for (AttributeModifier modifier : healthAttr.getModifiers()) {
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
                        for (Player player : onlinePlayers) {
                            deathObjective.getScore(player.getName()).setScore(0);
                        }

                        admin.sendMessage(Component.text("  → Reset death counts for all players", NamedTextColor.GRAY));
                    }
                } catch (Exception e) {
                    this.plugin.getLogger().warning("Failed to reset death scoreboard: " + e.getMessage());
                }

                admin.sendMessage(Component.text("[4/9] Priming new session and incrementing game ID...", NamedTextColor.GRAY));
                this.plugin.getSessionManager().primeNewSession();
                this.plugin.getSessionManager().incrementGameID();

                admin.sendMessage(Component.text("[4.5/9] Resetting game state flags in config.yml...", NamedTextColor.GRAY));
                this.plugin.getConfig().set("first_beacon_converted", false);
                this.plugin.getConfig().set("humans_own_all_beacons", false);
                this.plugin.getConfig().set("vampires_own_all_beacons", false);
                this.plugin.getConfig().set("one_human_left", false);
                this.plugin.getConfig().set("fourth_book_has_spawned", false);
                this.plugin.getConfig().set("fourth_book_spawn_enabled", false);
                this.plugin.saveConfig();

                admin.sendMessage(Component.text("[4.6/9] Clearing sire mappings...", NamedTextColor.GRAY));
                this.plugin.getSireManager().clearAllSireMappings();
                admin.sendMessage(Component.text("[4.7/9] Stopping vampire tracking...", NamedTextColor.GRAY));

                if (this.plugin.getVampireTrackingManager() != null) {
                    this.plugin.getVampireTrackingManager().stopAllTracking();
                }

                admin.sendMessage(Component.text("[4.8/9] Clearing permadeath preferences...", NamedTextColor.GRAY));
                this.plugin.getPermadeathManager().clearAllPermadeathModes();
                admin.sendMessage(Component.text("[5/9] Setting world time and border...", NamedTextColor.GRAY));

                world.setFullTime(1L);
                world.getWorldBorder().setSize(900000.0);
                admin.sendMessage(Component.text("[6/9] Applying saturation effect...", NamedTextColor.GRAY));

                for (Player player : onlinePlayers) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 200, 9));
                }

                admin.sendMessage(Component.text("[7/9] Teleporting players...", NamedTextColor.GRAY));

                for (Player player : onlinePlayers) {
                    if (player.getGameMode() != GameMode.SURVIVAL) {
                        GameMode oldMode = player.getGameMode();
                        player.setGameMode(GameMode.SURVIVAL);
                        admin.sendMessage(Component.text("  → Reset " + player.getName() + " from " + oldMode.name().toLowerCase() + " to survival", NamedTextColor.GRAY));
                    }

                    Location teleportLoc = this.getRandomTeleportLocation(world);

                    if (teleportLoc != null) {
                        player.teleport(teleportLoc);
                    } else {
                        admin.sendMessage(Component.text("Warning: Could not find valid teleport location for " + player.getName(), NamedTextColor.RED));
                    }
                }

                admin.sendMessage(Component.text("[8/9] Assigning vampires...", NamedTextColor.GRAY));

                List<Player> playersToConvert = new ArrayList<>();

                if (data.mode == InitGameManager.InitData.VampireMode.RANDOM) {
                    int vampireCount = ThreadLocalRandom.current().nextInt(data.minVampires, data.maxVampires + 1);
                    List<Player> availablePlayers = new ArrayList<>(onlinePlayers);
                    Collections.shuffle(availablePlayers);

                    vampireCount = Math.min(vampireCount, availablePlayers.size());
                    playersToConvert = availablePlayers.subList(0, vampireCount);

                } else {
                    for (Player player : onlinePlayers) {
                        if (data.selectedVampires.contains(player.getUniqueId())) {
                            playersToConvert.add(player);
                        }
                    }
                }

                Set<UUID> vampireIds = new HashSet<>();

                for (Player player : playersToConvert) {
                    this.plugin.getVampireManager().setPlayerAsVampire(player, 1);
                    vampireIds.add(player.getUniqueId());

                    player.setExp(0.5F);
                    player.showTitle(Title.title(
                            Component.text("Vampire", NamedTextColor.DARK_RED)
                                    .decorate(TextDecoration.BOLD),
                            Component.empty(),
                            Title.Times.times(
                                    Duration.ofMillis(500),     // 1/2 second
                                    Duration.ofSeconds(5),
                                    Duration.ofSeconds(1)
                            )));
                    player.sendMessage("");

                    player.sendMessage(Component.text("========================================", NamedTextColor.DARK_RED)
                            .decorate(TextDecoration.BOLD)
                            .append(Component.newline())
                            .append(Component.text("You are a creature of the night, and it is time to feed.", NamedTextColor.RED)
                                    .decoration(TextDecoration.BOLD, false))
                            .append(Component.newline()).append(Component.newline())
                            .append(Component.text("What to do: Turn other humans by 'killing' them when no one is looking. As a level 1 vampire, there are very few ways you can be found out, but still be cautious. You cannot help turn beacons, eating food is bad but stomachable for now, only attack during the night. Press \"k\" to customize your vampire ability keybinds.", NamedTextColor.GRAY))
                            .append(Component.newline())
                            .append(Component.text("========================================", NamedTextColor.DARK_RED)
                                    .decorate(TextDecoration.BOLD))
                    );

                    player.sendMessage("");

                    player.sendMessage(Component.text("Apply the vampire texture pack: ", NamedTextColor.GRAY)
                            .append(Component.text("[CLICK HERE]", NamedTextColor.RED)
                                    .decorate(TextDecoration.UNDERLINED)
                                    .clickEvent(ClickEvent.runCommand("/pow texture vampire"))
                                    .hoverEvent(HoverEvent.showText(Component.text("Click to apply the vampire texture pack", NamedTextColor.GRAY)))
                            ));
                }

                admin.sendMessage(Component.text("  → Converted " + playersToConvert.size() + " players to vampires", NamedTextColor.GRAY));

                for (Player player : onlinePlayers) {
                    if (!vampireIds.contains(player.getUniqueId())) {
                        player.addScoreboardTag(VampireManager.HUMAN_TAG);
                        player.showTitle(Title.title(
                                Component.text("Human", NamedTextColor.YELLOW)
                                        .decorate(TextDecoration.BOLD),
                                Component.empty(),
                                Title.Times.times(
                                        Duration.ofMillis(500),
                                        Duration.ofSeconds(5),
                                        Duration.ofSeconds(1)
                                )));
                        player.sendMessage("");
                        player.sendMessage(Component.text("========================================", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD).append(Component.newline())
                                .append(Component.text("Welcome to " + plugin.getConfigManager().getTownName() + ". Survive, consecrate beacons, find tomes, and above all: Fear the night.", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false)).append(Component.newline())
                                .append(Component.text("========================================", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                        );

                        player.sendMessage("");
                    }
                }

                admin.sendMessage(Component.text("[9/11] Starting session...", NamedTextColor.GRAY));
                this.plugin.getSessionManager().startSession();

                admin.sendMessage(Component.text("[10/11] Distributing tomes to chests...", NamedTextColor.GRAY));
                if (this.plugin.getTomeDistributionManager().getTomeLocations().isEmpty()) {
                    admin.sendMessage(Component.text("  → No tome chest locations configured, skipping tome distribution", NamedTextColor.YELLOW));
                } else {
                    this.plugin.getTomeDistributionManager().triggerDistribution();
                    admin.sendMessage(Component.text("  → Tomes distributed to " + this.plugin.getTomeDistributionManager().getTomeLocations().size() + " chest locations", NamedTextColor.GRAY));
                }

                admin.sendMessage(Component.text("[11/11] Clearing potion effects...", NamedTextColor.GRAY));

                for (Player player : onlinePlayers) {
                    for (PotionEffect effect : player.getActivePotionEffects()) {
                        player.removePotionEffect(effect.getType());
                    }
                }

                this.plugin.getVampireTurningManager().enableAllVampireTurning();

                admin.sendMessage("");
                admin.sendMessage(Component.text("========================================", NamedTextColor.GREEN).decorate(TextDecoration.BOLD).append(Component.newline())
                        .append(Component.text("GAME INITIALIZED SUCCESSFULLY.")).append(Component.newline())
                        .append(Component.text("========================================")).append(Component.newline())
                        .append(Component.text("Players: ", NamedTextColor.GRAY)
                                .decoration(TextDecoration.BOLD, false)
                                .append(Component.text(onlinePlayers.size(), NamedTextColor.YELLOW))
                        )
                        .append(Component.newline())
                        .append(Component.text("Vampires: ", NamedTextColor.GRAY)
                                .decoration(TextDecoration.BOLD, false)
                                .append(Component.text(playersToConvert.size(), NamedTextColor.RED))
                        )
                        .append(Component.newline())
                        .append(Component.text("Humans: ", NamedTextColor.GRAY)
                                .decoration(TextDecoration.BOLD, false)
                                .append(Component.text(onlinePlayers.size() - playersToConvert.size(), NamedTextColor.GREEN))
                        )
                        .append(Component.newline())
                        .append(Component.text("========================================", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                );

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
        ConfigManager configManager = this.plugin.getConfigManager();
        final int maxAttempts = 50;

        final double townCenterX = configManager.getTownCenterX(), townCenterZ = configManager.getTownCenterZ();
        final double teleportRadius = configManager.getTeleportRadius();

        final double minX = configManager.getBorderMinX(), maxX = configManager.getBorderMaxX();
        final double minZ = configManager.getBorderMinZ(), maxZ = configManager.getBorderMaxZ();

        double angle, distance;
        int x, z;

        for (int attempt = 0; attempt < maxAttempts; ++attempt) {
            angle = ThreadLocalRandom.current().nextDouble() * 2.0 * Math.PI;
            distance = Math.sqrt(ThreadLocalRandom.current().nextDouble()) * teleportRadius;
            x = (int)Math.floor(townCenterX + distance * Math.cos(angle));
            z = (int)Math.floor(townCenterZ + distance * Math.sin(angle));

            if (!(x < minX + BORDER_BUFFER) && !(x > maxX - BORDER_BUFFER) && !(z < minZ + BORDER_BUFFER) && !(z > maxZ - BORDER_BUFFER)) {
                Location loc = new Location(world, x + 0.5, world.getHighestBlockYAt(x, z) + 1, z + 0.5);

                if (loc.getY() > 0 && loc.getY() < world.getMaxHeight()) {
                    return loc;
                }
            }
        }

        return new Location(world, townCenterX + 0.5, world.getHighestBlockYAt((int)townCenterX, (int)townCenterZ) + 1, townCenterZ + 0.5);
    }

    /**
     * Cancel the new game initialization process.
     *
     * @param admin the player running the initialization command.
     */
    public void cancelInitialization(Player admin) {
        final UUID adminId = admin.getUniqueId();
        this.adminStates.remove(adminId);
        this.adminData.remove(adminId);

        admin.sendMessage(Component.text("Game initialization cancelled.", NamedTextColor.RED));
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
            return switch (command.substring(COMMAND_PREFIX.length())) {
                case "confirm1" -> {
                    this.handleFirstConfirmation(admin);
                    yield true;
                }
                case "mode_random" -> {
                    this.handleRandomMode(admin);
                    yield true;
                }
                case "mode_selected" -> {
                    this.handleSelectedMode(admin);
                    yield true;
                }
                case "execute" -> {
                    this.executeInitialization(admin);
                    yield true;
                }
                default -> false;
            };
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
     * @param title the name of an inventory menu.
     * @return {@code true} if the current event has the admin manually selecting vampires.
     */
    public boolean isPlayerSelectionGUI(Component title) {
        return title.equals(SELECT_VAMPIRES_GUI_TITLE);
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

    public enum InitState {
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

        public enum VampireMode {
            RANDOM,
            SELECTED
        }
    }
}
