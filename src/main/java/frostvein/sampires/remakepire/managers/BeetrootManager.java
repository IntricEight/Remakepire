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
    private final VampireManager vampireManager;
    private final SessionManager sessionManager;
    private final ConfigManager configManager;
    public static final String BEETROOT_USED_TAG = "beetroot_used_session";
    public static final String BEETROOT_PROCESSING_TAG = "beetroot_processing";
    public static final String BEETROOT_IMMUNITY_TAG = "beetroot_immunity";
    // Controls the duration of vampire nausea when eating garlic
    private static final int NAUSEA_DURATION = 500;
    // Controls the intensity of vampire nausea when eating garlic
    private static final int NAUSEA_AMPLIFIER = 1;
    private final Map<UUID, Integer> processingTimers = new HashMap<>();
    private final Map<UUID, Integer> immunityTimers = new HashMap<>();
    private File beetrootFile;
    private BukkitTask beetrootTask;

    /**
     * Create an instance of the Beetroot "garlic" manager.
     *
     * @param plugin the host plugin object.
     */
    public BeetrootManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.sessionManager = plugin.getSessionManager();
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
                this.plugin.getLogger().info("Created beetroot timer persistence file");
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
            for(Map.Entry<UUID, Integer> entry : this.processingTimers.entrySet()) {
                writer.write(((UUID)entry.getKey()).toString() + ":processing:" + String.valueOf(entry.getValue()));
                writer.newLine();
            }

            for(Map.Entry<UUID, Integer> entry : this.immunityTimers.entrySet()) {
                writer.write(((UUID)entry.getKey()).toString() + ":immunity:" + String.valueOf(entry.getValue()));
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
                if (BeetrootManager.this.sessionManager.isSessionActive()) {
                    Set<UUID> onlinePlayers = (Set)Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getUniqueId).collect(Collectors.toSet());
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
        Set<UUID> processingToRemove = new HashSet<>();

        for(Map.Entry<UUID, Integer> entry : this.processingTimers.entrySet()) {
            UUID playerId = (UUID)entry.getKey();

            if (onlinePlayers.contains(playerId)) {
                int timeLeft = (Integer)entry.getValue() - 1;

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

        for(UUID uuid : processingToRemove) {
            this.processingTimers.remove(uuid);
        }

        Set<UUID> immunityToRemove = new HashSet<>();

        for(Map.Entry<UUID, Integer> entry : this.immunityTimers.entrySet()) {
            UUID playerId = (UUID)entry.getKey();

            if (onlinePlayers.contains(playerId)) {
                int timeLeft = (Integer)entry.getValue() - 1;

                if (timeLeft <= 0) {
                    immunityToRemove.add(playerId);
                    Player player = Bukkit.getPlayer(playerId);

                    if (player != null) {
                        this.endImmunityPeriod(player);
                    }
                } else {
                    this.immunityTimers.put(playerId, timeLeft);
                }
            }
        }

        for(UUID uuid : immunityToRemove) {
            this.immunityTimers.remove(uuid);
        }

        if (!processingToRemove.isEmpty() || !immunityToRemove.isEmpty() || !this.processingTimers.isEmpty() || !this.immunityTimers.isEmpty()) {
            this.saveTimerData();
        }
    }

    /**
     * Begin the garlic timer if the player meets the consumption conditions.
     *
     * @param player the player who ate the garlic.
     */
    public void handleBeetrootConsumption(Player player) {
        if (!this.vampireManager.isHuman(player)) {
            player.sendMessage("§c§lThe garlic burns your throat and causes you to retch...");
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, NAUSEA_DURATION, NAUSEA_AMPLIFIER, false, false));

        } else if (player.getScoreboardTags().contains(BEETROOT_USED_TAG)) {
            player.sendMessage("§eYou have already consumed garlic this session.");
            player.sendMessage("§eYour body cannot process another dose so soon.");

        } else if (player.getScoreboardTags().contains(BEETROOT_PROCESSING_TAG)) {
            player.sendMessage("§eYou are already processing garlic substance...");

        } else if (player.getScoreboardTags().contains(BEETROOT_IMMUNITY_TAG)) {
            player.sendMessage("§a§ou already have garlic immunity.");

        } else {
            player.addScoreboardTag(BEETROOT_USED_TAG);
            player.addScoreboardTag(BEETROOT_PROCESSING_TAG);

            int minProcessing = this.configManager.getGarlicProcessingTimeMin();
            int maxProcessing = this.configManager.getGarlicProcessingTimeMax();
            int processingRange = maxProcessing - minProcessing;
            int processingSeconds = minProcessing + (new Random()).nextInt(processingRange + 1);

            UUID playerId = player.getUniqueId();
            this.processingTimers.put(playerId, processingSeconds);
            this.saveTimerData();

            player.sendMessage("§e§lYou consume the garlic...");
            player.sendMessage("§eThe earthy taste lingers in your mouth. You feel it will take §6" + minProcessing / 60 + "-" + maxProcessing / 60 + " minutes §eto take effect.");
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

        int minImmunity = this.configManager.getGarlicImmunityDurationMin();
        int maxImmunity = this.configManager.getGarlicImmunityDurationMax();
        int immunityRange = maxImmunity - minImmunity;
        int immunitySeconds = minImmunity + (new Random()).nextInt(immunityRange + 1);

        UUID playerId = player.getUniqueId();
        this.immunityTimers.put(playerId, immunitySeconds);
        this.saveTimerData();

        if (this.vampireManager.isHuman(player)) {
            player.sendMessage("§aThe garlic should have made its way into your system by now... You feel protected from the creatures of the night, should such things even exist.");
            player.sendMessage("§aImmunity will last for §2" + minImmunity / 60 + "-" + maxImmunity / 60 + " minutes§a.");
        }

        player.playSound(player, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1.0F, 1.5F);
    }

    /**
     * Remove garlic immunity from the player.
     *
     * @param player the player who ate garlic.
     */
    private void endImmunityPeriod(Player player) {
        player.removeScoreboardTag(BEETROOT_IMMUNITY_TAG);
        if (this.vampireManager.isHuman(player)) {
            player.sendMessage("§cYou imagine by now that the effects of the garlic have worn off...");
        }

        player.playSound(player, "iwie:creaking_deactivate", SoundCategory.PLAYERS, 1.0F, 1.0F);
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
        UUID playerId = player.getUniqueId();
        int timeLeft = 0;

        if (this.processingTimers.containsKey(playerId)) {
            player.addScoreboardTag(BEETROOT_USED_TAG);
            player.addScoreboardTag(BEETROOT_PROCESSING_TAG);
            timeLeft = this.processingTimers.get(playerId);
            player.sendMessage("Garlic you have previously ingested is still processing...");
        }

        if (this.immunityTimers.containsKey(playerId)) {
            player.addScoreboardTag(BEETROOT_USED_TAG);
            player.addScoreboardTag(BEETROOT_IMMUNITY_TAG);
            timeLeft = this.immunityTimers.get(playerId);
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
            return "§eProcessing beetroot... " + timeLeft / 60 + "m " + timeLeft % 60 + "s remaining";

        } else if (this.immunityTimers.containsKey(playerId)) {
            int timeLeft = this.immunityTimers.get(playerId);
            return "§aImmune to vampire turning " + timeLeft / 60 + "m " + timeLeft % 60 + "s remaining";

        } else {
            return this.hasUsedBeetrootThisSession(player) ? "§cBeetroot used this session (no longer immune)" : "§7No beetroot consumed this session";
        }
    }
}
