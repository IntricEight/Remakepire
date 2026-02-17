package frostvein.sampires.remakepire.listeners;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.ThirstManager;
import frostvein.sampires.remakepire.managers.VampireManager;

public class ExperienceBottleListener implements Listener {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final ThirstManager thirstManager;

    public ExperienceBottleListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.thirstManager = plugin.getThirstManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.EXPERIENCE_BOTTLE) {
            if (this.vampireManager.isVampire(player) || this.vampireManager.isHuman(player)) {
                Action action = event.getAction();
                if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                    if (this.vampireManager.isHuman(player)) {
                        event.setCancelled(true);
                        player.sendMessage("§cYou recoil at the sight of thick crimson blood rolling across the inside of the bottle... Who would collect something like this?");
                    } else {
                        event.setCancelled(true);
                        PlayerInventory inventory = player.getInventory();
                        ItemStack heldItem = inventory.getItemInMainHand();
                        if (heldItem.getType() == Material.EXPERIENCE_BOTTLE) {
                            if (heldItem.getAmount() > 1) {
                                heldItem.setAmount(heldItem.getAmount() - 1);
                            } else {
                                inventory.setItemInMainHand(new ItemStack(Material.AIR));
                            }

                            ItemStack glassBottle = new ItemStack(Material.GLASS_BOTTLE, 1);
                            if (inventory.firstEmpty() != -1) {
                                inventory.addItem(new ItemStack[]{glassBottle});
                            } else {
                                player.getWorld().dropItemNaturally(player.getLocation(), glassBottle);
                            }

                            int experienceGained = 8;
                            this.thirstManager.quenchThirst(player, experienceGained);
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cYou drain the essence from the bottle, satisfying your vampiric thirst..."));
                        }

                    }
                }
            }
        }
    }
}
