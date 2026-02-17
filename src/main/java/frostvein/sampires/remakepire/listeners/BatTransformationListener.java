package frostvein.sampires.remakepire.listeners;

import org.bukkit.entity.Bat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.BatTransformationManager;

public class BatTransformationListener implements Listener {
    private final RemakepirePlugin plugin;
    private final BatTransformationManager batManager;

    public BatTransformationListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.batManager = plugin.getBatTransformationManager();
    }

    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.batManager.handlePlayerJoin(player);
    }

    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        this.batManager.handlePlayerQuit(player);
    }

    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Bat) {
            Bat bat = (Bat)event.getEntity();
            if (bat.getCustomName() != null && bat.getCustomName().startsWith("§8")) {
                Player player = this.batManager.getPlayerFromBat(bat);
                if (player != null && player.isOnline() && this.batManager.isInBatForm(player)) {
                    this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                        if (player.isOnline()) {
                            this.batManager.handleBatDeath(player, null);
                        }

                    });
                    this.plugin.getLogger().info("Bat entity for player " + player.getName() + " was killed - triggering player death");
                }
            }

        }
    }

    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Bat) {
            Bat bat = (Bat)event.getEntity();
            if (bat.getCustomName() != null && bat.getCustomName().startsWith("§8")) {
                Player transformedPlayer = this.batManager.getPlayerFromBat(bat);
                if (transformedPlayer != null && transformedPlayer.isOnline()) {
                    transformedPlayer.sendMessage("§c You have taken damage while in bat form, be careful...");

                    double health = bat.getHealth() - event.getFinalDamage();
                    double maxHealth = bat.getMaxHealth();
                    double healthPercent = health / maxHealth * 100.0;

                    String healthColor;
                    if (healthPercent > 60.0) {
                        healthColor = "§a";
                    } else if (healthPercent > 30.0) {
                        healthColor = "§e";
                    } else {
                        healthColor = "§c";
                    }

                    transformedPlayer.sendMessage("§7Bat Health: " + healthColor + String.format("%.1f", health) + "§7/§f" + String.format("%.1f", maxHealth) + " §7(" + healthColor + String.format("%.1f", healthPercent) + "%§7)");
                }
            }

        }
    }

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onEntityDamage(EntityDamageEvent event) {
        if (!event.isCancelled()) {
            if (event.getEntity() instanceof Bat) {
                Bat bat = (Bat)event.getEntity();
                if (bat.getCustomName() != null && bat.getCustomName().startsWith("§8")) {
                    Player transformedPlayer = this.batManager.getPlayerFromBat(bat);
                    if (transformedPlayer != null && transformedPlayer.isOnline() && !(event instanceof EntityDamageByEntityEvent)) {
                        String damageType = event.getCause().name().toLowerCase().replace("_", " ");
                        transformedPlayer.sendMessage("§c You have taken damage while in bat form, be careful...");
                        double health = bat.getHealth() - event.getFinalDamage();
                        double maxHealth = bat.getMaxHealth();

                        if (health > 0.0) {
                            double healthPercent = health / maxHealth * 100.0;
                            String healthColor = healthPercent > 50.0 ? "§a" : (healthPercent > 25.0 ? "§e" : "§c");
                            transformedPlayer.sendMessage("§7Remaining Health: " + healthColor + String.format("%.1f", health) + "§7/" + String.format("%.1f", maxHealth));
                        } else {
                            transformedPlayer.sendMessage("§cYour bat forms life force, and your own, are growing thin.");
                        }
                    }
                }
            }
        }
    }

    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (this.batManager.isInBatForm(player)) {
            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                if (player.isOnline() && this.batManager.isInBatForm(player)) {
                    player.setAllowFlight(true);
                    player.setFlying(true);
                }
            }, 1L);
        }
    }

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (this.batManager.isInBatForm(player) && !event.isCancelled() && (event.getCause() == TeleportCause.COMMAND || event.getCause() == TeleportCause.PLUGIN)) {
            player.sendMessage("§7Your bat form moves with you...");
        }
    }

    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (this.batManager.isInBatForm(player)) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot use a fishing rod while in bat form.");
        }
    }

    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player)event.getEntity();
            if (this.batManager.isInBatForm(player)) {
                event.setCancelled(true);
                player.sendMessage("§cYou cannot shoot a bow while in bat form.");
            }
        }
    }

    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (this.batManager.isInBatForm(player) && event.getItem() != null) {
            switch (event.getItem().getType()) {
                case FISHING_ROD:
                case BOW:
                case CROSSBOW:
                    event.setCancelled(true);
                    player.sendMessage("§cYou cannot use " + event.getItem().getType().name().toLowerCase().replace("_", " ") + " while in bat form.");
            }
        }
    }
}
