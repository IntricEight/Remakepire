package frostvein.sampires.remakepire.managers;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.beacons.BeaconSite;
import frostvein.sampires.remakepire.beacons.BeaconSite.BeaconState;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

public class BeaconManager {
    private final RemakepirePlugin plugin;
    private final Map<String, BeaconSite> beacons;
    private final File dataFile;
    private final Gson gson;
    private BukkitTask particleTask;
    private BukkitTask conversionCircleParticleTask;
    private BukkitTask autoSaveTask;
    private BukkitTask holyRegenTask;
    private BukkitTask cooldownTrackingTask;
    private BukkitTask beaconMaintenanceTask;
    // Controls the radius from a beacon that players can be while effecting it
    public static final double BEACON_CONVERSION_RANGE = 3.0;
    // Controls the radius from a holy beacon where vampires cannot use their abilities
    public static final double HOLY_SUPPRESSION_RANGE = 25.0;
    // Controls the radius from a holy beacon where humans gain regeneration
    public static final double HOLY_REGENERATION_RANGE = 25.0;
    // Controls the duration of the holy beacon's regeneration
    private static final int REGEN_DURATION_TICKS = 100;
    // Controls the intensity of the regeneration
    private static final int REGEN_AMPLIFIER = 0;
    private long lastCooldownUpdate = System.currentTimeMillis();
    private final Map<String, ItemDisplay> beaconDisplays = new HashMap<>();
    private final Map<String, BukkitTask> pendingNeutralBroadcasts = new HashMap<>();

