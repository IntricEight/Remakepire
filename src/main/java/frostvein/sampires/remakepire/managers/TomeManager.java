package frostvein.sampires.remakepire.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.abilities.tome.BanishUndeadTomeAbility;
import frostvein.sampires.remakepire.abilities.tome.BlessingTomeAbility;
import frostvein.sampires.remakepire.abilities.tome.EnlightenedEyeTomeAbility;
import frostvein.sampires.remakepire.abilities.tome.HolyWordTomeAbility;
import frostvein.sampires.remakepire.abilities.tome.LanternThrashTomeAbility;
import frostvein.sampires.remakepire.abilities.tome.PrayerOfFaithTomeAbility;
import frostvein.sampires.remakepire.abilities.tome.RallyingCryTomeAbility;
import frostvein.sampires.remakepire.abilities.tome.ShoulderBargeTomeAbility;
import frostvein.sampires.remakepire.abilities.tome.StopTheBleedingTomeAbility;
import frostvein.sampires.remakepire.abilities.tome.TomeAbility;
import frostvein.sampires.remakepire.abilities.tome.TurnUndeadTomeAbility;
import frostvein.sampires.remakepire.abilities.tome.UncannyDirectionTomeAbility;
import frostvein.sampires.remakepire.abilities.tome.UnnaturalHasteTomeAbility;
import frostvein.sampires.remakepire.abilities.tome.WayOfTheLandTomeAbility;
import frostvein.sampires.remakepire.abilities.tome.WayOfTheLumberjackTomeAbility;
import frostvein.sampires.remakepire.abilities.tome.WayOfTheProspectorTomeAbility;

public class TomeManager {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final Map<String, TomeAbility> abilities;
    private final Map<UUID, Integer> playerTomeUsageSession = new HashMap<>();
    private final Map<UUID, UUID> tomeSelectionTargets = new HashMap<>();
    private final boolean TOME_CAP_ENABLED;
    public static final String TOME_TAG_PREFIX = "tome_ability_";
    public static final String TOME_SELECTION_GUI_TITLE = "§6§lSelect Tome Abilities";

