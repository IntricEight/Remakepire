package frostvein.sampires.remakepire.listeners;

import java.util.Map;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.FoodComponent;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.SessionManager;
import frostvein.sampires.remakepire.managers.TomeManager;

public class BeetrootHarvestListener implements Listener {
    private final RemakepirePlugin plugin;
    private final SessionManager sessionManager;
    private final TomeManager tomeManager;
    private final Random random = new Random();

    /**
     * Create an instance of the Beetroot "garlic" Harvest listener.
     *
     * @param plugin the host plugin object.
     */
    public BeetrootHarvestListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.sessionManager = plugin.getSessionManager();
        this.tomeManager = plugin.getTomeManager();
    }

    /**
     * Recreate the garlic harvesting process in a controlled manner
     *
     * @param event a block being broken.
     */
    @EventHandler
    public void onBeetrootHarvest(BlockBreakEvent event) {
        if (this.sessionManager.isSessionActive()) {
            if (event.getBlock().getType() == Material.BEETROOTS) {
                Ageable crop = (Ageable)event.getBlock().getBlockData();

                if (crop.getAge() == crop.getMaximumAge()) {
                    Player player = event.getPlayer();

                    // Stop bats from harvesting beetroot
                    if (this.plugin.getBatTransformationManager().isInBatForm(player)) {
                        event.setCancelled(true);

                    } else {
                        event.setDropItems(false);
                        Location dropLocation = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
                        int beetrootQuantity = this.calculateBeetrootQuantity(player);
                        this.giveAlwaysEdibleBeetroot(player, beetrootQuantity);
                        this.dropBeetrootSeeds(dropLocation, player.getInventory().getItemInMainHand());
                    }
                }
            }
        }
    }

    /**
     * Calculate the garlic drops based on whether the player has a crop drop booster tome.
     *
     * @param player the player who broke the beetroot plant.
     * @return An {@code int} of garlic to return to the player.
     */
    private int calculateBeetrootQuantity(Player player) {
        return this.tomeManager.hasAbility(player, "wayoftheland") && this.random.nextDouble() < 0.75 ? 2 : 1;
    }

    /**
     * Give the harvested garlic items straight into the players inventory, unless it is full.<br/>
     * Make the garlic always edible, not just when a player is hungry.
     *
     * @param harvester the player who collected the beetroot plant.
     * @param quantity the amount of garlic that the player will be given.
     */
    private void giveAlwaysEdibleBeetroot(Player harvester, int quantity) {
        try {
            if (harvester != null) {
                ItemStack beetroot = new ItemStack(Material.BEETROOT, quantity);

                ItemMeta meta = beetroot.getItemMeta();
                if (meta != null) {
                    FoodComponent food = meta.getFood();

                    food.setCanAlwaysEat(true);
                    food.setNutrition(1);
                    food.setSaturation(1.2f);

                    meta.setFood(food);
                    beetroot.setItemMeta(meta);
                }

                Map<Integer, ItemStack> leftovers = harvester.getInventory().addItem(beetroot);

                for (ItemStack leftover : leftovers.values()) {
                    harvester.getWorld().dropItemNaturally(harvester.getLocation(), leftover);
                }

                this.plugin.logInfo("Gave " + quantity + " always-edible beetroot(s) to " + harvester.getName());

            } else {
                this.plugin.getLogger().warning("Harvesting player is null, cannot give beetroot");
            }
        } catch (Exception e) {
            this.plugin.getLogger().warning("Failed to give always-edible beetroot: " + e.getMessage());
        }
    }

    /**
     * Calculate the garlic seed yield based on the enchantments used.
     *
     * @param location the location where items should be spawned.
     * @param tool the tool used to harvest the beetroot plant.
     */
    private void dropBeetrootSeeds(Location location, ItemStack tool) {
        final int baseSeeds = this.random.nextInt(4);
        int fortuneLevel = 0;

        if (tool != null && tool.containsEnchantment(Enchantment.FORTUNE)) {
            fortuneLevel = tool.getEnchantmentLevel(Enchantment.FORTUNE);
        }

        final int maxSeeds = 3 + fortuneLevel;
        int totalSeeds = this.random.nextInt(maxSeeds + 1);
        totalSeeds = Math.max(baseSeeds, totalSeeds);

        if (totalSeeds > 0) {
            ItemStack seeds = new ItemStack(Material.BEETROOT_SEEDS, totalSeeds);
            location.getWorld().dropItemNaturally(location, seeds);
        }
    }
}
