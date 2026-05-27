package frostvein.sampires.remakepire.managers;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.listeners.CureBookReadingListener;

public class CureBookManager {
    private final RemakepirePlugin plugin;
    private final ConfigManager configManager;

    // Remove this if a separate file for book text gets implemented
    private static final boolean CUSTOM_BOOKS = false;

    /**
     * Create an instance of the Cure Book manager.
     *
     * @param plugin the host plugin object.
     */
    public CureBookManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();

        // TODO Get whether we are using custom books or not
    }

    /**
     * Retrieve a cure book using its sequence number.
     *
     * @param bookNumber the cure book's order in the sequence.
     * @return the cure book.
     */
    public ItemStack getCureBook(int bookNumber) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        book.setItemMeta(this.getCureBookMeta(bookNumber));

        return book;
    }


    /**
     *
     *
     * @return the message to send to the cure book's attempted reader.
     */
    public String[] getCureBook4UnreadableMessage() {
        List<String> messages = new ArrayList<>();

        if (CUSTOM_BOOKS) {

        } else {
            messages.add("§8§o[The words within this tome are beyond your comprehension... Perhaps you must first complete the Trinity of Restoration.]");
        }

        return messages.toArray(new String[0]);
    }

    /**
     * Retrieve the placeholder text that fills the book before the player has completed the cure trinity.
     *
     * @return a {@code String[]} of pages to fill the book's placeholder pages.
     */
    public String[] getCureBook4UnreadableText() {
        List<String> pages = new ArrayList<>();

        if (CUSTOM_BOOKS) {

        } else {
            pages.add("§8§oThe words within this tome are beyond your comprehension...\n\n§7Perhaps you must first complete the Trinity of Restoration.");
        }

        return pages.toArray(new String[0]);
    }



    /*
     * Add other public methods for retrieving the messages that are sent to the player when cure books are interacted with
     */

    /**
     * Inform the reader that they have learnt the knowledge within the cure book
     *
     * @param bookNumber the cure book's order in the sequence.
     * @return the message to send to the cure book's reader.
     */
    public String[] getCureBookReadMessage(int bookNumber) {
        List<String> messages = new ArrayList<>();

        if (CUSTOM_BOOKS) {
            switch (bookNumber) {
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    break;
                case 4:
                    break;
                default:
                    break;
            }
        } else {
            messages.add("§8§o[You absorb the ancient knowledge within " + bookNumber + "...]");
        }

        return messages.toArray(new String[0]);
    }

    /**
     * Inform the reader that they have completed the cure trinity and can now cure themselves of vampirism.
     *
     * @return the message to send to the cure book's reader.
     */
    public String[] getSelfCureLearntMessage() {
        List<String> messages = new ArrayList<>();

        if (CUSTOM_BOOKS) {

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
     * @return the message to send to the cure book's reader.
     */
    public String[] getForceCureLearntMessage() {
        List<String> messages = new ArrayList<>();

        if (CUSTOM_BOOKS) {

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

    /**
     * Retrieve the name of the cure book.
     *
     * @param bookNumber the cure book's order in the sequence.
     * @param itemName whether to return the book's name or the item's name.<br/>
     *                 (Example: "The Remedy" vs "The Remedy 1/3")
     * @return the cure book's name.
     */
    public String getCureBookName(int bookNumber, boolean itemName) {
        switch (bookNumber) {
            case 1:
                if (itemName && CUSTOM_BOOKS) {
                    // The item name of the custom cure book

                } else if (itemName) {
                    // The item name of the default first cure book
                    return "The Remedy 1/3";
                } else if (CUSTOM_BOOKS) {
                    // The book name of the custom cure book

                } else {
                    // The book name of the default first cure book
                    return "The Remedy";
                }
            case 2:
                if (itemName && CUSTOM_BOOKS) {
                    // The item name of the custom cure book

                } else if (itemName) {
                    // The item name of the default second cure book
                    return "The Cure 2/3";
                } else if (CUSTOM_BOOKS) {
                    // The book name of the custom cure book

                } else {
                    // The book name of the default second cure book
                    return "The Cure";
                }
            case 3:
                if (itemName && CUSTOM_BOOKS) {
                    // The item name of the custom cure book

                } else if (itemName) {
                    // The item name of the default third cure book
                    return "The Absolution 3/3";
                } else if (CUSTOM_BOOKS) {
                    // The book name of the custom cure book

                } else {
                    // The book name of the default third cure book
                    return "The Absolution";
                }
            case 4:
                if (itemName && CUSTOM_BOOKS) {
                    // The item name of the custom cure book

                } else if (itemName) {
                    // The item name of the default fourth cure book
                    return "The Retribution 4/3";
                } else if (CUSTOM_BOOKS) {
                    // The book name of the custom cure book

                } else {
                    // The book name of the default fourth cure book
                    return "The Retribution";
                }
            default:
                return "ERROR in title";
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
            // TODO Retrieve from config file. Give an option for each book's author
            return "";
        } else {
            return switch (bookNumber) {
                case 1, 2, 3 -> "§5An ancient scholar";
                case 4 -> "§4A vengeful spirit";
                default -> "ERROR in author";
            };
        }
    }










    /**
     * Retrieve the data of a cure book using its sequence number.
     *
     * @param bookNumber the cure book's order in the sequence.
     * @return the {@code BookMeta} of the cure book.
     */
    private BookMeta getCureBookMeta(int bookNumber) {
        return switch (bookNumber) {
            case 1 -> getCureBook1Meta();
            case 2 -> getCureBook2Meta();
            case 3 -> getCureBook3Meta();
            case 4 -> getCureBook4Meta();
            default -> null;
        };
    }

    /**
     * Retrieve the data of the first cure book.
     *
     * @return the {@code BookMeta} of the first cure book.
     */
    private BookMeta getCureBook1Meta() {
//        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
//        BookMeta bookMeta = (BookMeta)(new ItemStack(Material.WRITTEN_BOOK)).getItemMeta();
        BookMeta bookMeta = (BookMeta)Bukkit.getItemFactory().getItemMeta(Material.WRITTEN_BOOK);
        final int BOOK_NUMBER = 1;

        if (CUSTOM_BOOKS) {
            // 3 Fields for main book contents
            // Title: 1 line only
            // Author: 1 line only
            // Pages: Allow as many as they want, figure out how to allow them to make an array inside a yml file and bring that in here (Through a String[])



            // Lore format
            List<String> lore = new ArrayList<>();

            // Figure out if each line in lore needs to be added separately, or if I can reuse the String[] from above.
            // If each must be added individually, make a loop for adding that to the book lore




        } else {
            bookMeta.setTitle(this.getCureBookName(BOOK_NUMBER, true));
            bookMeta.setAuthor(this.getCureBookAuthor(BOOK_NUMBER));
            bookMeta.setPages(
                    "§5§lTHE REMEDY§r\n§8Part I of III\n\n§7In the darkest hours, when the cursed blood burns within your veins, know that salvation exists.\n\n§7The ancients spoke of a trinity of knowledge...",
                    "§7...that when combined, can sever the unholy bond between mortal and monster.\n\n§7This is the first piece of that forbidden wisdom.\n\n§8Read on, seeker of the light..."
            );

            List<String> lore = new ArrayList<>();
            lore.add("§5An ancient tome of forbidden knowledge");
            lore.add("§7Part 1 of the cure series");
            lore.add("");
            lore.add("§eRead this book to absorb its wisdom");

            bookMeta.setLore(lore);
            CureBookReadingListener.markAsAuthenticCureBook(bookMeta, BOOK_NUMBER, this.plugin);
        }

        return bookMeta;
    }

    /**
     * Retrieve the data of the second cure book.
     *
     * @return the {@code BookMeta} of the second cure book.
     */
    private BookMeta getCureBook2Meta() {
        BookMeta bookMeta = (BookMeta)Bukkit.getItemFactory().getItemMeta(Material.WRITTEN_BOOK);
        final int BOOK_NUMBER = 2;

        if (CUSTOM_BOOKS) {

        } else {
            bookMeta.setTitle(this.getCureBookName(BOOK_NUMBER, true));
            bookMeta.setAuthor(this.getCureBookAuthor(BOOK_NUMBER));
            bookMeta.setPages(
                    "§5§lTHE CURE§r\n§8Part II of III\n\n§7The second fragment reveals the nature of the curse itself.\n\n§7Born of darkness, sustained by blood, the vampire's existence is a perversion of nature's order...",
                    "§7...yet within this perversion lies the key to its undoing.\n\n§7Holy water, blessed by the righteous, weakens the bond.\n\n§8Continue your search, truth-seeker..."
            );

            List<String> lore = new ArrayList<>();
            lore.add("§5An ancient tome of forbidden knowledge");
            lore.add("§7Part 2 of the cure series");
            lore.add("");
            lore.add("§eRead this book to absorb its wisdom");

            bookMeta.setLore(lore);
            CureBookReadingListener.markAsAuthenticCureBook(bookMeta, BOOK_NUMBER, this.plugin);
        }

        return bookMeta;
    }

    /**
     * Retrieve the data of the third cure book.
     *
     * @return the {@code BookMeta} of the third cure book.
     */
    private BookMeta getCureBook3Meta() {
        BookMeta bookMeta = (BookMeta)Bukkit.getItemFactory().getItemMeta(Material.WRITTEN_BOOK);
        final int BOOK_NUMBER = 3;

        if (CUSTOM_BOOKS) {

        } else {
            bookMeta.setTitle(this.getCureBookName(BOOK_NUMBER, true));
            bookMeta.setAuthor(this.getCureBookAuthor(BOOK_NUMBER));
            bookMeta.setPages(
                    "§5§lTHE ABSOLUTION§r\n§8Part III of III\n\n§7The final piece completes the trinity.\n\n§7With all three fragments of knowledge, the words of power are revealed:\n\n§6voluntate-mea-hoc-nefandum-vinculum-abicio",
                    "§7Stand near a holy beacon, with holy water upon your person, beneath the light of day.\n\n§7Speak the words, and be free of the curse forevermore.\n\n§8May the light guide your path."
            );

            List<String> lore = new ArrayList<>();
            lore.add("§5An ancient tome of forbidden knowledge");
            lore.add("§7Part 3 of the cure series");
            lore.add("");
            lore.add("§eRead this book to absorb its wisdom");

            bookMeta.setLore(lore);
            CureBookReadingListener.markAsAuthenticCureBook(bookMeta, BOOK_NUMBER, this.plugin);
        }

        return bookMeta;
    }

    /**
     * Retrieve the data of the fourth cure book.
     *
     * @return the {@code BookMeta} of the fourth cure book.
     */
    private BookMeta getCureBook4Meta() {
        BookMeta bookMeta = (BookMeta)Bukkit.getItemFactory().getItemMeta(Material.WRITTEN_BOOK);
        final int BOOK_NUMBER = 4;

        if (CUSTOM_BOOKS) {

        } else {
            bookMeta.setTitle(this.getCureBookName(BOOK_NUMBER, true));
            bookMeta.setAuthor(this.getCureBookAuthor(BOOK_NUMBER));
            bookMeta.setPages(
                    "§4§lTHE RETRIBUTION§r\n§8The Fourth Tome\n\n§7This knowledge was never meant to be found.\n\n§7While the trinity speaks of self-salvation, this tome reveals darker words - words of forced redemption...",
                    "§7...or forced damnation.\n\n§4hoc-vinculum-tibi-dirumpo-mala-creatura\n\n§7With these words, you may force the choice upon another creature of the night.\n\n§8Use this power wisely, for it carries great consequence."
            );

            List<String> lore = new ArrayList<>();
            lore.add("§5An ancient tome of forbidden knowledge");
            lore.add("§7Part 4 of the cure series");
            lore.add("");
            lore.add("§eRead this book to absorb its wisdom");

            bookMeta.setLore(lore);
            CureBookReadingListener.markAsAuthenticCureBook(bookMeta, BOOK_NUMBER, this.plugin);
        }

        return bookMeta;
    }














}
