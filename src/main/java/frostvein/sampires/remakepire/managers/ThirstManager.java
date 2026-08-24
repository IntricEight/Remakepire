package frostvein.sampires.remakepire.managers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class ThirstManager {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final float THIRST_PER_SECOND;
    // The number of minutes that vampires will not lose blood naturally during after a stage change
    private final int IMMUNITY_DURATION_MINUTES = 15;
    private File immunityFile;
    private Map<UUID, Integer> immunityTimers = new HashMap<>();
    private BukkitTask thirstTask;
    private int minuteCounter = 60;
    private final Set<EntityType> thirstQuenchers;
    public static final String THIRST_IMMUNITY_TAG = "ImmuneToThirst";

    /**
     * Create an instance of the Armor Storage manager.
     *
     * @param plugin the host plugin object.
     */
    public ThirstManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.thirstQuenchers = this.initializeThirstQuenchers();
        this.THIRST_PER_SECOND = 1.0F / (float)plugin.getConfigManager().getThirstDepletionMinutes() / 60.0F;
        this.setupImmunitySystem();
        this.startThirstTask();
    }

    /**
     * Retrieve the list of entities that can fill vampire's blood bars.
     *
     * @return A {@code Set} of entities for vampire to feed on.
     */
    private Set<EntityType> initializeThirstQuenchers() {
        Set<EntityType> quenchers = new HashSet<>();

        quenchers.add(EntityType.ARMADILLO);
        quenchers.add(EntityType.CAMEL);
        quenchers.add(EntityType.CHICKEN);
        quenchers.add(EntityType.CAT);
        quenchers.add(EntityType.COW);
        quenchers.add(EntityType.DONKEY);
        quenchers.add(EntityType.FOX);
        quenchers.add(EntityType.GOAT);
        quenchers.add(EntityType.HORSE);
        quenchers.add(EntityType.LLAMA);
        quenchers.add(EntityType.MULE);
        quenchers.add(EntityType.OCELOT);
        quenchers.add(EntityType.PANDA);
        quenchers.add(EntityType.PARROT);
        quenchers.add(EntityType.PIG);
        quenchers.add(EntityType.POLAR_BEAR);
        quenchers.add(EntityType.RABBIT);
        quenchers.add(EntityType.SHEEP);
        quenchers.add(EntityType.SNIFFER);
        quenchers.add(EntityType.TURTLE);
        quenchers.add(EntityType.WOLF);
        quenchers.add(EntityType.EVOKER);
        quenchers.add(EntityType.HOGLIN);
        quenchers.add(EntityType.ILLUSIONER);
        quenchers.add(EntityType.PIGLIN_BRUTE);
        quenchers.add(EntityType.PIGLIN);
        quenchers.add(EntityType.PILLAGER);
        quenchers.add(EntityType.RAVAGER);
        quenchers.add(EntityType.VILLAGER);
        quenchers.add(EntityType.VINDICATOR);
        quenchers.add(EntityType.WANDERING_TRADER);
        quenchers.add(EntityType.WITCH);
        quenchers.add(EntityType.STRIDER);

        return quenchers;
    }

    /**
     * Create the file to store vampiric thirst immunity timers in.
     */
    private void setupImmunitySystem() {
        if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
        }

        this.immunityFile = new File(this.plugin.getDataFolder(), "thirst_immunity.txt");

        if (!this.immunityFile.exists()) {
            try {
                this.immunityFile.createNewFile();
                this.plugin.logInfo("Created thirst immunity persistence file");

            } catch (IOException e) {
                this.plugin.getLogger().severe("Failed to create thirst immunity file: " + e.getMessage());
                e.printStackTrace();
            }
        }

        this.loadImmunityData();
    }

    /**
     * Load the data on vampire thirst immunity timers from the file.
     */
    private void loadImmunityData() {
        String line;

        try (BufferedReader reader = new BufferedReader(new FileReader(this.immunityFile))) {
            while((line = reader.readLine()) != null) {
                String[] parts = line.split(":");

                if (parts.length == 2) {
                    UUID uuid = UUID.fromString(parts[0]);
                    int minutes = Integer.parseInt(parts[1]);
                    this.immunityTimers.put(uuid, minutes);
                }
            }
        } catch (IOException e) {
            this.plugin.getLogger().warning("Could not load thirst immunity data: " + e.getMessage());
        }
    }

    /**
     * Save the current vampire thirst immunity timers into the file.
     */
    private void saveImmunityData() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.immunityFile))) {
            for (Map.Entry<UUID, Integer> entry : this.immunityTimers.entrySet()) {
                writer.write((entry.getKey()).toString() + ":" + entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            this.plugin.getLogger().warning("Could not save thirst immunity data: " + e.getMessage());
        }
    }

    /**
     * Begin processing the passive blood drain of online vampires.
     */
    private void startThirstTask() {
        this.thirstTask = (new BukkitRunnable() {
            public void run() {
                if (ThirstManager.this.plugin.getSessionManager().isSessionActive()) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (ThirstManager.this.vampireManager.isVampire(player)) {
                            ThirstManager.this.processVampireThirst(player);
                        }
                    }

                    --ThirstManager.this.minuteCounter;

                    if (ThirstManager.this.minuteCounter <= 0) {
                        ThirstManager.this.minuteCounter = 60;
                        ThirstManager.this.updateImmunityTimers();
                    }
                }
            }
        }).runTaskTimer(this.plugin, 20L, 20L);
    }

    /**
     * Lower the vampire's blood bar and handle any consequences of the blood loss.
     *
     * @param vampire the vampire losing blood.
     */
    private void processVampireThirst(Player vampire) {
        if (!this.hasThirstImmunity(vampire)) {
            float currentThirst = vampire.getExp();
            float newThirst = currentThirst - this.THIRST_PER_SECOND;

            if (newThirst <= 0.0F) {
                vampire.setExp(0.0F);
                this.handleThirstStarvation(vampire);
            } else {
                vampire.setExp(newThirst);
            }
        }
    }

    /**
     * Decrease the vampire's stage, if they aren't already stage 1.
     *
     * @param vampire the vampire who is starving.
     */
    private void handleThirstStarvation(Player vampire) {
        int currentStage = this.vampireManager.getVampireStage(vampire);

        if (currentStage > 1) {
            this.demoteVampire(vampire, true);
        }
    }

    /**
     * Grant the vampire blood from a kill.
     *
     * @param vampire the player gaining blood.
     * @param entityType the type of entity the vampire killed.
     * @param experienceDropped the experience points that the entity dropped when killed.
     */
    public void handleEntityKill(Player vampire, EntityType entityType, int experienceDropped) {
        if (this.thirstQuenchers.contains(entityType)) {
            experienceDropped = Math.max(experienceDropped * 2 + 3, 1);

            if (entityType == EntityType.WANDERING_TRADER || entityType == EntityType.PILLAGER || entityType == EntityType.VILLAGER) {
                experienceDropped += 10;
            }

            this.quenchThirst(vampire, experienceDropped);
        }
    }

    /**
     * Increase the vampire's blood amount, and promote them if enough blood is consumed.
     *
     * @param vampire the player gaining blood.
     * @param experienceDropped the experience points collected.
     */
    public void quenchThirst(Player vampire, int experienceDropped) {
        this.quenchThirst(vampire, experienceDropped, false);
    }

    /**
     * Increase the vampire's blood amount, and promote them if enough blood is consumed.
     *
     * @param vampire the player gaining blood.
     * @param experienceDropped the experience points collected.
     * @param fromPlayerKill {@code true} if the experience has come from killing another player.
     */
    public void quenchThirst(Player vampire, int experienceDropped, boolean fromPlayerKill) {
        float thirstGained = (float)experienceDropped * 0.01F;
        float currentThirst = vampire.getExp();
        float newThirst = currentThirst + thirstGained;
        float maxThirst = this.getMaxThirstForVampire(vampire, fromPlayerKill);

        if (newThirst >= 1.0F && this.vampireManager.getVampireStage(vampire) < 3) {
            this.promoteVampire(vampire);
        } else {
            vampire.setExp(Math.min(maxThirst, newThirst));
        }
    }

    /**
     * Determine the blood cap of the vampire.
     *
     * @param vampire the player being checked.
     * @param fromPlayerKill {@code true} if the experience has come from killing another player.
     * @return The percentage of the blood bar that can be filled.
     */
    private float getMaxThirstForVampire(Player vampire, boolean fromPlayerKill) {
        if (this.vampireManager.getVampireStage(vampire) >= 3) {
            return 0.99F;
        } else {
            return this.vampireManager.hasPromotionBan(vampire) && fromPlayerKill ? 0.99F : 1.0F;
        }
    }

    /**
     * Increase the vampire's blood amount, and promote them if enough blood is drunk.
     *
     * @param vampire the player gaining blood.
     * @param quenchPoints the points of blood to gain.
     */
    public void modifyQuench(Player vampire, int quenchPoints) {
        this.quenchThirst(vampire, quenchPoints, false);
    }

    /**
     *Increase the vampire's blood amount, and promote them if enough blood is drunk.
     *
     * @param vampire the player gaining blood.
     * @param quenchPoints the points of blood to gain.
     * @param fromPlayerKill {@code true} if the blood has come from drinking from another player.
     */
    public void modifyQuench(Player vampire, int quenchPoints, boolean fromPlayerKill) {
        this.quenchThirst(vampire, quenchPoints, fromPlayerKill);
    }

    /**
     * Retrieve the blood rewarded when a vampire kills a human player.
     *
     * @param killer the player who killed the victim.
     * @param victim the player who has been killed
     * @return The blood experience points to be rewarded.
     */
    public int getKillThirstReward(Player killer, Player victim) {
        return 75;
    }

    /**
     * Attempt to level the vampire up to the next stage of vampirism.
     *
     * @param vampire the player who drank enough blood.
     */
    public void promoteVampire(Player vampire) {
        if (this.vampireManager.hasPromotionBan(vampire)) {
            vampire.sendMessage(Component.text("PROMOTION DENIED", NamedTextColor.DARK_RED)
                    .decorate(TextDecoration.BOLD));
            vampire.sendMessage(Component.text("The curse of death still lingers upon you...", NamedTextColor.RED));
            vampire.sendMessage(Component.text("You cannot grow stronger until the next session begins.", NamedTextColor.RED));
            vampire.setExp(0.99F);

        } else {
            int currentStage = this.vampireManager.getVampireStage(vampire);
            int newStage = Math.min(3, currentStage + 1);

            if (this.vampireManager.hasStageCap(vampire)) {
                int stageCap = this.vampireManager.getStageCap(vampire);

                if (newStage > stageCap) {
                    vampire.sendMessage(Component.text("PROMOTION DENIED", NamedTextColor.DARK_RED)
                            .decorate(TextDecoration.BOLD));
                    vampire.sendMessage(Component.text("The weakness of your starvation still haunts you...", NamedTextColor.RED));
                    vampire.sendMessage(Component.text("You cannot reach Stage " + newStage + " until the next session begins.", NamedTextColor.RED));
                    vampire.setExp(0.99F);
                    return;
                }
            }

            this.vampireManager.setPlayerAsVampire(vampire, newStage);
            this.giveThirstImmunity(vampire);
            vampire.setExp(0.25F);

            vampire.sendMessage(Component.text("ASCENSION", NamedTextColor.DARK_RED)
                    .decorate(TextDecoration.BOLD));
            vampire.sendMessage(Component.text("The crimson blood coats the inside of your throat, your pupils dilate as your tension eases.", NamedTextColor.RED));
            vampire.sendMessage(Component.text("Your thirst is quenched, you are stronger, for now...", NamedTextColor.RED));
            vampire.sendMessage(Component.text("You are now a Stage " + newStage + " vampire.", NamedTextColor.DARK_PURPLE));
            vampire.playSound(vampire, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.MASTER, 1.0F, 0.5F);
        }
    }

    /**
     * Drop the vampire down a stage and apply stage capping.
     *
     * @param vampire the player who lost too much blood.
     * @param fromStarvation {@code false} if the vampire has dropped their stage because of dying.
     */
    private void demoteVampire(Player vampire, boolean fromStarvation) {
        final int currentStage = this.vampireManager.getVampireStage(vampire);

        if (currentStage > 1) {
            if (fromStarvation) {
                final int newStage = currentStage - 1;
                this.vampireManager.setStageCap(vampire, newStage);
                vampire.sendMessage(Component.text("You cannot return to Stage " + currentStage + " until the next session begins.", NamedTextColor.DARK_RED)
                        .decorate(TextDecoration.BOLD));
            }

            this.vampireManager.reduceVampireStage(vampire);
            this.giveThirstImmunity(vampire);
            vampire.setExp(0.5F);

            if (fromStarvation) {
                vampire.sendMessage(Component.text("WEAKENING", NamedTextColor.DARK_RED)
                        .decorate(TextDecoration.BOLD));
                vampire.sendMessage(Component.text("The pain of hunger stabs through your stomach like a knife.", NamedTextColor.RED));
                vampire.sendMessage(Component.text("You feel weaker. Closer to death than ever before... Be careful, spawn.", NamedTextColor.RED));

            } else {
                vampire.sendMessage(Component.text("DEATH'S EMBRACE", NamedTextColor.DARK_RED)
                        .decorate(TextDecoration.BOLD));
                vampire.sendMessage(Component.text("The world fades to grey, and you awake within your resting grounds.", NamedTextColor.RED));
            }

            vampire.playSound(vampire, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, SoundCategory.MASTER, 1.0F, 1.0F);
        }
    }

    /**
     * Grant the vampire thirst immunity.
     *
     * @param vampire the player gaining thirst immunity.
     */
    private void giveThirstImmunity(Player vampire) {
        final UUID playerUUID = vampire.getUniqueId();
        this.immunityTimers.put(playerUUID, IMMUNITY_DURATION_MINUTES);
        vampire.addScoreboardTag(THIRST_IMMUNITY_TAG);
        this.saveImmunityData();
    }

    /**
     * Update the list of players with thirst immunity and inform those whose immunity has expired.
     */
    private void updateImmunityTimers() {
        Set<UUID> onlinePlayers = Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getUniqueId).collect(Collectors.toSet());
        Set<UUID> toRemove = new HashSet<>();

        for (Map.Entry<UUID, Integer> entry : this.immunityTimers.entrySet()) {
            final UUID playerUUID = entry.getKey();

            if (onlinePlayers.contains(playerUUID)) {
                int timeLeft = entry.getValue() - 1;

                if (timeLeft <= 0) {
                    toRemove.add(playerUUID);
                    Player player = Bukkit.getPlayer(playerUUID);

                    if (player != null) {
                        player.removeScoreboardTag(THIRST_IMMUNITY_TAG);

                        if (this.vampireManager.isVampire(player)) {
                            player.sendMessage(Component.text("IMMUNITY EXPIRED", NamedTextColor.DARK_RED)
                                    .decorate(TextDecoration.BOLD));
                            player.sendMessage(Component.text("The stabbing pain in your gut tells you everything you need to know...", NamedTextColor.RED));
                            player.sendMessage(Component.text("The time to feed is approaching...", NamedTextColor.RED));
                            player.playSound(player, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, SoundCategory.MASTER, 1.0F, 1.0F);
                        }
                    }
                } else {
                    this.immunityTimers.put(playerUUID, timeLeft);
                }
            }
        }

        // Remove the expired immunity timers from the map
        for (UUID uuid : toRemove) {
            this.immunityTimers.remove(uuid);
        }

        this.saveImmunityData();
    }

    /**
     * Use the vampire's blood to regenerate their food.
     *
     * @param vampire the player consuming their blood.
     */
    public void regenerateFood(Player vampire) {
        if (!this.plugin.getHolyWaterEffectManager().isAbilitiesDisabled(vampire)) {
            final int currentFoodLevel = vampire.getFoodLevel();

            if (currentFoodLevel < 20) {
                final int foodToRegen = Math.min(1, 20 - currentFoodLevel);
                final float thirstCost = foodToRegen * 0.0105F, currentThirst = vampire.getExp();

                if (currentThirst < thirstCost) {
                    if (this.vampireManager.getVampireStage(vampire) > 1) {
                        this.demoteVampire(vampire, true);
                    }
                } else {
                    vampire.setExp(Math.max(0.0F, currentThirst - thirstCost));
                    vampire.setFoodLevel(Math.min(20, currentFoodLevel + foodToRegen));
                    vampire.setSaturation(Math.min((float)vampire.getFoodLevel(), vampire.getSaturation() + 1.0F));
                }
            }
        }
    }

    /**
     * Retrieve if an entity can give vampires blood.
     *
     * @param entityType the type of entity being checked.
     * @return {@code true} if the entity is listed as the vampire's prey.
     */
    public boolean isThirstQuencher(EntityType entityType) {
        return this.thirstQuenchers.contains(entityType);
    }

    /**
     * Retrieve if the player has an active vampiric thirst immunity.
     *
     * @param player the player being checked.
     * @return {@code true} if the player has thirst immunity.
     */
    public boolean hasThirstImmunity(Player player) {
        return player.getScoreboardTags().contains(THIRST_IMMUNITY_TAG);
    }

    /**
     * Retrieve the remaining time on the player's thirst immunity timer.
     *
     * @param player the player with a timer.
     * @return The remaining seconds of the thirst immunity.
     */
    public int getRemainingImmunity(Player player) {
        return this.immunityTimers.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * Stop monitoring the blood bar thirst depletion before shutting down the manager.
     */
    public void shutdown() {
        if (this.thirstTask != null) {
            this.thirstTask.cancel();
        }

        this.saveImmunityData();
    }

    /**
     * Demote the vampire to stage 1.
     *
     * @param vampire the player who died.
     */
    public void handleVampireDeath(Player vampire) {
        this.demoteVampire(vampire, false);
    }
}
