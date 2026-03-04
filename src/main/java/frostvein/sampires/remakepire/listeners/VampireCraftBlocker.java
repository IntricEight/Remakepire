package frostvein.sampires.remakepire.listeners;

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class VampireCraftBlocker implements Listener {
    RemakepirePlugin plugin;
    private static final Set<Material> BLOCKED_WEAPONS = EnumSet.of(Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE, Material.BOW, Material.CROSSBOW, Material.MACE, Material.TRIDENT);

    /**
     * Create an instance of the Vampire Crafting listener.
     *
     * @param plugin the host plugin object.
     */
    public VampireCraftBlocker(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Prevent vampires from crafting certain items.
     *
     * @param event an item is crafted.
     */
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (this.plugin.getSessionManager().isOutOfSession() && !this.plugin.getSessionManager().isPreSession()) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage("§cYou cannot craft while the session is inactive.");
        }

        Material craftedMaterial = event.getRecipe().getResult().getType();

        if (BLOCKED_WEAPONS.contains(craftedMaterial)) {
            if (this.plugin.getIronWeaknessListener().getIronMaterials().contains(craftedMaterial)) {
                Player player = (Player)event.getWhoClicked();

                if (this.plugin.getVampireManager().isVampire(player)) {
                    event.setCancelled(true);

                    if (!player.getScoreboardTags().contains("informed_crafting_items")) {
                        player.addScoreboardTag("informed_crafting_items");
                        player.sendMessage("§cYou find yourself unable to put your mind to the task of crafting this... Such trinkets are beneath you.");
                    }
                }
            }
        }
    }

    /**
     * Prevent vampires from enchanting items.
     *
     * @param event an item receives an enchantment.
     */
    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        Player player = event.getEnchanter();

        if (this.plugin.getVampireManager().isVampire(player)) {
            event.setCancelled(true);

            if (!player.getScoreboardTags().contains("informed_enchanting_items")) {
                player.addScoreboardTag("informed_enchanting_items");
                player.sendMessage("§cThe ancient magics resist your alien essence... You cannot channel enchantments.");
            }
        }
    }
}
