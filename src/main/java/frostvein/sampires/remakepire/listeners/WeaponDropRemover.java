package frostvein.sampires.remakepire.listeners;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class WeaponDropRemover implements Listener {
    private final RemakepirePlugin plugin;
    private final Random random;
    private static final Set<Material> FISHING_REPLACEMENTS = EnumSet.of(Material.COD, Material.SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH, Material.KELP, Material.SEAGRASS, Material.STICK, Material.LEATHER, Material.BONE, Material.STRING);
    private static final Set<Material> WEAPON_MATERIALS = EnumSet.of(Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE, Material.BOW, Material.CROSSBOW, Material.TRIDENT, Material.MACE, Material.ARROW, Material.SPECTRAL_ARROW, Material.TIPPED_ARROW);

    /**
     * Create an instance of the Weapon Drop Remover listener.
     *
     * @param plugin the host plugin object.
     */
    public WeaponDropRemover(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    /**
     * Prevent entities from dropping weapons from their drop table.
     *
     * @param event an entity dying.
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (!(entity instanceof Player)) {
            Iterator<ItemStack> dropIterator = event.getDrops().iterator();
            int weaponsRemoved = 0;

            while(dropIterator.hasNext()) {
                ItemStack drop = dropIterator.next();

                if (drop != null && WEAPON_MATERIALS.contains(drop.getType())) {
                    dropIterator.remove();
                    ++weaponsRemoved;
                }
            }

            if (weaponsRemoved > 0) {
                this.plugin.getLogger().info("Removed " + weaponsRemoved + " weapon(s) from " + String.valueOf(entity.getType()) + " drops");
            }
        }
    }

    /**
     * Prevent fishing from dropping weapons from the fishing drop table.
     *
     * @param event a player attempting to fish.
     */
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == State.CAUGHT_FISH) {
            if (event.getCaught() instanceof Item caughtItem) {
                ItemStack itemStack = caughtItem.getItemStack();

                if (itemStack != null && WEAPON_MATERIALS.contains(itemStack.getType())) {
                    Material replacement = this.getRandomFishingReplacement();
                    ItemStack replacementItem = new ItemStack(replacement, 1);
                    caughtItem.setItemStack(replacementItem);
                }
            }
        }
    }

    /**
     * Retrieve a random fishing item.
     *
     * @return A random {@code Material} from the fishing drop table replacements {@code Set}.
     */
    private Material getRandomFishingReplacement() {
        Material[] replacements = FISHING_REPLACEMENTS.toArray(new Material[0]);
        return replacements[this.random.nextInt(replacements.length)];
    }

    /**
     * Retrieve the list of weapons blocked from drop tables.
     *
     * @return A {@code Set} of materials that will not be dropped.
     */
    public Set<Material> getWeaponMaterials() {
        return WEAPON_MATERIALS;
    }

    /**
     * Retrieve the list of weapon replacements for the fishing drop table.
     *
     * @return A {@code Set} of materials that will replace weapons in the fishing drop table.
     */
    public Set<Material> getFishingReplacements() {
        return FISHING_REPLACEMENTS;
    }

    /**
     * Determine if an item or block is a weapon.
     *
     * @param material the item or block to check.
     * @return {@code true} if the material is within the weapons list.
     */
    public boolean isWeapon(Material material) {
        return WEAPON_MATERIALS.contains(material);
    }
}
