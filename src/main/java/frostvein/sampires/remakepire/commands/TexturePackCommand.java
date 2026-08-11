package frostvein.sampires.remakepire.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.VampireTexturePackManager;

public class TexturePackCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;
    private final VampireTexturePackManager texturePackManager;

    /**
     * Create an instance of the plugin's texture pack command handler.
     *
     * @param plugin the host plugin object.
     */
    public TexturePackCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.texturePackManager = plugin.getVampireTexturePackManager();
    }

    /**
     * Handle the command execution of applying plugin texture packs to online players.
     *
     * @return {@code true}
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));

        } else {
            if (args.length > 0) {
                String subCommand = args[0].toLowerCase();

                if (player.hasPermission("vampiresmp.admin")) {
                    if (subCommand.equals("all")) {
                        this.texturePackManager.ensureAllVampiresHaveTexturePack();
                        player.sendMessage(Component.text("Applied vampire texture pack to all online vampires.", NamedTextColor.GREEN));
                        return true;

                    } else if (subCommand.equals("force")) {
                        this.texturePackManager.forceApplyVampireTexturePack(player, "admin force command");
                        return true;
                    }
                }

                if (subCommand.equals("vampire")) {
                    this.texturePackManager.applyVampireTexturePack(player, "manual command");
                    return true;

                } else if (subCommand.equals("human")) {
                    this.texturePackManager.applyHumanTexturePack(player, "manual command");
                    return true;
                }
            }

            if (!this.plugin.getVampireManager().isVampire(player)) {
                player.sendMessage(Component.text("Only vampires can apply the vampire texture pack.", NamedTextColor.RED));
                player.sendMessage(Component.text("Use ", NamedTextColor.GRAY)
                        .append(Component.text("/pow texture human", NamedTextColor.YELLOW))
                        .append(Component.text(" to apply the human texture pack.", NamedTextColor.GRAY))
                );

            } else {
                this.texturePackManager.manualApplication(player);
            }
        }

        return true;
    }
}
