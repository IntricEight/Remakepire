package frostvein.sampires.remakepire.abilities.tome;

import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class RallyingCryTomeAbility extends TomeAbility {
    private static final int EFFECT_RADIUS = 20;
    private static final int STRENGTH_DURATION = 600;
    private static final int STRENGTH_AMPLIFIER = 0;

    public RallyingCryTomeAbility(RemakepirePlugin plugin) {
        super(plugin, "RallyingCry", new String[]{"You learn the secrets needed to inspire man.", "At your word, you and humans around you", "gain strength for 30 seconds."}, plugin.getConfigManager().getTomeRallyingCryCooldown());
    }

    protected boolean useAbility(Player player) {
        if (!this.canUse(player)) {
            this.sendCannotUseMessage(player, "Only humans can use tome abilities!");
            return false;
        } else {
            int affectedCount = 0;
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 600, 0, false, false));
            ++affectedCount;
            List<Player> nearbyHumans = player.getNearbyEntities(20.0, 20.0, 20.0).stream().filter((entity) -> entity instanceof Player).map((entity) -> (Player)entity).filter((nearbyPlayer) -> this.plugin.getVampireManager().isHuman(nearbyPlayer)).toList();
            List<Player> nearbyVampires = player.getNearbyEntities(20.0, 20.0, 20.0).stream().filter((entity) -> entity instanceof Player).map((entity) -> (Player)entity).filter((nearbyPlayer) -> !this.plugin.getVampireManager().isHuman(nearbyPlayer)).toList();

            for(Player nearbyHuman : nearbyHumans) {
                nearbyHuman.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 600, 0, false, false));
                nearbyHuman.sendMessage("§6" + player.getName() + "'s rallying cry fills you with strength.");
                ++affectedCount;
            }

            for(Player nearbyVampire : nearbyVampires) {
                nearbyVampire.sendMessage("§7A human nearby rallies strength to their comrades. The words find no purchase in your cold dead heart.");
            }

            this.plugin.getWorld().playSound(player.getLocation(), "minecraft:entity.pillager.celebrate", 1.0F, 1.0F);
            this.sendSuccessMessage(player, "Your rallying cry inspires those around you!");
            return true;
        }
    }
}

