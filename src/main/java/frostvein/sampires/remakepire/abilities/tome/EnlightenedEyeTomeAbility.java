package frostvein.sampires.remakepire.abilities.tome;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class EnlightenedEyeTomeAbility extends TomeAbility {
    // Controls the duration of the ability (in ticks)
    private static final int NIGHT_VISION_DURATION = 6000;
    // Controls the intensity of the ability
    private static final int NIGHT_VISION_AMPLIFIER = 0;

    /**
     * Create an instance of the Enlightened Eye tome ability.
     *
     * @param plugin the host plugin object.
     */
    public EnlightenedEyeTomeAbility(RemakepirePlugin plugin) {
        super(plugin, "EnlightenedEye", new String[]{"You learn the secret to discern shapes from shadow,", "and gain night vision for 5 minutes."}, plugin.getConfigManager().getTomeEnlightenedEyeCooldown());
    }

    protected boolean useAbility(Player player) {
        if (!this.canUse(player)) {
            this.sendCannotUseMessage(player, "Only humans can use tome abilities!");
            return false;

        } else {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, NIGHT_VISION_DURATION, NIGHT_VISION_AMPLIFIER, false, false));
            player.playSound(player.getLocation(), "minecraft:block.beacon.power_select", 1.0F, 1.5F);
            this.sendSuccessMessage(player, "Your eyes adjust to pierce the darkness...");
            player.sendMessage("§7You can now see clearly in the shadows for 5 minutes.");

            return true;
        }
    }
}
