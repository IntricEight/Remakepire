package frostvein.sampires.remakepire.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
import frostvein.sampires.remakepire.beacons.BeaconSite;
import frostvein.sampires.remakepire.beacons.BeaconSite.BeaconState;
import frostvein.sampires.remakepire.managers.VampireAbilityManager;

public class BeaconTeleportListener implements Listener {
    private final RemakepirePlugin plugin;
    private final Map<UUID, ChannelingData> channelingPlayers = new HashMap();

    public BeaconTeleportListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§4Desecrated Beacon Network")) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player)event.getWhoClicked();
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() == Material.BEACON) {
                    ItemMeta meta = clickedItem.getItemMeta();
                    if (meta != null && meta.getDisplayName() != null) {
                        String beaconName = meta.getDisplayName().replaceAll("§[0-9a-fklmnor]", "");
                        BeaconSite beacon = this.plugin.getBeaconManager().getBeacon(beaconName);
                        if (beacon == null) {
                            player.sendMessage("§cBeacon not found: " + beaconName);
                            player.closeInventory();
                        } else if (beacon.getState() != BeaconState.DESECRATED) {
                            player.sendMessage("§cThat beacon is no longer desecrated and cannot be used for beacon travel.");
                            player.closeInventory();
                        } else if (!this.plugin.getVampireManager().isVampire(player)) {
                            player.sendMessage("§cOnly vampires can use beacon travel.");
                            player.closeInventory();
                        } else {
                            BeaconSite suppressingBeacon = this.plugin.getBeaconManager().checkHolySuppression(player.getLocation());
                            if (suppressingBeacon != null) {
                                player.sendMessage("§cThe holy power from '" + suppressingBeacon.getName() + "' prevents beacon travel.");
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

    private void startChanneling(final Player player, BeaconSite beacon) {
        final UUID playerId = player.getUniqueId();
        this.cancelChanneling(playerId, false);
        Location startLocation = player.getLocation().clone();
        startLocation.setPitch(0.0F);
        startLocation.setYaw(0.0F);
        player.sendMessage("§5§lShadow Travel initiated...");
        player.sendMessage("§7Destination: §f" + beacon.getName());
        player.sendMessage("§c§lDo not move for 5 seconds.");
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 0.5F, 1.5F);
        BukkitTask channelingTask = (new BukkitRunnable() {
            int ticksElapsed = 0;
            final int totalTicks = 100;

            public void run() {
                ChannelingData data = (ChannelingData)BeaconTeleportListener.this.channelingPlayers.get(playerId);
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
                            int secondsRemaining = (100 - this.ticksElapsed) / 20;
                            if (secondsRemaining > 0) {
                                player.sendMessage("§7Channeling... §e" + VampireAbilityManager.formatTime((long)secondsRemaining) + " remaining");
                                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.3F, 1.0F + (float)secondsRemaining * 0.1F);
                            }
                        }

                        if (this.ticksElapsed % 4 == 0) {
                            Location particleLoc = player.getLocation().add(0.0, 1.0, 0.0);
                            player.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 2, 0.2, 0.5, 0.2, 0.1);
                            player.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 1, 0.3, 0.3, 0.3, 0.5);
                        }

                        if (this.ticksElapsed >= 100) {
                            BeaconTeleportListener.this.completeChanneling(playerId);
                            this.cancel();
                        }
                    }
                }
            }
        }).runTaskTimer(this.plugin, 0L, 1L);
        this.channelingPlayers.put(playerId, new ChannelingData(startLocation, beacon, channelingTask));
    }

    private void cancelChanneling(UUID playerId, boolean sendMessage) {
        ChannelingData data = (ChannelingData)this.channelingPlayers.remove(playerId);
        if (data != null) {
            data.channelingTask.cancel();
            if (sendMessage) {
                Player player = this.plugin.getServer().getPlayer(playerId);

                if (player != null) {
                    player.sendMessage("§c§lShadow Travel cancelled.");
                    player.sendMessage("§7You moved during channeling. Your cooldown has been reset.");
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_HURT, 0.8F, 1.2F);
                    Location particleLoc = player.getLocation().add(0.0, 1.0, 0.0);
                    player.getWorld().spawnParticle(Particle.LARGE_SMOKE, particleLoc, 10, 0.3, 0.5, 0.3, 0.1);
                }
            }
        }

    }

    private void completeChanneling(UUID playerId) {
        ChannelingData data = (ChannelingData)this.channelingPlayers.remove(playerId);
        if (data != null) {
            Player player = this.plugin.getServer().getPlayer(playerId);
            if (player != null) {
                this.performShadowTeleport(player, data.targetBeacon);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (this.channelingPlayers.containsKey(playerId)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
                this.cancelChanneling(playerId, true);
            }

        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        this.cancelChanneling(playerId, false);
    }

    private void performShadowTeleport(final Player player, final BeaconSite beacon) {
        Location destination = beacon.getLocation().clone();
        destination.add(0.5, 1.0, 0.5);
        destination = this.findSafeTeleportLocation(destination);

        player.sendMessage("§5§lShadow Travel initiated...");
        player.sendMessage("§7Destination: §f" + beacon.getName());
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.5F);

        Location playerLoc = player.getLocation().add(0.0, 1.0, 0.0);
        player.getWorld().spawnParticle(Particle.PORTAL, playerLoc, 50, 0.5, 1.0, 0.5, 1.0);
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, playerLoc, 20, 0.3, 0.5, 0.3, 0.1);

        final Location finalDestination = destination;
        (new BukkitRunnable() {
            public void run() {
                if (player.teleport(finalDestination)) {
                    if (BeaconTeleportListener.this.plugin.getVampireAbilityManager().applyCooldownForAbility(player, "beacontravel")) {
                        BeaconTeleportListener.this.plugin.getLogger().info("Applied beacon travel cooldown for player: " + player.getName());
                    } else {
                        BeaconTeleportListener.this.plugin.getLogger().warning("Failed to apply beacon travel cooldown for player: " + player.getName());
                    }

                    player.sendMessage("§5You emerge from the shadows at §f" + beacon.getName() + "§5.");
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.8F);

                    Location arrivalLoc = player.getLocation().add(0.0, 1.0, 0.0);
                    player.getWorld().spawnParticle(Particle.PORTAL, arrivalLoc, 30, 0.5, 1.0, 0.5, 0.8);
                    player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, arrivalLoc, 15, 0.4, 0.8, 0.4, 0.05);

                    plugin.getLogger().info("Player " + player.getName() + " used beacon travel to beacon: " + beacon.getName());

                } else {
                    player.sendMessage("§cBeacon travel failed. The destination may be unsafe or blocked.");
//                    BeaconTeleportListener.this.plugin.getLogger().warning("Beacon travel failed for " + player.getName() + " to beacon: " + beacon.getName());
                    plugin.getLogger().warning("Beacon travel failed for " + player.getName() + " to beacon: " + beacon.getName());
                }

            }
        }).runTaskLater(this.plugin, 20L);
    }

    private Location findSafeTeleportLocation(Location original) {
        Location safe = original.clone();
        if (this.isSafeLocation(safe)) {
            return safe;

        } else {
            for(int y = -2; y <= 3; ++y) {
                for(int x = -2; x <= 2; ++x) {
                    for(int z = -2; z <= 2; ++z) {
                        Location test = original.clone().add(x, y, z);
                        if (this.isSafeLocation(test)) {
                            return test;
                        }
                    }
                }
            }

            this.plugin.getLogger().warning("Could not find safe teleport location near beacon at " + original.getBlockX() + ", " + original.getBlockY() + ", " + original.getBlockZ());
            return safe;
        }
    }

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

        ChannelingData(Location startLocation, BeaconSite targetBeacon, BukkitTask channelingTask) {
            this.startLocation = startLocation;
            this.targetBeacon = targetBeacon;
            this.channelingTask = channelingTask;
            this.secondsRemaining = 5;
        }
    }
}
