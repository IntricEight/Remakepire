package frostvein.sampires.remakepire.abilities.tome;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class UncannyDirectionTomeAbility extends TomeAbility {
    public UncannyDirectionTomeAbility(RemakepirePlugin plugin) {
        super(plugin, "UncannyDirection", new String[]{"You gain an understanding of navigation you thought not possible,", "and can navigate your way back to home far more easily than before."}, plugin.getConfigManager().getTomeUncannyDirectionCooldown());
    }

    protected boolean useAbility(final Player player) {
        if (!this.canUse(player)) {
            this.sendCannotUseMessage(player, "Only humans can use tome abilities!");
            return false;
        } else {
            final double townCenterX = this.plugin.getConfigManager().getOakhurstTownCenterX();
            final double townCenterZ = this.plugin.getConfigManager().getOakhurstTownCenterZ();
            (new BukkitRunnable() {
                int ticksRemaining = 140;

                public void run() {
                    if (this.ticksRemaining > 0 && player.isOnline()) {
                        Location currentLocation = player.getLocation();
                        double deltaX = townCenterX - currentLocation.getX();
                        double deltaZ = townCenterZ - currentLocation.getZ();
                        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                        String direction = UncannyDirectionTomeAbility.this.getRelativeDirection(deltaX, deltaZ, currentLocation.getYaw());
                        String actionBarMessage = String.format("§6Town Center: §f %s §7(§f%.0f blocks§7)", direction, distance);
                        UncannyDirectionTomeAbility.this.plugin.getSessionManager().sendActionBar(player, actionBarMessage);
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

    private String getRelativeDirection(double deltaX, double deltaZ, float playerYaw) {
        double targetAngle = Math.atan2(deltaX, -deltaZ);
        double targetDegrees = Math.toDegrees(targetAngle);
        if (targetDegrees < (double)0.0F) {
            targetDegrees += (double)360.0F;
        }

        double playerFacing = (double)((playerYaw + 180.0F) % 360.0F);
        if (playerFacing < (double)0.0F) {
            playerFacing += (double)360.0F;
        }

        double relativeAngle = (targetDegrees - playerFacing + (double)360.0F) % (double)360.0F;
        if (!(relativeAngle >= (double)337.5F) && !(relativeAngle < (double)22.5F)) {
            if (relativeAngle >= (double)22.5F && relativeAngle < (double)67.5F) {
                return "\ue00b";
            } else if (relativeAngle >= (double)67.5F && relativeAngle < (double)112.5F) {
                return "\ue00c";
            } else if (relativeAngle >= (double)112.5F && relativeAngle < (double)157.5F) {
                return "\ue00d";
            } else if (relativeAngle >= (double)157.5F && relativeAngle < (double)202.5F) {
                return "\ue00e";
            } else if (relativeAngle >= (double)202.5F && relativeAngle < (double)247.5F) {
                return "\ue00f";
            } else if (relativeAngle >= (double)247.5F && relativeAngle < (double)292.5F) {
                return "\ue010";
            } else {
                return relativeAngle >= (double)292.5F && relativeAngle < (double)337.5F ? "\ue011" : "\ue00a";
            }
        } else {
            return "\ue00a";
        }
    }
}
