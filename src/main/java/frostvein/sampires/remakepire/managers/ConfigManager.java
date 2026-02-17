package frostvein.sampires.remakepire.managers;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import frostvein.sampires.remakepire.beacons.BeaconSite;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.loadConfig();
    }

    public void loadConfig() {
        this.plugin.saveDefaultConfig();
        this.plugin.reloadConfig();
        this.config = this.plugin.getConfig();
    }

    public void saveConfig() {
        this.plugin.saveConfig();
    }

    public List<Location> getTomeChestLocations() {
        List<String> locationStrings = this.config.getStringList("tome-chests.locations");
        List<Location> locations = new ArrayList();
        World world = this.plugin.getServer().getWorld("world");
        if (world == null) {
            this.plugin.getLogger().severe("World 'world' not found! Cannot load tome chest locations.");
            return locations;
        } else {
            for(String locString : locationStrings) {
                try {
                    String[] parts = locString.split(",");
                    if (parts.length == 3) {
                        int x = Integer.parseInt(parts[0].trim());
                        int y = Integer.parseInt(parts[1].trim());
                        int z = Integer.parseInt(parts[2].trim());
                        locations.add(new Location(world, (double)x, (double)y, (double)z));
                    }
                } catch (NumberFormatException var10) {
                    this.plugin.getLogger().warning("Invalid tome chest location format: " + locString);
                }
            }

            return locations;
        }
    }

    public boolean addTomeChestLocation(Location location) {
        List<String> locations = this.config.getStringList("tome-chests.locations");
        String locationString = String.format("%d,%d,%d", location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (locations.contains(locationString)) {
            return false;
        } else {
            locations.add(locationString);
            this.config.set("tome-chests.locations", locations);
            this.saveConfig();
            return true;
        }
    }

    public boolean removeTomeChestLocation(Location location) {
        List<String> locations = this.config.getStringList("tome-chests.locations");
        String locationString = String.format("%d,%d,%d", location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (!locations.contains(locationString)) {
            return false;
        } else {
            locations.remove(locationString);
            this.config.set("tome-chests.locations", locations);
            this.saveConfig();
            return true;
        }
    }

    public int getVampireBatCooldown() {
        return this.config.getInt("abilities.vampire.bat-cooldown", 900);
    }

    public int getVampireLungeCooldown() {
        return this.config.getInt("abilities.vampire.lunge-cooldown", 45);
    }

    public int getVampireVanishCooldown() {
        return this.config.getInt("abilities.vampire.vanish-cooldown", 420);
    }

    public int getVampireStormCallCooldown() {
        return this.config.getInt("abilities.vampire.storm-call-cooldown", 7200);
    }

    public int getVampireBeaconTeleportCooldown() {
        return this.config.getInt("abilities.vampire.beacon-teleport-cooldown", 300);
    }

    public int getVampireVisionCooldown() {
        return this.config.getInt("abilities.vampire.vampire-vision-cooldown", 1);
    }

    public int getTomeBlessingCooldown() {
        return this.config.getInt("abilities.tome.blessing-cooldown", 7200);
    }

    public int getTomeBanishUndeadCooldown() {
        return this.config.getInt("abilities.tome.banish-undead-cooldown", 900);
    }

    public int getTomeHolyWordCooldown() {
        return this.config.getInt("abilities.tome.holy-word-cooldown", 600);
    }

    public int getTomeEnlightenedEyeCooldown() {
        return this.config.getInt("abilities.tome.enlightened-eye-cooldown", 900);
    }

    public int getTomeLanternThrashCooldown() {
        return this.config.getInt("abilities.tome.lantern-thrash-cooldown", 300);
    }

    public int getTomePrayerOfFaithCooldown() {
        return this.config.getInt("abilities.tome.prayer-of-faith-cooldown", 900);
    }

    public int getTomeRallyingCryCooldown() {
        return this.config.getInt("abilities.tome.rallying-cry-cooldown", 1200);
    }

    public int getTomeShoulderBargeCooldown() {
        return this.config.getInt("abilities.tome.shoulder-barge-cooldown", 300);
    }

    public int getTomeTurnUndeadCooldown() {
        return this.config.getInt("abilities.tome.turn-undead-cooldown", 1800);
    }

    public int getTomeUncannyDirectionCooldown() {
        return this.config.getInt("abilities.tome.uncanny-direction-cooldown", 30);
    }

    public int getTomeUnnaturalHasteCooldown() {
        return this.config.getInt("abilities.tome.unnatural-haste-cooldown", 900);
    }

    public int getTomeWayOfTheLandCooldown() {
        return this.config.getInt("abilities.tome.way-of-the-land-cooldown", 600);
    }

    public int getTomeWayOfTheLumberjackCooldown() {
        return this.config.getInt("abilities.tome.way-of-the-lumberjack-cooldown", 600);
    }

    public int getTomeWayOfTheProspectorCooldown() {
        return this.config.getInt("abilities.tome.way-of-the-prospector-cooldown", 600);
    }

    public int getTomeStopTheBleedingCooldown() {
        return this.config.getInt("abilities.tome.stop-the-bleeding-cooldown", 7200);
    }

    public int getThirstDepletionMinutes() {
        return this.config.getInt("thirst.depletion-minutes", 120);
    }

    public int getMaxFeedingThirstPerSession() {
        return this.config.getInt("thirst.max-feeding-per-session", 60);
    }

    public long getBeaconConversionTimeMs() {
        long seconds = this.config.getLong("beacons.conversion-time-seconds", 300L);
        return seconds * 1000L;
    }

    public long getBeaconConversionCooldownMs() {
        long minutes = this.config.getLong("beacons.conversion-cooldown-minutes", 60L);
        return minutes * 60L * 1000L;
    }

    public double getBeaconHumanSpeedMultiplier() {
        return this.config.getDouble("beacons.human-speed-multiplier", (double)1.5F);
    }

    public double getBeaconFinalStandMultiplier() {
        return this.config.getDouble("beacons.final-stand-multiplier", (double)6.0F);
    }

    public int getBeaconNeutralAnnouncementDelaySeconds() {
        return this.config.getInt("beacons.neutral-announcement-delay-seconds", 60);
    }

    public int getGarlicProcessingTimeMin() {
        return this.config.getInt("garlic.processing-time-min-seconds", 480);
    }

    public int getGarlicProcessingTimeMax() {
        return this.config.getInt("garlic.processing-time-max-seconds", 720);
    }

    public int getGarlicImmunityDurationMin() {
        return this.config.getInt("garlic.immunity-duration-min-seconds", 480);
    }

    public int getGarlicImmunityDurationMax() {
        return this.config.getInt("garlic.immunity-duration-max-seconds", 600);
    }

    public boolean isPassiveMobAutoSpawnEnabled() {
        return this.config.getBoolean("passive-mob-spawning.auto-spawn-enabled", true);
    }

    public int getPassiveMobMinimumThreshold() {
        return this.config.getInt("passive-mob-spawning.minimum-animal-threshold", 40);
    }

    public int getPassiveMobSpawnCount() {
        return this.config.getInt("passive-mob-spawning.spawn-count", 80);
    }

    public int getPermadeathMinimumStage() {
        return this.config.getInt("combat.permadeath-minimum-stage", 1);
    }

    public int getWoodenStakeCooldownTicks() {
        return this.config.getInt("combat.wooden-stake-cooldown-ticks", 80);
    }

    public boolean isCureBooksEnabled() {
        return this.config.getBoolean("cure_books_enabled", true);
    }

    public double getCureBooksSpawnChance() {
        return this.config.getDouble("cure_books_spawn_chance", 0.3);
    }

    public double getCureBeaconDistance() {
        return this.config.getDouble("cure.cure-distance", (double)25.0F);
    }

    public boolean isFirstMessageBlockingEnabled() {
        return this.config.getBoolean("chat.first-message-blocking-enabled", true);
    }

    public String getFirstMessageBlockedMessage() {
        return this.config.getString("chat.first-message-blocked-message", "&eIt looks like you've attempted to send a message! Vampire SMP is geared to revolve around immersion, consider finding the person you need to speak to, or messaging them on discord. If you still need to send your chat message, [Click Here]&e. This prevention message will not appear again until your next log on if you do choose to send your message via the blue text.");
    }

    public Location getVampireRespawnLocation(World world) {
        String locationStr = this.config.getString("vampire.respawn-location", "40,101,-113");
        String[] parts = locationStr.split(",");

        try {
            double x = Double.parseDouble(parts[0].trim());
            double y = Double.parseDouble(parts[1].trim());
            double z = Double.parseDouble(parts[2].trim());
            return new Location(world, x, y, z);
        } catch (Exception var10) {
            this.plugin.getLogger().warning("Invalid vampire respawn location format: " + locationStr + ". Using default.");
            return new Location(world, (double)40.0F, (double)101.0F, (double)-113.0F);
        }
    }

    public void setVampireRespawnLocation(String locationStr) {
        this.config.set("vampire.respawn-location", locationStr);
        this.plugin.saveConfig();
    }

    public double getOakhurstTownCenterX() {
        return this.config.getDouble("oakhurst.town-center-x", (double)79.0F);
    }

    public double getOakhurstTownCenterZ() {
        return this.config.getDouble("oakhurst.town-center-z", (double)440.0F);
    }

    public double getOakhurstTeleportRadius() {
        return this.config.getDouble("oakhurst.teleport-radius", (double)400.0F);
    }

    public double getOakhurstBorderCenterX() {
        return this.config.getDouble("oakhurst.border.center-x", (double)50.0F);
    }

    public double getOakhurstBorderCenterZ() {
        return this.config.getDouble("oakhurst.border.center-z", (double)50.0F);
    }

    public double getOakhurstBorderDiameter() {
        return this.config.getDouble("oakhurst.border.diameter", (double)1098.0F);
    }

    public double getOakhurstBorderRadius() {
        return this.getOakhurstBorderDiameter() / (double)2.0F;
    }

    public double getOakhurstMinX() {
        return this.getOakhurstBorderCenterX() - this.getOakhurstBorderRadius();
    }

    public double getOakhurstMaxX() {
        return this.getOakhurstBorderCenterX() + this.getOakhurstBorderRadius();
    }

    public double getOakhurstMinZ() {
        return this.getOakhurstBorderCenterZ() - this.getOakhurstBorderRadius();
    }

    public double getOakhurstMaxZ() {
        return this.getOakhurstBorderCenterZ() + this.getOakhurstBorderRadius();
    }

    public boolean isLocationWithinBorder(double x, double z) {
        return x >= this.getOakhurstMinX() && x <= this.getOakhurstMaxX() && z >= this.getOakhurstMinZ() && z <= this.getOakhurstMaxZ();
    }

    public List<String> validateConfiguredLocations(BeaconManager beaconManager) {
        List<String> warnings = new ArrayList();
        double townX = this.getOakhurstTownCenterX();
        double townZ = this.getOakhurstTownCenterZ();
        if (!this.isLocationWithinBorder(townX, townZ)) {
            warnings.add("Town center (" + (int)townX + ", " + (int)townZ + ")");
        }

        World world = this.plugin.getServer().getWorld("world");
        if (world != null) {
            Location vampireSpawn = this.getVampireRespawnLocation(world);
            if (!this.isLocationWithinBorder(vampireSpawn.getX(), vampireSpawn.getZ())) {
                int var10001 = vampireSpawn.getBlockX();
                warnings.add("Vampire respawn (" + var10001 + ", " + vampireSpawn.getBlockZ() + ")");
            }

            for(Location loc : this.getTomeChestLocations()) {
                if (!this.isLocationWithinBorder(loc.getX(), loc.getZ())) {
                    int var15 = loc.getBlockX();
                    warnings.add("Tome chest at (" + var15 + ", " + loc.getBlockZ() + ")");
                }
            }
        }

        if (beaconManager != null) {
            for(BeaconSite beacon : beaconManager.getAllBeacons()) {
                Location loc = beacon.getLocation();
                if (!this.isLocationWithinBorder(loc.getX(), loc.getZ())) {
                    String var16 = beacon.getName();
                    warnings.add("Beacon '" + var16 + "' (" + loc.getBlockX() + ", " + loc.getBlockZ() + ")");
                }
            }
        }

        return warnings;
    }
}
