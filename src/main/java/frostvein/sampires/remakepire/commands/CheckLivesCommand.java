package frostvein.sampires.remakepire.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class CheckLivesCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;

    /**
     * Create an instance of the command manager to check how many lives the sender has remaining.
     *
     * @param plugin the host plugin object.
     */
    public CheckLivesCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Inform the sender how many lives they have remaining
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can check their remaining lives.", NamedTextColor.RED));
            return true;
        }

        // Check the player's own lives if no additional parameter is provided
        if (args.length < 1) {
            // End the command process if the player is a vampire
            if (this.plugin.getVampireManager().isVampire(player)) {
                sender.sendMessage(Component.text("An immortal creature of darkness such as yourself has no need to concern themselves with \"life counts\"...", NamedTextColor.DARK_RED));
                return true;
            }

            Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Objective deathObjective = mainScoreboard.getObjective("vsmp_death");

            if (deathObjective != null) {
                final int currentDeaths = deathObjective.getScore(player.getName()).getScore();
                final int maxDeaths = this.plugin.getConfigManager().getHumanLifeCount();

                // Send a message to the player depending on their life counter
                if (currentDeaths < maxDeaths) {
                    sender.sendMessage(Component.text(maxDeaths - currentDeaths, NamedTextColor.GOLD)
                            .append(Component.text(" lives remaining", NamedTextColor.GRAY)));
                } else {
                    sender.sendMessage(Component.text("No lives remain... Be careful!", NamedTextColor.RED));
                }
            }
        } else {
            if (sender.hasPermission("vampiresmp.admin")) {
                Player target = Bukkit.getPlayerExact(args[0]);

                if (target == null) {
                    sender.sendMessage(Component.text("Player '" + args[0] + "' is not online or does not exist.", NamedTextColor.RED));
                    return true;
                }

                // End the command process if the target is a vampire
                if (this.plugin.getVampireManager().isVampire(target)) {
                    sender.sendMessage(Component.text("Immortal creatures of darkness like " + args[0] + " are not restrained by life counts.", NamedTextColor.RED));
                    return true;
                }

                Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                Objective deathObjective = mainScoreboard.getObjective("vsmp_death");

                if (deathObjective != null) {
                    final int currentDeaths = deathObjective.getScore(target.getName()).getScore();
                    final int maxDeaths = this.plugin.getConfigManager().getHumanLifeCount();

                    // Send a message to the admin about the target's remaining lives
                    if (currentDeaths < maxDeaths) {
                        sender.sendMessage(Component.text(args[0],  NamedTextColor.YELLOW)
                                .append(Component.text(" has ", NamedTextColor.GRAY))
                                .append(Component.text(maxDeaths - currentDeaths, NamedTextColor.YELLOW))
                                .append(Component.text(" lives remaining.", NamedTextColor.GRAY)));
                    } else {
                        sender.sendMessage(Component.text(args[0], NamedTextColor.YELLOW)
                                .append(Component.text(" has no lives remaining.", NamedTextColor.GRAY)));
                    }
                }
            }
        }

        return true;
    }
}