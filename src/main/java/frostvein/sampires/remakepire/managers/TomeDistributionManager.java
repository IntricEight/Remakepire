package frostvein.sampires.remakepire.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.scheduler.BukkitTask;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.abilities.tome.TomeAbility;
import frostvein.sampires.remakepire.commands.BrigadierCommands;
import frostvein.sampires.remakepire.utils.ConversionAssistant;

public class TomeDistributionManager {
    private final RemakepirePlugin plugin;
    private final ConfigManager configManager;
    private final Random random;
    private BukkitTask distributionTask;
    private final ConversionAssistant conversionAssistant;
    private int distributionCount;
    private List<Location> tomeLocations = new ArrayList<>();
    private final String[] tomeTypes;
    private final Enchantment[] enchantmentTypes;

    // The tome ability books and enchantments allowed to spawn inside the chests
    private static final Set<String> ALLOWED_TOMES = new HashSet<>(BrigadierCommands.TOME_ABILITIES);
    private static final Map<String, Enchantment> ENCHANTMENT_OPTIONS = Map.of(
            "Efficiency", Enchantment.EFFICIENCY,
            "FeatherFalling", Enchantment.FEATHER_FALLING,
            "Knockback", Enchantment.KNOCKBACK,
            "Mending", Enchantment.MENDING,
            "Power", Enchantment.POWER,
            "Protection", Enchantment.PROTECTION,
            "Punch",  Enchantment.PUNCH,
            "Respiration", Enchantment.RESPIRATION,
            "Sharpness", Enchantment.SHARPNESS,
            "SweepingEdge", Enchantment.SWEEPING_EDGE
    );

    /**
     * Create an instance of the Armor Storage manager.
     *
     * @param plugin the host plugin object.
     */
    public TomeDistributionManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.random = new Random();
        this.conversionAssistant = new ConversionAssistant();
        this.initializeTomeLocations();

        // Set what books can be found within the tome chests
        tomeTypes = this.loadTomeAbilityOptions();
        enchantmentTypes = this.loadEnchantmentBookOptions();

