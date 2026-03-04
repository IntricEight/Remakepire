package frostvein.sampires.remakepire.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.VampireManager;

public class IronWeaknessListener implements Listener {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final Set<Material> ironMaterials;
    private final Map<UUID, Long> knockbackCooldowns;
    // Controls how close a vampire has to get to be repelled or weakened.
    private final double REPEL_DISTANCE = 2.0;
    private final double WEAKNESSS_DISTANCE = 5.0;
    // Controls how far and quickly a vampire gets thrown back.
    private final double REPEL_STRENGTH = 0.5;

    /**
     * Create an instance of the Iron "silver" Weakness listener.
     *
     * @param plugin the host plugin object.
     * @param vampireManager the manager for generic vampire traits.
     */
    public IronWeaknessListener(RemakepirePlugin plugin, VampireManager vampireManager) {
        this.plugin = plugin;
        this.vampireManager = vampireManager;
        this.ironMaterials = this.initializeIronMaterials();
        this.knockbackCooldowns = new HashMap<>();

        (new BukkitRunnable() {
            public void run() {
                IronWeaknessListener.this.checkIronProximity();
            }
        }).runTaskTimer(plugin, 0L, 20L);

        (new BukkitRunnable() {
            public void run() {
                IronWeaknessListener.this.scanAndRemoveIronFromInventories();
            }
        }).runTaskTimer(plugin, 0L, 1200L);
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

        materials.add(Material.IRON_INGOT);
        materials.add(Material.IRON_NUGGET);
//        materials.add(Material.RAW_IRON);
//        materials.add(Material.IRON_BLOCK);
//        materials.add(Material.NETHERITE_BLOCK);    // The block that replaces placed silver blocks to provide the increased breaking time.
//        materials.add(Material.RAW_IRON_BLOCK);
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
//        materials.add(Material.IRON_DOOR);
//        materials.add(Material.IRON_TRAPDOOR);

        return materials;
    }

    /**
     * Remove silver items from the inventories of higher vampires.
     */
    private void scanAndRemoveIronFromInventories() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            if (this.vampireManager.isIronAffected(player)) {
                PlayerInventory inventory = player.getInventory();
                boolean foundIronItems = false;
                List<String> droppedItems = new ArrayList<>();
                ItemStack[] contents = inventory.getContents();

                for(int i = 0; i < contents.length; ++i) {
                    ItemStack item = contents[i];
                    if (item != null && !item.getType().isAir() && this.ironMaterials.contains(item.getType())) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                        inventory.setItem(i, null);
                        droppedItems.add(item.getAmount() + "x " + item.getType().name().replace("_", " ").toLowerCase());
                        foundIronItems = true;
                    }
                }

                ItemStack[] armor = inventory.getArmorContents();