    /**
     * Create an instance of the Beacon manager.
     *
     * @param plugin the host plugin object.
     */
    public BeaconManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.beacons = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "beacons.json");
        this.gson = (new GsonBuilder()).setPrettyPrinting().create();

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        this.loadBeacons();
        this.startParticleTask();
        this.startAutoSaveTask();
        this.startHolyRegenerationTask();
        this.startConversionCircleParticleTask();
        this.startCooldownTrackingTask();
        this.startBeaconMaintenanceTask();
    }

    /**
     * Begin intermittently saving the current beacon conditions.
     */
    private void startAutoSaveTask() {
        this.autoSaveTask = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> {
            if (!this.beacons.isEmpty()) {
                this.plugin.getLogger().fine("Auto-saving beacons...");
                this.saveBeacons();
            }
        }, 6000L, 6000L);

        this.plugin.getLogger().info("Started beacon auto-save task (every 5 minutes)");
    }

    /**
     * Begin tracking the beacon cooldown values.
     */
    private void startCooldownTrackingTask() {
        this.cooldownTrackingTask = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> this.updateBeaconCooldowns(), 20L, 20L);
        this.plugin.getLogger().info("Started beacon cooldown tracking task");
    }

    /**
     * Begin regularly validating the beacons placed in unloaded chunks.
     */
    private void startBeaconMaintenanceTask() {
        this.beaconMaintenanceTask = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> {
            if (this.plugin.getSessionManager().isSessionActive()) {
                this.performBeaconMaintenance();
            }
        }, 1200L, 1200L);

        this.plugin.getLogger().info("Started beacon maintenance task (runs every minute during active sessions)");
    }

    /**
     * Validate the beacons placed in unloaded chunks.
     */
    private void performBeaconMaintenance() {
        this.forceLoadBeaconChunks();
        this.validateDisplays();
        this.plugin.getLogger().fine("Beacon maintenance completed - chunks loaded and displays validated");
    }

    /**
     * Force the world to load chunks with beacons in them.
     */
    private void forceLoadBeaconChunks() {
        int chunksLoaded = 0;

        for(BeaconSite beacon : this.beacons.values()) {
            Location location = beacon.getLocation();

            if (location != null && location.getWorld() != null && !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                location.getWorld().loadChunk(location.getBlockX() >> 4, location.getBlockZ() >> 4);
                ++chunksLoaded;
            }
        }

        if (chunksLoaded > 0) {
            this.plugin.getLogger().fine("Force loaded " + chunksLoaded + " beacon chunks");
        }
    }

    /**
     * Update the conversion cooldowns on the beacons every 500 milliseconds.
     */
    private void updateBeaconCooldowns() {
        if (!this.plugin.getSessionManager().isSessionActive()) {
            this.lastCooldownUpdate = System.currentTimeMillis();
        } else {
            long currentTime = System.currentTimeMillis();
            long timePassed = currentTime - this.lastCooldownUpdate;

            if (timePassed >= 500L) {
                boolean cooldownsChanged = false;

                for(BeaconSite beacon : this.beacons.values()) {
                    if (beacon.isOnConversionCooldown()) {
                        beacon.reduceCooldown(timePassed);
                        cooldownsChanged = true;
                    }
                }

                if (cooldownsChanged) {
                    this.saveBeacons();
                }

                this.lastCooldownUpdate = currentTime;
            }
        }
    }

    /**
     * Clear the conversion cooldowns on all beacons.
     */
    public void clearAllBeaconCooldownsForNewSession() {
        int clearedBeacons = 0;

        for(BeaconSite beacon : this.beacons.values()) {
            if (beacon.getConversionCooldownUntil() > 0L) {
                beacon.setConversionCooldownUntil(0L);
                ++clearedBeacons;
            }
        }

        this.plugin.getLogger().info("NEW SESSION: Cleared conversion cooldowns for " + clearedBeacons + " beacons");

        if (clearedBeacons > 0) {
            this.saveBeacons();
        }
    }

    /**
     * Begin showing particles around beacons while a conversion is happening.
     */
    private void startConversionCircleParticleTask() {
        this.conversionCircleParticleTask = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> this.showAllConversionAndSuppressionCircles(), 0L, 4L);
        this.plugin.getLogger().info("Started conversion and suppression circle particle task (every 2 ticks)");
    }

    /**
     * Begin applying regeneration to nearby humans from a holy beacon.
     */
    private void startHolyRegenerationTask() {
        this.holyRegenTask = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> this.applyHolyRegeneration(), 60L, 100L);
        this.plugin.getLogger().info("Started holy beacon regeneration task for humans");
    }

    /**
     * Apply regeneration to nearby humans from a holy beacon.
     */
    private void applyHolyRegeneration() {
        for(Player player : this.plugin.getServer().getOnlinePlayers()) {
            if (this.plugin.getVampireManager().isHuman(player)) {
                BeaconSite nearestHolyBeacon = this.getNearestHolyBeacon(player.getLocation());

                if (nearestHolyBeacon != null) {
                    PotionEffect regenEffect = new PotionEffect(PotionEffectType.REGENERATION, REGEN_DURATION_TICKS, REGEN_AMPLIFIER, false, false, true);
                    player.addPotionEffect(regenEffect);

                    if (!player.hasPotionEffect(PotionEffectType.REGENERATION)) {
                        player.sendMessage("§a§You feel the holy energy rejuvenating you...");
                    }
                }
            }
        }
    }

    /**
     * Create an item display for the beacon.
     *
     * @param beacon a beacon of any alignment.
     */
    public void createBeaconDisplay(BeaconSite beacon) {
        if (beacon != null && beacon.getLocation() != null) {
            Location displayLoc = beacon.getLocation().clone();
            displayLoc.add(0.0, 0.5, 0.0);
            ItemStack pumpkinItem = new ItemStack(Material.CARVED_PUMPKIN);
            ItemMeta meta = pumpkinItem.getItemMeta();
            String expectedCMD = this.getCustomModelDataForState(beacon.getState());

            if (meta != null) {
                meta.setDisplayName("§6Beacon: " + beacon.getName());
                pumpkinItem.setItemMeta(meta);
            }

            pumpkinItem.setData(DataComponentTypes.CUSTOM_MODEL_DATA, (CustomModelData)CustomModelData.customModelData().addString(expectedCMD).build());

            ItemDisplay display = displayLoc.getWorld().spawn(displayLoc, ItemDisplay.class);
            display.setItemStack(pumpkinItem);
            display.setPersistent(true);

            Transformation transform = new Transformation(new Vector3f(0.0F, 0.0F, 0.0F), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 1.0F), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F));
            display.setTransformation(transform);

            this.beaconDisplays.put(beacon.getName().toLowerCase(), display);
            this.plugin.getLogger().info("Created item display for beacon: " + beacon.getName());
        } else {
            this.plugin.getLogger().warning("Cannot create display for null beacon or location");
        }
    }

    /**
     * Update the item display for the beacon.
     *
     * @param beacon a beacon of any alignment.
     */
    public void updateBeaconDisplay(BeaconSite beacon) {
        ItemDisplay display = this.beaconDisplays.get(beacon.getName().toLowerCase());
        if (display != null && display.isValid()) {
            ItemStack pumpkinItem = new ItemStack(Material.CARVED_PUMPKIN);
            ItemMeta meta = pumpkinItem.getItemMeta();
            String expectedCMD = this.getCustomModelDataForState(beacon.getState());

            if (meta != null) {
                meta.setDisplayName("§6Beacon: " + beacon.getName());
                pumpkinItem.setItemMeta(meta);
            }

            pumpkinItem.setData(DataComponentTypes.CUSTOM_MODEL_DATA, (CustomModelData)CustomModelData.customModelData().addString(expectedCMD).build());
            display.setItemStack(pumpkinItem);

        } else {
            this.createBeaconDisplay(beacon);
        }
    }

    /**
     * Remove a beacon's item display.
     *
     * @param beaconName the name of the beacon being removed.
     * @param fallbackLocation the location of the beacon.
     */
    public void removeBeaconDisplay(String beaconName, Location fallbackLocation) {
        if (fallbackLocation != null && fallbackLocation.getWorld() != null) {
            fallbackLocation.getWorld().loadChunk(fallbackLocation.getBlockX() >> 4, fallbackLocation.getBlockZ() >> 4);
            this.plugin.getLogger().fine("Loaded chunk for beacon removal: " + beaconName);
        }

        ItemDisplay display = this.beaconDisplays.get(beaconName.toLowerCase());
        if (display != null && display.isValid()) {
            display.remove();
            this.beaconDisplays.remove(beaconName.toLowerCase());
            this.plugin.getLogger().info("Removed item display for beacon: " + beaconName);

        } else if (fallbackLocation != null && fallbackLocation.getWorld() != null) {
            int removed = 0;

            for(Entity entity : fallbackLocation.getWorld().getNearbyEntities(fallbackLocation, 5.0, 5.0, 5.0)) {
                if (entity instanceof ItemDisplay) {
                    entity.remove();
                    ++removed;
                }
            }

            if (removed > 0) {
                this.plugin.getLogger().info("Removed " + removed + " item displays near beacon: " + beaconName);
            }
        }
    }

    /**
     * Retrieve model data for the beacon alignments.
     *
     * @param state the current beacon alignment.
     * @return A 3-digit string code:<br>{@code "664"} for holy beacons, {@code "665"} for neutral beacons, {@code "666"} for evil beacons, {@code "668"} for cured beacons,
     */
    private String getCustomModelDataForState(BeaconSite.BeaconState state) {
        return switch (state) {
            case HOLY -> "664";
            case DESECRATED -> "666";
            case PERMANENTLY_DESECRATED -> "668";
            default -> "665";   // NEUTRAL also falls within default
        };
    }

    /**
     * Recreate the beacon item displays.
     */
    public void recreateAllDisplays() {
        this.plugin.getLogger().info("Recreating item displays for " + this.beacons.size() + " beacons...");
        this.forceLoadBeaconChunks();
        this.aggressiveCleanupItemDisplays();
        this.beaconDisplays.clear();

        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            for(BeaconSite beacon : this.beacons.values()) {
                this.createBeaconDisplay(beacon);
            }

            this.plugin.getLogger().info("Finished recreating beacon displays after aggressive cleanup");
        }, 1L);
    }

    /**
     * Clear the beacon displays.
     */
    public void cleanupAllDisplays() {
        for(ItemDisplay display : this.beaconDisplays.values()) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }

        this.beaconDisplays.clear();
        this.aggressiveCleanupItemDisplays();
        this.plugin.getLogger().info("Cleaned up all beacon displays using both methods");
    }

    /**
     * Remove all item displays near beacons.
     */
    private void aggressiveCleanupItemDisplays() {
        int totalRemoved = 0;

        for(BeaconSite beacon : this.beacons.values()) {
            Location location = beacon.getLocation();

            if (location != null && location.getWorld() != null) {
                for(Entity entity : location.getWorld().getNearbyEntities(location, 5.0, 5.0, 5.0)) {
                    if (entity instanceof ItemDisplay) {
                        entity.remove();
                        ++totalRemoved;
                    }
                }
            }
        }

        if (totalRemoved > 0) {
            this.plugin.getLogger().info("AGGRESSIVE CLEANUP: Removed " + totalRemoved + " item displays at beacon locations");
        }
    }

    /**
     * Refresh all the beacon item displays.
     */
    public void forceRefreshAllDisplays() {
        int refreshed = 0;

        for(BeaconSite beacon : this.beacons.values()) {
            this.updateBeaconDisplay(beacon);
            ++refreshed;
        }

        this.plugin.getLogger().info("Force refreshed " + refreshed + " beacon displays");
    }

    /**
     * Remove and recreate all the beacon displays.
     */
    public void validateDisplays() {
        this.aggressiveCleanupItemDisplays();
        this.beaconDisplays.clear();

        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            int created = 0;

            for(BeaconSite beacon : this.beacons.values()) {
                this.createBeaconDisplay(beacon);
                ++created;
            }

            this.plugin.getLogger().info("Display validation complete: AGGRESSIVE cleanup + " + created + " displays recreated");
        }, 1L);
    }

    /**
     * Retrieve an analysis on a beacon's item display.
     *
     * @param beaconName the beacon's name.
     * @return A description of the issue with the item display.
     */
    public String getBeaconDisplayDebugInfo(String beaconName) {
        BeaconSite beacon = this.getBeacon(beaconName);

        if (beacon == null) {
            return null;
        } else {
            ItemDisplay display = this.beaconDisplays.get(beaconName.toLowerCase());
            StringBuilder info = new StringBuilder();
            info.append("§e").append(beacon.getName()).append("§7:\n");
            info.append("  §7Beacon State: §f").append(beacon.getState().getDisplayName()).append("\n");
            info.append("  §7Expected CMD: §f").append(this.getCustomModelDataForState(beacon.getState())).append("\n");

            if (display == null) {
                info.append("  §cNo item display found!");
            } else if (!display.isValid()) {
                info.append("  §cItem display is invalid/removed!");
            } else {
                ItemStack item = display.getItemStack();

                if (item != null && item.getType() == Material.CARVED_PUMPKIN) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) {
                        info.append("  §cItem has no metadata!");

                    } else {
                        CustomModelData cmdComponent = (CustomModelData)item.getData(DataComponentTypes.CUSTOM_MODEL_DATA);
                        String actualCMD = cmdComponent != null && !cmdComponent.strings().isEmpty() ? (String)cmdComponent.strings().get(0) : "none";
                        String expectedCMD = this.getCustomModelDataForState(beacon.getState());
                        info.append("  §7Actual CMD: §f").append(actualCMD);

                        if (!actualCMD.equals(expectedCMD)) {
                            info.append(" §c(WRONG! Should be ").append(expectedCMD).append(")");
                        } else {
                            info.append(" §a(Correct)");
                        }

                        info.append("\n  §7Display Name: §f").append(meta.hasDisplayName() ? meta.getDisplayName() : "none");
                    }
                } else {
                    info.append("  §cItem display has wrong item: §f").append(item != null ? item.getType() : "null");
                }
            }

            return info.toString();
        }
    }

    /**
     * Retrieve the nearest holy beacon within regeneration range to the location.
     *
     * @param location a player's location.
     * @return The {@code BeaconSite} of the nearest beacon within a set distance.
     */
    public BeaconSite getNearestHolyBeacon(Location location) {
        if (location != null && location.getWorld() != null) {
            BeaconSite nearestHolyBeacon = null;
            double nearestDistance = Double.MAX_VALUE;

            for(BeaconSite beacon : this.beacons.values()) {
                if (beacon.getState() == BeaconState.HOLY) {
                    Location beaconLoc = beacon.getLocation();

                    if (beaconLoc != null && beaconLoc.getWorld().equals(location.getWorld())) {
                        double distance = beaconLoc.distance(location);

                        if (distance <= HOLY_REGENERATION_RANGE && distance < nearestDistance) {
                            nearestHolyBeacon = beacon;
                            nearestDistance = distance;
                        }
                    }
                }
            }

            return nearestHolyBeacon;
        } else {
            return null;
        }
    }

    /**
     * Retrieve the nearest holy beacon within the provided range to the location.
     *
     * @param location a player's location.
     * @param maxRange the distance to search within.
     * @return The {@code BeaconSite} of the nearest beacon within the given distance.
     */
    public BeaconSite getNearestHolyBeacon(Location location, double maxRange) {
        if (location != null && location.getWorld() != null) {
            BeaconSite nearestHolyBeacon = null;
            double nearestDistance = Double.MAX_VALUE;

            for(BeaconSite beacon : this.beacons.values()) {
                if (beacon.getState() == BeaconState.HOLY) {
                    Location beaconLoc = beacon.getLocation();

                    if (beaconLoc != null && beaconLoc.getWorld().equals(location.getWorld())) {
                        double distance = beaconLoc.distance(location);

                        if (distance <= maxRange && distance < nearestDistance) {
                            nearestHolyBeacon = beacon;
                            nearestDistance = distance;
                        }
                    }
                }
            }

            return nearestHolyBeacon;
        } else {
            return null;
        }
    }

    /**
     * Determine if a location is within the regeneration range of a holy beacon.
     *
     * @param location a player's location.
     * @return {@code true} if the location is within range of a holy beacon.
     */
    public boolean isInHolyRegenerationZone(Location location) {
        return this.getNearestHolyBeacon(location) != null;
    }

    /**
     * Show all passive beacon-affiliated particles to indicate the suppression and conversion circles.
     */
    private void showAllConversionAndSuppressionCircles() {
        for(BeaconSite beacon : this.beacons.values()) {
            Location particleLoc = beacon.getParticleLocation();

            if (particleLoc != null && particleLoc.getWorld() != null) {
                this.showConversionRangeCircle(beacon, particleLoc);

                if (beacon.getState() == BeaconState.HOLY) {
                    this.showSuppressionRangeCircle(beacon, particleLoc);
                }
            }
        }
    }

    /**
     * Add a new beacon to the world.
     *
     * @param name the new beacon name.
     * @param location the new beacon location.
     * @return {@code true} if the beacon was created.
     */
    public boolean addBeacon(String name, Location location) {
        if (this.beacons.containsKey(name.toLowerCase())) {
            return false;

        } else if (location != null && location.getWorld() != null) {
            BeaconSite beacon = new BeaconSite(name, location);
            this.beacons.put(name.toLowerCase(), beacon);
            this.createBeaconDisplay(beacon);
            this.saveBeacons();

            this.plugin.getLogger().info("Added new beacon: " + name + " at " + location.getWorld().getName() + " (" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")");
            return true;

        } else {
            return false;
        }
    }

    /**
     * Remove a beacon from the world.
     *
     * @param name the beacon's name.
     * @return {@code true} if the beacon was removed.
     */
    public boolean removeBeacon(String name) {
        BeaconSite removed = this.beacons.remove(name.toLowerCase());

        if (removed != null) {
            Location beaconLoc = removed.getLocation();

            if (beaconLoc != null) {
                beaconLoc.getBlock().setType(Material.AIR);
            }

            this.removeBeaconDisplay(name, removed.getLocation());
            this.saveBeacons();
            this.plugin.getLogger().info("Removed beacon and cleaned up display: " + name);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Retrieve the beacon using its name.
     *
     * @param name the beacon's name.
     * @return The {@code BeaconSite} of the beacon.
     */
    public BeaconSite getBeacon(String name) {
        return this.beacons.get(name.toLowerCase());
    }

    /**
     * Retrieve the beacons used by the game.
     *
     * @return The {@code Collection} of beacons in the plugin.
     */
    public Collection<BeaconSite> getAllBeacons() {
        return this.beacons.values();
    }

    /**
     * Retrieve the list of evil beacons.
     *
     * @return The {@code List} of desecrated beacons.
     */
    public List<BeaconSite> getDesecratedBeacons() {
        return (List)this.beacons.values().stream().filter((beacon) -> beacon.getState() == BeaconState.DESECRATED).collect(Collectors.toList());
    }

    /**
     * Retrieve the list of evil and purified beacons.
     *
     * @return The {@code List} of desecrated and permanently desecrated beacons.
     */
    public List<BeaconSite> getAllEvilBeacons() {
        return (List)this.beacons.values().stream().filter((beacon) -> beacon.getState() == BeaconState.DESECRATED || beacon.getState() == BeaconState.PERMANENTLY_DESECRATED).collect(Collectors.toList());
    }

    /**
     * Retrieve the list of holy beacons.
     *
     * @return The {@code List} of holy beacons.
     */
    public List<BeaconSite> getHolyBeacons() {
        return (List)this.beacons.values().stream().filter((beacon) -> beacon.getState() == BeaconState.HOLY).collect(Collectors.toList());
    }

    /**
     * Retrieve the list of neutral beacons.
     *
     * @return The {@code List} of neutral beacons.
     */
    public List<BeaconSite> getNeutralBeacons() {
        return (List)this.beacons.values().stream().filter((beacon) -> beacon.getState() == BeaconState.NEUTRAL).collect(Collectors.toList());
    }

    /**
     * Instantly convert a beacon into a holy alignment.
     *
     * @param name the beacon to convert.
     * @return {@code true} if the beacon was converted.
     */
    public boolean setBeaconHoly(String name) {
        BeaconSite beacon = this.beacons.get(name.toLowerCase());

        if (beacon != null) {
            this.cancelPendingNeutralBroadcast(name.toLowerCase());

            beacon.setState(BeaconState.HOLY);
            beacon.setLastChangedBy("Admin command");
            beacon.setConversionCooldownUntil(0L);

            this.updateBeaconDisplay(beacon);
            this.saveBeacons();

            this.plugin.getLogger().info("Set beacon '" + name + "' as holy (cooldown cleared)");
            this.triggerFirstBeaconConvertedEffects(beacon, false);
            this.plugin.getBeaconMajorityManager().updateBeaconMajorityBonuses();

            this.broadcastBeaconGainToTeam(beacon, BeaconState.HOLY);
            this.checkAndBroadcastCompleteControl();
            this.checkAndDisableEternalNight();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Remove the complete evil control final stand effects.
     */
    private void checkAndDisableEternalNight() {
        if (this.plugin.getSessionManager().isVampiresEternalNightActive()) {
            int evilCount = this.getAllEvilBeacons().size();
            int totalBeacons = this.getAllBeacons().size();

            if (evilCount < totalBeacons) {
                this.plugin.getLogger().info("ETERNAL NIGHT LIFTED - Not all beacons are evil anymore!");
                this.plugin.getSessionManager().setVampiresEternalNightActive(false);

                for(Player player : Bukkit.getOnlinePlayers()) {
                    if (this.plugin.getVampireManager().isHuman(player)) {
                        player.removePotionEffect(PotionEffectType.DARKNESS);
                    }

                    player.sendMessage("§6A beacon has been reclaimed by the light... The eternal darkness recedes.");
                }

                for(Player player : Bukkit.getOnlinePlayers()) {
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.MASTER, 1.0F, 1.0F);
                }
            }
        }
    }

    /**
     * Announce to the server that the first beacon has been converted.
     *
     * @param beacon the beacon that was first converted.
     * @param isDesecration {@code true} if a vampire converted the first beacon.
     */
    public void triggerFirstBeaconConvertedEffects(BeaconSite beacon, boolean isDesecration) {
        if (!this.plugin.getSessionManager().isFirstBeaconConvertedTriggered()) {
            this.plugin.getSessionManager().setFirstBeaconConvertedTriggered(true);
            Location beaconLocation = beacon.getLocation();
            String nearMessage, farMessage;

            if (isDesecration) {
                nearMessage = "\n§4A cold dread washes over you as the beacon's light twists into something sinister. The air grows heavy with malice... \n§7But just as suddenly, you feel a faint warmth stirring within, as if a force of light is rising to oppose the darkness. Perhaps there is still hope...\n";
                farMessage = "§4A dark beacon has been desecrated somewhere amongst the land, you feel its corrupted presence seep through the earth. \n§7But just as soon after, a faint warmth touches your heart, like a force of good is awakening to fight back. Probably just your imagination...\n";

            } else {
                nearMessage = "\n§6The beacons soft light warms your heart, filling you with peace. \n§7But just as soon as it activates, the air around you seems to thicken, like a dark presence is moving to snuff out the light... Have you awoken an evil force? Perhaps it is just superstition...\n";
                farMessage = "§6A holy beacon has been activated somewhere amongst the land, you feel its divine presence radiate through the earth. \n§7But just as soon after, a chill runs through your spine, like a strange dark force is moving in to snuff the light. Probably just your nerves...\n";
            }

            for(Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.MASTER, 0.8F, 0.7F);
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0, false, false));

                if (beaconLocation.getWorld() == player.getLocation().getWorld() && beaconLocation.distance(player.getLocation()) <= 50.0) {
                    player.sendMessage(nearMessage);
                } else {
                    player.sendMessage(farMessage);
                }
            }

            this.plugin.getLogger().info("FIRST BEACON CONVERTED EFFECTS triggered for beacon: " + beacon.getName() + " (desecration: " + isDesecration + ")");
        }
    }

    /**
     * Instantly convert a beacon into an evil alignment.
     *
     * @param name the beacon to convert.
     * @return {@code true} if the beacon was converted.
     */
    public boolean setBeaconDesecrated(String name) {
        BeaconSite beacon = this.beacons.get(name.toLowerCase());

        if (beacon != null) {
            this.cancelPendingNeutralBroadcast(name.toLowerCase());
            beacon.setState(BeaconState.DESECRATED);
            beacon.setLastChangedBy("Admin command");

            beacon.setConversionCooldownUntil(0L);
            this.updateBeaconDisplay(beacon);
            this.saveBeacons();

            this.plugin.getLogger().info("Set beacon '" + name + "' as desecrated (cooldown cleared)");
            this.plugin.getBeaconMajorityManager().updateBeaconMajorityBonuses();

            this.broadcastBeaconGainToTeam(beacon, BeaconState.DESECRATED);
            this.checkAndBroadcastCompleteControl();
            this.checkAndDisableHumansFinalStand();
            return true;

        } else {
            return false;
        }
    }

    /**
     * Instantly convert a beacon into a neutral alignment.
     *
     * @param name the beacon to convert.
     * @return {@code true} if the beacon was converted.
     */
    public boolean setBeaconNeutral(String name) {
        return this.setBeaconNeutral(name, false);
    }

    /**
     * Instantly convert a beacon into a neutral alignment.
     *
     * @param name the beacon to convert.
     * @param silent {@code true} if no notification should send to the server.
     * @return {@code true} if the beacon was converted.
     */
    public boolean setBeaconNeutral(String name, boolean silent) {
        BeaconSite beacon = this.beacons.get(name.toLowerCase());

        if (beacon != null) {
            BeaconSite.BeaconState previousState = beacon.getState();
            beacon.setState(BeaconState.NEUTRAL);
            beacon.setConversionCooldownUntil(0L);

            this.updateBeaconDisplay(beacon);
            this.saveBeacons();
            this.plugin.getLogger().info("Set beacon '" + name + "' as neutral (cooldown cleared)");
            this.plugin.getBeaconMajorityManager().updateBeaconMajorityBonuses();

            if (!silent) {
                this.broadcastNeutralConversionToAll(beacon, previousState);
                this.checkAndDisableHumansFinalStand();
                this.checkAndDisableEternalNight();
            }

            return true;
        } else {
            return false;
        }
    }

    /**
     * Remove the complete holy control final stand effects.
     */
    private void checkAndDisableHumansFinalStand() {
        if (this.plugin.getSessionManager().isHumansFinalStandActive()) {
            if (this.getHolyBeacons().size() < this.getAllBeacons().size()) {
                this.plugin.getLogger().info("HUMANS FINAL STAND ENDED - Not all beacons are holy anymore!");

                for(Player player : Bukkit.getOnlinePlayers()) {
                    if (this.plugin.getVampireManager().isVampire(player)) {
                        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20.0);
                    }

                    player.sendMessage("§4A beacon has fallen to darkness... The humans' final stand wavers.");
                }

                for(Player player : Bukkit.getOnlinePlayers()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.MASTER, 1.0F, 0.8F);
                }

                this.plugin.getSessionManager().setHumansFinalStandActive(false);
            }
        }
    }

    /**
     * Begin generating particles around beacons.
     */
    private void startParticleTask() {
        this.particleTask = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> this.showBeaconParticles(), 0L, 40L);
        this.plugin.getLogger().fine("Started beacon particle task");
    }

    /**
     * Generate the team alignment particles around the beacons.
     */
    private void showBeaconParticles() {
        for(BeaconSite beacon : this.beacons.values()) {
            Location particleLoc = beacon.getParticleLocation();

            if (particleLoc != null && particleLoc.getWorld() != null && beacon.shouldShowParticles()) {
                this.showParticleEffect(beacon, particleLoc);
            }
        }
    }

    /**
     * Retrieve all beacon names from the game.
     *
     * @return A {@code Set} of beacon names.
     */
    public Set<String> getBeaconNames() {
        return new HashSet<>(this.beacons.keySet());
    }

    /**
     * Create particles based on the beacon's alignment.
     *
     * @param beacon the beacon where particles are generated.
     * @param location THe location of the particles.
     */
    private void showParticleEffect(BeaconSite beacon, Location location) {
        try {
            switch (beacon.getState()) {
                case HOLY:
                    this.showHolyParticles(location);
                    break;
                case DESECRATED:
                    this.showDesecratedParticles(location);
                    break;
                case PERMANENTLY_DESECRATED:
                case NEUTRAL:
            }
        } catch (Exception e) {
            this.plugin.getLogger().warning("Failed to show particles for beacon " + beacon.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Create holy particles around a location.
     *
     * @param location the center of the particle spawns.
     */
    private void showHolyParticles(Location location) {
        double radius = 1.5;

        for(int i = 0; i < 8; ++i) {
            double angle = (Math.PI * 2D) * i / 8.0;
            Location particleLoc = location.clone().add(Math.cos(angle) * radius, Math.sin(System.currentTimeMillis() / 1000.0) * 0.3, Math.sin(angle) * radius);
            location.getWorld().spawnParticle(Particle.WHITE_ASH, particleLoc, 2, 0.1, 0.1, 0.1, 0.01);
        }

        location.getWorld().spawnParticle(Particle.ENCHANT, location, 5, 0.5, 0.5, 0.5, 0.5);

        if (Math.random() < 0.3) {
            location.getWorld().spawnParticle(Particle.END_ROD, location, 3, 0.3, 0.3, 0.3, 0.1);
        }

    }

    /**
     * Create evil particles around a location.
     *
     * @param location the center of the particle spawns.
     */
    private void showDesecratedParticles(Location location) {
        location.getWorld().spawnParticle(Particle.LARGE_SMOKE, location, 3, 0.3, 0.1, 0.3, 0.02);
        double radius = 1.2;

        for(int i = 0; i < 6; ++i) {
            double angle = (Math.PI * 2D) * i / 6.0;

            Location particleLoc = location.clone().add(Math.cos(angle) * radius, -Math.sin(System.currentTimeMillis() / 800.0) * 0.2, Math.sin(angle) * radius);
            Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(139, 0, 0), 1.0F);
            location.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0.1, 0.1, 0.1, dustOptions);
        }

        if (Math.random() < 0.4) {
            location.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, location, 2, 0.2, 0.2, 0.2, 0.01);
        }

        if (Math.random() < 0.2) {
            location.getWorld().spawnParticle(Particle.CRIMSON_SPORE, location, 4, 0.5, 0.5, 0.5, 0.01);
        }
    }

    /**
     * Create particles to indicate the beacon's conversion range.
     *
     * @param beacon the beacon to circle.
     * @param center the location of the beacon.
     */
    private void showConversionRangeCircle(BeaconSite beacon, Location center) {
        if (center != null && center.getWorld() != null) {
            int circlePoints = 24;
            double radius = BEACON_CONVERSION_RANGE;
            long time = System.currentTimeMillis();

            int currentPointIndex = (int)(time / 50L % (long)circlePoints);
            double angle = (Math.PI * 2D) * (double)currentPointIndex / (double)circlePoints;
            double rotationOffset = (double)time / 8000.0 % (Math.PI * 2D);
            angle += rotationOffset;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            Location circlePoint = center.clone().add(x, -1.2, z);
            this.showConversionRangeParticle(beacon.getState(), circlePoint);
        } else {
            this.plugin.getLogger().fine("Invalid center location for beacon: " + beacon.getName());
        }
    }

    /**
     * Create particles to indicate the beacon's vampiric suppression range.
     *
     * @param beacon the beacon to circle.
     * @param center the location of the beacon.
     */
    private void showSuppressionRangeCircle(BeaconSite beacon, Location center) {
        if (center != null && center.getWorld() != null) {
            int circlePoints = 48;
            double radius = HOLY_SUPPRESSION_RANGE;
            long time = System.currentTimeMillis();

            int currentPointIndex = (int)(time / 50L % (long)circlePoints);
            double angle = (Math.PI * 2D) * currentPointIndex / (double)circlePoints;
            double rotationOffset = time / 12000.0 % (Math.PI * 2D);
            angle += rotationOffset;
            double x = Math.cos(angle) * radius, z = Math.sin(angle) * radius;

            Location circlePoint = center.clone().add(x, 0.0, z);
            Location highestPoint = this.plugin.getWorld().getHighestBlockAt(circlePoint).getLocation();
            highestPoint.add(0.0, 1.2, 0.0);
            this.showSuppressionRangeParticle(highestPoint);

        } else {
            this.plugin.getLogger().fine("Invalid center location for beacon: " + beacon.getName());
        }
    }

    /**
     * Find the highest block at a location.
     *
     * @param location the location to check.
     * @return The {@code Location} of the lowest block of surface air.
     */
    private Location findHighestBlock(Location location) {
        if (location != null && location.getWorld() != null) {
            Location highest = location.clone();
            int maxY = location.getWorld().getMaxHeight() - 1;
            int minY = location.getWorld().getMinHeight();

            for(int y = maxY; y >= minY; --y) {
                highest.setY(y);
                Material blockType = highest.getBlock().getType();

                if (blockType.isSolid() && blockType != Material.AIR) {
                    return highest.clone().add(0.0, 1.5, 0.0);
                }
            }

            return location.clone().add(0.0, 0.5, 0.0);
        } else {
            return null;
        }
    }

    /**
     * Spawn a team-aligned particle for the beacon's conversion range.
     *
     * @param state the beacon's team alignment.
     * @param location the beacon's location.
     */
    private void showConversionRangeParticle(BeaconSite.BeaconState state, Location location) {
        if (location != null && location.getWorld() != null) {
            try {
                switch (state) {
                    case HOLY:
                        location.getWorld().spawnParticle(Particle.END_ROD, location, 1, 0.0, 0.0, 0.0, 0.0);
                        break;
                    case DESECRATED:
                        Particle.DustOptions darkRedDust = new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.2F);
                        location.getWorld().spawnParticle(Particle.DUST, location, 1, 0.0, 0.0, 0.0, darkRedDust);
                    case PERMANENTLY_DESECRATED:
                    case NEUTRAL:
                }
            } catch (Exception e) {
                this.plugin.getLogger().warning("Failed to show conversion range particle for beacon : " + e.getMessage());
            }
        }
    }

    /**
     * Spawn a particle for the beacon's suppression range.
     *
     * @param location
     */
    private void showSuppressionRangeParticle(Location location) {
        if (location != null && location.getWorld() != null) {
            try {
                location.getWorld().spawnParticle(Particle.END_ROD, location, 1, 0.0, 0.0, 0.0, 0.0);
            } catch (Exception e) {
                this.plugin.getLogger().warning("Failed to show suppression range particle: " + e.getMessage());
            }
        }
    }

    /**
     * Return the highest solid block within 5 vertical blocks of the location.
     *
     * @param location a location.
     * @return A location of the first solid block at or under (within 5 blocks) the location.
     */
    private Location findGroundLevel(Location location) {
        Location ground = location.clone();

        for(int y = 0; y >= -5; --y) {
            ground.setY(location.getY() + y);

            if (ground.getBlock().getType().isSolid()) {
                return ground.add(0.0, 1.2, 0.0);
            }
        }

        return location.clone().add(0.0, 0.5, 0.0);
    }

    /**
     * Retrieve all beacons within the provided distance.
     *
     * @param location a player's location.
     * @param range the distance to search within.
     * @return A {@code List} of beacons within range of the location.
     */
    public List<BeaconSite> getBeaconsInRange(Location location, double range) {
        List<BeaconSite> nearby = new ArrayList<>();

        for(BeaconSite beacon : this.beacons.values()) {
            Location beaconLoc = beacon.getLocation();

            if (beaconLoc != null && beaconLoc.getWorld().equals(location.getWorld()) && beaconLoc.distance(location) <= range) {
                nearby.add(beacon);
            }
        }

        return nearby;
    }

    /**
     * Determine if the location is inside the vampiric suppression range of a holy beacon.
     *
     * @param location a player's location.
     * @return A {@code BeaconSite} of the closest holy beacon to the location.
     */
    public BeaconSite checkHolySuppression(Location location) {
        if (location != null && location.getWorld() != null) {
            BeaconSite closestHolyBeacon = null;
            double closestDistance = Double.MAX_VALUE;

            for(BeaconSite beacon : this.beacons.values()) {
                if (beacon.getState() == BeaconState.HOLY) {
                    Location beaconLoc = beacon.getLocation();

                    if (beaconLoc != null && beaconLoc.getWorld().equals(location.getWorld())) {
                        double distance = beaconLoc.distance(location);

                        if (distance <= HOLY_SUPPRESSION_RANGE && distance < closestDistance) {
                            closestHolyBeacon = beacon;
                            closestDistance = distance;
                        }
                    }
                }
            }

            return closestHolyBeacon;
        } else {
            return null;
        }
    }

    /**
     * Retrieve all beacon names with an icon indicating their alignment.
     *
     * @return A {@code List} of beacon names.
     */
    public List<String> getBeaconList() {
        List<String> list = new ArrayList<>();
        if (this.beacons.isEmpty()) {
            list.add("§7No beacons configured.");
            return list;

        } else {
            list.add("§6§l=== BEACON SITES ===");
            list.add("§7Total: §e" + this.beacons.size() + " beacons");
            list.add("");
            Map<BeaconSite.BeaconState, List<BeaconSite>> grouped = new HashMap<>();

            for(BeaconSite.BeaconState state : BeaconState.values()) {
                grouped.put(state, new ArrayList<>());
            }

            for(BeaconSite beacon : this.beacons.values()) {
                ((List)grouped.get(beacon.getState())).add(beacon);
            }

            for(BeaconSite.BeaconState state : BeaconState.values()) {
                List<BeaconSite> stateBeacons = (List)grouped.get(state);

                if (!stateBeacons.isEmpty()) {
                    String icon = "";

                    switch (state) {
                        case HOLY:
                            icon = "✦ ";
                            break;
                        case DESECRATED:
                            icon = "☠ ";
                        case PERMANENTLY_DESECRATED:
                            break;
                        case NEUTRAL:
                            icon = "◯ ";
                            break;
                        default:
                            break;
                    }

                    list.add(state.getColorCode() + "§l" + icon + state.getDisplayName() + " Beacons: §r§7(" + stateBeacons.size() + ")");

                    for(BeaconSite beacon : stateBeacons) {
                        list.add("  " + beacon.getStatusString().replace("\n", "\n  "));
                    }

                    list.add("");
                }
            }

            return list;
        }
    }

    /**
     * Write the current condition of the beacons into the file.
     */
    public void saveBeacons() {
        synchronized(this) {
            try {
                this.plugin.getLogger().fine("Saving " + this.beacons.size() + " beacons to file...");

                for(Map.Entry<String, BeaconSite> entry : this.beacons.entrySet()) {
                    BeaconSite beacon = (BeaconSite)entry.getValue();

                    if (beacon == null) {
                        this.plugin.getLogger().severe("Found null beacon with key: " + (String)entry.getKey());
                    } else if (beacon.getName() == null || beacon.getWorldName() == null) {
                        this.plugin.getLogger().severe("Found beacon with null fields: " + (String)entry.getKey());
                    }
                }

                File backupFile = new File(this.dataFile.getParent(), "beacons_backup.json");

                if (this.dataFile.exists()) {
                    try {
                        Files.copy(this.dataFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception e) {
                        this.plugin.getLogger().warning("Failed to create backup: " + e.getMessage());
                    }
                }

                Type type = (new TypeToken<Map<String, BeaconSite>>() {}).getType();
                String jsonString = this.gson.toJson(this.beacons, type);

                if (!this.beacons.isEmpty() && jsonString.length() < 50) {
                    this.plugin.getLogger().severe("JSON output suspiciously small for " + this.beacons.size() + " beacons. JSON: " + jsonString);
                    this.plugin.getLogger().severe("ABORTING SAVE to prevent data loss!");
                    return;
                }

                File tempFile = new File(this.dataFile.getParent(), "beacons_temp.json");
                FileWriter writer = new FileWriter(tempFile);

                try {
                    writer.write(jsonString);
                    writer.flush();

                } catch (Throwable failedWrite) {
                    try {
                        writer.close();
                    } catch (Throwable failedClose) {
                        failedWrite.addSuppressed(failedClose);
                    }

                    throw failedWrite;
                }

                writer.close();
                if (tempFile.exists() && tempFile.length() > 0L) {
                    if (this.dataFile.exists()) {
                        this.dataFile.delete();
                    }

                    if (tempFile.renameTo(this.dataFile)) {
                        this.plugin.getLogger().fine("Successfully saved " + this.beacons.size() + " beacons to file.");
                    } else {
                        this.plugin.getLogger().severe("Failed to rename temp file to main file!");
                    }
                } else {
                    this.plugin.getLogger().severe("Temp file was not created properly!");
                }

                if (tempFile.exists()) {
                    tempFile.delete();
                }
            } catch (Exception e) {
                this.plugin.getLogger().severe("Failed to save beacons: " + e.getMessage());
                e.printStackTrace();
                this.plugin.getLogger().severe("Beacon map size: " + this.beacons.size());
                this.plugin.getLogger().severe("Data file path: " + this.dataFile.getAbsolutePath());
                this.plugin.getLogger().severe("Can write to parent directory: " + this.dataFile.getParentFile().canWrite());
            }
        }
    }

    /**
     * Create the default Oakhurst beacon setup.
     */
    private void createDefaultBeacons() {
        this.beacons.clear();

        World world = this.plugin.getServer().getWorld("world");

        if (world == null) {
            this.plugin.getLogger().severe("World 'world' not found! Cannot create default beacons.");

        } else {
            BeaconSite town = new BeaconSite("Town", new Location(world, 79.5, 86.0, 440.5));
            town.setState(BeaconState.NEUTRAL);
            town.setLastChangedBy("Default");
            this.beacons.put("town", town);

            BeaconSite castle = new BeaconSite("Castle", new Location(world, 39.5, 104.0, -170.5));
            castle.setState(BeaconState.DESECRATED);
            castle.setLastChangedBy("Default");
            this.beacons.put("castle", castle);

            BeaconSite crypt = new BeaconSite("Crypt", new Location(world, 454.5, 138.0, 395.5));
            crypt.setState(BeaconState.NEUTRAL);
            crypt.setLastChangedBy("Default");
            this.beacons.put("crypt", crypt);

            BeaconSite obelisk = new BeaconSite("Obelisk", new Location(world, -374.5, 72.0, 73.5F));
            obelisk.setState(BeaconState.NEUTRAL);
            obelisk.setLastChangedBy("Default");
            this.beacons.put("obelisk", obelisk);

            BeaconSite paleoakforest = new BeaconSite("PaleOakForest", new Location(world, 481.5, 113.0, -433.5));
            paleoakforest.setState(BeaconState.NEUTRAL);
            paleoakforest.setLastChangedBy("Default");
            this.beacons.put("paleoakforest", paleoakforest);

            BeaconSite lake = new BeaconSite("Lake", new Location(world, -328.5, 76.0, 492.5));
            lake.setState(BeaconState.NEUTRAL);
            lake.setLastChangedBy("Default");
            this.beacons.put("lake", lake);

            BeaconSite ruinedtower = new BeaconSite("RuinedTower", new Location(world, -300.5, 231.0, -371.5));
            ruinedtower.setState(BeaconState.NEUTRAL);
            ruinedtower.setLastChangedBy("Default");
            this.beacons.put("ruinedtower", ruinedtower);

            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> this.recreateAllDisplays(), 20L);
        }
    }

    /**
     * Load the beacon setup into the active plugin game.
     */
    public void loadBeacons() {
        if (!this.dataFile.exists()) {
            this.plugin.getLogger().info("No beacons file found, creating default beacons...");
            this.createDefaultBeacons();
            this.saveBeacons();
            this.plugin.getLogger().info("Created " + this.beacons.size() + " default beacons.");

        } else {
            File backupFile = new File(this.dataFile.getParent(), "beacons_backup.json");
            this.plugin.getLogger().info("Loading beacons from file: " + this.dataFile.getAbsolutePath());

            try {
                try (FileReader reader = new FileReader(this.dataFile)) {
                    StringBuilder content = new StringBuilder();
                    char[] buffer = new char[1024];
                    FileReader debugReader = new FileReader(this.dataFile);

                    int bytesRead;
                    while((bytesRead = debugReader.read(buffer)) != -1) {
                        content.append(buffer, 0, bytesRead);
                    }

                    debugReader.close();
                    String fileContent = content.toString().trim();

                    if (fileContent.isEmpty()) {
                        this.plugin.getLogger().severe("Beacons file is empty!");
                        if (!backupFile.exists()) {
                            return;
                        }

                        this.plugin.getLogger().info("Attempting to restore from backup file...");

                        try {
                            Files.copy(backupFile.toPath(), this.dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            this.plugin.getLogger().info("Restored from backup, retrying load...");
                            this.loadBeacons();
                            return;

                        } catch (Exception e) {
                            this.plugin.getLogger().severe("Failed to restore from backup: " + e.getMessage());
                            return;
                        }
                    }

                    Type type = (new TypeToken<Map<String, BeaconSite>>() {
                    }).getType();
                    Map<String, BeaconSite> loadedBeacons = (Map)this.gson.fromJson(fileContent, type);
                    if (loadedBeacons != null) {
                        int validBeacons = 0, invalidBeacons = 0;

                        for(Map.Entry<String, BeaconSite> entry : loadedBeacons.entrySet()) {
                            BeaconSite beacon = (BeaconSite)entry.getValue();
                            if (beacon == null) {
                                this.plugin.getLogger().warning("Beacon with key '" + (String)entry.getKey() + "' is null");
                                ++invalidBeacons;
                            } else if (beacon.getName() != null && beacon.getWorldName() != null) {
                                ++validBeacons;
                            } else {
                                this.plugin.getLogger().warning("Beacon '" + (String)entry.getKey() + "' has null fields");
                                ++invalidBeacons;
                            }
                        }

                        this.beacons.clear();
                        this.beacons.putAll(loadedBeacons);

                        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                            this.recreateAllDisplays();
                            this.validateDisplays();
                        }, 10L);

                        this.plugin.getLogger().info("Loaded " + this.beacons.size() + " beacons from file.");
                        return;
                    }

                    this.plugin.getLogger().severe("Gson returned null when parsing beacons file!");
                }

                return;

            } catch (IOException e) {
                this.plugin.getLogger().severe("Failed to load beacons: " + e.getMessage());
                e.printStackTrace();

            } catch (JsonSyntaxException e) {
                this.plugin.getLogger().severe("Failed to parse beacons file - JSON syntax error: " + e.getMessage());
                e.printStackTrace();

                if (backupFile.exists()) {
                    this.plugin.getLogger().info("Attempting to restore from backup due to JSON corruption...");

                    try {
                        Files.copy(backupFile.toPath(), this.dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        this.plugin.getLogger().info("Restored from backup, retrying load...");
                        this.loadBeacons();

                    } catch (Exception backupError) {
                        this.plugin.getLogger().severe("Failed to restore from backup: " + backupError.getMessage());
                    }
                }
            } catch (Exception e) {
                this.plugin.getLogger().severe("Unexpected error loading beacons: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Load the beacons from the file.
     */
    public void reloadBeacons() {
        this.loadBeacons();
        this.plugin.getLogger().info("Reloaded beacons from file.");
    }

    /**
     * Convert the cooldown value from a system counter into a session timer value.
     */
    private void migrateBeaconCooldownsToSessionTime() {
        this.plugin.getLogger().info("Migrating existing beacon cooldowns from system time to session time...");
        long currentSystemTime = System.currentTimeMillis();
        long currentSessionTime = this.plugin.getSessionManager().getSessionTime();
        int migratedBeacons = 0;

        for(BeaconSite beacon : this.beacons.values()) {
            long systemCooldownUntil = beacon.getConversionCooldownUntil();

            if (systemCooldownUntil > currentSystemTime) {
                long remainingSystemTime = systemCooldownUntil - currentSystemTime;
                long newSessionCooldownUntil = currentSessionTime + remainingSystemTime / 2L;
                beacon.setConversionCooldownUntil(newSessionCooldownUntil);
                ++migratedBeacons;
            }
        }

        this.plugin.getLogger().info("Migrated " + migratedBeacons + " beacon cooldowns to session time");

        if (migratedBeacons > 0) {
            this.saveBeacons();
        }
    }

    /**
     * Count how many beacons are in each alignment.
     *
     * @return A {@code Map} of Beacon states to the number of beacons in that state.
     */
    public Map<BeaconSite.BeaconState, Integer> getStateStats() {
        Map<BeaconSite.BeaconState, Integer> stats = new HashMap<>();

        // Set the number of each alignment to 0
        for(BeaconSite.BeaconState state : BeaconState.values()) {
            stats.put(state, 0);
        }

        // Increment the alignment's counter when a beacon is found to match it
        for(BeaconSite beacon : this.beacons.values()) {
            stats.put(beacon.getState(), stats.get(beacon.getState()) + 1);
        }

        return stats;
    }

    /**
     * Ensure that all beacons have a location attached.
     */
    public void validateBeacons() {
        List<String> invalidBeacons = new ArrayList<>();

        for(Map.Entry<String, BeaconSite> entry : this.beacons.entrySet()) {
            if (((BeaconSite)entry.getValue()).getLocation() == null) {
                invalidBeacons.add((String)entry.getKey());
            }
        }

        if (!invalidBeacons.isEmpty()) {
            this.plugin.getLogger().warning("Found " + invalidBeacons.size() + " beacons with invalid worlds:");

            for(String name : invalidBeacons) {
                this.plugin.getLogger().warning("  - " + name + " (world: " + (this.beacons.get(name)).getWorldName() + ")");
            }
        }
    }

    /**
     * Clean up the numerous maintenance and particle tasks before shutting down the manager.
     */
    public void shutdown() {
        this.cleanupAllDisplays();

        if (this.cooldownTrackingTask != null) {
            this.cooldownTrackingTask.cancel();
            this.cooldownTrackingTask = null;
            this.plugin.getLogger().info("Stopped beacon cooldown tracking task");
        }

        if (this.beaconMaintenanceTask != null) {
            this.beaconMaintenanceTask.cancel();
            this.beaconMaintenanceTask = null;
            this.plugin.getLogger().info("Stopped beacon maintenance task");
        }

        if (this.autoSaveTask != null) {
            this.autoSaveTask.cancel();
            this.autoSaveTask = null;
            this.plugin.getLogger().fine("Stopped beacon auto-save task");
        }

        if (this.particleTask != null) {
            this.particleTask.cancel();
            this.particleTask = null;
            this.plugin.getLogger().fine("Stopped beacon particle task");
        }

        if (this.conversionCircleParticleTask != null) {
            this.conversionCircleParticleTask.cancel();
            this.conversionCircleParticleTask = null;
            this.plugin.getLogger().info("Stopped conversion circle particle task");
        }

        if (this.holyRegenTask != null) {
            this.holyRegenTask.cancel();
            this.holyRegenTask = null;
            this.plugin.getLogger().info("Stopped holy beacon regeneration task");
        }

        for(BukkitTask task : this.pendingNeutralBroadcasts.values()) {
            if (task != null) {
                task.cancel();
            }
        }

        this.pendingNeutralBroadcasts.clear();
        this.plugin.getLogger().info("Cancelled all pending neutral broadcast messages");
        this.saveBeacons();
        this.plugin.getLogger().info("BeaconManager shutdown - displays cleaned up and data saved.");
    }

    /**
     * Alert the server that a beacon has become neutral after a delay.
     *
     * @param beacon the converted beacon.
     * @param previousState the beacon's previous alignment.
     */
    public void broadcastNeutralConversionToAll(BeaconSite beacon, BeaconSite.BeaconState previousState) {
        String beaconKey = beacon.getName().toLowerCase();
        this.cancelPendingNeutralBroadcast(beaconKey);
        int delaySeconds = this.plugin.getConfigManager().getBeaconNeutralAnnouncementDelaySeconds();
        long delayTicks = (long)delaySeconds * 20L;

        BukkitTask task = this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            if (beacon.getState() == BeaconState.NEUTRAL) {
                String message;
                if (previousState == BeaconState.HOLY) {
                    message = "§7Beacon §e" + beacon.getName() + " §7has lost its divine protection...";
                } else if (previousState == BeaconState.DESECRATED) {
                    message = "§7Beacon §e" + beacon.getName() + " §7has been cleansed of dark influence...";
                } else {
                    message = "§7Beacon §e" + beacon.getName() + " §7is now neutral.";
                }

                for(Player player : this.plugin.getServer().getOnlinePlayers()) {
                    player.sendMessage(message);
                }
            }

            this.pendingNeutralBroadcasts.remove(beaconKey);
        }, delayTicks);

        this.pendingNeutralBroadcasts.put(beaconKey, task);
    }

    /**
     * Cancel the scheduled alert about a beacon becoming neutral.
     *
     * @param beaconKey an identifier key of the converted beacon.
     */
    public void cancelPendingNeutralBroadcast(String beaconKey) {
        BukkitTask existingTask = this.pendingNeutralBroadcasts.remove(beaconKey.toLowerCase());

        if (existingTask != null) {
            existingTask.cancel();
            this.plugin.getLogger().fine("Cancelled pending neutral broadcast for beacon: " + beaconKey);
        }
    }

    /**
     * Alert the server that a beacon has been converted onto a team's side.
     *
     * @param beacon the converted beacon.
     * @param newState the beacon's new alignment.
     */
    private void broadcastBeaconGainToTeam(BeaconSite beacon, BeaconSite.BeaconState newState) {
        String message;

        if (newState == BeaconState.HOLY) {
            message = "§aBeacon §e" + beacon.getName() + " §ahas been blessed with divine energy!";
        } else {
            if (newState != BeaconState.DESECRATED) {
                return;
            }

            message = "§4Beacon §e" + beacon.getName() + " §4has been consumed by dark forces!";
        }

        for(Player player : this.plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    /**
     * Alert the server that all beacons are aligned to one team.
     */
    public void checkAndBroadcastCompleteControl() {
        if (!this.beacons.isEmpty()) {
            boolean allDesecrated = true;
            boolean allHoly = true;

            for(BeaconSite beacon : this.beacons.values()) {
                if (beacon.getState() != BeaconState.DESECRATED && beacon.getState() != BeaconState.PERMANENTLY_DESECRATED) {
                    allDesecrated = false;
                }

                if (beacon.getState() != BeaconState.HOLY) {
                    allHoly = false;
                }
            }

            if (allDesecrated) {
                boolean humansRemain = false;

                for(Player player : this.plugin.getServer().getOnlinePlayers()) {
                    if (player.getGameMode() == GameMode.SURVIVAL && this.plugin.getVampireManager().isHuman(player)) {
                        humansRemain = true;
                        break;
                    }
                }

                String message;
                if (humansRemain) {
                    message = "§cBut while humans remain... Hope still stands...";
                } else {
                    message = "§cYou are free of your chains, creatures of the night...";
                }

                for(Player player : this.plugin.getServer().getOnlinePlayers()) {
                    if (this.plugin.getVampireManager().isVampire(player)) {
                        player.sendMessage(message);
                    }
                }
            } else if (allHoly) {
                boolean vampiresRemain = false;

                for(Player player : this.plugin.getServer().getOnlinePlayers()) {
                    if (player.getGameMode() == GameMode.SURVIVAL && this.plugin.getVampireManager().isVampire(player)) {
                        vampiresRemain = true;
                        break;
                    }
                }

                if (vampiresRemain) {
                    for(Player player : this.plugin.getServer().getOnlinePlayers()) {
                        if (this.plugin.getVampireManager().isHuman(player)) {
                            player.sendMessage("§aBut while vampires remain... Now only the vampires lie between you and freedom...");
                        }
                    }
                } else {
                    for(Player player : this.plugin.getServer().getOnlinePlayers()) {
                        if (this.plugin.getVampireManager().isVampire(player)) {
                            player.sendTitle("§c§lDEFEAT", "§7The light has prevailed", 20, 100, 40);
                            player.sendMessage("");
                            player.sendMessage("§cAll beacons shine with divine light...");
                            player.sendMessage("§cLight reigns supreme over Frostvein. You have lost.");
                            player.sendMessage("");
                            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 1.0F, 1.0F);
                        } else {
                            player.sendTitle("§a§lVICTORY", "§eThe darkness has been vanquished", 20, 100, 40);
                            player.sendMessage("");
                            player.sendMessage("§aAll beacons shine with divine light...");
                            player.sendMessage("§aLight reigns supreme over Frostvein. And yet, the storm rages on.");
                            player.sendMessage("");
                            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.MASTER, 1.0F, 1.0F);
                        }
                    }
                }
            }
        }
    }
}
