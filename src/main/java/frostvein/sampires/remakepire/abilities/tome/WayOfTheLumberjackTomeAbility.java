package frostvein.sampires.remakepire.abilities.tome;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import frostvein.sampires.remakepire.utils.ConversionAssistant;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import frostvein.sampires.remakepire.RemakepirePlugin;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class WayOfTheLumberjackTomeAbility extends TomeAbility implements Listener {
    private final Random random = new Random();
    private final Gson gson = new Gson();
    private final File placedLogsFile;
    private final ConversionAssistant conversionAssistant;
    private Set<String> placedLogs = new HashSet<>();
    private static final Set<Material> LOG_MATERIALS = Set.of(Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG, Material.CRIMSON_STEM, Material.WARPED_STEM);;

    /**
     * Create an instance of the Way of the Land tome ability.
     *
     * @param plugin the host plugin object.
     */
    public WayOfTheLumberjackTomeAbility(RemakepirePlugin plugin) {
        super(plugin, "WayOfTheLumberjack", new String[]{"You gain knowledge on how to fell the forest.", "You permanently gain a 30% chance to harvest", "twice the yield from each harvest."}, 0);
        this.placedLogsFile = new File(plugin.getDataFolder(), "placed_logs.json");
        this.loadPlacedLogs();
        this.conversionAssistant =  new ConversionAssistant();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    protected boolean useAbility(Player player) {
        if (!this.canUse(player)) {
            this.sendCannotUseMessage(player, "Only humans can use tome abilities!");
            return false;

        } else {
            this.sendSuccessMessage(player, "You have absorbed the knowledge of the lumberjack!");
            player.sendMessage("§7You now have a permanent 30% chance to receive double drops when harvesting natural logs.");
            player.sendMessage("§7This knowledge flows through your very being - you need not activate it again.");

            this.plugin.getWorld().playSound(player.getLocation(), "minecraft:block.wood.break", 1.0F, 1.2F);
            this.plugin.getWorld().playSound(player.getLocation(), "minecraft:entity.experience_orb.pickup", 0.5F, 0.8F);

            return true;
        }
    }

    /**
     * Track the placed blocks to prevent abuse of this ability.
     *
     * @param event a block being placed.
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();

        if (LOG_MATERIALS.contains(block.getType())) {
            String locationKey = this.conversionAssistant.locationToString(block.getLocation());
            this.placedLogs.add(locationKey);
            this.savePlacedLogs();
        }
    }

    /**
     * Roll the extra crop chance when breaking relevant blocks.
     *
     * @param event a block being broken.
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (LOG_MATERIALS.contains(block.getType())) {
            String locationKey = this.conversionAssistant.locationToString(block.getLocation());

            // Prevent placed logs from triggering the ability
            if (this.placedLogs.contains(locationKey)) {
                this.placedLogs.remove(locationKey);
                this.savePlacedLogs();

            } else if (this.plugin.getTomeManager().hasAbility(player, "wayofthelumberjack")) {
                if (this.random.nextDouble() < 0.3) {
                    ItemStack drop = new ItemStack(block.getType(), 1);
                    block.getWorld().dropItemNaturally(block.getLocation(), drop);
                }
            }
        }
    }

    /**
     * Mark the placed logs to prevent ability abuse.
     */
    private void loadPlacedLogs() {
        if (this.placedLogsFile.exists()) {
            try (FileReader reader = new FileReader(this.placedLogsFile)) {
                Type setType = (new TypeToken<HashSet<String>>() {}).getType();
                Set<String> loaded = this.gson.fromJson(reader, setType);

                if (loaded != null) {
                    this.placedLogs = loaded;
                    this.plugin.logInfo("WayOfTheLumberjack: Loaded " + this.placedLogs.size() + " placed log locations");
                }
            } catch (IOException e) {
                this.plugin.getLogger().warning("WayOfTheLumberjack: Failed to load placed logs file: " + e.getMessage());
            }
        }
    }

    /**
     * Record logs as they are placed to prevent ability abuse.
     */
    private void savePlacedLogs() {
        try {
            if (!this.plugin.getDataFolder().exists()) {
                this.plugin.getDataFolder().mkdirs();
            }

            try (FileWriter writer = new FileWriter(this.placedLogsFile)) {
                this.gson.toJson(this.placedLogs, writer);
            }
        } catch (IOException e) {
            this.plugin.getLogger().warning("WayOfTheLumberjack: Failed to save placed logs file: " + e.getMessage());
        }
    }

    /**
     * Save the record of placed logs when the server is shut down.
     */
    public void cleanup() {
        this.savePlacedLogs();
        this.plugin.logInfo("WayOfTheLumberjack: Saved " + this.placedLogs.size() + " placed log locations on shutdown");
    }
}
