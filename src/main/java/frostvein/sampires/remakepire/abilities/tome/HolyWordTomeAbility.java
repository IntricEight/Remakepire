package frostvein.sampires.remakepire.abilities.tome;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.VampireManager;

public class HolyWordTomeAbility extends TomeAbility implements Listener {
    private static final int RADIUS = 20;
    private static final int PARALYSIS_DURATION = 300;
    private final Map<UUID, BukkitTask> paralyzedPlayers = new HashMap();

    public HolyWordTomeAbility(RemakepirePlugin plugin) {
        super(plugin, "HolyWord", new String[]{"You speak a word of divine power,", "paralysing all vampires within a 20 block radius for 15 seconds", "(Vampires are impervious to all damage while frozen)."}, plugin.getConfigManager().getTomeHolyWordCooldown());
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    protected boolean useAbility(Player player) {
        if (!this.canUse(player)) {
            this.sendCannotUseMessage(player, "Only humans can use tome abilities!");
            return false;
        } else {
            VampireManager vampireManager = this.plugin.getVampireManager();
            List<Player> nearbyPlayers = player.getWorld().getPlayers();
            int stage1Affected = 0;
            int stage2And3Paralyzed = 0;

            for(Player target : nearbyPlayers) {
                if (!target.equals(player) && !(target.getLocation().distance(player.getLocation()) > (double)20.0F)) {
                    if (vampireManager.isVampireStage1(target)) {
                        target.sendMessage("§cA holy word sends your mind reeling, but you hold fast against it's paralysing effects.");
                        ++stage1Affected;
                    } else if (vampireManager.isVampireStage2(target) || vampireManager.isVampireStage3(target)) {
                        target.sendMessage("§cYou are frozen by divine power!");
                        target.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 300, 255, false, false));
                        UUID targetId = target.getUniqueId();
                        BukkitTask paralysisTask = Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.paralyzedPlayers.remove(targetId), 300L);
                        this.paralyzedPlayers.put(targetId, paralysisTask);
                        target.getWorld().playSound(target.getLocation(), "minecraft:entity.zombie_villager.cure", 0.8F, 2.0F);
                        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                            if (target.isOnline()) {
                                target.sendMessage("§7The divine paralysis fades... You can move again.");
                            }

                        }, 300L);
                        ++stage2And3Paralyzed;
                    }
                }
            }

            this.createHolyLightRings(player);
            player.getWorld().playSound(player.getLocation(), "minecraft:block.beacon.power_select", 1.0F, 1.0F);
            if (stage2And3Paralyzed > 0) {
                this.sendSuccessMessage(player, "You speak the HOLY WORD with divine authority!");
            } else {
                this.sendSuccessMessage(player, "You speak the HOLY WORD, but no known evil is around to hear it.");
            }

            return true;
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player)event.getEntity();
            UUID playerId = player.getUniqueId();
            if (this.paralyzedPlayers.containsKey(playerId)) {
                event.setCancelled(true);
            }

        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (this.paralyzedPlayers.containsKey(playerId) && (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getY() != event.getTo().getY() || event.getFrom().getZ() != event.getTo().getZ())) {
            event.setCancelled(true);
            if (System.currentTimeMillis() % 3000L < 50L) {
                player.sendMessage("§4You are frozen by divine power and cannot move!");
            }
        }

    }

    public boolean isParalyzed(Player player) {
        return this.paralyzedPlayers.containsKey(player.getUniqueId());
    }

    public void cleanup() {
        for(BukkitTask task : this.paralyzedPlayers.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }

        this.paralyzedPlayers.clear();
    }

    private void createHolyLightRings(Player player) {
        final Location center = player.getLocation().add((double)0.0F, (double)0.5F, (double)0.0F);
        long delayBetweenRings = 8L;
        (new BukkitRunnable() {
            int ringCount = 0;
            final int maxRings = 3;

            public void run() {
                if (this.ringCount >= 3) {
                    this.cancel();
                } else {
                    HolyWordTomeAbility.this.createHolyLightRing(center, this.ringCount);
                    ++this.ringCount;
                }
            }
        }).runTaskTimer(this.plugin, 0L, delayBetweenRings);
    }

    private void createHolyLightRing(final Location center, int ringIndex) {
        final double baseRadius = (double)5.0F + (double)ringIndex * (double)8.0F;
        final int particleCount = 40 + ringIndex * 20;
        final double angleStep = (Math.PI * 2D) / (double)particleCount;
        (new BukkitRunnable() {
            double currentRadius = (double)0.0F;
            final double maxRadius = baseRadius;
            final double radiusStep;
            int tickCount;

            {
                this.radiusStep = this.maxRadius / (double)10.0F;
                this.tickCount = 0;
            }

            public void run() {
                if (!(this.currentRadius >= this.maxRadius) && this.tickCount < 15) {
                    for(int i = 0; i < particleCount; ++i) {
                        double angle = (double)i * angleStep;
                        double x = center.getX() + this.currentRadius * Math.cos(angle);
                        double z = center.getZ() + this.currentRadius * Math.sin(angle);
                        double y = center.getY();
                        Location particleLocation = new Location(center.getWorld(), x, y, z);
                        center.getWorld().spawnParticle(Particle.END_ROD, particleLocation, 1, (double)0.0F, 0.2, (double)0.0F, 0.01);
                        if (i % 4 == 0) {
                            center.getWorld().spawnParticle(Particle.ENCHANT, particleLocation.add((double)0.0F, 0.3, (double)0.0F), 2, 0.1, 0.1, 0.1, 0.02);
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
