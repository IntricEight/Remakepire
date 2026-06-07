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
import frostvein.sampires.remakepire.utils.ConversionAssistant;

public class TomeDistributionManager {
    private final RemakepirePlugin plugin;
    private final ConfigManager configManager;
    private final Random random;
    private BukkitTask distributionTask;
    private final ConversionAssistant conversionAssistant;
    private int distributionCount = 4;
    private List<Location> tomeLocations = new ArrayList<>();
    private final String[] tomeTypes = new String[]{"BanishUndead", "Blessing", "EnlightenedEye", "HolyWord", "LanternThrash", "PrayerOfFaith", "RallyingCry", "ShoulderBarge", "TurnUndead", "UncannyDirection", "UnnaturalHaste", "WayOfTheLand", "WayOfTheLumberjack", "WayOfTheProspector", "StopTheBleeding"};
    private final Enchantment[] enchantmentTypes = new Enchantment[]{Enchantment.EFFICIENCY, Enchantment.PROTECTION, Enchantment.FEATHER_FALLING, Enchantment.KNOCKBACK, Enchantment.SWEEPING_EDGE};

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

            // Spawn enchanted books at any location where a tome book was not spawned
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

            this.plugin.logInfo("TomeDistributionManager: Distributed " + tomeSelectedLocations.size() + " tomes, " + emptyLocations.size() + " enchantment books" + (cureBookAdded ? ", and 1 cure book (replaced a chest)" : "") + " to chest locations");
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
        int bookChoice = this.random.nextInt(3), bookNumber = bookChoice + 1;
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta)book.getItemMeta();

        if (bookMeta != null) {
            switch (bookChoice) {
                case 0:
                    bookMeta.setTitle("A Study on Blood");    // The Remedy 1/3
                    bookMeta.setAuthor("§5Fernida Penfield");
                    bookMeta.setPages("§4§lA Study on Blood§r\n" +
                            "§0§lIntroduction§r\n" +
                            "Over the course of this book I, Fernida Penfield, shall enquire into the effects of Sanguine blood on Humans as well as other Creatures. I will focus on the fields of Sanguine Conjuration, Thralling, and Sanguine Biology.",
                            "§o[The chapter on Thralling seems to have been torn out]§r",
                            "§4§lChapter 2:§r§4\n" +
                            "§r§0Sanguine Conjuration§r\n\n" +
                            "Sanguine Conjuration is the act of transforming or altering living things via the insertion of Vampiric or Deific blood. As a general principal, experimentation into conjuration should be taken with extreme",
                            "caution as Sanguine creations tend to be larger, more aggressive, and stronger than their mundane variants. For this purpose I have created a series of cells for containing test subjects. I will now list a series of experiments I have conducted and their results.",
                            "EXPERIMENT LOG 1\n" +
                            "Subject: Common Dairy Cow.\n" +
                            "Result:\n" +
                            "The subject began to develop a remarkably human lower body, unfortunately its organs failed to develop properly and it perspired four hours after the introduction of Blood.",
                            "EXPERIMENT LOG 2\n" +
                            "Subject: Pig\n" +
                            "Result:\n" +
                            "The subject has been developing nicely growing largely, stronger and a touch more intelligent, strangely it's body shows signs of decay, although it is not dying. §7§oThis is the one.\nMeet me in the mines\nYou know the spot~ PS§r",
                            "§8§oAgreed, this will work nicely. I'll bring the Sulfates and zinc dust the book recommends. What's the plan once its ready? release it in the mines and run like hell? ~ LS §r\n§7§oPretty much. ~ PS§r",
                            "§o[The remaining experiment logs are either too damaged to read or uninteresting]§r",
                            "§4§lChapter 3:§r\n" +
                            "§0Sanguine Biology§r\n\n" +
                            "Regardless of whether they are a Vampire or a god, my tests has proven a definitive link between the blood of Sire and a fledgling.\n" +
                            "Introducing the blood of a sire to a fledgling seems to have a limited",
                            "reg ne ati e eff . I b   e e th s can in effec pre n t e   ing of a sang  ne bei altogether, quite curi inde  . in ad    n to     t  s ho y water is     al  st certainly key to \"c     \" san uine b  \n\n" +
                            "§o[The rest of the writing is completely illegible]§r");
                    break;
                case 1:
                    bookMeta.setTitle("Notes for Future Biographers");      // The Cure 2/3
                    bookMeta.setAuthor("§5Fernida Penfield");
                    bookMeta.setPages("§0§lNotes for Future Biographers§r§l\n" +
                            "§r§8By Fernida Penfield§r\n\n" +
                            "§o[You skip to the only entries relevant to your investigation\n" +
                            "The text is surprisingly disorganised, and oozes ego.]",
                            "",
                            "§r§8§lEntry 28, March 1st  1845§r\n" +
                            "Our relentless pursuit of the perfect sanguine creature, has exhausted our stores of zinc and sulphates, I'll have to make another trip to Wendell Jr. and see if he will fund some more in the next shipment. The paranoid fool, will be sure we meet in his",
                            "grandiose library, a treasure trove for those deluded enough to believe in his magicks and con-artistry. The mans wealth would be better funding better facilities for us, better than the charlatans he's been hiring to fix this \"curse\" of a late. Ginnethon S especially.",
                            "The man is creepy, and is far too influential, He corrupts Wendell more and more. I fear he could screw us all by offering to turn the blathering idiot. Sanguine blood may be useful, but it may be better sourced from those who think themselves deities, I was wrong to think",
                            "others might be more \"grounded\" or \"sane\".",
                            "§8§lEntry 29, March 3rd 1845§r\n" +
                            "A great success! Not only has Wendell has agreed to meet with me, but he has sworn to share with me the device he uses to mix alchemical ingredients. The thing must have cost thousands of dollars; a specialist for such a machine would not be easy to",
                            "find nor cheap. The Vortex, as he calls it, mixes Alchemical ingredients beyond the capabilities of any standard machine, intermeshing them in a way previously unachievable. It is essential for the preparation of many concoctions, such as the Sulphates I use for my research, in",
                            "addition to §o[illegible]§r and §o[illegible]§r. With this in my hands, I'll soon have no need of Wendell or his hanger on charlatans and magicians.\n" +
                            "\n- Fernida, Future Alchemical Legend.");
//                    bookMeta.setPages("§5§lTHE CURE§r\n§8Part II of III\n\n§7The second fragment reveals the nature of the curse itself.\n\n§7Born of darkness, sustained by blood, the vampire's existence is a perversion of nature's order...", "§7...yet within this perversion lies the key to its undoing.\n\n§7Holy water, blessed by the righteous, weakens the bond.\n\n§8Continue your search, truth-seeker...");
                    break;
                case 2:
                    bookMeta.setTitle("Reversing Vampirism");
                    bookMeta.setAuthor("§5Prior Sala Negahban");
                    bookMeta.setPages("§4§lReversing Vampirism§r§l\n" +
                                    "§r§oBy Prior Sala Negahban\n\n" +
                                    "§rSince their discovery by the church fathers in the 2nd century AD, the Vampire has been a perverse fascination of a subsect of clergymen. These clergymen can be",
                                    "divided into two camps: those who seek to eradicate the Vampire entirely, and those who seek to \"fix\" or \"cure\" them. While this has been the status quo for the last sixteen centuries, I believe I have discovered the foundations, at least, for a real cure for vampirism, a way to",
                                    "reverse their condition permanently. This has only become possible using recent developments in medical technology. As a scholar of occultism, medical science, and theology, I feel I am uniquely qualified to determine the exact recipe for this cure. The cure is to be delivered via",
                                    "injection, which is to say, via needle into the blood stream, as demonstrated by Francis Rynd. In this needle. lies our more occult and theological ingredients. The mixture must be of both Holy Water, and the blood of the one who turned the individual (archaically referred to as",
                                    "\"The Sire\") to be cured, referred to. While I have yet to find a device capable of mixing these reagents such that they do not react to one another, if such a thing should exist, I am certain that simply injecting a Vampire with this solution would reverse their Vampirism.\n",
                                    "§8§o[A note is scrawled in the margin]\n§r§8§oPerhaps we could use the Vortex Mixer we have for creating chemical mixtures? Don't think I have a Vampire to test it on though. At least not a willing one.§r§o\n\n" +
                                    "§rIt is worth remembering, however, that it is deeply",
                                    "unsafe to inject an individual unwillingly. Only those with the proper knowledge and training should attempt to forcefully cure someone using this method. Otherwise you risk hurting or even killing the vampire via medical malpractice.",
                                    "§8§o[Another note is scrawled in the margin]\n" +
                                    "§8§oI believe I left a book on the discovery of injection in the chamber below the witches hut.");
//                    bookMeta.setPages("§5§lTHE ABSOLUTION§r\n§8Part III of III\n\n§7The final piece completes the trinity.\n\n§7With all three fragments of knowledge, the words of power are revealed:\n\n§6voluntate-mea-hoc-nefandum-vinculum-abicio", "§7Stand near a holy beacon, with holy water upon your person, beneath the light of day.\n\n§7Speak the words, and be free of the curse forevermore.\n\n§8May the light guide your path.");
            }

            List<String> lore = new ArrayList<>();
            lore.add("§5An ancient tome of forbidden knowledge");
            lore.add("");

            bookMeta.setLore(lore);
            CureBookReadingListener.markAsAuthenticCureBook(bookMeta, bookNumber, this.plugin);
            book.setItemMeta(bookMeta);
        }

        return book;
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
