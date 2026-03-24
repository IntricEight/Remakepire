package frostvein.sampires.remakepire.managers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class BatTransformationManager {
    private final RemakepirePlugin plugin;
    private final ArmorStorageManager armorStorageManager;
    public static final String BAT_FORM_TAG = "bat_form";
    // Controls the maximum duration of bat form (in ticks)
    private static final long BAT_DURATION_MS = 120000L;
    // Controls how long the post-transformation slow falling lasts.
    private static final int SLOW_FALLING_DURATION = 200;
    private final Map<UUID, BatData> activeBats = new ConcurrentHashMap<>();
    private File batStateFile;
    private BukkitTask batCheckTask;

    /**
     * Create an instance of the Bat Transformation manager.
     *
     * @param plugin the host plugin object.
     */
    public BatTransformationManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.armorStorageManager = new ArmorStorageManager(plugin);
        this.setupPersistence();
        this.startBatCheckTask();
        this.loadBatStates();
    }

    /**
     * Create the file to store bat location information in.
     */
    private void setupPersistence() {
        if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
        }

        this.batStateFile = new File(this.plugin.getDataFolder(), "bat_transformations.txt");

        try {
            if (!this.batStateFile.exists()) {
                this.batStateFile.createNewFile();
            }

            this.plugin.logInfo("Created bat transformation persistence file");
        } catch (IOException e) {
            this.plugin.getLogger().severe("Failed to create bat transformation file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Begin monitoring the bats spawned by the Bat ability.
     */
    private void startBatCheckTask() {
        this.batCheckTask = (new BukkitRunnable() {
            int tickCount = 0;

            public void run() {
                BatTransformationManager.this.checkExpiredTransformations();
                BatTransformationManager.this.checkBatEntityHealth();
                BatTransformationManager.this.updateBatActionBars();
                ++this.tickCount;

                if (this.tickCount >= 6000) {
                    BatTransformationManager.this.armorStorageManager.cleanupExpiredEntries();
                    this.tickCount = 0;
                }
            }
        }).runTaskTimer(this.plugin, 20L, 20L);
    }

    /**
     * Force the vampire back into human form once the ability duration has elapsed.
     */
    private void checkExpiredTransformations() {
        Iterator<Map.Entry<UUID, BatData>> iterator = this.activeBats.entrySet().iterator();

        while(iterator.hasNext()) {
            Map.Entry<UUID, BatData> entry = iterator.next();
            UUID playerId = entry.getKey();
            BatData batData = entry.getValue();

            if (batData.isExpired()) {
                Player player = Bukkit.getPlayer(playerId);

                if (player != null && player.isOnline()) {
                    this.forceTransformToHuman(player, batData);
                    player.sendMessage("§6Your bat transformation has expired.");
                    player.sendMessage("§7You transform back into your vampiric form.");
                    player.playSound(player, Sound.ENTITY_BAT_TAKEOFF, SoundCategory.MASTER, 0.8F, 0.8F);
                }

                this.cleanupBatData(batData);
                iterator.remove();
                this.saveBatStates();
            }
        }
    }

    /**
     * Update the visual countdown on the player's remaining time in bat form.
     */
    private void updateBatActionBars() {
        for(Map.Entry<UUID, BatData> entry : this.activeBats.entrySet()) {
            UUID playerId = entry.getKey();
            BatData batData = entry.getValue();
            Player player = Bukkit.getPlayer(playerId);

            if (player != null && player.isOnline()) {
                int remainingSeconds = batData.getRemainingSeconds();
                int minutes = remainingSeconds / 60, seconds = remainingSeconds % 60;
                String actionBar = "§8Bat Form: §c§l" + String.format("%d:%02d", minutes, seconds);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionBar));
            }
        }
    }

    /**
     * Monitor the health of the active bat forms.
     */
    private void checkBatEntityHealth() {
        Iterator<Map.Entry<UUID, BatData>> iterator = this.activeBats.entrySet().iterator();

        while(iterator.hasNext()) {
            Map.Entry<UUID, BatData> entry = iterator.next();
            UUID playerId = entry.getKey();
            BatData batData = entry.getValue();

            if (batData.batEntity != null && !batData.batEntity.isValid()) {
                Player player = Bukkit.getPlayer(playerId);

                if (player != null && player.isOnline()) {
                    this.handleBatDeath(player, batData);
                }

                this.cleanupBatData(batData);
                iterator.remove();
                this.saveBatStates();
            }
        }
    }

    /**
     * Remove the player from bat form and kill them.
     *
     * @param player the vampire in bat form.
     * @param batData the bat ability usage information.
     */
    public void handleBatDeath(Player player, BatData batData) {
        player.removeScoreboardTag(BAT_FORM_TAG);
        this.restorePlayerArmor(player);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);

        // Make sure to only disable these if bat form is the only way the player can use them
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }

        player.setHealth(0.0);
        this.plugin.logInfo("Player " + player.getName() + " died because their bat form was destroyed");
    }

    /**
     * Remove the player from bat form and kill them.
     *
     * @param player the vampire in bat form.
     */
    public void handleBatDeath(Player player) {
        if (this.isInBatForm(player)) {
            BatData batData = this.activeBats.get(player.getUniqueId());

            if (batData != null) {
                this.handleBatDeath(player, batData);
                this.cleanupBatData(batData);
                this.activeBats.remove(player.getUniqueId());
                this.saveBatStates();

            } else {
                player.removeScoreboardTag(BAT_FORM_TAG);
                this.restorePlayerArmor(player);
                player.removePotionEffect(PotionEffectType.INVISIBILITY);

                // Make sure to only disable these if bat form is the only way the player can use them
                if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                    player.setFlying(false);
                    player.setAllowFlight(false);
                }

                player.setHealth(0.0);
                this.plugin.getLogger().warning("Player " + player.getName() + " bat died but no batData found - cleaned up player state");
            }
        }
    }

    /**
     * Retrieve the player using the tag given to their bat.
     *
     * @param bat a bat entity.
     * @return The player who is controlling the bat.
     */
    public Player getPlayerFromBat(Bat bat) {
        if (bat.getCustomName() != null && bat.getCustomName().startsWith("Â§8")) {
            String playerName = bat.getCustomName().substring(2);
            return Bukkit.getPlayer(playerName);

        } else {
            return null;
        }
    }

    /**
     * Turn the vampire into a bat that they can fly and control. This bat's life force is tied to the vampire's.
     *
     * @param player the vampire using the bat ability.
     * @return {@code true} if the player entered their bat form.
     */
    public boolean transformToBat(final Player player) {
        if (this.isInBatForm(player)) {
            return false;
        } else {
            try {
                Location currentLoc = player.getLocation().clone();
                BatData batData = new BatData(System.currentTimeMillis());
                batData.lastValidLocation = currentLoc;

                if (this.armorStorageManager.storeAndClearPlayerArmor(player.getUniqueId(), player)) {
                    this.plugin.logInfo("Successfully stored and cleared armor for player " + player.getName() + " during bat transformation");
                } else {
                    this.plugin.logInfo("No armor to store for player " + player.getName() + " during bat transformation");
                }

                player.addScoreboardTag(BAT_FORM_TAG);
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 0, false, false));
                player.setAllowFlight(true);
                player.setFlying(true);
                final Bat bat = (Bat)player.getWorld().spawn(player.getLocation(), Bat.class);

                for(int i = 0; i < 10; ++i) {
                    player.getWorld().spawn(player.getLocation(), Bat.class);
                }

                bat.setCustomName("Â§8" + player.getName());
                bat.setCustomNameVisible(false);
                bat.setSilent(true);
                bat.setPersistent(true);

                if (this.plugin.getVampireCastTeam() != null) {
                    this.plugin.getVampireCastTeam().addEntry(bat.getUniqueId().toString());
                }

                batData.batEntity = bat;
                batData.transformationTask = (new BukkitRunnable() {
                    public void run() {
                        if (player.isOnline() && BatTransformationManager.this.isInBatForm(player)) {
                            if (bat.isValid()) {
                                bat.teleport(player.getLocation());
                            } else {
                                this.cancel();
                            }
                        } else {
                            this.cancel();
                        }
                    }
                }).runTaskTimer(this.plugin, 1L, 1L);

                this.activeBats.put(player.getUniqueId(), batData);
                this.saveBatStates();
                this.plugin.logInfo("Player " + player.getName() + " transformed into bat form");
                return true;

            } catch (Exception e) {
                this.plugin.getLogger().severe("Failed to transform player " + player.getName() + " into bat: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * Turn the vampire back into their ordinary form.
     *
     * @param player the vampire using the bat ability.
     * @return {@code true} if the player ended their bat form.
     */
    public boolean transformToHuman(Player player) {
        if (!this.isInBatForm(player)) {
            return false;

        } else {
            BatData batData = this.activeBats.get(player.getUniqueId());

            if (batData == null) {
                player.removeScoreboardTag(BAT_FORM_TAG);
                return false;

            } else {
                this.forceTransformToHuman(player, batData);
                this.activeBats.remove(player.getUniqueId());
                this.saveBatStates();
                this.plugin.logInfo("Player " + player.getName() + " transformed back to human form");
                return true;
            }
        }
    }

    /**
     * Force the vampire to return to their ordinary form.
     *
     * @param player the vampire using the bat ability.
     * @param batData the bat ability usage information.
     */
    private void forceTransformToHuman(Player player, BatData batData) {
        try {
            player.removeScoreboardTag(BAT_FORM_TAG);
            this.restorePlayerArmor(player);
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, SLOW_FALLING_DURATION, 0, false, false));

            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }

            this.cleanupBatData(batData);

        } catch (Exception e) {
            this.plugin.getLogger().severe("Failed to transform player " + player.getName() + " back to human: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Restore the vampire's armor.
     *
     * @param player the vampire using the bat ability.
     */
    private void restorePlayerArmor(Player player) {
        UUID playerId = player.getUniqueId();

        try {
            if (!this.armorStorageManager.hasStoredArmor(playerId)) {
                this.plugin.logInfo("No stored armor found for player " + player.getName() + " - nothing to restore");
                return;
            }

            ArmorStorageManager.StoredArmor storedArmor = this.armorStorageManager.getStoredArmor(playerId);
            if (storedArmor == null) {
                this.plugin.getLogger().warning("Stored armor was null for player " + player.getName());
                return;
            }

            ItemStack[] currentArmor = player.getInventory().getArmorContents();
            boolean hasCurrentArmor = false;

            for(ItemStack piece : currentArmor) {
                if (piece != null && piece.getType() != Material.AIR) {
                    hasCurrentArmor = true;
                    break;
                }
            }

            if (hasCurrentArmor) {
                this.plugin.getLogger().warning("ARMOR DUPLICATION BUG DETECTED: Player " + player.getName() + " has armor equipped when restoring bat transformation armor!");
                this.plugin.getLogger().warning("This should not happen with the fixed armor system. Dropping current armor to prevent duplication.");
                Location dropLocation = player.getLocation();

                for(ItemStack piece : currentArmor) {
                    if (piece != null && piece.getType() != Material.AIR) {
                        player.getWorld().dropItemNaturally(dropLocation, piece);
                        this.plugin.getLogger().warning("Dropped armor piece: " + String.valueOf(piece.getType()));
                    }
                }

                player.getInventory().setHelmet(null);
                player.getInventory().setChestplate(null);
                player.getInventory().setLeggings(null);
                player.getInventory().setBoots(null);
                player.getInventory().setArmorContents(new ItemStack[4]);
            }

            ItemStack[] restoredArmor = storedArmor.getArmorContents();
            player.getInventory().setArmorContents(restoredArmor);
            player.updateInventory();
            this.armorStorageManager.clearStoredArmor(playerId);
            int restoredPieces = 0;

            for(ItemStack piece : restoredArmor) {
                if (piece != null && piece.getType() != Material.AIR) {
                    ++restoredPieces;
                }
            }

            this.plugin.logInfo("Successfully restored " + restoredPieces + " armor pieces for player " + player.getName() + " after bat transformation");
        } catch (Exception e) {
            this.plugin.getLogger().severe("Failed to restore armor for player " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * Clean up the bat entity and cancel the transformation monitor.
     *
     * @param batData the bat ability usage information.
     */
    private void cleanupBatData(BatData batData) {
        if (batData == null) {
            this.plugin.getLogger().warning("Attempted to cleanup null batData");
        } else {
            if (batData.transformationTask != null) {
                batData.transformationTask.cancel();
            }

            if (batData.batEntity != null && batData.batEntity.isValid()) {
                batData.batEntity.remove();
            }
        }
    }

    /**
     * Retrieve if the player is using their bat ability
     *
     * @param player the player who is being checked.
     * @return {@code true} if the player is in bat form.
     */
    public boolean isInBatForm(Player player) {
        return player.getScoreboardTags().contains(BAT_FORM_TAG);
    }

    /**
     * Determine how much longer the vampire can remain as a bat.
     *
     * @param player the vampire using the bat ability.
     * @return The seconds remaining until the player will be removed from bat form.
     */
    public int getRemainingTime(Player player) {
        BatData batData = this.activeBats.get(player.getUniqueId());
        return batData == null ? 0 : batData.getRemainingSeconds();
    }

    /**
     * Check if a player has rejoined after leaving the game while in bat form.
     *
     * @param player the player joining the game.
     */
    public void handlePlayerJoin(Player player) {
        UUID playerId = player.getUniqueId();

        if (this.armorStorageManager.hasStoredArmor(playerId)) {
            this.plugin.logInfo("Found stored armor for player " + player.getName() + " on join - attempting restoration");
            this.restorePlayerArmor(player);
        }

        if (this.isInBatForm(player)) {
            BatData batData = this.activeBats.get(player.getUniqueId());
            this.forceTransformToHuman(player, batData);
            this.plugin.logInfo("Player " + player.getName() + " was forced back to human form upon joining (was in bat form when offline)");
        }
    }

    /**
     * Revert the player to their ordinary form when they quit the game.
     *
     * @param player the player quitting the game.
     */
    public void handlePlayerQuit(Player player) {
        if (this.isInBatForm(player)) {
            this.transformToHuman(player);
            this.plugin.logInfo("Player " + player.getName() + " exited bat form due to disconnect");
        }
    }

    /**
     * Load in the list of active bat forms from the file.
     */
    private void loadBatStates() {
        try (BufferedReader reader = new BufferedReader(new FileReader(this.batStateFile))) {
            while(true) {
                String line;
                if ((line = reader.readLine()) == null) {
                    this.plugin.logInfo("Loaded bat transformation states from file");
                    break;
                }

                String[] parts = line.split(":");
                if (parts.length == 2) {
                    UUID playerId = UUID.fromString(parts[0]);
                    long startTime = Long.parseLong(parts[1]);
                    BatData batData = new BatData(startTime);
                    this.activeBats.put(playerId, batData);
                }
            }
        } catch (IOException e) {
            this.plugin.getLogger().warning("Could not load bat transformation states: " + e.getMessage());
        }
    }

    /**
     * Save the list of active bat forms into the file.
     */
    private void saveBatStates() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.batStateFile))) {
            for(Map.Entry<UUID, BatData> entry : this.activeBats.entrySet()) {
                UUID playerId = entry.getKey();
                BatData batData = entry.getValue();

                if (!batData.isExpired()) {
                    writer.write(playerId.toString() + ":" + batData.startTime);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            this.plugin.getLogger().warning("Could not save bat transformation states: " + e.getMessage());
        }
    }

    /**
     * Clear the bat form effects before shutting down the manager.
     */
    public void shutdown() {
        if (this.batCheckTask != null) {
            this.batCheckTask.cancel();
        }

        for(BatData batData : this.activeBats.values()) {
            this.cleanupBatData(batData);
        }

        for(Player player : Bukkit.getOnlinePlayers()) {
            if (this.isInBatForm(player)) {
                player.removeScoreboardTag(BAT_FORM_TAG);
                this.restorePlayerArmor(player);
                player.removePotionEffect(PotionEffectType.INVISIBILITY);

                if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                    player.setFlying(false);
                    player.setAllowFlight(false);
                }
            }
        }

        if (this.armorStorageManager != null) {
            this.armorStorageManager.shutdown();
        }

        this.saveBatStates();
        this.plugin.logInfo("Bat transformation manager shutdown complete");
    }

    private static class BatData {
        public final long startTime;
        public final long endTime;
        public Bat batEntity;
        public BukkitTask transformationTask;
        public Location lastValidLocation;

        /**
         * Create an instance of the bat transformation record.
         *
         * @param startTime The time when the player entered bat form.
         */
        public BatData(long startTime) {
            this.startTime = startTime;
            this.endTime = startTime + BAT_DURATION_MS;
            this.lastValidLocation = null;
        }

        /**
         * Determine if the bat form duration has elapsed.
         *
         * @return {@code true} if the bat form ability is expired.
         */
        public boolean isExpired() {
            return System.currentTimeMillis() >= this.endTime;
        }

        /**
         * Retrieve the remaining milliseconds that the bat form can remain active.
         *
         * @return The milliseconds remaining until the vampire will be removed from bat form.
         */
        public long getRemainingTime() {
            return Math.max(0L, this.endTime - System.currentTimeMillis());
        }

        /**
         * Retrieve the remaining seconds that the bat form can remain active.
         *
         * @return The seconds remaining until the vampire will be removed from bat form.
         */
        public int getRemainingSeconds() {
            return (int)(this.getRemainingTime() / 1000L);
        }
    }
}
