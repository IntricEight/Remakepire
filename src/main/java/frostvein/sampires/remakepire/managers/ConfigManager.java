package frostvein.sampires.remakepire.managers;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.beacons.BeaconSite;

public class ConfigManager {
    private final RemakepirePlugin plugin;

    /**
     * Create an instance of the Config manager.
     *
     * @param plugin the host plugin object.
     */
    public ConfigManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Save the plugin configuration into the file.
     */
    public void saveConfig() {
        this.plugin.saveConfig();
    }

    /**
     * Retrieve the locations of all the tome chests from the config.
     *
     * @return A {@code List} of chest {@code Locations}.
     */
    public List<Location> getTomeChestLocations() {
        List<String> locationStrings = this.plugin.getConfig().getStringList("tome-chests.locations");
        List<Location> locations = new ArrayList<>();
        World world = this.plugin.getServer().getWorld(RemakepirePlugin.WORLD_NAME);

        if (world == null) {
            this.plugin.getLogger().severe("World '" + RemakepirePlugin.WORLD_NAME + "' not found! Cannot load tome chest locations.");

        } else {
            for(String locString : locationStrings) {
                try {
                    String[] parts = locString.split(",");

                    if (parts.length == 3) {
                        int x = Integer.parseInt(parts[0].trim());
                        int y = Integer.parseInt(parts[1].trim());
                        int z = Integer.parseInt(parts[2].trim());

                        locations.add(new Location(world, x, y, z));
                    }
                } catch (NumberFormatException e) {
                    this.plugin.getLogger().warning("Invalid tome chest location format: " + locString);
                }
            }

        }

        return locations;
    }

    /**
     * Add a new tome chest to the config.
     *
     * @param location the new tome chest's location.
     * @return {@code true} if a new tome chest was added to the config.
     */
    public boolean addTomeChestLocation(Location location) {
        List<String> locations = this.plugin.getConfig().getStringList("tome-chests.locations");
        String locationString = String.format("%d,%d,%d", location.getBlockX(), location.getBlockY(), location.getBlockZ());

        if (locations.contains(locationString)) {
            return false;

        } else {
            locations.add(locationString);
            this.plugin.getConfig().set("tome-chests.locations", locations);
            this.saveConfig();
            return true;
        }
    }

    /**
     * Remove a tome chest from the config.
     *
     * @param location the tome chest's location.
     * @return {@code true} if the tome chest was removed from the config.
     */
    public boolean removeTomeChestLocation(Location location) {
        List<String> locations = this.plugin.getConfig().getStringList("tome-chests.locations");
        String locationString = String.format("%d,%d,%d", location.getBlockX(), location.getBlockY(), location.getBlockZ());

        if (!locations.contains(locationString)) {
            return false;

        } else {
            locations.remove(locationString);
            this.plugin.getConfig().set("tome-chests.locations", locations);
            this.saveConfig();
            return true;
        }
    }

    /**
     * Retrieve the cooldown on the vampire ability Bat Transformation.
     *
     * @return the seconds between ability uses.
     */
    public int getVampireBatCooldown() {
        return this.plugin.getConfig().getInt("abilities.vampire.bat-cooldown", 900);
    }

    /**
     * Retrieve the cooldown on the vampire ability Vampiric Lunge.
     *
     * @return the seconds between ability uses.
     */
    public int getVampireLungeCooldown() {
        return this.plugin.getConfig().getInt("abilities.vampire.lunge-cooldown", 45);
    }

    /**
     * Retrieve the cooldown on the vampire ability Vampiric Vanish.
     *
     * @return the seconds between ability uses.
     */
    public int getVampireVanishCooldown() {
        return this.plugin.getConfig().getInt("abilities.vampire.vanish-cooldown", 420);
    }

    /**
     * Retrieve the cooldown on the vampire ability Raise Undead.
     *
     * @return the seconds between ability uses.
     */
    public int getRaiseUndeadCooldown() {
        return this.plugin.getConfig().getInt("abilities.vampire.raise-undead-cooldown", 600);
    }

    /**
     * Retrieve the cooldown on the vampire ability Storm Call.
     *
     * @return the seconds between ability uses.
     */
    public int getVampireStormCallCooldown() {
        return this.plugin.getConfig().getInt("abilities.vampire.storm-call-cooldown", 7200);
    }

