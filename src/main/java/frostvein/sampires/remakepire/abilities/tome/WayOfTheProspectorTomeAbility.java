package frostvein.sampires.remakepire.abilities.tome;

import java.util.Random;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class WayOfTheProspectorTomeAbility extends TomeAbility implements Listener {
    private final Random random = new Random();
    private static final Set<Material> ORE_MATERIALS = Set.of(Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE, Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE, Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE, Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE, Material.ANCIENT_DEBRIS);;

    /**
     * Create an instance of the Way of the Land tome ability.
     *
     * @param plugin the host plugin object.
     */
    public WayOfTheProspectorTomeAbility(RemakepirePlugin plugin) {
        super(plugin, "WayOfTheProspector", new String[]{"You gain knowledge on how to extract value from the very earth beneath you.", "You permanently gain a 50% chance for an ore", "to drop twice its drops."}, 0);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    protected boolean useAbility(Player player) {
        if (!this.canUse(player)) {
            this.sendCannotUseMessage(player, "Only humans can use tome abilities!");
            return false;

        } else {
            this.sendSuccessMessage(player, "You have absorbed the knowledge of the prospector!");
            player.sendMessage("§7You now have a permanent 50% chance to receive double drops when mining ores.");
            player.sendMessage("§7This knowledge flows through your very being - you need not activate it again.");

            this.plugin.getWorld().playSound(player.getLocation(), "minecraft:block.stone.break", 1.0F, 1.2F);
            this.plugin.getWorld().playSound(player.getLocation(), "minecraft:entity.experience_orb.pickup", 0.5F, 0.8F);

            return true;
        }
    }

    /**
     * Roll the extra crop chance when breaking relevant blocks.
     *
     * @param event a block being broken.
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (this.plugin.getTomeManager().hasAbility(player, "wayoftheprospector")) {
            if (ORE_MATERIALS.contains(block.getType())) {
                ItemStack tool = player.getInventory().getItemInMainHand();

                if (!tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
                    if (this.random.nextDouble() < 0.5) {
                        for(ItemStack drop : block.getDrops(tool)) {
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
}
