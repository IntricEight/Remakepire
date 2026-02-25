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

    /**
     * Create an instance of the Bat Transformation listener.
     *
     * @param plugin the host plugin object.
     */
    public BatTransformationListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.batManager = plugin.getBatTransformationManager();
    }

    /**
     * Inform the bat manager that a player has joined the game.
     *
     * @param event a player joining the world.
     */
    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.batManager.handlePlayerJoin(player);
    }

    /**
     * Inform the bat manager that a player has left the game.
     *
     * @param event a player leaving the world.
     */
    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        this.batManager.handlePlayerQuit(player);
    }

    /**
     * Check if the player has died within their bat form.
     *
     * @param event an entity dying.
     */
    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Bat bat) {
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

    /**
     * Check if the player has been damaged within their bat form.
     *
     * @param event an entity being damaged by another entity.
     */
    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Bat bat) {
            if (bat.getCustomName() != null && bat.getCustomName().startsWith("§8")) {
                Player transformedPlayer = this.batManager.getPlayerFromBat(bat);

                if (transformedPlayer != null && transformedPlayer.isOnline()) {
                    transformedPlayer.sendMessage("§c You have taken damage while in bat form, be careful...");

                    double health = bat.getHealth() - event.getFinalDamage();
                    double maxHealth = bat.getMaxHealth();
                    double healthPercent = health / maxHealth * 100.0;

                    String healthColor;
                    if (healthPercent > 60) {
                        healthColor = "§a";
                    } else if (healthPercent > 30) {
                        healthColor = "§e";
                    } else {
                        healthColor = "§c";
                    }

                    transformedPlayer.sendMessage("§7Bat Health: " + healthColor + String.format("%.1f", health) + "§7/§f" + String.format("%.1f", maxHealth) + " §7(" + healthColor + String.format("%.1f", healthPercent) + "%§7)");
                }
            }
        }
    }

    /**
     * Check if the player has been damaged within their bat form.
     *
     * @param event an entity receives damage.
     */
    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onEntityDamage(EntityDamageEvent event) {
        if (!event.isCancelled()) {
            if (event.getEntity() instanceof Bat bat) {
                if (bat.getCustomName() != null && bat.getCustomName().startsWith("§8")) {
                    Player transformedPlayer = this.batManager.getPlayerFromBat(bat);

                    if (transformedPlayer != null && transformedPlayer.isOnline() && !(event instanceof EntityDamageByEntityEvent)) {
                        String damageType = event.getCause().name().toLowerCase().replace("_", " ");
                        transformedPlayer.sendMessage("§c You have taken damage while in bat form, be careful...");
                        double health = bat.getHealth() - event.getFinalDamage();
                        double maxHealth = bat.getMaxHealth();

                        if (health > 0) {
                            double healthPercent = health / maxHealth * 100.0;
                            String healthColor = healthPercent > 50 ? "§a" : (healthPercent > 25 ? "§e" : "§c");
                            transformedPlayer.sendMessage("§7Remaining Health: " + healthColor + String.format("%.1f", health) + "§7/" + String.format("%.1f", maxHealth));

                        } else {
                            transformedPlayer.sendMessage("§cYour bat forms life force, and your own, are growing thin.");
                        }
                    }
                }
            }
        }
    }

    /**
     * Check if a player is in bat form when they change game modes.
     *
     * @param event a player's game mode changing.
     */
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

    /**
     * Check if a player is in bat form when they teleport through either a command or plugin execution.
     *
     * @param event a player teleporting.
     */
    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        if (this.batManager.isInBatForm(player) && !event.isCancelled() && (event.getCause() == TeleportCause.COMMAND || event.getCause() == TeleportCause.PLUGIN)) {
            player.sendMessage("§7Your bat form moves with you...");
        }
    }

    /**
     * Prevent a player from fishing while in bat form.
     *
     * @param event a player attempting to fish.
     */
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

    /**
     * Prevent a player from using a bow while in bat form.
     *
     * @param event an entity uses a bow.
     */
    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (this.batManager.isInBatForm(player)) {
                event.setCancelled(true);
                player.sendMessage("§cYou cannot shoot a bow while in bat form.");
            }
        }
    }

    /**
     * Prevent the player from using a fishing rod, bow, or crossbow while in bat form.
     *
     * @param event a player interacts with an object.
     */
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
