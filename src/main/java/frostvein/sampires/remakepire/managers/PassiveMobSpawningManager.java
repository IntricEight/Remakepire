package frostvein.sampires.remakepire.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import frostvein.sampires.remakepire.utils.ConversionAssistant;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitTask;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class PassiveMobSpawningManager {
    private final RemakepirePlugin plugin;
    private final ConfigManager configManager;
    private final Random random;
    private BukkitTask autoSpawnTask;
    private long lastDaySpawned = -1L;
    private static final long MORNING_START = 0L, MORNING_END = 100L;
    private final Map<EntityType, Integer> mobTypeWeights = new LinkedHashMap<>();
    private final Set<Biome> blacklistedBiomes = new HashSet<>();

    /**
     * Create an instance of the Passive Mob Spawning manager.
     *
     * @param plugin the host plugin object.
     */
    public PassiveMobSpawningManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.random = new Random();

        this.mobTypeWeights.put(EntityType.COW, 30);
        this.mobTypeWeights.put(EntityType.PIG, 30);
        this.mobTypeWeights.put(EntityType.SHEEP, 25);
        this.mobTypeWeights.put(EntityType.CHICKEN, 15);

        this.initializeBiomeBlacklist();

        if (configManager.isPassiveMobAutoSpawnEnabled()) {
            this.startAutoSpawnTask();
            plugin.logInfo("PassiveMobSpawningManager: Initialized with automatic morning spawning (threshold: " + configManager.getPassiveMobMinimumThreshold() + " animals)");
        } else {
            plugin.logInfo("PassiveMobSpawningManager: Initialized (manual spawning only)");
        }
    }

    /**
     * Prevent passive mobs from spawning within bodies of water, underground, or in the dead forest.
     */
    private void initializeBiomeBlacklist() {
        this.blacklistedBiomes.add(Biome.OCEAN);
        this.blacklistedBiomes.add(Biome.DEEP_OCEAN);
        this.blacklistedBiomes.add(Biome.COLD_OCEAN);
        this.blacklistedBiomes.add(Biome.DEEP_COLD_OCEAN);
        this.blacklistedBiomes.add(Biome.FROZEN_OCEAN);
        this.blacklistedBiomes.add(Biome.DEEP_FROZEN_OCEAN);
        this.blacklistedBiomes.add(Biome.LUKEWARM_OCEAN);
        this.blacklistedBiomes.add(Biome.DEEP_LUKEWARM_OCEAN);
        this.blacklistedBiomes.add(Biome.WARM_OCEAN);
        this.blacklistedBiomes.add(Biome.RIVER);
        this.blacklistedBiomes.add(Biome.FROZEN_RIVER);
        this.blacklistedBiomes.add(Biome.MUSHROOM_FIELDS);

        try {
            Biome paleOakForest = Biome.valueOf("PALE_GARDEN");
            this.blacklistedBiomes.add(paleOakForest);
        } catch (IllegalArgumentException e) {}

        this.blacklistedBiomes.add(Biome.DEEP_DARK);
        this.blacklistedBiomes.add(Biome.DRIPSTONE_CAVES);
        this.blacklistedBiomes.add(Biome.LUSH_CAVES);
        this.blacklistedBiomes.add(Biome.STONY_SHORE);

        this.plugin.logInfo("PassiveMobSpawningManager: Blacklisted " + this.blacklistedBiomes.size() + " biomes");
    }

    /**
     * Attempt to spawn passive mobs in valid locations around the game map.
     */
    public void spawnPassiveMobs() {
        World world = this.plugin.getWorld();

        if (world == null) {
            this.plugin.getLogger().warning("PassiveMobSpawningManager: World 'world' not found");
        } else {
            Chunk[] loadedChunks = world.getLoadedChunks();
            final ConversionAssistant conversionAssistant = new ConversionAssistant();

            if (loadedChunks.length == 0) {
                this.plugin.getLogger().warning("PassiveMobSpawningManager: No loaded chunks found");
            } else {
                List<Location> validLocations = this.findValidSpawnLocations(world, loadedChunks);

                if (validLocations.isEmpty()) {
                    this.plugin.getLogger().warning("PassiveMobSpawningManager: No valid spawn locations found");
                } else {
                    Collections.shuffle(validLocations, this.random);
                    int mobsSpawned = 0;
                    Map<EntityType, Integer> spawnCounts = new HashMap<>();
                    int mobsToSpawn = this.configManager.getPassiveMobSpawnCount();

                    for(int i = 0; i < mobsToSpawn && i < validLocations.size(); ++i) {
                        Location spawnLocation = validLocations.get(i);
                        EntityType mobType = this.selectRandomMobType();

                        try {
                            world.spawn(spawnLocation, mobType.getEntityClass(), true, (entity) -> entity.setPersistent(true));
                            ++mobsSpawned;
                            spawnCounts.put(mobType, spawnCounts.getOrDefault(mobType, 0) + 1);

                        } catch (Exception e) {
                            this.plugin.getLogger().warning("PassiveMobSpawningManager: Failed to spawn " + String.valueOf(mobType) + " at " + conversionAssistant.locationToString(spawnLocation) + ": " + e.getMessage());
                        }
                    }

                    StringBuilder report = new StringBuilder("PassiveMobSpawningManager: Spawned " + mobsSpawned + " mobs - ");

                    for(Map.Entry<EntityType, Integer> entry : spawnCounts.entrySet()) {
                        report.append(entry.getValue()).append(" ").append(entry.getKey()).append(", ");
                    }

                    this.plugin.logInfo(report.toString());
                }
            }
        }
    }

    /**
     * Collect a list of locations where passive mobs can spawn.
     *
     * @param world the world hosting the plugin interactions.
     * @param loadedChunks the active chunks around the players.
     * @return A {@code List} of up to 500 locations that passive mobs could spawn at.
     */
    private List<Location> findValidSpawnLocations(World world, Chunk[] loadedChunks) {
        List<Location> validLocations = new ArrayList<>();
        final int maxLocationsToCheck = 500;
        int locationsChecked = 0;
        List<Chunk> chunkList = Arrays.asList(loadedChunks);
        Collections.shuffle(chunkList, this.random);

        for(Chunk chunk : chunkList) {
            if (locationsChecked >= maxLocationsToCheck) {
                break;
            }

            for(int attempt = 0; attempt < 3 && locationsChecked < maxLocationsToCheck; ++attempt) {
                int x = (chunk.getX() << 4) + this.random.nextInt(16);
                int z = (chunk.getZ() << 4) + this.random.nextInt(16);

                Block highestBlock = world.getHighestBlockAt(x, z);
                Location spawnLocation = highestBlock.getLocation().add(0.0, 1.0, 0.0);

                if (this.isValidSpawnLocation(spawnLocation)) {
                    validLocations.add(spawnLocation);
                }

                ++locationsChecked;
            }
        }

        this.plugin.logInfo("PassiveMobSpawningManager: Found " + validLocations.size() + " valid spawn locations from " + locationsChecked + " checks");
        return validLocations;
    }

    /**
     * Determine if a location is a valid spawn point for a passive mob.
     *
     * @param location A location to check.
     * @return {@code true} if the location and its biome are both valid.
     */
    private boolean isValidSpawnLocation(Location location) {
        Block blockAt = location.getBlock();
        Block blockBelow = blockAt.getRelative(0, -1, 0), blockAbove = blockAt.getRelative(0, 1, 0);

        if (blockBelow.getType() != Material.GRASS_BLOCK) {
            return false;
        } else if (blockAt.getType().isAir() && blockAbove.getType().isAir()) {
            if (blockAt.getLightLevel() < 9) {
                return false;
            } else {
                Biome biome = location.getBlock().getBiome();
                return !this.blacklistedBiomes.contains(biome);
            }
        } else {
            return false;
        }
    }

    /**
     * Choose a random mob from the mobTypeWeights list.
     *
     * @return The entity name of a random valid passive mob.
     */
    private EntityType selectRandomMobType() {
        final int roll = this.random.nextInt(100);
        int cumulative = 0;

        for(Map.Entry<EntityType, Integer> entry : this.mobTypeWeights.entrySet()) {
            cumulative += entry.getValue();

            if (roll < cumulative) {
                return entry.getKey();
            }
        }

        return EntityType.COW;
    }

    /**
     * Retrieve the maximum number of mobs the plugin will attempt to spawn.
     *
     * @return The number of mobs that will be spawned.
     */
    public int getMobsPerCycle() {
        return this.configManager.getPassiveMobSpawnCount();
    }

    /**
     * Attempt to spawn passive mobs inside the border.
     */
    public void triggerSpawning() {
        this.spawnPassiveMobs();
    }

    /**
     * Begin regularly checking each morning if there are enough passive mobs spawned in the game boundaries.
     */
    private void startAutoSpawnTask() {
        this.autoSpawnTask = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> this.checkAndSpawnIfNeeded(), 20L, 20L);
        this.plugin.logInfo("PassiveMobSpawningManager: Started automatic morning spawn task (checks every second)");
    }

    /**
     * Check if enough passive mobs are inside the loaded game chunks in the morning.
     */
    private void checkAndSpawnIfNeeded() {
        World world = this.plugin.getWorld();

        if (world != null) {
            long worldTime = world.getTime();
            long currentDay = world.getFullTime() / 24000L;
            boolean isMorning = worldTime >= MORNING_START && worldTime <= MORNING_END;

            if (isMorning && currentDay != this.lastDaySpawned) {
                int animalCount = this.countPassiveAnimalsInLoadedChunks(world);
                int threshold = this.configManager.getPassiveMobMinimumThreshold();
                this.plugin.logInfo("PassiveMobSpawningManager: Morning check - Found " + animalCount + " animals (threshold: " + threshold + ")");

                if (animalCount < threshold) {
                    this.plugin.logInfo("PassiveMobSpawningManager: Animal count below threshold, spawning animals...");
                    this.spawnPassiveMobs();
                    this.lastDaySpawned = currentDay;

                } else {
                    this.plugin.logInfo("PassiveMobSpawningManager: Animal count sufficient, skipping spawn");
                    this.lastDaySpawned = currentDay;
                }
            }
        }
    }

    /**
     * Count the number of passive mobs found within the loaded chunks.
     *
     * @param world the world hosting the plugin interactions.
     * @return The number of passive mobs.
     */
    private int countPassiveAnimalsInLoadedChunks(World world) {
        int count = 0;
        Chunk[] loadedChunks = world.getLoadedChunks();

        for(Chunk chunk : loadedChunks) {
            for(Entity entity : chunk.getEntities()) {
                EntityType type = entity.getType();

                if (type == EntityType.COW || type == EntityType.PIG || type == EntityType.SHEEP || type == EntityType.CHICKEN) {
                    ++count;
                }
            }
        }

        return count;
    }

    /**
     * Stop checking and spawning passive animals before shutting down the manager.
     */
    public void shutdown() {
        if (this.autoSpawnTask != null) {
            this.autoSpawnTask.cancel();
            this.autoSpawnTask = null;
            this.plugin.logInfo("PassiveMobSpawningManager: Stopped auto-spawn task");
        }

        this.plugin.logInfo("PassiveMobSpawningManager: Shutdown complete");
    }
}