                for(int i = 0; i < armor.length; ++i) {
                    ItemStack item = armor[i];

                    if (item != null && !item.getType().isAir() && this.ironMaterials.contains(item.getType())) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                        armor[i] = null;
                        droppedItems.add(item.getAmount() + "x " + item.getType().name().replace("_", " ").toLowerCase());
                        foundIronItems = true;
                    }
                }

                inventory.setArmorContents(armor);
                ItemStack offhand = inventory.getItemInOffHand();

                if (offhand != null && !offhand.getType().isAir() && this.ironMaterials.contains(offhand.getType())) {
                    player.getWorld().dropItemNaturally(player.getLocation(), offhand);
                    inventory.setItemInOffHand(null);
                    droppedItems.add(offhand.getAmount() + "x " + offhand.getType().name().replace("_", " ").toLowerCase());
                    foundIronItems = true;
                }

                if (foundIronItems) {
                    player.sendMessage("§bIron in your pocket begins to burn your skin through the cloth. You involuntarily drop it to protect yourself.");
                }
            }
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
        player.sendMessage("You attempt to grab the item, but it burns your hand as you reach for it.");
        event.setCancelled(true);
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
            if (this.vampireManager.isVampireStage2(player) || this.vampireManager.isVampireStage3(player)) {
                Material itemType = event.getItem().getItemStack().getType();

                if (this.ironMaterials.contains(itemType)) {
                    event.setCancelled(true);

                    if (!player.getScoreboardTags().contains("informed_pickup_item")) {
                        player.addScoreboardTag("informed_pickup_item");
                        player.sendMessage("§bThe iron you have tried to pick up burns your fingers as you touch it... Best leave it alone...");
                    }
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

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (this.vampireManager.isVampireStage2(player) || this.vampireManager.isVampireStage3(player)) {
                ItemStack item = event.getItem();

                if (item != null && this.isHolyWater(item)) {
                    event.setCancelled(true);
                    player.sendMessage("§cThe Holy Water burns your hand as you try to throw it! You feel unable to bring yourself to use this item...");
                }
            }
        }
    }

    /**
     * Prevent higher vampires from throwing bottles of holy water.
     *
     * @param event a projectile is released or thrown.
     */
    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof ThrownPotion potion) {
            if (potion.getShooter() instanceof Player player) {
                if (this.vampireManager.isVampireStage2(player) || this.vampireManager.isVampireStage3(player)) {
                    ItemStack potionItem = potion.getItem();

                    if (this.isHolyWater(potionItem)) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    /**
     * Toss higher vampires away from silver-typed blocks.
     *
     * @param event a player moving.
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (this.vampireManager.isVampire(player)) {
            if (!this.vampireManager.isVampireStage1(player)) {
                Location to = event.getTo();

                if (to != null) {
                    UUID playerId = player.getUniqueId();
                    long currentTime = System.currentTimeMillis();

                    if (this.knockbackCooldowns.containsKey(playerId)) {
                        if (currentTime - this.knockbackCooldowns.get(playerId) < 1000L) {
                            return;
                        }
                    }

                    if (this.isNearIronBlock(to, this.REPEL_DISTANCE)) {
                        event.setCancelled(true);

                        if (!player.getScoreboardTags().contains("informed_iron_block_reply")) {
                            player.sendMessage("§cA block of silver is repelling you from this area...");
                            player.addScoreboardTag("informed_iron_block_reply");
                        }

                        Vector knockbackDirection = this.getDirectionAwayFromNearestIron(to);
                        knockbackDirection.multiply(this.REPEL_STRENGTH);
                        knockbackDirection.setY(Math.max(0.3, knockbackDirection.getY()));
                        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> player.setVelocity(knockbackDirection));
                        this.knockbackCooldowns.put(playerId, currentTime);
                    }
                }
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
        int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();
        Location nearestIron = null;
        double nearestDistance = Double.MAX_VALUE;

        // Search for the silver block within a cube around the player
        for(int dx = (int)(-1 * REPEL_DISTANCE); dx <= (int)(REPEL_DISTANCE); ++dx) {
            for(int dy = (int)(-1 * REPEL_DISTANCE); dy <= (int)(REPEL_DISTANCE); ++dy) {
                for(int dz = (int)(-1 * REPEL_DISTANCE); dz <= (int)(REPEL_DISTANCE); ++dz) {
                    Block block = location.getWorld().getBlockAt(x + dx, y + dy, z + dz);

                    if (this.ironMaterials.contains(block.getType())) {
                        Location ironLoc = block.getLocation().add(0.5, 0.5, 0.5);
                        double distance = location.distance(ironLoc);

                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestIron = ironLoc;
                        }
                    }
                }
            }
        }

        if (nearestIron != null) {
            return location.toVector().subtract(nearestIron.toVector()).normalize();
        } else {
            return new Vector(0, 0, 1);
        }
    }

    /**
     * Determine if the vampire should be weakened by nearby silver.
     */
    public void checkIronProximity() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            if (this.vampireManager.isVampire(player)) {
                Location playerLoc = player.getLocation();

                if (this.isNearIronBlock(playerLoc, WEAKNESSS_DISTANCE)) {
                    this.applyIronEffects(player);
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
        int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();

        for(double dx = -radius; dx <= radius; ++dx) {
            for(double dy = -radius; dy <= radius; ++dy) {
                for(double dz = -radius; dz <= radius; ++dz) {
                    Block block = location.getWorld().getBlockAt((int)(x + dx), (int)(y + dy), (int)(z + dz));

                    if (this.ironMaterials.contains(block.getType())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Weaken the player with the effect of silver proximity.
     *
     * @param player the player being weakened.
     */
    private void applyIronEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 2, false, false));

        if (!player.getScoreboardTags().contains("informed_iron_block_effects")) {
            player.addScoreboardTag("informed_iron_block_effects");
            player.sendMessage("§cA source of silver nearby is weakening you...");
        }
    }

    /**
     * Determine if an item is a bottle of holy water.
     *
     * @param item the item to check.
     * @return {@code true} if the item is holy water (an effectless splash potion).
     */
    private boolean isHolyWater(ItemStack item) {
        if (item == null) {
            return false;
        } else if (item.getType() != Material.SPLASH_POTION) {
            return false;
        } else if (!item.hasItemMeta()) {
            return true;
        } else if (!(item.getItemMeta() instanceof PotionMeta)) {
            return true;
        } else {
            PotionMeta potionMeta = (PotionMeta)item.getItemMeta();
            return !potionMeta.hasCustomEffects();
        }
    }
}
