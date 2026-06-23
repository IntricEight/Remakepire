package frostvein.sampires.remakepire.managers;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class ConfigGuiManager {
    private final RemakepirePlugin plugin;
    private final ConfigManager configManager;
    private final SessionManager sessionManager;
    public static final String CONFIG_GUI_TITLE = "§8§lConfiguration Options";
    private Inventory configGui;

    public ConfigGuiManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.configManager = this.plugin.getConfigManager();
        this.sessionManager = this.plugin.getSessionManager();

        this.createConfigGui();
    }

    /**
     * Create an inventory screen for modifying values within the config file.
     */
    public void createConfigGui() {
        /* Inventory Layout
         * Leave a row and column of empty space around the items to format things.
         * This means that for every row of 9 items, only the middle 7 should be used.
         * Additionally, the first and last 9 items should be unused to create the top and bottom empty rows.
         */

        String[] commandsInGui = { "alert_on_quit", "holy_water_cap", "tome_cap", "vampire_level_cap", "new_vampire_tracking", "allow_vampire_mounts", "vampire_health_check",
                "damage_suppression", "cure_requires_dead_sire", "cure_requires_daylight", "cure_book_spawning", "enable_npc_mobs", "breeding_out_of_session", "stake_permadeath_stage", "human_life_limit",
                "one_human_left", "border_active" };
        final int ROWS = 5;

        this.configGui = Bukkit.createInventory(null, 9 * ROWS, CONFIG_GUI_TITLE);

        for (String commandName : commandsInGui) {
            this.configGui.setItem(getCommandPositionInGui(commandName), this.getGuiItem(commandName));
        }
    }

    /**
     * Open an inventory screen for modifying values within the config.
     *
     * @param admin the admin selecting tomes.
     */
    public void openConfigGUI(Player admin) {
        admin.openInventory(configGui);
        admin.sendMessage("§6Select configuration value to change");
    }

    /**
     * Create the inventory item for the Config GUI interface.
     *
     * @param commandName the name of the command which modifies the config value.
     * @return The item that will represent the config setting in the GUI interface.
     */
    private ItemStack getGuiItem(String commandName) {
        ItemStack item = getItemVisual(commandName);
        item.setItemMeta(this.getItemText(commandName, item));

        return item;
    }

    /**
     * Determine the command's position within the GUI inventory.
     *
     * @param commandName the name of the command which modifies the config value.
     * @return The location of the command's item inside the config GUI.
     */
    public static int getCommandPositionInGui(String commandName) {
        // Retrieve the placement of each command within the GUI design (0-indexed).
        return switch (commandName) {
            // Row 1
            case "alert_on_quit" ->             10;
            case "holy_water_cap" ->            11;
            case "tome_cap" ->                  12;
            case "vampire_level_cap" ->         13;
            case "new_vampire_tracking" ->      14;
            case "allow_vampire_mounts" ->      15;
            case "vampire_health_check" ->      16;
            // Row 2
            case "damage_suppression" ->        19;
            case "cure_requires_dead_sire" ->   20;
            case "cure_requires_daylight" ->    21;
            case "cure_book_spawning" ->        22;
            case "enable_npc_mobs" ->           23;
            case "breeding_out_of_session" ->   24;
            case "stake_permadeath_stage" ->    25;
            // Row 3
            case "human_life_limit" ->          28;
            case "one_human_left" ->            29;
            case "border_active" ->             30;

            default -> throw new IllegalStateException("Unexpected command name: " + commandName);
        };
    }

    /**
     * Create the literal item to fill the inventory slot.
     *
     * @param commandName the name of the command that is being represented.
     * @return The item that visually ties to the config setting in the GUI interface.
     */
    private static ItemStack getItemVisual(String commandName) {
        /* Configuration Item Visuals
         * alert_on_quit            "noteblock"
         * holy_water_cap           "splash bottle (holy water)"
         * tome_cap                 "chest"
         * vampire_level_cap        "bat egg"
         * new_vampire_tracking     "recovery compass"
         * allow_vampire_mounts     "saddle"
         * vampire_health_check     "bottle of enchanting (blood bottle)"
         * damage_suppression       "stone sword"
         * cure_requires_dead_sire  "wither skeleton skull"
         * cure_requires_daylight   "shroomlight"
         * cure_book_spawning       "written book"
         * enable_npc_mobs          "wandering trader spawn egg"
         * breeding_out_of_session  "wheat"
         * stake_permadeath_stage   "wooden sword (stake)"
         * human_life_limit         "skull"
         * one_human_left           "Ochre froglight"
         * border_active            "barrier"
         *
         * If changing any values from this, don't forget to change the associated values inside both getItemVisual() and getCommandNameFromItem()
         */

        return ItemStack.of( switch (commandName) {
            case "alert_on_quit" ->             Material.NOTE_BLOCK;
            case "holy_water_cap" ->            Material.POTION;
            case "tome_cap" ->                  Material.CHEST;
            case "vampire_level_cap" ->         Material.BAT_SPAWN_EGG;
            case "new_vampire_tracking" ->      Material.RECOVERY_COMPASS;
            case "allow_vampire_mounts" ->      Material.SADDLE;
            case "vampire_health_check" ->      Material.EXPERIENCE_BOTTLE;
            case "damage_suppression" ->        Material.STONE_SWORD;
            case "cure_requires_dead_sire" ->   Material.WITHER_SKELETON_SKULL;
            case "cure_requires_daylight" ->    Material.SHROOMLIGHT;
            case "cure_book_spawning" ->        Material.WRITTEN_BOOK;
            case "enable_npc_mobs" ->           Material.WANDERING_TRADER_SPAWN_EGG;
            case "breeding_out_of_session" ->   Material.WHEAT;
            case "stake_permadeath_stage" ->    Material.WOODEN_SWORD;
            case "human_life_limit" ->          Material.SKELETON_SKULL;
            case "one_human_left" ->            Material.OCHRE_FROGLIGHT;
            case "border_active" ->             Material.BARRIER;

            default -> throw new IllegalStateException("Unexpected command: " + commandName);
        });
    }

    /**
     * Determine which command is being interacted with using its visual item representation.
     *
     * @param material the item being used for the command's slot.
     * @return The command's name.
     */
    public static String getCommandNameFromItem(Material material) {
        return switch (material) {
            case Material.NOTE_BLOCK ->                     "alert_on_quit";
            case Material.POTION ->                         "holy_water_cap";
            case Material.CHEST ->                          "tome_cap";
            case Material.BAT_SPAWN_EGG ->                  "vampire_level_cap";
            case Material.RECOVERY_COMPASS ->               "new_vampire_tracking";
            case Material.SADDLE ->                         "allow_vampire_mounts";
            case Material.EXPERIENCE_BOTTLE ->              "vampire_health_check";
            case Material.STONE_SWORD ->                    "damage_suppression";
            case Material.WITHER_SKELETON_SKULL ->          "cure_requires_dead_sire";
            case Material.SHROOMLIGHT ->                    "cure_requires_daylight";
            case Material.WRITTEN_BOOK ->                   "cure_book_spawning";
            case Material.WANDERING_TRADER_SPAWN_EGG ->     "enable_npc_mobs";
            case Material.WHEAT ->                          "breeding_out_of_session";
            case Material.WOODEN_SWORD ->                   "stake_permadeath_stage";
            case Material.SKELETON_SKULL ->                 "human_life_limit";
            case Material.OCHRE_FROGLIGHT ->                "one_human_left";
            case Material.BARRIER ->                        "border_active";

            default -> throw new IllegalStateException("Unexpected material: " + material);
        };
    }

    /**
     * Create the item's description that will inform the user about the associated config option.
     *
     * @param commandName the name of the command we are informing the reader about.
     * @return Item information that relays information about the config setting in the GUI interface.
     */
    private ItemMeta getItemText(String commandName, ItemStack visual) {
        /* Configuration Item Descriptions
         * Majority of text is in standard white/light gray text
         * Boolean values related to true and false are green and red, respectively
         * Other values are in yellow
         *
         * Title
         * Status
         * Description
         */

        ItemMeta meta = visual.getItemMeta();
        List<String> instructions = new ArrayList<>();

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        // Create the title of the config control
        String itemTitle = getConfigGuiTitle(commandName);

        // Create the item description of the config control
        instructions.add(this.getConfigStatus(commandName));
        instructions.add("");
        instructions.addAll(getConfigCommandDescription(commandName));
        instructions.add("");
        instructions.add("§7§oClick to modify this value.");

        // Assign the created values to the config control item
        meta.setDisplayName(itemTitle);
        meta.setLore(instructions);

        return meta;
    }

    /**
     * Determine the title of the GUI item's current config status line.
     *
     * @param commandName the name of the command whose config title we are formatting.
     * @return A string to be used as a config control item's title.
     */
    private static String getConfigGuiTitle(String commandName) {
        return "§r§f" + switch (commandName) {
            case "alert_on_quit" ->             "Alert Admin on Player Quit";
            case "holy_water_cap" ->            "Limit Holy Water Creation";
            case "tome_cap" ->                  "Limit Tome Ability Acquisition";
            case "vampire_level_cap" ->         "Prevent Returning to Stages";
            case "new_vampire_tracking" ->      "Track New Vampires";
            case "allow_vampire_mounts" ->      "Allow Vampires Living Mounts";
            case "vampire_health_check" ->      "Vampire Regeneration Rate";
            case "damage_suppression" ->        "Player Damage Resistance";
            case "cure_requires_dead_sire" ->   "Sire Death for Curing";
            case "cure_requires_daylight" ->    "Daylight for Curing";
            case "cure_book_spawning" ->        "Allow Cure Book Spawning";
            case "enable_npc_mobs" ->           "Allow NPC Mob Spawns";
            case "breeding_out_of_session" ->   "Allow Animal Breeding Anytime";
            case "stake_permadeath_stage" ->    "Stage where Staking Permakills";
            case "human_life_limit" ->          "Restrict Causes of Final Deaths";
            case "one_human_left" ->            "Significantly Accelerate Human Beacon Conversions";
            case "border_active" ->             "Prevent Players from Leaving Boundaries";

            default -> "";
        };
    }

    /**
     * Determine the content and style of the GUI item's current config status line.
     *
     * @param commandName the name of the command whose config we are formatting the value of.
     * @return A string to add to an item's lore {@code ItemMeta}.
     */
    private String getConfigStatus(String commandName) {
        // Some handy control settings to make changing the colors later easier
        final String TRUE = "§2", FALSE = "§4", OTHER = "§6";

        // Display the current setting of the config
        return switch (commandName) {
            case "alert_on_quit" ->
                    this.configManager.shouldAlertOnPlayerQuit() ? TRUE + "ALERT" : FALSE + "SILENCED";
            case "holy_water_cap" ->
                    this.configManager.isHolyWaterSessionCapped() ? TRUE + "SINGLE USE" : FALSE + "COOLDOWN";
            case "tome_cap" ->
                    this.configManager.isTomeAbsorptionCapped() ? TRUE + "SINGLE USE" : FALSE + "COOLDOWN";
            case "vampire_level_cap" ->
                    this.configManager.isVampireLevelingCapped() ? TRUE + "LOCKED OUT" : FALSE + "REGAIN STAGES";
            case "new_vampire_tracking" ->
                    this.configManager.canTrackNewVampires() ? TRUE + "TRACKING" : FALSE + "HIDDEN";
            case "allow_vampire_mounts" ->
                    this.configManager.canVampiresRideLivingMounts() ? TRUE + "ALLOWED" : FALSE + "FORBIDDEN";
            case "vampire_health_check" ->
                    OTHER + this.configManager.getVampireHealthCheckTicks();
            case "damage_suppression" ->
                    OTHER + this.configManager.getDamageSuppression() + "%";
            case "cure_requires_dead_sire" ->
                    this.configManager.doCuresRequireSireDeath() ? TRUE + "REQUIRED" : FALSE + "NOT REQUIRED";
            case "cure_requires_daylight" ->
                    this.configManager.doCuresRequireDaytime() ? TRUE + "REQUIRED" : FALSE + "NOT REQUIRED";
            case "cure_book_spawning" ->
                    this.sessionManager.isCureBooksEnabled() ? TRUE + "SPAWNING" : FALSE + "DISABLED";
            case "enable_npc_mobs" ->
                    this.configManager.areNpcMobsEnabled() ? TRUE + "ALLOWED" : FALSE + "PREVENTED";
            case "breeding_out_of_session" ->
                    this.configManager.canBreedAnimalsOutOfSession() ? TRUE + "ALLOWED" : FALSE + "PREVENTED";
            case "stake_permadeath_stage" ->
                    OTHER + this.configManager.getPermadeathMinimumStage();
            case "human_life_limit" ->
                    this.configManager.isLifeLimitEnforced() ? TRUE + "ENFORCED" : FALSE + "VAMPIRE ONLY";
            case "one_human_left" ->
                    this.sessionManager.isOneHumanLeftActive() ? TRUE + "ACCELERATED" : FALSE + "NORMAL";
            case "border_active" ->
                    this.sessionManager.isBorderActive() ? TRUE + "TRAPPED" : FALSE + "DISABLED";
            default -> "§8No value found";
        };
    }

    /**
     * Determine the description and appearance of the configuration setting.
     *
     * @param commandName the name of the command that is being described.
     * @return A List of strings that describe the config option.
     */
    private static List<String> getConfigCommandDescription(String commandName) {
        List<String> description = new ArrayList<>();
        final String s = "§r§5";

        // Get the description of the config command
        switch (commandName) {
            case "alert_on_quit":
                description.add("Alert admins when a player leaves the game.");
                break;

            case "holy_water_cap":
                description.add("Limit players to creating only a single");
                description.add("holy water each session.");
                break;

            case "tome_cap":
                description.add("Limit players to absorbing only a single");
                description.add("tome book each session.");
                break;

            case "vampire_level_cap":
                description.add("Prevent vampires from returning to a vampire");
                description.add("stage lost during the active session.");
                break;

            case "new_vampire_tracking":
                description.add("Provide vampires with a tracking arrow");
                description.add("toward newly created vampires.");
                break;

            case "allow_vampire_mounts":
                description.add("Let higher vampires ride animals that would");
                description.add("ordinarily recoil from their true nature.");
                break;

            case "vampire_health_check":
                description.add("The tick intervals where vampires drain");
                description.add("their blood bar to regenerate health.");
                break;

            case "damage_suppression":
                description.add("The percentage of damage that players");
                description.add("ignore from all sources.");
                break;

            case "cure_requires_dead_sire":
                description.add("Require a vampire's sire to have been");
                description.add("permakilled before they can be cured of");
                description.add("vampirism.");
                break;

            case "cure_requires_daylight":
                description.add("Require it to be day time before a");
                description.add("a player can be cured of vampirism.");
                break;

            case "cure_book_spawning":
                description.add("Spawn cure books inside the tome chests");
                description.add("at random, replacing their previous contents.");
                break;

            case "enable_npc_mobs":
                description.add("Allow the spawning of humanoid entities");
                description.add("that players may find disruptive to");
                description.add("the classic Vampires experience.");
                description.add("");
                description.add("Check the setting \"enable-npc-mobs\" to");
                description.add("view the list of NPC mobs.");
                break;

            case "breeding_out_of_session":
                description.add("Let players breed and hatch passive");
                description.add("animals during Build Mode.");
                break;

            case "stake_permadeath_stage":
                description.add("Staked vampire will be permakilled");
                description.add("while at this stage or lower.");
                break;

            case "human_life_limit":
                description.add("Cause humans to be permakilled when");
                description.add("they die after running out of lives");
                description.add("regardless of their cause of death.");
                break;

            case "one_human_left":
                description.add("Massively boost the human conversion");
                description.add("rate on beacons.");
                break;

            case "border_active":
                description.add("Trap players within the boundaries of the");
                description.add("game while that player has not met one of");
                description.add("the leaving conditions.");
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + commandName);
        }

        // Add the formatting to the descriptions
        description.replaceAll(string -> s + string);

        return description;
    }

    /**
     * Refresh the item representing the configuration setting's display.
     *
     * @param commandName the name of the command that is being described.
     */
    public void refreshConfigGuiItem(String commandName) {
        this.configGui.setItem(getCommandPositionInGui(commandName), this.getGuiItem(commandName));
    }

    /**
     * Modify the value within the configuration file depending on the setting's current value.
     *
     * @param commandName the name of the command to be run.
     */
    public void runConfigCommand(String commandName) {
        /* AUTONOMOUS COMMAND PROCEDURE
         * Since the admin is not entering values to control the new config value, we are going to use existing value to determine what behavior we should see.
         * If the config option is a simple boolean, we should toggle between the two. True becomes false, and false becomes true.
         * If the config option is numeric, move from low to high numbers until the maximum is reached, before looping back to the lowest value.
         * If the config option is text-based, there is no standard, just try to have it make sense.
         *
         * If there are tab completion options created for the command line variant of these executions, use those values as the stepping stones for non-boolean config changes.
         * */

        switch (commandName) {
            case "alert_on_quit":
                this.configManager.setAlertOnPlayerQuit( !this.configManager.shouldAlertOnPlayerQuit() );
                break;

            case "holy_water_cap":
                this.configManager.setHolyWaterCapping( !this.configManager.isHolyWaterSessionCapped() );
                break;

            case "tome_cap":
                this.configManager.setTomeAbsorptionCapping( !this.configManager.isTomeAbsorptionCapped() );
                break;

            case "vampire_level_cap":
                this.configManager.setVampireLevelCapping( !this.configManager.isVampireLevelingCapped() );
                break;

            case "new_vampire_tracking":
                this.configManager.setTrackingNewVampires( !this.configManager.canTrackNewVampires() );
                break;

            case "allow_vampire_mounts":
                this.configManager.setVampiresRideLivingMounts( !this.configManager.canVampiresRideLivingMounts() );
                break;

            case "vampire_health_check":
                // Step through the values found in the command autofill section
                if (this.configManager.getVampireHealthCheckTicks() < 20) {
                    this.configManager.setVampireHealthCheckTicks( 20 );

                } else if (this.configManager.getVampireHealthCheckTicks() < 40) {
                    this.configManager.setVampireHealthCheckTicks( 40 );

                } else if (this.configManager.getVampireHealthCheckTicks() < 60) {
                    this.configManager.setVampireHealthCheckTicks( 60 );

                } else if (this.configManager.getVampireHealthCheckTicks() < 100) {
                    this.configManager.setVampireHealthCheckTicks( 100 );

                } else if (this.configManager.getVampireHealthCheckTicks() < 200) {
                    this.configManager.setVampireHealthCheckTicks( 200 );

                } else {
                    // To stop us from getting too high, we'll stop here and loop back to the plugin default
                    this.configManager.setVampireHealthCheckTicks(9);
                }

                break;

            case "damage_suppression":
                // Step through the values found in the command autofill section
                if (this.configManager.getDamageSuppression() < 10) {
                    this.configManager.setDamageSuppression( 10 );

                } else if (this.configManager.getDamageSuppression() < 25) {
                    this.configManager.setDamageSuppression( 25 );

                } else if (this.configManager.getDamageSuppression() < 50) {
                    this.configManager.setDamageSuppression( 50 );

                } else if (this.configManager.getDamageSuppression() < 75) {
                    this.configManager.setDamageSuppression( 75 );

                } else if (this.configManager.getDamageSuppression() < 100) {
                    this.configManager.setDamageSuppression( 100 );

                } else {
                    this.configManager.setDamageSuppression(0);
                }

                break;

            case "cure_requires_dead_sire":
                this.configManager.setCureRequiresSireDeath( !this.configManager.doCuresRequireSireDeath() );
                break;

            case "cure_requires_daylight":
                this.configManager.setCureRequiresDaytime( !this.configManager.doCuresRequireDaytime() );
                break;

            case "cure_book_spawning":
                this.sessionManager.setCureBooksEnabled( !this.sessionManager.isCureBooksEnabled() );
                break;

            case "enable_npc_mobs":
                this.sessionManager.setNpcSpawningGamerules( !this.configManager.areNpcMobsEnabled() );
                break;

            case "breeding_out_of_session":
                this.configManager.setBreedAnimalsOutOfSession( !this.configManager.canBreedAnimalsOutOfSession() );
                break;

            case "stake_permadeath_stage":
                // Shift the value up by one until we loop back around to Stage 1
                switch (this.configManager.getPermadeathMinimumStage()) {
                    case 1 -> this.configManager.setStakePermadeathMinimumStage(2);
                    case 2 -> this.configManager.setStakePermadeathMinimumStage(3);
                    case 3 -> this.configManager.setStakePermadeathMinimumStage(1);
                    default -> throw new IllegalStateException("Unknown stage attempted in config GUI adjustment");
                }
                break;

            case "human_life_limit":
                this.configManager.setLifeLimitEnforced( !this.configManager.isLifeLimitEnforced() );
                break;

            case "one_human_left":
                this.sessionManager.setOneHumanLeftActive( !this.sessionManager.isOneHumanLeftActive() );
                break;

            case "border_active":
                this.sessionManager.setBorderActive( !this.sessionManager.isBorderActive() );
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + commandName);
        }
    }

    /**
     * Clear the config inventory of items before shutting down the manager.
     */
    public void shutdown() {
        if (this.configGui == null) {
            return;
        }

        // Clear the config GUI inventory of items
        this.configGui.clear();
        this.configGui = null;
    }
}