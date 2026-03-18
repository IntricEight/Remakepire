package frostvein.sampires.remakepire.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
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
import frostvein.sampires.remakepire.listeners.CureBookReadingListener;

public class TomeDistributionManager {
    private final RemakepirePlugin plugin;
    private final ConfigManager configManager;
    private final Random random;
    private BukkitTask distributionTask;
    private int distributionCount = 4;
    private List<Location> tomeLocations = new ArrayList<>();
    private final String[] tomeTypes = new String[]{"BanishUndead", "Blessing", "EnlightenedEye", "HolyWord", "LanternThrash", "PrayerOfFaith", "RallyingCry", "ShoulderBarge", "TurnUndead", "UncannyDirection", "UnnaturalHaste", "WayOfTheLand", "WayOfTheLumberjack", "WayOfTheProspector"};
    private final Enchantment[] enchantmentTypes = new Enchantment[]{Enchantment.EFFICIENCY, Enchantment.PROTECTION, Enchantment.FEATHER_FALLING, Enchantment.KNOCKBACK, Enchantment.SWEEPING_EDGE};

    /**
     * Create an instance of the Armor Storage manager.
     *
     * @param plugin the host plugin object.
     * @param configManager the manager for the config values.
     */
    public TomeDistributionManager(RemakepirePlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.random = new Random();
        this.initializeTomeLocations();
    }

    /**
     * Load the list of tome chest locations in from the config.
     */
    private void initializeTomeLocations() {
        this.tomeLocations = this.configManager.getTomeChestLocations();

        if (this.tomeLocations.isEmpty()) {
            this.plugin.getLogger().warning("TomeDistributionManager: No tome locations found in config!");
        } else {
            this.plugin.getLogger().info("TomeDistributionManager: Loaded " + this.tomeLocations.size() + " tome locations from config");
        }
    }

    /**
     * Begin distributing tomes to the chest locations each time a full day passes.
     */
    public void startDistributionTask() {
        long intervalTicks = this.configManager.getTomeDistributionIntervalTicks();

        this.stopDistributionTask();
        this.distributionTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::distributeTomes, 0L, intervalTicks);

