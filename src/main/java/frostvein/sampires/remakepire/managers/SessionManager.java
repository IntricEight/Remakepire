package frostvein.sampires.remakepire.managers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class SessionManager {
    private final RemakepirePlugin plugin;
    private Objective sessionObjective;
    private Objective sessionIDObjective;
    private Objective gameIDObjective;
    private final Map<UUID, Integer> pausedFoodLevels = new HashMap();
    private final Map<UUID, Float> pausedSaturationLevels = new HashMap();
    public static final int BEFORE_SESSION = 0;
    public static final int IN_SESSION = 1;
    public static final int PAUSED = 2;
    public static final int AFTER_SESSION = 3;
    public static final int PRE_SESSION = 4;
    private long totalSessionTime = 0L;
    private long currentPhaseStartTime = 0L;
    private boolean trackingSessionTime = false;
    public static final String INFORMED_IRON_BLOCK_REPEL = "informed_iron_block_reply";
    public static final String INFORMED_CRAFTING_ITEMS = "informed_crafting_items";
    public static final String INFORMED_PICKUP_ITEM = "informed_pickup_item";
    public static final String INFORMED_PICKUP_HOLY_WATER = "informed_pickup_holy_water";
    public static final String INFORMED_USE_HOLY_WATER = "informed_use_holy_water";
    public static final String INFORMED_IRON_BLOCK_WEAKNESS = "informed_iron_block_effects";
    public static final String INFORMED_SUCCESSFUL_FEEDING = "informed_successful_feeding";
    public static final String INFORMED_BLOOD_MOON = "informed_blood_moon";
    public static final String INFORMED_ENCHANTING_ITEMS = "informed_enchanting_items";
    public static final String INFORMED_WEAPON_WEAKNESS = "informed_weapon_weakness";
    public static final String INFORMED_VAMPIRE_CLAWS = "informed_vampire_claws";
    public static final String INFORMED_BOUNDARY = "informed_boundary";
    public static final String STOPTHEBLEEDING_USED_SESSION = "stopthebleeding_used_session";
    public static final String BLESSING_USED_SESSION = "blessing_used_session";
    public static final List<String> INFORMED_CONSTANTS = Arrays.asList("informed_iron_block_reply", "informed_crafting_items", "informed_pickup_item", "informed_pickup_holy_water", "informed_use_holy_water", "informed_iron_block_effects", "informed_successful_feeding", "informed_blood_moon", "informed_enchanting_items", "informed_weapon_weakness", "informed_vampire_claws", "informed_boundary", "stopthebleeding_used_session", "blessing_used_session");

    public SessionManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    public void startBackgroundTasks() {
        this.startSaturationTask();
        this.startActionBarTask();
    }

    public void executeServerCommand(String command) {
        try {
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                this.plugin.getLogger().info("Executed command: /" + command);
            });
        } catch (Exception e) {
            this.plugin.getLogger().warning("Failed to execute command: /" + command + " - " + e.getMessage());
        }

    }

    private void freezeTick() {
        this.executeServerCommand("tick freeze");
    }

    private void unfreezeTick() {
        this.executeServerCommand("tick unfreeze");
    }

    private void startSaturationTask() {
        (new BukkitRunnable() {
            public void run() {
                int sessionState = SessionManager.this.getSessionState();
                if (sessionState == 2) {
                    SessionManager.this.restorePausedFoodLevels();
                } else if (SessionManager.this.isOutOfSession()) {
                    SessionManager.this.applySaturationToAllPlayers();
                    SessionManager.this.setAllPlayersMaxFood();
                } else if (sessionState == 4) {
                    SessionManager.this.plugin.getLogger().info("PRE_SESSION: Setting all players to max food and saturation");
                    SessionManager.this.setAllPlayersMaxFood();
                }

            }
        }).runTaskTimer(this.plugin, 0L, 80L);
    }

    private void startActionBarTask() {
        (new BukkitRunnable() {
            public void run() {
                if (SessionManager.this.isOutOfSession() || SessionManager.this.isPreSession()) {
                    SessionManager.this.updateActionBarForAllPlayers();
                }

            }
        }).runTaskTimer(this.plugin, 0L, 20L);
    }

    private void updateActionBarForAllPlayers() {
        String message = this.getSessionStatusMessage();

        for(Player player : Bukkit.getOnlinePlayers()) {
            this.sendActionBar(player, message);
        }

    }

    public void sendActionBar(Player player, String message) {
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        } catch (Exception var6) {
            try {
                player.sendTitle("", message, 0, 25, 5);
            } catch (Exception var5) {
                player.sendMessage("§8[§6Session§8] " + message);
            }
        }

    }

    private String getSessionStatusMessage() {
        int sessionState = this.getSessionState();
        switch (sessionState) {
            case 0:
                return "§e§lSession is primed and ready to start";
            case 1:
            default:
                return "§7§lSession status unknown";
            case 2:
                return "§6§lSession is currently paused";
            case 3:
                return "§c§lSession has ended";
            case 4:
                return "§b§lBuilding Mode - interactions enabled";
        }
    }

    private void applySaturationToAllPlayers() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 100, 100, false, false));
        }

    }

    private void setAllPlayersMaxFood() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            player.setFoodLevel(20);
            player.setSaturation(20.0F);
        }

    }

    private void capturePausedFoodLevels() {
        this.pausedFoodLevels.clear();
        this.pausedSaturationLevels.clear();

        for(Player player : Bukkit.getOnlinePlayers()) {
            this.pausedFoodLevels.put(player.getUniqueId(), player.getFoodLevel());
            this.pausedSaturationLevels.put(player.getUniqueId(), player.getSaturation());
        }

        this.plugin.getLogger().info("Captured food levels for " + this.pausedFoodLevels.size() + " players when session was paused");
    }

    private void restorePausedFoodLevels() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            if (this.pausedFoodLevels.containsKey(playerId)) {
                int pausedFood = (Integer)this.pausedFoodLevels.get(playerId);
                float pausedSaturation = (Float)this.pausedSaturationLevels.get(playerId);
                player.setFoodLevel(pausedFood);
                player.setSaturation(pausedSaturation);
            }
        }

    }

    public Objective getSessionObjective() {
        return this.sessionObjective;
    }

    public Objective getSessionIDObjective() {
        return this.sessionIDObjective;
    }

    public Objective getGameIDObjective() {
        return this.gameIDObjective;
    }

    public boolean isFirstBeaconConvertedTriggered() {
        return this.plugin.getConfig().getBoolean("first_beacon_converted", false);
    }

    public void setFirstBeaconConvertedTriggered(boolean triggered) {
        this.plugin.getConfig().set("first_beacon_converted", triggered);
        this.plugin.saveConfig();
    }

    public boolean areHumansOwningAllBeacons() {
        return this.plugin.getConfig().getBoolean("humans_own_all_beacons", false);
    }

    public void setHumansOwningAllBeacons(boolean active) {
        this.plugin.getConfig().set("humans_own_all_beacons", active);
        this.plugin.saveConfig();
    }

    public boolean areVampiresOwningAllBeacons() {
        return this.plugin.getConfig().getBoolean("vampires_own_all_beacons", false);
    }

    public void setVampiresOwningAllBeacons(boolean active) {
        this.plugin.getConfig().set("vampires_own_all_beacons", active);
        this.plugin.saveConfig();
    }

    public boolean isHumansFinalStandActive() {
        return this.areHumansOwningAllBeacons();
    }

    public void setHumansFinalStandActive(boolean active) {
        this.setHumansOwningAllBeacons(active);
    }

    public boolean isOneHumanLeftActive() {
        return this.plugin.getConfig().getBoolean("one_human_left", false);
    }

    public void setOneHumanLeftActive(boolean active) {
        this.plugin.getConfig().set("one_human_left", active);
        this.plugin.saveConfig();
    }

    public boolean isVampiresEternalNightActive() {
        return this.areVampiresOwningAllBeacons();
    }

    public void setVampiresEternalNightActive(boolean active) {
        this.setVampiresOwningAllBeacons(active);
    }

    public int getVampireHealthCheckTicks() {
        return this.plugin.getConfig().getInt("vampire_health_check_ticks", 9);
    }

    public void setVampireHealthCheckTicks(int ticks) {
        this.plugin.getConfig().set("vampire_health_check_ticks", Math.max(1, ticks));
        this.plugin.saveConfig();
    }

    public void incrementSessionID() {
        this.sessionIDObjective.getScore("session_id_holder").setScore(this.sessionIDObjective.getScore("session_id_holder").getScore() + 1);
        this.updateAllPlayersSessionIDs();
    }

    public void incrementGameID() {
        this.gameIDObjective.getScore("game_id_holder").setScore(this.gameIDObjective.getScore("game_id_holder").getScore() + 1);
        this.updateAllPlayersGameIDs();
        this.plugin.getLogger().info("Game ID incremented to: " + this.gameIDObjective.getScore("game_id_holder").getScore());
    }

    public void initializeScoreboard() {
        Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        this.sessionObjective = mainScoreboard.getObjective("smp_session");
        if (this.sessionObjective == null) {
            this.sessionObjective = mainScoreboard.registerNewObjective("smp_session", "dummy", "SMP Session State");
            Score sessionScore = this.sessionObjective.getScore("state");
            sessionScore.setScore(0);
        }

        this.sessionIDObjective = mainScoreboard.getObjective("vsmp_session_id");
        if (this.sessionIDObjective == null) {
            this.sessionIDObjective = mainScoreboard.registerNewObjective("vsmp_session_id", "dummy");
            this.sessionIDObjective.getScore("session_id_holder").setScore(1);
        }

        this.gameIDObjective = mainScoreboard.getObjective("vsmp_game_id");
        if (this.gameIDObjective == null) {
            this.gameIDObjective = mainScoreboard.registerNewObjective("vsmp_game_id", "dummy");
            this.gameIDObjective.getScore("game_id_holder").setScore(1);
        }

        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            this.setOutOfSessionRules();
            this.plugin.getLogger().info("Server initialized in OUT OF SESSION mode - PvP, block breaking, and time cycles disabled.");
        }, 20L);
    }

    public boolean playerReturningToSession(Player player) {
        return this.sessionIDObjective.getScore(player.getName()).getScore() == this.sessionIDObjective.getScore("session_id_holder").getScore();
    }

    public boolean playerReturningToGame(Player player) {
        return this.gameIDObjective.getScore(player.getName()).getScore() == this.gameIDObjective.getScore("game_id_holder").getScore();
    }

    public long getSessionTime() {
        return this.trackingSessionTime && this.isSessionActive() ? this.totalSessionTime + (System.currentTimeMillis() - this.currentPhaseStartTime) : this.totalSessionTime;
    }

    public long getSessionTimeSeconds() {
        return this.getSessionTime() / 1000L;
    }

    private void startTrackingSessionTime() {
        if (!this.trackingSessionTime) {
            this.currentPhaseStartTime = System.currentTimeMillis();
            this.trackingSessionTime = true;
            this.plugin.getLogger().info("Started tracking session time. Total session time: " + this.totalSessionTime / 1000L + " seconds");
        }

    }

    private void stopTrackingSessionTime() {
        if (this.trackingSessionTime) {
            long phaseTime = System.currentTimeMillis() - this.currentPhaseStartTime;
            this.totalSessionTime += phaseTime;
            this.trackingSessionTime = false;
            this.plugin.getLogger().info("Stopped tracking session time. Added " + phaseTime / 1000L + " seconds. Total session time: " + this.totalSessionTime / 1000L + " seconds");
        }

    }

    public void updateAllPlayersSessionIDs() {
        int session_id = this.sessionIDObjective.getScore("session_id_holder").getScore();

        for(Player player : this.plugin.getWorld().getPlayers()) {
            this.sessionIDObjective.getScore(player.getName()).setScore(session_id);
        }

    }

    public void updateAllPlayersGameIDs() {
        int game_id = this.gameIDObjective.getScore("game_id_holder").getScore();

        for(Player player : this.plugin.getWorld().getPlayers()) {
            this.gameIDObjective.getScore(player.getName()).setScore(game_id);
        }

        this.plugin.getLogger().info("Updated game IDs for all online players to: " + game_id);
    }

    public void startSession() {
        Score sessionScore = this.sessionObjective.getScore("state");
        sessionScore.setScore(1);
        this.setInSessionRules();
        this.resetPlayers();
        this.incrementSessionID();
        this.plugin.getVampireManager().clearAllPromotionBans();
        this.plugin.getVampireManager().clearAllStageCaps();
        this.plugin.getVampireFeedingManager().resetSessionFeedingThirst();
        this.plugin.getVampireAbilityManager().clearAllCooldownsForNewSession();
        this.plugin.getBeaconManager().clearAllBeaconCooldownsForNewSession();
        if (this.plugin.getVampireTurningManager() != null) {
            this.plugin.getVampireTurningManager().enableAllVampireTurning();
        }

        this.startTrackingSessionTime();
        this.unfreezeTick();
        this.plugin.getBeaconMajorityManager().updateBeaconMajorityBonuses();
        this.broadcastMessage("§a§lSESSION STARTED! §aThe SMP session has begun. Good luck!");
    }

    public void resumeSession() {
        Score sessionScore = this.sessionObjective.getScore("state");
        sessionScore.setScore(1);
        this.setInSessionRules();
        this.startTrackingSessionTime();
        this.unfreezeTick();
        this.plugin.getBeaconMajorityManager().updateBeaconMajorityBonuses();
        this.broadcastMessage("§a§lSESSION RESUMED! §aThe SMP session has been resumed.");
    }

    public void pauseSession() {
        this.capturePausedFoodLevels();
        this.stopTrackingSessionTime();
        Score sessionScore = this.sessionObjective.getScore("state");
        sessionScore.setScore(2);
        this.setOutOfSessionRules();
        this.freezeTick();
        this.broadcastMessage("§e§lSESSION PAUSED! §eThe session has been temporarily paused.");
    }

    public void endSession() {
        this.stopTrackingSessionTime();
        Score sessionScore = this.sessionObjective.getScore("state");
        sessionScore.setScore(3);
        this.setOutOfSessionRules();
        this.setAllPlayersMaxFood();
        this.plugin.getVampireAbilityManager().clearAllCooldownsForNewSession();
        this.freezeTick();
        this.broadcastMessage("§c§lSESSION ENDED! §cThe SMP session has concluded. See you next time!");
    }

    public void primeNewSession() {
        Score sessionScore = this.sessionObjective.getScore("state");
        sessionScore.setScore(0);
        this.setTimeToNextMorning();
        this.setOutOfSessionRules();
        this.resetPlayers();
        this.incrementSessionID();
        this.plugin.getWorld().setClearWeatherDuration(Integer.MAX_VALUE);
        this.plugin.getVampireManager().clearAllPromotionBans();
        this.plugin.getVampireManager().clearAllStageCaps();
        this.plugin.getVampireAbilityManager().clearAllCooldownsForNewSession();
        this.plugin.getBeaconManager().clearAllBeaconCooldownsForNewSession();
        this.setAllPlayersMaxFood();
        this.freezeTick();
        this.broadcastMessage("§c§lSESSION PRIMED! §cThe SMP session state has been primed for the next session!");
    }

    public void preStartSession() {
        Score sessionScore = this.sessionObjective.getScore("state");
        sessionScore.setScore(4);
        this.setOutOfSessionRules();
        this.unfreezeTick();
        this.broadcastMessage("§b§lBUILDING MODE ENABLED! §bInteractions are now enabled. Use '/pow admin session start' to begin the full session.");
    }

    private void setTimeToNextMorning() {
        World world = this.plugin.getWorld();
        long currentTime = world.getTime();
        long currentDayTime = currentTime % 24000L;
        if (currentDayTime <= 1000L) {
            world.setTime(currentTime);
        } else {
            long timeUntilMorning = 24000L - currentDayTime;
            long nextMorningTime = currentTime + timeUntilMorning;
            world.setTime(nextMorningTime);
        }

    }

    private void setInSessionRules() {
        World world = this.plugin.getWorld();
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, true);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.DO_MOB_LOOT, true);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setGameRule(GameRule.REDUCED_DEBUG_INFO, true);
        world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
        world.setGameRule(GameRule.DO_INSOMNIA, false);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
    }

    private void setOutOfSessionRules() {
        World world = this.plugin.getWorld();
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.DO_MOB_LOOT, false);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setGameRule(GameRule.REDUCED_DEBUG_INFO, false);
        world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
        world.setGameRule(GameRule.DO_INSOMNIA, false);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, false);
    }

    private void resetPlayers() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            this.resetPlayer(player);
        }

    }

    public void resetPlayer(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
            this.plugin.getLogger().info("Reset " + player.getName() + " from spectator to survival mode (new game/session)");
        }

        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue((double)20.0F);
        double actualMaxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        player.setHealth(actualMaxHealth);

        for(String string : INFORMED_CONSTANTS) {
            player.removeScoreboardTag(string);
        }

        this.plugin.getVampireManager().clearPromotionBan(player);
        BeetrootManager beetrootManager = this.plugin.getBeetrootManager();
        if (beetrootManager != null) {
            beetrootManager.resetPlayerBeetrootUsage(player);
        }

        player.removeScoreboardTag("ChatPrevented");
    }

    public int getSessionState() {
        if (this.sessionObjective == null) {
            return 0;
        } else {
            Score sessionScore = this.sessionObjective.getScore("state");
            return sessionScore.getScore();
        }
    }

    public boolean isSessionActive() {
        return this.getSessionState() == 1;
    }

    public boolean isOutOfSession() {
        int state = this.getSessionState();
        return state == 0 || state == 2 || state == 3;
    }

    public boolean isPreSession() {
        return this.getSessionState() == 4;
    }

    private void broadcastMessage(String message) {
        Bukkit.broadcastMessage(message);
    }
}
