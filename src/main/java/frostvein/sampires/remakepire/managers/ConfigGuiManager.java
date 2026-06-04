package frostvein.sampires.remakepire.managers;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class ConfigGuiManager {
    private final RemakepirePlugin plugin;
    public static final String CONFIG_GUI_TITLE = "§8§lConfiguration Options";

    public ConfigGuiManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Open an inventory screen for modifying values within the config.
     *
     * @param admin the admin selecting tomes.
     */
    public void openConfigGUI(Player admin) {
        /* Inventory Layout
         * Leave a row and column of empty space around the items to format things.
         * This means that for every row of 9 items, only the middle 7 should be used.
         * Additionally, the first and last 9 items should be unused to create the top and bottom empty rows.
         */
        Inventory gui = Bukkit.createInventory(null, 9 * 5, CONFIG_GUI_TITLE);

        // Row 2
        gui.setItem(getCommandPositionInGui("alert_on_quit"), this.getGuiItem("alert_on_quit"));
        gui.setItem(getCommandPositionInGui("holy_water_cap"), this.getGuiItem("holy_water_cap"));
        gui.setItem(getCommandPositionInGui("tome_cap"), this.getGuiItem("tome_cap"));
        gui.setItem(getCommandPositionInGui("vampire_level_cap"), this.getGuiItem("vampire_level_cap"));
        gui.setItem(getCommandPositionInGui("new_vampire_tracking"), this.getGuiItem("new_vampire_tracking"));
        gui.setItem(getCommandPositionInGui("allow_vampire_mounts"), this.getGuiItem("allow_vampire_mounts"));
        gui.setItem(getCommandPositionInGui("vampire_health_check"), this.getGuiItem("vampire_health_check"));

        // Row 3
        gui.setItem(getCommandPositionInGui("damage_suppression"), this.getGuiItem("damage_suppression"));
        gui.setItem(getCommandPositionInGui("cure_requires_dead_sire"), this.getGuiItem("cure_requires_dead_sire"));
        gui.setItem(getCommandPositionInGui("cure_book_spawning"), this.getGuiItem("cure_book_spawning"));
        gui.setItem(getCommandPositionInGui("enable_npc_mobs"), this.getGuiItem("enable_npc_mobs"));
        gui.setItem(getCommandPositionInGui("breeding_out_of_session"), this.getGuiItem("breeding_out_of_session"));
        gui.setItem(getCommandPositionInGui("stake_permadeath_stage"), this.getGuiItem("stake_permadeath_stage"));
        gui.setItem(getCommandPositionInGui("human_life_limit"), this.getGuiItem("human_life_limit"));

        // Row 4
        gui.setItem(getCommandPositionInGui("one_human_left"), this.getGuiItem("one_human_left"));
        gui.setItem(getCommandPositionInGui("border_active"), this.getGuiItem("border_active"));

        admin.openInventory(gui);
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
        // Retrieve the placement of each command within the GUI design.
        return switch (commandName) {
            // Row 1
            case "alert_on_quit" ->             11;
            case "holy_water_cap" ->            12;
            case "tome_cap" ->                  13;
            case "vampire_level_cap" ->         14;
            case "new_vampire_tracking" ->      15;
            case "allow_vampire_mounts" ->      16;
            case "vampire_health_check" ->      17;
            // Row 2
            case "damage_suppression" ->        20;
            case "cure_requires_dead_sire" ->   21;
            case "cure_book_spawning" ->        22;
            case "enable_npc_mobs" ->           23;
            case "breeding_out_of_session" ->   24;
            case "stake_permadeath_stage" ->    25;
            case "human_life_limit" ->          26;
            // Row 3
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
            case "holy_water_cap" ->            Material.SPLASH_POTION;
            case "tome_cap" ->                  Material.CHEST;
            case "vampire_level_cap" ->         Material.BAT_SPAWN_EGG;
            case "new_vampire_tracking" ->      Material.RECOVERY_COMPASS;
            case "allow_vampire_mounts" ->      Material.SADDLE;
            case "vampire_health_check" ->      Material.EXPERIENCE_BOTTLE;
            case "damage_suppression" ->        Material.STONE_SWORD;
            case "cure_requires_dead_sire" ->   Material.WITHER_SKELETON_SKULL;
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
         * Status (Bolded)
         * Description
         */

        ItemMeta meta = visual.getItemMeta();
        List<String> instructions = new ArrayList<>();

        // Create the title of the config control
        String itemTitle = getConfigGuiTitle(commandName);

        // Create the item description of the config control
        instructions.add(this.getConfigStatus(commandName));
        instructions.add("");
        instructions.addAll(getConfigCommandDescription(commandName));
        instructions.add("");
        instructions.add("§oClick to modify this value");

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
        return switch (commandName) {
            case "alert_on_quit" ->             "Alert Admin on Player Quit";
            case "holy_water_cap" ->            "Limit Holy Water Creation";
            case "tome_cap" ->                  "Limit Tome Ability Acquisition";
            case "vampire_level_cap" ->         "Prevent Returning to Levels";
            case "new_vampire_tracking" ->      "Track New Vampires";
            case "allow_vampire_mounts" ->      "Allow Vampires Living Mounts";
            case "vampire_health_check" ->      "Vampire Regeneration Rate";
            case "damage_suppression" ->        "Player Damage Resistance";
            case "cure_requires_dead_sire" ->   "Sire Death for Curing";
            case "cure_book_spawning" ->        "Allow Cure Book Spawning";
            case "enable_npc_mobs" ->           "Allow NPC Mob Spawns";
            case "breeding_out_of_session" ->   "Allow Animal Breeding anytime";
            case "stake_permadeath_stage" ->    "Level where Staking Permakills";
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
                    this.plugin.getConfigManager().shouldAlertOnPlayerQuit() ? TRUE + "ALERT" : FALSE + "SILENCED";
            case "holy_water_cap" ->
                    this.plugin.getConfigManager().isHolyWaterSessionCapped() ? TRUE + "SINGLE USE" : FALSE + "COOLDOWN";
            case "tome_cap" ->
                    this.plugin.getConfigManager().isTomeAbsorptionCapped() ? TRUE + "SINGLE USE" : FALSE + "COOLDOWN";
            case "vampire_level_cap" ->
                    this.plugin.getConfigManager().isVampireLevelingCapped() ? TRUE + "LOCKED OUT" : FALSE + "REGAIN LEVELS";
            case "new_vampire_tracking" ->
                    this.plugin.getConfigManager().canTrackNewVampires() ? TRUE + "TRACKING" : FALSE + "HIDDEN";
            case "allow_vampire_mounts" ->
                    this.plugin.getConfigManager().canVampiresRideLivingMounts() ? TRUE + "ALLOWED" : FALSE + "FORBIDDEN";
            case "vampire_health_check" ->
                    OTHER + this.plugin.getConfigManager().getVampireHealthCheckTicks();
            case "damage_suppression" ->
                    OTHER + this.plugin.getConfigManager().getDamageSuppression() + "%";
            case "cure_requires_dead_sire" ->
                    this.plugin.getConfigManager().doCuresRequireSireDeath() ? TRUE + "REQUIRED" : FALSE + "NOT REQUIRED";
            case "cure_book_spawning" ->
                    this.plugin.getSessionManager().isCureBooksEnabled() ? TRUE + "SPAWNING" : FALSE + "DISABLED";
            case "enable_npc_mobs" ->
                    this.plugin.getConfigManager().areNpcMobsEnabled() ? TRUE + "ALLOWED" : FALSE + "PREVENTED";
            case "breeding_out_of_session" ->
                    this.plugin.getConfigManager().canBreedAnimalsOutOfSession() ? TRUE + "ALLOWED" : FALSE + "PREVENTED";
            case "stake_permadeath_stage" ->
                    OTHER + this.plugin.getConfigManager().getPermadeathMinimumStage();
            case "human_life_limit" ->
                    this.plugin.getConfigManager().isLifeLimitEnforced() ? TRUE + "ENFORCED" : FALSE + "VAMPIRE ONLY";
            case "one_human_left" ->
                    this.plugin.getSessionManager().isOneHumanLeftActive() ? TRUE + "ACCELERATED" : FALSE + "NORMAL";
            case "border_active" ->
                    this.plugin.getSessionManager().isBorderActive() ? TRUE + "TRAPPED" : FALSE + "DISABLED";
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

        switch (commandName) {
            case "alert_on_quit":
                description.add("Alert admins when a player leaves the game.");
                break;

            case "holy_water_cap":
                description.add("Limit players to creating only a single holy water each session.");
                break;

            case "tome_cap":
                description.add("Limit players to absorbing only a single tome book each session.");
                break;

            case "vampire_level_cap":
                description.add("Prevent vampires from returning to a vampire stage lost during the active session.");
                break;

            case "new_vampire_tracking":
                description.add("Provide vampires with a tracking arrow toward newly created vampires.");
                break;

            case "allow_vampire_mounts":
                description.add("Let higher vampires ride animals that would ordinarily recoil from their true nature.");
                break;

            case "vampire_health_check":
                description.add("The tick intervals where vampires drain their blood bar to regenerate health.");
                break;

            case "damage_suppression":
                description.add("The percentage of damage that players ignore from all sources.");
                break;

            case "cure_requires_dead_sire":
                description.add("Require a vampire's sire to have been permakilled before they can be cured of vampirism.");
                break;

            case "cure_book_spawning":
                description.add("Spawn cure books inside the tome chests at random, replacing their previous contents.");
                break;

            case "enable_npc_mobs":
                description.add("Allow the spawning of humanoid entities that players may find disruptive to the classic Vampires experience.");
                description.add("");
                description.add("Check the setting \"enable-npc-mobs\" to view the list of NPC mobs.");
                break;

            case "breeding_out_of_session":
                description.add("Let players breed and hatch passive animals during Build Mode.");
                break;

            case "stake_permadeath_stage":
                description.add("Staked vampire will be permakilled while at this stage or lower.");
                break;

            case "human_life_limit":
                description.add("Cause humans to be permakilled when they die after running out of lives, regardless of their cause of death.");
                break;

            case "one_human_left":
                description.add("Massively boost the human conversion rate on beacons.");
                break;

            case "border_active":
                description.add("Trap players within the boundaries of the game while that player has not met one of the leaving conditions.");
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + commandName);
        }

        return description;
    }


}