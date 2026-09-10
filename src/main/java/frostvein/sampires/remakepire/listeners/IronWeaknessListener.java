package frostvein.sampires.remakepire.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Shelf;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.SessionManager;
import frostvein.sampires.remakepire.managers.VampireManager;

public class IronWeaknessListener implements Listener {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final Set<Material> ironMaterials;
    private final Map<UUID, Long> knockbackCooldowns;
    // Controls how close a vampire has to get to be repelled or weakened.
    private final double REPEL_DISTANCE = 2.0, WEAKNESS_DISTANCE = 5.0;
    // Controls how far and quickly a vampire gets thrown back.
    private final double REPEL_STRENGTH = 0.5;

    /**
     * Create an instance of the Iron "silver" Weakness listener.
     *
     * @param plugin the host plugin object.
     */
    public IronWeaknessListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.ironMaterials = this.initializeIronMaterials();
        this.knockbackCooldowns = new HashMap<>();

        (new BukkitRunnable() {
            public void run() {
                IronWeaknessListener.this.checkIronProximity();
            }
        }).runTaskTimer(plugin, 0L, 10L);

        (new BukkitRunnable() {
            public void run() {
                IronWeaknessListener.this.scanAndRemoveIronFromInventories();
            }
        }).runTaskTimer(plugin, 0L, 200L);
    }

    /**
     * Retrieve the list of silver materials.
     *
     * @return A {@code Set} of materials considered as silver.
     */
    public Set<Material> getIronMaterials() {
        return this.ironMaterials;
    }

    /**
     * Define the materials that are considered as silver.
     *
     * @return A {@code Set} of materials considered as silver.
     */
    private Set<Material> initializeIronMaterials() {
        Set<Material> materials = new HashSet<>();

        materials.add(Material.RAW_IRON);
        materials.add(Material.IRON_INGOT);
        materials.add(Material.IRON_NUGGET);
        materials.add(Material.IRON_SWORD);
        materials.add(Material.IRON_PICKAXE);
        materials.add(Material.IRON_AXE);
        materials.add(Material.IRON_SHOVEL);
        materials.add(Material.IRON_HOE);
        materials.add(Material.IRON_HELMET);
        materials.add(Material.IRON_CHESTPLATE);
        materials.add(Material.IRON_LEGGINGS);
        materials.add(Material.IRON_BOOTS);
        materials.add(Material.IRON_HORSE_ARMOR);

        // Add the blocks to the materials list if vampires are to be affected by them
        if (plugin.getConfigManager().doSilverBlocksWeakenVampires()) {
            materials.add(Material.IRON_BLOCK);
            materials.add(Material.RAW_IRON_BLOCK);
            materials.add(Material.IRON_DOOR);
            materials.add(Material.IRON_TRAPDOOR);

            materials.add(Material.NETHERITE_BLOCK);    // The block that replaces placed silver blocks to provide the increased breaking time.
        }

        return materials;
    }

    /**
     * Remove silver items from the inventory of a single higher vampire.
     *
     * @param player the player whose inventory is being scanned.
     */
    private void scanAndRemoveIronFromSingleInventory(Player player) {
        if (this.vampireManager.isIronAffected(player)) {
            PlayerInventory inventory = player.getInventory();
            boolean foundIronItems = false;

            // Clear silver items from the player's main inventory
            ItemStack[] contents = inventory.getContents();

            for (int i = 0; i < contents.length; ++i) {
                ItemStack item = contents[i];
                if (item != null && !item.getType().isAir() && this.ironMaterials.contains(item.getType())) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                    inventory.setItem(i, null);
                    foundIronItems = true;
                }
            }

            // Clear silver items from the player's equipped armor
            ItemStack[] armor = inventory.getArmorContents();

            for (int i = 0; i < armor.length; ++i) {
                ItemStack item = armor[i];

                if (item != null && !item.getType().isAir() && this.ironMaterials.contains(item.getType())) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                    armor[i] = null;
                    foundIronItems = true;
                }
            }

            inventory.setArmorContents(armor);

            // Clear silver items from the player's offhand
            ItemStack offhand = inventory.getItemInOffHand();

            if (!offhand.getType().isAir() && this.ironMaterials.contains(offhand.getType())) {
                player.getWorld().dropItemNaturally(player.getLocation(), offhand);
                inventory.setItemInOffHand(null);
                foundIronItems = true;
            }

            if (foundIronItems) {
                player.sendMessage(Component.text("Silver in your pocket begins to burn your skin through the cloth. You involuntarily drop it to protect yourself", NamedTextColor.RED));
            }
        }
    }

    /**
     * Remove silver items from the inventories of higher vampires.
     */
    private void scanAndRemoveIronFromInventories() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.scanAndRemoveIronFromSingleInventory(player);
        }
    }

    /**
     * Prevent higher vampires from taking silver items from other inventories.
     *
     * @param event a player clicks inside an inventory menu.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (this.vampireManager.isIronAffected(player)) {
                if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT || event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT) {
                    ItemStack clickedItem = event.getCurrentItem();

                    if (clickedItem == null || clickedItem.getType().isAir()) {
                        return;
                    }

                    if (event.getClickedInventory() != player.getInventory() && this.ironMaterials.contains(clickedItem.getType())) {
                        this.handleShiftClickIntoInventory(player, clickedItem, event);
                    }
                }
            }
        }
    }

    /**
     * Cancel the player's action and inform them that they cannot pick up silver items.
     *
     * @param player the player attempting to take a silver item.
     * @param item the silver item.
     * @param event a player clicks inside an inventory menu.
     */
    private void handleShiftClickIntoInventory(Player player, ItemStack item, InventoryClickEvent event) {
        event.setCancelled(true);
        player.sendMessage(Component.text("You attempt to grab the item, but it burns your hand as you reach for it."));
    }

    /**
     * Prevent higher vampires from picking up silver items.
     *
     * @param event an entity picks up an item.
     */
    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (this.vampireManager.isIronAffected(player)) {
                Material itemType = event.getItem().getItemStack().getType();

                if (this.ironMaterials.contains(itemType)) {
                    event.setCancelled(true);

                    // Only inform the player of the burning silver a single time
                    if (!player.getScoreboardTags().contains(SessionManager.INFORMED_PICKUP_ITEM)) {
                        player.addScoreboardTag(SessionManager.INFORMED_PICKUP_ITEM);
                        player.sendMessage(Component.text("The silver you have tried to pick up burns your fingers as you touch it... Best leave it alone...", NamedTextColor.RED));
                    }
                }
            }
        }
    }

    /**
     * Prevent higher vampires from retrieving silver items from armor stands.
     *
     * @param event a player interacts with an armor stand.
     */
    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        Player player = event.getPlayer();
        Material armorStandItemType = event.getArmorStandItem().getType();

        if (this.vampireManager.isIronAffected(player)) {
            if (this.ironMaterials.contains(armorStandItemType)) {
                event.setCancelled(true);

                // Only inform the player of the burning silver a single time each session
                if (!player.getScoreboardTags().contains(SessionManager.INFORMED_PICKUP_ITEM)) {
                    player.addScoreboardTag(SessionManager.INFORMED_PICKUP_ITEM);
                    player.sendMessage(Component.text("The silver you have tried to pick up burns your fingers as you touch it... Best leave it alone...", NamedTextColor.RED));
                }
            }
        }
    }

    /**
     * Prevent higher vampires from throwing bottles of holy water.
     *
     * @param event a player interacts with an object.
     */
    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        try {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (this.vampireManager.isIronAffected(player)) {
                    // Check if the player is taking an item from a shelf
                    if (event.getClickedBlock() != null && event.getClickedBlock().getState() instanceof Shelf) {
                        // Check if any iron items have entered the player's inventory after they have taken from the shelf
                        Bukkit.getScheduler().runTaskLater(this.plugin, () -> scanAndRemoveIronFromSingleInventory(player), 5L);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Toss higher vampires away from silver-typed blocks.
     *
     * @param event a player moving.
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Don't check for repulsion when the session is not active
        if (!this.plugin.getSessionManager().isSessionActive()) {
            return;
        }

        Player player = event.getPlayer();

        if (this.vampireManager.isIronAffected(player)) {
            final Location to = event.getTo();

            final UUID playerId = player.getUniqueId();
            final long currentTime = System.currentTimeMillis();

            if (this.knockbackCooldowns.containsKey(playerId)) {
                if (currentTime - this.knockbackCooldowns.get(playerId) < 1000L) {
                    return;
                }
            }

            Location nearestIronBlock = this.getNearestIronBlock(to, this.REPEL_DISTANCE);
            if (nearestIronBlock != null) {
                event.setCancelled(true);
                this.applyIronRepulsion(player, to, nearestIronBlock);
            }
        }
    }

    /**
     * Determine the direction the player should be knocked back toward.
     *
     * @param location the player's location.
     * @return A {@code Vector} away from the nearest silver block.
     */
    private Vector getDirectionAwayFromNearestIron(Location location) {
        // Retrieve the nearest iron block to the player
        Location nearestIron = this.getNearestIronBlock(location, this.REPEL_DISTANCE);
        return this.getDirectionAwayFromNearestIron(location, nearestIron);
    }

    /**
     * Determine the direction the player should be knocked back toward.
     *
     * @param playerLocation the player's location.
     * @param ironLocation the closest silver's location.
     * @return A {@code Vector} away from the provided silver block.
     */
    private Vector getDirectionAwayFromNearestIron(Location playerLocation, Location ironLocation) {
        if (ironLocation != null) {
            return playerLocation.toVector().subtract(ironLocation.toVector()).normalize();
        } else {
            return new Vector(0, 0, 1);
        }
    }

    /**
     * Determine if the vampire should be repelled or weakened by nearby silver during active sessions.
     */
    public void checkIronProximity() {
        // Don't check for silver proximity when the session is not active
        if (this.plugin.getSessionManager().isSessionActive()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Only apply the silver effects to higher vampires
                if (this.vampireManager.isIronAffected(player)) {
                    // Repel the player from the silver block
                    Location nearestIronBlock = this.getNearestIronBlock(player.getLocation(), REPEL_DISTANCE);
                    if (nearestIronBlock != null) {
                        this.applyIronRepulsion(player, player.getLocation(), nearestIronBlock);
                    }

                    // Weaken the player from the silver block's proximity
                    if (this.isNearIronBlock(player.getLocation(), WEAKNESS_DISTANCE)) {
                        this.applyIronWeakness(player);
                    }
                }
            }
        }
    }

    /**
     * Determine if the player is within a distance of a silver-typed block.
     *
     * @param location the player's location.
     * @param radius the range around the player to search.
     * @return {@code true} if the player is nearby a silver block.
     */
    private boolean isNearIronBlock(Location location, double radius) {
        return this.getNearestIronBlock(location, radius) != null;
    }

    /**
     * Retrieve the location of the nearest silver-typed block to the player within the radius.
     *
     * @param location the player's location.
     * @param radius the range around the player to search.
     * @return The {@code Location} of the silver block, or {@code null} if none is found.
     */
    private Location getNearestIronBlock(Location location, double radius) {
        final int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();
        double nearestDistanceSquared = Double.MAX_VALUE;
        Location nearestIron = null;

        for (double dx = -radius; dx <= radius; ++dx) {
            for (double dy = -radius; dy <= radius; ++dy) {
                for (double dz = -radius; dz <= radius; ++dz) {
                    // Check each block within the cube radius to see if it is a silver-type block
                    Block block = location.getWorld().getBlockAt((int)(x + dx), (int)(y + dy), (int)(z + dz));

                    if (this.ironMaterials.contains(block.getType())) {
                        // Make sure we are returning the closest silver block to the search location
                        final double distanceSquared = Math.pow(dx ,2) + Math.pow(dy ,2) + Math.pow(dz ,2);

                        if (distanceSquared < nearestDistanceSquared) {
                            nearestDistanceSquared = distanceSquared;
                            nearestIron = block.getLocation();
                        }
                    }
                }
            }
        }

        return nearestIron;
    }

    /**
     * Repel the player away from the silver block effecting them.
     *
     * @param player the vampire being repelled.
     */
    private void applyIronRepulsion(Player player) {
        this.applyIronRepulsion(player, player.getLocation(), this.getNearestIronBlock(player.getLocation(), REPEL_DISTANCE));
    }

    /**
     * Repel the player away from the silver block effecting them.
     *
     * @param player the vampire being repelled.
     * @param newLocation the location a player attempted to move to.
     * @param ironLocation the repulsing silver block's location.
     */
    private void applyIronRepulsion(Player player, Location newLocation, Location ironLocation) {
        final UUID playerId = player.getUniqueId();
        final long currentTime = System.currentTimeMillis();

        // Only inform the player of the silver's repulsion effects once each session
        if (!player.getScoreboardTags().contains(SessionManager.INFORMED_IRON_BLOCK_REPEL)) {
            player.addScoreboardTag(SessionManager.INFORMED_IRON_BLOCK_REPEL);
            player.sendMessage(Component.text("A block of silver is repelling you from this area...", NamedTextColor.RED));
        }

        Vector knockbackDirection = this.getDirectionAwayFromNearestIron(newLocation, ironLocation);
        knockbackDirection.multiply(this.REPEL_STRENGTH);
        knockbackDirection.setY(Math.max(0.3, knockbackDirection.getY()));
        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> player.setVelocity(knockbackDirection));
        this.knockbackCooldowns.put(playerId, currentTime);
    }

    /**
     * Weaken the player with the effect of silver proximity.
     *
     * @param player the vampire being weakened.
     */
    private void applyIronWeakness(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 0, false, false));

        // Only inform the player of the silver weakness effect a single time each session
        if (!player.getScoreboardTags().contains(SessionManager.INFORMED_IRON_BLOCK_WEAKNESS)) {
            player.addScoreboardTag(SessionManager.INFORMED_IRON_BLOCK_WEAKNESS);
            player.sendMessage(Component.text("A source of silver nearby is weakening you...", NamedTextColor.RED));
        }
    }
}
