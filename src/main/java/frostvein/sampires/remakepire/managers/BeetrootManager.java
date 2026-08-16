package frostvein.sampires.remakepire.managers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class BeetrootManager {
    private final RemakepirePlugin plugin;
    private final ConfigManager configManager;
    public static final String BEETROOT_USED_TAG = "beetroot_used_session", BEETROOT_PROCESSING_TAG = "beetroot_processing", BEETROOT_IMMUNITY_TAG = "beetroot_immunity";
    // Controls the duration of vampire nausea when eating garlic
    private static final int NAUSEA_DURATION = 500;
    // Controls the intensity of vampire nausea when eating garlic
    private static final int NAUSEA_AMPLIFIER = 1;
    private final Map<UUID, Integer> processingTimers = new HashMap<>(), immunityTimers = new HashMap<>(), recoveryTimers = new HashMap<>();
    private File beetrootFile;
    private BukkitTask beetrootTask;

    /**
     * Create an instance of the Beetroot "garlic" manager.
     *
     * @param plugin the host plugin object.
     */
    public BeetrootManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.setupPersistenceSystem();
        this.startBeetrootTask();
    }

    /**
     * Create the file to store garlic timers in.
     */
    private void setupPersistenceSystem() {
        if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
        }

        this.beetrootFile = new File(this.plugin.getDataFolder(), "beetroot_timers.txt");

        if (!this.beetrootFile.exists()) {
            try {
                this.beetrootFile.createNewFile();
                this.plugin.logInfo("Created beetroot timer persistence file");
            } catch (IOException e) {
                this.plugin.getLogger().severe("Failed to create beetroot timer file: " + e.getMessage());
                e.printStackTrace();
            }
        }

        this.loadTimerData();
    }

    /**
     * Load the garlic timers in from the file.
     */
    private void loadTimerData() {
        String line;

        try (BufferedReader reader = new BufferedReader(new FileReader(this.beetrootFile))) {
            while((line = reader.readLine()) != null) {
                String[] parts = line.split(":");

                if (parts.length == 3) {
                    UUID uuid = UUID.fromString(parts[0]);
                    String type = parts[1];
                    int seconds = Integer.parseInt(parts[2]);

                    if ("processing".equals(type)) {
                        this.processingTimers.put(uuid, seconds);
                    } else if ("immunity".equals(type)) {
                        this.immunityTimers.put(uuid, seconds);
                    } else if ("recovering".equals(type)) {
                        this.recoveryTimers.put(uuid, seconds);
                    }
                }
            }
        } catch (IOException e) {
            this.plugin.getLogger().warning("Could not load beetroot timer data: " + e.getMessage());
        }
    }

    /**
     * Save the garlic timers into the file.
     */
    private void saveTimerData() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.beetrootFile))) {
            for (Map.Entry<UUID, Integer> entry : this.processingTimers.entrySet()) {
                writer.write((entry.getKey()).toString() + ":processing:" + entry.getValue());
                writer.newLine();
            }

            for (Map.Entry<UUID, Integer> entry : this.immunityTimers.entrySet()) {
                writer.write((entry.getKey()).toString() + ":immunity:" + entry.getValue());
                writer.newLine();
            }

            for (Map.Entry<UUID, Integer> entry : this.recoveryTimers.entrySet()) {
                writer.write((entry.getKey()).toString() + ":recovering:" + entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            this.plugin.getLogger().warning("Could not save beetroot timer data: " + e.getMessage());
        }
    }

    /**
     * Begin checking the garlic timers of players.
     */
    private void startBeetrootTask() {
        this.beetrootTask = (new BukkitRunnable() {
            public void run() {
                if (BeetrootManager.this.plugin.getSessionManager().isSessionActive()) {
                    Set<UUID> onlinePlayers = Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getUniqueId).collect(Collectors.toSet());
                    BeetrootManager.this.processTimersForOnlinePlayers(onlinePlayers);
                }
            }
        }).runTaskTimer(this.plugin, 20L, 20L);
    }

    /**
     * Grant garlic immunity to online players once their garlic timer has elapsed.
     *
     * @param onlinePlayers identifiers for all online players.
     */
    private void processTimersForOnlinePlayers(Set<UUID> onlinePlayers) {
        Set<UUID> processingToRemove = new HashSet<>(), immunityToRemove = new HashSet<>(), recoveryToRemove = new HashSet<>();

        // Handle the manual timers for activating the garlic immunity
        for (Map.Entry<UUID, Integer> entry : this.processingTimers.entrySet()) {
            UUID playerId = entry.getKey();

            if (onlinePlayers.contains(playerId)) {
                int timeLeft = entry.getValue() - 1;

                if (timeLeft <= 0) {
                    processingToRemove.add(playerId);
                    Player player = Bukkit.getPlayer(playerId);

                    if (player != null) {
                        this.startImmunityPeriod(player);
                    }
                } else {
                    this.processingTimers.put(playerId, timeLeft);
                }
            }
        }

        // Remove any players whose processing timer has elapsed from the set of processing timers.
        for (UUID uuid : processingToRemove) {
            this.processingTimers.remove(uuid);
        }

        // Handle the manual timers for the garlic immunity duration
        for (Map.Entry<UUID, Integer> entry : this.immunityTimers.entrySet()) {
            UUID playerId = entry.getKey();

            if (onlinePlayers.contains(playerId)) {
                int timeLeft = entry.getValue() - 1;

                if (timeLeft <= 0) {
                    immunityToRemove.add(playerId);
                    Player player = Bukkit.getPlayer(playerId);

                    if (player != null) {
                        this.endImmunityPeriod(player); // This also starts the recovery sequence
                    }
                } else {
                    this.immunityTimers.put(playerId, timeLeft);
                }
            }
        }

        // Remove any players whose immunity timer has elapsed from the set of immunity timers
        for (UUID uuid : immunityToRemove) {
            this.immunityTimers.remove(uuid);
        }

        // Handle the manual timers for the garlic recovery duration
        for (Map.Entry<UUID, Integer> entry : this.recoveryTimers.entrySet()) {
            UUID playerId = entry.getKey();

            if (onlinePlayers.contains(playerId)) {
                int timeLeft = entry.getValue() - 1;

                if (timeLeft <= 0) {
                    recoveryToRemove.add(playerId);
                    Player player = Bukkit.getPlayer(playerId);

                    if (player != null) {
                        this.endRecoveryPeriod(player);
                    }
                } else {
                    this.recoveryTimers.put(playerId, timeLeft);
                }
            }
        }

        // Remove any players whose recovery timer has elapsed from the set of recovery timers
        for (UUID uuid : recoveryToRemove) {
            this.recoveryTimers.remove(uuid);
        }

        // Save the current timer data into the backup file
        if (!processingToRemove.isEmpty() || !immunityToRemove.isEmpty() || !recoveryToRemove.isEmpty()
                || !this.processingTimers.isEmpty() || !this.immunityTimers.isEmpty() || !this.recoveryTimers.isEmpty())
        {
            this.saveTimerData();
        }
    }

    /**
     * Begin the garlic timer if the player meets the consumption conditions.
     *
     * @param player the player who ate the garlic.
     */
    public void handleBeetrootConsumption(Player player) {
        if (!this.plugin.getVampireManager().isHuman(player)) {
            player.sendMessage(Component.text("The garlic burns your throat and causes you to retch...", NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, NAUSEA_DURATION, NAUSEA_AMPLIFIER, false, false));

        } else if (this.hasUsedBeetrootThisSession(player)) {
            player.sendMessage(Component.text("You have already consumed garlic this session.", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Your body cannot process another dose so soon.", NamedTextColor.YELLOW));

        } else if (this.isProcessingBeetroot(player)) {
            player.sendMessage(Component.text("You are already processing garlic substance...", NamedTextColor.YELLOW));

        } else if (this.hasBeetrootImmunity(player)) {
            player.sendMessage(Component.text("You already have garlic immunity.", NamedTextColor.GREEN));

        } else {
            player.addScoreboardTag(BEETROOT_USED_TAG);
            player.addScoreboardTag(BEETROOT_PROCESSING_TAG);

            int minProcessing = this.configManager.getGarlicProcessingTimeMin(), maxProcessing = this.configManager.getGarlicProcessingTimeMax();
            int processingRange = maxProcessing - minProcessing;
            int processingSeconds = minProcessing + (new Random()).nextInt(processingRange + 1);

            UUID playerId = player.getUniqueId();
            this.processingTimers.put(playerId, processingSeconds);
            this.saveTimerData();

            player.sendMessage(Component.text("You consume the garlic...", NamedTextColor.YELLOW)
                    .decorate(TextDecoration.BOLD));
            player.sendMessage(Component.text("The earthy taste lingers in your mouth. You feel it will take ", NamedTextColor.YELLOW)
                    .append(Component.text(minProcessing / 60 + "-" + maxProcessing / 60 + " minutes", NamedTextColor.GOLD))
                    .append(Component.text(" to take effect.", NamedTextColor.YELLOW))
            );
            player.playSound(player, Sound.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
    }

    /**
     * Grant the player garlic immunity from vampire turnings.
     *
     * @param player the player who ate garlic.
     */
    private void startImmunityPeriod(Player player) {
        player.removeScoreboardTag(BEETROOT_PROCESSING_TAG);
        player.addScoreboardTag(BEETROOT_IMMUNITY_TAG);

        final int minImmunity = this.configManager.getGarlicImmunityDurationMin(), maxImmunity = this.configManager.getGarlicImmunityDurationMax();
        final int immunityRange = maxImmunity - minImmunity;
        final int immunitySeconds = minImmunity + (new Random()).nextInt(immunityRange + 1);

        final UUID playerId = player.getUniqueId();
        this.immunityTimers.put(playerId, immunitySeconds);
        this.saveTimerData();

        if (this.plugin.getVampireManager().isHuman(player)) {
            player.sendMessage(Component.text("The garlic should have made its way into your system by now... You feel protected from the creatures of the night, should such things even exist.", NamedTextColor.GREEN));
            player.sendMessage(Component.text("Immunity will last for ", NamedTextColor.GREEN)
                    .append(Component.text(minImmunity / 60 + "-" + maxImmunity / 60 + " minutes", NamedTextColor.DARK_GREEN))
                    .append(Component.text(".", NamedTextColor.GREEN))
            );
        }

        player.playSound(player, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1.0F, 1.5F);
    }

    /**
     * Remove garlic immunity from the player and begin the timer for their recovery.
     *
     * @param player the player who ate garlic.
     */
    private void endImmunityPeriod(Player player) {
        player.removeScoreboardTag(BEETROOT_IMMUNITY_TAG);

        final int minRecovery = this.configManager.getGarlicRecoveryDurationMin(), maxRecovery = this.configManager.getGarlicRecoveryDurationMax();
        final int recoveryRange = maxRecovery - minRecovery;
        final int recoverySeconds = minRecovery + (new Random()).nextInt(recoveryRange + 1);

        final UUID playerId = player.getUniqueId();
        this.recoveryTimers.put(playerId, recoverySeconds);
        this.saveTimerData();

        if (this.plugin.getVampireManager().isHuman(player)) {
            player.sendMessage(Component.text("You imagine by now that the effects of the garlic have worn off...", NamedTextColor.RED));
            player.sendMessage(Component.text("The strain on your body is severe. You cannot handle more garlic for ", NamedTextColor.RED)
                    .append(Component.text(minRecovery / 60 + "-" + maxRecovery / 60 + " minutes", NamedTextColor.DARK_RED))
                    .append(Component.text(".", NamedTextColor.RED))
            );
        }

        player.playSound(player, "iwie:creaking_deactivate", SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    /**
     * Allow the player to begin the garlic cycle anew.
     *
     * @param player the player who ate garlic.
     */
    private void endRecoveryPeriod(Player player) {
        player.removeScoreboardTag(BEETROOT_USED_TAG);

        if (this.plugin.getVampireManager().isHuman(player)) {
            player.sendMessage(Component.text("Your body seems to have recovered from the lingering effects of the garlic.", NamedTextColor.GREEN));
        }

        player.playSound(player, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 0.8F, 0.6F);
    }

    /**
     * Retrieve if the player has an active garlic immunity.
     *
     * @param player a player being checked.
     * @return {@code true} if the player has garlic immunity.
     */
    public boolean hasBeetrootImmunity(Player player) {
        return player.getScoreboardTags().contains(BEETROOT_IMMUNITY_TAG);
    }

    /**
     * Retrieve if the player has already used this session's garlic immunity.
     *
     * @param player a player being checked.
     * @return {@code true} if the player's garlic immunity has expired.
     */
    public boolean hasUsedBeetrootThisSession(Player player) {
        return player.getScoreboardTags().contains(BEETROOT_USED_TAG);
    }

    /**
     * Retrieve if the player is waiting for their garlic timer to expire before receiving garlic immunity
     *
     * @param player a player being checked.
     * @return {@code true} if the player has a garlic timer.
     */
    public boolean isProcessingBeetroot(Player player) {
        return player.getScoreboardTags().contains(BEETROOT_PROCESSING_TAG);
    }

    /**
     * Retrieve the time left before the player is granted garlic immunity.
     *
     * @param player the player who ate garlic.
     * @return The seconds until the player's garlic immunity actives.
     */
    public int getRemainingProcessingTime(Player player) {
        return this.processingTimers.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * Retrieve the time left before the player's garlic immunity expires.
     *
     * @param player the player who ate garlic.
     * @return The seconds until the player's garlic immunity expires.
     */
    public int getRemainingImmunityTime(Player player) {
        return this.immunityTimers.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * Reset the player's garlic data, removing their immunity and allowing them to consume more.
     *
     * @param player the player to reset.
     */
    public void resetPlayerBeetrootUsage(Player player) {
        UUID playerId = player.getUniqueId();
        this.processingTimers.remove(playerId);
        this.immunityTimers.remove(playerId);

        player.removeScoreboardTag(BEETROOT_USED_TAG);
        player.removeScoreboardTag(BEETROOT_PROCESSING_TAG);
        player.removeScoreboardTag(BEETROOT_IMMUNITY_TAG);

        this.saveTimerData();
    }

    /**
     * Repair the player's garlic tags using their timer data.
     *
     * @param player a player to restore.
     */
    public void restorePlayerState(Player player) {
        final UUID playerId = player.getUniqueId();
        int timeLeft = 0;

        if (this.processingTimers.containsKey(playerId)) {
            player.addScoreboardTag(BEETROOT_USED_TAG);
            player.addScoreboardTag(BEETROOT_PROCESSING_TAG);
            timeLeft = this.processingTimers.get(playerId);
            player.sendMessage(Component.text("The garlic you previously ingested is still processing...", NamedTextColor.YELLOW));
        }

        if (this.immunityTimers.containsKey(playerId)) {
            player.addScoreboardTag(BEETROOT_USED_TAG);
            player.addScoreboardTag(BEETROOT_IMMUNITY_TAG);
            timeLeft = this.immunityTimers.get(playerId);
            player.sendMessage(Component.text("The garlic you previously ingested still grants you protection...", NamedTextColor.GREEN));
        }
    }

    /**
     * Cancel the timer checks before shutting down the manager.
     */
    public void shutdown() {
        if (this.beetrootTask != null) {
            this.beetrootTask.cancel();
        }

        this.saveTimerData();
    }

    /**
     * Retrieve a description of the player's garlic timers.
     *
     * @param player a player to check.
     * @return A description of the garlic status.
     */
    public String getPlayerStatus(Player player) {
        UUID playerId = player.getUniqueId();

        if (this.processingTimers.containsKey(playerId)) {
            int timeLeft = this.processingTimers.get(playerId);
            return "§eProcessing garlic... " + timeLeft / 60 + "m " + timeLeft % 60 + "s remaining";

        } else if (this.immunityTimers.containsKey(playerId)) {
            int timeLeft = this.immunityTimers.get(playerId);
            return "§aImmune to vampire turning " + timeLeft / 60 + "m " + timeLeft % 60 + "s remaining";

        } else {
            return this.hasUsedBeetrootThisSession(player) ? "§cGarlic used this session (no longer immune)" : "§7No garlic consumed this session";
        }
    }
}
