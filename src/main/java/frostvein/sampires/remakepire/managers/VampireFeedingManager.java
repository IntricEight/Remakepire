package frostvein.sampires.remakepire.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class VampireFeedingManager implements Listener {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final ThirstManager thirstManager;
    private final ConfigManager configManager;
    // Controls the distance players can be while feeding
    private static final double FEEDING_RANGE = 1.5;
    // Controls the time a vampire needs to be crouching nearby before feeding begins
    private static final int PREPARATION_TIME = 5;
    // Controls how quickly humans are hurt by feeding
    private static final double HEALTH_DRAIN_PER_SECOND = 1.0;
    // Controls how quickly vampires gain blood from feeding
    private static final int THIRST_GAIN_PER_SECOND = 2;
    private final Map<UUID, FeedingSession> activeSessions = new HashMap<>();
    private final Map<UUID, Integer> sessionFeedingThirst = new HashMap<>();

    /**
     * Create an instance of the Vampire Feeding manager.
     *
     * @param plugin the host plugin object.
     */
    public VampireFeedingManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.thirstManager = plugin.getThirstManager();
        this.configManager = plugin.getConfigManager();

        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.startFeedingDetectionTask();
        plugin.getLogger().info("VampireFeedingManager initialized");
    }

    private void startFeedingDetectionTask() {
        (new BukkitRunnable() {
            public void run() {
                VampireFeedingManager.this.checkFeedingSessions();
            }
        }).runTaskTimer(this.plugin, 20L, 20L);
    }

    private void checkFeedingSessions() {
        for(FeedingSession session : (FeedingSession[])this.activeSessions.values().toArray(new FeedingSession[0])) {
            Player vampire = Bukkit.getPlayer(session.vampireId);
            Player target = Bukkit.getPlayer(session.targetId);
            if (vampire != null && target != null && vampire.isOnline() && target.isOnline() && vampire.getGameMode() != GameMode.SPECTATOR) {
                if (target.getGameMode() != GameMode.SURVIVAL) {
                    vampire.sendMessage("§cYou cannot feed on players who are not in survival mode.");
                    this.cancelFeedingSession(session);
                } else if (!this.plugin.getSessionManager().isSessionActive()) {
                    this.cancelFeedingSession(session);
                } else if (vampire.isSneaking() && this.isInFeedingRange(vampire, target)) {
                    if (this.vampireManager.isVampire(vampire) && (this.vampireManager.isHuman(target) || this.vampireManager.isVampire(target))) {
                        if (session.phase == VampireFeedingManager.FeedingPhase.PREPARATION) {
                            this.processPreparationPhase(session, vampire, target);
                        } else if (session.phase == VampireFeedingManager.FeedingPhase.ACTIVE_FEEDING) {
                            this.processActiveFeedingPhase(session, vampire, target);
                        }
                    } else {
                        this.cancelFeedingSession(session);
                    }
                } else {
                    this.cancelFeedingSession(session);
                }
            } else {
                this.cancelFeedingSession(session);
            }
        }
    }

    private void processPreparationPhase(FeedingSession session, Player vampire, Player target) {
        --session.preparationSecondsRemaining;
        String preparationMessage;
        if (this.vampireManager.isHuman(target)) {
            preparationMessage = "§8Preparing to feed... " + VampireAbilityManager.formatTime((long)session.preparationSecondsRemaining) + " remaining";
        } else {
            preparationMessage = "§8Preparing to siphon... " + VampireAbilityManager.formatTime((long)session.preparationSecondsRemaining) + " remaining";
        }

        this.plugin.getSessionManager().sendActionBar(vampire, preparationMessage);
        if (session.preparationSecondsRemaining <= 0) {
            session.phase = VampireFeedingManager.FeedingPhase.ACTIVE_FEEDING;
            this.plugin.getSessionManager().sendActionBar(vampire, "");
            if (this.vampireManager.isHuman(target)) {
                vampire.sendMessage("§4§lYou begin feeding on " + target.getName() + "!");
                target.sendMessage("§c§lYou feel a vampire draining your life force!");
                target.sendMessage("§7Move away or break the vampire's crouch to escape!");
            } else {
                vampire.sendMessage("§4§lYou begin siphoning from " + target.getName() + "!");
                target.sendMessage("§c§lYou feel another vampire siphoning your essence!");
                target.sendMessage("§7Move away or break the vampire's crouch to escape!");
            }

            vampire.getWorld().playSound(vampire.getLocation(), Sound.ENTITY_WITCH_DRINK, SoundCategory.PLAYERS, 1.0F, 0.8F);
        }
    }

    private void processActiveFeedingPhase(FeedingSession session, Player vampire, Player target) {
        if (this.vampireManager.isHuman(target)) {
            UUID vampireId = vampire.getUniqueId();
            int currentSessionThirst = this.sessionFeedingThirst.getOrDefault(vampireId, 0);
            int maxFeedingThirst = this.configManager.getMaxFeedingThirstPerSession();

            if (currentSessionThirst >= maxFeedingThirst) {
                vampire.sendMessage("§cYour thirst is quenched, for now. You are unable to drink any more blood from feeding until the next session.");
                this.cancelFeedingSession(session);
                return;
            }

            double currentHealth = target.getHealth();
            if (currentHealth <= 1.0) {
                this.handleFeedingDeath(session, vampire, target);
                return;
            }

            double newHealth = currentHealth - HEALTH_DRAIN_PER_SECOND;
            target.setHealth(newHealth);
            int currentFoodLevel = target.getFoodLevel();
            int newFoodLevel = Math.max(0, currentFoodLevel - 1);
            target.setFoodLevel(newFoodLevel);
            int thirstToGive = Math.min(2, maxFeedingThirst - currentSessionThirst);

            this.thirstManager.modifyQuench(vampire, thirstToGive);
            this.sessionFeedingThirst.put(vampireId, currentSessionThirst + thirstToGive);

            this.plugin.getSessionManager().sendActionBar(vampire, "§4Feeding...");
            this.plugin.getSessionManager().sendActionBar(target, "§cYour life force is being drained...");

        } else {
            if (target.getExp() <= 0.1F) {
                vampire.sendMessage("§cThe vampiric essence has become too low to continue siphoning from.");
                this.cancelFeedingSession(session);
                return;
            }

            this.thirstManager.modifyQuench(target, -1 * THIRST_GAIN_PER_SECOND);
            this.thirstManager.modifyQuench(vampire, THIRST_GAIN_PER_SECOND);
            this.plugin.getSessionManager().sendActionBar(vampire, "§4Siphoning...");
            this.plugin.getSessionManager().sendActionBar(target, "§cYour vampiric essence is being siphoned...");
        }

        float pitch = session.highPitch ? 0.8F : 0.6F;
        vampire.getWorld().playSound(vampire.getLocation(), Sound.ENTITY_WITCH_DRINK, SoundCategory.PLAYERS, 1.0F, pitch);
        session.highPitch = !session.highPitch;
    }

    private void handleFeedingDeath(FeedingSession session, Player vampire, Player target) {
        if (this.plugin.getPermadeathManager().hasAbsolutePermadeathEnabled(target)) {
            vampire.sendMessage("§4You watch the light of " + target.getName() + "'s eyes fade, and extinguish. Lost forever.");
            target.sendMessage("§7The world grows dim, blurry, you feel a darkness reach out, offering you one last chance to live, as a creature of the night... But you refuse... And slip under the veil of the afterlife.");
            target.addScoreboardTag("PermadeathChosen");
            target.setHealth(0.0);
            this.cancelFeedingSession(session);

        } else if (this.plugin.getBeetrootManager().hasBeetrootImmunity(target)) {
            vampire.sendMessage("§cThe sting of garlic sears at your gums, protecting your meal from your bite.");

            if (this.plugin.getVampireTurningManager().isTurningEnabled(vampire)) {
                vampire.sendMessage("§cYou have failed to turn " + target.getName() + " - they will die as a human, wounded.");
            } else {
                vampire.sendMessage("§cYou have killed " + target.getName() + " - they will die as a human, wounded.");
            }

            vampire.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 3600, 9, false, false));
            target.sendMessage("§a§lYour garlic immunity protects you from turning.");
            target.sendMessage("§aYou will respawn as a human, not as a cursed creature.");
            target.setHealth(0.0);
            this.cancelFeedingSession(session);

        } else if (!this.plugin.getVampireTurningManager().isTurningEnabled(vampire)) {
            try {
                Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                Objective deathObjective = mainScoreboard.getObjective("vsmp_death");

                if (deathObjective != null) {
                    int currentDeaths = deathObjective.getScore(target.getName()).getScore();

                    if (currentDeaths >= 5) {
                        vampire.sendMessage("§4You watch the light of " + target.getName() + "'s eyes fade, and extinguish. Lost forever.");
                        target.sendMessage("§7The world grows dim, blurry, you feel a darkness reach out, offering you one last chance to live, as a creature of the night... But you refuse... And slip under the veil of the afterlife.");
                        target.addScoreboardTag("PermadeathChosen");
                        target.setHealth(0.0);

                        this.cancelFeedingSession(session);
                        return;
                    }
                }
            } catch (Exception e) {
                this.plugin.getLogger().warning("Failed to check death count for " + target.getName() + ": " + e.getMessage());
            }

            vampire.sendMessage("§cYou have killed " + target.getName() + ". They will respawn as a human, wounded.");
            target.sendMessage("§7You have been slain by a vampire, but they do not turn you...");
            target.setHealth(0.0);
            this.cancelFeedingSession(session);

        } else if (target.getScoreboardTags().contains("CuredVampire")) {
            vampire.sendMessage("§4You taste the blood of " + target.getName() + ", but it rejects your curse...");
            vampire.sendMessage("§4They have been cleansed by holy power - their soul slips beyond your grasp, lost forever.");
            target.sendMessage("§7The darkness reaches for you, but the holy blessing protects your soul...");
            target.sendMessage("§7You feel yourself slipping away, into a peaceful sleep.");
            target.addScoreboardTag("PermadeathChosen");
            target.setHealth(0.0);

            this.cancelFeedingSession(session);

        } else if (this.plugin.getPermadeathManager().hasPermadeathEnabled(target)) {
            vampire.sendMessage("§4You watch the light of " + target.getName() + "'s eyes fade, and extinguish. Lost forever.");
            target.sendMessage("§7The world grows dim, blurry, you feel a darkness reach out, offering you one last chance to live, as a creature of the night... But you refuse... And slip under the veil of the afterlife.");
            target.addScoreboardTag("PermadeathChosen");
            target.setHealth(0.0);

            this.cancelFeedingSession(session);

        } else {
            this.vampireManager.performVampireTurning(target, vampire);
            int killThirst = this.thirstManager.getKillThirstReward(vampire, target);
            this.thirstManager.modifyQuench(vampire, killThirst, true);

            vampire.sendMessage("§cYou feel the last drops of life force leave " + target.getName() + ".");
            vampire.sendMessage("§cThey have become a creature of the night...");

            if (this.plugin.getVampireTrackingManager() != null) {
                this.plugin.getVampireTrackingManager().startTrackingNewVampire(target);
            }

            this.cancelFeedingSession(session);
            this.plugin.getLogger().info("Vampire " + vampire.getName() + " transformed " + target.getName() + " into a vampire through feeding");
        }
    }

    private boolean isInFeedingRange(Player vampire, Player target) {
        if (!vampire.getWorld().equals(target.getWorld())) {
            return false;
        } else {
            return vampire.getLocation().distance(target.getLocation()) <= FEEDING_RANGE;
        }
    }

    private void attemptStartFeeding(Player vampire) {
        if (!this.activeSessions.containsKey(vampire.getUniqueId())) {
            if (vampire.isSneaking()) {
                if (this.vampireManager.isVampire(vampire)) {
                    UUID vampireId = vampire.getUniqueId();
                    int currentSessionThirst = this.sessionFeedingThirst.getOrDefault(vampireId, 0);

                    if (currentSessionThirst >= this.configManager.getMaxFeedingThirstPerSession()) {
                        vampire.sendMessage("§cYour thirst is quenched, for now. You are unable to drink any more blood from feeding until the next session.");
                    } else {
                        int humansChecked = 0;

                        for(Player nearbyPlayer : vampire.getWorld().getPlayers()) {
                            if (!nearbyPlayer.equals(vampire) && nearbyPlayer.getGameMode() == GameMode.SURVIVAL) {
                                ++humansChecked;
                                double distance = vampire.getLocation().distance(nearbyPlayer.getLocation());
                                boolean isHuman = this.vampireManager.isHuman(nearbyPlayer);
                                boolean inRange = this.isInFeedingRange(vampire, nearbyPlayer);
                                boolean isVampire = this.vampireManager.isVampire(nearbyPlayer);

                                if ((isHuman || isVampire) && inRange) {
                                    if (isVampire && nearbyPlayer.getExp() <= 0.1F) {
                                        vampire.sendMessage("§cThe vampiric essence has become too low to continue siphoning from.");
                                        return;
                                    }

                                    FeedingSession session = new FeedingSession(vampire.getUniqueId(), nearbyPlayer.getUniqueId());
                                    this.activeSessions.put(vampire.getUniqueId(), session);
                                    if (isHuman) {
                                        vampire.sendMessage("§8You begin preparing to feed on " + nearbyPlayer.getName() + "...");
                                    } else {
                                        vampire.sendMessage("§8You begin preparing to siphon from " + nearbyPlayer.getName() + "...");
                                    }

                                    vampire.sendMessage("§7Stay crouched within range for " + VampireAbilityManager.formatTime(5L));
                                    this.plugin.getLogger().info("Vampire " + vampire.getName() + " started feeding on " + nearbyPlayer.getName());
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void cancelFeedingSession(FeedingSession session) {
        Player vampire = Bukkit.getPlayer(session.vampireId);
        Player target = Bukkit.getPlayer(session.targetId);

        if (target != null && target.isOnline() && session.phase == VampireFeedingManager.FeedingPhase.ACTIVE_FEEDING) {
            target.sendMessage("§aYou no longer feel a vampire draining your life force");
        }

        this.activeSessions.remove(session.vampireId);
    }

    public void cancelFeedingSessionByTarget(Player target) {
        UUID targetId = target.getUniqueId();

        for(FeedingSession session : this.activeSessions.values().toArray(new FeedingSession[0])) {
            if (session.targetId.equals(targetId)) {
                this.cancelFeedingSession(session);
                return;
            }
        }
    }

    /**
     *
     *
     * @param event a player beginning or stopping to sneak.
     */
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (this.vampireManager.isVampire(player) && player.getGameMode() != GameMode.SPECTATOR) {
            if (this.plugin.getSessionManager().isSessionActive()) {
                if (event.isSneaking()) {
                    Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                        if (player.isOnline() && player.isSneaking()) {
                            this.attemptStartFeeding(player);
                        }
                    }, 1L);

                } else {
                    FeedingSession session = this.activeSessions.get(player.getUniqueId());

                    if (session != null) {
                        this.cancelFeedingSession(session);
                    }
                }
            }
        }
    }

    /**
     *
     *
     * @param event a player leaving the world.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        FeedingSession session = this.activeSessions.get(playerId);

        if (session != null) {
            this.cancelFeedingSession(session);
        }

        for(FeedingSession activeSession : this.activeSessions.values().toArray(new FeedingSession[0])) {
            if (activeSession.targetId.equals(playerId)) {
                this.cancelFeedingSession(activeSession);
            }
        }
    }

    public int getActiveFeedingCount() {
        return this.activeSessions.size();
    }

    public boolean isVampireFeeding(Player vampire) {
        return this.activeSessions.containsKey(vampire.getUniqueId());
    }

    public boolean isPlayerBeingFedUpon(Player player) {
        UUID playerId = player.getUniqueId();
        return this.activeSessions.values().stream().anyMatch((session) -> session.targetId.equals(playerId));
    }

    public boolean isFeeding(Player player) {
        return this.activeSessions.containsKey(player.getUniqueId());
    }

    public void resetSessionFeedingThirst() {
        this.sessionFeedingThirst.clear();
        this.plugin.getLogger().info("Reset feeding thirst tracking for new session");
    }

    public int getSessionFeedingThirst(Player vampire) {
        return this.sessionFeedingThirst.getOrDefault(vampire.getUniqueId(), 0);
    }

    public int getMaxFeedingThirstPerSession() {
        return this.configManager.getMaxFeedingThirstPerSession();
    }

    public void shutdown() {
        for(FeedingSession session : this.activeSessions.values().toArray(new FeedingSession[0])) {
            this.cancelFeedingSession(session);
        }

        this.activeSessions.clear();
        this.sessionFeedingThirst.clear();
        this.plugin.getLogger().info("VampireFeedingManager shutdown complete");
    }

    private static class FeedingSession {
        public final UUID vampireId;
        public final UUID targetId;
        public final long startTime;
        public FeedingPhase phase;
        public BukkitTask task;
        public int preparationSecondsRemaining;
        public boolean highPitch;

        public FeedingSession(UUID vampireId, UUID targetId) {
            this.vampireId = vampireId;
            this.targetId = targetId;
            this.startTime = System.currentTimeMillis();
            this.phase = VampireFeedingManager.FeedingPhase.PREPARATION;
            this.preparationSecondsRemaining = PREPARATION_TIME;
            this.highPitch = true;
        }
    }

    private static enum FeedingPhase {
        PREPARATION,
        ACTIVE_FEEDING;
    }
}
