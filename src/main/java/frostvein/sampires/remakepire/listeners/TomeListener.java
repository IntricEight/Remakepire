package frostvein.sampires.remakepire.listeners;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.TomeManager;

public class TomeListener implements Listener {
    private final RemakepirePlugin plugin;
    private final TomeManager tomeManager;

    /**
     * Create an instance of the Tome listener.
     *
     * @param plugin the host plugin object.
     */
    public TomeListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.tomeManager = plugin.getTomeManager();
    }

    /**
     * Grant human players tome and cure abilities when using the respective books.
     *
     * @param event a player interacts with an object.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Action action = event.getAction();

        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            final Material blockType = event.getClickedBlock().getType();

            // Allow players to open containers while holding a tome book
            if (blockType == Material.CHEST || blockType == Material.TRAPPED_CHEST || blockType == Material.BARREL || blockType == Material.ENDER_CHEST || Tag.WOODEN_SHELVES.isTagged(blockType) || Tag.SHULKER_BOXES.isTagged(blockType) || blockType == Material.CRAFTING_TABLE || blockType == Material.FURNACE || blockType == Material.BLAST_FURNACE || blockType == Material.SMOKER || blockType == Material.BREWING_STAND || Tag.ANVIL.isTagged(blockType) || blockType == Material.ENCHANTING_TABLE || blockType == Material.GRINDSTONE || blockType == Material.STONECUTTER || blockType == Material.LOOM || blockType == Material.CARTOGRAPHY_TABLE || blockType == Material.SMITHING_TABLE || blockType == Material.LECTERN || blockType == Material.HOPPER || blockType == Material.DROPPER || blockType == Material.DISPENSER) {
                return;
            }
        }

        if (item == null || item.getType() != Material.WRITTEN_BOOK) {
            return;
        }

        BookMeta bookMeta = (BookMeta)item.getItemMeta();

        if (bookMeta == null || !bookMeta.hasTitle()) {
            return;
        }

        final String tomeTitle = bookMeta.getTitle();
        final int cureBookNumber = this.plugin.getCureBookReadingListener().getAuthenticCureBookNumber(item);
        this.plugin.logInfo("Player " + player.getName() + " using tome with title: '" + tomeTitle + "'");

        if (cureBookNumber > 0) {
            // Prevent the player from reading the fourth cure book if the rest of the trinity has not been read
            if (cureBookNumber == 4 && !CureBookReadingListener.hasReadAllCureBooks(player)) {
                event.setCancelled(true);
                player.openBook(this.plugin.getCureBookManager().getObscuredBook());

            } else {
                this.plugin.getCureBookReadingListener().onCureBookRead(player, cureBookNumber);
            }
        } else if (tomeTitle == null || !this.tomeManager.isValidAbility(tomeTitle)) {
            this.plugin.logInfo("Invalid tome ability: '" + tomeTitle + "'");

        } else if (!this.plugin.getVampireManager().isHuman(player)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("The ancient knowledge within this tome is beyond your vampiric comprehension...", NamedTextColor.RED));

        } else if (!this.plugin.getSessionManager().isSessionActive()) {
            event.setCancelled(true);
            player.sendMessage(Component.text("The tome's magic lies dormant... It can only be absorbed during an active session.", NamedTextColor.RED));

        } else {
            this.plugin.logInfo("Valid tome ability: '" + tomeTitle + "'");
            event.setCancelled(true);

            if (this.tomeManager.hasAbility(player, tomeTitle)) {
                this.plugin.logInfo("Player " + player.getName() + " already has ability: '" + tomeTitle + "'");
                player.sendMessage(Component.text("The words seem familiar and hold no new secrets for you.", NamedTextColor.GRAY));

            } else {
                this.plugin.logInfo("Attempting to grant ability '" + tomeTitle + "' to player " + player.getName());

                if (this.tomeManager.grantAbility(player, tomeTitle)) {
                    this.informNewTomeAbility(player, tomeTitle);

                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        player.getInventory().setItemInMainHand(null);
                    }

                    this.plugin.logInfo(tomeTitle + "grant result: Successful");

                } else {
                    this.plugin.logInfo(tomeTitle + "grant result: Failure");
                }
            }
        }
    }

    /**
     * Grant players knowledge of vampiric cures when reading cure books from lecterns.
     *
     * @param event a player interacts with an object.
     */
    @EventHandler
    public void onPlayerInteractWithLectern(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Action action = event.getAction();

        // Check that the player is interacting with a lectern, not another block or air
        if (action != Action.RIGHT_CLICK_BLOCK || block == null || block.getType() != Material.LECTERN) {
            return;
        }

        // Get the book from the lectern and determine if it is a cure book
        if (event.getClickedBlock().getState() instanceof Lectern lectern) {
            ItemStack book = lectern.getInventory().getItem(0);

            if (book == null || book.getType() != Material.WRITTEN_BOOK) {
                return;
            }

            final int cureBookNumber = this.plugin.getCureBookReadingListener().getAuthenticCureBookNumber(book);

            if (cureBookNumber > 0) {
                // Prevent the player from reading the fourth cure book if the rest of the trinity has not been read
                if (cureBookNumber == 4 && !CureBookReadingListener.hasReadAllCureBooks(player)) {
                    event.setCancelled(true);
                    player.openBook(this.plugin.getCureBookManager().getObscuredBook());

                } else {
                    this.plugin.getCureBookReadingListener().onCureBookRead(player, cureBookNumber);
                }
            }
        }
    }

    /**
     * Control interactions with the tome selection UI.
     *
     * @param event a player clicks inside an inventory menu.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().title().equals(TomeManager.TOME_SELECTION_GUI_TITLE)) {
            event.setCancelled(true);

            if (event.getWhoClicked() instanceof Player admin) {
                ItemStack clickedItem = event.getCurrentItem();

                if (clickedItem == null || clickedItem.getType() != Material.WRITTEN_BOOK) {
                    return;
                }

                final UUID targetUUID = this.tomeManager.getTomeSelectionTarget(admin.getUniqueId());

                if (targetUUID == null) {
                    admin.sendMessage(Component.text("Error: Could not find target player for this selection.", NamedTextColor.RED));
                    admin.closeInventory();
                    return;
                }

                Player target = Bukkit.getPlayer(targetUUID);

                if (target != null && target.isOnline()) {
                    ItemMeta meta = clickedItem.getItemMeta();

                    if (meta != null && meta.customName() != null) {
                        if (meta.hasLore()) {
                            for (String line : meta.getLore()) {
                                if (line.startsWith("§8[CURE_BOOK:")) {
                                    final String tag = line.substring("§8[CURE_BOOK:".length(), line.length() - 1);
                                    this.handleCureBookClick(admin, target, tag);
                                    return;
                                }
                            }
                        }

                        String displayName = PlainTextComponentSerializer.plainText().serialize(meta.customName());

                        // Filter out extra content from the string name
                        displayName = displayName.replaceAll("§[0-9a-fk-or]", "").trim();
                        if (displayName.startsWith("✓ ")) {
                            displayName = displayName.substring(2);
                        }

                        if (displayName.contains(" (Already has)")) {
                            displayName = displayName.replace(" (Already has)", "");
                        }

                        final String abilityName = displayName.replace(" ", "").toLowerCase();

                        if (this.tomeManager.hasAbility(target, abilityName)) {
                            this.tomeManager.removeAbility(target, abilityName);

                            admin.sendMessage(Component.text("Removed ", NamedTextColor.RED)
                                    .append(Component.text(displayName, NamedTextColor.WHITE))
                                    .append(Component.text(" from ", NamedTextColor.RED))
                                    .append(Component.text(target.getName(), NamedTextColor.YELLOW))
                            );
                            target.sendMessage(Component.text("The tome ability ", NamedTextColor.RED)
                                    .append(Component.text(displayName, NamedTextColor.WHITE))
                                    .append(Component.text(" has been removed from you.", NamedTextColor.RED))
                            );

                            target.playSound(target.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0F, 1.0F);

                        } else {
                            this.tomeManager.forceGrantAbility(target, abilityName);

                            admin.sendMessage(Component.text("Granted ", NamedTextColor.GREEN)
                                    .append(Component.text(displayName, NamedTextColor.WHITE))
                                    .append(Component.text(" to ", NamedTextColor.GREEN))
                                    .append(Component.text(target.getName(), NamedTextColor.YELLOW))
                            );
                            target.sendMessage(Component.text("You have been granted the tome ability: ", NamedTextColor.GREEN)
                                    .append(Component.text(displayName, NamedTextColor.WHITE)));

                            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
                        }

                        this.tomeManager.openTomeSelectionGUI(admin, target);
                    }
                } else {
                    admin.sendMessage(Component.text("Target player is no longer online.", NamedTextColor.RED));
                    admin.closeInventory();
                }
            }
        }
    }

    /**
     * Close the tome selection UI.
     *
     * @param event a player closes an inventory window.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().title().equals(TomeManager.TOME_SELECTION_GUI_TITLE)) {
            if (event.getPlayer() instanceof Player player) {
                Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                    if (!player.getOpenInventory().title().equals(TomeManager.TOME_SELECTION_GUI_TITLE)) {
                        this.tomeManager.removeTomeSelectionTarget(player.getUniqueId());
                    }
                }, 1L);
            }
        }
    }

    /**
     * Give or remove the tome from the human player's access,
     *
     * @param admin the player managing the tome selection.
     * @param target the player earning or losing tomes.
     * @param tag the book's name.
     */
    private void handleCureBookClick(Player admin, Player target, String tag) {
        final boolean hasTag = target.getScoreboardTags().contains(tag);

        final String friendlyName = switch (tag) {
            case CureBookReadingListener.TAG_CURE_BOOK_1 -> "Cure Book 1 (" + this.plugin.getCureBookManager().getCureBookName(1, false) + ")";
            case CureBookReadingListener.TAG_CURE_BOOK_2 -> "Cure Book 2 (" + this.plugin.getCureBookManager().getCureBookName(2, false) + ")";
            case CureBookReadingListener.TAG_CURE_BOOK_3 -> "Cure Book 3 (" + this.plugin.getCureBookManager().getCureBookName(3, false) + ")";
            case CureBookReadingListener.TAG_CURE_BOOK_4 -> "Cure Book 4 (" + this.plugin.getCureBookManager().getCureBookName(4, false) + ")";
            default -> tag;
        };

        if (hasTag) {
            target.removeScoreboardTag(tag);

            admin.sendMessage(Component.text("Removed ", NamedTextColor.RED)
                    .append(Component.text(friendlyName, NamedTextColor.DARK_PURPLE))
                    .append(Component.text(" tag from ", NamedTextColor.RED))
                    .append(Component.text(target.getName(), NamedTextColor.YELLOW))
            );
            target.sendMessage(Component.text("The ", NamedTextColor.RED)
                    .append(Component.text(friendlyName, NamedTextColor.DARK_PURPLE))
                    .append(Component.text(" tag has been removed from you.", NamedTextColor.RED))
            );

            target.playSound(target.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0F, 1.0F);

        } else {
            target.addScoreboardTag(tag);

            admin.sendMessage(Component.text("Granted ", NamedTextColor.GREEN)
                    .append(Component.text(friendlyName, NamedTextColor.DARK_PURPLE))
                    .append(Component.text(" tag to ", NamedTextColor.GREEN))
                    .append(Component.text(target.getName(), NamedTextColor.YELLOW))
            );
            target.sendMessage(Component.text("You have been granted the ", NamedTextColor.GREEN)
                    .append(Component.text(friendlyName, NamedTextColor.DARK_PURPLE))
                    .append(Component.text(" tag.", NamedTextColor.GREEN))
            );

            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
        }

        this.tomeManager.openTomeSelectionGUI(admin, target);
    }

    /**
     * Inform the player of their new tome ability.
     *
     * @param player the human gaining the ability.
     * @param tomeTitle the ability within the tome book.
     */
    private void informNewTomeAbility(Player player, String tomeTitle) {
        final String command = "/pow tome " + tomeTitle.toLowerCase();

        player.sendMessage("");

        player.sendMessage(Component.text("TOME LEARNT", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("You feel ancient knowledge flowing into your mind...", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("You have learned the ability: ", NamedTextColor.GREEN)
                .append(Component.text(tomeManager.getAbilityDisplayName(tomeTitle), NamedTextColor.WHITE)));

        player.sendMessage("");

        player.sendMessage(Component.text("Use ", NamedTextColor.GRAY)
                .append(Component.text(command, NamedTextColor.WHITE)
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.copyToClipboard(command))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to copy command to clipboard", NamedTextColor.GRAY)))
                )
                .append(Component.text(" to activate this ability.", NamedTextColor.GRAY)));

        player.sendMessage("");

        player.playSound(player, "minecraft:ambient.crimson_forest.mood", 1.0F, 1.0F);
    }
}