    /**
     * Retrieve the cooldown on the vampire ability Beacon Teleport.
     *
     * @return the seconds between ability uses.
     */
    public int getVampireBeaconTeleportCooldown() {
        return this.plugin.getConfig().getInt("abilities.vampire.beacon-teleport-cooldown", 300);
    }

    /**
     * Retrieve the cooldown on the vampire ability Vampire Vision.
     *
     * @return the seconds between ability uses.
     */
    public int getVampireVisionCooldown() {
        return this.plugin.getConfig().getInt("abilities.vampire.vampire-vision-cooldown", 1);
    }

    /**
     * Retrieve the cooldown on the tome ability Blessing.
     *
     * @return the seconds between ability uses.
     */
    public int getTomeBlessingCooldown() {
        return this.plugin.getConfig().getInt("abilities.tome.blessing-cooldown", 7200);
    }

    /**
     * Retrieve the cooldown on the tome ability Banish Undead.
     *
     * @return the seconds between ability uses.
     */
    public int getTomeBanishUndeadCooldown() {
        return this.plugin.getConfig().getInt("abilities.tome.banish-undead-cooldown", 900);
    }

    /**
     * Retrieve the cooldown on the tome ability Holy Word.
     *
     * @return the seconds between ability uses.
     */
    public int getTomeHolyWordCooldown() {
        return this.plugin.getConfig().getInt("abilities.tome.holy-word-cooldown", 600);
    }

    /**
     * Retrieve the cooldown on the tome ability Enlightened Eye.
     *
     * @return the seconds between ability uses.
     */
    public int getTomeEnlightenedEyeCooldown() {
        return this.plugin.getConfig().getInt("abilities.tome.enlightened-eye-cooldown", 900);
    }

    /**
     * Retrieve the cooldown on the tome ability Lantern Thrash.
     *
     * @return the seconds between ability uses.
     */
    public int getTomeLanternThrashCooldown() {
        return this.plugin.getConfig().getInt("abilities.tome.lantern-thrash-cooldown", 300);
    }

    /**
     * Retrieve the cooldown on the tome ability Prayer of Faith.
     *
     * @return the seconds between ability uses.
     */
    public int getTomePrayerOfFaithCooldown() {
        return this.plugin.getConfig().getInt("abilities.tome.prayer-of-faith-cooldown", 900);
    }

    /**
     * Retrieve the cooldown on the tome ability Rallying Cry.
     *
     * @return the seconds between ability uses.
     */
    public int getTomeRallyingCryCooldown() {
        return this.plugin.getConfig().getInt("abilities.tome.rallying-cry-cooldown", 1200);
    }

    /**
     * Retrieve the cooldown on the tome ability Shoulder Barge.
     *
     * @return the seconds between ability uses.
     */
    public int getTomeShoulderBargeCooldown() {
        return this.plugin.getConfig().getInt("abilities.tome.shoulder-barge-cooldown", 300);
    }

    /**
     * Retrieve the cooldown on the tome ability Turn Undead.
     *
     * @return the seconds between ability uses.
     */
    public int getTomeTurnUndeadCooldown() {
        return this.plugin.getConfig().getInt("abilities.tome.turn-undead-cooldown", 1800);
    }


    /**
     * Retrieve the cooldown on the tome ability Uncanny Direction.
     *
     * @return the seconds between ability uses.
     */
    public int getTomeUncannyDirectionCooldown() {
        return this.plugin.getConfig().getInt("abilities.tome.uncanny-direction-cooldown", 30);
    }


    /**
     * Retrieve the cooldown on the tome ability Unnatural Haste.
     *
     * @return the seconds between ability uses.
     */
    public int getTomeUnnaturalHasteCooldown() {
        return this.plugin.getConfig().getInt("abilities.tome.unnatural-haste-cooldown", 900);
    }

    /**
     * Retrieve the cooldown on the tome ability Way of the Land.
     *
     * @return the seconds between ability uses.
     */
    public int getTomeWayOfTheLandCooldown() {
        return this.plugin.getConfig().getInt("abilities.tome.way-of-the-land-cooldown", 600);
    }

    /**
     * Retrieve the cooldown on the tome ability Way of the Lumberjack.
     *
     * @return the seconds between ability uses.
     */
    public int getTomeWayOfTheLumberjackCooldown() {
        return this.plugin.getConfig().getInt("abilities.tome.way-of-the-lumberjack-cooldown", 600);
    }