    /**
     * Create an instance of the Tome manager.
     *
     * @param plugin the host plugin object.
     */
    public TomeManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.TOME_CAP_ENABLED = plugin.getConfigManager().isTomeAbsorptionCapped();
        this.abilities = new HashMap<>();
        this.registerTomeAbilities();
    }

    /**
     * List all human tome abilities into an abilities {@code Map}.
     */
    private void registerTomeAbilities() {
        this.registerAbility(new RallyingCryTomeAbility(this.plugin));
        this.registerAbility(new LanternThrashTomeAbility(this.plugin));
        this.registerAbility(new TurnUndeadTomeAbility(this.plugin));
        this.registerAbility(new EnlightenedEyeTomeAbility(this.plugin));
        this.registerAbility(new UnnaturalHasteTomeAbility(this.plugin));
        this.registerAbility(new UncannyDirectionTomeAbility(this.plugin));
        this.registerAbility(new PrayerOfFaithTomeAbility(this.plugin));
        this.registerAbility(new BanishUndeadTomeAbility(this.plugin));
        this.registerAbility(new BlessingTomeAbility(this.plugin));
        this.registerAbility(new ShoulderBargeTomeAbility(this.plugin));
        this.registerAbility(new HolyWordTomeAbility(this.plugin));
        this.registerAbility(new WayOfTheLandTomeAbility(this.plugin));
        this.registerAbility(new WayOfTheLumberjackTomeAbility(this.plugin));
        this.registerAbility(new WayOfTheProspectorTomeAbility(this.plugin));
        this.registerAbility(new StopTheBleedingTomeAbility(this.plugin));

        this.plugin.logInfo("TomeManager initialized - registered " + this.abilities.size() + " tome abilities");
    }

    /**
     * Add a new ability to the list of tome abilities.
     *
     * @param ability the ability to add.
     */
    public void registerAbility(TomeAbility ability) {
        this.abilities.put(ability.getName().toLowerCase(), ability);
        this.plugin.logInfo("Registered tome ability: " + ability.getName());
    }

    /**
     * Retrieve an ability using its name.
     *
     * @param abilityName the name of the ability to retrieve.
     * @return The tome ability.
     */
    public TomeAbility getAbility(String abilityName) {
        return this.abilities.get(abilityName.toLowerCase());
    }

    /**
     * Check if an ability has been registered.
     *
     * @param abilityName the name of the ability.
     * @return {@code true} if the ability is within the abilities {@code Map}.
     */
    public boolean isValidAbility(String abilityName) {
        return this.abilities.containsKey(abilityName.toLowerCase());
    }

    /**
     * Attempt to enable the player to use the tome ability.
     *
     * @param player the player gaining an ability.
     * @param abilityName the name of the ability being granted.
     * @return {@code true} if the player gains the ability.
     */
    public boolean grantAbility(Player player, String abilityName) {
        if (!this.vampireManager.isHuman(player)) {
            return false;
        } else if (!this.isValidAbility(abilityName)) {
            return false;
        } else if (this.hasAbility(player, abilityName)) {
            return false;
        } else if (player.getGameMode() != GameMode.CREATIVE && this.hasUsedTomeThisSession(player)) {
            if (TOME_CAP_ENABLED) {
                player.sendMessage("§cYou have already absorbed one tome this session. Your mind cannot handle more ancient knowledge.");
            } else {
                player.sendMessage("§cYour mind requires more time to recover from the ancient knowledge you absorbed.");
            }

            return false;
        } else {
            String tag = TOME_TAG_PREFIX + abilityName.toLowerCase();
            player.addScoreboardTag(tag);

            if (player.getGameMode() != GameMode.CREATIVE) {
                int currentSessionId = this.plugin.getSessionManager().getSessionIDObjective().getScore("session_id_holder").getScore();
                this.playerTomeUsageSession.put(player.getUniqueId(), currentSessionId);

                // Set a timer to remove the player from the tome prevention list after the timer elapses
                BukkitTask absorptionCooldonTask = Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                    // Only remove the player if tome capping is not enabled. After the timer elapses
                    if (!TOME_CAP_ENABLED) {
                        this.playerTomeUsageSession.remove(player.getUniqueId());

                        if (player.isOnline()) {
                            player.sendMessage("§aYour mind eases, recovered from the strain of ancient knowledge.");
                        }
                    }
                }, (long)plugin.getConfigManager().getTomeAbsorptionIntervalMinutes() * 60 * 20);
            }

            this.plugin.logInfo("Granted tome ability '" + abilityName + "' to player " + player.getName());
            return true;
        }
    }

    /**
     * Check if the player has access to the tome ability.
     *
     * @param player the player being checked.
     * @param abilityName the name of the ability.
     * @return {@code true} if the player has access to the ability.
     */
    public boolean hasAbility(Player player, String abilityName) {
        String tag = TOME_TAG_PREFIX + abilityName.toLowerCase();
        return player.getScoreboardTags().contains(tag);
    }

    /**
     * Retrieve a set of the player's current abilities.
     *
     * @param player the player being checked.
     * @return The {@code Set} of abilities the player has consumed.
     */
    public Set<String> getPlayerAbilities(Player player) {
        Set<String> abilities = new HashSet<>();

        for(String tag : player.getScoreboardTags()) {
            if (tag.startsWith(TOME_TAG_PREFIX)) {
                String abilityName = tag.substring(TOME_TAG_PREFIX.length());
                abilities.add(abilityName);
            }
        }

        return abilities;
    }

    /**
     * Remove the player's access to all tome abilities.
     *
     * @param player the player losing their abilities.
     */
    public void removeAllAbilities(Player player) {
        Set<String> tagsToRemove = new HashSet<>();

        for(String tag : player.getScoreboardTags()) {
            if (tag.startsWith(TOME_TAG_PREFIX)) {
                tagsToRemove.add(tag);
            }
        }

        for(String tag : tagsToRemove) {
            player.removeScoreboardTag(tag);
        }

        if (!tagsToRemove.isEmpty()) {
            this.plugin.logInfo("Removed " + tagsToRemove.size() + " tome abilities from " + player.getName() + " (converted to vampire)");
        }
    }

    /**
     * Attempt to use the tome ability.
     *
     * @param player the player who cast the ability.
     * @param abilityName the name of the ability.
     * @return {@code true} if the ability was successfully used.
     */
    public boolean useAbility(Player player, String abilityName) {
        if (!this.vampireManager.isHuman(player)) {
            player.sendMessage("§cOnly humans can use tome abilities.");
            return false;

        } else if (!this.hasAbility(player, abilityName)) {
            player.sendMessage("§cYou don't have access to the '" + abilityName + "' ability.");
            return false;

        } else {
            TomeAbility ability = this.getAbility(abilityName);

            if (ability == null) {
                player.sendMessage("§cAbility '" + abilityName + "' is not implemented.");
                return false;
            } else {
                return ability.use(player);
            }
        }
    }

    /**
     * Provide the admin with a GUI to grant or remove tome abilities from the target.
     *
     * @param admin the player selecting tome abilities.
     * @param target the player receiving tome abilities.
     */
    public void openTomeSelectionGUI(Player admin, Player target) {
        this.tomeSelectionTargets.put(admin.getUniqueId(), target.getUniqueId());
        Inventory gui = Bukkit.createInventory(null, 54, TOME_SELECTION_GUI_TITLE);
        List<String> abilityNames = new ArrayList<>(this.abilities.keySet());
        abilityNames.sort(String::compareToIgnoreCase);
        int slot = 0;

        for(String abilityName : abilityNames) {
            if (slot >= 54) {
                break;
            }

            TomeAbility ability = this.abilities.get(abilityName);
            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            ItemMeta meta = book.getItemMeta();

            if (meta != null) {
                String displayName = this.formatAbilityName(abilityName);
                boolean hasAbility = this.hasAbility(target, abilityName);

                if (hasAbility) {
                    meta.setDisplayName("§a✓ " + displayName + " §7(Already has)");
                } else {
                    meta.setDisplayName("§e" + displayName);
                }

                List<String> lore = new ArrayList<>();
                if (ability != null) {
                    for(String line : ability.getDescriptionLines()) {
                        lore.add("§7" + line);
                    }
                }

                lore.add("");
                if (hasAbility) {
                    lore.add("§cClick to remove from " + target.getName());
                } else {
                    lore.add("§eClick to grant to " + target.getName());
                }

                meta.setLore(lore);
                book.setItemMeta(meta);
            }

            gui.setItem(slot, book);
            ++slot;
        }

        this.addCureBookToGUI(gui, target, 45, "CureBook1Read", "§5Cure Book 1", this.plugin.getCureBookManager().getCureBookName(1, true));
        this.addCureBookToGUI(gui, target, 46, "CureBook2Read", "§5Cure Book 2", this.plugin.getCureBookManager().getCureBookName(2, true));
        this.addCureBookToGUI(gui, target, 47, "CureBook3Read", "§5Cure Book 3", this.plugin.getCureBookManager().getCureBookName(3, true));
        this.addCureBookToGUI(gui, target, 48, "CureBook4Read", "§5Cure Book 4", this.plugin.getCureBookManager().getCureBookName(4, true));

        admin.openInventory(gui);
        admin.sendMessage("§6Select tome abilities to grant to §e" + target.getName());
    }

    /**
     * Add the provided tome book to a GUI menu.
     *
     * @param gui the interactive menu.
     * @param target the player being given the cure book.
     * @param slot the desired inventory slot in the menu.
     * @param tag the cure book's tag.
     * @param displayName the cure book's title in the GUI menu.
     * @param bookTitle the cure book's lore title.
     */
    private void addCureBookToGUI(Inventory gui, Player target, int slot, String tag, String displayName, String bookTitle) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta meta = book.getItemMeta();

        if (meta != null) {
            boolean hasTag = target.getScoreboardTags().contains(tag);

            if (hasTag) {
                meta.setDisplayName("§a✓ " + displayName + " §7(Read)");
            } else {
                meta.setDisplayName(displayName);
            }

            List<String> lore = new ArrayList<>();
            lore.add("§7" + bookTitle);
            lore.add("");

            if (hasTag) {
                lore.add("§cClick to remove cure book tag from " + target.getName());
            } else {
                lore.add("§eClick to grant cure book tag to " + target.getName());
            }

            lore.add("§8[CURE_BOOK:" + tag + "]");
            meta.setLore(lore);
            book.setItemMeta(meta);
        }

        gui.setItem(slot, book);
    }

    /**
     * Format the ability's name into separated words and capitalization.
     *
     * @param abilityName the name of the ability.
     * @return The formatted ability name.
     */
    private String formatAbilityName(String abilityName) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for(int i = 0; i < abilityName.length(); ++i) {
            char c = abilityName.charAt(i);
            if (i > 0) {
                String remaining = abilityName.substring(i).toLowerCase();

                if (remaining.startsWith("of") || remaining.startsWith("the") || remaining.startsWith("word") || remaining.startsWith("eye") || remaining.startsWith("cry") || remaining.startsWith("barge") || remaining.startsWith("undead") || remaining.startsWith("direction") || remaining.startsWith("haste") || remaining.startsWith("land") || remaining.startsWith("lumberjack") || remaining.startsWith("prospector") || remaining.startsWith("bleeding") || remaining.startsWith("nails") || remaining.startsWith("stand") || remaining.startsWith("thrash") || remaining.startsWith("faith")) {
                    result.append(" ");
                    capitalizeNext = true;
                }
            }

            if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;

            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Retrieve the ID of the player being given tome abilities through the tome selection menu.
     *
     * @param adminUUID the UUID of the admin selecting tomes.
     * @return the UUID of the player being granted a tome by the admin's tome selection panel.
     */
    public UUID getTomeSelectionTarget(UUID adminUUID) {
        return this.tomeSelectionTargets.get(adminUUID);
    }

    /**
     * Remove a player from the list of those being gifted tome abilities.
     *
     * @param adminUUID the UUID of the admin selecting tomes.
     */
    public void removeTomeSelectionTarget(UUID adminUUID) {
        this.tomeSelectionTargets.remove(adminUUID);
    }

    /**
     * Give the player the tome ability.
     *
     * @param player the player being given the ability.
     * @param abilityName the name of the ability.
     */
    public void forceGrantAbility(Player player, String abilityName) {
        String normalizedName = abilityName.toLowerCase();

        if (this.isValidAbility(normalizedName)) {
            String tag = TOME_TAG_PREFIX + normalizedName;
            player.addScoreboardTag(tag);
            this.plugin.logInfo("Admin force-granted tome ability '" + normalizedName + "' to player " + player.getName());
        }
    }

    /**
     * Remove the tome ability from the player.
     *
     * @param player the player being stripped of the ability.
     * @param abilityName the name of the ability.
     */
    public void removeAbility(Player player, String abilityName) {
        String normalizedName = abilityName.toLowerCase();
        String tag = TOME_TAG_PREFIX + normalizedName;
        player.removeScoreboardTag(tag);
        this.plugin.logInfo("Admin removed tome ability '" + normalizedName + "' from player " + player.getName());
    }

    /**
     * Retrieve a list of the tome abilities humans can acquire.
     *
     * @return a {@code Set} containing the name of all tome abilities.
     */
    public Set<String> getAllAbilityNames() {
        return new HashSet<>(this.abilities.keySet());
    }

    /**
     * Clean up the tome ability records before shutting down the manager.
     */
    public void shutdown() {
        TomeAbility shoulderBarge = this.getAbility("shoulderbarge");
        if (shoulderBarge instanceof ShoulderBargeTomeAbility) {
            ((ShoulderBargeTomeAbility)shoulderBarge).cleanup();
        }

        TomeAbility holyWord = this.getAbility("holyword");
        if (holyWord instanceof HolyWordTomeAbility) {
            ((HolyWordTomeAbility)holyWord).cleanup();
        }

        TomeAbility lumberjack = this.getAbility("wayofthelumberjack");
        if (lumberjack instanceof WayOfTheLumberjackTomeAbility) {
            ((WayOfTheLumberjackTomeAbility)lumberjack).cleanup();
        }

        TomeAbility stopTheBleeding = this.getAbility("stopthebleeding");
        if (stopTheBleeding instanceof StopTheBleedingTomeAbility) {
            ((StopTheBleedingTomeAbility)stopTheBleeding).cleanup();
        }

        TomeAbility.cancelAllNotificationTasks();
        this.playerTomeUsageSession.clear();
        this.plugin.logInfo("TomeManager shutdown complete");
    }

    /**
     * Determine if the player used a tome ability book in the current session.
     *
     * @param player the player being checked.
     * @return {@code true} if the player has absorbed an ability this session.
     */
    private boolean hasUsedTomeThisSession(Player player) {
        UUID playerUUID = player.getUniqueId();

        if (!this.playerTomeUsageSession.containsKey(playerUUID)) {
            return false;
        } else {
            int currentSessionId = this.plugin.getSessionManager().getSessionIDObjective().getScore("session_id_holder").getScore();
            int tomeUsageSessionId = this.playerTomeUsageSession.get(playerUUID);
            return tomeUsageSessionId == currentSessionId;
        }
    }
}