        // Set the number of chests which should contain tome ability books each cycle
        distributionCount = configManager.getAbilityDistributionCount();
    }

    /**
     * Retrieve the list of tome ability books that can spawn inside tome chests.
     *
     * @return A {@code String[]} of ability names that correlated to tome abilities.
     */
    private String[] loadTomeAbilityOptions() {
        List<String> options = this.configManager.getTomeAbilityOptions();

        // Remove items from the list if they don't match an existing tome book
        options.removeIf(tome -> !ALLOWED_TOMES.contains(tome));

        return options.toArray(new String[0]);
    }

    /**
     * Retrieve the list of enchantment books that can spawn inside tome chests.
     *
     * @return A {@code Enchantment[]} of enchantments that should be spawned inside tome chests.
     */
    private Enchantment[] loadEnchantmentBookOptions() {
        List<String> options = this.configManager.getTomeEnchantmentOptions();

        // Add each enchantment book to the list if it is found within the provided config list
        List<Enchantment> books = options.stream()
                .map(ENCHANTMENT_OPTIONS::get)
                .filter(Objects::nonNull)
                .toList();

        return books.toArray(new Enchantment[0]);
    }

    /**
     * Load the list of tome chest locations in from the config.
     */
    private void initializeTomeLocations() {
        this.tomeLocations = this.configManager.getTomeChestLocations();

        if (this.tomeLocations.isEmpty()) {
            this.plugin.getLogger().warning("TomeDistributionManager: No tome locations found in config!");
        } else {
            this.plugin.logInfo("TomeDistributionManager: Loaded " + this.tomeLocations.size() + " tome locations from config");
        }
    }

    /**
     * Begin distributing tomes to the chest locations each time a full day passes.
     */
    public void startDistributionTask() {
        long intervalTicks = this.configManager.getTomeDistributionIntervalTicks();

        this.stopDistributionTask();
        this.distributionTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::distributeTomes, 0L, intervalTicks);

        this.plugin.logInfo("TomeDistributionManager: Started daily tome distribution task");
    }

    /**
     * Stop distributing tomes to the chest locations.
     */
    public void stopDistributionTask() {
        if (this.distributionTask != null) {
            this.distributionTask.cancel();
            this.distributionTask = null;

            this.plugin.logInfo("TomeDistributionManager: Stopped tome distribution task");
        }
    }

    /**
     * Distribute a random (weighted) tome, cure, or enchanted book to each tome location.
     */
    public void distributeTomes() {
        if (this.plugin.getSessionManager().getSessionState() == SessionManager.IN_SESSION) {
            if (this.tomeLocations.isEmpty()) {
                this.plugin.getLogger().warning("TomeDistributionManager: No tome locations available for distribution");
            } else {
                this.clearAllTomeChests();
                List<Location> tomeSelectedLocations = this.selectRandomLocations(this.distributionCount);

                for (Location location : tomeSelectedLocations) {
                    String randomTome = this.getRandomTomeType();
                    this.distributeTomeToLocation(location, randomTome);
                }

                // Spawn enchanted books at any location where a tome book was not spawned
                List<Location> emptyLocations = new ArrayList<>(this.tomeLocations);
                emptyLocations.removeAll(tomeSelectedLocations);

                for (Location location : emptyLocations) {
                    this.addEnchantmentBookToLocation(location);
                }

                boolean cureBooksEnabled = this.plugin.getSessionManager().isCureBooksEnabled();
                double cureBooksSpawnChance = this.configManager.getCureBooksSpawnChance();
                boolean cureBookAdded = false;

                if (cureBooksEnabled && this.random.nextDouble() < cureBooksSpawnChance) {
                    Location randomLocation = this.tomeLocations.get(this.random.nextInt(this.tomeLocations.size()));
                    this.replaceCureBookAtLocation(randomLocation);
                    cureBookAdded = true;
                }

                this.plugin.logInfo("TomeDistributionManager: Distributed " + tomeSelectedLocations.size() + " tomes, " + emptyLocations.size() + " enchantment books" + (cureBookAdded ? ", and 1 cure book (replaced a chest)" : "") + " to chest locations");
            }
        }
    }

    /**
     * Clear the contents of all tome chests.
     */
    private void clearAllTomeChests() {
        for(Location location : this.tomeLocations) {
            Block block = location.getBlock();

            if (block.getType() == Material.CHEST) {
                Chest chest = (Chest)block.getState();
                chest.getInventory().clear();
            }
        }
    }

    /**
     * Shuffle the tome chest locations and trim the list down to the distribution count.
     *
     * @return A {@code List} of {@code distributionCount} tome chest locations.
     */
    private List<Location> selectRandomLocations(int numberOf) {
        List<Location> availableLocations = new ArrayList<>(this.tomeLocations);
        Collections.shuffle(availableLocations, this.random);

        return availableLocations.subList(0, Math.min(numberOf, availableLocations.size()));
    }

    /**
     * Retrieve a random tome ability from the list of abilities.
     *
     * @return The name of a tome ability.
     */
    private String getRandomTomeType() {
        return this.tomeTypes[this.random.nextInt(this.tomeTypes.length)];
    }

    /**
     * Add the tome ability book to the tome chest.
     *
     * @param location the tome chest location.
     * @param tomeType the tome ability name.
     */
    private void distributeTomeToLocation(Location location, String tomeType) {
        Block block = location.getBlock();

        if (block.getType() != Material.CHEST) {
            block.setType(Material.CHEST);
            this.plugin.logInfo("TomeDistributionManager: Created chest at " + this.conversionAssistant.locationToString(location));
        }

        Chest chest = (Chest)block.getState();
        chest.getInventory().addItem(this.createTomeItem(tomeType));

        this.plugin.logInfo("TomeDistributionManager: Added " + tomeType + " tome to chest at " + this.conversionAssistant.locationToString(location));
    }

    /**
     * Create the tome ability book for the tome ability.
     *
     * @param tomeType the tome ability name.
     * @return A book that will grant a human player the tome ability when used.
     */
    private ItemStack createTomeItem(String tomeType) {
        ItemStack tome = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta)tome.getItemMeta();

        if (bookMeta != null) {
            bookMeta.setTitle(tomeType);
            bookMeta.setAuthor("§6A source unknown...");
            TomeAbility ability = this.plugin.getTomeManager().getAbility(tomeType);

            if (ability != null) {
                List<String> lore = new ArrayList<>();
                String[] descriptionLines = ability.getDescriptionLines();

                for(String line : descriptionLines) {
                    lore.add("§7" + line);
                }

                lore.add("");
                lore.add("§eRight-click with this tome in hand to learn its secrets");
                bookMeta.setLore(lore);
            }

            List<String> pages = new ArrayList<>();
            StringBuilder pageContent = new StringBuilder();
            pageContent.append("§5§lANCIENT KNOWLEDGE§r\n\n");
            pageContent.append("§8The secrets of ").append(tomeType).append(" are contained within these pages.\n\n");

            if (ability != null) {
                String[] descriptionLines = ability.getDescriptionLines();

                for(String line : descriptionLines) {
                    pageContent.append("§7").append(line).append("\n");
                }
            } else {
                pageContent.append("§7No description available\n");
            }

            pageContent.append("\n§6Use this knowledge wisely, for it comes with great responsibility. ");
            pages.add(pageContent.toString());
            bookMeta.setPages(pages);
            tome.setItemMeta(bookMeta);
        }

        return tome;
    }

    /**
     * Retrieve a random enchantment book from the list of valid enchantments.
     *
     * @return An enchanted book.
     */
    private ItemStack createRandomEnchantmentBook() {
        ItemStack enchantedBook = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta)enchantedBook.getItemMeta();

        if (meta != null) {
            Enchantment randomEnchantment = this.enchantmentTypes[this.random.nextInt(this.enchantmentTypes.length)];
            int level = 1;
            meta.addStoredEnchant(randomEnchantment, level, true);
            enchantedBook.setItemMeta(meta);
        }

        return enchantedBook;
    }

    /**
     * Add a random enchantment book to the tome chest.
     *
     * @param location the tome chest location.
     */
    private void addEnchantmentBookToLocation(Location location) {
        Block block = location.getBlock();

        if (block.getType() != Material.CHEST) {
            block.setType(Material.CHEST);
            this.plugin.logInfo("TomeDistributionManager: Created chest at " + this.conversionAssistant.locationToString(location));
        }

        Chest chest = (Chest)block.getState();
        chest.getInventory().addItem(this.createRandomEnchantmentBook());

        this.plugin.logInfo("TomeDistributionManager: Added enchantment book to chest at " + this.conversionAssistant.locationToString(location));
    }

    /**
     * Replace the tome chest's content with one of the cure books.
     *
     * @param location the tome chest location.
     */
    private void replaceCureBookAtLocation(Location location) {
        Block block = location.getBlock();

        if (block.getType() != Material.CHEST) {
            block.setType(Material.CHEST);
            this.plugin.logInfo("TomeDistributionManager: Created chest at " + this.conversionAssistant.locationToString(location));
        }

        Chest chest = (Chest)block.getState();
        Inventory chestInventory = chest.getInventory();
        chestInventory.clear();
        ItemStack cureBook = this.createRandomCureBook();
        chestInventory.addItem(cureBook);

        this.plugin.logInfo("TomeDistributionManager: Replaced chest contents with cure book (" + cureBook.getItemMeta().getDisplayName() + ") at " + this.conversionAssistant.locationToString(location));
    }

    /**
     * Create one of the three basic cure books.
     *
     * @return The cure book 1/3, 2/3, or 3/3.
     */
    private ItemStack createRandomCureBook() {
        return this.plugin.getCureBookManager().getCureBook(this.random.nextInt(3) + 1);
    }

    /**
     * Retrieve the number of tome ability books that will be spawned.
     *
     * @return The number of tome chests to fill with ability books.
     */
    public int getDistributionCount() {
        return this.distributionCount;
    }

    /**
     * Set the number of tome chests to fill with ability books.
     *
     * @param count the number of tome ability books to spawn in the tome chests.
     */
    public void setDistributionCount(int count) {
        this.distributionCount = Math.max(1, Math.min(count, this.tomeLocations.size()));
        this.plugin.logInfo("TomeDistributionManager: Distribution count set to " + this.distributionCount);
    }

    /**
     * Add a new tome chest.
     *
     * @param location the new tome chest's location.
     * @return {@code true} if the new tome chest was successfully added.
     */
    public boolean addTomeLocation(Location location) {
        if (this.configManager.addTomeChestLocation(location)) {
            this.tomeLocations = this.configManager.getTomeChestLocations();
            this.plugin.logInfo("TomeDistributionManager: Added tome location at " + this.conversionAssistant.locationToString(location));
            return true;

        } else {
            this.plugin.getLogger().warning("TomeDistributionManager: Location " + this.conversionAssistant.locationToString(location) + " already exists in config");
            return false;
        }
    }

    /**
     * Remove a tome chest.
     *
     * @param location the tome chest's location.
     * @return {@code true} if the tome chest was successfully removed.
     */
    public boolean removeTomeLocation(Location location) {
        if (this.configManager.removeTomeChestLocation(location)) {
            this.tomeLocations = this.configManager.getTomeChestLocations();
            this.plugin.logInfo("TomeDistributionManager: Removed tome location at " + this.conversionAssistant.locationToString(location));
            return true;

        } else {
            this.plugin.getLogger().warning("TomeDistributionManager: Location " + this.conversionAssistant.locationToString(location) + " not found in config");
            return false;
        }
    }

    /**
     * Retrieve the tome chest locations.
     *
     * @return A {@code List} of locations.
     */
    public List<Location> getTomeLocations() {
        return new ArrayList<>(this.tomeLocations);
    }

    /**
     * Generate the tome, enchantment, and cure books into the tome chests.
     */
    public void triggerDistribution() {
        this.distributeTomes();
    }

    public void shutdown() {
        if (this.distributionTask != null) {
            this.distributionTask.cancel();
            this.distributionTask = null;
        }

        this.plugin.logInfo("TomeDistributionManager: Shutdown complete");
    }
}
