package frostvein.sampires.remakepire.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
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

                    this.plugin.logInfo("Bat entity for player " + player.getName() + " was killed - triggering player death");
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
                    transformedPlayer.sendMessage(Component.text(" You have taken damage while in bat form, be careful...", NamedTextColor.RED));

                    final double health = bat.getHealth() - event.getFinalDamage();
                    final double maxHealth = bat.getAttribute(Attribute.MAX_HEALTH).getValue();
                    final double healthPercent = health / maxHealth * 100.0;
                    NamedTextColor healthColor;

                    // Color the health text based on how much remains
                    if (healthPercent > 60) {
                        healthColor = NamedTextColor.GREEN;
                    } else if (healthPercent > 30) {
                        healthColor = NamedTextColor.YELLOW;
                    } else {
                        healthColor = NamedTextColor.RED;
                    }

                    transformedPlayer.sendMessage(Component.text("Bat Health: ", NamedTextColor.GRAY)
                            .append(Component.text(String.format("%.1f", health), healthColor))
                            .append(Component.text("/", NamedTextColor.GRAY))
                            .append(Component.text(String.format("%.1f", maxHealth), NamedTextColor.WHITE))
                            .append(Component.text(" (", NamedTextColor.GRAY))
                            .append(Component.text(String.format("%.1f", healthPercent) + "%", healthColor))
                            .append(Component.text(")", NamedTextColor.GRAY))
                    );
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
                        transformedPlayer.sendMessage(Component.text(" You have taken damage while in bat form, be careful...", NamedTextColor.RED));

                        final double newHealth = bat.getHealth() - event.getFinalDamage(), maxHealth = bat.getAttribute(Attribute.MAX_HEALTH).getValue();

                        if (newHealth > 0) {
                            final double healthPercent = newHealth / maxHealth * 100.0;
                            NamedTextColor healthColor;

                            // Color the health text based on how much remains
                            if (healthPercent > 50) {
                                healthColor = NamedTextColor.GREEN;
                            } else if (healthPercent > 25) {
                                healthColor = NamedTextColor.YELLOW;
                            } else {
                                healthColor = NamedTextColor.RED;
                            }

                            transformedPlayer.sendMessage(Component.text("Remaining Health: ", NamedTextColor.GRAY)
                                    .append(Component.text(String.format("%.1f", newHealth), healthColor))
                                    .append(Component.text("/", NamedTextColor.GRAY))
                                    .append(Component.text(String.format("%.1f", maxHealth), NamedTextColor.GRAY))
                            );

                        } else {
                            transformedPlayer.sendMessage(Component.text("Your bat forms life force, and your own, are growing thin.", NamedTextColor.RED));
                        }
                    }
                }
            }
        }
    }

    /**
     * Ensure a player can still fly if they change game modes away from one with flight.
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
            player.sendMessage(Component.text("Your bat form moves with you...", NamedTextColor.GRAY));
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
            player.sendMessage(Component.text("You cannot use a fishing rod while in bat form.", NamedTextColor.RED));
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
                player.sendMessage(Component.text("You cannot shoot a bow while in bat form.", NamedTextColor.RED));
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
                    player.sendMessage(Component.text("You cannot use " + event.getItem().getType().name().toLowerCase().replace("_", " ") + " while in bat form.", NamedTextColor.RED));
            }
        }
    }
}
