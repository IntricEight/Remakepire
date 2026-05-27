package frostvein.sampires.remakepire.listeners;

import java.util.UUID;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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
import frostvein.sampires.remakepire.managers.VampireManager;

public class TomeListener implements Listener {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final TomeManager tomeManager;

    /**
     * Create an instance of the Tome listener.
     *
     * @param plugin the host plugin object.
     */
    public TomeListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
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

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                Material blockType = event.getClickedBlock().getType();

                // Allow players to open containers while holding a tome book
                if (blockType == Material.CHEST || blockType == Material.TRAPPED_CHEST || blockType == Material.BARREL || blockType == Material.ENDER_CHEST || blockType == Material.SHULKER_BOX || blockType.name().contains("SHULKER_BOX") || blockType == Material.CRAFTING_TABLE || blockType == Material.FURNACE || blockType == Material.BLAST_FURNACE || blockType == Material.SMOKER || blockType == Material.BREWING_STAND || blockType == Material.ANVIL || blockType == Material.CHIPPED_ANVIL || blockType == Material.DAMAGED_ANVIL || blockType == Material.ENCHANTING_TABLE || blockType == Material.GRINDSTONE || blockType == Material.STONECUTTER || blockType == Material.LOOM || blockType == Material.CARTOGRAPHY_TABLE || blockType == Material.SMITHING_TABLE || blockType == Material.LECTERN || blockType == Material.HOPPER || blockType == Material.DROPPER || blockType == Material.DISPENSER) {
                    return;
                }
            }

            if (item != null && item.getType() == Material.WRITTEN_BOOK) {
                BookMeta bookMeta = (BookMeta)item.getItemMeta();

                if (bookMeta != null && bookMeta.hasTitle()) {
                    String tomeTitle = bookMeta.getTitle();
                    this.plugin.logInfo("Player " + player.getName() + " using tome with title: '" + tomeTitle + "'");

                    int cureBookNumber = CureBookReadingListener.getAuthenticCureBookNumber(item, this.plugin);

                    if (cureBookNumber > 0) {
                        // Prevent the player from reading the fourth cure book if the rest of the trinity has not been read
                        if (cureBookNumber == 4 && !CureBookReadingListener.hasReadAllCureBooks(player)) {
                            event.setCancelled(true);
                            ItemStack obscuredBook = new ItemStack(Material.WRITTEN_BOOK);
                            BookMeta obscuredMeta = (BookMeta)obscuredBook.getItemMeta();

                            if (obscuredMeta != null) {
                                obscuredMeta.setTitle(this.plugin.getCureBookManager().getCureBookName(4, true));
                                obscuredMeta.setAuthor(this.plugin.getCureBookManager().getCureBookAuthor(4));
                                obscuredMeta.setPages(this.plugin.getCureBookManager().getCureBook4UnreadablePages());
                                obscuredBook.setItemMeta(obscuredMeta);
                            }

                            player.openBook(obscuredBook);
                        } else {
                            this.plugin.getCureBookReadingListener().onCureBookRead(player, cureBookNumber);
                        }
                    } else if (!this.tomeManager.isValidAbility(tomeTitle)) {
                        this.plugin.logInfo("Invalid tome ability: '" + tomeTitle + "'");

                    } else if (!this.vampireManager.isHuman(player)) {
                        event.setCancelled(true);
                        player.sendMessage("§cThe ancient knowledge within this tome is beyond your vampiric comprehension...");

                    } else if (!this.plugin.getSessionManager().isSessionActive()) {
                        event.setCancelled(true);
                        player.sendMessage("§cThe tome's magic lies dormant... It can only be absorbed during an active session.");

                    } else {
                        this.plugin.logInfo("Valid tome ability: '" + tomeTitle + "'");
                        event.setCancelled(true);

                        if (this.tomeManager.hasAbility(player, tomeTitle)) {
                            this.plugin.logInfo("Player " + player.getName() + " already has ability: '" + tomeTitle + "'");
                            player.sendMessage("§7The words seem familiar and hold no new secrets for you.");

                        } else {
                            this.plugin.logInfo("Attempting to grant ability '" + tomeTitle + "' to player " + player.getName());
                            boolean success = this.tomeManager.grantAbility(player, tomeTitle);
                            this.plugin.logInfo("Grant result: " + success);

                            if (success) {
                                player.sendMessage("\n§6§lTOME LEARNT");
                                player.sendMessage("§eYou feel ancient knowledge flowing into your mind...");
                                player.sendMessage("§aYou have learned the ability: §f" + tomeTitle);

                                String command = "/pow tome " + tomeTitle.toLowerCase();
                                TextComponent prefix = new TextComponent("§7Use ");
                                TextComponent clickableCommand = new TextComponent("§f§n" + command);
                                clickableCommand.setClickEvent(new ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.COPY_TO_CLIPBOARD, command));
                                clickableCommand.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder("§7Click to copy command to clipboard")).create()));
                                TextComponent suffix = new TextComponent("§7 to activate this ability.");
                                TextComponent fullMessage = new TextComponent("");

                                fullMessage.addExtra(prefix);
                                fullMessage.addExtra(clickableCommand);
                                fullMessage.addExtra(suffix);

                                player.sendMessage("");
                                player.spigot().sendMessage(fullMessage);
                                player.sendMessage("");

                                if (item.getAmount() > 1) {
                                    item.setAmount(item.getAmount() - 1);
                                } else {
                                    player.getInventory().setItemInMainHand(null);
                                }

                                player.playSound(player, "minecraft:ambient.crimson_forest.mood", 1.0F, 1.0F);
                            }
                        }
                    }
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
        if (event.getView().getTitle() != null && event.getView().getTitle().equals("§6§lSelect Tome Abilities")) {
            event.setCancelled(true);

            if (event.getWhoClicked() instanceof Player admin) {
                ItemStack clickedItem = event.getCurrentItem();

                if (clickedItem != null && clickedItem.getType() == Material.WRITTEN_BOOK) {
                    UUID targetUUID = this.tomeManager.getTomeSelectionTarget(admin.getUniqueId());

                    if (targetUUID == null) {
                        admin.sendMessage("§cError: Could not find target player for this selection.");
                        admin.closeInventory();

                    } else {
                        Player target = Bukkit.getPlayer(targetUUID);

                        if (target != null && target.isOnline()) {
                            ItemMeta meta = clickedItem.getItemMeta();

                            if (meta != null && meta.getDisplayName() != null) {
                                if (meta.hasLore()) {
                                    for(String line : meta.getLore()) {
                                        if (line.startsWith("§8[CURE_BOOK:")) {
                                            String tag = line.substring("§8[CURE_BOOK:".length(), line.length() - 1);
                                            this.handleCureBookClick(admin, target, tag);
                                            return;
                                        }
                                    }
                                }

                                String displayName = meta.getDisplayName();
                                String cleanName = displayName.replaceAll("§[0-9a-fk-or]", "").trim();
                                if (cleanName.startsWith("✓ ")) {
                                    cleanName = cleanName.substring(2);
                                }

                                if (cleanName.contains(" (Already has)")) {
                                    cleanName = cleanName.replace(" (Already has)", "");
                                }

                                String abilityName = cleanName.replace(" ", "").toLowerCase();

                                if (this.tomeManager.hasAbility(target, abilityName)) {
                                    this.tomeManager.removeAbility(target, abilityName);
                                    admin.sendMessage("§cRemoved §f" + cleanName + " §cfrom §e" + target.getName());
                                    target.sendMessage("§cThe tome ability §f" + cleanName + " §chas been removed from you.");
                                    target.playSound(target.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0F, 1.0F);

                                } else {
                                    this.tomeManager.forceGrantAbility(target, abilityName);
                                    admin.sendMessage("§aGranted §f" + cleanName + " §ato §e" + target.getName());
                                    target.sendMessage("§aYou have been granted the tome ability: §f" + cleanName);
                                    target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
                                }

                                this.tomeManager.openTomeSelectionGUI(admin, target);
                            }
                        } else {
                            admin.sendMessage("§cTarget player is no longer online.");
                            admin.closeInventory();
                        }
                    }
                }
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
        boolean hasTag = target.getScoreboardTags().contains(tag);

        String friendlyName = switch (tag) {
            case "CureBook1Read" -> "Cure Book 1 (" + this.plugin.getCureBookManager().getCureBookName(1, false) + ")";
            case "CureBook2Read" -> "Cure Book 2 (" + this.plugin.getCureBookManager().getCureBookName(2, false) + ")";
            case "CureBook3Read" -> "Cure Book 3 (" + this.plugin.getCureBookManager().getCureBookName(3, false) + ")";
            case "CureBook4Read" -> "Cure Book 4 (" + this.plugin.getCureBookManager().getCureBookName(4, false) + ")";
            default -> tag;
        };

        if (hasTag) {
            target.removeScoreboardTag(tag);
            admin.sendMessage("§cRemoved §5" + friendlyName + " §ctag from §e" + target.getName());
            target.sendMessage("§cThe §5" + friendlyName + " §ctag has been removed from you.");
            target.playSound(target.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0F, 1.0F);

        } else {
            target.addScoreboardTag(tag);
            admin.sendMessage("§aGranted §5" + friendlyName + " §atag to §e" + target.getName());
            target.sendMessage("§aYou have been granted the §5" + friendlyName + " §atag.");
            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
        }

        this.tomeManager.openTomeSelectionGUI(admin, target);
    }

    /**
     * Close the tome selection UI.
     *
     * @param event a player closes an inventory window.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        event.getView().getTitle();

        if (event.getView().getTitle().equals("§6§lSelect Tome Abilities")) {
            if (event.getPlayer() instanceof Player player) {
                Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                    if (player.getOpenInventory() == null || player.getOpenInventory().getTitle() == null || !player.getOpenInventory().getTitle().equals("§6§lSelect Tome Abilities")) {
                        this.tomeManager.removeTomeSelectionTarget(player.getUniqueId());
                    }
                }, 1L);
            }
        }
    }
}
