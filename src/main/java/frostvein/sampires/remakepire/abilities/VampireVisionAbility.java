package frostvein.sampires.remakepire.abilities;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.VampireManager;

public class VampireVisionAbility extends VampireAbility {
    public String getName() {
        return "vision";
    }

    public String getDisplayName() {
        return "Vampire Vision";
    }

    public String getDescription() {
        return "Toggle supernatural night vision on and off.";
    }

    public int getCooldownSeconds(RemakepirePlugin plugin) {
        return plugin.getConfigManager().getVampireVisionCooldown();
    }

    public int getMinimumStage() {
        return 1;
    }

    public boolean execute(Player player, VampireManager vampireManager, RemakepirePlugin plugin) {
        // Deactivate the ability if it is in use
        if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.sendMessage("§8Your supernatural vision fades... The world returns to natural darkness.");

        } else {
            PotionEffect nightVision = new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 0, false, false, false);
            player.addPotionEffect(nightVision);
            player.sendMessage("§5Your vampiric eyes pierce through the darkness...");
        }

        player.playSound(player.getLocation(), Sound.BLOCK_LODESTONE_PLACE, 0.5F, 1.0F);
        return true;
    }
}
