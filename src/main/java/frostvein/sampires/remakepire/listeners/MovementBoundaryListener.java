package frostvein.sampires.remakepire.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.beacons.BeaconSite;
import org.bukkit.util.Vector;

public class MovementBoundaryListener
implements Listener {
    private final RemakepirePlugin plugin;
    private final String TOWN_NAME;

    /**
     * Create an instance of the Movement Boundary border listener.
     *
     * @param plugin the host plugin object.
     */
    public MovementBoundaryListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.TOWN_NAME = plugin.getConfigManager().getTownName();
    }

    /**
     * Prevent players from leaving the border surrounding the game map until leaving conditions are met.<br/>
     * This triggers on movement from players controlling their character's movement directly as well as players controlling a mount's movement.
     *
     * @param event a player moving.
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Entity mount = player.getVehicle();

        Location to, from = event.getFrom();

        // Prevent excessive rubber-banding due to the player's location being slightly different from the mount's
        if (mount != null) {
            to = mount.getLocation();
        } else {
            to = event.getTo();
        }

        // Creative mode players can move uninhibited beyond the border
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        // End the event check early if the player has not moved from their previous position
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        // Determine the condition of the player's movement attempt regarding the border
        boolean wasInsideBoundary = this.isInsideBoundary(from), isOutsideBoundary = !this.isInsideBoundary(to);

        // End the event check early if the player is still inside the border
        if (!isOutsideBoundary) {
            return;
        }

        // Tune the freedom message based on the game's condition and player's alignment
        boolean canLeave = this.meetsLeaveCondition(player);

        // Send the leave message to the player if they have legally escaped beyond the border
        if (canLeave && wasInsideBoundary && isOutsideBoundary) {
            if (!player.getScoreboardTags().contains("LeftOakhurst")) {
                player.addScoreboardTag("LeftOakhurst");
                player.sendMessage(this.getLeaveSuccessMessage(player));
            }

            return;
        }

        // Prevent the player from passing the border, and inform them on why they are denied freedom
        if (!canLeave && isOutsideBoundary) {
            // Depending on whether the player is mounted or not, different methods must be used to stop their movement
            if (mount != null) {
                mount.teleport(from);
                mount.setVelocity(new Vector(0, 0, 0));
            } else {
                event.setCancelled(true);
            }

            this.informPlayerOnBlockedExit(player);
        }
    }

    /**
     * Prevent players from leaving the border surrounding the game map using vehicles until leaving conditions are met.<br/>
     * This triggers on movement from inside a vehicle like a minecart or a boat.
     *
     * @param event a vehicle (minecart, boat, etc.) moving.
     */
    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        Vehicle vehicle = event.getVehicle();

        // Don't check entities that have no passengers (Or cannot hold passengers)
        if (vehicle.getPassengers().isEmpty()) {
            return;
        }

        // Retrieve the entity's new and previous location
        Location to = event.getTo(), from = event.getFrom();

        // End the event check early if the vehicle has not moved from its previous position
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        // Determine the condition of the vehicle's movement attempt regarding the border
        boolean wasInsideBoundary = this.isInsideBoundary(from), isOutsideBoundary = !this.isInsideBoundary(to);

        // End the event check early if the vehicle is still inside the border
        if (!isOutsideBoundary) {
            return;
        }

        // Store checked players into two separate groups for messaging purposes
        List<Player> illegalPassengers = new ArrayList<>(), allowedPassengers = new ArrayList<>();

        // Only proceed if a player is in the vehicle
        for (Entity passenger : vehicle.getPassengers()) {
            // Determine if the passenger is a player who should be prevented from leaving the border
            if (passenger instanceof Player player) {
                // Creative mode players can move uninhibited beyond the border
                if (player.getGameMode() == GameMode.CREATIVE) continue;

                // Determine if this player can leave the border
                if (this.meetsLeaveCondition(player)) {
                    // Add the player to the list of allowed passengers
                    allowedPassengers.add(player);
                } else {
                    // Add the player to the list of prevented passengers
                    illegalPassengers.add(player);
                }
            }
        }

        // If there are any prevented passengers, stop the vehicle's progression
        if (!illegalPassengers.isEmpty() && isOutsideBoundary) {
            // Cannot cancel VehicleMoveEvent events, so we need to teleport them back to their previous position
            vehicle.teleport(from);
            vehicle.setVelocity(new Vector(0, 0, 0));

            // Let all blocked players know why they cannot leave
            for (Player passenger : illegalPassengers) {
                this.informPlayerOnBlockedExit(passenger);
            }

            // Give any players who can leave the border a message that they are held back by their forbidden companion
            for (Player passenger : allowedPassengers) {
                if (!passenger.getScoreboardTags().contains("informed_boundary_companion")) {
                    passenger.addScoreboardTag("informed_boundary_companion");

                    // Let the player know that while they can leave, they are prevented as long as their companion is in the same vehicle
                    passenger.sendMessage("§cWhilst freedom lies within your reach, you feel a force dragging your companion back... You cannot leave " + TOWN_NAME + " while one tethered to the beacons remains by you.");
                }
            }
        } else if (!allowedPassengers.isEmpty() && wasInsideBoundary && isOutsideBoundary) {
            // Send the leave message to the players if they have legally escaped beyond the border
            for (Player passenger : allowedPassengers) {
                if (!passenger.getScoreboardTags().contains("LeftOakhurst")) {
                    passenger.addScoreboardTag("LeftOakhurst");
                    passenger.sendMessage(this.getLeaveSuccessMessage(passenger));
                }
            }
        }
    }

    /**
     * Prevent players from leaving the border surrounding the game map using idle mounts until leaving conditions are met.<br/>
     * This triggers on movement done by mobs not being actively steered by a player. This function therefore prevents idling mobs from
     * taking the player outside the game map.
     *
     * @param event a non-player entity moving.
     */
    @EventHandler
    public void onEntityMove(EntityMoveEvent event) {
        Entity entity = event.getEntity();

        // Don't check entities that have no passengers (Or cannot hold passengers)
        if (entity.getPassengers().isEmpty()) {
            return;
        }

        // Retrieve the entity's new and previous location
        Location to = event.getTo(), from = event.getFrom();

        // End the event check early if the vehicle has not moved from its previous position
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        // Determine the condition of the entity's movement attempt regarding the border
        boolean wasInsideBoundary = this.isInsideBoundary(from), isOutsideBoundary = !this.isInsideBoundary(to);

        // End the event check early if the entity is still inside the border
        if (!isOutsideBoundary) {
            return;
        }

        // Store checked players into two separate groups for messaging purposes
        List<Player> illegalPassengers = new ArrayList<>(), allowedPassengers = new ArrayList<>();

        // Only proceed if a player is on the entity
        for (Entity passenger : entity.getPassengers()) {
            // Determine if the passenger is a player who should be prevented from leaving the border
            if (passenger instanceof Player player) {
                // Creative mode players can move uninhibited beyond the border
                if (player.getGameMode() == GameMode.CREATIVE) continue;

                // Determine if this player can leave the border
                if (this.meetsLeaveCondition(player)) {
                    // Add the player to the list of allowed passengers
                    allowedPassengers.add(player);
                } else {
                    // Add the player to the list of prevented passengers
                    illegalPassengers.add(player);
                }
            }
        }

        // If there are any prevented passengers, stop the entity's progression
        if (!illegalPassengers.isEmpty() && isOutsideBoundary) {
            event.setCancelled(true);

            // Let all blocked players know why they cannot leave
            for (Player passenger : illegalPassengers) {
                this.informPlayerOnBlockedExit(passenger);
            }

            // Give any players who can leave the border a message that they are held back by their forbidden companion
            for (Player passenger : allowedPassengers) {
                if (!passenger.getScoreboardTags().contains("informed_boundary_companion")) {
                    passenger.addScoreboardTag("informed_boundary_companion");

                    // Let the player know that while they can leave, they are prevented as long as their companion is in the same vehicle
                    passenger.sendMessage("§cWhilst freedom lies within your reach, you feel a force dragging your companion back... You cannot leave " + TOWN_NAME + " while one tethered to the beacons remains by you.");
                }
            }
        } else if (!allowedPassengers.isEmpty() && wasInsideBoundary && isOutsideBoundary) {
            // Send the leave message to the players if they have legally escaped beyond the border
            for (Player passenger : allowedPassengers) {
                if (!passenger.getScoreboardTags().contains("LeftOakhurst")) {
                    passenger.addScoreboardTag("LeftOakhurst");
                    passenger.sendMessage(this.getLeaveSuccessMessage(passenger));
                }
            }
        }
    }

    /**
     * Check if all beacons have been desecrated.
     *
     * @return {@code true} if all beacons are either desecrated or corrupted.
     */
    private boolean areAllBeaconsDesecrated() {
        Collection<BeaconSite> beacons = this.plugin.getBeaconManager().getAllBeacons();
        boolean skipCorrupted = !this.plugin.getConfigManager().doCorruptedBeaconsTrapHumans();
        int beaconsChecked = 0;

        if (beacons.isEmpty()) {
            return false;
        }

        for (BeaconSite beacon : beacons) {
            // If this beacon is corrupted, skip over it if the config setting doesn't want it counted
            if (skipCorrupted && beacon.getState() == BeaconSite.BeaconState.PERMANENTLY_DESECRATED) continue;

            // Count the number of non-corrupted beacons (if skipping) counted to ensure we don't falsely tag when all beacons are destroyed.
            ++beaconsChecked;

            if (beacon.getState() == BeaconSite.BeaconState.DESECRATED || beacon.getState() == BeaconSite.BeaconState.PERMANENTLY_DESECRATED) continue;
            return false;
        }

        return beaconsChecked > 0;
    }

    /**
     * Check if all beacons have been purified.
     *
     * @return {@code true} if all beacons are holy.
     */
    private boolean areAllBeaconsHoly() {
        Collection<BeaconSite> beacons = this.plugin.getBeaconManager().getAllBeacons();
        boolean skipCorrupted = !this.plugin.getConfigManager().doCorruptedBeaconsTrapHumans();
        int beaconsChecked = 0;

        if (beacons.isEmpty()) {
            return false;
        }

        for (BeaconSite beacon : beacons) {
            // If this beacon is corrupted, skip over it if the config setting doesn't want it counted
            if (skipCorrupted && beacon.getState() == BeaconSite.BeaconState.PERMANENTLY_DESECRATED) continue;

            // Count the number of non-corrupted beacons (if skipping) counted to ensure we don't falsely tag when all beacons are destroyed.
            ++beaconsChecked;

            if (beacon.getState() == BeaconSite.BeaconState.HOLY) continue;
            return false;
        }

        return beaconsChecked > 0;
    }

    /**
     * Determine if all humans have been defeated.
     * Checks if any human players are in survival or adventure mode.
     *
     * @return {@code true} if there are no survival-mode humans online.
     */
    private boolean anySurvivingHumansExist() {
        for (Player onlinePlayer : this.plugin.getServer().getOnlinePlayers()) {
            if (onlinePlayer.getGameMode() != GameMode.SURVIVAL || onlinePlayer.getGameMode() != GameMode.ADVENTURE || !this.plugin.getVampireManager().isHuman(onlinePlayer)) continue;
            return true;
        }

        return false;
    }

    /**
     * Determine if all vampires have been defeated.
     * Checks if any vampire players are in survival or adventure mode.
     *
     * @return {@code true} if there are no survival-mode vampires online.
     */
    private boolean anySurvivingVampiresExist() {
        for (Player onlinePlayer : this.plugin.getServer().getOnlinePlayers()) {
            if (onlinePlayer.getGameMode() != GameMode.SURVIVAL || onlinePlayer.getGameMode() != GameMode.ADVENTURE || this.plugin.getVampireManager().isHuman(onlinePlayer)) continue;
            return true;
        }

        return false;
    }

    /**
     * Determine if a location is inside the game's border boundaries.
     *
     * @param location the location to check.
     * @return {@code true} if the location is inside the border region.
     */
    private boolean isInsideBoundary(Location location) {
        // Retrieve the border values from the config
        double minX = this.plugin.getConfigManager().getBorderMinX(), minZ = this.plugin.getConfigManager().getBorderMinZ();
        double maxX = this.plugin.getConfigManager().getBorderMaxX(), maxZ = this.plugin.getConfigManager().getBorderMaxZ();

        // Retrieve the current horizontal coordinates of the location
        double locX = location.getX(), locZ = location.getZ();

        // Determine whether any of the coordinate points lie outside the border.
        return locX >= minX && locX <= maxX && locZ >= minZ && locZ <= maxZ;
    }

    /**
     * Inform the player on why they cannot leave through the border.
     *
     * @param player the player who is being prevented from leaving.
     */
    private void informPlayerOnBlockedExit(Player player) {
        // Only inform the player a single time on the border condition
        if (!player.getScoreboardTags().contains("informed_boundary")) {
            String blockedMessage;
            player.addScoreboardTag("informed_boundary");

            if (this.plugin.getVampireManager().isVampire(player) && this.areAllBeaconsDesecrated()) {
                blockedMessage = "§4But while humans remain... Hope still stands...";
            } else if (this.plugin.getVampireManager().isHuman(player) && this.areAllBeaconsHoly()) {
                blockedMessage = "§aBut while evil creatures still walk " + TOWN_NAME + ", your job is not yet finished...";
            } else {
                blockedMessage = "§cYou feel a force tying you to " + TOWN_NAME + "... You may not leave while an enemy's beacon remains... But one that has embraced darkness, and yet has found strength to return to the light... Could escape...";
            }

            player.sendMessage(blockedMessage);
        }
    }

    /**
     * Determine if the player has met the conditions necessary to leave beyond the border.
     *
     * @param player the player being checked.
     * @return {@code true} if the player can leave beyond the game boundaries.
     */
    private boolean meetsLeaveCondition(Player player) {
        // Determine if the player is allowed to leave the game boundaries
        // Each of the following is a leave condition
        if (player.getScoreboardTags().contains("CuredVampire")) {
            return true;

        } else if (!this.plugin.getVampireManager().isHuman(player)) {
            return this.areAllBeaconsDesecrated() && !this.anySurvivingHumansExist();

        } else if (this.areAllBeaconsHoly() && !this.anySurvivingVampiresExist()) {
            return true;
        }

        // If none of the above have been met, then the player should not be allowed to leave the game boundaries
        return false;
    }

    /**
     * Retrieve a status message for the player about the conditions of their successful escape beyond the border.<br/>
     * This function assumes that it will only be called if the player has been confirmed to be able to meet the leaving conditions.
     *
     * @param player the player being checked.
     * @return A message about how the player is leaving.
     */
    private String getLeaveSuccessMessage(Player player) {
        // Tune the freedom message based on the game's condition and player's alignment
        if (player.getScoreboardTags().contains("CuredVampire")) {
            return "§6You are leaving " + TOWN_NAME + "...\n§eThe familiar lands fade behind you as you venture beyond the border.";

        } else if (!this.plugin.getVampireManager().isHuman(player)) {
            if (this.areAllBeaconsDesecrated() && !this.anySurvivingHumansExist()) {
                return "§4You are free of your chains, creature of the night...";
            }
        } else if (this.areAllBeaconsHoly() && !this.anySurvivingVampiresExist()) {
            return "§aYou are free... Finally free...";
        }

        // Because this function is only meant to be used when the player is allowed to leave, this return statement should not fire
        return "§aDespite all odds, you have slipped beyond the beacon's grasp and escaped.";
    }



}

