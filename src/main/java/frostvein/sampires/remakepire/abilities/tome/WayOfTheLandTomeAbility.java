package frostvein.sampires.remakepire.abilities.tome;

import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class WayOfTheLandTomeAbility extends TomeAbility implements Listener {
    private static final int NO_COOLDOWN = 0;
    private final Random random = new Random();

    /**
     * Create an instance of the Way of the Land tome ability.
     *
     * @param plugin the host plugin object.
     */
    public WayOfTheLandTomeAbility(RemakepirePlugin plugin) {
        super(plugin, "WayOfTheLand", new String[]{"You gain knowledge on how to live off the land.", "You permanently have a 75% chance when harvesting a crop", "to receive double drops."}, 0);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    protected boolean useAbility(Player player) {
        if (!this.canUse(player)) {
            this.sendCannotUseMessage(player, "Only humans can use tome abilities!");
            return false;

        } else {
            this.sendSuccessMessage(player, "You have absorbed the knowledge of living off the land!");
            player.sendMessage("§7You now have a permanent 75% chance to receive double drops when harvesting crops.");
            player.sendMessage("§7This knowledge flows through your very being - you need not activate it again.");
            this.plugin.getWorld().playSound(player.getLocation(), "minecraft:block.grass.break", 1.0F, 1.2F);
            this.plugin.getWorld().playSound(player.getLocation(), "minecraft:entity.experience_orb.pickup", 0.5F, 0.8F);

            return true;
        }
    }

    /**
     * Roll the extra crop chance when breaking relevant blocks.
     *
     * @param event the block breaking event by the player.
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (this.plugin.getTomeManager().hasAbility(player, "wayoftheland")) {
            if (this.isFullyGrownCrop(block)) {
                if (block.getType() != Material.BEETROOTS) {
                    if (this.random.nextDouble() < 0.75) {
                        for(ItemStack drop : block.getDrops(player.getInventory().getItemInMainHand())) {
                            if (drop != null && drop.getType() != Material.AIR) {
                                ItemStack extraDrop = drop.clone();
                                block.getWorld().dropItemNaturally(block.getLocation(), extraDrop);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Determine if the crop being broken was fully grown.
     *
     * @param block the crop that has been broken.
     * @return {@code true} if the crop had reached maturity before being harvested.
     */
    private boolean isFullyGrownCrop(Block block) {
        switch (block.getType()) {
            case WHEAT:
            case CARROTS:
            case POTATOES:
            case BEETROOTS:
            case NETHER_WART:
                BlockData blockData = block.getBlockData();
                if (blockData instanceof Ageable ageable) {
                    return ageable.getAge() == ageable.getMaximumAge();
                }
                break;

            case PUMPKIN:
            case MELON:
            case COCOA:
                return true;

            case SWEET_BERRY_BUSH:
                BlockData berryData = block.getBlockData();
                if (berryData instanceof Ageable ageable) {
                    return ageable.getAge() >= 2;
                }
        }

        return false;
    }
}
