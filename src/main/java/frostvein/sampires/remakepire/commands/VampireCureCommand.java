package frostvein.sampires.remakepire.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.listeners.CureBookReadingListener;
import frostvein.sampires.remakepire.managers.VampireManager;

public class VampireCureCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;

    /**
     * Create an instance of the plugin's self cure command handler.
     *
     * @param plugin the host plugin object.
     */
    public VampireCureCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
    }

    /**
     * Handle the command execution of the self cure.
     *
     * @return {@code true}
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;

        } else if (!CureBookReadingListener.hasReadAllCureBooks(player)) {
            player.sendMessage(Component.text("You do not know these ancient words...", NamedTextColor.RED));
            player.sendMessage(Component.text("You must first read all three cure books to learn the ritual.", NamedTextColor.GRAY));
            return true;

        } else if (!this.vampireManager.isVampire(player)) {
            player.sendMessage(Component.text("Only vampires can use this cure ritual.", NamedTextColor.RED));
            return true;

        } else if (!this.plugin.getSireManager().canBeCured(player)) {
            player.sendMessage(Component.text("The curse cannot be broken while your sire still walks the world in mortal form...", NamedTextColor.DARK_RED));
            player.sendMessage(Component.text("Only through your maker's true death can you find release.", NamedTextColor.DARK_RED));
            return true;
        }

        // Only allow a cure during the day (if this setting is enabled)
        if (this.plugin.getConfigManager().doCuresRequireDaytime() && !this.plugin.getEffectManager().isDaytime(player.getWorld())) {
            player.sendMessage(Component.text("This ritual can only be performed during the day.", NamedTextColor.RED));
            return true;
        }

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
            String sireName = this.plugin.getSireManager().getSire(player);
            if (sireName != null && !this.plugin.getSireManager().canBeCured(player)) {
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