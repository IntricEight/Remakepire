package frostvein.sampires.remakepire.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.abilities.BeaconTeleportAbility;
import frostvein.sampires.remakepire.beacons.BeaconSite;
import frostvein.sampires.remakepire.beacons.BeaconSite.BeaconState;
import frostvein.sampires.remakepire.managers.VampireAbilityManager;

public class BeaconTeleportListener implements Listener {
    private final RemakepirePlugin plugin;
    private final Map<UUID, ChannelingData> channelingPlayers = new HashMap<>();

    /**
     * Create an instance of the Beacon Teleport listener.
     *
     * @param plugin the host plugin object.
     */
    public BeaconTeleportListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Control interactions with the beacon destination UI.
     *
     * @param event a player clicks inside an inventory menu.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().title().equals(BeaconTeleportAbility.INVENTORY_GUI_TITLE)) {
            event.setCancelled(true);

            if (event.getWhoClicked() instanceof Player player) {
                ItemStack clickedItem = event.getCurrentItem();

                // Check that the player clicked a valid slot in the inventory GUI
                if (clickedItem != null && clickedItem.getType() == Material.BEACON) {
                    ItemMeta meta = clickedItem.getItemMeta();

                    if (meta != null && meta.customName() != null) {
                        // Remove the § code from the front of the beacon names
                        String beaconName = PlainTextComponentSerializer.plainText().serialize(meta.customName());
                        BeaconSite beacon = this.plugin.getBeaconManager().getBeacon(beaconName);

                        if (beacon == null) {
                            player.sendMessage(Component.text("Beacon not found: " + beaconName, NamedTextColor.RED));
                            player.closeInventory();

                        } else if (beacon.getState() != BeaconState.DESECRATED) {
                            player.sendMessage(Component.text("That beacon is no longer desecrated and cannot be used for beacon travel.", NamedTextColor.RED));
                            player.closeInventory();

                        } else if (!this.plugin.getVampireManager().isVampire(player)) {
                            player.sendMessage(Component.text("Only vampires can use beacon travel.", NamedTextColor.RED));
                            player.closeInventory();

                        } else {
                            BeaconSite suppressingBeacon = this.plugin.getBeaconManager().checkHolySuppression(player.getLocation());

                            if (suppressingBeacon != null) {
                                player.sendMessage(Component.text("The holy power from '" + suppressingBeacon.getName() + "' prevents beacon travel.", NamedTextColor.RED));
                                player.closeInventory();

                            } else {
                                player.closeInventory();
                                this.startChanneling(player, beacon);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Begin and process the beacon teleportation.
     *
     * @param player the vampire attempting to teleport.
     * @param beacon the destination beacon.
     */
    private void startChanneling(final Player player, BeaconSite beacon) {
        final UUID playerId = player.getUniqueId();
        this.cancelChanneling(playerId, false);

        Location startLocation = player.getLocation().clone();
        startLocation.setPitch(0.0F);
        startLocation.setYaw(0.0F);

        player.sendMessage(Component.text("Shadow Travel initiated...", NamedTextColor.DARK_PURPLE)
                .decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("Destination: ", NamedTextColor.GRAY)
                .append(Component.text(beacon.getName(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Do not move for 5 seconds.", NamedTextColor.RED)
                .decorate(TextDecoration.BOLD));
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 0.5F, 1.5F);

        BukkitTask channelingTask = (new BukkitRunnable() {
            int ticksElapsed = 0;
            final int totalTicks = 100;

            public void run() {
                ChannelingData data = BeaconTeleportListener.this.channelingPlayers.get(playerId);

                if (data == null) {
                    this.cancel();
                } else {
                    Location currentLoc = player.getLocation().clone();
                    currentLoc.setPitch(0.0F);
                    currentLoc.setYaw(0.0F);

                    if (currentLoc.distanceSquared(data.startLocation) > 0.01) {
                        BeaconTeleportListener.this.cancelChanneling(playerId, true);
                    } else {
                        ++this.ticksElapsed;

                        if (this.ticksElapsed % 20 == 0) {
                            int secondsRemaining = (totalTicks - this.ticksElapsed) / 20;

                            if (secondsRemaining > 0) {
                                player.sendMessage(Component.text("Channeling... ", NamedTextColor.GRAY)
                                        .append(Component.text(VampireAbilityManager.formatTime(secondsRemaining) + " remaining", NamedTextColor.YELLOW)));
                                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.3F, 1.0F + (float)secondsRemaining * 0.1F);
                            }
                        }

                        if (this.ticksElapsed % 4 == 0) {
                            Location particleLoc = player.getLocation().add(0.0, 1.0, 0.0);
                            player.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 2, 0.2, 0.5, 0.2, 0.1);
                            player.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 1, 0.3, 0.3, 0.3, 0.5);
                        }

                        if (this.ticksElapsed >= totalTicks) {
                            BeaconTeleportListener.this.completeChanneling(playerId);
                            this.cancel();
                        }
                    }
                }
            }
        }).runTaskTimer(this.plugin, 0L, 1L);

        this.channelingPlayers.put(playerId, new ChannelingData(startLocation, beacon, channelingTask));
    }

    /**
     * Cancel the beacon teleportation attempt.
     *
     * @param playerId the id of the vampire who just failed to teleport.
     * @param sendMessage {@code true} if the player should be informed with a message.
     */
    private void cancelChanneling(UUID playerId, boolean sendMessage) {
        ChannelingData data = this.channelingPlayers.remove(playerId);

        if (data != null) {
            data.channelingTask.cancel();

            if (sendMessage) {
                Player player = this.plugin.getServer().getPlayer(playerId);

                if (player != null) {
                    player.sendMessage(Component.text("Shadow Travel cancelled.", NamedTextColor.RED)
                            .decorate(TextDecoration.BOLD));
                    player.sendMessage(Component.text("You moved during channeling. Your cooldown has been reset.", NamedTextColor.GRAY));
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_HURT, 0.8F, 1.2F);

                    Location particleLoc = player.getLocation().add(0.0, 1.0, 0.0);
                    player.getWorld().spawnParticle(Particle.LARGE_SMOKE, particleLoc, 10, 0.3, 0.5, 0.3, 0.1);
                }
            }
        }
    }

    /**
     * Clear the player from the waiting conditions and execute the teleportation.
     *
     * @param playerId the id of the player teleporting.
     */
    private void completeChanneling(UUID playerId) {
        ChannelingData data = this.channelingPlayers.remove(playerId);

        if (data != null) {
            Player player = this.plugin.getServer().getPlayer(playerId);

            if (player != null) {
                this.performShadowTeleport(player, data.targetBeacon);
            }
        }
    }

    /**
     * Cancel the teleportation if a teleporting vampire moves.
     *
     * @param event a player moving.
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        final UUID playerId = player.getUniqueId();

        if (this.channelingPlayers.containsKey(playerId)) {
            final Location from = event.getFrom(), to = event.getTo();

            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                this.cancelChanneling(playerId, true);
            }
        }
    }

    /**
     * Cancel the teleportation if a teleporting vampire quits the game.
     *
     * @param event a player leaving the world.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.cancelChanneling(event.getPlayer().getUniqueId(), false);
    }

    /**
     * Perform the teleportation on the vampire.
     *
     * @param player a vampire attempting to teleport.
     * @param beacon the destination beacon.
     */
    private void performShadowTeleport(final Player player, final BeaconSite beacon) {
        Location destination = beacon.getLocation().clone();
        destination.add(0.5, 1.0, 0.5);
        destination = this.findSafeTeleportLocation(destination);

        player.sendMessage(Component.text("Shadow Travel initiated...", NamedTextColor.DARK_PURPLE)
                .decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("Destination: ", NamedTextColor.GRAY)
                .append(Component.text(beacon.getName(), NamedTextColor.WHITE)));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.5F);

        Location playerLoc = player.getLocation().add(0.0, 1.0, 0.0);
        player.getWorld().spawnParticle(Particle.PORTAL, playerLoc, 50, 0.5, 1.0, 0.5, 1.0);
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, playerLoc, 20, 0.3, 0.5, 0.3, 0.1);

        final Location finalDestination = destination;
        (new BukkitRunnable() {
            public void run() {
                if (player.teleport(finalDestination)) {
                    if (BeaconTeleportListener.this.plugin.getVampireAbilityManager().applyCooldownForAbility(player, "beacontravel")) {
                        BeaconTeleportListener.this.plugin.logInfo("Applied beacon travel cooldown for player: " + player.getName());
                    } else {
                        BeaconTeleportListener.this.plugin.getLogger().warning("Failed to apply beacon travel cooldown for player: " + player.getName());
                    }

                    player.sendMessage(Component.text("You emerge from the shadows at ", NamedTextColor.DARK_PURPLE)
                            .append(Component.text(beacon.getName(), NamedTextColor.WHITE))
                            .append(Component.text(".", NamedTextColor.DARK_PURPLE))
                    );
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.8F);

                    Location arrivalLoc = player.getLocation().add(0.0, 1.0, 0.0);
                    player.getWorld().spawnParticle(Particle.PORTAL, arrivalLoc, 30, 0.5, 1.0, 0.5, 0.8);
                    player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, arrivalLoc, 15, 0.4, 0.8, 0.4, 0.05);

                    plugin.logInfo("Player " + player.getName() + " used beacon travel to beacon: " + beacon.getName());

                } else {
                    player.sendMessage(Component.text("Beacon travel failed. The destination may be unsafe or blocked.", NamedTextColor.RED));
                    plugin.getLogger().warning("Beacon travel failed for " + player.getName() + " to beacon: " + beacon.getName());
                }
            }
        }).runTaskLater(this.plugin, 20L);
    }

    /**
     * Find a safe location for the vampire to be placed after teleporting.
     *
     * @param original the vampire's location before teleporting.
     * @return A location where the vampire can be placed safely, or their original location.
     */
    private Location findSafeTeleportLocation(Location original) {
        Location safe = original.clone();

        if (!this.isSafeLocation(safe)) {
            for (int y = -2; y <= 3; ++y) {
                for (int x = -2; x <= 2; ++x) {
                    for (int z = -2; z <= 2; ++z) {
                        Location test = original.clone().add(x, y, z);

                        if (this.isSafeLocation(test)) {
                            return test;
                        }
                    }
                }
            }

            this.plugin.getLogger().warning("Could not find safe teleport location near beacon at " + original.getBlockX() + ", " + original.getBlockY() + ", " + original.getBlockZ());
        }

        return safe;
    }

    /**
     * Test if a given location is safe for a player to be placed at.
     *
     * @param loc the potential teleport destination.
     * @return {@code true} if the location is safe for a player.
     */
    private boolean isSafeLocation(Location loc) {
        if (loc.getWorld() == null) {
            return false;

        } else {
            Material groundMaterial = loc.clone().subtract(0.0, 1.0, 0.0).getBlock().getType();
            Material feetMaterial = loc.getBlock().getType();
            Material headMaterial = loc.clone().add(0.0, 1.0, 0.0).getBlock().getType();

            boolean hasGround = groundMaterial.isSolid() && !groundMaterial.equals(Material.LAVA) && !groundMaterial.equals(Material.WATER);
            boolean feetClear = !feetMaterial.isSolid() || feetMaterial.equals(Material.WATER) || feetMaterial.equals(Material.LAVA);
            boolean headClear = !headMaterial.isSolid() || headMaterial.equals(Material.WATER) || headMaterial.equals(Material.LAVA);

            return hasGround && feetClear && headClear;
        }
    }

    private static class ChannelingData {
        final Location startLocation;
        final BeaconSite targetBeacon;
        final BukkitTask channelingTask;
        int secondsRemaining;

        /**
         * Create an instance of a beacon teleportation record.
         *
         * @param startLocation the players starting location.
         * @param targetBeacon the intended destination.
         * @param channelingTask the beacon teleportation task.
         */
        ChannelingData(Location startLocation, BeaconSite targetBeacon, BukkitTask channelingTask) {
            this.startLocation = startLocation;
            this.targetBeacon = targetBeacon;
            this.channelingTask = channelingTask;
            this.secondsRemaining = 5;
        }
    }
}
