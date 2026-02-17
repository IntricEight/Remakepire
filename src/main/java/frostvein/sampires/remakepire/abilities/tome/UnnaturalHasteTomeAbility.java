package frostvein.sampires.remakepire.abilities.tome;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class UnnaturalHasteTomeAbility extends TomeAbility {
    private static final int HASTE_DURATION = 6000;
    private static final int HASTE_AMPLIFIER = 1;

    public UnnaturalHasteTomeAbility(RemakepirePlugin plugin) {
        super(plugin, "UnnaturalHaste", new String[]{"You learn how to dip into a pool of strength unknown to this world,", "and gain haste for 5 minutes."}, plugin.getConfigManager().getTomeUnnaturalHasteCooldown());
    }

    protected boolean useAbility(Player player) {
        if (!this.canUse(player)) {
            this.sendCannotUseMessage(player, "Only humans can use tome abilities!");
            return false;
        } else {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 6000, 1, false, false));
            this.plugin.getWorld().playSound(player.getLocation(), "minecraft:block.portal.ambient", 1.0F, 1.8F);
            this.sendSuccessMessage(player, "You tap into an otherworldly pool of energy...");
            player.sendMessage("§7Your hands move with unnatural speed for 5 minutes.");
            return true;
        }
    }
}