    /**
     * Retrieve the cooldown on the tome ability Way of the Prospector.
     *
     * @return the seconds between ability uses.
     */
    public int getTomeWayOfTheProspectorCooldown() {
        return this.plugin.getConfig().getInt("abilities.tome.way-of-the-prospector-cooldown", 600);
    }

    /**
     * Retrieve the cooldown on the tome ability Stop the Bleeding.
     *
     * @return the seconds between ability uses.
     */
    public int getTomeStopTheBleedingCooldown() {
        return this.plugin.getConfig().getInt("abilities.tome.stop-the-bleeding-cooldown", 7200);
    }

    /**
     * Retrieve how many tome chests should contain tome ability books.
     *
     * @return the number of tome chests.
     */
    public int getAbilityDistributionCount() {
        return this.plugin.getConfig().getInt("tome-chests.ability-book-count", 4);
    }

    /**
     * Retrieve the tome abilities whose books are allowed to appear inside tome chests.
     *
     * @return A list of tome ability names.
     */
    public List<String> getTomeAbilityOptions() {
        return this.plugin.getConfig().getStringList("tome-chests.tome-options");
    }

    /**
     * Retrieve the enchantments whose books are allowed to appear inside tome chests.
     *
     * @return A list of item enchantment names.
     */
    public List<String> getTomeEnchantmentOptions() {
        return this.plugin.getConfig().getStringList("tome-chests.enchantment-options");
    }

    /**
     * Retrieve the ticks between tome book distribution cycles.
     *
     * @return The number of ticks after which tome chests will be refilled.
     */
    public long getTomeDistributionIntervalTicks() {
        int minutes = this.plugin.getConfig().getInt("tome-distribution-interval-minutes", 20);
        return (long)minutes * 60L * 20L;
    }

    /**
     * Retrieve whether humans are limited to only a single tome each session.
     *
     * @return {@code true} if humans can only absorb a single tome ability during a session.
     */
    public boolean isTomeAbsorptionCapped() {
        return this.plugin.getConfig().getBoolean("tome-absorption.tome-absorption-capping", true);
    }

    /**
     * Update the config on whether tome ability absorptions should be capped per session.
     *
     * @param capped {@code true} if tome absorption should be capped to one each session.
     */
    public void setTomeAbsorptionCapping(boolean capped) {
        this.plugin.getConfig().set("tome-absorption.tome-absorption-capping", capped);
        this.plugin.saveConfig();
    }

    /**
     * Retrieve the cooldown time between when a human can gain new tome abilities.
     *
     * @return the minutes between tome absorptions.
     */
    public int getTomeAbsorptionIntervalMinutes() {
        return this.plugin.getConfig().getInt("tome-absorption.tome-absorption-interval-minutes", 120);
    }

    /**
     * Retrieve the time it takes for a vampire's thirst bar to fully deplete.
     *
     * @return the minutes it takes to run out of blood.
     */
    public int getThirstDepletionMinutes() {
        return this.plugin.getConfig().getInt("thirst.depletion-minutes", 120);
    }

    /**
     * Retrieve the maximum amount of blood a vampire can drain through drinking.
     *
     * @return the points of blood that each vampire can gain.
     */
    public int getMaxFeedingThirstPerSession() {
        return this.plugin.getConfig().getInt("thirst.max-feeding-per-session", 60);
    }

    /**
     * Retrieve whether vampires will be prevented from leveling up after dropping down during the session.
     *
     * @return {@code true} if the vampire is prevented from leveling.
     */
    public boolean isVampireLevelingCapped() {
        return this.plugin.getConfig().getBoolean("vampire.vampire-level-capping", true);
    }

    /**
     * Update the config on whether vampires can return to lost levels during a session.
     *
     * @param capped {@code true} if vampire levels should be restricted upon dropping a level.
     */
    public void setVampireLevelCapping(boolean capped) {
        this.plugin.getConfig().set("vampire.vampire-level-capping", capped);
        this.plugin.saveConfig();
    }

    /**
     * Retrieve if vampires are given a direction and distance indicator toward new vampires.
     *
     * @return {@code true} if vampires are pointed toward new vampires.
     */
    public boolean canTrackNewVampires() {
        return this.plugin.getConfig().getBoolean("vampire.new-vampire-tracking", true);
    }

