package frostvein.sampires.remakepire.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

public class ItemTypeChecking {
    /**
     * Determine if the item is an empty hand.
     *
     * @param type the item being checked.
     * @return {@code true} if the item is air or nonexistent.
     */
    public static boolean isBareFist(Material type) {
        return type == Material.AIR;
    }

    /**
     * Determine if the item is a sword.
     *
     * @param type the item being checked.
     * @return {@code true} if the item is a sword.
     */
    public static boolean isSword(Material type) {
        return type == Material.WOODEN_SWORD || type == Material.STONE_SWORD || type == Material.IRON_SWORD || type == Material.GOLDEN_SWORD || type == Material.DIAMOND_SWORD || type == Material.NETHERITE_SWORD;
    }

    /**
     * Determine if the item is an axe.
     *
     * @param type the item being checked.
     * @return {@code true} if the item is an axe.
     */
    public static boolean isAxe(Material type) {
        return type == Material.WOODEN_AXE || type == Material.STONE_AXE || type == Material.IRON_AXE || type == Material.GOLDEN_AXE || type == Material.DIAMOND_AXE || type == Material.NETHERITE_AXE;
    }

    /**
     * Determine if the item is a spear.
     *
     * @param type the item being checked.
     * @return {@code true} if the item is a spear.
     */
    public static boolean isSpear(Material type) {
        return type == Material.WOODEN_SPEAR || type == Material.STONE_SPEAR || type == Material.COPPER_SPEAR || type == Material.IRON_SPEAR || type == Material.GOLDEN_SPEAR || type == Material.DIAMOND_SPEAR || type == Material.NETHERITE_SPEAR;
    }

    /**
     * Determine if the item is classified as a weapon.
     *
     * @param type the item being checked.
     * @return {@code true} if the item is a conventional weapon.
     */
    public static boolean isWeapon(Material type) {
        return isSword(type) || isAxe(type) || isSpear(type);
    }

    /**
     * Determine if the item is a wooden stake.
     *
     * @param type the item being checked.
     * @return {@code true} if the item is a wooden sword.
     */
    public static boolean isStake(Material type) {
        return type == Material.WOODEN_SWORD || type == Material.WOODEN_SPEAR;
    }

    /**
     * Determine if the item is a bottle of blood.
     *
     * @param type the item being checked.
     * @return {@code true} if the item is an experience bottle.
     */
    public static boolean isBloodBottle(Material type) {
        return type == Material.EXPERIENCE_BOTTLE;
    }

    /**
     * Retrieve the item type being used for blood bottles in this plugin.
     */
    public static Material getBloodBottleType() {
        return Material.EXPERIENCE_BOTTLE;
    }

    /**
     * Determine if the item is a wooden weapon.
     *
     * @param type the item being checked.
     * @return {@code true} if the item is a wooden sword or axe.
     */
    public static boolean isWoodenWeapon(Material type) {
        if (type == null) {
            return false;
        } else {
            return type == Material.WOODEN_SWORD || type == Material.WOODEN_AXE || type == Material.WOODEN_SPEAR;
        }
    }

    /**
     * Determine if the item is an iron weapon.
     *
     * @param type the item being checked.
     * @return {@code true} if the item is a wooden sword or axe.
     */
    public static boolean isIronWeapon(Material type) {
        if (type == null) {
            return false;
        } else {
            return type == Material.IRON_SWORD || type == Material.IRON_AXE || type == Material.IRON_SPEAR;
        }
    }

    /**
     * Determine if a potion is a splash bottle of water.
     *
     * @param item the item being checked.
     * @return {@code true} if the item does not have potion metadata or is an effectless potion.
     */
    public static boolean isHolyWater(ItemStack item) {
        if (item == null) {
            return false;
        } else if (item.getType() != Material.SPLASH_POTION) {
            return false;
        } else if (!item.hasItemMeta()) {
            return true;
        } else if (!(item.getItemMeta() instanceof PotionMeta potionMeta)) {
            return true;
        } else {
            if (potionMeta.hasCustomEffects()) {
                return false;
            } else {
                PotionType baseType = potionMeta.getBasePotionType();

                if (baseType != null && baseType != PotionType.WATER) {
                    return baseType == PotionType.AWKWARD || baseType == PotionType.MUNDANE || baseType == PotionType.THICK;
                } else {
                    return true;
                }
            }
        }
    }

    /**
     * Determine if the item is a water bottle.
     *
     * @param item the item being checked.
     * @return {@code true} if this item is a water bottle.
     */
    public static boolean isWaterBottle(ItemStack item) {
        if (item.getType() != Material.POTION) {
            return false;
        } else {
            return !item.hasItemMeta() || item.getItemMeta().getPersistentDataContainer().isEmpty();
        }
    }

    /**
     * Determine if an item is raw food.
     *
     * @param item the item being checked.
     * @return {@code true} if the item is raw meat.
     */
    public static boolean isRaw(ItemStack item) {
        String itemName = item.getType().name();

        if (itemName.contains("RAW")) {
            return true;
        } else {
            return switch (itemName) {
                case "BEEF", "PORKCHOP", "CHICKEN", "RABBIT", "MUTTON" -> true;
                default -> false;
            };
        }
    }

    /**
     * Determine if the item is a proper food item.
     *
     * @param item the item being checked.
     * @return {@code true} if the item is regular food (Check this function for the list of "not-regular" foods)
     */
    public static boolean isActualFood(ItemStack item) {
        Material type = item.getType();

        if (!type.isEdible()) {
            return false;
        } else {
            return !type.name().contains("POTION") && type != Material.ENDER_PEARL && type != Material.CHORUS_FRUIT && type != Material.ENCHANTED_GOLDEN_APPLE && type != Material.GOLDEN_APPLE && type != Material.BEETROOT;
        }
    }
}
