package frostvein.sampires.remakepire.managers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.abilities.BatAbility;
import frostvein.sampires.remakepire.abilities.BeaconTeleportAbility;
import frostvein.sampires.remakepire.abilities.InvisibilityAbility;
import frostvein.sampires.remakepire.abilities.LungeAbility;
import frostvein.sampires.remakepire.abilities.RaiseUndeadAbility;
import frostvein.sampires.remakepire.abilities.StormCallAbility;
import frostvein.sampires.remakepire.abilities.VampireAbility;
import frostvein.sampires.remakepire.abilities.VampireVisionAbility;
import frostvein.sampires.remakepire.abilities.tome.HolyWordTomeAbility;
import frostvein.sampires.remakepire.abilities.tome.TomeAbility;

public class VampireAbilityManager {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final SessionManager sessionManager;
    private final Map<UUID, Map<String, Long>> abilityCooldowns = new ConcurrentHashMap<>();
    private final Map<String, GlobalCooldownData> globalCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> invisibilityAttackCounts = new ConcurrentHashMap<>();
    private final Map<String, VampireAbility> abilities = new HashMap<>();
    private File cooldownFile, globalCooldownFile;
    private BukkitTask cooldownTask;
    private static final String COOLDOWN_FILE_VERSION = "VERSION:2";

