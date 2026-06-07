package frostvein.sampires.remakepire.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.listeners.CureBookReadingListener;
import frostvein.sampires.remakepire.listeners.DeathHandler;
import frostvein.sampires.remakepire.listeners.ForcedCureChoiceListener;
import frostvein.sampires.remakepire.managers.VampireManager;
import frostvein.sampires.remakepire.managers.VampireSireManager;


public class VampireCureCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final VampireSireManager sireManager;

    public VampireCureCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.sireManager = plugin.getSireManager();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;

        } else if (!CureBookReadingListener.hasReadAllCureBooks(player)) {
            player.sendMessage("§cYou do not know these ancient words...");
            player.sendMessage("§7Within three ancient tomes lie the secrets to this ritual.");
            return true;

        } else if (!this.vampireManager.isVampire(player)) {
            player.sendMessage("§cOnly vampires can use this cure ritual.");
            return true;

        } else {
            // Retrieve the prismarine shard in either hand, prioritizing one held in the main hand
            ItemStack mainHandSyringe = null, offHandSyringe = null;

            if (player.getInventory().getItemInMainHand().getType() == Material.PRISMARINE_SHARD) {
                mainHandSyringe = player.getInventory().getItemInMainHand();

            } else if (player.getInventory().getItemInOffHand().getType() == Material.PRISMARINE_SHARD) {
                offHandSyringe = player.getInventory().getItemInOffHand();
            }

            if (mainHandSyringe == null && offHandSyringe == null) {
                player.sendMessage("§cYou must hold a syringe of the sire's blood to enact the cure.");

            } else {
                String sireName = this.sireManager.getSire(player);

                if (sireName != null && !this.sireManager.canBeCured(player)) {
                    player.sendMessage("§4The curse cannot be broken while your sire, " + sireName + ", still walks the world in mortal form...");
                    player.sendMessage("§4Only through your maker's true death can you find release.");
                } else {
                    // Once that time has elapsed, run the force cure
                    this.plugin.getForcedCureChoiceListener().startSelfCureSession(player);
                }
            }

            return true;
        }
    }
}