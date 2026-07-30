package frostvein.sampires.remakepire.abilities.tome;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.utils.ConversionAssistant;

public class UncannyDirectionTomeAbility extends TomeAbility {
    /**
     * Create an instance of the Uncanny Direction tome ability.
     *
     * @param plugin the host plugin object.
     */
    public UncannyDirectionTomeAbility(RemakepirePlugin plugin) {
        super(plugin, "UncannyDirection", new String[]{"You gain an understanding of navigation you thought not possible,", "and can navigate your way back to home far more easily than before."}, plugin.getConfigManager().getTomeUncannyDirectionCooldown());
    }

    protected boolean useAbility(final Player player) {
        if (!this.canUse(player)) {
            this.sendCannotUseMessage(player, "Only humans can use tome abilities!");
            return false;

        } else {
            final double townCenterX = this.plugin.getConfigManager().getTownCenterX(), townCenterZ = this.plugin.getConfigManager().getTownCenterZ();

            (new BukkitRunnable() {
                final ConversionAssistant conversionAssistant =  new ConversionAssistant();
                int ticksRemaining = 140;

                public void run() {
                    if (this.ticksRemaining > 0 && player.isOnline()) {
                        Location currentLocation = player.getLocation();
                        final double deltaX = townCenterX - currentLocation.getX(), deltaZ = townCenterZ - currentLocation.getZ();
                        final double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

                        final String direction = conversionAssistant.getRelativeDirection(deltaX, deltaZ, currentLocation.getYaw());
                        final Component actionBarMessage = Component.text("Town Center: ", NamedTextColor.GOLD)
                                .append(Component.text(direction, NamedTextColor.WHITE))
                                .append(Component.text(" (", NamedTextColor.GRAY))
                                .append(Component.text(String.format("%.0f", distance), NamedTextColor.WHITE))
                                .append(Component.text(" blocks)", NamedTextColor.GRAY));

                        player.sendActionBar(actionBarMessage);

                        this.ticksRemaining -= 4;

                    } else {
                        this.cancel();
                    }
                }
            }).runTaskTimer(this.plugin, 0L, 4L);

            this.plugin.getWorld().playSound(player.getLocation(), "minecraft:item.lodestone_compass.lock", 1.0F, 1.2F);
            this.sendSuccessMessage(player, "Your inner compass awakens, pointing you toward home...");
            return true;
        }
    }
}
