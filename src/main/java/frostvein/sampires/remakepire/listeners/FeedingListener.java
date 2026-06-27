package frostvein.sampires.remakepire.listeners;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.SessionManager;
import frostvein.sampires.remakepire.managers.ThirstManager;
import frostvein.sampires.remakepire.managers.VampireManager;

public class FeedingListener implements Listener {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final ThirstManager thirstManager;

    /**
     * Create an instance of the vampire Feeding listener.
     *
     * @param plugin the host plugin object.
     */
    public FeedingListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.thirstManager = plugin.getThirstManager();
    }

    /**
     * Provide vampires with blood when they kill a valid entity, and prevent xp from spawning.
     *
     * @param event an entity dying.
     */
    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onEntityDeath(EntityDeathEvent event) {
        if (this.plugin.getSessionManager().isSessionActive()) {
            LivingEntity deadEntity = event.getEntity();

            if (!(deadEntity instanceof Player)) {
                Player killer = deadEntity.getKiller();
                ItemStack offhandItem = killer.getInventory().getItemInOffHand();

                if (killer != null && this.vampireManager.isVampire(killer)) {
                    int experienceDropped = event.getDroppedExp();
                    event.setDroppedExp(0);

                    if (this.thirstManager.isThirstQuencher(deadEntity.getType())) {
                        boolean bottleFilled = this.tryFillBottleWithBlood(killer);

                        if (!bottleFilled) {
                            this.thirstManager.handleEntityKill(killer, deadEntity.getType(), experienceDropped);

                            if (experienceDropped > 0 && !killer.getScoreboardTags().contains(SessionManager.INFORMED_SUCCESSFUL_FEEDING)) {
                                killer.addScoreboardTag(SessionManager.INFORMED_SUCCESSFUL_FEEDING);
                                killer.sendMessage("§cYou taste the metallic essence of life...");
                            }
                        }
                    }
                } else if (killer != null && this.vampireManager.isHuman(killer) && offhandItem != null && offhandItem.getType() == Material.GLASS_BOTTLE) {
                    if (this.tryFillBottleWithBlood(killer)) {
                        event.setDroppedExp(0);
                    }
                }
            }
        }
    }

    /**
     * Attempt to fill a bottle with crimson blood from an animal.
     *
     * @param killer the player attempting to fill a bottle.
     * @return {@code true} if the bottle was filled.
     */
    private boolean tryFillBottleWithBlood(Player killer) {
        PlayerInventory inventory = killer.getInventory();
        ItemStack offhandItem = inventory.getItemInOffHand();

        if (offhandItem != null && offhandItem.getType() == Material.GLASS_BOTTLE) {
            if (offhandItem.getAmount() > 1) {
                offhandItem.setAmount(offhandItem.getAmount() - 1);
            } else {
                inventory.setItemInOffHand(new ItemStack(Material.AIR));
            }

            ItemStack experienceBottle = new ItemStack(Material.EXPERIENCE_BOTTLE, 1);
            if (inventory.firstEmpty() != -1) {
                inventory.addItem(experienceBottle);
            } else {
                killer.getWorld().dropItemNaturally(killer.getLocation(), experienceBottle);
            }

            this.plugin.getSessionManager().sendActionBar(killer, "§cThe creatures blood pours freely into your open bottle.");
            return true;

        } else {
            return false;
        }
    }

    /**
     * Prevent vampires from gaining blood through regular xp gain.
     *
     * @param event a player's xp level changes.
     */
    @EventHandler(
            priority = EventPriority.LOWEST
    )
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();

        if (this.vampireManager.isVampire(player)) {
            event.setAmount(0);

            if (event.getAmount() > 0 && Math.random() < 0.1) {
                player.sendMessage("§8Such mundane activities no longer sustain your cursed existence...");
            }
        }
    }

    /**
     * Handle any unexpected or special methods of gaining experience points.
     *
     * @param vampire the player generating the xp.
     * @param amount the amount of xp.
     * @param source the source of the xp.
     */
    private void handleSpecialExperienceSource(Player vampire, int amount, String source) {}
}