    /**
     * Create an instance of the Vampire Ability manager.
     *
     * @param plugin the host plugin object.
     */
    public VampireAbilityManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.sessionManager = plugin.getSessionManager();
        this.setupPersistence();
        this.registerAbilities();
        this.startCooldownTask();
        this.loadCooldowns();
        this.loadGlobalCooldowns();
    }

    /**
     * Create the files to store individual and global ability cooldowns in.
     */
    private void setupPersistence() {
        if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
        }

        this.cooldownFile = new File(this.plugin.getDataFolder(), "ability_cooldowns.txt");
        this.globalCooldownFile = new File(this.plugin.getDataFolder(), "global_ability_cooldowns.txt");

        try {
            if (!this.cooldownFile.exists()) {
                this.cooldownFile.createNewFile();
            }

            if (!this.globalCooldownFile.exists()) {
                this.globalCooldownFile.createNewFile();
            }

            this.plugin.logInfo("Created ability cooldown persistence files");
        } catch (IOException e) {
            this.plugin.getLogger().severe("Failed to create ability cooldown files: " + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * List all vampire abilities into a {@code Map}.
     */
    private void registerAbilities() {
        this.registerAbility(new LungeAbility());
        this.registerAbility(new InvisibilityAbility());
        this.registerAbility(new StormCallAbility());
        this.registerAbility(new BeaconTeleportAbility());
        this.registerAbility(new BatAbility());
        this.registerAbility(new VampireVisionAbility());
        this.registerAbility(new RaiseUndeadAbility());

        this.plugin.logInfo("Registered " + this.abilities.size() + " vampire abilities");
    }

    /**
     * Add a new ability to the list of vampire abilities.
     *
     * @param ability the ability to add.
     */
    private void registerAbility(VampireAbility ability) {
        this.abilities.put(ability.getName().toLowerCase(), ability);
    }

    /**
     * Begin checking the cooldowns on vampire abilities.
     */
    private void startCooldownTask() {
        this.cooldownTask = (new BukkitRunnable() {
            public void run() {
                VampireAbilityManager.this.checkCooldownExpirations();
                VampireAbilityManager.this.checkGlobalCooldownExpirations();
            }
        }).runTaskTimer(this.plugin, 20L, 20L);
    }


    /**
     * Notify vampires when their ability's cooldown has elapsed.
     */
    private void checkCooldownExpirations() {
        long currentTime = this.sessionManager.getSessionTimeSeconds();

        for(UUID playerId : this.abilityCooldowns.keySet()) {
            Player player = Bukkit.getPlayer(playerId);

            if (player != null && player.isOnline()) {
                Map<String, Long> playerCooldowns = this.abilityCooldowns.get(playerId);
                Iterator<Map.Entry<String, Long>> iterator = playerCooldowns.entrySet().iterator();

                while(iterator.hasNext()) {
                    Map.Entry<String, Long> entry = iterator.next();
                    String abilityName = entry.getKey();
                    long cooldownEnd = entry.getValue();

                    if (currentTime >= cooldownEnd) {
                        iterator.remove();
                        this.notifyAbilityReady(player, abilityName);
                    }
                }

                if (playerCooldowns.isEmpty()) {
                    this.abilityCooldowns.remove(playerId);
                }
            }
        }
    }

    /**
     * Notify vampires when the cooldown on shared vampire abilities has elapsed.
     */
    private void checkGlobalCooldownExpirations() {
        long currentTime = this.sessionManager.getSessionTimeSeconds();
        Iterator<Map.Entry<String, GlobalCooldownData>> iterator = this.globalCooldowns.entrySet().iterator();

        while(iterator.hasNext()) {
            Map.Entry<String, GlobalCooldownData> entry = iterator.next();
            String abilityName = entry.getKey();
            GlobalCooldownData data = entry.getValue();

            if (currentTime >= data.endTime) {
                iterator.remove();
                this.notifyGlobalAbilityReady(abilityName);
                this.saveGlobalCooldowns();
            }
        }

    }

    /**
     * Inform the player that their vampire ability is ready to use.
     *
     * @param player the player who had used the ability.
     * @param abilityName the name of the ability.
     */
    private void notifyAbilityReady(Player player, String abilityName) {
        VampireAbility ability = this.abilities.get(abilityName.toLowerCase());

        if (ability != null) {
            player.sendMessage("§a§l⚡ ABILITY READY ⚡");
            player.sendMessage("§a" + ability.getDisplayName() + " is now available.");
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.MASTER, 0.5F, 2.0F);
        }
    }

    /**
     * Inform all vampires that the shared vampire ability is ready to use.
     *
     * @param abilityName the name of the ability.
     */
    private void notifyGlobalAbilityReady(String abilityName) {
        VampireAbility ability = this.abilities.get(abilityName.toLowerCase());

        if (ability != null) {
            for(Player player : Bukkit.getOnlinePlayers()) {
                if (this.vampireManager.isVampire(player) && ability.canUse(player, this.vampireManager)) {
                    player.sendMessage("§6§l⚡ GLOBAL ABILITY READY ⚡");
                    player.sendMessage("§6" + ability.getDisplayName() + " is now available to all vampires.");
                    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.MASTER, 0.7F, 1.5F);
                }
            }
        }
    }

    /**
     * Attempt to use a vampire ability.
     *
     * @param player the player who cast the ability.
     * @param abilityName the name of the ability.
     * @return {@code true} if the ability was successfully used.
     */
    public boolean useAbility(Player player, String abilityName) {
        if (!this.vampireManager.isVampire(player)) {
            player.sendMessage("§cOnly vampires can use abilities.");
            return false;

        } else if (player.getGameMode() == GameMode.SPECTATOR) {
            player.sendMessage("§cYou cannot use vampire abilities while in spectator mode.");
            return false;

        } else if (!this.sessionManager.isSessionActive()) {
            player.sendMessage("§cAbilities cannot be used while the session is inactive.");
            return false;

        } else if (this.plugin.getHolyWaterEffectManager().isAbilitiesDisabled(player)) {
            long remainingTime = this.plugin.getHolyWaterEffectManager().getRemainingDisableTime(player);
            player.sendMessage("§4§lHOLY WATER EFFECT ACTIVE");
            player.sendMessage("§cYour abilities are disabled by holy water!");
            player.sendMessage("§cAbilities will return in approximately " + (remainingTime / 60L + 1L) + " minute(s).");
            return false;

        } else {
            TomeAbility holyWordAbility = this.plugin.getTomeManager().getAbility("holyword");

            if (holyWordAbility instanceof HolyWordTomeAbility && ((HolyWordTomeAbility)holyWordAbility).isParalyzed(player)) {
                player.sendMessage("§cYou cannot use abilities while being cured.");
                return false;

            } else if (this.plugin.getForcedCureChoiceManager().hasPendingCure(player)) {
                player.sendMessage("§cYou cannot use abilities while being cured.");
                return false;

            } else {
                VampireAbility ability = this.abilities.get(abilityName.toLowerCase());

                if (ability == null) {
                    player.sendMessage("§cUnknown ability: " + abilityName);
                    return false;

                } else if (!ability.canUse(player, this.vampireManager)) {
                    String requirement = ability.getRequirementMessage(player, this.vampireManager);
                    player.sendMessage("§c" + requirement);
                    return false;

                } else {
                    if (ability instanceof StormCallAbility) {
                        if (this.isOnGlobalCooldown(abilityName)) {
                            long remainingSeconds = this.getRemainingGlobalCooldown(abilityName);
                            GlobalCooldownData data = this.globalCooldowns.get(abilityName.toLowerCase());

                            player.sendMessage("§c§l GLOBAL ABILITY COOLDOWN");
                            player.sendMessage("§c" + ability.getDisplayName() + " was recently used by " + data.lastUserName + ".");
                            player.sendMessage("§cIt will be available to all vampires in " + formatTime(remainingSeconds) + ".");

                            return false;
                        }
                    } else {
                        if (abilityName.equalsIgnoreCase("vision")) {
                            return ability.execute(player, this.vampireManager, this.plugin);
                        }

                        if (abilityName.equalsIgnoreCase("vanish") && player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                            return ability.execute(player, this.vampireManager, this.plugin);
                        }

                        if (this.isOnCooldown(player, abilityName)) {
                            long remainingSeconds = this.getRemainingCooldown(player, abilityName);
                            player.sendMessage("§c§l ABILITY ON COOLDOWN");
                            player.sendMessage("§c" + ability.getDisplayName() + " will be ready in " + formatTime(remainingSeconds) + ".");
                            return false;
                        }
                    }

                    boolean wasInvisibleBeforeVanish = false;
                    if (abilityName.equalsIgnoreCase("vanish")) {
                        wasInvisibleBeforeVanish = player.hasPotionEffect(PotionEffectType.INVISIBILITY);
                    }

                    if (ability.execute(player, this.vampireManager, this.plugin)) {
                        if (ability instanceof StormCallAbility) {
                            this.setGlobalCooldown(abilityName, ability.getCooldownSeconds(this.plugin), player);
                            this.saveGlobalCooldowns();

                        } else if (!abilityName.equalsIgnoreCase("vision") && (!abilityName.equalsIgnoreCase("vanish") || !wasInvisibleBeforeVanish)) {
                            this.setCooldown(player, abilityName, ability.getCooldownSeconds(this.plugin));
                            this.saveCooldowns();
                        }

                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }
    }

    /**
     * Set the cooldown for the vampire ability used by the player.
     *
     * @param player the player who cast the ability.
     * @param abilityName the name of the ability.
     * @return {@code true} if the ability cooldown was activated.
     */
    public boolean applyCooldownForAbility(Player player, String abilityName) {
        VampireAbility ability = this.abilities.get(abilityName.toLowerCase());

        if (ability == null) {
            return false;

        } else {
            if (ability instanceof StormCallAbility) {
                this.setGlobalCooldown(abilityName, ability.getCooldownSeconds(this.plugin), player);
                this.saveGlobalCooldowns();

            } else {
                this.setCooldown(player, abilityName, ability.getCooldownSeconds(this.plugin));
                this.saveCooldowns();
            }

            return true;
        }
    }

    /**
     * Determine if the vampire ability cooldown has elapsed for the player.
     *
     * @param player the player attempting to use the ability.
     * @param abilityName the name of the ability.
     * @return {@code true} if the ability is on cooldown.
     */
    public boolean isOnCooldown(Player player, String abilityName) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> playerCooldowns = this.abilityCooldowns.get(playerId);

        if (playerCooldowns == null) {
            return false;

        } else {
            Long cooldownEnd = playerCooldowns.get(abilityName.toLowerCase());

            if (cooldownEnd == null) {
                return false;

            } else {
                long currentTime = this.sessionManager.getSessionTimeSeconds();
                return currentTime < cooldownEnd;
            }
        }
    }

    /**
     * Determine if the vampire ability is on a global cooldown.
     *
     * @param abilityName the name of the ability.
     * @return {@code true} if the ability is on cooldown.
     */
    public boolean isOnGlobalCooldown(String abilityName) {
        GlobalCooldownData data = this.globalCooldowns.get(abilityName.toLowerCase());

        if (data == null) {
            return false;
        } else {
            long currentTime = this.sessionManager.getSessionTimeSeconds();
            return currentTime < data.endTime;
        }
    }

    /**
     * Retrieve the remaining time until the player can use the ability again.
     *
     * @param player the player attempting to use the ability.
     * @param abilityName the name of the ability.
     * @return The remaining seconds until the cooldown has elapsed.
     */
    public long getRemainingCooldown(Player player, String abilityName) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> playerCooldowns = this.abilityCooldowns.get(playerId);

        if (playerCooldowns == null) {
            return 0L;

        } else {
            Long cooldownEnd = playerCooldowns.get(abilityName.toLowerCase());

            if (cooldownEnd == null) {
                return 0L;

            } else {
                long currentTime = this.sessionManager.getSessionTimeSeconds();
                return Math.max(0L, cooldownEnd - currentTime);
            }
        }
    }

    /**
     * Retrieve the remaining time until a player can use the global ability again.
     *
     * @param abilityName the name of the ability.
     * @return The remaining seconds until the cooldown has elapsed.
     */
    public long getRemainingGlobalCooldown(String abilityName) {
        GlobalCooldownData data = this.globalCooldowns.get(abilityName.toLowerCase());

        if (data == null) {
            return 0L;

        } else {
            long currentTime = this.sessionManager.getSessionTimeSeconds();
            return Math.max(0L, data.endTime - currentTime);
        }
    }

    /**
     * Store the cooldown for the vampire ability used by the player.
     *
     * @param player the player who used the ability.
     * @param abilityName the name of the ability.
     * @param cooldownSeconds the time until the player can use the ability again.
     */
    private void setCooldown(Player player, String abilityName, int cooldownSeconds) {
        UUID playerId = player.getUniqueId();
        long cooldownEnd = this.sessionManager.getSessionTimeSeconds() + (long)cooldownSeconds;
        (this.abilityCooldowns.computeIfAbsent(playerId, k -> new HashMap<>())).put(abilityName.toLowerCase(), cooldownEnd);
    }

    /**
     * Store the cooldown for the global vampire ability.
     *
     * @param abilityName the name of the ability.
     * @param cooldownSeconds the time until the ability may be used again.
     * @param lastUser the player who used the ability.
     */
    private void setGlobalCooldown(String abilityName, int cooldownSeconds, Player lastUser) {
        long cooldownEnd = this.sessionManager.getSessionTimeSeconds() + (long)cooldownSeconds;
        GlobalCooldownData data = new GlobalCooldownData(cooldownEnd, lastUser.getName(), lastUser.getUniqueId());
        this.globalCooldowns.put(abilityName.toLowerCase(), data);
    }

    /**
     * Reset all the player's cooldowns.
     *
     * @param player the player with active cooldowns.
     */
    public void clearAllCooldowns(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> playerCooldowns = this.abilityCooldowns.get(playerId);

        if (playerCooldowns != null) {
            int clearedCount = playerCooldowns.size();
            playerCooldowns.clear();
            this.abilityCooldowns.remove(playerId);
            this.plugin.logInfo("Cleared " + clearedCount + " personal cooldowns for player: " + player.getName());
        }

        this.saveCooldowns();
    }

    /**
     * Reset all cooldowns for global vampire abilities.
     */
    public void clearGlobalCooldowns() {
        if (!this.globalCooldowns.isEmpty()) {
            int clearedCount = this.globalCooldowns.size();
            List<String> clearedAbilities = new ArrayList<>(this.globalCooldowns.keySet());
            this.globalCooldowns.clear();
            this.plugin.logInfo("Cleared " + clearedCount + " global cooldowns for abilities: " + String.join(", ", clearedAbilities));

            for(String abilityName : clearedAbilities) {
                this.notifyGlobalAbilityReady(abilityName);
            }
        }

        this.saveGlobalCooldowns();
    }

    /**
     * Reset all vampire ability cooldowns.
     */
    public void clearAllCooldownsForNewSession() {
        int clearedPersonal = 0, clearedGlobal = 0;

        for(Map<String, Long> playerCooldowns : this.abilityCooldowns.values()) {
            clearedPersonal += playerCooldowns.size();
        }

        this.abilityCooldowns.clear();

        if (!this.globalCooldowns.isEmpty()) {
            clearedGlobal = this.globalCooldowns.size();
            this.globalCooldowns.clear();
        }

        this.plugin.logInfo("NEW SESSION: Cleared " + clearedPersonal + " personal cooldowns and " + clearedGlobal + " global cooldowns for new session");

        this.saveCooldowns();
        this.saveGlobalCooldowns();
    }

    /**
     * Retrieve all vampire abilities available to the player.
     *
     * @param player the player checking their abilities.
     * @return A {@code List} of vampire abilities.
     */
    public List<VampireAbility> getAvailableAbilities(Player player) {
        List<VampireAbility> available = new ArrayList<>();

        for(VampireAbility ability : this.abilities.values()) {
            if (ability.canUse(player, this.vampireManager)) {
                available.add(ability);
            }
        }

        return available;
    }

    /**
     * Retrieve all vampire abilities.
     *
     * @return A {@code Collection} of existing vampire abilities.
     */
    public Collection<VampireAbility> getAllAbilities() {
        return this.abilities.values();
    }

    /**
     * Retrieve a vampire ability.
     *
     * @param name the name of the ability.
     * @return The vampire ability.
     */
    public VampireAbility getAbility(String name) {
        return this.abilities.get(name.toLowerCase());
    }

    /**
     * Retrieve the cooldown and usage info on a global vampire ability.
     *
     * @param abilityName the name of the ability.
     * @return A string description of the ability's condition.
     */
    public String getGlobalCooldownInfo(String abilityName) {
        GlobalCooldownData data = this.globalCooldowns.get(abilityName.toLowerCase());

        if (data != null && this.isOnGlobalCooldown(abilityName)) {
            long remaining = this.getRemainingGlobalCooldown(abilityName);
            return "Global cooldown: " + formatTime(remaining) + " (last used by " + data.lastUserName + ")";

        } else {
            return null;
        }
    }

    /**
     * Load vampire ability cooldowns in from the file.
     */
    private void loadCooldowns() {
        try {
            try (BufferedReader reader = new BufferedReader(new FileReader(this.cooldownFile))) {
                String firstLine = reader.readLine();

                if (firstLine != null && firstLine.equals("VERSION:2")) {
                    int loadedCount = 0;

                    String line;

                    while((line = reader.readLine()) != null) {
                        String[] parts = line.split(":");

                        if (parts.length == 3) {
                            try {
                                UUID playerId = UUID.fromString(parts[0]);
                                String abilityName = parts[1];
                                long cooldownEnd = Long.parseLong(parts[2]);

                                (this.abilityCooldowns.computeIfAbsent(playerId, k -> new HashMap<>())).put(abilityName, cooldownEnd);
                                ++loadedCount;

                            } catch (IllegalArgumentException e) {
                                this.plugin.getLogger().warning("Skipping invalid cooldown entry: " + line);
                            }
                        }
                    }

                    this.plugin.logInfo("Loaded " + loadedCount + " personal ability cooldowns from file (version 2)");
                    return;
                }

                this.plugin.getLogger().warning("Personal cooldown file is missing version marker or outdated - clearing old cooldowns");
                this.plugin.logInfo("Old cooldowns were likely using system time instead of session time");

                this.abilityCooldowns.clear();
                this.saveCooldowns();
            }

        } catch (IOException e) {
            this.plugin.getLogger().warning("Could not load personal ability cooldowns: " + e.getMessage());
        }
    }

    /**
     * Load vampire ability global cooldowns in from the file.
     */
    private void loadGlobalCooldowns() {
        try {
            try (BufferedReader reader = new BufferedReader(new FileReader(this.globalCooldownFile))) {
                String firstLine = reader.readLine();

                if (firstLine != null && firstLine.equals("VERSION:2")) {
                    int loadedCount = 0;

                    String line;
                    while((line = reader.readLine()) != null) {
                        String[] parts = line.split(":");

                        if (parts.length == 4) {
                            try {
                                String abilityName = parts[0];
                                long cooldownEnd = Long.parseLong(parts[1]);
                                String lastUserName = parts[2];
                                UUID lastUserUUID = UUID.fromString(parts[3]);

                                GlobalCooldownData data = new GlobalCooldownData(cooldownEnd, lastUserName, lastUserUUID);
                                this.globalCooldowns.put(abilityName, data);
                                ++loadedCount;

                            } catch (IllegalArgumentException e) {
                                this.plugin.getLogger().warning("Skipping invalid global cooldown entry: " + line);
                            }
                        }
                    }

                    this.plugin.logInfo("Loaded " + loadedCount + " global ability cooldowns from file (version 2)");
                    return;
                }

                this.plugin.getLogger().warning("Global cooldown file is missing version marker or outdated - clearing old cooldowns");
                this.plugin.logInfo("Old cooldowns were likely using system time instead of session time");

                this.globalCooldowns.clear();
                this.saveGlobalCooldowns();
            }

        } catch (IOException e) {
            this.plugin.getLogger().warning("Could not load global ability cooldowns: " + e.getMessage());
        }
    }

    /**
     * Save the active vampire ability cooldowns into the file.
     */
    private void saveCooldowns() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.cooldownFile))){
            writer.write(COOLDOWN_FILE_VERSION);
            writer.newLine();

            for (Map.Entry<UUID, Map<String, Long>> playerEntry : this.abilityCooldowns.entrySet()) {
                UUID playerId = playerEntry.getKey();

                for (Map.Entry<String, Long> abilityEntry : playerEntry.getValue().entrySet()) {
                    writer.write(playerId.toString() + ":" + abilityEntry.getKey() + ":" + String.valueOf(abilityEntry.getValue()));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            this.plugin.getLogger().warning("Could not save personal ability cooldowns: " + e.getMessage());
        }
    }

    /**
     * Save the active vampire ability global cooldowns into the file.
     */
    private void saveGlobalCooldowns() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.globalCooldownFile))) {
            writer.write("VERSION:2");
            writer.newLine();

            for(Map.Entry<String, GlobalCooldownData> entry : this.globalCooldowns.entrySet()) {
                String abilityName = entry.getKey();
                GlobalCooldownData data = entry.getValue();
                writer.write(abilityName + ":" + data.endTime + ":" + data.lastUserName + ":" + data.lastUserUUID.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            this.plugin.getLogger().warning("Could not save global ability cooldowns: " + e.getMessage());
        }
    }

    /**
     * Convert a number of seconds into a minutes-seconds format.
     *
     * @param seconds the seconds to convert.
     * @return A string representation of the minutes and seconds conversion.
     */
    public static String formatTime(long seconds) {
        long minutes = seconds / 60L;
        long remainingSeconds = seconds % 60L;
        return String.format("%d:%02d", minutes, remainingSeconds);
    }

    /**
     * Update how many times the player has been hit whilst invisible.
     *
     * @param player the invisible vampire.
     * @return {@code true} if the player's invisibility has been removed.
     */
    public boolean trackInvisibilityAttack(Player player) {
        UUID playerId = player.getUniqueId();
        int attackCount = this.invisibilityAttackCounts.getOrDefault(playerId, 0) + 1;

        if (attackCount >= 3) {
            this.invisibilityAttackCounts.remove(playerId);
            return true;
        } else {
            this.invisibilityAttackCounts.put(playerId, attackCount);
            return false;
        }
    }

    /**
     * Clear the player's counter on hits taken while they were invisible.
     *
     * @param player the vampire who was invisible.
     */
    public void clearInvisibilityAttackCount(Player player) {
        this.invisibilityAttackCounts.remove(player.getUniqueId());
    }

    /**
     * Retrieve how many hits the invisible vampire has taken.
     *
     * @param player the invisible vampire.
     * @return The number of hits taken.
     */
    public int getInvisibilityAttackCount(Player player) {
        return this.invisibilityAttackCounts.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * Stop updating players on the cooldowns before shutting down the manager.
     */
    public void shutdown() {
        if (this.cooldownTask != null) {
            this.cooldownTask.cancel();
        }

        this.saveCooldowns();
        this.saveGlobalCooldowns();
    }

    private static class GlobalCooldownData {
        public final long endTime;
        public final String lastUserName;
        public final UUID lastUserUUID;

        /**
         * Create an instance of the global vampire abilities cooldown record.
         *
         * @param endTime the time when the ability will become usable again.
         * @param lastUserName the name of the player who last used the global ability.
         * @param lastUserUUID the UUID of the player who last used the global ability.
         */
        public GlobalCooldownData(long endTime, String lastUserName, UUID lastUserUUID) {
            this.endTime = endTime;
            this.lastUserName = lastUserName;
            this.lastUserUUID = lastUserUUID;
        }
    }
}
