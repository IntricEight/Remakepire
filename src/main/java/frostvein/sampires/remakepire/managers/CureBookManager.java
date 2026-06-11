package frostvein.sampires.remakepire.managers;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.listeners.CureBookReadingListener;

public class CureBookManager {
    private final RemakepirePlugin plugin;
    private final FileConfiguration textConfig;
    private final boolean CUSTOM_BOOKS, CUSTOM_MESSAGES;

    /**
     * Create an instance of the Cure Book manager.
     *
     * @param plugin the host plugin object.
     */
    public CureBookManager(RemakepirePlugin plugin) {
        this.plugin = plugin;

        // Get the custom config file, which requires a different method than the default config file
        this.textConfig = this.plugin.getTextConfig();

        // Determine whether we are using custom features or not
        this.CUSTOM_BOOKS = this.textConfig.getBoolean("custom-cure-books", false);
        this.CUSTOM_MESSAGES = this.textConfig.getBoolean("custom-cure-book-messages", false);
    }

    /**
     * Retrieve a cure book using its sequence number.
     *
     * @param bookNumber the cure book's order in the sequence.
     * @return The cure book.
     */
    public ItemStack getCureBook(int bookNumber) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        book.setItemMeta(this.getCureBookMeta(bookNumber));

        return book;
    }

    /**
     * Retrieve the data of a cure book using its sequence number.
     *
     * @param bookNumber the cure book's order in the sequence.
     * @return The {@code BookMeta} of the cure book.
     */
    private BookMeta getCureBookMeta(int bookNumber) {
        BookMeta bookMeta = (BookMeta)Bukkit.getItemFactory().getItemMeta(Material.WRITTEN_BOOK);

        // Fill out the contents of the book
        bookMeta.setTitle(this.getCureBookName(bookNumber, true));
        bookMeta.setAuthor(this.getCureBookAuthor(bookNumber));
        bookMeta.setPages(this.getCureBookPages(bookNumber));
        bookMeta.setLore(this.getCureBookLore(bookNumber));
        CureBookReadingListener.markAsAuthenticCureBook(bookMeta, bookNumber, this.plugin);

        return bookMeta;
    }

    /**
     * Retrieve the name of the cure book.
     *
     * @param bookNumber the cure book's order in the sequence.
     * @param itemName whether to return the book's name or the item's name.<br/>
     *                 (Example: "The Remedy" vs "The Remedy 1/3").<br/>
     *                 {@code true} returns the item's name.
     * @return The cure book's name.
     */
    public String getCureBookName(int bookNumber, boolean itemName) {
        if (itemName && CUSTOM_BOOKS) {
            // The item name of the custom cure books
            String bookName = this.textConfig.getString("cure-book-" + bookNumber + ".item-title", "ERROR could not find item title");
            return bookName.substring(0, Math.min(bookName.length(), 32));

        } else if (CUSTOM_BOOKS) {
            // The book name of the custom cure book
            return this.textConfig.getString("cure-book-" + bookNumber + ".title", "ERROR could not find title");

        } else if (itemName) {
            // The item name of the default cure books
            return switch (bookNumber) {
                case 1 -> "The Remedy 1/3";
                case 2 -> "The Cure 2/3";
                case 3 -> "The Absolution 3/3";
                case 4 -> "The Retribution 4/3";
                default -> "ERROR in title";
            };
        } else {
            // The book name of the default cure books
            return switch (bookNumber) {
                case 1 -> "The Remedy";
                case 2 -> "The Cure";
                case 3 -> "The Absolution";
                case 4 -> "The Retribution";
                default -> "ERROR in title";
            };
        }
    }

    /**
     * Retrieve the author of the cure book.
     *
     * @param bookNumber the cure book's order in the sequence.
     * @return The cure book's author.
     */
    public String getCureBookAuthor(int bookNumber) {
        if (CUSTOM_BOOKS) {
            // The author of the custom cure book
            return this.textConfig.getString("cure-book-" + bookNumber + ".author", "ERROR could not find author");

        } else {
            return switch (bookNumber) {
                case 1, 2, 3 -> "§5An ancient scholar";
                case 4 -> "§4A vengeful spirit";
                default -> "ERROR in author default name";
            };
        }
    }

    /**
     * Retrieve the pages of the cure book.
     *
     * @param bookNumber the cure book's order in the sequence.
     * @return An {@code ArrayList} of page contents in the book.
     */
    private List<String> getCureBookPages(int bookNumber) {
        List<String> pages = new ArrayList<>();

        if (CUSTOM_BOOKS) {
            pages = this.textConfig.getStringList("cure-book-" + bookNumber + ".pages");
        } else {
            switch (bookNumber) {
                case 1:
                    pages.add("§5§lTHE REMEDY§r\n§8Part I of III\n\n§7In the darkest hours, when the cursed blood burns within your veins, know that salvation exists.\n\n§7The ancients spoke of a trinity of knowledge...");
                    pages.add("§7...that when combined, can sever the unholy bond between mortal and monster.\n\n§7This first piece reveals the place of redemption: beneath the light of a holy beacon, where divine grace gathers.\n\n§8Read on, seeker of the light...");
                    break;
                case 2:
                    pages.add("§5§lTHE CURE§r\n§8Part II of III\n\n§7The second fragment reveals the nature of the curse itself.\n\n§7Born of darkness, sustained by blood, the vampire's existence is a perversion of nature's order...");
                    pages.add("§7...yet within this perversion lies the key to its undoing.\n\n§7Holy water, blessed by the righteous, weakens the bond. Yet the bloodline binds; Your sire must lie dead for the cure to hold.\n\n§8Continue your search, truth-seeker...");
                    break;
                case 3:
                    pages.add("§5§lTHE ABSOLUTION§r\n§8Part III of III\n\n§7The final piece completes the trinity.\n\n§7With all three fragments of knowledge, the words of power are revealed:\n\n§6voluntate-mea-hoc-nefandum-vinculum-abicio");
                    pages.add("§7These words have no power in shadow — only beneath the light of day may they bind.\n\n§7Recall what the other tomes have taught you, gather what is needed, and speak.\n\n§8May the light guide your path.");
                    break;
                case 4:
                    pages.add("§4§lTHE RETRIBUTION§r\n§8The Fourth Tome\n\n§7This knowledge was never meant to be found.\n\n§7While the trinity speaks of self-salvation, this tome reveals darker words - words of forced redemption...");
                    pages.add("§7...or forced damnation.\n\n§4hoc-vinculum-tibi-dirumpo-mala-creatura\n\n§7With these words, you may force the choice upon another creature of the night.\n\n§8Use this power wisely, for it carries great consequence.");
                    break;
                default:
                    break;
            }
        }

        return pages;
    }

    /**
     * Retrieve the item description of the cure book.
     *
     * @param bookNumber the cure book's order in the sequence.
     * @return An {@code ArrayList} of lines to go into the lore field.
     */
    private List<String> getCureBookLore(int bookNumber) {
        List<String> lore = new ArrayList<>();

        if (CUSTOM_BOOKS) {
            lore = this.textConfig.getStringList("cure-book-" + bookNumber + ".item-details");
        } else {
            lore.add("§5An ancient tome of forbidden knowledge");
            lore.add("§7Part " + bookNumber + " of the cure series");
            lore.add("");
            lore.add("§eRead this book to absorb its wisdom");
        }

        return lore;
    }

    /**
     * Retrieve the placeholder text that fills the book before the player has completed the cure trinity.
     *
     * @return An {@code ArrayList} of pages to fill the book's placeholder pages.
     */
    public List<String> getCureBook4UnreadablePages() {
        List<String> pages = new ArrayList<>();

        if (CUSTOM_BOOKS) {
            pages = this.textConfig.getStringList("cure-book-4.unreadable-pages");
        } else {
            pages.add("§8§oThe words within this tome are beyond your comprehension...\n\n§7Perhaps you must first complete the Trinity of Restoration.");
        }

        return pages;
    }

    /**
     * Retrieve a message to inform the reader that they have learnt the knowledge within the cure book.
     *
     * @param bookNumber the cure book's order in the sequence.
     * @return The message to send to the cure book's reader.
     */
    public String[] getCureBookReadMessage(int bookNumber) {
        List<String> messages = new ArrayList<>();

        if (CUSTOM_MESSAGES) {
            // Retrieve the custom message from reading the cure book
            messages = this.textConfig.getStringList("cure-book-messages.cure-book-" + bookNumber +"-read");
        } else {
            messages.add("§8§o[You absorb the ancient knowledge within " + this.getCureBookName(bookNumber, false) + "...]");
        }

        return messages.toArray(new String[0]);
    }

    /**
     * Retrieve a message to inform the reader that thy lack the necessary knowledge to read this cure book.
     *
     * @return The message to send to the cure book's attempted reader.
     */
    public String[] getCureBook4UnreadableMessage() {
        List<String> messages = new ArrayList<>();

        if (CUSTOM_MESSAGES) {
            messages = this.textConfig.getStringList("cure-book-messages.cure-book-4-unreadable");
        } else {
            messages.add("§8§o[The words within this tome are beyond your comprehension... Perhaps you must first complete the Trinity of Restoration.]");
        }

        return messages.toArray(new String[0]);
    }


    /**
     * Inform the reader that they have completed the cure trinity and can now cure themselves of vampirism.
     *
     * @return The message to send to the cure book's reader.
     */
    public String[] getSelfCureLearntMessage() {
        List<String> messages = new ArrayList<>();

        if (CUSTOM_MESSAGES) {
            messages = this.textConfig.getStringList("cure-book-messages.willing-cure-learnt");
        } else {
            messages.add("");
            messages.add("§6§lTRINITY OF RESTORATION COMPLETE.");
            messages.add("§eYou have absorbed the knowledge from all three cure books. You now know the words to cure yourself of vampirism:");
            messages.add("§7/§6voluntate-mea-hoc-nefandum-vinculum-abicio");
            messages.add("§7Stand near a holy beacon, with a bottle of holy water on your person, and in the light of day, say those words, and be free.");
            messages.add("");
        }

        return messages.toArray(new String[0]);
    }

    /**
     * Inform the reader that they have discovered the force cure and can now cure others of vampirism.
     *
     * @return The message to send to the cure book's reader.
     */
    public String[] getForceCureLearntMessage() {
        List<String> messages = new ArrayList<>();

        if (CUSTOM_MESSAGES) {
            messages = this.textConfig.getStringList("cure-book-messages.force-cure-learnt");
        } else {
            messages.add("");
            messages.add("§4§lWORDS OF RETRIBUTION LEARNED.");
            messages.add("§cYou have learned the vengeful words to force cure others:");
            messages.add("§7/§4hoc-vinculum-tibi-dirumpo-mala-creatura §7<§4player§7>");
            messages.add("§7You and the creature must be within range of a holy beacon, hold a holy water on your person, and in the light of day, say those words, and give them the final choice.");
            messages.add("");
        }

        return messages.toArray(new String[0]);
    }
}