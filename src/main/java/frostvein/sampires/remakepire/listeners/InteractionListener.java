package frostvein.sampires.remakepire.listeners;

import java.util.Arrays;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Block;
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
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.SessionManager;

public class InteractionListener implements Listener {
    private final RemakepirePlugin plugin;
    private final SessionManager sessionManager;
    private static final List<Material> FEEDING_ITEMS;

    public InteractionListener(RemakepirePlugin plugin, SessionManager sessionManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity targetEntity = event.getRightClicked();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (this.plugin.getVampireManager().isVampire(player)) {
            if (this.plugin.getBatTransformationManager().isInBatForm(player)) {
                event.setCancelled(true);
                player.sendMessage("§cYou cannot interact with anything while you are in your bat form.");
            } else if (itemInHand != null && itemInHand.getType() != Material.AIR && FEEDING_ITEMS.contains(itemInHand.getType())) {
                if (!(targetEntity instanceof Player)) {
                    if (this.isFeedableMob(targetEntity)) {
                        this.handleVampireFeedingAttempt(player, targetEntity, itemInHand, event);
                    }

                }
            }
        }
    }

    private boolean isFeedableMob(Entity entity) {
        return entity instanceof Animals || entity instanceof Horse || entity instanceof Llama || entity instanceof Wolf || entity instanceof Cat || entity instanceof Parrot || entity instanceof Rabbit || entity instanceof Turtle || entity instanceof Fox || entity instanceof Bee || entity instanceof Strider || entity instanceof Hoglin;
    }

    private void handleVampireFeedingAttempt(Player vampire, Entity mob, ItemStack foodItem, PlayerInteractEntityEvent event) {
        this.plugin.getLogger().info("Vampire " + vampire.getName() + " attempted to feed " + String.valueOf(mob.getType()) + " with " + String.valueOf(foodItem.getType()));

        if (this.plugin.getVampireManager().isVampireStage1(vampire)) {
            vampire.sendMessage("§cThe animal tentatively eats from your hand, eyeing you suspiciously, as if it knows your true nature...");
        } else {
            vampire.sendMessage("§cThe animal recoils from you as you extend a hand to it...");
            event.setCancelled(true);
        }

    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (clickedBlock != null && this.isAnvil(clickedBlock.getType()) && this.plugin.getVampireManager().isVampire(player)) {
                event.setCancelled(true);
                player.sendMessage("§cYou cannot use anvils as a vampire.");
            }

        }
    }

    private boolean isAnvil(Material material) {
        return material == Material.ANVIL || material == Material.CHIPPED_ANVIL || material == Material.DAMAGED_ANVIL;
    }

    static {
        FEEDING_ITEMS = Arrays.asList(Material.WHEAT, Material.CARROT, Material.POTATO, Material.BEETROOT, Material.WHEAT_SEEDS, Material.MELON_SEEDS, Material.PUMPKIN_SEEDS, Material.BEETROOT_SEEDS, Material.SWEET_BERRIES, Material.APPLE, Material.GOLDEN_APPLE, Material.GOLDEN_CARROT, Material.BREAD, Material.COOKIE, Material.MELON_SLICE, Material.DRIED_KELP, Material.HAY_BLOCK, Material.SUGAR);
    }
}
