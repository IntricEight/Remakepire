/* BLOOD TIES - Vampire Ability by IntricEight
 *
 * This ability gives vampires a directional indicator of one of their fledgling's locations.
 *
 * Use Conditions:
 *  - The vampire has turned another player into a vampire.
 *
 * Implementation Requirements:
 *  - Include the BloodTiesListener.java file and follow its instructions
 *  - Add "blood-ties-cooldown: {integer}" to config.yml in abilities.vampire
 *  - Implement getBloodTiesCooldown() inside managers/ConfigManager.java
 *  - Add this file (BloodTiesAbility.java) to the abilities folder
 *  - Add "bloodties" to the VAMPIRE_ABILITIES list inside commands/BrigadierCommands.java
 *  - Register BloodTiesAbility() in registerAbilities inside managers/VampireAbilityManager.java
 */

package frostvein.sampires.remakepire.abilities;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.VampireManager;
import org.bukkit.inventory.meta.SkullMeta;


public class BloodTiesAbility extends VampireAbility {
    public static final Component INVENTORY_GUI_TITLE = Component.text("Your Fledglings", NamedTextColor.DARK_RED)
            .decorate(TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false);

    public String getName() {
        return "bloodties";
    }

    public String getDisplayName() {
        return "Blood Ties";
    }

    public String getDescription() {
        return "Use your bond as a sire to find your fledgelings.";
    }

    public int getCooldownSeconds(RemakepirePlugin plugin) {
        return plugin.getConfigManager().getBloodTiesCooldown();
    }

    public int getMinimumStage() {
        return 1;
    }

    public boolean execute(Player player, VampireManager vampireManager, RemakepirePlugin plugin) {


        // Get a list of the vampire's fledglings
        List<String> fledglingNames = plugin.getSireManager().getFledglings(player);

        // Remove offline, dead, or cured players from the tracking list
        fledglingNames.removeIf(name -> {
            Player fledgling = Bukkit.getPlayerExact(name);

            return fledgling == null || !fledgling.isOnline() || fledgling.getGameMode() == GameMode.SPECTATOR || fledgling.isDead() || fledgling.getScoreboardTags().contains("CuredVampire");
        });

        // Make sure the player has fledglings to track before letting them use the ability
        if (fledglingNames.isEmpty()) {
            player.sendMessage(Component.text("You have no fledglings to track.", NamedTextColor.RED));
            return false;
        }

        this.openFledgingOptionGUI(player, fledglingNames);
        return false;
    }

    /**
     * Provide the player with a GUI for the beacon teleportation.
     *
     * @param player the player using the ability.
     * @param fledglings a list of the player's fledglings.
     */
    private void openFledgingOptionGUI(Player player, List<String> fledglings) {
        int slots = Math.max(9, (fledglings.size() + 8) / 9 * 9);
        slots = Math.min(54, slots);
        Inventory inventory = Bukkit.createInventory(null, slots, INVENTORY_GUI_TITLE);

        for (int i = 0; i < fledglings.size() && i < slots; ++i) {
            Player fledgling = Bukkit.getPlayer(fledglings.get(i));
            ItemStack item = this.createBeaconItem(fledgling);
            inventory.setItem(i, item);
        }

        player.openInventory(inventory);
    }

    /**
     * Create a beacon item representing a beacon.
     *
     * @param fledgling a player who can be tracked.
     * @return a beacon item detailing a desecrated beacon.
     */
    private ItemStack createBeaconItem(Player fledgling) {
        ItemStack item = this.createPlayerHead(fledgling);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.customName(Component.text(fledgling.getName(), NamedTextColor.WHITE)
                    .decorate(TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false)
            );

            List<String> lore = new ArrayList<>();

            lore.add("§7Whispers nudge you in the direction of your fledgling.");
            lore.add("");
            lore.add("§e▶ Click to track " + fledgling.getName());

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Create a head item for the player.
     *
     * @param player the player being represented by their head.
     * @return An {@code ItemStack} of the player's head.
     */
    private ItemStack createPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta)head.getItemMeta();

        meta.setPlayerProfile(player.getPlayerProfile());
        meta.displayName(Component.text(player.getName()));

        head.setItemMeta(meta);

        return head;
    }
}
