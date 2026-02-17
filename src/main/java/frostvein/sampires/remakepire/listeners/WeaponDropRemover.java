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
    private static final Set<Material> FISHING_REPLACEMENTS;
    private static final Set<Material> WEAPON_MATERIALS;

    public WeaponDropRemover(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            Iterator<ItemStack> dropIterator = event.getDrops().iterator();
            int weaponsRemoved = 0;

            while(dropIterator.hasNext()) {
                ItemStack drop = (ItemStack)dropIterator.next();
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

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == State.CAUGHT_FISH) {
            if (event.getCaught() instanceof Item) {
                Item caughtItem = (Item)event.getCaught();
                ItemStack itemStack = caughtItem.getItemStack();
                if (itemStack != null && WEAPON_MATERIALS.contains(itemStack.getType())) {
                    Material replacement = this.getRandomFishingReplacement();
                    ItemStack replacementItem = new ItemStack(replacement, 1);
                    caughtItem.setItemStack(replacementItem);
                }

            }
        }
    }

    private Material getRandomFishingReplacement() {
        Material[] replacements = (Material[])FISHING_REPLACEMENTS.toArray(new Material[0]);
        return replacements[this.random.nextInt(replacements.length)];
    }

    public Set<Material> getWeaponMaterials() {
        return WEAPON_MATERIALS;
    }

    public Set<Material> getFishingReplacements() {
        return FISHING_REPLACEMENTS;
    }

    public boolean isWeapon(Material material) {
        return WEAPON_MATERIALS.contains(material);
    }

    static {
        FISHING_REPLACEMENTS = EnumSet.of(Material.COD, Material.SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH, Material.KELP, Material.SEAGRASS, Material.STICK, Material.LEATHER, Material.BONE, Material.STRING);
        WEAPON_MATERIALS = EnumSet.of(Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE, Material.BOW, Material.CROSSBOW, Material.TRIDENT, Material.MACE, Material.ARROW, Material.SPECTRAL_ARROW, Material.TIPPED_ARROW);
    }
}