    /**
     * Update the config on whether vampires are given a direction and distance indicator toward new vampires.
     *
     * @param track {@code true} if vampires are given directions to the new vampire.
     */
    public void setTrackingNewVampires(boolean track) {
        this.plugin.getConfig().set("vampire.new-vampire-tracking", track);
        this.plugin.saveConfig();
    }

    /**
     * Retrieve the baseline complete beacon conversion time for a single player.
     *
     * @return the milliseconds it takes to convert a beacon from one side's alignment into the others.
     */
    public long getBeaconConversionTimeMs() {
        long seconds = this.plugin.getConfig().getLong("beacons.conversion-time-seconds", 300L);
        return seconds * 1000L;
    }

    /**
     * Retrieve the cooldown between beacon conversions.
     *
     * @return the milliseconds it takes to convert a beacon from one side's alignment into the others.
     */
    public long getBeaconConversionCooldownMs() {
        long minutes = this.plugin.getConfig().getLong("beacons.conversion-cooldown-minutes", 60L);
        return minutes * 60L * 1000L;
    }

    /**
     * Retrieve the conversion speed multiplier that humans get.
     *
     * @return A {@code double} multiplier for the conversion speed.
     */
    public double getBeaconHumanSpeedMultiplier() {
        return this.plugin.getConfig().getDouble("beacons.human-speed-multiplier", 1.5);
    }

    /**
     * Retrieve the conversion speed multiplier that humans get during a darkness final stand.
     *
     * @return A {@code double} multiplier for the conversion speed.
     */
    public double getBeaconFinalStandMultiplier() {
        return this.plugin.getConfig().getDouble("beacons.final-stand-multiplier", 6.0);
    }

    /**
     * Retrieve the seconds after a beacon comes neutral until a server-wide alert will be given.
     *
     * @return The seconds between the conversion and message.
     */
    public int getBeaconNeutralAnnouncementDelaySeconds() {
        return this.plugin.getConfig().getInt("beacons.neutral-announcement-delay-seconds", 60);
    }

    /**
     * Retrieve whether humans are allowed to achieve the victory condition if a vampire has been cured.
     *
     * @return {@code true} if curing a vampire and corrupting a beacon traps the humans inside the border.
     */
    public boolean doCorruptedBeaconsTrapHumans() {
        return this.plugin.getConfig().getBoolean("beacons.corrupted-beacons-trap-humans", true);
    }

    /**
     * Retrieve the minimum number of seconds where garlic can activate after consumption.
     *
     * @return The seconds until the garlic effect could activate.
     */
    public int getGarlicProcessingTimeMin() {
        return this.plugin.getConfig().getInt("garlic.processing-time-min-seconds", 480);
    }

    /**
     * Retrieve the maximum number of seconds where garlic can activate after consumption.
     *
     * @return The seconds until the garlic effect has to activate.
     */
    public int getGarlicProcessingTimeMax() {
        return this.plugin.getConfig().getInt("garlic.processing-time-max-seconds", 720);
    }

    /**
     * Retrieve the minimum number of seconds that garlic will stay in effect.
     *
     * @return The seconds until the garlic effect could wear off.
     */
    public int getGarlicImmunityDurationMin() {
        return this.plugin.getConfig().getInt("garlic.immunity-duration-min-seconds", 480);
    }

    /**
     * Retrieve the maximum number of seconds that garlic will stay in effect.
     *
     * @return The seconds until the garlic effect has to wear off.
     */
    public int getGarlicImmunityDurationMax() {
        return this.plugin.getConfig().getInt("garlic.immunity-duration-max-seconds", 600);
    }

    /**
     * Retrieve the minimum number of seconds until garlic can be used again.
     *
     * @return The seconds until the garlic effect could wear off.
     */
    public int getGarlicRecoveryDurationMin() {
        return this.plugin.getConfig().getInt("garlic.recovery-duration-min-seconds", 2100);
    }

    /**
     * Retrieve the maximum number of seconds until garlic can be used again.
     *
     * @return The seconds until the garlic effect has to wear off.
     */
    public int getGarlicRecoveryDurationMax() {
        return this.plugin.getConfig().getInt("garlic.recovery-duration-max-seconds", 2700);
    }

