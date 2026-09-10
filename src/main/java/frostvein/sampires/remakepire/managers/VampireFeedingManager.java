package frostvein.sampires.remakepire.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import frostvein.sampires.remakepire.listeners.DeathHandler;

public class VampireFeedingManager implements Listener {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final ThirstManager thirstManager;
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
    private final Map<UUID, Boolean> justDiedToFeeding = new HashMap<>();

    /**
     * Create an instance of the Vampire Feeding manager.
     *
     * @param plugin the host plugin object.
     */
    public VampireFeedingManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.thirstManager = plugin.getThirstManager();

        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.startFeedingDetectionTask();
        plugin.logInfo("VampireFeedingManager initialized");
    }

    /**
     * Begin checking if a vampire is feeding on another player.
     */
    private void startFeedingDetectionTask() {
        (new BukkitRunnable() {
            public void run() {
                VampireFeedingManager.this.checkFeedingSessions();
            }
        }).runTaskTimer(this.plugin, 20L, 20L);
    }

    /**
     * Manage a vampire's feeding attempt.
     */
    private void checkFeedingSessions() {
        for (FeedingSession session : this.activeSessions.values().toArray(new FeedingSession[0])) {
            Player vampire = Bukkit.getPlayer(session.vampireId), target = Bukkit.getPlayer(session.targetId);

            if (vampire != null && target != null && vampire.isOnline() && target.isOnline() && vampire.getGameMode() != GameMode.SPECTATOR) {
                if (target.getGameMode() != GameMode.SURVIVAL) {
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

    /**
     * Manage the countdown until the vampire begins draining blood from the target.
     *
     * @param session the blood feeding session.
     * @param vampire the player feeding.
     * @param target the player about to lose health or blood.
     */
    private void processPreparationPhase(FeedingSession session, Player vampire, Player target) {
        if (target.getGameMode() != GameMode.SURVIVAL) {
            this.cancelFeedingSession(session);
            return;
        }

        --session.preparationSecondsRemaining;
        String preparationMessage;

        if (this.vampireManager.isHuman(target)) {
            preparationMessage = "Preparing to feed... " + VampireAbilityManager.formatTime(session.preparationSecondsRemaining) + " remaining";
        } else {
            preparationMessage = "Preparing to siphon... " + VampireAbilityManager.formatTime(session.preparationSecondsRemaining) + " remaining";
        }

        vampire.sendActionBar(Component.text(preparationMessage, NamedTextColor.DARK_GRAY));

        // Alert the players that the feeding has begun.
        if (session.preparationSecondsRemaining <= 0) {
            session.phase = VampireFeedingManager.FeedingPhase.ACTIVE_FEEDING;
            vampire.sendActionBar(Component.text(""));

            if (this.vampireManager.isHuman(target)) {
                vampire.sendMessage(Component.text("You begin feeding on " + target.getName() + "!", NamedTextColor.DARK_RED)
                        .decorate(TextDecoration.BOLD));

                target.sendMessage(Component.text("You feel a vampire draining your life force!", NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD));
                target.sendMessage(Component.text("Move away or break the vampire's crouch to escape!", NamedTextColor.GRAY));

            } else {
                vampire.sendMessage(Component.text("You begin siphoning from " + target.getName() + "!", NamedTextColor.DARK_RED)
                        .decorate(TextDecoration.BOLD));

                target.sendMessage(Component.text("You feel another vampire siphoning your essence!", NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD));
                target.sendMessage(Component.text("Move away or break the vampire's crouch to escape!", NamedTextColor.GRAY));
            }

            vampire.getWorld().playSound(vampire.getLocation(), Sound.ENTITY_WITCH_DRINK, SoundCategory.PLAYERS, 1.0F, 0.8F);
        }
    }

    /**
     * Manage the draining process as the vampire feeds.
     *
     * @param session the blood feeding session.
     * @param vampire the player feeding.
     * @param target the player losing health or blood.
     */
    private void processActiveFeedingPhase(FeedingSession session, Player vampire, Player target) {
        if (target.getGameMode() != GameMode.SURVIVAL) {
            this.cancelFeedingSession(session);
            return;
        }

        if (this.vampireManager.isHuman(target)) {
            final UUID vampireId = vampire.getUniqueId();
            final int currentSessionThirst = this.getSessionFeedingThirst(vampire);
            final int maxFeedingThirst = this.plugin.getConfigManager().getMaxFeedingThirstPerSession();

            // Prevent the vampire from draining more blood than the config setting allows
            if (currentSessionThirst >= maxFeedingThirst) {
                vampire.sendMessage(Component.text("Your thirst is quenched, for now. You are unable to drink any more blood from feeding until the next session.", NamedTextColor.RED));
                this.cancelFeedingSession(session);
                return;
            }

            double currentHealth = target.getHealth();

            // Kill the target if their health reaches minimum
            if (currentHealth <= 1) {
                this.handleFeedingDeath(session, vampire, target);
                return;
            }

            final double newHealth = currentHealth - HEALTH_DRAIN_PER_SECOND;
            target.setHealth(newHealth);
            final int currentFoodLevel = target.getFoodLevel(), newFoodLevel = Math.max(0, currentFoodLevel - 1);
            target.setFoodLevel(newFoodLevel);
            final int thirstToGive = Math.min(2, maxFeedingThirst - currentSessionThirst);

            this.thirstManager.modifyQuench(vampire, thirstToGive);
            this.sessionFeedingThirst.put(vampireId, currentSessionThirst + thirstToGive);

            vampire.sendActionBar(Component.text("Feeding...", NamedTextColor.DARK_RED));
            target.sendActionBar(Component.text("Your life force is being drained...", NamedTextColor.RED));

        } else {
            // Prevent the vampire from feeding on vampires without low on blood
            if (target.getExp() <= 0.1F) {
                vampire.sendMessage(Component.text("The vampiric essence has become too low to continue siphoning from.", NamedTextColor.RED));
                this.cancelFeedingSession(session);
                return;
            }

            this.thirstManager.modifyQuench(target, -1 * THIRST_GAIN_PER_SECOND);
            this.thirstManager.modifyQuench(vampire, THIRST_GAIN_PER_SECOND);

            vampire.sendActionBar(Component.text("Siphoning...", NamedTextColor.DARK_RED));
            target.sendActionBar(Component.text("Your vampiric essence is being siphoned...", NamedTextColor.RED));
        }

        // Create the custom drinking sound effect
        final float pitch = session.highPitch ? 0.8F : 0.6F;
        vampire.getWorld().playSound(vampire.getLocation(), Sound.ENTITY_WITCH_DRINK, SoundCategory.PLAYERS, 1.0F, pitch);
        session.highPitch = !session.highPitch;
    }

    /**
     * Kill or turn the target of vampire feeding.
     *
     * @param session the blood feeding session.
     * @param vampire the player feeding.
     * @param target the player who has been killed.
     */
    private void handleFeedingDeath(FeedingSession session, Player vampire, Player target) {
        if (this.plugin.getPermadeathManager().hasAbsolutePermadeathEnabled(target)) {
            vampire.sendMessage(Component.text("You watch the light of " + target.getName() + "'s eyes fade, and extinguish. Lost forever.", NamedTextColor.DARK_RED));
            target.sendMessage(Component.text("The world grows dim, blurry, you feel a darkness reach out, offering you one last chance to live, as a creature of the night... But you refuse... And slip under the veil of the afterlife.", NamedTextColor.GRAY));

            target.addScoreboardTag(DeathHandler.PERMADEATH_CHOSEN_TAG);
            target.setHealth(0.0);
            this.cancelFeedingSession(session);

        } else if (this.plugin.getBeetrootManager().hasBeetrootImmunity(target)) {
            vampire.sendMessage(Component.text("The sting of garlic sears at your gums, protecting your meal from your bite.", NamedTextColor.RED));

            if (this.plugin.getVampireTurningManager().isTurningEnabled(vampire)) {
                vampire.sendMessage(Component.text("You have failed to turn " + target.getName() + " - they will die as a human, wounded.", NamedTextColor.RED));
            } else {
                vampire.sendMessage(Component.text("You have killed " + target.getName() + " - they will die as a human, wounded.", NamedTextColor.RED));
            }

            vampire.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, this.plugin.getConfigManager().getGarlicWeaknessDuration() * 20, 9, false, false));

            // Only trigger the target's death once
            if (!didVictimAlreadyDie(target)) {
                target.sendMessage(Component.text("Your garlic immunity protects you from turning.", NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD));
                target.sendMessage(Component.text("You will respawn as a human, not as a cursed creature.", NamedTextColor.GREEN));

                this.killVictim(target);
            }

            this.cancelFeedingSession(session);

        } else if (!this.plugin.getVampireTurningManager().isTurningEnabled(vampire)) {
            try {
                Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                Objective deathObjective = mainScoreboard.getObjective("vsmp_death");

                if (deathObjective != null) {
                    final int currentDeaths = deathObjective.getScore(target.getName()).getScore();

                    if (currentDeaths >= this.plugin.getConfigManager().getHumanLifeCount()) {
                        vampire.sendMessage(Component.text("You watch the light of " + target.getName() + "'s eyes fade, and extinguish. Lost forever.", NamedTextColor.DARK_RED));
                        target.sendMessage(Component.text("The world grows dim, blurry, you feel a darkness reach out, offering you one last chance to live, as a creature of the night... But you refuse... And slip under the veil of the afterlife.", NamedTextColor.GRAY));
                        target.addScoreboardTag(DeathHandler.PERMADEATH_CHOSEN_TAG);
                        target.setHealth(0.0);

                        this.cancelFeedingSession(session);
                        return;
                    }
                }
            } catch (Exception e) {
                this.plugin.getLogger().warning("Failed to check death count for " + target.getName() + ": " + e.getMessage());
            }

            vampire.sendMessage(Component.text("You have killed " + target.getName() + ". They will respawn as a human, wounded.", NamedTextColor.RED));

            // Only trigger the target's death once
            if (!didVictimAlreadyDie(target)) {
                target.sendMessage(Component.text("You have been slain by a vampire, but they do not turn you...", NamedTextColor.GRAY));

                this.killVictim(target);
            }

            this.cancelFeedingSession(session);

        } else if (target.getScoreboardTags().contains(VampireManager.CURED_VAMPIRE_TAG)) {
            vampire.sendMessage(Component.text("You taste the blood of " + target.getName() + ", but it rejects your curse...", NamedTextColor.DARK_RED));
            vampire.sendMessage(Component.text("They have been cleansed by holy power - their soul slips beyond your grasp, lost forever.", NamedTextColor.DARK_RED));

            target.sendMessage(Component.text("The darkness reaches for you, but the holy blessing protects your soul...", NamedTextColor.GRAY));
            target.sendMessage(Component.text("You feel yourself slipping away, into a peaceful sleep.", NamedTextColor.GRAY));

            target.addScoreboardTag(DeathHandler.PERMADEATH_CHOSEN_TAG);
            target.setHealth(0.0);

            this.cancelFeedingSession(session);

        } else if (this.plugin.getPermadeathManager().hasPermadeathEnabled(target)) {
            vampire.sendMessage(Component.text("You watch the light of " + target.getName() + "'s eyes fade, and extinguish. Lost forever.", NamedTextColor.DARK_RED));
            target.sendMessage(Component.text("The world grows dim, blurry, you feel a darkness reach out, offering you one last chance to live, as a creature of the night... But you refuse... And slip under the veil of the afterlife.", NamedTextColor.GRAY));

            target.addScoreboardTag(DeathHandler.PERMADEATH_CHOSEN_TAG);
            target.setHealth(0.0);

            this.cancelFeedingSession(session);

        } else {
            this.vampireManager.performVampireTurning(target, vampire);
            int killThirst = this.thirstManager.getKillThirstReward(vampire, target);
            this.thirstManager.modifyQuench(vampire, killThirst, true);

            vampire.sendMessage(Component.text("You feel the last drops of life force leave " + target.getName() + ".", NamedTextColor.RED));
            vampire.sendMessage(Component.text("They have become a creature of the night...", NamedTextColor.RED));

            if (this.plugin.getVampireTrackingManager() != null) {
                this.plugin.getVampireTrackingManager().startTrackingNewVampire(target);
            }

            this.cancelFeedingSession(session);
            this.plugin.logInfo("Vampire " + vampire.getName() + " transformed " + target.getName() + " into a vampire through feeding");
        }
    }

    /**
     * Determine if the target is in feeding range of the vampire.
     *
     * @param vampire the player feeding.
     * @param target the player who might be fed on.
     * @return {@code true} if the target is close enough to the vampire.
     */
    private boolean isInFeedingRange(Player vampire, Player target) {
        if (!vampire.getWorld().equals(target.getWorld())) {
            return false;
        } else {
            return vampire.getLocation().distance(target.getLocation()) <= FEEDING_RANGE;
        }
    }

    /**
     * Determine if the feeding process can begin.
     *
     * @param vampire the player feeding.
     */
    private void attemptStartFeeding(Player vampire) {
        if (!this.isFeeding(vampire)) {
            if (vampire.isSneaking()) {
                if (this.vampireManager.isVampire(vampire)) {
                    final int currentSessionThirst = this.getSessionFeedingThirst(vampire);

                    if (currentSessionThirst >= this.plugin.getConfigManager().getMaxFeedingThirstPerSession()) {
                        vampire.sendMessage(Component.text("Your thirst is quenched, for now. You are unable to drink any more blood from feeding until the next session.", NamedTextColor.RED));

                    } else {
                        double distance;
                        boolean isHuman, isVampire, inRange;

                        for (Player nearbyPlayer : vampire.getWorld().getPlayers()) {
                            if (!nearbyPlayer.equals(vampire) && nearbyPlayer.getGameMode() == GameMode.SURVIVAL) {
                                distance = vampire.getLocation().distance(nearbyPlayer.getLocation());
                                isHuman = this.vampireManager.isHuman(nearbyPlayer);
                                isVampire = this.vampireManager.isVampire(nearbyPlayer);
                                inRange = this.isInFeedingRange(vampire, nearbyPlayer);

                                if ((isHuman || isVampire) && inRange) {
                                    if (isVampire && nearbyPlayer.getExp() <= 0.1F) {
                                        vampire.sendMessage(Component.text("The vampiric essence has become too low to continue siphoning from.", NamedTextColor.RED));
                                        return;
                                    }

                                    FeedingSession session = new FeedingSession(vampire.getUniqueId(), nearbyPlayer.getUniqueId());
                                    this.activeSessions.put(vampire.getUniqueId(), session);

                                    // Modify the message based on whether the target is human or vampire
                                    if (isHuman) {
                                        vampire.sendMessage(Component.text("You begin preparing to feed on " + nearbyPlayer.getName() + "...", NamedTextColor.DARK_GRAY));
                                    } else {
                                        vampire.sendMessage(Component.text("You begin preparing to siphon from " + nearbyPlayer.getName() + "...", NamedTextColor.DARK_GRAY));
                                    }

                                    vampire.sendMessage(Component.text("Stay crouched within range for " + VampireAbilityManager.formatTime(5L), NamedTextColor.GRAY));
                                    this.plugin.logInfo("Vampire " + vampire.getName() + " started feeding on " + nearbyPlayer.getName());
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Stop the vampire feeding process.
     *
     * @param session the blood feeding session.
     */
    private void cancelFeedingSession(FeedingSession session) {
        Player vampire = Bukkit.getPlayer(session.vampireId), target = Bukkit.getPlayer(session.targetId);

        if (target != null && target.isOnline() && session.phase == VampireFeedingManager.FeedingPhase.ACTIVE_FEEDING) {
            target.sendMessage(Component.text("You no longer feel a vampire draining your life force", NamedTextColor.GREEN));
        }

        this.activeSessions.remove(session.vampireId);
    }

    /**
     * Stop the vampire feeding process.
     *
     * @param target the player being fed on.
     */
    public void cancelFeedingSessionByTarget(Player target) {
        final UUID targetId = target.getUniqueId();

        for (FeedingSession session : this.activeSessions.values().toArray(new FeedingSession[0])) {
            if (session.targetId.equals(targetId)) {
                this.cancelFeedingSession(session);
                return;
            }
        }
    }

    /**
     * Check if a vampire is attempting to feed on another player whenever a vampire sneaks.
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
     * Stop the blood feeding process if one of the players leaves the game.
     *
     * @param event a player leaving the world.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        final UUID playerId = player.getUniqueId();
        FeedingSession session = this.activeSessions.get(playerId);

        if (session != null) {
            this.cancelFeedingSession(session);
        }

        for (FeedingSession activeSession : this.activeSessions.values().toArray(new FeedingSession[0])) {
            if (activeSession.targetId.equals(playerId)) {
                this.cancelFeedingSession(activeSession);
            }
        }
    }

    /**
     * Retrieve the number of current vampire feeding sessions.
     *
     * @return The number of current sessions.
     */
    public int getActiveFeedingCount() {
        return this.activeSessions.size();
    }

    /**
     * Check if a player is being fed upon
     *
     * @param player the player being checked.
     * @return {@code true} if the player is being fed upon by a vampire.
     */
    public boolean isPlayerBeingFedUpon(Player player) {
        return this.activeSessions.values().stream().anyMatch((session) -> session.targetId.equals(player.getUniqueId()));
    }

    /**
     * Record that the player has just died by feeding, and set a timer to prevent them from dying again for a short period.
     *
     * @param victim the player who died.
     */
    private void killVictim(Player victim) {
        // Kill the target player
        victim.setHealth(0.0);

        // Add the player to the list of feeding casualties
        this.justDiedToFeeding.put(victim.getUniqueId(), true);

        // Set a timer to remove the player from the feeding death's list
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.justDiedToFeeding.remove(victim.getUniqueId()), 40L);
    }

    /**
     * Prevent the player from being killed multiple times during
     *
     * @param victim the player being checked.
     * @return {@code true} if the player is on the list of recent feeding deaths.
     */
    private boolean didVictimAlreadyDie(Player victim) {
        return this.justDiedToFeeding.containsKey(victim.getUniqueId());
    }

    /**
     * Check if a player is feeding.
     *
     * @param player the player being checked.
     * @return {@code true} if the player is current feeding on another player.
     */
    public boolean isFeeding(Player player) {
        return this.activeSessions.containsKey(player.getUniqueId());
    }

    /**
     * Reset the blood feeding cap on all players.
     */
    public void resetSessionFeedingThirst() {
        this.sessionFeedingThirst.clear();
        this.plugin.logInfo("Reset feeding thirst tracking for new session");
    }

    /**
     * Retrieve how much blood the vampire has gained by feeding this session.
     *
     * @param vampire the player being checked.
     * @return The amount of blood points gained through feeding.
     */
    public int getSessionFeedingThirst(Player vampire) {
        return this.sessionFeedingThirst.getOrDefault(vampire.getUniqueId(), 0);
    }

    /**
     * Cancel the feeding processes before shutting down the manager.
     */
    public void shutdown() {
        for (FeedingSession session : this.activeSessions.values().toArray(new FeedingSession[0])) {
            this.cancelFeedingSession(session);
        }

        this.activeSessions.clear();
        this.sessionFeedingThirst.clear();
        this.plugin.logInfo("VampireFeedingManager shutdown complete");
    }

    private static class FeedingSession {
        public final UUID vampireId, targetId;
        public final long startTime;
        public FeedingPhase phase;
        public BukkitTask task;
        public int preparationSecondsRemaining;
        public boolean highPitch;

        /**
         * Create an instance of the feeding interaction record.
         *
         * @param vampireId the UUID of the player feeding.
         * @param targetId the UUID of the player being fed on.
         */
        public FeedingSession(UUID vampireId, UUID targetId) {
            this.vampireId = vampireId;
            this.targetId = targetId;
            this.startTime = System.currentTimeMillis();
            this.phase = VampireFeedingManager.FeedingPhase.PREPARATION;
            this.preparationSecondsRemaining = PREPARATION_TIME;
            this.highPitch = true;
        }
    }

    private enum FeedingPhase {
        PREPARATION,
        ACTIVE_FEEDING
    }
}
