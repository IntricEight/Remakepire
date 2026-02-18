package frostvein.sampires.remakepire.abilities.tome;

import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class RallyingCryTomeAbility extends TomeAbility {
    // Controls the size of the ability
    private static final int EFFECT_RADIUS = 20;
    // Controls the duration of the ability
    private static final int STRENGTH_DURATION = 600;
    // Controls the intensity of the ability
    private static final int STRENGTH_AMPLIFIER = 0;

    /**
     * Create an instance of the Rallying Cry tome ability.
     *
     * @param plugin the host plugin object.
     */
    public RallyingCryTomeAbility(RemakepirePlugin plugin) {
        super(plugin, "RallyingCry", new String[]{"You learn the secrets needed to inspire man.", "At your word, you and humans around you", "gain strength for " + (STRENGTH_DURATION / 20) + " seconds."}, plugin.getConfigManager().getTomeRallyingCryCooldown());
    }

    protected boolean useAbility(Player player) {
        if (!this.canUse(player)) {
            this.sendCannotUseMessage(player, "Only humans can use tome abilities!");
            return false;

        } else {
            int affectedCount = 0;

            // Give the caster the strength effect
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, STRENGTH_DURATION, STRENGTH_AMPLIFIER, false, false));
            ++affectedCount;

            List<Player> nearbyHumans = player.getNearbyEntities(EFFECT_RADIUS, EFFECT_RADIUS, EFFECT_RADIUS).stream().
                    filter((entity) -> entity instanceof Player).map((entity) -> (Player)entity).filter((nearbyPlayer) -> this.plugin.getVampireManager().isHuman(nearbyPlayer)).toList();
            List<Player> nearbyVampires = player.getNearbyEntities(EFFECT_RADIUS, EFFECT_RADIUS, EFFECT_RADIUS).stream()
                    .filter((entity) -> entity instanceof Player).map((entity) -> (Player)entity).filter((nearbyPlayer) -> !this.plugin.getVampireManager().isHuman(nearbyPlayer)).toList();

            // Give nearby humans the strength effect
            for(Player nearbyHuman : nearbyHumans) {
                nearbyHuman.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, STRENGTH_DURATION, STRENGTH_AMPLIFIER, false, false));
                nearbyHuman.sendMessage("§6" + player.getName() + "'s rallying cry fills you with strength.");
                ++affectedCount;
            }

            // Inform nearby vampires of their inhumanity
            for(Player nearbyVampire : nearbyVampires) {
                nearbyVampire.sendMessage("§7A human nearby rallies strength to their comrades. The words find no purchase in your cold dead heart.");
            }

            this.plugin.getWorld().playSound(player.getLocation(), "minecraft:entity.pillager.celebrate", 1.0F, 1.0F);
            this.sendSuccessMessage(player, "Your rallying cry inspires those around you!");

            return true;
        }
    }
}