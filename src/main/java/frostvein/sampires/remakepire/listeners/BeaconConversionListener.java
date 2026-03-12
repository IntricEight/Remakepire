package frostvein.sampires.remakepire.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.beacons.BeaconSite;
import frostvein.sampires.remakepire.beacons.BeaconSite.BeaconState;
import frostvein.sampires.remakepire.managers.BeaconManager;
import frostvein.sampires.remakepire.managers.SessionManager;
import frostvein.sampires.remakepire.managers.VampireManager;

public class BeaconConversionListener implements Listener {
    private final RemakepirePlugin plugin;
    private final BeaconManager beaconManager;
    private final VampireManager vampireManager;
    private final SessionManager sessionManager;
    private final Map<String, ConversionData> activeConversions;
    private final long BASE_CONVERSION_TIME;
    // Controls how frequently the beacon is checked for progress or cancellation (in ticks).
    private static final long CONVERSION_TICK_INTERVAL = 20L;
    // Controls the distance you can interact with beacons from (in blocks)
    private static final double BEACON_INTERACTION_RANGE = 3.0;

    /**
     * Create an instance of the Beacon Conversion listener.
     *
     * @param plugin the host plugin object.
     */
    public BeaconConversionListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.beaconManager = plugin.getBeaconManager();
        this.vampireManager = plugin.getVampireManager();
        this.sessionManager = plugin.getSessionManager();
        this.activeConversions = new HashMap<>();
        this.BASE_CONVERSION_TIME = plugin.getConfigManager().getBeaconConversionTimeMs();
    }

    /**
     * Handle the beacon interaction as a player begins or ceases crouching.
     *
     * @param event a player beginning or stopping to sneak.
     */
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (event.isSneaking()) {
            this.handlePlayerStartCrouching(player);
        } else {
            this.handlePlayerStopCrouching(player);
        }
    }

    /**
     * Handle the beacon interaction as a player moves.
     *
     * @param event a player moving.
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.isSneaking()) {
            this.checkPlayerProximityToConversions(player);
        }
    }

    /**
     * Cancel a player's beacon interaction when they leave the game.
     *
     * @param event a player leaving the world.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.handlePlayerStopCrouching(event.getPlayer());
    }

    /**
     * Handle the beacon interaction as the player begins crouching.
     *
     * @param player the player who started sneaking.
     */
    private void handlePlayerStartCrouching(Player player) {
        if (this.vampireManager.isHuman(player) || this.vampireManager.isVampire(player)) {
            if (player.getGameMode() != GameMode.SPECTATOR) {
                for(BeaconSite beacon : this.beaconManager.getBeaconsInRange(player.getLocation(), BEACON_INTERACTION_RANGE)) {
                    if (!this.sessionManager.isSessionActive()) {
                        player.sendMessage("§c⚠ Cannot convert beacons - no session is currently active.");
                        return;
                    }

                    if (this.canPlayerConvertBeacon(player, beacon)) {
                        this.startBeaconConversion(player, beacon);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Handle the beacon interaction as the player stops crouching.
     *
     * @param player the player who stopped sneaking.
     */
    private void handlePlayerStopCrouching(Player player) {
        Iterator<Map.Entry<String, ConversionData>> iterator = this.activeConversions.entrySet().iterator();

        while(iterator.hasNext()) {
            Map.Entry<String, ConversionData> entry = (Map.Entry)iterator.next();
            ConversionData data = entry.getValue();

            if (data.getConverters().contains(player.getUniqueId())) {
                data.removeConverter(player.getUniqueId());

                if (data.getConverters().isEmpty()) {
                    this.cancelConversion(entry.getKey(), data);
                    iterator.remove();

                } else {
                    data.recalculateConversionTime();
                }
            }
        }
    }

    /**
     * Determine if a player is close enough for beacon interaction.
     *
     * @param player the player near the beacon.
     */
    private void checkPlayerProximityToConversions(Player player) {
        for(Map.Entry<String, ConversionData> entry : this.activeConversions.entrySet()) {
            ConversionData data = entry.getValue();
            BeaconSite beacon = this.beaconManager.getBeacon(entry.getKey());

            if (beacon != null) {
                if (data.getConverters().contains(player.getUniqueId())) {
                    Location beaconLoc = beacon.getLocation();

                    if (beaconLoc == null || !beaconLoc.getWorld().equals(player.getWorld()) || beaconLoc.distance(player.getLocation()) > 3 || player.getGameMode() == GameMode.SPECTATOR) {
                        this.handlePlayerStopCrouching(player);
                        break;
                    }
                }

                // Check if a player on the opposite team can interfere with the conversion
                if (this.checkForEnemyInterference(player, beacon, data)) {
                    this.cancelConversion(entry.getKey(), data);
                    this.activeConversions.remove(entry.getKey());
                    break;
                }
            }
        }
    }

    /**
     * Determine if a player from the opposing team is interfering with the beacon conversion.
     *
     * @param player the player near the beacon.
     * @param beacon the beacon being converted.
     * @param data the details of the current conversion attempt.
     * @return {@code true} if the player has prevented the conversion.
     */
    private boolean checkForEnemyInterference(Player player, BeaconSite beacon, ConversionData data) {
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return false;
        } else {
            Location beaconLoc = beacon.getLocation();

            if (beaconLoc != null && beaconLoc.getWorld().equals(player.getWorld()) && !(beaconLoc.distance(player.getLocation()) > BEACON_INTERACTION_RANGE)) {
                if (data.getConverters().contains(player.getUniqueId())) {
                    return false;
                } else {
                    boolean playerIsVampire = this.vampireManager.isVampireStage2OrHigher(player);
                    boolean playerIsHuman = this.vampireManager.isHuman(player);

                    if (!playerIsVampire && !playerIsHuman) {
                        return false;
                    } else if (playerIsVampire && player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                        return false;
                    } else {
                        return data.isVampireConversion() && playerIsHuman || !data.isVampireConversion() && playerIsVampire;
                    }
                }
            } else {
                return false;
            }
        }
    }

    /**
     * Determine if the player is attempting a valid conversion.
     *
     * @param player the player near the beacon.
     * @param beacon the beacon the player is attempting to convert.
     * @return {@code true} if the player can convert the beacon.
     */
    private boolean canPlayerConvertBeacon(Player player, BeaconSite beacon) {
        if (beacon.getState() == BeaconState.PERMANENTLY_DESECRATED) {
            player.sendMessage("§cThis beacon has been permanently corrupted and cannot be converted.");
            return false;

        } else {
            boolean isOneHumanLeftActive = this.plugin.getSessionManager().isOneHumanLeftActive();
            boolean isPlayerHuman = this.vampireManager.isHuman(player);

            if (!beacon.isOnConversionCooldown(this.sessionManager) || isOneHumanLeftActive && isPlayerHuman) {
                if (this.activeConversions.containsKey(beacon.getName().toLowerCase())) {
                    ConversionData existing = (ConversionData)this.activeConversions.get(beacon.getName().toLowerCase());
                    boolean playerIsVampire = this.vampireManager.isVampire(player);
                    return existing.isVampireConversion() == playerIsVampire;

                } else {
                    boolean playerIsVampire = this.vampireManager.isVampire(player);
                    BeaconSite.BeaconState currentState = beacon.getState();

                    if (playerIsVampire && currentState == BeaconState.DESECRATED) {
                        return false;
                    } else if (!playerIsVampire && currentState == BeaconState.HOLY) {
                        return false;
                    } else {
                        for(Player nearbyPlayer : this.getPlayersInRange(beacon.getLocation(), BEACON_INTERACTION_RANGE)) {
                            if (!nearbyPlayer.equals(player)) {
                                boolean nearbyIsVampire = this.vampireManager.isVampireStage2OrHigher(nearbyPlayer);
                                boolean nearbyIsHuman = this.vampireManager.isHuman(nearbyPlayer);

                                if ((!nearbyIsVampire || !nearbyPlayer.hasPotionEffect(PotionEffectType.INVISIBILITY)) && (playerIsVampire && nearbyIsHuman || !playerIsVampire && nearbyIsVampire)) {
                                    if (playerIsVampire) {
                                        player.sendMessage("§cA pure being is nearby... They are stopping you from converting this beacon...");
                                    } else {
                                        player.sendMessage("§cYou feel unable to convert this beacon... As if a dark presence is choking the very light from it...");
                                    }

                                    return false;
                                }
                            }
                        }

                        return true;
                    }
                }
            } else {
                player.sendMessage("§c⏰ Beacon " + beacon.getName() + " cannot be converted yet. Cooldown remaining: §e" + beacon.getRemainingCooldownString(this.sessionManager) + " (session time)");
                return false;
            }
        }
    }

    /**
     * Begin the conversion and register the beacon conversion attempt.
     *
     * @param player the player near the beacon.
     * @param beacon the beacon the player is converting.
     */
    private void startBeaconConversion(Player player, BeaconSite beacon) {
        String beaconKey = beacon.getName().toLowerCase();
        boolean isVampireConversion = this.vampireManager.isVampire(player);
        ConversionData data = this.activeConversions.get(beaconKey);

        if (data == null) {
            data = new ConversionData(beacon, isVampireConversion);
            this.activeConversions.put(beaconKey, data);
        }

        data.addConverter(player.getUniqueId());
        data.recalculateConversionTime();
        player.sendMessage("§eConverting beacon §f" + beacon.getName() + "§e... Stay crouched.");
    }

    /**
     * Cancel the conversion and clean up the beacon conversion attempt.
     *
     * @param beaconKey the identifier to filter information about the conversion.
     * @param data the recorded information about the conversion.
     */
    private void cancelConversion(String beaconKey, ConversionData data) {
        data.cleanup();
        BeaconSite beacon = this.beaconManager.getBeacon(beaconKey);

        if (beacon != null) {
            this.broadcastToBeaconArea(beacon, "§7Beacon conversion of §e" + beacon.getName() + " §7has been interrupted!");
        }
    }

    /**
     * Retrieve a list of players within a provided distance of a beacon.
     *
     * @param center the beacon's location.
     * @param range a distance around the beacon (in blocks).
     * @return A list of players within the provide range of the beacon.
     */
    private List<Player> getPlayersInRange(Location center, double range) {
        List<Player> players = new ArrayList<>();

        if (center != null && center.getWorld() != null) {
            for(Player player : center.getWorld().getPlayers()) {
                if (player.getGameMode() == GameMode.SURVIVAL && center.distance(player.getLocation()) <= range) {
                    players.add(player);
                }
            }

            return players;
        } else {
            return players;
        }
    }

    /**
     * Inform the players near the beacon with a message.
     *
     * @param beacon the beacon being converted.
     * @param message the message for players near the beacon.
     */
    private void broadcastToBeaconArea(BeaconSite beacon, String message) {
        Location beaconLoc = beacon.getLocation();

        if (beaconLoc != null) {
            for(Player player : this.getPlayersInRange(beaconLoc, beacon.getCaptureRadius() * 2)) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * Request a server-wide broadcast that a beacon has been converted to neutral.
     *
     * @param beacon the beacon being converted.
     * @param previousState the beacon's previous alignment.
     */
    private void broadcastNeutralConversionToAll(BeaconSite beacon, BeaconSite.BeaconState previousState) {
        this.beaconManager.broadcastNeutralConversionToAll(beacon, previousState);
    }

    /**
     * Retrieve the beacon's previous alignment.
     *
     * @param beacon the beacon being converted.
     * @param isVampireConversion {@code true} if a vampire is converting the beacon.
     * @return the beacon's previous alignment.
     */
    private BeaconSite.BeaconState getPreviousBeaconState(BeaconSite beacon, boolean isVampireConversion) {
        return isVampireConversion ? BeaconState.HOLY : BeaconState.DESECRATED;
    }

    /**
     * Broadcast the beacon's completed conversion to the server.
     *
     * @param beacon the beacon being converted.
     * @param newState the beacon's new alignment.
     */
    private void broadcastBeaconGainToTeam(BeaconSite beacon, BeaconSite.BeaconState newState) {
        for(Player player : this.plugin.getServer().getOnlinePlayers()) {
            if (newState == BeaconState.HOLY) {
                player.sendMessage("§aBeacon §e" + beacon.getName() + " §ahas been blessed with divine energy.");
            } else if (newState == BeaconState.DESECRATED) {
                player.sendMessage("§4Beacon §e" + beacon.getName() + " §4has been consumed by dark forces.");
            }
        }
    }

    /**
     * Display conversion particles around the beacon.
     *
     * @param beacon the beacon being converted.
     * @param isVampireConversion {@code true} if a vampire is converting the beacon.
     * @param progress the completion percent of the beacon conversion.
     */
    private void showConversionParticles(BeaconSite beacon, boolean isVampireConversion, double progress) {
        Location particleLoc = beacon.getParticleLocation();

        if (particleLoc != null) {
            if (isVampireConversion) {
                particleLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, particleLoc, (int)(5 * progress), 0.5, 0.5, 0.5, 0.01);
                particleLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, (int)(3 * progress), 0.3, 0.3, 0.3, 0.02);

            } else {
                particleLoc.getWorld().spawnParticle(Particle.WHITE_ASH, particleLoc, (int)(8 * progress), 0.5, 0.5, 0.5, 0.01);
                particleLoc.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, (int)(5 * progress), 0.3, 0.3, 0.3, 0.1);
            }
        }
    }

    /**
     * Clean up the conversions before shutting down the server.
     */
    public void shutdown() {
        int conversionCount = this.activeConversions.size();

        for(ConversionData data : this.activeConversions.values()) {
            data.cleanup();
        }

        this.activeConversions.clear();
        this.plugin.getLogger().info("BeaconConversionListener shutdown - cleaned up " + conversionCount + " active conversions.");
    }

    /**
     * Check if a team has achieved complete control over the beacons. Initiate the final stand if one does.
     */
    private void checkForFinalStand() {
        Map<BeaconSite.BeaconState, Integer> stateStats = this.beaconManager.getStateStats();
        int holyCount = stateStats.get(BeaconState.HOLY);
        int desecratedCount = stateStats.get(BeaconState.DESECRATED);
        int permanentlyDesecratedCount = stateStats.getOrDefault(BeaconState.PERMANENTLY_DESECRATED, 0);
        int totalEvilCount = desecratedCount + permanentlyDesecratedCount;
        int totalBeacons = this.beaconManager.getAllBeacons().size();

        // Begin the holy controlled final stand
        if (holyCount >= totalBeacons && totalBeacons > 0) {
            this.triggerHumansFinalStand();
        }

        // Begin the darkness controlled final stand
        if (totalEvilCount >= totalBeacons && totalBeacons > 0) {
            this.triggerVampiresEternalNight();
        }
    }

    /**
     * Trigger the complete holy control final stand scenario.
     */
    private void triggerHumansFinalStand() {
        this.plugin.getLogger().info("HUMANS FINAL STAND TRIGGERED - All 7 beacons are holy!");

        // Alert all online players of the final stand
        for(Player player : this.plugin.getServer().getOnlinePlayers()) {
            player.sendTitle("§c§lALL BEACONS SANCTIFIED", "§eThe divine light weakens all creatures of darkness", 20, 100, 40);
            player.sendMessage("§e All beacons now shine with divine energy.");
            player.sendMessage("§e Evil has been weakened by the overwhelming holy presence.");
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 1.0F, 0.7F);
        }

        // Reduce the vampires' max health while the final stand is active
        for(Player player : this.plugin.getServer().getOnlinePlayers()) {
            if (this.vampireManager.isVampire(player)) {
                player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(6.0);
                if (player.getHealth() > 6) {
                    player.setHealth(6.0);
                }
            }
        }

        this.plugin.getSessionManager().setHumansFinalStandActive(true);
    }

    /**
     * Disable the complete holy control final stand scenario.
     */
    private void disableHumansFinalStand() {
        this.plugin.getLogger().info("HUMANS FINAL STAND ENDED - A vampire has converted a beacon!");

        for(Player player : this.plugin.getServer().getOnlinePlayers()) {
            player.sendTitle("§4§lFINAL STAND BROKEN", "§cDarkness pushes back against the light", 20, 80, 20);
            player.sendMessage("§cA vampire has broken through the holy defenses.");
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.MASTER, 1.0F, 0.5F);
        }

        // Restore the vampires' max health
        for(Player player : this.plugin.getServer().getOnlinePlayers()) {
            if (this.vampireManager.isVampire(player)) {
                player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20.0);
            }
        }

        this.plugin.getSessionManager().setHumansFinalStandActive(false);
    }

    /**
     * Trigger the complete darkness control final stand scenario.
     */
    private void triggerVampiresEternalNight() {
        this.plugin.getLogger().info("VAMPIRES ETERNAL NIGHT TRIGGERED - All 7 beacons are desecrated!");

        for(Player player : this.plugin.getServer().getOnlinePlayers()) {
            player.sendTitle("§4§lETERNAL NIGHT FALLS", "§cThe darkness consumes all hope", 20, 100, 40);
            player.sendMessage("§c All beacons now pulse with unholy energy.");
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.MASTER, 1.0F, 0.5F);
        }

        // Blind all humans while the final stand is active
        for(Player player : this.plugin.getServer().getOnlinePlayers()) {
            if (this.vampireManager.isHuman(player) && player.getGameMode() == GameMode.SURVIVAL) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, -1, 0, false, false, true));
            }
        }

        this.plugin.getSessionManager().setVampiresEternalNightActive(true);
    }

    /**
     * Disable the complete darkness control final stand scenario.
     */
    private void disableVampiresEternalNight() {
        this.plugin.getLogger().info("VAMPIRES ETERNAL NIGHT ENDED - A human has sanctified a beacon!");

        for(Player player : this.plugin.getServer().getOnlinePlayers()) {
            player.sendTitle("§6§lLIGHT RETURNS", "§eA beacon shines with holy light once more", 20, 80, 20);
            player.sendMessage("§aA human has broken through the darkness.");
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 1.0F, 1.2F);
        }

        // Remove the mining fatigue effect from humans
        for(Player player : this.plugin.getServer().getOnlinePlayers()) {
            if (this.vampireManager.isHuman(player)) {
                player.removePotionEffect(PotionEffectType.DARKNESS);
            }
        }

        this.plugin.getSessionManager().setVampiresEternalNightActive(false);
    }

    private class ConversionData {
        private final BeaconSite beacon;
        private final boolean isVampireConversion;
        private final Set<UUID> converters;
        private final long startTime;
        private long phaseStartTime;
        private BukkitTask task;
        private long adjustedConversionTime;
        private boolean neutralStageComplete;
        private BossBar bossBar;

        /**
         * Create an instance of a beacon conversion record.
         *
         * @param beacon the beacon being converted.
         * @param isVampireConversion {@code true} if a vampire is converting the beacon.
         */
        public ConversionData(BeaconSite beacon, boolean isVampireConversion) {
            this.beacon = beacon;
            this.isVampireConversion = isVampireConversion;
            this.converters = new HashSet<>();
            this.startTime = System.currentTimeMillis();
            this.phaseStartTime = this.startTime;
            this.adjustedConversionTime = BeaconConversionListener.this.BASE_CONVERSION_TIME;
            this.neutralStageComplete = beacon.getState() == BeaconState.NEUTRAL;

            if (this.neutralStageComplete) {
                this.adjustedConversionTime = BeaconConversionListener.this.BASE_CONVERSION_TIME;
            } else {
                this.adjustedConversionTime = BeaconConversionListener.this.BASE_CONVERSION_TIME / 2L;
            }

            this.createBossBar();
        }

        /**
         * Add the player to the list of active converters.
         *
         * @param playerId the id of the player converting the beacon.
         */
        public void addConverter(UUID playerId) {
            this.converters.add(playerId);
            Player player = BeaconConversionListener.this.plugin.getServer().getPlayer(playerId);

            if (player != null && player.isOnline()) {
                this.bossBar.addPlayer(player);
            }
        }

        /**
         * Remove the player from the list of active converters.
         *
         * @param playerId the id of the player converting the beacon.
         */
        public void removeConverter(UUID playerId) {
            this.converters.remove(playerId);
            Player player = BeaconConversionListener.this.plugin.getServer().getPlayer(playerId);

            if (player != null) {
                this.bossBar.removePlayer(player);
            }
        }

        /**
         * Create a progress bar for the beacon conversion.
         */
        private void createBossBar() {
            String beaconName = this.beacon.getName();
            BarColor color = this.isVampireConversion ? BarColor.RED : BarColor.WHITE;
            String title = this.isVampireConversion ? "§4Desecrating Beacon: §f" + beaconName : "§fConsecrating Beacon: §f" + beaconName;

            this.bossBar = BeaconConversionListener.this.plugin.getServer().createBossBar(title, color, BarStyle.SOLID, new BarFlag[0]);
            this.bossBar.setProgress(this.neutralStageComplete ? 0.5 : 0.0);
        }

        /**
         * Update the progress bar for the beacon conversion.
         *
         * @param progress the beacon's conversion progress percentage.
         * @param phase what transition the beacon is making.
         */
        public void updateBossBar(double progress, String phase) {
            this.bossBar.setProgress(Math.min(1.0, progress));
            String beaconName = this.beacon.getName();
            String title;
            if (phase.equals("neutral")) {
                title = "§7Cleansing Beacon: §f" + beaconName + " §7(" + (int)(progress * 100) + "%)";
                this.bossBar.setColor(BarColor.YELLOW);

            } else if (this.isVampireConversion) {
                title = "§4Desecrating Beacon: §f" + beaconName + " §4(" + (int)(progress * 100) + "%)";
                this.bossBar.setColor(BarColor.RED);

            } else {
                title = "§fConsecrating Beacon: §f" + beaconName + " §f(" + (int)(progress * 100) + "%)";
                this.bossBar.setColor(BarColor.WHITE);
            }

            this.bossBar.setTitle(title);
        }

        /**
         * Remove the progress bar for the beacon conversion.
         */
        public void cleanup() {
            if (this.bossBar != null) {
                this.bossBar.removeAll();
                this.bossBar = null;
            }

            if (this.task != null) {
                this.task.cancel();
                this.task = null;
            }
        }

        /**
         * Calculate the time needed to convert the beacon.
         */
        public void recalculateConversionTime() {
            long currentTime = System.currentTimeMillis(), existingElapsed = currentTime - this.phaseStartTime;
            double currentProgress = Math.min(1.0, (double)existingElapsed / (double)this.adjustedConversionTime);
            long oldAdjustedTime = this.adjustedConversionTime;

            // Get the conversion multipliers for the two teams
            double speedMultiplier = (double)this.converters.size();
            double humanSpeedMultiplier = BeaconConversionListener.this.plugin.getConfigManager().getBeaconHumanSpeedMultiplier();

            if (!this.isVampireConversion) {
                speedMultiplier *= humanSpeedMultiplier;
            }

            double finalStandMultiplier = BeaconConversionListener.this.plugin.getConfigManager().getBeaconFinalStandMultiplier();
            if (!this.isVampireConversion && BeaconConversionListener.this.plugin.getSessionManager().isHumansFinalStandActive() && this.converters.size() == 1) {
                speedMultiplier = finalStandMultiplier * humanSpeedMultiplier;
            }

            long baseTime = this.neutralStageComplete ? BeaconConversionListener.this.BASE_CONVERSION_TIME : BeaconConversionListener.this.BASE_CONVERSION_TIME / 2L;
            this.adjustedConversionTime = (long)((double)baseTime / speedMultiplier);
            long preservedElapsed = (long)((double)this.adjustedConversionTime * currentProgress);
            this.phaseStartTime = currentTime - preservedElapsed;
            this.startConversionTask();
        }

        /**
         * Begin and process the beacon conversion.
         */
        private void startConversionTask() {
            if (this.task != null) {
                this.task.cancel();
            }

            this.task = BeaconConversionListener.this.plugin.getServer().getScheduler().runTaskTimer(BeaconConversionListener.this.plugin, () -> {
                if (!BeaconConversionListener.this.sessionManager.isSessionActive()) {
                    BeaconConversionListener.this.cancelConversion(this.beacon.getName().toLowerCase(), this);
                    BeaconConversionListener.this.activeConversions.remove(this.beacon.getName().toLowerCase());

                    for(UUID converterId : this.converters) {
                        Player converter = BeaconConversionListener.this.plugin.getServer().getPlayer(converterId);
                        if (converter != null && converter.isOnline()) {
                            converter.sendMessage("§c⚠ Beacon conversion cancelled - session is not active.");
                        }
                    }

                } else if (this.checkForEnemyInterferenceInArea()) {
                    BeaconConversionListener.this.cancelConversion(this.beacon.getName().toLowerCase(), this);
                    BeaconConversionListener.this.activeConversions.remove(this.beacon.getName().toLowerCase());

                } else {
                    long elapsed = System.currentTimeMillis() - this.phaseStartTime;
                    double rawProgress = Math.min(1.0, (double)elapsed / (double)this.adjustedConversionTime);

                    double displayProgress;
                    String currentPhase;

                    if (this.neutralStageComplete) {
                        displayProgress = 0.5 + rawProgress * 0.5;
                        currentPhase = this.isVampireConversion ? "desecrating" : "consecrating";

                    } else {
                        displayProgress = rawProgress * 0.5;
                        currentPhase = "cleansing";
                    }

                    this.updateBossBar(displayProgress, currentPhase);
                    BeaconConversionListener.this.showConversionParticles(this.beacon, this.isVampireConversion, displayProgress);

                    if (elapsed >= this.adjustedConversionTime) {
                        if (this.neutralStageComplete) {
                            this.completeConversion();
                        } else {
                            this.transitionToNeutral();
                        }
                    }
                }
            }, 0L, CONVERSION_TICK_INTERVAL);
        }

        /**
         * Convert the beacon into a neutral state, and update the progress bar.
         */
        private void transitionToNeutral() {
            this.neutralStageComplete = true;
            this.beacon.setState(BeaconState.NEUTRAL);
            this.phaseStartTime = System.currentTimeMillis();
            this.adjustedConversionTime = BeaconConversionListener.this.BASE_CONVERSION_TIME;

            // Get the conversion multipliers for the two teams
            double speedMultiplier = (double)this.converters.size();
            double humanSpeedMultiplier = BeaconConversionListener.this.plugin.getConfigManager().getBeaconHumanSpeedMultiplier();

            if (!this.isVampireConversion) {
                speedMultiplier *= humanSpeedMultiplier;
            }

            double finalStandMultiplier = BeaconConversionListener.this.plugin.getConfigManager().getBeaconFinalStandMultiplier();
            if (!this.isVampireConversion && BeaconConversionListener.this.plugin.getSessionManager().isHumansFinalStandActive() && this.converters.size() == 1) {
                speedMultiplier = finalStandMultiplier * humanSpeedMultiplier;
            }

            this.adjustedConversionTime = (long)((double)this.adjustedConversionTime / speedMultiplier);
            BeaconConversionListener.this.beaconManager.updateBeaconDisplay(this.beacon);
            BeaconConversionListener.this.beaconManager.saveBeacons();
            BeaconConversionListener.this.plugin.getBeaconMajorityManager().updateBeaconMajorityBonuses();

            this.updateBossBar(0.5, "neutral");

            BeaconSite.BeaconState previousState = BeaconConversionListener.this.getPreviousBeaconState(this.beacon, this.isVampireConversion);
            BeaconConversionListener.this.broadcastNeutralConversionToAll(this.beacon, previousState);
            Location beaconLoc = this.beacon.getLocation();

            if (beaconLoc != null) {
                beaconLoc.getWorld().playSound(beaconLoc, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
        }

        /**
         * Convert the beacon into one of its captured states.
         */
        private void completeConversion() {
            BeaconSite.BeaconState newState = this.isVampireConversion ? BeaconState.DESECRATED : BeaconState.HOLY;
            BeaconConversionListener.this.beaconManager.cancelPendingNeutralBroadcast(this.beacon.getName());

            long cooldownMs = BeaconConversionListener.this.plugin.getConfigManager().getBeaconConversionCooldownMs();
            this.beacon.changeState(newState, "Player conversion", BeaconConversionListener.this.plugin.getSessionManager(), cooldownMs);

            BeaconConversionListener.this.beaconManager.updateBeaconDisplay(this.beacon);
            BeaconConversionListener.this.beaconManager.saveBeacons();
            BeaconConversionListener.this.plugin.getBeaconMajorityManager().updateBeaconMajorityBonuses();

            // Check if this conversion is the first in the game lifetime
            BeaconConversionListener.this.beaconManager.triggerFirstBeaconConvertedEffects(this.beacon, this.isVampireConversion);

            // Inform the players that a beacon has been given to a team
            BeaconConversionListener.this.broadcastBeaconGainToTeam(this.beacon, newState);
            BeaconConversionListener.this.beaconManager.checkAndBroadcastCompleteControl();

            // Check if this conversion has triggered a final stand
            BeaconConversionListener.this.checkForFinalStand();

            // Check if this conversion has ended a final stand
            if (this.isVampireConversion && BeaconConversionListener.this.plugin.getSessionManager().isHumansFinalStandActive()) {
                BeaconConversionListener.this.disableHumansFinalStand();
            } else if (!this.isVampireConversion && BeaconConversionListener.this.plugin.getSessionManager().isVampiresEternalNightActive()) {
                BeaconConversionListener.this.disableVampiresEternalNight();
            }

            // Run the conversion sound effects
            Location beaconLoc = this.beacon.getLocation();
            if (beaconLoc != null) {
                Sound completionSound = this.isVampireConversion ? Sound.ENTITY_WITHER_SPAWN : Sound.BLOCK_BEACON_ACTIVATE;
                beaconLoc.getWorld().playSound(beaconLoc, completionSound, SoundCategory.BLOCKS, 1.0F, this.isVampireConversion ? 0.5F : 1.2F);
            }

            this.cleanup();
            BeaconConversionListener.this.activeConversions.remove(this.beacon.getName().toLowerCase());
        }

        /**
         * Determine if a player from the opposing team has canceled the beacon conversion.
         *
         * @return {@code true} if a player has interrupted the beacon conversion process.
         */
        private boolean checkForEnemyInterferenceInArea() {
            Location beaconLoc = this.beacon.getLocation();

            if (beaconLoc != null) {
                for (Player player : BeaconConversionListener.this.getPlayersInRange(beaconLoc, BEACON_INTERACTION_RANGE)) {
                    if (!this.converters.contains(player.getUniqueId())) {
                        boolean playerIsVampire = BeaconConversionListener.this.vampireManager.isVampireStage2OrHigher(player);
                        boolean playerIsHuman = BeaconConversionListener.this.vampireManager.isHuman(player);

                        if ((playerIsVampire || playerIsHuman) && (!playerIsVampire || !player.hasPotionEffect(PotionEffectType.INVISIBILITY))) {
                            if (this.isVampireConversion && playerIsHuman || !this.isVampireConversion && playerIsVampire) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        }

        /**
         * Retrieve the list of players participating in the beacon conversion.
         *
         * @return A set of player ids from those crouching near the beacon.
         */
        public Set<UUID> getConverters() {
            return this.converters;
        }

        /**
         * Retrieve whether this beacon conversion is being done by vampires.
         *
         * @return {@code true} if vampires are converting the beacon.
         */
        public boolean isVampireConversion() {
            return this.isVampireConversion;
        }

        /**
         * Retrieve the beacon conversion task.
         *
         * @return A Bukkit {@code task} monitoring the beacon conversion progress.
         */
        public BukkitTask getTask() {
            return this.task;
        }
    }
}
