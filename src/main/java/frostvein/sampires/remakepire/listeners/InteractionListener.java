package frostvein.sampires.remakepire.listeners;

import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Shelf;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Strider;
import org.bukkit.entity.Turtle;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import frostvein.sampires.remakepire.RemakepirePlugin;
import org.bukkit.inventory.PlayerInventory;

public class InteractionListener implements Listener {
    private final RemakepirePlugin plugin;
    private static final List<Material> FEEDING_ITEMS = Arrays.asList(Material.WHEAT, Material.CARROT, Material.POTATO, Material.BEETROOT, Material.WHEAT_SEEDS, Material.MELON_SEEDS, Material.PUMPKIN_SEEDS, Material.BEETROOT_SEEDS, Material.SWEET_BERRIES, Material.APPLE, Material.GOLDEN_APPLE, Material.GOLDEN_CARROT, Material.BREAD, Material.COOKIE, Material.MELON_SLICE, Material.DRIED_KELP, Material.HAY_BLOCK, Material.SUGAR);

    /**
     * Create an instance of the Interactions listener.
     *
     * @param plugin the host plugin object.
     */
    public InteractionListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Determine in an item is the chalice.
     *
     * @param item the item being checked.
     * @return {@code true} if the item is the chalice's material type.
     */
    private boolean isChalice(ItemStack item) {
        if (item == null) {
            return false;
        }

        return item.getType() == Material.LAPIS_BLOCK;
    }

    /**
     * Determine in a block is the chalice.
     *
     * @param block the block being checked.
     * @return {@code true} if the block is the chalice's material type.
     */
    private boolean isChalice(Block block) {
        if (block == null) {
            return false;
        }

        return block.getType() == Material.LAPIS_BLOCK;
    }

    /**
     * Force the chalice to be in the player's active slots.
     *
     * @param player the player with the chalice.
     */
    private void applyChalice(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && !player.isDead() && player.getGameMode() != GameMode.SPECTATOR) {
                enforcePumpkin(player);
            }
        });
    }

    /**
     * Place the chalice in one of the player's hands.
     *
     * @param player the player with the chalice.
     */
    private void enforcePumpkin(Player player) {
        PlayerInventory inventory = player.getInventory();

        // Already holding a pumpkin in either hand.
        if (isChalice(inventory.getItemInMainHand()) || isChalice(inventory.getItemInOffHand())) {
            return;
        }

        // Search storage inventory, including hotbar.
        for (int slot = 0; slot < 36; slot++) {
            ItemStack item = inventory.getItem(slot);

            // Swap pumpkin with whatever is in offhand.
            if (isChalice(item)) {
                ItemStack offhand = inventory.getItemInOffHand();

                inventory.setItemInOffHand(item);
                inventory.setItem(slot, offhand);

                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            applyChalice(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        applyChalice(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        applyChalice(event.getPlayer());
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        applyChalice(event.getPlayer());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        applyChalice(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        applyChalice(event.getPlayer());
    }

    @EventHandler
    public void onShelfInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getClickedBlock() == null) {
            return;
        }

        if (!(event.getClickedBlock().getState() instanceof Shelf)) {
            return;
        }

        // Shelves can transfer items from the hotbar.
        PlayerInventory inventory = event.getPlayer().getInventory();

        for (int slot = 0; slot < 9; slot++) {
            if (isChalice(inventory.getItem(slot))) {
                event.setCancelled(true);
                return;
            }
        }

        // Also check the offhand.
        if (isChalice(inventory.getItemInOffHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChalicePlace(BlockPlaceEvent event) {
        if (isChalice(event.getBlockPlaced())) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent vampires from interacting with entities in bat form or feeding entities.<br/>
     * Prevent players from breeding animals out of session, if the setting is prevented.
     *
     * @param event a player interacts with an entity.
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity targetEntity = event.getRightClicked();

        if (this.plugin.getVampireManager().isVampire(player)) {
            // Stop bat players from doing shenanigans
            if (this.plugin.getBatTransformationManager().isInBatForm(player)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("You cannot interact with anything while you are in your bat form.", NamedTextColor.RED));

            } else if (!(targetEntity instanceof Player)) {
                // Stop higher vampires from feeding animals
                if (this.isFeedableMob(targetEntity)) {
                    ItemStack handItem = event.getHand() == EquipmentSlot.OFF_HAND ? player.getInventory().getItemInOffHand() : player.getInventory().getItemInMainHand();

                    if (FEEDING_ITEMS.contains(handItem.getType())) {
                        this.handleVampireFeedingAttempt(player, targetEntity, handItem, event);
                    }
                }
            }
        }
    }

    /**
     * Determine if an entity is feedable.
     *
     * @param entity an entity to check.
     * @return {@code true} if the entity can be fed by players.
     */
    private boolean isFeedableMob(Entity entity) {
        return entity instanceof Animals || entity instanceof Horse || entity instanceof Llama || entity instanceof Wolf || entity instanceof Cat || entity instanceof Parrot || entity instanceof Rabbit || entity instanceof Turtle || entity instanceof Fox || entity instanceof Bee || entity instanceof Strider || entity instanceof Hoglin;
    }

    /**
     * Inform the vampire of the changing or preventative circumstances when they attempt to feed an animal.
     *
     * @param vampire the vampire attempting to feed an animal.
     * @param mob the entity that the vampire is trying to feed.
     * @param foodItem the item used to feed the animal.
     * @param event a player interacts with an entity.
     */
    private void handleVampireFeedingAttempt(Player vampire, Entity mob, ItemStack foodItem, PlayerInteractEntityEvent event) {
        this.plugin.logInfo("Vampire " + vampire.getName() + " attempted to feed " + mob.getType() + " with " + foodItem.getType());

        if (this.plugin.getVampireManager().isVampireStage1(vampire) && !this.plugin.getVampireManager().isRestrictedVampire(vampire)) {
            vampire.sendMessage(Component.text("The animal tentatively eats from your hand, eyeing you suspiciously, as if it knows your true nature...", NamedTextColor.RED));

        } else {
            event.setCancelled(true);
            vampire.sendMessage(Component.text("The animal recoils from you as you extend a hand to it...", NamedTextColor.RED));
        }
    }

    /**
     * Prevent vampires from using anvils.
     *
     * @param event a player interacts with an object.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (clickedBlock != null && Tag.ANVIL.isTagged(clickedBlock.getType()) && this.plugin.getVampireManager().isVampire(player)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("You cannot use anvils as a vampire.", NamedTextColor.RED));
            }
        }
    }
}
