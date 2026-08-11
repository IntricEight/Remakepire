package frostvein.sampires.remakepire.listeners;

import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Tag;
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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import frostvein.sampires.remakepire.RemakepirePlugin;

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
     * Inform the player of the changing circumstances when they attempt to feed an animal.
     *
     * @param vampire the player attempting to feed an animal.
     * @param mob the entity that the vampire is trying to feed.
     * @param foodItem the item used to feed the animal.
     * @param event a player interacts with an entity.
     */
    private void handleVampireFeedingAttempt(Player vampire, Entity mob, ItemStack foodItem, PlayerInteractEntityEvent event) {
        this.plugin.logInfo("Vampire " + vampire.getName() + " attempted to feed " + mob.getType() + " with " + foodItem.getType());

        if (this.plugin.getVampireManager().isVampireStage1(vampire)) {
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
