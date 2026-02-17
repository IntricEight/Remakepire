package frostvein.sampires.remakepire.abilities.tome;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class ShoulderBargeTomeAbility extends TomeAbility {
    private static final int CHARGE_DURATION = 20;
    private static final double CHARGE_VELOCITY = (double)1.5F;
    private static final double UPWARD_VELOCITY = 0.3;
    private static final double KNOCKBACK_STRENGTH = 1.2;
    private static final int SLOWNESS_DURATION = 300;
    private static final double DAMAGE_TO_PLAYERS = (double)10.0F;
    private static final double DAMAGE_TO_MOBS = (double)20.0F;
    private final Map<UUID, BukkitTask> chargingPlayers = new HashMap();
    private final Map<UUID, Set<UUID>> chargeHitEntities = new HashMap();
    private final Map<UUID, Long> recentlyBargedEntities = new HashMap();
    private static final long TARGET_COOLDOWN_MS = 3000L;

    public ShoulderBargeTomeAbility(RemakepirePlugin plugin) {
        super(plugin, "ShoulderBarge", new String[]{"You learn to use your very body as a weapon.", "You charge forwards, and any entity that collides with you during this charge", "is knocked back and given slowness for 15 seconds."}, plugin.getConfigManager().getTomeShoulderBargeCooldown());
        Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupOldEntries, 600L, 600L);
    }

    protected boolean useAbility(final Player player) {
        if (!this.canUse(player)) {
            this.sendCannotUseMessage(player, "Only humans can use tome abilities!");
            return false;
        } else if (this.chargingPlayers.containsKey(player.getUniqueId())) {
            this.sendCannotUseMessage(player, "You are already charging!");
            return false;
        } else {
            Vector direction = player.getLocation().getDirection();
            direction.setY(Math.max(direction.getY(), 0.1));
            Vector chargeVelocity = direction.multiply((double)1.5F);
            chargeVelocity.setY(0.3);
            player.setVelocity(chargeVelocity);
            player.getWorld().playSound(player.getLocation(), "minecraft:entity.player.attack.crit", 0.8F, 1.2F);
            this.sendSuccessMessage(player, "You lower your shoulder and charge forward!");
            final UUID playerId = player.getUniqueId();
            synchronized(this.chargeHitEntities) {
                this.chargeHitEntities.put(playerId, new HashSet());
            }

            BukkitRunnable collisionTask = new BukkitRunnable() {
                int ticksRemaining = 20;

                public void run() {
                    if (this.ticksRemaining > 0 && player.isOnline() && ShoulderBargeTomeAbility.this.chargingPlayers.containsKey(playerId)) {
                        ShoulderBargeTomeAbility.this.checkForCollisions(player);
                        --this.ticksRemaining;
                    } else {
                        this.cancel();
                    }
                }
            };
            collisionTask.runTaskTimer(this.plugin, 0L, 1L);
            BukkitTask chargeTask = Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                this.chargingPlayers.remove(playerId);
                synchronized(this.chargeHitEntities) {
                    this.chargeHitEntities.remove(playerId);
                }

                collisionTask.cancel();
            }, 20L);
            this.chargingPlayers.put(playerId, chargeTask);
            return true;
        }
    }

    private void checkForCollisions(Player player) {
        UUID playerId = player.getUniqueId();
        Set<UUID> hitEntities;
        synchronized(this.chargeHitEntities) {
            hitEntities = (Set)this.chargeHitEntities.get(playerId);
        }

        if (hitEntities != null) {
            for(Entity entity : player.getNearbyEntities((double)1.5F, (double)2.0F, (double)1.5F)) {
                UUID entityId = entity.getUniqueId();
                synchronized(hitEntities) {
                    if (hitEntities.contains(entityId)) {
                        continue;
                    }
                }

                if (!entity.equals(player)) {
                    synchronized(this.recentlyBargedEntities) {
                        Long lastBargeTime = (Long)this.recentlyBargedEntities.get(entityId);
                        if (lastBargeTime != null && System.currentTimeMillis() - lastBargeTime < 3000L) {
                            continue;
                        }
                    }

                    this.handleCollision(player, entity);
                    synchronized(hitEntities) {
                        hitEntities.add(entityId);
                    }
                }
            }

        }
    }

    private void handleCollision(Player player, Entity target) {
        synchronized(this.recentlyBargedEntities) {
            this.recentlyBargedEntities.put(target.getUniqueId(), System.currentTimeMillis());
        }

        Vector knockbackDirection = target.getLocation().subtract(player.getLocation()).toVector();
        if (knockbackDirection.lengthSquared() == (double)0.0F) {
            knockbackDirection = player.getLocation().getDirection();
        }

        knockbackDirection = knockbackDirection.normalize();
        knockbackDirection.setY(Math.max(knockbackDirection.getY(), 0.2));
        Vector knockback = knockbackDirection.multiply(1.2);
        target.setVelocity(knockback);
        if (target instanceof LivingEntity livingTarget) {
            double damageAmount;
            if (target instanceof Player) {
                damageAmount = (double)10.0F;
            } else {
                damageAmount = (double)20.0F;
            }

            livingTarget.damage(damageAmount, player);
            livingTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 300, 1, false, false));
        }

        player.getWorld().playSound(player.getLocation(), "minecraft:entity.player.attack.knockback", 1.0F, 0.8F);
        player.getWorld().playSound(target.getLocation(), "minecraft:entity.generic.hurt", 0.8F, 1.1F);
        String var10001 = this.getEntityName(target);
        player.sendMessage("§aYou barrel into " + var10001 + ".");
        if (target instanceof Player) {
            ((Player)target).sendMessage("§c" + player.getName() + " charges into you with a shoulder barge.");
        }

    }

    private String getEntityName(Entity entity) {
        return entity instanceof Player ? ((Player)entity).getName() : entity.getType().name().toLowerCase().replace("_", " ");
    }

    private void cleanupOldEntries() {
        long currentTime = System.currentTimeMillis();
        synchronized(this.recentlyBargedEntities) {
            this.recentlyBargedEntities.entrySet().removeIf((entry) -> currentTime - (Long)entry.getValue() > 3000L);
        }
    }

    public void cleanup() {
        for(BukkitTask task : this.chargingPlayers.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }

        this.chargingPlayers.clear();
        synchronized(this.recentlyBargedEntities) {
            this.recentlyBargedEntities.clear();
        }
    }
}
