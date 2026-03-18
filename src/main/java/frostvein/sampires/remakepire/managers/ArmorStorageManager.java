package frostvein.sampires.remakepire.managers;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import frostvein.sampires.remakepire.RemakepirePlugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import frostvein.sampires.remakepire.utils.OptionalTypeAdapter;

public class ArmorStorageManager {
    private final RemakepirePlugin plugin;
    private final Gson gson;
    private final File storageFile, backupFile;
    private final Map<UUID, StoredArmor> armorCache = new ConcurrentHashMap<>();

    /**
     * Create an instance of the Armor Storage manager.
     *
     * @param plugin the host plugin object.
     */
    public ArmorStorageManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.gson = (new GsonBuilder()).registerTypeAdapterFactory(new OptionalTypeAdapter()).setPrettyPrinting().create();
        this.storageFile = new File(plugin.getDataFolder(), "bat_armor_storage.json");
        this.backupFile = new File(plugin.getDataFolder(), "bat_armor_storage.backup.json");
        this.setupStorageFiles();
        this.loadArmorData();
    }

    /**
     * Create the file to store armor information in.
     */
    private void setupStorageFiles() {
        try {
            if (!this.plugin.getDataFolder().exists()) {
                this.plugin.getDataFolder().mkdirs();
            }

            if (!this.storageFile.exists()) {
                this.storageFile.createNewFile();

                try (FileWriter writer = new FileWriter(this.storageFile)) {
                    writer.write("{}");
                }
            }

            this.plugin.getLogger().info("ArmorStorageManager: Storage files initialized");
        } catch (IOException e) {
            this.plugin.getLogger().severe("ArmorStorageManager: Failed to initialize storage files: " + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * Store the player's armor into the cache.
     *
     * @param playerId the UUID of the player who owns the armor.
     * @param player the player who owns the armor.
     * @return {@code true} if armor was saved into the cache.
     */
    public boolean storeAndClearPlayerArmor(UUID playerId, Player player) {
        try {
            ItemStack[] currentArmor = player.getInventory().getArmorContents();
            StoredArmor storedArmor = new StoredArmor(currentArmor[3], currentArmor[2], currentArmor[1], currentArmor[0]);

            if (storedArmor.hasAnyArmor()) {
                this.armorCache.put(playerId, storedArmor);

                player.getInventory().setHelmet(null);
                player.getInventory().setChestplate(null);
                player.getInventory().setLeggings(null);
                player.getInventory().setBoots(null);
                player.getInventory().setArmorContents(new ItemStack[4]);
                player.updateInventory();

                this.saveArmorData();
                this.plugin.getLogger().info("ArmorStorageManager: Stored and cleared armor for player " + player.getName());
                return true;
            } else {
                this.plugin.getLogger().info("ArmorStorageManager: No armor to store for player " + player.getName());
                return false;
            }
        } catch (Exception e) {
            this.plugin.getLogger().severe("ArmorStorageManager: Failed to store and clear armor for player " + String.valueOf(playerId) + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieve the player's armor information from the cache.
     *
     * @param playerId the UUID of the player who has armor stored in the file.
     * @return The info on each armor piece the player owned.
     */
    public StoredArmor getStoredArmor(UUID playerId) {
        return this.armorCache.get(playerId);
    }

    /**
     * Check if the player has any armor logged within the cache.
     *
     * @param playerId the UUID of the player who might have stored armor.
     * @return {@code true} if the player has armor stored away.
     */
    public boolean hasStoredArmor(UUID playerId) {
        StoredArmor stored = this.armorCache.get(playerId);
        return stored != null && stored.hasAnyArmor();
    }

    /**
     * Clear the player's entry in the armor cache.
     *
     * @param playerId the UUID of the player who might have stored armor.
     */
    public void clearStoredArmor(UUID playerId) {
        if (this.armorCache.remove(playerId) != null) {
            this.saveArmorData();
            this.plugin.getLogger().info("ArmorStorageManager: Cleared stored armor for player " + String.valueOf(playerId));
        }
    }

    /**
     * Load the cached armor sets from the file.
     */
    private void loadArmorData() {
        File fileToLoad = this.storageFile;

        if (!this.isFileValid(this.storageFile) && this.isFileValid(this.backupFile)) {
            this.plugin.getLogger().warning("ArmorStorageManager: Main storage file corrupt, using backup");
            fileToLoad = this.backupFile;
        }

        try (FileReader reader = new FileReader(fileToLoad)) {
            Type mapType = (new TypeToken<Map<String, StoredArmor>>() {}).getType();
            Map<String, StoredArmor> loadedData = this.gson.fromJson(reader, mapType);
            if (loadedData != null) {
                for(Map.Entry<String, StoredArmor> entry : loadedData.entrySet()) {
                    try {
                        UUID playerId = UUID.fromString(entry.getKey());
                        this.armorCache.put(playerId, entry.getValue());

                    } catch (IllegalArgumentException e) {
                        this.plugin.getLogger().warning("ArmorStorageManager: Invalid UUID in storage: " + entry.getKey());
                    }
                }

                this.plugin.getLogger().info("ArmorStorageManager: Loaded armor data for " + this.armorCache.size() + " players");
            }
        } catch (IOException e) {
            this.plugin.getLogger().warning("ArmorStorageManager: Could not load armor storage file: " + e.getMessage());
        } catch (Exception e) {
            this.plugin.getLogger().severe("ArmorStorageManager: Error parsing armor storage file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Save the players' armor in the current cache into the file.
     */
    private void saveArmorData() {
        try {
            if (this.storageFile.exists() && this.storageFile.length() > 0L) {
                try {
                    Files.copy(this.storageFile.toPath(), this.backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    this.plugin.getLogger().warning("ArmorStorageManager: Failed to create backup: " + e.getMessage());
                }
            }

            Map<String, StoredArmor> dataToSave = new HashMap<>();

            for(Map.Entry<UUID, StoredArmor> entry : this.armorCache.entrySet()) {
                dataToSave.put((entry.getKey()).toString(), entry.getValue());
            }

            try (FileWriter writer = new FileWriter(this.storageFile)) {
                this.gson.toJson(dataToSave, writer);
            }
        } catch (IOException e) {
            this.plugin.getLogger().severe("ArmorStorageManager: Failed to save armor data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Determine if the file exists and is sufficiently sized.
     *
     * @param file the file being checked.
     * @return {@code true} if the file exists and is longer than 2.
     */
    private boolean isFileValid(File file) {
        return file.exists() && file.length() > 2L;
    }

    /**
     * Clear old entries from the armor cache.
     */
    public void cleanupExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        final long maxAge = 604800000L;

        this.armorCache.entrySet().removeIf((entry) -> {
            if (currentTime - (entry.getValue()).timestamp > maxAge) {
                this.plugin.getLogger().info("ArmorStorageManager: Removed expired armor storage for player " + String.valueOf(entry.getKey()));
                return true;
            }

            return false;
        });

        if (!this.armorCache.isEmpty()) {
            this.saveArmorData();
        }
    }

    /**
     * Retrieve the armor cache.
     *
     * @return A {@code Map} of all the players' stored armor.
     */
    public Map<UUID, StoredArmor> getAllStoredArmor() {
        return new HashMap<>(this.armorCache);
    }

    /**
     * Save the armor cache into the file before shutting down the manager.
     */
    public void shutdown() {
        this.saveArmorData();
        this.plugin.getLogger().info("ArmorStorageManager: Shutdown complete, " + this.armorCache.size() + " armor sets saved");
    }

    public static class StoredArmor {
        public final ItemStack helmet, chestplate, leggings, boots;
        public final long timestamp;

        /**
         * Create an instance of an armor set alongside its storage timestamp.
         *
         * @param helmet the helmet to store.
         * @param chestplate the chestplate to store.
         * @param leggings the leggings to store.
         * @param boots the boots to store.
         */
        public StoredArmor(ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots) {
            this.helmet = helmet != null ? helmet.clone() : null;
            this.chestplate = chestplate != null ? chestplate.clone() : null;
            this.leggings = leggings != null ? leggings.clone() : null;
            this.boots = boots != null ? boots.clone() : null;
            this.timestamp = System.currentTimeMillis();
        }

        /**
         * Check if any armor is being stored.
         *
         * @return {@code true} if all the armor slots are empty.
         */
        public boolean hasAnyArmor() {
            return this.helmet != null || this.chestplate != null || this.leggings != null || this.boots != null;
        }

        /**
         * Retrieve the armor being stored.
         *
         * @return An {@code ItemStack} array with the armor in reverse indices. Helmet = 3, chestplate = 2, leggings = 1, and boots = 0.
         */
        public ItemStack[] getArmorContents() {
            return new ItemStack[]{this.boots, this.leggings, this.chestplate, this.helmet};
        }
    }
}