    /**
     * Retrieve the number of seconds that vampires get weakness for after killing someone with garlic.
     *
     * @return The seconds until vampires regain their strength.
     */
    public int getGarlicWeaknessDuration() {
        return this.plugin.getConfig().getInt("garlic.weakness-duration", 180);
    }

    /**
     * Retrieve whether the console logging should be reduced to only essential messages.
     *
     * @return {@code true} if logging should be reduced.
     */
    public boolean isNonEssentialLoggingDisabled() {
        return this.plugin.getConfig().getBoolean("disable-nonessential-logging", false);
    }

    /**
     * Retrieve the duration that holy water disables vampire abilities.
     *
     * @return The number of seconds that holy water disables a vampire's abilities.
     */
    public int getHolyWaterDisableDurationSeconds() {
        return this.plugin.getConfig().getInt("holy-water.disable-duration-seconds", 120);
    }

    /**
     * Retrieve whether players will be limited to creating one bottle of holy water per session.
     *
     * @return {@code true} if holy water has been session capped.
     */
    public boolean isHolyWaterSessionCapped() {
        return this.plugin.getConfig().getBoolean("holy-water.holy-water-session-capped", true);
    }

    /**
     * Update the config on whether multiple bottles of holy water can be created in a single session
     *
     * @param capped {@code true} if only a single holy water can be made by each player each session.
     */
    public void setHolyWaterCapping(boolean capped) {
        this.plugin.getConfig().set("holy-water.holy-water-session-capped", capped);
        this.plugin.saveConfig();
    }

    /**
     * Retrieve if passive mobs will be automatically spawned each morning.
     *
     * @return {@code true} if passive mobs will be spawned without admin intervention.
     */
    public boolean isPassiveMobAutoSpawnEnabled() {
        return this.plugin.getConfig().getBoolean("passive-mob-spawning.auto-spawn-enabled", true);
    }

    /**
     * Retrieve the minimum number of passive mobs that will be spawned with each morning.
     *
     * @return The lowest number of passive mobs that will spawn.
     */
    public int getPassiveMobMinimumThreshold() {
        return this.plugin.getConfig().getInt("passive-mob-spawning.minimum-animal-threshold", 40);
    }

    /**
     * Retrieve the number of passive mobs spawns that will be attempted each morning.
     *
     * @return The highest number of passive mobs that will spawn.
     */
    public int getPassiveMobSpawnCount() {
        return this.plugin.getConfig().getInt("passive-mob-spawning.spawn-count", 80);
    }

    /**
     * Retrieve if NPC mobs (such as pillagers and wandering traders) can spawn naturally.
     *
     * @return {@code true} if NPC mobs can spawn.
     */
    public boolean areNpcMobsEnabled() {
        return this.plugin.getConfig().getBoolean("enable-npc-mobs", false);
    }

    /**
     * Retrieve if players are allowed to breed animals when the game session is not active.
     *
     * @return {@code true} if animals can be bred out of session.
     */
    public boolean canBreedAnimalsOutOfSession() {
        return this.plugin.getConfig().getBoolean("allow-breeding-out-of-session", true);
    }

    /**
     * Update the config on whether animals can be bred and chickens can be hatched out of session.
     *
     * @param canBreed {@code true} if animals can be bred out of session.
     */
    public void setBreedAnimalsOutOfSession(boolean canBreed) {
        this.plugin.getConfig().set("allow-breeding-out-of-session", canBreed);
        this.plugin.saveConfig();
    }

    /**
     * Retrieve the highest stage that a vampire can be on and get permakilled.
     *
     * @return The highest permadeath-enabled vampire stage.
     */
    public int getPermadeathMinimumStage() {
        return this.plugin.getConfig().getInt("combat.permadeath-minimum-stage", 1);
    }

    /**
     * Update the config on when vampires can be permakilled.
     *
     * @param stage the highest stage that vampires can be permakilled at.
     */
    public void setStakePermadeathMinimumStage(int stage) {
        this.plugin.getConfig().set("combat.permadeath-minimum-stage", Math.max(1, stage));
        this.plugin.saveConfig();
    }

