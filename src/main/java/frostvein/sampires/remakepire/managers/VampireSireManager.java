package frostvein.sampires.remakepire.managers;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class VampireSireManager {
    private final RemakepirePlugin plugin;
    private final Map<String, String> sireMap;
    private final File dataFile;
    private final Gson gson;

    /**
     * Create an instance of the Vampire Sire manager.
     *
     * @param plugin the host plugin object.
     */
    public VampireSireManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.sireMap = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "sire_mappings.json");
        this.gson = new Gson();
        this.loadSireMappings();

        plugin.logInfo("VampireSireManager initialized with " + this.sireMap.size() + " sire mappings");
    }

    /**
     * Retrieve the sire of the vampire.
     *
     * @param vampire the vampire whose sire is being retrieved.
     * @return The name of the vampire's sire.
     */
    public String getSire(Player vampire) {
        return this.sireMap.get(vampire.getName().toLowerCase());
    }

    /**
     * Determine if the vampire's sire is dead.
     *
     * @param vampire the vampire whose sire's survival is being checked.
     * @return {@code true} if the vampire's sire is dead.
     */
    public boolean isSireDead(Player vampire) {
        String sireName = this.getSire(vampire);

        if (sireName == null) {
            // If the sire cannot be found, act as though they are dead
            return true;
        } else if (vampire.getScoreboardTags().contains("CannotCure")) {
            // If the player is impossible to cure, act as though their sire is alive
            return false;
        } else {
            // Only act like the sire is alive if they are online and in an activate game mode
            Player sire = Bukkit.getPlayer(sireName);

            if (sire == null) {
                return true;
            } else {
                return sire.getGameMode() == GameMode.SPECTATOR;
            }
        }
    }

    /**
     * Check if the vampire can be cured.
     *
     * @param vampire the vampire being cured.
     * @return {@code true} if the vampire is curable.
     */
    public boolean canBeCured(Player vampire) {
        // Determine if the player is allowed to be cured
        if (vampire.getScoreboardTags().contains("CannotCure")) {
            return false;
        }

        // Allow the cure if either the sire death requirement is disabled or the vampire's sire is dead
        return !this.plugin.getConfigManager().doCuresRequireSireDeath() || this.isSireDead(vampire);
    }

    /**
     * Retrieve the survival status of the vampire's sire.
     *
     * @param vampire the vampire whose sire's survival is being checked.
     * @return The gamemode or offline status of the sire.
     */
    public String getSireStatus(Player vampire) {
        String sireName = this.getSire(vampire);

        if (sireName == null) {
            return "No sire assigned (can cure freely)";
        } else if (vampire.getScoreboardTags().contains("CannotCure")) {
            // If the player is impossible to cure, act as though their sire is alive
            return "No sire assigned, but the player is prevented from being cured.";
        } else {
            Player sire = Bukkit.getPlayer(sireName);

            if (sire == null) {
                return "Sire '" + sireName + "' is OFFLINE (can cure)";
            } else {
                GameMode sireGameMode = sire.getGameMode();
                return sireGameMode == GameMode.SPECTATOR ? "Sire '" + sireName + "' is in SPECTATOR mode (can cure)" : "Sire '" + sireName + "' is ALIVE in " + String.valueOf(sireGameMode) + " mode (CANNOT cure)";
            }
        }
    }

    /**
     * Add a sire relation to the sire mappings.
     *
     * @param vampireName the fledgling vampire's name.
     * @param sireName the vampire sire's name.
     */
    public void setSire(String vampireName, String sireName) {
        this.sireMap.put(vampireName.toLowerCase(), sireName);
        this.saveSireMappings();

        this.plugin.logInfo("Sire mapping added: " + vampireName + " -> " + sireName);
    }

    /**
     * Remove a sire relation from the sire mappings.
     *
     * @param vampireName the fledgling vampire's name.
     */
    public void removeSire(String vampireName) {
        this.sireMap.remove(vampireName.toLowerCase());
        this.saveSireMappings();

        this.plugin.logInfo("Sire mapping removed for: " + vampireName);
    }

    /**
     * Retrieve all current sire mappings.
     *
     * @return A {@code Map} of fledgling and vampire names.
     */
    public Map<String, String> getAllSireMappings() {
        return new HashMap<>(this.sireMap);
    }

    /**
     * Remove all sire mappings.
     */
    public void clearAllSireMappings() {
        this.sireMap.clear();
        this.saveSireMappings();

        this.plugin.logInfo("VampireSireManager: Cleared all sire mappings");
    }

    /**
     * Log the sire relationships and create the file to store them within.
     */
    private void loadSireMappings() {
        if (!this.dataFile.exists()) {
            this.plugin.logInfo("VampireSireManager: No existing sire mappings file found, starting fresh.");
            this.plugin.logInfo("VampireSireManager: Edit plugins/VampireSMP/sire_mappings.json to add sire relationships.");
            this.plugin.logInfo("VampireSireManager: Format: {\"Fledgling_Name\": \"Sire_Name\"}");

            Map<String, String> exampleMap = new HashMap<>();
            exampleMap.put("Fledgling_Name", "Sire_Name");

            try {
                if (!this.plugin.getDataFolder().exists()) {
                    this.plugin.getDataFolder().mkdirs();
                }

                try (FileWriter writer = new FileWriter(this.dataFile)) {
                    this.gson.toJson(exampleMap, writer);
                }

                this.plugin.logInfo("VampireSireManager: Created example sire_mappings.json file");
            } catch (IOException e) {
                this.plugin.getLogger().severe("VampireSireManager: Failed to create example file: " + e.getMessage());
            }

            this.sireMap.putAll(exampleMap);
        } else {
            try (FileReader reader = new FileReader(this.dataFile)) {
                Type type = (new TypeToken<Map<String, String>>() {}).getType();
                Map<String, String> rawData = this.gson.fromJson(reader, type);

                if (rawData != null) {
                    for(Map.Entry<String, String> entry : rawData.entrySet()) {
                        this.sireMap.put((entry.getKey()).toLowerCase(), entry.getValue());
                    }

                    this.plugin.logInfo("VampireSireManager: Loaded " + this.sireMap.size() + " sire mappings from file");
                }
            } catch (IOException e) {
                this.plugin.getLogger().severe("VampireSireManager: Failed to load sire mappings: " + e.getMessage());
            }
        }
    }

    /**
     * Record the sire relationships into the file.
     */
    private void saveSireMappings() {
        try {
            if (!this.plugin.getDataFolder().exists()) {
                this.plugin.getDataFolder().mkdirs();
            }

            try (FileWriter writer = new FileWriter(this.dataFile)) {
                this.gson.toJson(this.sireMap, writer);
            }

            this.plugin.logInfo("VampireSireManager: Saved " + this.sireMap.size() + " sire mappings to file");
        } catch (IOException e) {
            this.plugin.getLogger().severe("VampireSireManager: Failed to save sire mappings: " + e.getMessage());
        }
    }

    /**
     * Save the sire mappings to file before shutting down the manager.
     */
    public void shutdown() {
        this.saveSireMappings();
        this.plugin.logInfo("VampireSireManager: Shutdown complete");
    }
}
