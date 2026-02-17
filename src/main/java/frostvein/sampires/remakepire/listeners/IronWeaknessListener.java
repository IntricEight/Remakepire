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
    private double repelDistance = 2.0;
    private double repelStrength = 0.5;

    public IronWeaknessListener(RemakepirePlugin plugin, VampireManager vampireManager) {
        this.plugin = plugin;
        this.vampireManager = vampireManager;
        this.ironMaterials = this.initializeIronMaterials();
        this.knockbackCooldowns = new HashMap();
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

    public Set<Material> getIronMaterials() {
        return this.ironMaterials;
    }

    private Set<Material> initializeIronMaterials() {
        Set<Material> materials = new HashSet();

        materials.add(Material.IRON_INGOT);
        materials.add(Material.IRON_NUGGET);
        materials.add(Material.RAW_IRON);
        materials.add(Material.IRON_BLOCK);
        materials.add(Material.NETHERITE_BLOCK);
        materials.add(Material.RAW_IRON_BLOCK);
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
        materials.add(Material.IRON_DOOR);
        materials.add(Material.IRON_TRAPDOOR);

        return materials;
    }

    private void scanAndRemoveIronFromInventories() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            if (this.vampireManager.isIronAffected(player)) {
                PlayerInventory inventory = player.getInventory();
                boolean foundIronItems = false;
                List<String> droppedItems = new ArrayList();
                ItemStack[] contents = inventory.getContents();

                for(int i = 0; i < contents.length; ++i) {
                    ItemStack item = contents[i];
                    if (item != null && !item.getType().isAir() && this.ironMaterials.contains(item.getType())) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                        inventory.setItem(i, (ItemStack)null);
                        int var10001 = item.getAmount();
                        droppedItems.add(var10001 + "x " + item.getType().name().replace("_", " ").toLowerCase());
                        foundIronItems = true;
                    }
                }

                ItemStack[] armor = inventory.getArmorContents();

                for(int i = 0; i < armor.length; ++i) {
                    ItemStack item = armor[i];
                    if (item != null && !item.getType().isAir() && this.ironMaterials.contains(item.getType())) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                        armor[i] = null;
                        int var13 = item.getAmount();
                        droppedItems.add(var13 + "x " + item.getType().name().replace("_", " ").toLowerCase());
                        foundIronItems = true;
                    }
                }

                inventory.setArmorContents(armor);
                ItemStack offhand = inventory.getItemInOffHand();
                if (offhand != null && !offhand.getType().isAir() && this.ironMaterials.contains(offhand.getType())) {
                    player.getWorld().dropItemNaturally(player.getLocation(), offhand);
                    inventory.setItemInOffHand((ItemStack)null);
                    int var14 = offhand.getAmount();
                    droppedItems.add(var14 + "x " + offhand.getType().name().replace("_", " ").toLowerCase());
                    foundIronItems = true;
                }

                if (foundIronItems) {
                    player.sendMessage("§cSilver in your pocket begins to burn your skin through the cloth. You involuntarily drop it to protect yourself");
                }
            }
        }

    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player)event.getWhoClicked();
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

    private void handleShiftClickIntoInventory(Player player, ItemStack item, InventoryClickEvent event) {
        player.sendMessage("You attempt to grab the item, but it burns your hand as you reach for it.");
        event.setCancelled(true);
    }

    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player)event.getEntity();
            if (this.vampireManager.isVampireStage2(player) || this.vampireManager.isVampireStage3(player)) {
                Material itemType = event.getItem().getItemStack().getType();
                if (this.ironMaterials.contains(itemType)) {
                    event.setCancelled(true);
                    if (!player.getScoreboardTags().contains("informed_pickup_item")) {
                        player.addScoreboardTag("informed_pickup_item");
                        player.sendMessage("§cThe silver you have tried to pick up burns your fingers as you touch it... Best leave it alone...");
                    }
                }

            }
        }
    }

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

    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof ThrownPotion) {
            ThrownPotion potion = (ThrownPotion)event.getEntity();
            if (potion.getShooter() instanceof Player) {
                Player player = (Player)potion.getShooter();
                if (this.vampireManager.isVampireStage2(player) || this.vampireManager.isVampireStage3(player)) {
                    ItemStack potionItem = potion.getItem();
                    if (this.isHolyWater(potionItem)) {
                        event.setCancelled(true);
                    }

                }
            }
        }
    }

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
                        long lastKnockback = (Long)this.knockbackCooldowns.get(playerId);
                        if (currentTime - lastKnockback < 1000L) {
                            return;
                        }
                    }

                    if (this.isNearIronBlock(to, this.repelDistance)) {
                        event.setCancelled(true);
                        if (!player.getScoreboardTags().contains("informed_iron_block_reply")) {
                            player.sendMessage("§cA block of silver is repelling you from this area...");
                            player.addScoreboardTag("informed_iron_block_reply");
                        }

                        Vector knockbackDirection = this.getDirectionAwayFromNearestIron(to);
                        knockbackDirection.multiply(this.repelStrength);
                        knockbackDirection.setY(Math.max(0.3, knockbackDirection.getY()));
                        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> player.setVelocity(knockbackDirection));
                        this.knockbackCooldowns.put(playerId, currentTime);
                    }

                }
            }
        }
    }

    private Vector getDirectionAwayFromNearestIron(Location location) {
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        Location nearestIron = null;
        double nearestDistance = Double.MAX_VALUE;

        for(int dx = -2; dx <= 2; ++dx) {
            for(int dy = -2; dy <= 2; ++dy) {
                for(int dz = -2; dz <= 2; ++dz) {
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
            Vector direction = location.toVector().subtract(nearestIron.toVector()).normalize();
            return direction;
        } else {
            return new Vector(0, 0, 1);
        }
    }

    public void checkIronProximity() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            if (this.vampireManager.isVampire(player)) {
                Location playerLoc = player.getLocation();
                if (this.isNearIronBlock(playerLoc, 5.0)) {
                    this.applyIronEffects(player);
                }
            }
        }

    }

    private boolean isNearIronBlock(Location location, double radius) {
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

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

    private void applyIronEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 2, false, false));
        if (!player.getScoreboardTags().contains("informed_iron_block_effects")) {
            player.addScoreboardTag("informed_iron_block_effects");
            player.sendMessage("§cA source of silver nearby is weakening you...");
        }

    }

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
