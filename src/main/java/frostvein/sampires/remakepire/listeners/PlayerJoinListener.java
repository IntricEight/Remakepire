package frostvein.sampires.remakepire.listeners;

import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Team;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.abilities.tome.TurnUndeadTomeAbility;
import frostvein.sampires.remakepire.managers.BeetrootManager;
import frostvein.sampires.remakepire.managers.EffectManager;
import frostvein.sampires.remakepire.managers.VampireManager;

public class PlayerJoinListener implements Listener {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final EffectManager effectManager;
    private final BeetrootManager beetrootManager;

    public PlayerJoinListener(RemakepirePlugin plugin, VampireManager vampireManager, EffectManager effectManager) {
        this.plugin = plugin;
        this.vampireManager = vampireManager;
        this.effectManager = effectManager;
        this.beetrootManager = plugin.getBeetrootManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.addPlayerToCastTeam(player);
        TurnUndeadTomeAbility.cleanupHumanOnVampireCastTeam(this.plugin, player);
        this.vampireManager.initializeNewPlayer(player);
        this.vampireManager.ensureVampireTagConsistency(player);
        this.effectManager.applyJoinEffects(player);
        this.beetrootManager.restorePlayerState(player);
        this.plugin.getBeaconMajorityManager().applyBonusesToPlayer(player);
        if (this.plugin.getVampireTexturePackManager() != null && (this.vampireManager.isVampire(player) || player.getScoreboardTags().contains("CuredVampire"))) {
            this.plugin.getVampireTexturePackManager().onVampireLogin(player);
        }

        if (this.plugin.getBloodMoonAttributeListener() != null) {
            this.plugin.getBloodMoonAttributeListener().forceCleanupOnJoin(player);
        }

        String sessionStatus = this.getSessionStatusMessage();
        player.sendMessage("§7" + sessionStatus);
        if (!this.plugin.getSessionManager().playerReturningToGame(player)) {
            player.sendMessage("§cA new game has been initialized since you last played. Resetting your stats accordingly.");
            this.plugin.getSessionManager().resetPlayer(player);
            this.plugin.getSessionManager().getGameIDObjective().getScore(player.getName()).setScore(this.plugin.getSessionManager().getGameIDObjective().getScore("game_id_holder").getScore());
            this.plugin.getSessionManager().getSessionIDObjective().getScore(player.getName()).setScore(this.plugin.getSessionManager().getSessionIDObjective().getScore("session_id_holder").getScore());
            this.plugin.getVampireManager().clearPromotionBan(player);
        } else if (this.plugin.getSessionManager().playerReturningToSession(player)) {
            player.sendMessage("You have rejoined a session");
        } else {
            player.sendMessage("The previous session you were in has passed, resetting your stats accordingly");
            this.plugin.getSessionManager().resetPlayer(player);
            this.plugin.getSessionManager().getSessionIDObjective().getScore(player.getName()).setScore(this.plugin.getSessionManager().getSessionIDObjective().getScore("session_id_holder").getScore());
            this.plugin.getVampireManager().clearPromotionBan(player);
        }

        this.plugin.getPlayerChatManager().removePlayersPendingMessages(player);
        if (player.isOp() || player.hasPermission("vampiresmp.admin")) {
            List<String> warnings = this.plugin.getConfigManager().validateConfiguredLocations(this.plugin.getBeaconManager());
            if (!warnings.isEmpty()) {
                player.sendMessage("");
                player.sendMessage("§c§l[CONFIG WARNING] §eThe following locations are outside the border:");

                for(String warning : warnings) {
                    player.sendMessage("§c  - " + warning);
                }

                player.sendMessage("§7Border: X[" + (int)this.plugin.getConfigManager().getOakhurstMinX() + " to " + (int)this.plugin.getConfigManager().getOakhurstMaxX() + "] Z[" + (int)this.plugin.getConfigManager().getOakhurstMinZ() + " to " + (int)this.plugin.getConfigManager().getOakhurstMaxZ() + "]");
                player.sendMessage("§7Check config.yml oakhurst.border settings.");
                player.sendMessage("");
            }
        }

        event.setJoinMessage((String)null);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        for(Player potentialOp : this.plugin.getWorld().getPlayers()) {
            if (potentialOp.isOp()) {
                if (this.plugin.getSessionManager().isSessionActive()) {
                    potentialOp.sendMessage("Note: " + event.getPlayer().getName() + " has left during an active session. Consider pausing the session if this was unintended.");
                } else {
                    potentialOp.sendMessage("Note: " + event.getPlayer().getName() + " has left. Perhaps do not start/resume session until they return.");
                }
            }
        }

        this.plugin.getPlayerChatManager().removePlayersPendingMessages(player);
        this.plugin.getBeaconMajorityManager().removeBonusesFromPlayer(player);
        if (this.plugin.getVampireTexturePackManager() != null) {
            this.plugin.getVampireTexturePackManager().onPlayerQuit(player);
        }

        event.setQuitMessage((String)null);
    }

    private void addPlayerToCastTeam(Player player) {
        try {
            Team teamToJoin;
            if (this.plugin.getVampireManager().isIronAffected(player)) {
                teamToJoin = this.plugin.getVampireCastTeam();
            } else {
                teamToJoin = this.plugin.getCastTeam();
            }

            if (teamToJoin != null) {
                if (!teamToJoin.hasPlayer(player)) {
                    teamToJoin.addPlayer(player);
                    this.plugin.getLogger().info("Added player " + player.getName() + " to CastTeam");
                } else {
                    this.plugin.getLogger().info("Player " + player.getName() + " is already on CastTeam");
                }
            } else {
                this.plugin.getLogger().warning("CastTeam is null - cannot add player " + player.getName());
            }
        } catch (Exception e) {
            this.plugin.getLogger().severe("Failed to add player " + player.getName() + " to CastTeam: " + e.getMessage());
            e.printStackTrace();
        }

    }

    private String getSessionStatusMessage() {
        int sessionState = this.plugin.getSessionManager().getSessionState();
        switch (sessionState) {
            case 0 -> {
                return "The server is currently out of session. PvP, block breaking, and time are disabled.";
            }
            case 1 -> {
                return "A session is currently active! Be careful out there.";
            }
            case 2 -> {
                return "The session is currently paused. PvP, block breaking, and time are disabled.";
            }
            default -> {
                return "Session status unknown.";
            }
        }
    }
}
