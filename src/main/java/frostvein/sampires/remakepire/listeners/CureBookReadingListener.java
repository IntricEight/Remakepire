package frostvein.sampires.remakepire.listeners;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class CureBookReadingListener implements Listener {
    private final RemakepirePlugin plugin;
    public static final String TAG_CURE_BOOK_1 = "CureBook1Read", TAG_CURE_BOOK_2 = "CureBook2Read", TAG_CURE_BOOK_3 = "CureBook3Read", TAG_CURE_BOOK_4 = "CureBook4Read";
    public static final String CURE_BOOK_KEY = "vampiresmp_cure_book";
    public static final int BOOK_NUM_REMEDY = 1, BOOK_NUM_CURE = 2, BOOK_NUM_ABSOLUTION = 3, BOOK_NUM_RETRIBUTION = 4;

    /**
     * Create an instance of the Cure Book Reading listener.
     *
     * @param plugin the host plugin object.
     */
    public CureBookReadingListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Retrieve the identifier key for the cure books.
     *
     * @return A {@code NamespacedKey} to retrieve cure books.
     */
    public NamespacedKey getCureBookKey() {
        return new NamespacedKey(this.plugin, CURE_BOOK_KEY);
    }

    /**
     * Nothing is written in this function.
     *
     * @param event a book is edited.
     */
    @EventHandler
    public void onBookEdit(PlayerEditBookEvent event) {}

    /**
     * Retrieve if the player has read the first three cure books.
     *
     * @param player the player whose cure book consumption is being checked.
     * @return {@code true} if the player has read cure books 1/3, 2/3, and 3/3.
     */
    public static boolean hasReadAllCureBooks(Player player) {
        return player.getScoreboardTags().contains(TAG_CURE_BOOK_1) && player.getScoreboardTags().contains(TAG_CURE_BOOK_2) && player.getScoreboardTags().contains(TAG_CURE_BOOK_3);
    }

    /**
     * Retrieve if the player has read the fourth cure book.
     *
     * @param player the player whose cure book consumption is being checked.
     * @return {@code true} if the player has read cure book 4/3.
     */
    public static boolean hasReadFourthBook(Player player) {
        return player.getScoreboardTags().contains(TAG_CURE_BOOK_4);
    }

    /**
     * Add the cure book to the players record, and check whether they have reached a milestone.
     *
     * @param player the player reading a cure book.
     * @param bookNumber the cure book's number.
     * @return {@code true} if a new cure book is being reading.
     */
    public boolean onCureBookRead(Player player, int bookNumber) {
        if (bookNumber >= 1 && bookNumber <= 4) {
            String tagToAdd = null, bookName = null;
            boolean isNewTag = false;

            switch (bookNumber) {
                case BOOK_NUM_REMEDY:
                    if (!player.getScoreboardTags().contains(TAG_CURE_BOOK_1)) {
                        tagToAdd = TAG_CURE_BOOK_1;
                        bookName = "A Study on Blood";
                        isNewTag = true;
                    }

                    break;
                case BOOK_NUM_CURE:
                    if (!player.getScoreboardTags().contains(TAG_CURE_BOOK_2)) {
                        tagToAdd = TAG_CURE_BOOK_2;
                        bookName = "Notes for Future Biographers";
                        isNewTag = true;
                    }

                    break;
                case BOOK_NUM_ABSOLUTION:
                    if (!player.getScoreboardTags().contains(TAG_CURE_BOOK_3)) {
                        tagToAdd = TAG_CURE_BOOK_3;
                        bookName = "Reversing Vampirism";
                        isNewTag = true;
                    }

                    break;
                case BOOK_NUM_RETRIBUTION:
                    if (!hasReadAllCureBooks(player)) {
                        player.sendMessage("§8§o[The words within this tome are beyond your comprehension... Perhaps other books hold the key to deciphering this journal]");
                        return false;
                    }

                    if (!player.getScoreboardTags().contains(TAG_CURE_BOOK_4)) {
                        tagToAdd = TAG_CURE_BOOK_4;
                        bookName = "On the Nature of Sanguinity";
                        isNewTag = true;
                    }

                    break;
                default:
                    return false;
            }

            if (tagToAdd != null && isNewTag) {
                player.addScoreboardTag(tagToAdd);

                // Send a custom message for each cure book
                if (tagToAdd.equals(TAG_CURE_BOOK_1)) {
                    player.sendMessage("§8§o[You feel as if you have found the first part of a greater mystery]");
                } else if (tagToAdd.equals(TAG_CURE_BOOK_2)) {
                    player.sendMessage("§8§o[You feel like you've found the second part of a greater mystery]");
                } else if  (tagToAdd.equals(TAG_CURE_BOOK_3)) {
                    player.sendMessage("§8§o[This feels like the third piece of a greater mystery, perhaps you should find the others]");
                } else {
                    // TODO: Add more areas for each tome as Cleo gives more specifications
                    player.sendMessage("§8§o[You have gained a key understanding of vampire physiology]");
                }

                this.plugin.logInfo("CURE BOOK READ: " + player.getName() + " read book #" + bookNumber);

                if (bookNumber != BOOK_NUM_RETRIBUTION && hasReadAllCureBooks(player)) {
                    player.sendMessage("");
                    player.sendMessage("§6§lPenfield Journals Discovered:");
                    player.sendMessage("§eWith these three books, you've determined the recipe for a cure to vampirism");
                    player.sendMessage("");

                    this.plugin.logInfo("CURE UNLOCKED: " + player.getName() + " has unlocked the /vol cure command");

                    if (!this.plugin.getConfig().getBoolean("fourth_book_spawn_enabled", false)) {
                        this.plugin.getConfig().set("fourth_book_spawn_enabled", true);
                        this.plugin.saveConfig();
                        this.plugin.logInfo("FOURTH BOOK ENABLED: " + player.getName() + " completed the Trinity, the fourth book can now spawn");
                    }
                }

                if (bookNumber == BOOK_NUM_RETRIBUTION && hasReadAllCureBooks(player)) {
                    player.sendMessage("");
                    player.sendMessage("§4§lGained a key understanding of vampire physiology");
                    player.sendMessage("§cWith this, you have have everything you need to forcibly cure a vampire");
                    player.sendMessage("");

                    this.plugin.logInfo("FORCE CURE UNLOCKED: " + player.getName() + " has unlocked the /hoc force cure command");
                }

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Retrieve the cure book's number.
     *
     * @param item an item to inspect.
     * @param plugin the host plugin object.
     * @return A cure book's number (out of 4).
     */
    public static int getAuthenticCureBookNumber(ItemStack item, RemakepirePlugin plugin) {
        if (item != null && item.getType() == Material.WRITTEN_BOOK) {
            if (item.hasItemMeta() && item.getItemMeta() instanceof BookMeta bookMeta) {
                PersistentDataContainer pdc = bookMeta.getPersistentDataContainer();
                NamespacedKey key = new NamespacedKey(plugin, CURE_BOOK_KEY);

                return pdc.has(key, PersistentDataType.INTEGER) ? pdc.get(key, PersistentDataType.INTEGER) : 0;
            }
        }

        return 0;
    }

    /**
     * Validate the cure book.
     *
     * @param meta the meta info on the book item.
     * @param bookNumber the book's number in the series.
     * @param plugin the host plugin object.
     */
    public static void markAsAuthenticCureBook(BookMeta meta, int bookNumber, RemakepirePlugin plugin) {
        NamespacedKey key = new NamespacedKey(plugin, CURE_BOOK_KEY);
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, bookNumber);
    }
}
