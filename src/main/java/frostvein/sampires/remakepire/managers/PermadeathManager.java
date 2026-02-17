package frostvein.sampires.remakepire.managers;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import frostvein.sampires.remakepire.RemakepirePlugin;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class PermadeathManager {
    private final RemakepirePlugin plugin;
    private final Map<UUID, PermadeathMode> permadeathModes;
    private final File dataFile;
    private final Gson gson;

    public PermadeathManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.permadeathModes = new HashMap();
        this.dataFile = new File(plugin.getDataFolder(), "permadeath_modes.json");
        this.gson = new Gson();
        this.loadPermadeathData();
        this.migrateOldData();
    }

    public PermadeathMode getPermadeathMode(Player player) {
        return (PermadeathMode)this.permadeathModes.getOrDefault(player.getUniqueId(), PermadeathManager.PermadeathMode.OFF);
    }

    public void setPermadeathMode(Player player, PermadeathMode mode) {
        UUID playerId = player.getUniqueId();
        this.permadeathModes.put(playerId, mode);
        this.savePermadeathData();
        this.plugin.getLogger().info("Player " + player.getName() + " permadeath mode set to: " + String.valueOf((Object)mode));
    }

    public boolean hasPermadeathEnabled(Player player) {
        PermadeathMode mode = this.getPermadeathMode(player);
        return mode == PermadeathManager.PermadeathMode.ON || mode == PermadeathManager.PermadeathMode.ABSOLUTE;
    }

    public boolean hasAbsolutePermadeathEnabled(Player player) {
        return this.getPermadeathMode(player) == PermadeathManager.PermadeathMode.ABSOLUTE;
    }

    public void removePlayer(Player player) {
        UUID playerId = player.getUniqueId();
        this.permadeathModes.remove(playerId);
        this.savePermadeathData();
    }

    public int getPermadeathCount() {
        return (int)this.permadeathModes.values().stream().filter((mode) -> mode == PermadeathManager.PermadeathMode.ON || mode == PermadeathManager.PermadeathMode.ABSOLUTE).count();
    }

    public void clearAllPermadeathModes() {
        this.permadeathModes.clear();
        this.savePermadeathData();
        this.plugin.getLogger().info("PermadeathManager: Cleared all permadeath modes");
    }

    private void loadPermadeathData() {
        if (!this.dataFile.exists()) {
            this.plugin.getLogger().info("PermadeathManager: No existing permadeath data file found, starting fresh.");
        } else {
            try (FileReader reader = new FileReader(this.dataFile)) {
                Type type = (new TypeToken<Map<String, String>>() {
                }).getType();
                Map<String, String> rawData = (Map)this.gson.fromJson(reader, type);
                if (rawData != null) {
                    for(Map.Entry<String, String> entry : rawData.entrySet()) {
                        try {
                            UUID playerId = UUID.fromString((String)entry.getKey());
                            PermadeathMode mode = PermadeathManager.PermadeathMode.valueOf((String)entry.getValue());
                            this.permadeathModes.put(playerId, mode);
                        } catch (IllegalArgumentException var9) {
                            this.plugin.getLogger().warning("PermadeathManager: Invalid data in file: " + (String)entry.getKey() + " = " + (String)entry.getValue());
                        }
                    }

                    this.plugin.getLogger().info("PermadeathManager: Loaded " + this.permadeathModes.size() + " permadeath preferences");
                }
            } catch (IOException e) {
                this.plugin.getLogger().severe("PermadeathManager: Failed to load permadeath data: " + e.getMessage());
            }

        }
    }

    private void migrateOldData() {
        File oldPermadeathFile = new File(this.plugin.getDataFolder(), "permadeath_preferences.json");
        File oldAbsoluteFile = new File(this.plugin.getDataFolder(), "absolute_permadeath_preferences.json");

        boolean migrated = false;
        if (oldPermadeathFile.exists()) {
            try (FileReader reader = new FileReader(oldPermadeathFile)) {
                Type type = (new TypeToken<Map<String, Boolean>>() {
                }).getType();
                Map<String, Boolean> oldData = (Map)this.gson.fromJson(reader, type);
                if (oldData != null) {
                    for(Map.Entry<String, Boolean> entry : oldData.entrySet()) {
                        try {
                            UUID playerId = UUID.fromString((String)entry.getKey());
                            if ((Boolean)entry.getValue() && !this.permadeathModes.containsKey(playerId)) {
                                this.permadeathModes.put(playerId, PermadeathManager.PermadeathMode.ON);
                            }
                        } catch (IllegalArgumentException var13) {
                        }
                    }

                    migrated = true;
                }
            } catch (IOException e) {
                this.plugin.getLogger().warning("PermadeathManager: Failed to migrate old permadeath data: " + e.getMessage());
            }
        }

        if (oldAbsoluteFile.exists()) {
            try (FileReader reader = new FileReader(oldAbsoluteFile)) {
                Type type = (new TypeToken<Map<String, Boolean>>() {
                }).getType();
                Map<String, Boolean> oldData = (Map)this.gson.fromJson(reader, type);
                if (oldData != null) {
                    for(Map.Entry<String, Boolean> entry : oldData.entrySet()) {
                        try {
                            UUID playerId = UUID.fromString((String)entry.getKey());
                            if ((Boolean)entry.getValue()) {
                                this.permadeathModes.put(playerId, PermadeathManager.PermadeathMode.ABSOLUTE);
                            }
                        } catch (IllegalArgumentException var11) {
                        }
                    }

                    migrated = true;
                }
            } catch (IOException e) {
                this.plugin.getLogger().warning("PermadeathManager: Failed to migrate old absolute permadeath data: " + e.getMessage());
            }
        }

        if (migrated) {
            this.savePermadeathData();
            this.plugin.getLogger().info("PermadeathManager: Migrated old permadeath data to new format");
        }

    }

    private void savePermadeathData() {
        try {
            if (!this.plugin.getDataFolder().exists()) {
                this.plugin.getDataFolder().mkdirs();
            }

            Map<String, String> rawData = new HashMap();

            for(Map.Entry<UUID, PermadeathMode> entry : this.permadeathModes.entrySet()) {
                rawData.put(((UUID)entry.getKey()).toString(), ((PermadeathMode)entry.getValue()).name());
            }

            try (FileWriter writer = new FileWriter(this.dataFile)) {
                this.gson.toJson(rawData, writer);
            }
        } catch (IOException e) {
            this.plugin.getLogger().severe("PermadeathManager: Failed to save permadeath data: " + e.getMessage());
        }

    }

    public void shutdown() {
        this.savePermadeathData();
        this.plugin.getLogger().info("PermadeathManager: Shutdown complete");
    }

    public static enum PermadeathMode {
        OFF,
        ON,
        ABSOLUTE;
    }
}
