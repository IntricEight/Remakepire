package frostvein.sampires.remakepire.listeners;

import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.SessionManager;
import frostvein.sampires.remakepire.managers.TomeManager;

public class BeetrootHarvestListener implements Listener {
    private final RemakepirePlugin plugin;
    private final SessionManager sessionManager;
    private final TomeManager tomeManager;
    private final Random random = new Random();

    public BeetrootHarvestListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.sessionManager = plugin.getSessionManager();
        this.tomeManager = plugin.getTomeManager();
    }

    @EventHandler
    public void onBeetrootHarvest(BlockBreakEvent event) {
        if (this.sessionManager.isSessionActive()) {
            if (event.getBlock().getType() == Material.BEETROOTS) {
                Ageable crop = (Ageable)event.getBlock().getBlockData();
                if (crop.getAge() == crop.getMaximumAge()) {
                    Player player = event.getPlayer();
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

    private int calculateBeetrootQuantity(Player player) {
        return this.tomeManager.hasAbility(player, "wayoftheland") && this.random.nextDouble() < 0.75 ? 2 : 1;
    }

    private void giveAlwaysEdibleBeetroot(Player harvester, int quantity) {
        try {
            if (harvester != null) {
                String command = "give " + harvester.getName() + " minecraft:beetroot[minecraft:food={can_always_eat:1b,nutrition:1,saturation:1.2}] " + quantity;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                this.plugin.getLogger().info("Gave " + quantity + " always-edible beetroot(s) to " + harvester.getName());
            } else {
                this.plugin.getLogger().warning("Harvesting player is null, cannot give beetroot");
            }
        } catch (Exception e) {
            this.plugin.getLogger().warning("Failed to give always-edible beetroot: " + e.getMessage());
        }
    }

    private void dropBeetrootSeeds(Location location, ItemStack tool) {
        int baseSeeds = this.random.nextInt(4);
        int fortuneLevel = 0;
        if (tool != null && tool.containsEnchantment(Enchantment.FORTUNE)) {
            fortuneLevel = tool.getEnchantmentLevel(Enchantment.FORTUNE);
        }

        int maxSeeds = 3 + fortuneLevel;
        int totalSeeds = this.random.nextInt(maxSeeds + 1);
        totalSeeds = Math.max(baseSeeds, totalSeeds);
        if (totalSeeds > 0) {
            ItemStack seeds = new ItemStack(Material.BEETROOT_SEEDS, totalSeeds);
            location.getWorld().dropItemNaturally(location, seeds);
        }
    }
}