        this.plugin.getLogger().info("TomeDistributionManager: Started daily tome distribution task");
    }

    /**
     * Stop distributing tomes to the chest locations.
     */
    public void stopDistributionTask() {
        if (this.distributionTask != null) {
            this.distributionTask.cancel();
            this.distributionTask = null;

            this.plugin.getLogger().info("TomeDistributionManager: Stopped tome distribution task");
        }
    }

    /**
     * Distribute a random (weighted) tome, cure, or enchanted book to each tome location.
     */
    public void distributeTomes() {
        if (this.plugin.getSessionManager().getSessionState() != SessionManager.IN_SESSION) {
            this.plugin.getLogger().warning("TomeDistributionManager: Tomes may not be distributed outside of session");
        } else if (this.tomeLocations.isEmpty()) {
            this.plugin.getLogger().warning("TomeDistributionManager: No tome locations available for distribution");
        } else {
            this.clearAllTomeChests();
            List<Location> tomeSelectedLocations = this.selectRandomLocations();

            for(Location location : tomeSelectedLocations) {
                String randomTome = this.getRandomTomeType();
                this.distributeTomeToLocation(location, randomTome);
            }

            List<Location> emptyLocations = new ArrayList<>(this.tomeLocations);
            emptyLocations.removeAll(tomeSelectedLocations);

            for(Location location : emptyLocations) {
                this.addEnchantmentBookToLocation(location);
            }

            boolean cureBooksEnabled = this.configManager.isCureBooksEnabled();
            double cureBooksSpawnChance = this.configManager.getCureBooksSpawnChance();
            boolean cureBookAdded = false;

            if (cureBooksEnabled && this.random.nextDouble() < cureBooksSpawnChance) {
                Location randomLocation = this.tomeLocations.get(this.random.nextInt(this.tomeLocations.size()));
                this.replaceCureBookAtLocation(randomLocation);
                cureBookAdded = true;
            }

            this.plugin.getLogger().info("TomeDistributionManager: Distributed " + tomeSelectedLocations.size() + " tomes, " + emptyLocations.size() + " enchantment books" + (cureBookAdded ? ", and 1 cure book (replaced a chest)" : "") + " to chest locations");
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
    private List<Location> selectRandomLocations() {
        List<Location> availableLocations = new ArrayList<>(this.tomeLocations);
        Collections.shuffle(availableLocations, this.random);
        int locationsToSelect = Math.min(this.distributionCount, availableLocations.size());
        return availableLocations.subList(0, locationsToSelect);
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
            this.plugin.getLogger().info("TomeDistributionManager: Created chest at " + this.locationToString(location));
        }

        Chest chest = (Chest)block.getState();
        chest.getInventory().addItem(this.createTomeItem(tomeType));

        this.plugin.getLogger().info("TomeDistributionManager: Added " + tomeType + " tome to chest at " + this.locationToString(location));
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

            pageContent.append("\n§6Use this knowledge wisely, for it comes with great responsibility.");
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
            this.plugin.getLogger().info("TomeDistributionManager: Created chest at " + this.locationToString(location));
        }

        Chest chest = (Chest)block.getState();
        chest.getInventory().addItem(this.createRandomEnchantmentBook());

        this.plugin.getLogger().info("TomeDistributionManager: Added enchantment book to chest at " + this.locationToString(location));
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
            this.plugin.getLogger().info("TomeDistributionManager: Created chest at " + this.locationToString(location));
        }

        Chest chest = (Chest)block.getState();
        Inventory chestInventory = chest.getInventory();
        chestInventory.clear();
        ItemStack cureBook = this.createRandomCureBook();
        chestInventory.addItem(cureBook);

        this.plugin.getLogger().info("TomeDistributionManager: Replaced chest contents with cure book (" + cureBook.getItemMeta().getDisplayName() + ") at " + this.locationToString(location));
    }

    /**
     * Create one of the three basic cure books.
     *
     * @return The cure book 1/3, 2/3, or 3/3.
     */
    private ItemStack createRandomCureBook() {
        int bookChoice = this.random.nextInt(3), bookNumber = bookChoice + 1;
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta)book.getItemMeta();

        if (bookMeta != null) {
            switch (bookChoice) {
                case 0:
                    bookMeta.setTitle("The Remedy 1/3");
                    bookMeta.setAuthor("§5An ancient scholar");
                    bookMeta.setPages("§5§lTHE REMEDY§r\n§8Part I of III\n\n§7In the darkest hours, when the cursed blood burns within your veins, know that salvation exists.\n\n§7The ancients spoke of a trinity of knowledge...", "§7...that when combined, can sever the unholy bond between mortal and monster.\n\n§7This is the first piece of that forbidden wisdom.\n\n§8Read on, seeker of the light...");
                    break;
                case 1:
                    bookMeta.setTitle("The Cure 2/3");
                    bookMeta.setAuthor("§5An ancient scholar");
                    bookMeta.setPages("§5§lTHE CURE§r\n§8Part II of III\n\n§7The second fragment reveals the nature of the curse itself.\n\n§7Born of darkness, sustained by blood, the vampire's existence is a perversion of nature's order...", "§7...yet within this perversion lies the key to its undoing.\n\n§7Holy water, blessed by the righteous, weakens the bond.\n\n§8Continue your search, truth-seeker...");
                    break;
                case 2:
                    bookMeta.setTitle("The Absolution 3/3");
                    bookMeta.setAuthor("§5An ancient scholar");
                    bookMeta.setPages("§5§lTHE ABSOLUTION§r\n§8Part III of III\n\n§7The final piece completes the trinity.\n\n§7With all three fragments of knowledge, the words of power are revealed:\n\n§6voluntate-mea-hoc-nefandum-vinculum-abicio", "§7Stand near a holy beacon, with holy water upon your person, beneath the light of day.\n\n§7Speak the words, and be free of the curse forevermore.\n\n§8May the light guide your path.");
            }

            List<String> lore = new ArrayList<>();
            lore.add("§5An ancient tome of forbidden knowledge");
            lore.add("§7Part " + bookNumber + " of the cure series");
            lore.add("");
            lore.add("§eRead this book to absorb its wisdom");

            bookMeta.setLore(lore);
            CureBookReadingListener.markAsAuthenticCureBook(bookMeta, bookNumber, this.plugin);
            book.setItemMeta(bookMeta);
        }

        return book;
    }

    /**
     * Convert a {@code Location} into a {@code String} format.
     *
     * @param location a location to convert.
     * @return A {@code String} of the location's coordinates.
     */
    private String locationToString(Location location) {
        return String.format("(%d, %d, %d)", location.getBlockX(), location.getBlockY(), location.getBlockZ());
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
        this.plugin.getLogger().info("TomeDistributionManager: Distribution count set to " + this.distributionCount);
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
            this.plugin.getLogger().info("TomeDistributionManager: Added tome location at " + this.locationToString(location));
            return true;

        } else {
            this.plugin.getLogger().warning("TomeDistributionManager: Location " + this.locationToString(location) + " already exists in config");
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
            this.plugin.getLogger().info("TomeDistributionManager: Removed tome location at " + this.locationToString(location));
            return true;

        } else {
            this.plugin.getLogger().warning("TomeDistributionManager: Location " + this.locationToString(location) + " not found in config");
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

        this.plugin.getLogger().info("TomeDistributionManager: Shutdown complete");
    }
}
