package frostvein.sampires.remakepire.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
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
    private final Map<String, TomeAbility> registeredAbilities;
    private final Map<UUID, Integer> playerTomeUsageSession = new HashMap<>();
    public static final String TOME_TAG_PREFIX = "tome_ability_";
    public static final String TOME_SELECTION_GUI_TITLE = "§6§lSelect Tome Abilities";
    private final Map<UUID, UUID> tomeSelectionTargets = new HashMap<>();

    /**
     * Create an instance of the Tome manager.
     *
     * @param plugin the host plugin object.
     */
    public TomeManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.registeredAbilities = new HashMap<>();
        this.registerTomeAbilities();
    }

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

        this.plugin.getLogger().info("TomeManager initialized - registered " + this.registeredAbilities.size() + " tome abilities");
    }

    public void registerAbility(TomeAbility ability) {
        this.registeredAbilities.put(ability.getName().toLowerCase(), ability);
        this.plugin.getLogger().info("Registered tome ability: " + ability.getName());
    }

    public TomeAbility getAbility(String abilityName) {
        return (TomeAbility)this.registeredAbilities.get(abilityName.toLowerCase());
    }

    public boolean isValidAbility(String abilityName) {
        return this.registeredAbilities.containsKey(abilityName.toLowerCase());
    }

    public boolean grantAbility(Player player, String abilityName) {
        if (!this.vampireManager.isHuman(player)) {
            return false;
        } else if (!this.isValidAbility(abilityName)) {
            return false;
        } else if (this.hasAbility(player, abilityName)) {
            return false;
        } else if (player.getGameMode() != GameMode.CREATIVE && this.hasUsedTomeThisSession(player)) {
            player.sendMessage("§cYou have already absorbed one tome this session. Your mind cannot handle more ancient knowledge.");
            return false;
        } else {
            String tag = TOME_TAG_PREFIX + abilityName.toLowerCase();
            player.addScoreboardTag(tag);

            if (player.getGameMode() != GameMode.CREATIVE) {
                int currentSessionId = this.plugin.getSessionManager().getSessionIDObjective().getScore("session_id_holder").getScore();
                this.playerTomeUsageSession.put(player.getUniqueId(), currentSessionId);
            }

            this.plugin.getLogger().info("Granted tome ability '" + abilityName + "' to player " + player.getName());
            return true;
        }
    }

    public boolean hasAbility(Player player, String abilityName) {
        String tag = TOME_TAG_PREFIX + abilityName.toLowerCase();
        return player.getScoreboardTags().contains(tag);
    }

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
            this.plugin.getLogger().info("Removed " + tagsToRemove.size() + " tome abilities from " + player.getName() + " (converted to vampire)");
        }
    }

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

    public void openTomeSelectionGUI(Player admin, Player target) {
        this.tomeSelectionTargets.put(admin.getUniqueId(), target.getUniqueId());
        Inventory gui = Bukkit.createInventory(null, 54, TOME_SELECTION_GUI_TITLE);
        List<String> abilityNames = new ArrayList<>(this.registeredAbilities.keySet());
        abilityNames.sort(String::compareToIgnoreCase);
        int slot = 0;

        for(String abilityName : abilityNames) {
            if (slot >= 54) {
                break;
            }

            TomeAbility ability = this.registeredAbilities.get(abilityName);
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

        this.addCureBookToGUI(gui, target, 45, "CureBook1Read", "§5Cure Book 1", "The Remedy 1/3");
        this.addCureBookToGUI(gui, target, 46, "CureBook2Read", "§5Cure Book 2", "The Cure 2/3");
        this.addCureBookToGUI(gui, target, 47, "CureBook3Read", "§5Cure Book 3", "The Absolution 3/3");
        this.addCureBookToGUI(gui, target, 48, "CureBook4Read", "§5Cure Book 4", "The Retribution 4/3");

        admin.openInventory(gui);
        admin.sendMessage("§6Select tome abilities to grant to §e" + target.getName());
    }

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

    public UUID getTomeSelectionTarget(UUID adminUUID) {
        return (UUID)this.tomeSelectionTargets.get(adminUUID);
    }

    public void removeTomeSelectionTarget(UUID adminUUID) {
        this.tomeSelectionTargets.remove(adminUUID);
    }

    public void forceGrantAbility(Player player, String abilityName) {
        String normalizedName = abilityName.toLowerCase();

        if (this.isValidAbility(normalizedName)) {
            String tag = TOME_TAG_PREFIX + normalizedName;
            player.addScoreboardTag(tag);
            this.plugin.getLogger().info("Admin force-granted tome ability '" + normalizedName + "' to player " + player.getName());
        }
    }

    public void removeAbility(Player player, String abilityName) {
        String normalizedName = abilityName.toLowerCase();
        String tag = TOME_TAG_PREFIX + normalizedName;
        player.removeScoreboardTag(tag);
        this.plugin.getLogger().info("Admin removed tome ability '" + normalizedName + "' from player " + player.getName());
    }

    public Set<String> getAllAbilityNames() {
        return new HashSet<>(this.registeredAbilities.keySet());
    }

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
        this.plugin.getLogger().info("TomeManager shutdown complete");
    }

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
