package frostvein.sampires.remakepire.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.abilities.tome.TomeAbility;
import frostvein.sampires.remakepire.managers.TomeManager;

public class TomeAbilityCommand implements CommandExecutor, TabCompleter {
    private final RemakepirePlugin plugin;
    private final TomeManager tomeManager;

    /**
     * Create an instance of the plugin's tome ability command handler.
     *
     * @param plugin the host plugin object.
     */
    public TomeAbilityCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.tomeManager = plugin.getTomeManager();
    }

    /**
     * Handle the command execution of triggering a human tome ability.
     *
     * @return {@code true}
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use tome abilities.");
            return true;

        } else if (!this.plugin.getVampireManager().isHuman(player)) {
            player.sendMessage("§cOnly humans can use tome abilities.");
            return true;

        } else if (args.length == 0) {
            this.sendUsage(player);
            return true;

        } else {
            String subCommand = args[0].toLowerCase();
            return subCommand.equals("list") ? this.handleListCommand(player) : this.handleAbilityUse(player, subCommand);
        }
    }

    /**
     * Send the user a list of tome abilities that they have unlocked.
     *
     * @param player The human checking their available tome abilities.
     * @return {@code true}
     */
    private boolean handleListCommand(Player player) {
        // The player's current tome abilities
        Set<String> playerAbilities = this.tomeManager.getPlayerAbilities(player);

        if (playerAbilities.isEmpty()) {
            player.sendMessage("§7You have not learned any tome abilities yet.");
            player.sendMessage("§7Find ancient tomes scattered throughout the world to learn new abilities.");

        } else {
            player.sendMessage("§6§l=== YOUR TOME ABILITIES ===");

            for(String abilityName : playerAbilities) {
                TomeAbility ability = this.tomeManager.getAbility(abilityName);
                player.sendMessage("§e" + abilityName);

                if (ability != null) {
                    String[] descriptionLines = ability.getDescriptionLines();

                    for(String line : descriptionLines) {
                        player.sendMessage("§7  " + line);
                    }
                } else {
                    player.sendMessage("§7  No description available");
                }

                player.sendMessage("§8  Use: /pow tome " + abilityName.toLowerCase());
                player.sendMessage("");
            }

            player.sendMessage("§7Total abilities: §e" + playerAbilities.size());
        }

        return true;
    }

    /**
     * Trigger the chosen tome ability.
     *
     * @param player The human attempting to use a tome ability.
     * @param abilityName The command name of the ability.
     * @return {@code true}
     */
    private boolean handleAbilityUse(Player player, String abilityName) {
        this.tomeManager.useAbility(player, abilityName);
        return true;
    }

    /**
     * Print to the sender a list of available commands they can run using the tome command.
     *
     * @param player The human checking their options.
     */
    private void sendUsage(Player player) {
        player.sendMessage("§6§l=== TOME ABILITIES ===");
        player.sendMessage("§e/pow tome list §7- Show your available abilities");
        player.sendMessage("§e/pow tome <ability> §7- Use a specific ability");
        player.sendMessage("");
        player.sendMessage("§7Find ancient tomes in the world to learn new abilities.");
    }

    /**
     * Create the list of autocorrecting options for tome commands as they are written out in the command line.
     *
     * @param command the previous word in the argument list.
     * @return A {@code List} of options for the autocomplete to suggest.
     */
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return new ArrayList<>();

        } else {
            List<String> completions = new ArrayList<>();
            if (args.length == 1) {
                completions.add("list");

                Set<String> playerAbilities = this.tomeManager.getPlayerAbilities(player);
                completions.addAll(playerAbilities);
                completions.removeIf((s) -> !s.toLowerCase().startsWith(args[0].toLowerCase()));
            }

            return completions;
        }
    }
}
