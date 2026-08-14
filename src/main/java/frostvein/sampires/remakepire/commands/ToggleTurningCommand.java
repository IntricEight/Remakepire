package frostvein.sampires.remakepire.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class ToggleTurningCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;

    /**
     * Create an instance of the plugin's toggle vampire turning command handler.
     *
     * @param plugin the host plugin object.
     */
    public ToggleTurningCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle the command execution of toggling the vampire turning ability on and off.
     *
     * @return {@code true}
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (!this.plugin.getVampireManager().isVampire(player)) {
                player.sendMessage(Component.text("Only vampires can use this command.", NamedTextColor.RED));

            } else {
                if (this.plugin.getVampireTurningManager().toggleTurning(player)) {
                    player.sendMessage(Component.text("Vampire turning enabled. You will now turn humans into vampires when you kill them.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Vampire turning disabled. Humans will die normally when you kill them.", NamedTextColor.RED));
                }
            }
        } else {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
        }

        return true;
    }
}