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
    private static final long MORNING_START = 0L;
    private static final long MORNING_END = 100L;
    private final Map<EntityType, Integer> mobTypeWeights = new LinkedHashMap();
    private final Set<Biome> blacklistedBiomes = new HashSet();

    public PassiveMobSpawningManager(RemakepirePlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.random = new Random();
        this.mobTypeWeights.put(EntityType.COW, 30);
        this.mobTypeWeights.put(EntityType.PIG, 30);
        this.mobTypeWeights.put(EntityType.SHEEP, 25);
        this.mobTypeWeights.put(EntityType.CHICKEN, 15);
        this.initializeBiomeBlacklist();
        if (configManager.isPassiveMobAutoSpawnEnabled()) {
            this.startAutoSpawnTask();
            plugin.getLogger().info("PassiveMobSpawningManager: Initialized with automatic morning spawning (threshold: " + configManager.getPassiveMobMinimumThreshold() + " animals)");
        } else {
            plugin.getLogger().info("PassiveMobSpawningManager: Initialized (manual spawning only)");
        }

    }

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
        } catch (IllegalArgumentException e) {
        }

        this.blacklistedBiomes.add(Biome.DEEP_DARK);
        this.blacklistedBiomes.add(Biome.DRIPSTONE_CAVES);
        this.blacklistedBiomes.add(Biome.LUSH_CAVES);
        this.blacklistedBiomes.add(Biome.STONY_SHORE);
        this.plugin.getLogger().info("PassiveMobSpawningManager: Blacklisted " + this.blacklistedBiomes.size() + " biomes");
    }

    public void spawnPassiveMobs() {
        World world = this.plugin.getWorld();
        if (world == null) {
            this.plugin.getLogger().warning("PassiveMobSpawningManager: World 'world' not found");
        } else {
            Chunk[] loadedChunks = world.getLoadedChunks();
            if (loadedChunks.length == 0) {
                this.plugin.getLogger().warning("PassiveMobSpawningManager: No loaded chunks found");
            } else {
                List<Location> validLocations = this.findValidSpawnLocations(world, loadedChunks);
                if (validLocations.isEmpty()) {
                    this.plugin.getLogger().warning("PassiveMobSpawningManager: No valid spawn locations found");
                } else {
                    Collections.shuffle(validLocations, this.random);
                    int mobsSpawned = 0;
                    Map<EntityType, Integer> spawnCounts = new HashMap();
                    int mobsToSpawn = this.configManager.getPassiveMobSpawnCount();

                    for(int i = 0; i < mobsToSpawn && i < validLocations.size(); ++i) {
                        Location spawnLocation = (Location)validLocations.get(i);
                        EntityType mobType = this.selectRandomMobType();

                        try {
                            world.spawn(spawnLocation, mobType.getEntityClass(), true, (entity) -> entity.setPersistent(true));
                            ++mobsSpawned;
                            spawnCounts.put(mobType, (Integer)spawnCounts.getOrDefault(mobType, 0) + 1);
                        } catch (Exception e) {
                            this.plugin.getLogger().warning("PassiveMobSpawningManager: Failed to spawn " + String.valueOf(mobType) + " at " + this.locationToString(spawnLocation) + ": " + e.getMessage());
                        }
                    }

                    StringBuilder report = new StringBuilder("PassiveMobSpawningManager: Spawned " + mobsSpawned + " mobs - ");

                    for(Map.Entry<EntityType, Integer> entry : spawnCounts.entrySet()) {
                        report.append(entry.getValue()).append(" ").append(entry.getKey()).append(", ");
                    }

                    this.plugin.getLogger().info(report.toString());
                }
            }
        }
    }

    private List<Location> findValidSpawnLocations(World world, Chunk[] loadedChunks) {
        List<Location> validLocations = new ArrayList();
        int maxLocationsToCheck = 500;
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

        this.plugin.getLogger().info("PassiveMobSpawningManager: Found " + validLocations.size() + " valid spawn locations from " + locationsChecked + " checks");
        return validLocations;
    }

    private boolean isValidSpawnLocation(Location location) {
        Block blockBelow = location.getBlock().getRelative(0, -1, 0);
        Block blockAt = location.getBlock();
        Block blockAbove = location.getBlock().getRelative(0, 1, 0);

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

    private EntityType selectRandomMobType() {
        int roll = this.random.nextInt(100);
        int cumulative = 0;

        for(Map.Entry<EntityType, Integer> entry : this.mobTypeWeights.entrySet()) {
            cumulative += (Integer)entry.getValue();
            if (roll < cumulative) {
                return (EntityType)entry.getKey();
            }
        }

        return EntityType.COW;
    }

    private String locationToString(Location location) {
        return String.format("(%d, %d, %d)", location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public int getMobsPerCycle() {
        return this.configManager.getPassiveMobSpawnCount();
    }

    public void triggerSpawning() {
        this.spawnPassiveMobs();
    }

    private void startAutoSpawnTask() {
        this.autoSpawnTask = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> this.checkAndSpawnIfNeeded(), 20L, 20L);
        this.plugin.getLogger().info("PassiveMobSpawningManager: Started automatic morning spawn task (checks every second)");
    }

    private void checkAndSpawnIfNeeded() {
        World world = this.plugin.getWorld();
        if (world != null) {
            long worldTime = world.getTime();
            long currentDay = world.getFullTime() / 24000L;
            boolean isMorning = worldTime >= 0L && worldTime <= 100L;
            if (isMorning && currentDay != this.lastDaySpawned) {
                int animalCount = this.countPassiveAnimalsInLoadedChunks(world);
                int threshold = this.configManager.getPassiveMobMinimumThreshold();
                this.plugin.getLogger().info("PassiveMobSpawningManager: Morning check - Found " + animalCount + " animals (threshold: " + threshold + ")");
                if (animalCount < threshold) {
                    this.plugin.getLogger().info("PassiveMobSpawningManager: Animal count below threshold, spawning animals...");
                    this.spawnPassiveMobs();
                    this.lastDaySpawned = currentDay;
                } else {
                    this.plugin.getLogger().info("PassiveMobSpawningManager: Animal count sufficient, skipping spawn");
                    this.lastDaySpawned = currentDay;
                }
            }

        }
    }

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

    public void shutdown() {
        if (this.autoSpawnTask != null) {
            this.autoSpawnTask.cancel();
            this.autoSpawnTask = null;
            this.plugin.getLogger().info("PassiveMobSpawningManager: Stopped auto-spawn task");
        }

        this.plugin.getLogger().info("PassiveMobSpawningManager: Shutdown complete");
    }
}