    /**
     * Retrieve the number of lives that humans start out with.
     *
     * @return The total number of times humans can die and respawn.
     */
    public int getHumanLifeCount() {
        int lives = this.plugin.getConfig().getInt("combat.human-lives", 5);

        // Make sure the life count is within the specified range
        if (lives > 8) {
            lives = 8;
        } else if (lives < 0) {
            lives = 0;
        }

        return lives;
    }

    /**
     * Retrieve if humans will die once their lives run out, or be kept alive until a vampire gets the final kill.
     *
     * @return {@code true} is humans will permanently die on their sixth death, regardless of its cause.
     */
    public boolean isLifeLimitEnforced() {
        return this.plugin.getConfig().getBoolean("combat.enforce-life-limit", false);
    }

    /**
     * Update the config on whether humans require a vampire to kill them to permanently die.
     *
     * @param capped {@code true} if humans will be permakilled on their sixth death, regardless of the cause.
     */
    public void setLifeLimitEnforced(boolean capped) {
        this.plugin.getConfig().set("combat.enforce-life-limit", capped);
        this.plugin.saveConfig();
    }

    /**
     * Retrieve if vampires are weakened and repulsed by silver and adjacent blocks.
     *
     * @return {@code true} if silver-based blocks affect vampires.
     */
    public boolean doSilverBlocksWeakenVampires() {
        return this.plugin.getConfig().getBoolean("vampire.silver-weakness", true);
    }

    /**
     * Retrieve if vampires can mount living entities. Exceptions are made for undead mounts.
     *
     * @return {@code true} if vampires can mount any animal.
     */
    public boolean canVampiresRideLivingMounts() {
        return this.plugin.getConfig().getBoolean("vampire.allow-vampire-mounts", true);
    }

    /**
     * Update the config on whether vampires can ride on living mounts
     *
     * @param canRide {@code true} if vampires can ride living mounts.
     */
    public void setVampiresRideLivingMounts(boolean canRide) {
        this.plugin.getConfig().set("vampire.allow-vampire-mounts", canRide);
        this.plugin.saveConfig();
    }

    /**
     * Retrieve the speed at which vampires regenerate health.
     *
     * @return The number of ticks it takes for each health point regeneration.
     */
    public int getVampireHealthCheckTicks() {
        return this.plugin.getConfig().getInt("vampire_health_check_ticks", 9);
    }

    /**
     * Update the config on how quickly vampires regenerate health.
     *
     * @param ticks the number of ticks between each health point recovery.
     */
    public void setVampireHealthCheckTicks(int ticks) {
        this.plugin.getConfig().set("vampire_health_check_ticks", Math.max(1, ticks));
        this.plugin.saveConfig();
    }

    /**
     * Retrieve the ticks cooldown between wooden stake uses. 20 ticks is 1 second.
     *
     * @return The cooldown between wooden stake uses (in ticks).
     */
    public int getWoodenStakeCooldownTicks() {
        return this.plugin.getConfig().getInt("combat.wooden-stake-cooldown-ticks", 80);
    }

    /**
     * Retrieve the percentage of received damage that players will ignore.
     *
     * @return The percent of damage that will be ignored by players.
     */
    public int getDamageSuppression() {
        return this.plugin.getConfig().getInt("damage_suppression", 50);
    }

    /**
     * Update the config on what percent of damage should be ignored by players.
     *
     * @param percentage the percentage to ignore.
     */
    public void setDamageSuppression(int percentage) {
        this.plugin.getConfig().set("damage_suppression", percentage);
        this.plugin.saveConfig();
    }

    /**
     * Retrieve the chance that a tome chest's contents will be replaced by a cure book.
     *
     * @return The percentage chance of a cure book appearing in tome chests each cycle.
     */
    public double getCureBooksSpawnChance() {
        return this.plugin.getConfig().getDouble("cure_books_spawn_chance", 0.3);
    }

    /**
     * Retrieve how far a player can be from a beacon while converting it.
     *
     * @return The maximum distance (in blocks) that a player can be from a beacon and convert it.
     */
    public double getBeaconConversionDistance() {
        return this.plugin.getConfig().getDouble("beacons.distance.conversion-distance", 3.0);
    }

    /**
     * Retrieve how far a vampire must be from a beacon before they can use their powers.
     *
     * @return The maximum distance (in blocks) that a vampire can be from a beacon and have their abilities suppressed.
     */
    public double getBeaconSuppressionDistance() {
        return this.plugin.getConfig().getDouble("beacons.distance.vampire-suppression-distance", 25.0);
    }

