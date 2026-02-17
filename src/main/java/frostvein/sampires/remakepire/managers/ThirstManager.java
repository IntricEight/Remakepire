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
    private final SessionManager sessionManager;
    private final ConfigManager configManager;
    private final float THIRST_PER_SECOND;
    private final int IMMUNITY_DURATION_MINUTES = 15;
    private File immunityFile;
    private Map<UUID, Integer> immunityTimers = new HashMap();
    private BukkitTask thirstTask;
    private int minuteCounter = 60;
    private final Set<EntityType> thirstQuenchers;
    public static final String THIRST_IMMUNITY_TAG = "ImmuneToThirst";

    public ThirstManager(RemakepirePlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.vampireManager = plugin.getVampireManager();
        this.sessionManager = plugin.getSessionManager();
        this.thirstQuenchers = this.initializeThirstQuenchers();
        int depletionMinutes = configManager.getThirstDepletionMinutes();
        this.THIRST_PER_SECOND = 1.0F / (float)depletionMinutes / 60.0F;
        this.setupImmunitySystem();
        this.startThirstTask();
    }

    private Set<EntityType> initializeThirstQuenchers() {
        Set<EntityType> quenchers = new HashSet();
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
        return quenchers;
    }

    private void setupImmunitySystem() {
        if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
        }

        this.immunityFile = new File(this.plugin.getDataFolder(), "thirst_immunity.txt");
        if (!this.immunityFile.exists()) {
            try {
                this.immunityFile.createNewFile();
                this.plugin.getLogger().info("Created thirst immunity persistence file");
            } catch (IOException e) {
                this.plugin.getLogger().severe("Failed to create thirst immunity file: " + e.getMessage());
                e.printStackTrace();
            }
        }

        this.loadImmunityData();
    }

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

    private void saveImmunityData() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.immunityFile))) {
            for(Map.Entry<UUID, Integer> entry : this.immunityTimers.entrySet()) {
                String var10001 = ((UUID)entry.getKey()).toString();
                writer.write(var10001 + ":" + String.valueOf(entry.getValue()));
                writer.newLine();
            }
        } catch (IOException e) {
            this.plugin.getLogger().warning("Could not save thirst immunity data: " + e.getMessage());
        }

    }

    private void startThirstTask() {
        this.thirstTask = (new BukkitRunnable() {
            public void run() {
                if (ThirstManager.this.sessionManager.isSessionActive()) {
                    for(Player player : Bukkit.getOnlinePlayers()) {
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

    private void processVampireThirst(Player vampire) {
        if (!vampire.getScoreboardTags().contains("ImmuneToThirst")) {
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

    private void handleThirstStarvation(Player vampire) {
        int currentStage = this.vampireManager.getVampireStage(vampire);
        if (currentStage > 1) {
            this.demoteVampire(vampire, true);
        }

    }

    public void handleEntityKill(Player vampire, EntityType entityType, int experienceDropped) {
        if (this.thirstQuenchers.contains(entityType)) {
            experienceDropped = Math.max(experienceDropped * 2 + 3, 1);
            if (entityType == EntityType.WANDERING_TRADER || entityType == EntityType.PILLAGER || entityType == EntityType.VILLAGER) {
                experienceDropped += 10;
            }

            this.quenchThirst(vampire, experienceDropped);
        }
    }

    public void quenchThirst(Player vampire, int experienceDropped) {
        this.quenchThirst(vampire, experienceDropped, false);
    }

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

    private float getMaxThirstForVampire(Player vampire, boolean fromPlayerKill) {
        if (this.vampireManager.getVampireStage(vampire) >= 3) {
            return 0.99F;
        } else {
            return this.vampireManager.hasPromotionBan(vampire) && fromPlayerKill ? 0.99F : 1.0F;
        }
    }

    public void modifyQuench(Player vampire, int quenchPoints) {
        this.quenchThirst(vampire, quenchPoints, false);
    }

    public void modifyQuench(Player vampire, int quenchPoints, boolean fromPlayerKill) {
        this.quenchThirst(vampire, quenchPoints, fromPlayerKill);
    }

    public int getKillThirstReward(Player killer, Player victim) {
        return 75;
    }

    public void promoteVampire(Player vampire) {
        if (this.vampireManager.hasPromotionBan(vampire)) {
            vampire.sendMessage("§4§lPROMOTION DENIED");
            vampire.sendMessage("§c§lThe curse of death still lingers upon you...");
            vampire.sendMessage("§c§lYou cannot grow stronger until the next session begins.");
            vampire.setExp(0.99F);
        } else {
            int currentStage = this.vampireManager.getVampireStage(vampire);
            int newStage = Math.min(3, currentStage + 1);
            if (this.vampireManager.hasStageCap(vampire)) {
                int stageCap = this.vampireManager.getStageCap(vampire);
                if (newStage > stageCap) {
                    vampire.sendMessage("§4§lPROMOTION DENIED");
                    vampire.sendMessage("§c§lThe weakness of your starvation still haunts you...");
                    vampire.sendMessage("§c§lYou cannot reach Stage " + newStage + " until the next session begins.");
                    vampire.setExp(0.99F);
                    return;
                }
            }

            this.vampireManager.setPlayerAsVampire(vampire, newStage);
            this.giveThirstImmunity(vampire);
            vampire.setExp(0.25F);
            vampire.sendMessage("§4§lASCENSION");
            vampire.sendMessage("§cThe crimson blood coats the inside of your throat, your pupils dilate as your tension eases.");
            vampire.sendMessage("§cYour thirst is quenched, you are stronger, for now...");
            vampire.sendMessage("§5You are now a Stage " + newStage + " vampire.");
            vampire.playSound(vampire, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.MASTER, 1.0F, 0.5F);
        }
    }

    private void demoteVampire(Player vampire, boolean fromStarvation) {
        int currentStage = this.vampireManager.getVampireStage(vampire);
        if (currentStage > 1) {
            if (fromStarvation) {
                int newStage = currentStage - 1;
                this.vampireManager.setStageCap(vampire, newStage);
                vampire.sendMessage("§4§lYou cannot return to Stage " + currentStage + " until the next session begins.");
            }

            this.vampireManager.reduceVampireStage(vampire);
            this.giveThirstImmunity(vampire);
            vampire.setExp(0.5F);
            if (fromStarvation) {
                vampire.sendMessage("§4§lWEAKENING");
                vampire.sendMessage("§c§lThe pain of hunger stabs through your stomach like a knife.");
                vampire.sendMessage("§c§lYou feel weaker. Closer to death than ever before... Be careful, spawn.");
            } else {
                vampire.sendMessage("§4§lDEATH'S EMBRACE");
                vampire.sendMessage("§c§lThe world fades to grey, and you awake within your coffin.");
            }

            vampire.playSound(vampire, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, SoundCategory.MASTER, 1.0F, 1.0F);
        }
    }

    private void giveThirstImmunity(Player vampire) {
        UUID playerUUID = vampire.getUniqueId();
        this.immunityTimers.put(playerUUID, 15);
        vampire.addScoreboardTag("ImmuneToThirst");
        this.saveImmunityData();
    }

    private void updateImmunityTimers() {
        Set<UUID> onlinePlayers = (Set)Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getUniqueId).collect(Collectors.toSet());
        Set<UUID> toRemove = new HashSet();

        for(Map.Entry<UUID, Integer> entry : this.immunityTimers.entrySet()) {
            UUID playerUUID = (UUID)entry.getKey();
            if (onlinePlayers.contains(playerUUID)) {
                int timeLeft = (Integer)entry.getValue() - 1;
                if (timeLeft <= 0) {
                    toRemove.add(playerUUID);
                    Player player = Bukkit.getPlayer(playerUUID);
                    if (player != null) {
                        player.removeScoreboardTag("ImmuneToThirst");
                        if (this.vampireManager.isVampire(player)) {
                            player.sendMessage("§4§lIMMUNITY EXPIRED");
                            player.sendMessage("§cThe stabbing pain in your gut tells you everything you need to know...");
                            player.sendMessage("§cThe time to feed is approaching...");
                            player.playSound(player, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, SoundCategory.MASTER, 1.0F, 1.0F);
                        }
                    }
                } else {
                    this.immunityTimers.put(playerUUID, timeLeft);
                }
            }
        }

        for(UUID uuid : toRemove) {
            this.immunityTimers.remove(uuid);
        }

        this.saveImmunityData();
    }

    public void regenerateFood(Player vampire) {
        if (!this.plugin.getHolyWaterEffectManager().isAbilitiesDisabled(vampire)) {
            int currentFoodLevel = vampire.getFoodLevel();
            if (currentFoodLevel < 20) {
                int foodToRegen = Math.min(1, 20 - currentFoodLevel);
                float thirstCost = (float)foodToRegen * 0.0105F;
                float currentThirst = vampire.getExp();
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

    public boolean isThirstQuencher(EntityType entityType) {
        return this.thirstQuenchers.contains(entityType);
    }

    public boolean hasThirstImmunity(Player player) {
        return player.getScoreboardTags().contains("ImmuneToThirst");
    }

    public int getRemainingImmunity(Player player) {
        return (Integer)this.immunityTimers.getOrDefault(player.getUniqueId(), 0);
    }

    public void shutdown() {
        if (this.thirstTask != null) {
            this.thirstTask.cancel();
        }

        this.saveImmunityData();
    }

    public void handleVampireDeath(Player vampire) {
        this.demoteVampire(vampire, false);
    }
}
