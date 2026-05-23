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
    private final Map<UUID, Integer> pausedFoodLevels = new HashMap<>();
    private final Map<UUID, Float> pausedSaturationLevels = new HashMap<>();
    private Objective sessionObjective, sessionIDObjective, gameIDObjective;
    public static final int BEFORE_SESSION = 0, IN_SESSION = 1, PAUSED = 2, AFTER_SESSION = 3, PRE_SESSION = 4;
    private long totalSessionTime = 0L, currentPhaseStartTime = 0L;
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
    public static final String INFORMED_BOUNDARY_COMPANION = "informed_boundary_companion";
    public static final String STOPTHEBLEEDING_USED_SESSION = "stopthebleeding_used_session";
    public static final String BLESSING_USED_SESSION = "blessing_used_session";
    public static final List<String> INFORMED_CONSTANTS = Arrays.asList(INFORMED_IRON_BLOCK_REPEL, INFORMED_CRAFTING_ITEMS, INFORMED_PICKUP_ITEM, INFORMED_PICKUP_HOLY_WATER, INFORMED_USE_HOLY_WATER, INFORMED_IRON_BLOCK_WEAKNESS, INFORMED_SUCCESSFUL_FEEDING, INFORMED_BLOOD_MOON, INFORMED_ENCHANTING_ITEMS, INFORMED_WEAPON_WEAKNESS, INFORMED_VAMPIRE_CLAWS, INFORMED_BOUNDARY, INFORMED_BOUNDARY_COMPANION, STOPTHEBLEEDING_USED_SESSION, BLESSING_USED_SESSION);

    /**
     * Create an instance of the Session manager.
     *
     * @param plugin the host plugin object.
     */
    public SessionManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    public void startBackgroundTasks() {
        this.startSaturationTask();
        this.startActionBarTask();
    }

    /**
     * Execute a plugin command through the console.
     *
     * @param command the command to execute.
     */
    public void executeServerCommand(String command) {
        try {
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                this.plugin.logInfo("Executed command: /" + command);
            });
        } catch (Exception e) {
            this.plugin.getLogger().warning("Failed to execute command: /" + command + " - " + e.getMessage());
        }
    }

    /**
     * Freeze the game ticks.
     */
    private void freezeTick() {
        this.executeServerCommand("tick freeze");
    }

    /**
     * Resume the game ticks.
     */
    private void unfreezeTick() {
        this.executeServerCommand("tick unfreeze");
    }

    /**
     * Freeze the players' food levels while a session is inactive.
     */
    private void startSaturationTask() {
        (new BukkitRunnable() {
            public void run() {
                int sessionState = SessionManager.this.getSessionState();

                if (sessionState == PAUSED) {
                    SessionManager.this.restorePausedFoodLevels();

                } else if (SessionManager.this.isOutOfSession()) {
                    SessionManager.this.applySaturationToAllPlayers();
                    SessionManager.this.setAllPlayersMaxFood();

                } else if (sessionState == PRE_SESSION) {
                    SessionManager.this.plugin.logInfo("PRE_SESSION: Setting all players to max food and saturation");
                    SessionManager.this.setAllPlayersMaxFood();
                }
            }
        }).runTaskTimer(this.plugin, 0L, 80L);
    }

    /**
     * Continually display the current session status when the session is not in an active game.
     */
    private void startActionBarTask() {
        (new BukkitRunnable() {
            public void run() {
                if (SessionManager.this.isOutOfSession() || SessionManager.this.isPreSession()) {
                    SessionManager.this.updateActionBarForAllPlayers();
                }
            }
        }).runTaskTimer(this.plugin, 0L, 20L);
    }

    /**
     * Display the current session status when the session is not in an active game.
     */
    private void updateActionBarForAllPlayers() {
        String message = this.getSessionStatusMessage();

        for(Player player : Bukkit.getOnlinePlayers()) {
            this.sendActionBar(player, message);
        }
    }

    /**
     * Update the words above the player's hotbar.
     *
     * @param player the player who is receiving the message.
     * @param message the message to be placed above the hotbar.
     */
    public void sendActionBar(Player player, String message) {
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        } catch (Exception e) {
            try {
                player.sendTitle("", message, 0, 25, 5);
            } catch (Exception e1) {
                player.sendMessage("§8[§6Session§8] " + message);
            }
        }
    }

    /**
     * Retrieve a status message from the current state of the session.
     *
     * @return A description of the session state.
     */
    private String getSessionStatusMessage() {
        return switch (this.getSessionState()) {
            case BEFORE_SESSION -> "§e§lSession is primed and ready to start";
            case IN_SESSION -> "§7§lSession is currently active";
            case PAUSED -> "§6§lSession is currently paused";
            case AFTER_SESSION -> "§c§lSession has ended";
            case PRE_SESSION -> "§b§lBuilding Mode - interactions enabled";
            default -> "§7§lSession status unknown";
        };
    }

    /**
     * Apply an extreme saturation effect to all players.
     */
    private void applySaturationToAllPlayers() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 100, 100, false, false));
        }
    }

    /**
     * Fill the food and saturation bars of all players.
     */
    private void setAllPlayersMaxFood() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            player.setFoodLevel(20);
            player.setSaturation(20.0F);
        }
    }

    /**
     * Record the levels of the food and saturation bars for all players.
     */
    private void capturePausedFoodLevels() {
        this.pausedFoodLevels.clear();
        this.pausedSaturationLevels.clear();

        for(Player player : Bukkit.getOnlinePlayers()) {
            this.pausedFoodLevels.put(player.getUniqueId(), player.getFoodLevel());
            this.pausedSaturationLevels.put(player.getUniqueId(), player.getSaturation());
        }

        this.plugin.logInfo("Captured food levels for " + this.pausedFoodLevels.size() + " players when session was paused");
    }

    /**
     * Restore the food and saturation levels of all players to their last recorded values.
     */
    private void restorePausedFoodLevels() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();

            if (this.pausedFoodLevels.containsKey(playerId)) {
                int pausedFood = this.pausedFoodLevels.get(playerId);
                float pausedSaturation = this.pausedSaturationLevels.get(playerId);
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

    /**
     * Retrieve whether the first beacon of the game has been converted yet.
     *
     * @return {@code true} if the first beacon has been converted since the game was initialized.
     */
    public boolean isFirstBeaconConvertedTriggered() {
        return this.plugin.getConfig().getBoolean("first_beacon_converted", false);
    }

    /**
     * Update the config on whether the first beacon has been converted since the game was initialized.
     *
     * @param triggered {@code true} when the first beacon of the game has been converted.
     */
    public void setFirstBeaconConvertedTriggered(boolean triggered) {
        this.plugin.getConfig().set("first_beacon_converted", triggered);
        this.plugin.saveConfig();
    }

    /**
     * Retrieve whether all the beacons are holy aligned.
     *
     * @return {@code true} if the human team controls all beacons.
     */
    public boolean areHumansOwningAllBeacons() {
        return this.plugin.getConfig().getBoolean("humans_own_all_beacons", false);
    }

    /**
     * Update the config on whether the human team controls all beacons.
     *
     * @param active {@code true} if all the beacons are holy aligned.
     */
    public void setHumansOwningAllBeacons(boolean active) {
        this.plugin.getConfig().set("humans_own_all_beacons", active);
        this.plugin.saveConfig();
    }

    /**
     * Retrieve whether all the beacons are darkness aligned.
     *
     * @return {@code true} if the vampire team controls all beacons.
     */
    public boolean areVampiresOwningAllBeacons() {
        return this.plugin.getConfig().getBoolean("vampires_own_all_beacons", false);
    }

    /**
     * Update the config on whether the vampire team controls all beacons.
     *
     * @param active {@code true} if all the beacons are darkness aligned.
     */
    public void setVampiresOwningAllBeacons(boolean active) {
        this.plugin.getConfig().set("vampires_own_all_beacons", active);
        this.plugin.saveConfig();
    }

    /**
     * Check if the holy control final stand is active.
     *
     * @return {@code true} if the human team controls all beacons.
     */
    public boolean isHumansFinalStandActive() {
        return this.areHumansOwningAllBeacons();
    }

    /**
     * Set whether the holy control final stand is active.
     *
     * @param active {@code true} when the human team controls all beacons.
     */
    public void setHumansFinalStandActive(boolean active) {
        this.setHumansOwningAllBeacons(active);
    }

    /**
     * Retrieve whether there is a single human remaining in the game.
     *
     * @return {@code true} if only one human remains alive.
     */
    public boolean isOneHumanLeftActive() {
        return this.plugin.getConfig().getBoolean("one_human_left", false);
    }

    /**
     * Update the config on whether only one human remains alive.
     *
     * @param active {@code true} if there is a single human remaining in the game.
     */
    public void setOneHumanLeftActive(boolean active) {
        this.plugin.getConfig().set("one_human_left", active);
        this.plugin.saveConfig();
    }

    /**
     * Check if the darkness control final stand is active.
     *
     * @return {@code true} if the vampire team controls all beacons.
     */
    public boolean isVampiresEternalNightActive() {
        return this.areVampiresOwningAllBeacons();
    }

    /**
     * Set whether the darkness control final stand is active.
     *
     * @param active {@code true} when the vampire team controls all beacons.
     */
    public void setVampiresEternalNightActive(boolean active) {
        this.setVampiresOwningAllBeacons(active);
    }

    /**
     * Update the config on whether NPC mobs such as Pillagers and Wandering Traders can spawn naturally.
     *
     * @param enabled {@code true} if NPC mobs can spawn naturally.
     */
    public void setNpcSpawningGamerules(boolean enabled) {
        World world = this.plugin.getWorld();

        // Update the config's setting with the provided preference
        this.plugin.getConfig().set("enable-npc-mobs", enabled);
        this.plugin.saveConfig();

        // Update the world's gamerule state
        this.setNpcSpawningGamerules(world, enabled);
    }

    public void incrementSessionID() {
        this.sessionIDObjective.getScore("session_id_holder").setScore(this.sessionIDObjective.getScore("session_id_holder").getScore() + 1);
        this.updateAllPlayersSessionIDs();
    }

    public void incrementGameID() {
        this.gameIDObjective.getScore("game_id_holder").setScore(this.gameIDObjective.getScore("game_id_holder").getScore() + 1);
        this.updateAllPlayersGameIDs();
        this.plugin.logInfo("Game ID incremented to: " + this.gameIDObjective.getScore("game_id_holder").getScore());
    }

    public void initializeScoreboard() {
        Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        this.sessionObjective = mainScoreboard.getObjective("smp_session");

        if (this.sessionObjective == null) {
            this.sessionObjective = mainScoreboard.registerNewObjective("smp_session", "dummy", "SMP Session State");
            Score sessionScore = this.sessionObjective.getScore("state");
            sessionScore.setScore(BEFORE_SESSION);
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
            this.plugin.logInfo("Server initialized in OUT OF SESSION mode - PvP, block breaking, and time cycles disabled.");
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

    /**
     * Convert the session time into seconds.
     *
     * @return A whole number of seconds.
     */
    public long getSessionTimeSeconds() {
        return this.getSessionTime() / 1000L;
    }

    /**
     * Begin tracking the system time.
     */
    private void startTrackingSessionTime() {
        if (!this.trackingSessionTime) {
            this.currentPhaseStartTime = System.currentTimeMillis();
            this.trackingSessionTime = true;
            this.plugin.logInfo("Started tracking session time. Total session time: " + this.totalSessionTime / 1000L + " seconds");
        }
    }

    /**
     * Stop tracking the system time and record the time since the current game phase began.
     */
    private void stopTrackingSessionTime() {
        if (this.trackingSessionTime) {
            long phaseTime = System.currentTimeMillis() - this.currentPhaseStartTime;
            this.totalSessionTime += phaseTime;
            this.trackingSessionTime = false;
            this.plugin.logInfo("Stopped tracking session time. Added " + phaseTime / 1000L + " seconds. Total session time: " + this.totalSessionTime / 1000L + " seconds");
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

        this.plugin.logInfo("Updated game IDs for all online players to: " + game_id);
    }

    /**
     * Begin a new session and reset the multitude of timers and cooldowns.
     */
    public void startSession() {
        Score sessionScore = this.sessionObjective.getScore("state");
        sessionScore.setScore(IN_SESSION);
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
        this.plugin.getTomeDistributionManager().startDistributionTask();

        this.broadcastMessage("§a§lSESSION STARTED! §aThe SMP session has begun. Good luck!");
    }

    /**
     * Resume the current Vampires game session.
     */
    public void resumeSession() {
        Score sessionScore = this.sessionObjective.getScore("state");
        sessionScore.setScore(IN_SESSION);
        this.setInSessionRules();
        this.startTrackingSessionTime();
        this.unfreezeTick();

        this.plugin.getBeaconMajorityManager().updateBeaconMajorityBonuses();
        this.plugin.getTomeDistributionManager().startDistributionTask();

        this.broadcastMessage("§a§lSESSION RESUMED! §aThe SMP session has been resumed.");
    }

    /**
     * Freeze the current Vampires game session.
     */
    public void pauseSession() {
        this.capturePausedFoodLevels();
        this.stopTrackingSessionTime();

        Score sessionScore = this.sessionObjective.getScore("state");
        sessionScore.setScore(PAUSED);
        this.setOutOfSessionRules();
        this.freezeTick();
        this.plugin.getTomeDistributionManager().stopDistributionTask();

        this.broadcastMessage("§e§lSESSION PAUSED! §eThe session has been temporarily paused.");
    }

    /**
     * End the current Vampires game session.
     */
    public void endSession() {
        this.stopTrackingSessionTime();
        Score sessionScore = this.sessionObjective.getScore("state");
        sessionScore.setScore(AFTER_SESSION);

        this.setOutOfSessionRules();
        this.setAllPlayersMaxFood();
        this.plugin.getVampireAbilityManager().clearAllCooldownsForNewSession();
        this.freezeTick();
        this.plugin.getTomeDistributionManager().stopDistributionTask();

        this.broadcastMessage("§c§lSESSION ENDED! §cThe SMP session has concluded. See you next time!");
    }

    /**
     * Prime the Vampires game to begin a new session.
     */
    public void primeNewSession() {
        Score sessionScore = this.sessionObjective.getScore("state");
        sessionScore.setScore(BEFORE_SESSION);
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
        this.plugin.getTomeDistributionManager().stopDistributionTask();

        this.broadcastMessage("§c§lSESSION PRIMED! §cThe SMP session state has been primed for the next session!");
    }

    /**
     * Set the new Vampires game session into build mode before the proper session start.
     */
    public void preStartSession() {
        Score sessionScore = this.sessionObjective.getScore("state");
        sessionScore.setScore(PRE_SESSION);
        this.setOutOfSessionRules();
        this.unfreezeTick();
        this.broadcastMessage("§b§lBUILDING MODE ENABLED! §bInteractions are now enabled. Use '/pow admin session start' to begin the full session.");
    }

    /**
     * Skip the world's daylight cycle to the next morning.
     */
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

    /**
     * Set the Minecraft gamerules of the session.
     */
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

        this.setNpcSpawningGamerules(world, plugin.getConfigManager().areNpcMobsEnabled());
    }

    /**
     * Turn off the Minecraft gamerules of the session.
     */
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

        this.setNpcSpawningGamerules(world, false);
    }

    /**
     * Reset the tags of all online players.
     */
    private void resetPlayers() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            this.resetPlayer(player);
        }
    }

    /**
     * Reset the health and tags of the player.
     *
     * @param player the player being reset.
     */
    public void resetPlayer(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
            this.plugin.logInfo("Reset " + player.getName() + " from spectator to survival mode (new game/session)");
        }

        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20.0);
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
            return BEFORE_SESSION;

        } else {
            Score sessionScore = this.sessionObjective.getScore("state");
            return sessionScore.getScore();
        }
    }

    /**
     * Check if the session is currently active.
     *
     * @return {@code true} if the game state is IN_SESSION.
     */
    public boolean isSessionActive() {
        return this.getSessionState() == IN_SESSION;
    }

    /**
     * Check if the session is in an inactive state.
     *
     * @return {@code true} if the game state is BEFORE_SESSION, PAUSED, or AFTER_SESSION.
     */
    public boolean isOutOfSession() {
        int state = this.getSessionState();
        return state == BEFORE_SESSION || state == PAUSED || state == AFTER_SESSION;
    }

    /**
     * Check if the session is primed for starting.
     *
     * @return {@code true} if the game state is PRE_SESSION.
     */
    public boolean isPreSession() {
        return this.getSessionState() == PRE_SESSION;
    }

    /**
     * Set the game rules for whether certain NPC mobs are allowed to spawn.
     *
     * @param enabled {@code true} if NPC mobs should be able to spawn naturally.
     */
    private void setNpcSpawningGamerules(World world, boolean enabled) {
        world.setGameRule(GameRule.DO_TRADER_SPAWNING, enabled);
        world.setGameRule(GameRule.DO_PATROL_SPAWNING, enabled);
        world.setGameRule(GameRule.DISABLE_RAIDS, enabled);
    }

    /**
     * Broadcast the provided message to the server.
     *
     * @param message the message to broadcast.
     */
    private void broadcastMessage(String message) {
        Bukkit.broadcastMessage(message);
    }
}