    /**
     * Retrieve how far a vampire can be from a beacon until a cure stops working.
     *
     * @return The maximum distance (in blocks) that a vampire can be from a beacon while being cured.
     */
    public double getCureBeaconDistance() {
        return this.plugin.getConfig().getDouble("cure.cure-distance", 25.0);
    }

    /**
     * Retrieve whether a vampire's sire must be dead before the vampire can be cured.
     *
     * @return {@code true} if the sire must be permakilled for the cure to work.
     */
    public boolean doCuresRequireSireDeath() {
        return this.plugin.getConfig().getBoolean("cure.sire-death-requirement", true);
    }

    /**
     * Update the config on whether vampires can be cured while their sire lives.
     *
     * @param requireDeath {@code true} if the sire must be dead before curing.
     */
    public void setCureRequiresSireDeath(boolean requireDeath) {
        this.plugin.getConfig().set("cure.sire-death-requirement", requireDeath);
        this.plugin.saveConfig();
    }

    /**
     * Retrieve whether messages that players send will be blocked until they confirm otherwise.
     *
     * @return {@code true} if the message will be blocked.
     */
    public boolean isFirstMessageBlockingEnabled() {
        return this.plugin.getConfig().getBoolean("chat.first-message-blocking-enabled", true);
    }

    /**
     * Retrieve the message and prompt that appears when a user sends a blocked message.
     *
     * @return The message to stop the player from messaging others.
     */
    public String getFirstMessageBlockedMessage() {
        return this.plugin.getConfig().getString("chat.first-message-blocked-message", "&eIt looks like you've attempted to send a message! Vampire SMP is geared to revolve around immersion, consider finding the person you need to speak to, or messaging them on discord. If you still need to send your chat message, [Click Here]&e. This prevention message will not appear again until your next login if you do choose to send your message via the blue text.");
    }

    /**
     * Retrieve if a message should be sent to all Operators when a player leaves the game.
     *
     * @return {@code true} if an alert message should be sent.
     */
    public boolean shouldAlertOnPlayerQuit() {
        return this.plugin.getConfig().getBoolean("chat.alert-on-player-leave", true);
    }

    /**
     * Update the config on whether Operators should be alerted when a player quits the game.
     *
     * @param shouldAlert {@code true} if Operators should be messaged.
     */
    public void setAlertOnPlayerQuit(boolean shouldAlert) {
        this.plugin.getConfig().set("chat.alert-on-player-leave", shouldAlert);
        this.plugin.saveConfig();
    }

    /**
     * Retrieve the default location where vampires will respawn after dying.
     *
     * @param world the world hosting the plugin interactions.
     * @return A {@code Location} where vampires will respawn.
     */
    public Location getVampireRespawnLocation(World world) {
        String locationStr = this.plugin.getConfig().getString("vampire.respawn-location", "40,101,-113");
        String[] parts = locationStr.split(",");

        try {
            double x = Double.parseDouble(parts[0].trim());
            double y = Double.parseDouble(parts[1].trim());
            double z = Double.parseDouble(parts[2].trim());
            return new Location(world, x, y, z);

        } catch (Exception e) {
            this.plugin.getLogger().warning("Invalid vampire respawn location format: " + locationStr + ". Using default.");
            return new Location(world, 40.0, 101.0, -113.0);
        }
    }

    /**
     * Set a new respawn location for the vampires.
     *
     * @param locationStr The coordinates of a location.
     */
    public void setVampireRespawnLocation(String locationStr) {
        this.plugin.getConfig().set("vampire.respawn-location", locationStr);
        this.plugin.saveConfig();
    }

    /**
     * Retrieve the name of the game's primary town or location.
     *
     * @return The name of the town.
     */
    public String getTownName() {
        return this.plugin.getConfig().getString("oakhurst.town-name", "Oakhurst");
    }

    /**
     * Retrieve the X coordinate of the main settlement's location.
     *
     * @return The X coordinate of town's center.
     */
    public double getTownCenterX() {
        return this.plugin.getConfig().getDouble("oakhurst.town-center-x", 79.0);
    }

