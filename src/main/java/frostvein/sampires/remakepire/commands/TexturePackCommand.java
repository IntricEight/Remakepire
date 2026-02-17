package frostvein.sampires.remakepire.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.VampireTexturePackManager;

public class TexturePackCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;
    private final VampireTexturePackManager texturePackManager;

    public TexturePackCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.texturePackManager = plugin.getVampireTexturePackManager();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        } else {
            if (args.length > 0) {
                String subCommand = args[0].toLowerCase();
                if (player.hasPermission("vampiresmp.admin")) {
                    if (subCommand.equals("all")) {
                        this.texturePackManager.ensureAllVampiresHaveTexturePack();
                        player.sendMessage("§aApplied vampire texture pack to all online vampires.");
                        return true;
                    }

                    if (subCommand.equals("force")) {
                        this.texturePackManager.forceApplyVampireTexturePack(player, "admin force command");
                        return true;
                    }
                }

                if (subCommand.equals("vampire")) {
                    this.texturePackManager.applyVampireTexturePack(player, "manual command");
                    return true;
                }

                if (subCommand.equals("human")) {
                    this.texturePackManager.applyHumanTexturePack(player, "manual command");
                    return true;
                }
            }

            if (!this.plugin.getVampireManager().isVampire(player)) {
                player.sendMessage("§cOnly vampires can apply the vampire texture pack.");
                player.sendMessage("§7Use §e/pow texture human §7to apply the human texture pack.");
                return true;
            } else {
                this.texturePackManager.manualApplication(player);
                return true;
            }
        }
    }
}
