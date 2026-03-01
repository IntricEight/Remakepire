package frostvein.sampires.remakepire.listeners;

import java.util.Collection;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.beacons.BeaconSite;

public class MovementBoundaryListener
implements Listener {
    private final RemakepirePlugin plugin;

    /**
     * Create an instance of the Movement Boundary border listener.
     *
     * @param plugin the host plugin object.
     */
    public MovementBoundaryListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Prevent players from leaving the border surrounding the game map until leaving conditions are met.
     *
     * @param event a player moving.
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();

        if (to == null) {
            return;
        }

        Location from = event.getFrom();

        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        double minX = this.plugin.getConfigManager().getOakhurstMinX();
        double maxX = this.plugin.getConfigManager().getOakhurstMaxX();
        double minZ = this.plugin.getConfigManager().getOakhurstMinZ();
        double maxZ = this.plugin.getConfigManager().getOakhurstMaxZ();

        double toX = to.getX(), toZ = to.getZ();
        double fromX = from.getX(), fromZ = from.getZ();
        boolean wasInsideBoundary = fromX >= minX && fromX <= maxX && fromZ >= minZ && fromZ <= maxZ;
        boolean isOutsideBoundary = toX < minX || toX > maxX || toZ < minZ || toZ > maxZ;
        boolean crossedBoundary = toX < minX || toX > maxX || toZ < minZ || toZ > maxZ;
        boolean canLeave = false;
        String leaveMessage = null;

        // TODO: At present, there is no feature that applies this tag, so we will be using commands. In a final release version of this, make sure to implement this tag's attachment automatically.
        if (player.getScoreboardTags().contains("killer")) {
            canLeave = true;
            leaveMessage = "§6You are leaving Frostvein...\n§eThe snowfall stops for you... You are free...";
        }

        if (canLeave && wasInsideBoundary && isOutsideBoundary) {
            if (!player.getScoreboardTags().contains("LeftOakhurst")) {
                player.addScoreboardTag("LeftOakhurst");

                if (leaveMessage != null) {
                    player.sendMessage(leaveMessage);
                }
            }

            return;
        }
        if (!canLeave && crossedBoundary) {
            event.setCancelled(true);

            if (!player.getScoreboardTags().contains("informed_boundary")) {
                player.addScoreboardTag("informed_boundary");
                String blockedMessage = !this.plugin.getVampireManager().isHuman(player) ? (this.areAllBeaconsDesecrated() ? "§4The blizzard still hungers more... Your job is not finished..." : "§cYou recoil at the harsh bite of the blizzard touching your skin... You feel a icy cold burn start to bloom through your body...") : (this.areAllBeaconsHoly() ? "§aThe blizzard still hungers more... Your job is not finished..." : "§cYou recoil at the harsh bite of the blizzard touching your skin... You feel a icy cold burn start to bloom through your body...");
                player.sendMessage(blockedMessage);
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

        if (beacons.isEmpty()) {
            return false;
        }

        for (BeaconSite beacon : beacons) {
            if (beacon.getState() == BeaconSite.BeaconState.DESECRATED || beacon.getState() == BeaconSite.BeaconState.PERMANENTLY_DESECRATED) continue;
            return false;
        }

        return true;
    }

    /**
     * Check if all beacons have been purified.
     *
     * @return {@code true} if all beacons are holy.
     */
    private boolean areAllBeaconsHoly() {
        Collection<BeaconSite> beacons = this.plugin.getBeaconManager().getAllBeacons();
        if (beacons.isEmpty()) {
            return false;
        }

        for (BeaconSite beacon : beacons) {
            if (beacon.getState() == BeaconSite.BeaconState.HOLY) continue;
            return false;
        }

        return true;
    }

    /**
     * Determine if all humans have been defeated.
     *
     * @return {@code true} if there are no survival-mode humans online.
     */
    private boolean anySurvivalModeHumansExist() {
        for (Player onlinePlayer : this.plugin.getServer().getOnlinePlayers()) {
            if (onlinePlayer.getGameMode() != GameMode.SURVIVAL || !this.plugin.getVampireManager().isHuman(onlinePlayer)) continue;
            return true;
        }

        return false;
    }

    /**
     * Determine if all vampires have been defeated.
     *
     * @return {@code true} if there are no survival-mode vampires online.
     */
    private boolean anySurvivalModeVampiresExist() {
        for (Player onlinePlayer : this.plugin.getServer().getOnlinePlayers()) {
            if (onlinePlayer.getGameMode() != GameMode.SURVIVAL || this.plugin.getVampireManager().isHuman(onlinePlayer)) continue;
            return true;
        }

        return false;
    }
}