    /**
     * Retrieve the Z coordinate of the main settlement's location.
     *
     * @return The Z coordinate of town's center.
     */
    public double getTownCenterZ() {
        return this.plugin.getConfig().getDouble("oakhurst.town-center-z", 440.0);
    }

    /**
     * Retrieve the maximum distance from the main settlement where players can spawn on game init.
     *
     * @return The distance around town where players can spawn.
     */
    public double getTeleportRadius() {
        return this.plugin.getConfig().getDouble("oakhurst.teleport-radius", 400.0);
    }

    /**
     * Retrieve the X coordinate of the game border's center.
     *
     * @return The X coordinate of the border's center.
     */
    public double getBorderCenterX() {
        return this.plugin.getConfig().getDouble("oakhurst.border.center-x", 50.0);
    }

    /**
     * Retrieve the Z coordinate of the game border's center.
     *
     * @return The Z coordinate of the border's center.
     */
    public double getBorderCenterZ() {
        return this.plugin.getConfig().getDouble("oakhurst.border.center-z", 50.0);
    }

    /**
     * Retrieve the length of the game border.
     *
     * @return The diameter of the game area (in blocks).
     */
    public double getBorderDiameter() {
        return this.plugin.getConfig().getDouble("oakhurst.border.diameter", 1098.0);
    }

    /**
     * Retrieve the radius of the game border.
     *
     * @return The radius of the game area (in blocks).
     */
    public double getBorderRadius() {
        return this.getBorderDiameter() / 2.0;
    }

    /**
     * Determine the smallest X coordinate that is within the game border.
     *
     * @return The X coordinate of the western border.
     */
    public double getBorderMinX() {
        return this.getBorderCenterX() - this.getBorderRadius();
    }

    /**
     * Determine the largest X coordinate that is within the game border.
     *
     * @return The X coordinate of the eastern border.
     */
    public double getBorderMaxX() {
        return this.getBorderCenterX() + this.getBorderRadius();
    }

    /**
     * Determine the smallest Z coordinate that is within the game border.
     *
     * @return The Z coordinate of the southern border.
     */
    public double getBorderMinZ() {
        return this.getBorderCenterZ() - this.getBorderRadius();
    }

    /**
     * Determine the largest Z coordinate that is within the game border.
     *
     * @return The Z coordinate of the northern border.
     */
    public double getBorderMaxZ() {
        return this.getBorderCenterZ() + this.getBorderRadius();
    }

    /**
     * Determine if a location is within the game border.
     *
     * @param x the X coordinate of the location.
     * @param z the Z coordinate of the location.
     * @return {@code true} if the location is inside the game border region.
     */
    public boolean isLocationWithinBorder(double x, double z) {
        return x >= this.getBorderMinX() && x <= this.getBorderMaxX() && z >= this.getBorderMinZ() && z <= this.getBorderMaxZ();
    }

    public List<String> validateConfiguredLocations(BeaconManager beaconManager) {
        List<String> warnings = new ArrayList<>();
        double townX = this.getTownCenterX(), townZ = this.getTownCenterZ();

        if (!this.isLocationWithinBorder(townX, townZ)) {
            warnings.add("Town center (" + (int)townX + ", " + (int)townZ + ")");
        }

        World world = this.plugin.getServer().getWorld(RemakepirePlugin.WORLD_NAME);

        if (world != null) {
            Location vampireSpawn = this.getVampireRespawnLocation(world);
            if (!this.isLocationWithinBorder(vampireSpawn.getX(), vampireSpawn.getZ())) {
                warnings.add("Vampire respawn (" + vampireSpawn.getBlockX() + ", " + vampireSpawn.getBlockZ() + ")");
            }

            for(Location loc : this.getTomeChestLocations()) {
                if (!this.isLocationWithinBorder(loc.getX(), loc.getZ())) {
                    warnings.add("Tome chest at (" + loc.getBlockX() + ", " + loc.getBlockZ() + ")");
                }
            }
        }

        if (beaconManager != null) {
            for(BeaconSite beacon : beaconManager.getAllBeacons()) {
                Location loc = beacon.getLocation();
                if (!this.isLocationWithinBorder(loc.getX(), loc.getZ())) {
                    warnings.add("Beacon '" + beacon.getName() + "' (" + loc.getBlockX() + ", " + loc.getBlockZ() + ")");
                }
            }
        }

        return warnings;
    }
}
