package frostvein.sampires.remakepire.abilities.tome;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.SessionManager;
import frostvein.sampires.remakepire.utils.ItemTypeChecking;

public class BlessingTomeAbility extends TomeAbility {
    /**
     * Create an instance of the Blessing tome ability.
     *
     * @param plugin the host plugin object.
     */
    public BlessingTomeAbility(RemakepirePlugin plugin) {
        super(plugin, "Blessing", "Blessing", new String[]{"Once per day, you are able to turn a bottle of water", "into a splash bottle of holy water."}, plugin.getConfigManager().getTomeBlessingCooldown());
    }

    protected boolean useAbility(Player player) {
        final boolean isSessionCapped = this.plugin.getConfigManager().isHolyWaterSessionCapped();

        if (!this.canUse(player)) {
            this.sendCannotUseMessage(player, "Only humans can use tome abilities!");
            return false;

        } else if (isSessionCapped && player.getScoreboardTags().contains(SessionManager.BLESSING_USED_SESSION)) {
            this.sendCannotUseMessage(player, "You have already used Blessing this session!");
            return false;

        } else {
            PlayerInventory inventory = player.getInventory();
            ItemStack mainHandItem = inventory.getItemInMainHand();

            // Check if the player is holding a water bottle, and convert it into a splash potion of holy water
            if (mainHandItem.getType() == Material.POTION && ItemTypeChecking.isWaterBottle(mainHandItem)) {
                if (mainHandItem.getAmount() > 1) {
                    mainHandItem.setAmount(mainHandItem.getAmount() - 1);
                    ItemStack splashWater = new ItemStack(Material.SPLASH_POTION, 1);
                    this.addHolyWaterDescription(splashWater);

                    // Check if the player's inventory has room for the holy water
                    if (inventory.firstEmpty() != -1) {
                        inventory.addItem(splashWater);
                    } else {
                        player.getWorld().dropItemNaturally(player.getLocation(), splashWater);
                        player.sendMessage(Component.text("Your inventory is full. The holy water was dropped at your feet.", NamedTextColor.GRAY));
                    }
                } else {
                    ItemStack splashWater = new ItemStack(Material.SPLASH_POTION, 1);
                    this.addHolyWaterDescription(splashWater);
                    inventory.setItemInMainHand(splashWater);
                }

                player.playSound(player.getLocation(), "minecraft:block.beacon.activate", 0.8F, 1.4F);
                player.playSound(player.getLocation(), "minecraft:entity.player.levelup", 0.5F, 1.2F);
                this.sendSuccessMessage(player, "Divine light flows through the water, blessing it into holy water!");
                player.sendMessage(Component.text("The blessed water can now be thrown as a splash potion.", NamedTextColor.GRAY));

                player.addScoreboardTag(SessionManager.BLESSING_USED_SESSION);

                return true;

            } else {
                this.sendCannotUseMessage(player, "You must be holding a water bottle in your main hand!");
                return false;
            }
        }
    }

    /**
     * Replace the bottle's default item description with holy water's item description.
     *
     * @param item the item whose description will be replaced.
     */
    private void addHolyWaterDescription(ItemStack item) {
        ItemMeta meta = item.getItemMeta();

        if (meta instanceof PotionMeta potionMeta) {
            final int durationSeconds = this.plugin.getConfigManager().getHolyWaterDisableDurationSeconds();
            String durationText = durationSeconds >= 60
                    ? durationSeconds / 60 + " minute" + ((durationSeconds / 60) != 1 ? "s" : "")
                    : durationSeconds + " second" + (durationSeconds != 1 ? "s" : "");

            potionMeta.setBasePotionType(PotionType.WATER);
            potionMeta.customName(Component.text("Holy Water", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            potionMeta.lore(List.of(Component.text("Throw this on an evil creature to disable their powers for " + durationText + "!").decoration(TextDecoration.ITALIC, false)));

            item.setItemMeta(potionMeta);
        }
    }
}
