package frostvein.sampires.remakepire.abilities.tome;

import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Bogged;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Husk;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.PigZombie;
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
    // Controls the size of the ability (in blocks)
    private static final int RADIUS = 40;
    // Controls the mobs effected by the ability
    private final Set<Class<? extends Entity>> undeadMobTypes = Set.of(Zombie.class, ZombieVillager.class, Drowned.class, Husk.class, Skeleton.class, WitherSkeleton.class, Bogged.class, Stray.class, SkeletonHorse.class, ZombieHorse.class, Phantom.class, Wither.class, PigZombie.class, Zoglin.class);

    /**
     * Create an instance of the Banish Undead tome ability.
     *
     * @param plugin the host plugin object.
     */
    public BanishUndeadTomeAbility(RemakepirePlugin plugin) {
        super(plugin, "BanishUndead", new String[]{"All undead mobs within a " + RADIUS + " block radius of you die instantly."}, plugin.getConfigManager().getTomeBanishUndeadCooldown());
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

            // Search and kill all undead entities within a (RADIUS * 2)^3 cube
            for(Entity entity : player.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
                if (this.isUndeadMob(entity) && entity instanceof LivingEntity livingEntity) {
                    livingEntity.setHealth(0);
                    ++mobsKilled;
                }
            }

            // Display the visual effect of the ability
            this.createHolyLightRings(player);

            // Inform the player on the effect of their cast
            if (mobsKilled > 0) {
                player.playSound(player.getLocation(), "minecraft:block.beacon.power_select", 1.0F, 1.2F);
                this.sendSuccessMessage(player, "Holy light radiates from you, banishing " + mobsKilled + " undead creatures!");
            } else {
                player.playSound(player.getLocation(), "minecraft:block.beacon.ambient", 0.5F, 1.0F);
                this.sendSuccessMessage(player, "Holy light radiates from you, but no undead creatures were nearby.");
            }

            // Activate the ability cooldown
            return true;
        }
    }

    /**
     * Determines if an entity is inside our custom list of undead mobs.
     *
     * @param entity the entity being checked.
     * @return {@code true} if the {@code entity} is considered undead.
     */
    private boolean isUndeadMob(Entity entity) {
        for(Class<? extends Entity> mobType : this.undeadMobTypes) {
            if (mobType.isInstance(entity)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Creates a series of particle rings around the ability caster.
     *
     * @param player the player who cast the ability.
     */
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

    /**
     * Creates a ring of particles around the ability caster.
     *
     * @param center the center coordinates of the ring.
     * @param ringIndex which ring in the series this function will make.
     */
    private void createHolyLightRing(final Location center, int ringIndex) {
        final double baseRadius = 5 + ringIndex * 8.0;
        final int particleCount = 40 + ringIndex * 20;
        final double angleStep = (Math.PI * 2D) / (double)particleCount;

        (new BukkitRunnable() {
            double currentRadius = 0;
            final double maxRadius = baseRadius;
            final double radiusStep = this.maxRadius / 10.0;
            int tickCount = 0;

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
