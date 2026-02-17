package frostvein.sampires.remakepire.abilities.tome;

import java.util.Arrays;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Husk;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.Stray;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zoglin;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.ZombieHorse;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.scheduler.BukkitRunnable;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class BanishUndeadTomeAbility extends TomeAbility {
    private static final int RADIUS = 40;
    private final List<Class<? extends Entity>> undeadMobTypes = Arrays.asList(Zombie.class, Skeleton.class, Drowned.class, Husk.class, Stray.class, ZombieVillager.class, SkeletonHorse.class, ZombieHorse.class, Phantom.class, WitherSkeleton.class, Wither.class, Zoglin.class);

    public BanishUndeadTomeAbility(RemakepirePlugin plugin) {
        super(plugin, "BanishUndead", new String[]{"All undead mobs within a 40 block radius of you die instantly."}, plugin.getConfigManager().getTomeBanishUndeadCooldown());
    }

    protected boolean useAbility(Player player) {
        if (!this.canUse(player)) {
            this.sendCannotUseMessage(player, "Only humans can use tome abilities!");
            return false;
        } else if (player.getWorld() == null) {
            this.sendCannotUseMessage(player, "World not available!");
            return false;
        } else {
            int mobsKilled = 0;

            for(Entity entity : player.getNearbyEntities(40.0, 40.0, 40.0)) {
                if (this.isUndeadMob(entity) && entity instanceof LivingEntity) {
                    LivingEntity livingEntity = (LivingEntity)entity;
                    livingEntity.setHealth(0.0);
                    ++mobsKilled;
                }
            }

            this.createHolyLightRings(player);
            if (mobsKilled > 0) {
                player.playSound(player.getLocation(), "minecraft:block.beacon.power_select", 1.0F, 1.2F);
                this.sendSuccessMessage(player, "Holy light radiates from you, banishing " + mobsKilled + " undead creatures!");
            } else {
                player.playSound(player.getLocation(), "minecraft:block.beacon.ambient", 0.5F, 1.0F);
                this.sendSuccessMessage(player, "Holy light radiates from you, but no undead creatures were nearby.");
            }

            return true;
        }
    }

    private boolean isUndeadMob(Entity entity) {
        for(Class<? extends Entity> mobType : this.undeadMobTypes) {
            if (mobType.isInstance(entity)) {
                return true;
            }
        }

        return false;
    }

    private void createHolyLightRings(Player player) {
        final Location center = player.getLocation().add(0.0, 0.5, 0.0);
        long delayBetweenRings = 8L;
        (new BukkitRunnable() {
            int ringCount = 0;
            final int maxRings = 3;

            public void run() {
                if (this.ringCount >= maxRings) {
                    this.cancel();
                } else {
                    BanishUndeadTomeAbility.this.createHolyLightRing(center, this.ringCount);
                    ++this.ringCount;
                }
            }
        }).runTaskTimer(this.plugin, 0L, delayBetweenRings);
    }

    private void createHolyLightRing(final Location center, int ringIndex) {
        final double baseRadius = 5.0 + ringIndex * 8.0;
        final int particleCount = 40 + ringIndex * 20;
        final double angleStep = (Math.PI * 2D) / (double)particleCount;
        (new BukkitRunnable() {
            double currentRadius = 0.0;
            final double maxRadius = baseRadius;
            final double radiusStep;
            int tickCount;

            {
                this.radiusStep = this.maxRadius / 10.0;
                this.tickCount = 0;
            }

            public void run() {
                if (!(this.currentRadius >= this.maxRadius) && this.tickCount < 15) {
                    for(int i = 0; i < particleCount; ++i) {
                        double angle = i * angleStep;
                        double x = center.getX() + this.currentRadius * Math.cos(angle);
                        double z = center.getZ() + this.currentRadius * Math.sin(angle);
                        double y = center.getY();

                        Location particleLocation = new Location(center.getWorld(), x, y, z);
                        center.getWorld().spawnParticle(Particle.END_ROD, particleLocation, 1, 0.0, 0.2, 0.0, 0.01);

                        if (i % 4 == 0) {
                            center.getWorld().spawnParticle(Particle.ENCHANT, particleLocation.add(0.0, 0.3, 0.0), 2, 0.1, 0.1, 0.1, 0.02);
                        }
                    }

                    this.currentRadius += this.radiusStep;
                    ++this.tickCount;
                } else {
                    this.cancel();
                }
            }
        }).runTaskTimer(this.plugin, (long)ringIndex * 4L, 1L);
    }
}
