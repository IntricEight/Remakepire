package frostvein.sampires.remakepire.abilities.tome;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class UnnaturalHasteTomeAbility extends TomeAbility {
    // Controls the duration of the ability (in ticks)
    private static final int HASTE_DURATION = 6000;
    // Controls the intensity of the ability
    private static final int HASTE_AMPLIFIER = 1;

    /**
     * Create an instance of the Unnatural Haste tome ability.
     *
     * @param plugin the host plugin object..
     */
    public UnnaturalHasteTomeAbility(RemakepirePlugin plugin) {
        super(plugin, "UnnaturalHaste", new String[]{"You learn how to dip into a pool of strength unknown to this world,", "and gain haste for " + (HASTE_DURATION / 20 / 60) + " minutes."}, plugin.getConfigManager().getTomeUnnaturalHasteCooldown());
    }

    protected boolean useAbility(Player player) {
        if (!this.canUse(player)) {
            this.sendCannotUseMessage(player, "Only humans can use tome abilities!");
            return false;

        } else {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, HASTE_DURATION, HASTE_AMPLIFIER, false, false));
            this.plugin.getWorld().playSound(player.getLocation(), "minecraft:block.portal.ambient", 1.0F, 1.8F);
            this.sendSuccessMessage(player, "You tap into an otherworldly pool of energy...");
            player.sendMessage("§7Your hands move with unnatural speed for 5 minutes.");
            return true;
        }
    }
}