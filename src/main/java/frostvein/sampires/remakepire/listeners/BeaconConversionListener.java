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
    private static final long CONVERSION_TICK_INTERVAL = 20L;
    private static final double BEACON_INTERACTION_RANGE = (double)3.0F;

    public BeaconConversionListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.beaconManager = plugin.getBeaconManager();
        this.vampireManager = plugin.getVampireManager();
        this.sessionManager = plugin.getSessionManager();
        this.activeConversions = new HashMap();
        this.BASE_CONVERSION_TIME = plugin.getConfigManager().getBeaconConversionTimeMs();
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (event.isSneaking()) {
            this.handlePlayerStartCrouching(player);
        } else {
            this.handlePlayerStopCrouching(player);
        }

    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            this.checkPlayerProximityToConversions(player);
        }

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.handlePlayerStopCrouching(event.getPlayer());
    }

    private void handlePlayerStartCrouching(Player player) {
        if (this.vampireManager.isHuman(player) || this.vampireManager.isVampire(player)) {
            if (player.getGameMode() != GameMode.SPECTATOR) {
                for(BeaconSite beacon : this.beaconManager.getBeaconsInRange(player.getLocation(), (double)3.0F)) {
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

    private void handlePlayerStopCrouching(Player player) {
        Iterator<Map.Entry<String, ConversionData>> iterator = this.activeConversions.entrySet().iterator();

        while(iterator.hasNext()) {
            Map.Entry<String, ConversionData> entry = (Map.Entry)iterator.next();
            ConversionData data = (ConversionData)entry.getValue();
            if (data.getConverters().contains(player.getUniqueId())) {
                data.removeConverter(player.getUniqueId());
                if (data.getConverters().isEmpty()) {
                    this.cancelConversion((String)entry.getKey(), data);
                    iterator.remove();
                } else {
                    data.recalculateConversionTime();
                }
            }
        }

    }

    private void checkPlayerProximityToConversions(Player player) {
        for(Map.Entry<String, ConversionData> entry : this.activeConversions.entrySet()) {
            ConversionData data = (ConversionData)entry.getValue();
            BeaconSite beacon = this.beaconManager.getBeacon((String)entry.getKey());
            if (beacon != null) {
                if (data.getConverters().contains(player.getUniqueId())) {
                    Location beaconLoc = beacon.getLocation();
                    if (beaconLoc == null || !beaconLoc.getWorld().equals(player.getWorld()) || beaconLoc.distance(player.getLocation()) > (double)3.0F || player.getGameMode() == GameMode.SPECTATOR) {
                        this.handlePlayerStopCrouching(player);
                        break;
                    }
                }

                if (this.checkForEnemyInterference(player, beacon, data)) {
                    this.cancelConversion((String)entry.getKey(), data);
                    this.activeConversions.remove(entry.getKey());
                    break;
                }
            }
        }

    }

    private boolean checkForEnemyInterference(Player player, BeaconSite beacon, ConversionData data) {
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return false;
        } else {
            Location beaconLoc = beacon.getLocation();
            if (beaconLoc != null && beaconLoc.getWorld().equals(player.getWorld()) && !(beaconLoc.distance(player.getLocation()) > (double)3.0F)) {
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
                        boolean isEnemyInterference = data.isVampireConversion() && playerIsHuman || !data.isVampireConversion() && playerIsVampire;
                        return isEnemyInterference;
                    }
                }
            } else {
                return false;
            }
        }
    }

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
                        for(Player nearbyPlayer : this.getPlayersInRange(beacon.getLocation(), (double)3.0F)) {
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
                String var10001 = beacon.getName();
                player.sendMessage("§c⏰ Beacon " + var10001 + " cannot be converted yet. Cooldown remaining: §e" + beacon.getRemainingCooldownString(this.sessionManager) + " (session time)");
                return false;
            }
        }
    }

    private void startBeaconConversion(Player player, BeaconSite beacon) {
        String beaconKey = beacon.getName().toLowerCase();
        boolean isVampireConversion = this.vampireManager.isVampire(player);
        ConversionData data = (ConversionData)this.activeConversions.get(beaconKey);
        if (data == null) {
            data = new ConversionData(beacon, isVampireConversion);
            this.activeConversions.put(beaconKey, data);
        }

        data.addConverter(player.getUniqueId());
        data.recalculateConversionTime();
        player.sendMessage("§eConverting beacon §f" + beacon.getName() + "§e... Stay crouched.");
    }

    private void cancelConversion(String beaconKey, ConversionData data) {
        data.cleanup();
        BeaconSite beacon = this.beaconManager.getBeacon(beaconKey);
        if (beacon != null) {
            this.broadcastToBeaconArea(beacon, "§7Beacon conversion of §e" + beacon.getName() + " §7has been interrupted!");
        }

    }

    private List<Player> getPlayersInRange(Location center, double range) {
        List<Player> players = new ArrayList();
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

    private void broadcastToBeaconArea(BeaconSite beacon, String message) {
        Location beaconLoc = beacon.getLocation();
        if (beaconLoc != null) {
            for(Player player : this.getPlayersInRange(beaconLoc, (double)(beacon.getCaptureRadius() * 2))) {
                player.sendMessage(message);
            }

        }
    }

    private void broadcastNeutralConversionToAll(BeaconSite beacon, BeaconSite.BeaconState previousState) {
        this.beaconManager.broadcastNeutralConversionToAll(beacon, previousState);
    }

    private BeaconSite.BeaconState getPreviousBeaconState(BeaconSite beacon, boolean vampireConversion) {
        return vampireConversion ? BeaconState.HOLY : BeaconState.DESECRATED;
    }

    private void broadcastBeaconGainToTeam(BeaconSite beacon, BeaconSite.BeaconState newState) {
        for(Player player : this.plugin.getServer().getOnlinePlayers()) {
            if (newState == BeaconState.HOLY) {
                player.sendMessage("§aBeacon §e" + beacon.getName() + " §ahas been blessed with divine energy.");
            } else if (newState == BeaconState.DESECRATED) {
                player.sendMessage("§4Beacon §e" + beacon.getName() + " §4has been consumed by dark forces.");
            }
        }

    }

    private void showConversionParticles(BeaconSite beacon, boolean isVampireConversion, double progress) {
        Location particleLoc = beacon.getParticleLocation();
        if (particleLoc != null) {
            if (isVampireConversion) {
                particleLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, particleLoc, (int)((double)5.0F * progress), (double)0.5F, (double)0.5F, (double)0.5F, 0.01);
                particleLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, (int)((double)3.0F * progress), 0.3, 0.3, 0.3, 0.02);
            } else {
                particleLoc.getWorld().spawnParticle(Particle.WHITE_ASH, particleLoc, (int)((double)8.0F * progress), (double)0.5F, (double)0.5F, (double)0.5F, 0.01);
                particleLoc.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, (int)((double)5.0F * progress), 0.3, 0.3, 0.3, 0.1);
            }

        }
    }

    public void shutdown() {
        int conversionCount = this.activeConversions.size();

        for(ConversionData data : this.activeConversions.values()) {
            data.cleanup();
        }

        this.activeConversions.clear();
        this.plugin.getLogger().info("BeaconConversionListener shutdown - cleaned up " + conversionCount + " active conversions.");
    }

    private void checkForHumansFinalStand() {
        Map<BeaconSite.BeaconState, Integer> stateStats = this.beaconManager.getStateStats();
        int holyCount = (Integer)stateStats.get(BeaconState.HOLY);
        int desecratedCount = (Integer)stateStats.get(BeaconState.DESECRATED);
        int permanentlyDesecratedCount = (Integer)stateStats.getOrDefault(BeaconState.PERMANENTLY_DESECRATED, 0);
        int totalEvilCount = desecratedCount + permanentlyDesecratedCount;
        int totalBeacons = this.beaconManager.getAllBeacons().size();
        if (holyCount >= totalBeacons && totalBeacons > 0) {
            this.triggerHumansFinalStand();
        }

        if (totalEvilCount >= totalBeacons && totalBeacons > 0) {
            this.triggerVampiresEternalNight();
        }

    }

    private void triggerHumansFinalStand() {
        this.plugin.getLogger().info("HUMANS FINAL STAND TRIGGERED - All 7 beacons are holy!");

        for(Player player : this.plugin.getServer().getOnlinePlayers()) {
            player.sendTitle("§c§lALL BEACONS SANCTIFIED", "§eThe divine light weakens all creatures of darkness", 20, 100, 40);
            player.sendMessage("§e All beacons now shine with divine energy.");
            player.sendMessage("§e Evil has been weakened by the overwhelming holy presence.");
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 1.0F, 0.7F);
        }

        for(Player player : this.plugin.getServer().getOnlinePlayers()) {
            if (this.vampireManager.isVampire(player)) {
                player.getAttribute(Attribute.MAX_HEALTH).setBaseValue((double)6.0F);
                if (player.getHealth() > (double)6.0F) {
                    player.setHealth((double)6.0F);
                }
            }
        }

        this.plugin.getSessionManager().setHumansFinalStandActive(true);
    }

    private void disableHumansFinalStand() {
        this.plugin.getLogger().info("HUMANS FINAL STAND ENDED - A vampire has converted a beacon!");

        for(Player player : this.plugin.getServer().getOnlinePlayers()) {
            player.sendTitle("§4§lFINAL STAND BROKEN", "§cDarkness pushes back against the light", 20, 80, 20);
            player.sendMessage("§cA vampire has broken through the holy defenses.");
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.MASTER, 1.0F, 0.5F);
        }

        for(Player player : this.plugin.getServer().getOnlinePlayers()) {
            if (this.vampireManager.isVampire(player)) {
                player.getAttribute(Attribute.MAX_HEALTH).setBaseValue((double)20.0F);
            }
        }

        this.plugin.getSessionManager().setHumansFinalStandActive(false);
    }

    private void triggerVampiresEternalNight() {
        this.plugin.getLogger().info("VAMPIRES ETERNAL NIGHT TRIGGERED - All 7 beacons are desecrated!");

        for(Player player : this.plugin.getServer().getOnlinePlayers()) {
            player.sendTitle("§4§lETERNAL NIGHT FALLS", "§cThe darkness consumes all hope", 20, 100, 40);
            player.sendMessage("§c All beacons now pulse with unholy energy.");
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.MASTER, 1.0F, 0.5F);
        }

        for(Player player : this.plugin.getServer().getOnlinePlayers()) {
            if (this.vampireManager.isHuman(player) && player.getGameMode() == GameMode.SURVIVAL) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, -1, 0, false, false, true));
            }
        }

        this.plugin.getSessionManager().setVampiresEternalNightActive(true);
    }

    private void disableVampiresEternalNight() {
        this.plugin.getLogger().info("VAMPIRES ETERNAL NIGHT ENDED - A human has sanctified a beacon!");

        for(Player player : this.plugin.getServer().getOnlinePlayers()) {
            player.sendTitle("§6§lLIGHT RETURNS", "§eA beacon shines with holy light once more", 20, 80, 20);
            player.sendMessage("§aA human has broken through the darkness.");
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 1.0F, 1.2F);
        }

        for(Player player : this.plugin.getServer().getOnlinePlayers()) {
            if (this.vampireManager.isHuman(player)) {
                player.removePotionEffect(PotionEffectType.DARKNESS);
            }
        }

        this.plugin.getSessionManager().setVampiresEternalNightActive(false);
    }

    private class ConversionData {
        private final BeaconSite beacon;
        private final boolean vampireConversion;
        private final Set<UUID> converters;
        private final long startTime;
        private long phaseStartTime;
        private BukkitTask task;
        private long adjustedConversionTime;
        private boolean neutralStageComplete;
        private BossBar bossBar;

        public ConversionData(BeaconSite beacon, boolean vampireConversion) {
            this.beacon = beacon;
            this.vampireConversion = vampireConversion;
            this.converters = new HashSet();
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

        private void createBossBar() {
            String beaconName = this.beacon.getName();
            BarColor color = this.vampireConversion ? BarColor.RED : BarColor.WHITE;
            String title = this.vampireConversion ? "§4Desecrating Beacon: §f" + beaconName : "§fConsecrating Beacon: §f" + beaconName;
            this.bossBar = BeaconConversionListener.this.plugin.getServer().createBossBar(title, color, BarStyle.SOLID, new BarFlag[0]);
            this.bossBar.setProgress(this.neutralStageComplete ? (double)0.5F : (double)0.0F);
        }

        public void addConverter(UUID playerId) {
            this.converters.add(playerId);
            Player player = BeaconConversionListener.this.plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                this.bossBar.addPlayer(player);
            }

        }

        public void removeConverter(UUID playerId) {
            this.converters.remove(playerId);
            Player player = BeaconConversionListener.this.plugin.getServer().getPlayer(playerId);
            if (player != null) {
                this.bossBar.removePlayer(player);
            }

        }

        public void updateBossBar(double progress, String phase) {
            this.bossBar.setProgress(Math.min((double)1.0F, progress));
            String beaconName = this.beacon.getName();
            String title;
            if (phase.equals("neutral")) {
                title = "§7Cleansing Beacon: §f" + beaconName + " §7(" + (int)(progress * (double)100.0F) + "%)";
                this.bossBar.setColor(BarColor.YELLOW);
            } else if (this.vampireConversion) {
                title = "§4Desecrating Beacon: §f" + beaconName + " §4(" + (int)(progress * (double)100.0F) + "%)";
                this.bossBar.setColor(BarColor.RED);
            } else {
                title = "§fConsecrating Beacon: §f" + beaconName + " §f(" + (int)(progress * (double)100.0F) + "%)";
                this.bossBar.setColor(BarColor.WHITE);
            }

            this.bossBar.setTitle(title);
        }

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

        public void recalculateConversionTime() {
            long currentTime = System.currentTimeMillis();
            long existingElapsed = currentTime - this.phaseStartTime;
            double currentProgress = Math.min((double)1.0F, (double)existingElapsed / (double)this.adjustedConversionTime);
            long oldAdjustedTime = this.adjustedConversionTime;
            double speedMultiplier = (double)this.converters.size();
            double humanSpeedMultiplier = BeaconConversionListener.this.plugin.getConfigManager().getBeaconHumanSpeedMultiplier();
            if (!this.vampireConversion) {
                speedMultiplier *= humanSpeedMultiplier;
            }

            double finalStandMultiplier = BeaconConversionListener.this.plugin.getConfigManager().getBeaconFinalStandMultiplier();
            if (!this.vampireConversion && BeaconConversionListener.this.plugin.getSessionManager().isHumansFinalStandActive() && this.converters.size() == 1) {
                speedMultiplier = finalStandMultiplier * humanSpeedMultiplier;
            }

            long baseTime = this.neutralStageComplete ? BeaconConversionListener.this.BASE_CONVERSION_TIME : BeaconConversionListener.this.BASE_CONVERSION_TIME / 2L;
            this.adjustedConversionTime = (long)((double)baseTime / speedMultiplier);
            long preservedElapsed = (long)((double)this.adjustedConversionTime * currentProgress);
            this.phaseStartTime = currentTime - preservedElapsed;
            this.startConversionTask();
        }

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
                    double rawProgress = Math.min((double)1.0F, (double)elapsed / (double)this.adjustedConversionTime);
                    double displayProgress;
                    String currentPhase;
                    if (this.neutralStageComplete) {
                        displayProgress = (double)0.5F + rawProgress * (double)0.5F;
                        currentPhase = this.vampireConversion ? "desecrating" : "consecrating";
                    } else {
                        displayProgress = rawProgress * (double)0.5F;
                        currentPhase = "cleansing";
                    }

                    this.updateBossBar(displayProgress, currentPhase);
                    BeaconConversionListener.this.showConversionParticles(this.beacon, this.vampireConversion, displayProgress);
                    if (elapsed >= this.adjustedConversionTime) {
                        if (this.neutralStageComplete) {
                            this.completeConversion();
                        } else {
                            this.transitionToNeutral();
                        }

                    }
                }
            }, 0L, 20L);
        }

        private void transitionToNeutral() {
            this.neutralStageComplete = true;
            this.beacon.setState(BeaconState.NEUTRAL);
            this.phaseStartTime = System.currentTimeMillis();
            this.adjustedConversionTime = BeaconConversionListener.this.BASE_CONVERSION_TIME;
            double speedMultiplier = (double)this.converters.size();
            double humanSpeedMultiplier = BeaconConversionListener.this.plugin.getConfigManager().getBeaconHumanSpeedMultiplier();
            if (!this.vampireConversion) {
                speedMultiplier *= humanSpeedMultiplier;
            }

            double finalStandMultiplier = BeaconConversionListener.this.plugin.getConfigManager().getBeaconFinalStandMultiplier();
            if (!this.vampireConversion && BeaconConversionListener.this.plugin.getSessionManager().isHumansFinalStandActive() && this.converters.size() == 1) {
                speedMultiplier = finalStandMultiplier * humanSpeedMultiplier;
            }

            this.adjustedConversionTime = (long)((double)this.adjustedConversionTime / speedMultiplier);
            BeaconConversionListener.this.beaconManager.updateBeaconDisplay(this.beacon);
            BeaconConversionListener.this.beaconManager.saveBeacons();
            BeaconConversionListener.this.plugin.getBeaconMajorityManager().updateBeaconMajorityBonuses();
            this.updateBossBar((double)0.5F, "neutral");
            BeaconSite.BeaconState previousState = BeaconConversionListener.this.getPreviousBeaconState(this.beacon, this.vampireConversion);
            BeaconConversionListener.this.broadcastNeutralConversionToAll(this.beacon, previousState);
            Location beaconLoc = this.beacon.getLocation();
            if (beaconLoc != null) {
                beaconLoc.getWorld().playSound(beaconLoc, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }

        }

        private void completeConversion() {
            BeaconSite.BeaconState newState = this.vampireConversion ? BeaconState.DESECRATED : BeaconState.HOLY;
            BeaconConversionListener.this.beaconManager.cancelPendingNeutralBroadcast(this.beacon.getName());
            long cooldownMs = BeaconConversionListener.this.plugin.getConfigManager().getBeaconConversionCooldownMs();
            this.beacon.changeState(newState, "Player conversion", BeaconConversionListener.this.plugin.getSessionManager(), cooldownMs);
            BeaconConversionListener.this.beaconManager.updateBeaconDisplay(this.beacon);
            BeaconConversionListener.this.beaconManager.saveBeacons();
            BeaconConversionListener.this.plugin.getBeaconMajorityManager().updateBeaconMajorityBonuses();
            if (newState == BeaconState.HOLY || newState == BeaconState.DESECRATED) {
                BeaconConversionListener.this.beaconManager.triggerFirstBeaconConvertedEffects(this.beacon, this.vampireConversion);
            }

            BeaconConversionListener.this.broadcastBeaconGainToTeam(this.beacon, newState);
            BeaconConversionListener.this.beaconManager.checkAndBroadcastCompleteControl();
            if (newState == BeaconState.HOLY) {
                BeaconConversionListener.this.checkForHumansFinalStand();
            }

            if (newState == BeaconState.DESECRATED) {
                BeaconConversionListener.this.checkForHumansFinalStand();
            }

            if (this.vampireConversion && BeaconConversionListener.this.plugin.getSessionManager().isHumansFinalStandActive()) {
                BeaconConversionListener.this.disableHumansFinalStand();
            }

            if (!this.vampireConversion && BeaconConversionListener.this.plugin.getSessionManager().isVampiresEternalNightActive()) {
                BeaconConversionListener.this.disableVampiresEternalNight();
            }

            Location beaconLoc = this.beacon.getLocation();
            if (beaconLoc != null) {
                Sound completionSound = this.vampireConversion ? Sound.ENTITY_WITHER_SPAWN : Sound.BLOCK_BEACON_ACTIVATE;
                beaconLoc.getWorld().playSound(beaconLoc, completionSound, SoundCategory.BLOCKS, 1.0F, this.vampireConversion ? 0.5F : 1.2F);
            }

            this.cleanup();
            BeaconConversionListener.this.activeConversions.remove(this.beacon.getName().toLowerCase());
        }

        private boolean checkForEnemyInterferenceInArea() {
            Location beaconLoc = this.beacon.getLocation();
            if (beaconLoc == null) {
                return false;
            } else {
                for(Player player : BeaconConversionListener.this.getPlayersInRange(beaconLoc, (double)3.0F)) {
                    if (!this.converters.contains(player.getUniqueId())) {
                        boolean playerIsVampire = BeaconConversionListener.this.vampireManager.isVampireStage2OrHigher(player);
                        boolean playerIsHuman = BeaconConversionListener.this.vampireManager.isHuman(player);
                        if ((playerIsVampire || playerIsHuman) && (!playerIsVampire || !player.hasPotionEffect(PotionEffectType.INVISIBILITY))) {
                            boolean isEnemyInterference = this.vampireConversion && playerIsHuman || !this.vampireConversion && playerIsVampire;
                            if (isEnemyInterference) {
                                return true;
                            }
                        }
                    }
                }

                return false;
            }
        }

        public Set<UUID> getConverters() {
            return this.converters;
        }

        public boolean isVampireConversion() {
            return this.vampireConversion;
        }

        public BukkitTask getTask() {
            return this.task;
        }
    }
}
