package frostvein.sampires.remakepire.utils;

import org.bukkit.Material;

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
     * Determine if the item is classified as a weapon.
     *
     * @param type the item being checked.
     * @return {@code true} if the item is a conventional weapon.
     */
    public static boolean isWeapon(Material type) {
        return isSword(type) || isAxe(type);
    }

    /**
     * Determine if the item is a wooden stake.
     *
     * @param type the item being checked.
     * @return {@code true} if the item is a wooden stake.
     */
    public static boolean isStake(Material type) {
        return type == Material.WOODEN_SWORD;
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
            return type == Material.WOODEN_SWORD || type == Material.WOODEN_AXE;
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
            return type == Material.IRON_SWORD || type == Material.IRON_AXE;
        }
    }


}
