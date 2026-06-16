package frostvein.sampires.remakepire.abilities.tome;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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
    // Controls the duration of the ability (in ticks)
    private static final int CHARGE_DURATION = 20;
    // Controls how quickly the player moves forward.
    private static final double CHARGE_VELOCITY = 1.5;
    // Controls how quickly the player moves upward
    private static final double UPWARD_VELOCITY = 0.3;
    // Controls how strong the impact knockback effect is
    private static final double KNOCKBACK_STRENGTH = 1.2;
    // Controls the duration of the ability's effects (in ticks)
    private static final int SLOWNESS_DURATION = 300;
    // Controls the intensity of the ability's effects
    private static final int SLOWNESS_AMPLIFIER = 1;
    // Controls how much damage the ability does to players.
    private static final double DAMAGE_TO_PLAYERS = 10.0;
    // Controls how much damage the ability does to non-player entities.
    private static final double DAMAGE_TO_MOBS = 20.0;
    // Controls how frequently a target can be barged into (in ticks)
    private static final long TARGET_COOLDOWN_MS = 3000L;
    private final Map<UUID, BukkitTask> chargingPlayers = new HashMap<>();
    private final Map<UUID, Set<UUID>> chargeHitEntities = new HashMap<>();
    private final Map<UUID, Long> recentlyBargedEntities = new HashMap<>();

    /**
     * Create an instance of the Shoulder Barge tome ability.
     *
     * @param plugin the host plugin object.
     */
    public ShoulderBargeTomeAbility(RemakepirePlugin plugin) {
        super(plugin, "ShoulderBarge", new String[]{"You learn to use your very body as a weapon.", "You charge forwards, and any entity that collides with you during this charge", "is knocked back and given slowness for " + (SLOWNESS_DURATION / 20) + " seconds."}, plugin.getConfigManager().getTomeShoulderBargeCooldown());
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
            Vector chargeVelocity = direction.multiply(CHARGE_VELOCITY);
            chargeVelocity.setY(UPWARD_VELOCITY);

            player.setVelocity(chargeVelocity);
            player.getWorld().playSound(player.getLocation(), "minecraft:entity.player.attack.crit", 0.8F, 1.2F);
            this.sendSuccessMessage(player, "You lower your shoulder and charge forward!");

            this.chargeHitEntities.put(player.getUniqueId(), new HashSet<>());

            BukkitRunnable collisionTask = new BukkitRunnable() {
                int ticksRemaining = CHARGE_DURATION;

                public void run() {
                    if (this.ticksRemaining > 0 && player.isOnline() && ShoulderBargeTomeAbility.this.chargingPlayers.containsKey(player.getUniqueId())) {
                        ShoulderBargeTomeAbility.this.checkForCollisions(player);
                        --this.ticksRemaining;
                    } else {
                        this.cancel();
                    }
                }
            };

            collisionTask.runTaskTimer(this.plugin, 0L, 1L);

            BukkitTask chargeTask = Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                this.chargingPlayers.remove(player.getUniqueId());

                // Remove the caster from the list of hit entities
                this.chargeHitEntities.remove(player.getUniqueId());

                collisionTask.cancel();
            }, CHARGE_DURATION);

            this.chargingPlayers.put(player.getUniqueId(), chargeTask);
            return true;
        }
    }

    /**
     * Check if the ability hit any targets.
     *
     * @param player the player who cast the ability.
     */
    private void checkForCollisions(Player player) {
        UUID playerId = player.getUniqueId();
        Set<UUID> hitEntities = this.chargeHitEntities.get(playerId);

        if (hitEntities != null) {
            for(Entity entity : player.getNearbyEntities(1.5, 2.0, 1.5)) {
                UUID entityId = entity.getUniqueId();

                // Prevent item entities from being hit by the charge
                if (!(entity instanceof LivingEntity)) {
                    continue;
                }

                if (entity instanceof Player target) {
                    // Prevent players in Spectator mode from being hit
                    if (target.getGameMode() == GameMode.SPECTATOR) {
                        continue;
                    }
                }

                // Skip over entities that have just been hit by another charge
                if (hitEntities.contains(entityId)) {
                    continue;
                }

                // Skip over the charging player
                if (entity.equals(player)) {
                    continue;
                }

                Long lastBargeTime = this.recentlyBargedEntities.get(entityId);

                if (lastBargeTime != null && System.currentTimeMillis() - lastBargeTime < TARGET_COOLDOWN_MS) {
                    continue;
                }

                this.handleCollision(player, entity);
                hitEntities.add(entityId);
            }
        }
    }

    /**
     * Apply the physics and effects of the ability collision to a target.
     *
     * @param player the player who cast the ability.
     * @param target a player who was hit by the ability.
     */
    private void handleCollision(Player player, Entity target) {
        this.recentlyBargedEntities.put(target.getUniqueId(), System.currentTimeMillis());

        Vector knockbackDirection = target.getLocation().subtract(player.getLocation()).toVector();

        if (knockbackDirection.lengthSquared() == 0) {
            knockbackDirection = player.getLocation().getDirection();
        }

        knockbackDirection = knockbackDirection.normalize();
        knockbackDirection.setY(Math.max(knockbackDirection.getY(), 0.2));
        final Vector knockback = knockbackDirection.multiply(KNOCKBACK_STRENGTH);
        target.setVelocity(knockback);

        if (target instanceof LivingEntity livingTarget) {
            double damageAmount;

            if (target instanceof Player) {
                damageAmount = DAMAGE_TO_PLAYERS;
            } else {
                damageAmount = DAMAGE_TO_MOBS;
            }

            livingTarget.damage(damageAmount, player);
            livingTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, SLOWNESS_DURATION, SLOWNESS_AMPLIFIER, false, false));
        }

        player.getWorld().playSound(player.getLocation(), "minecraft:entity.player.attack.knockback", 1.0F, 0.8F);
        player.getWorld().playSound(target.getLocation(), "minecraft:entity.generic.hurt", 0.8F, 1.1F);
        player.sendMessage("§aYou barrel into " + this.getEntityName(target) + ".");

        if (target instanceof Player) {
            target.sendMessage("§c" + player.getName() + " charges into you with a shoulder barge.");
        }
    }

    /**
     * Retrieve the name of the entity's player name or entity type.
     *
     * @param entity the entity hit by the ability.
     * @return The {@code String} of a player's name or an entity type,
     */
    private String getEntityName(Entity entity) {
        return entity instanceof Player ? entity.getName() : entity.getType().name().toLowerCase().replace("_", " ");
    }

    /**
     * Clean up any expired instances of barging effects.
     */
    private void cleanupOldEntries() {
        final long currentTime = System.currentTimeMillis();
        this.recentlyBargedEntities.entrySet().removeIf((entry) -> currentTime - entry.getValue() > TARGET_COOLDOWN_MS);
    }

    /**
     * Clean up any instances of barging effects.
     */
    public void cleanup() {
        for(BukkitTask task : this.chargingPlayers.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }

        this.chargingPlayers.clear();
        this.recentlyBargedEntities.clear();
    }
}
