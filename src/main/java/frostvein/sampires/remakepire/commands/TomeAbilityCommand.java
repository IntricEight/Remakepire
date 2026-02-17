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
import frostvein.sampires.remakepire.managers.VampireManager;

public class TomeAbilityCommand implements CommandExecutor, TabCompleter {
    private final RemakepirePlugin plugin;
    private final TomeManager tomeManager;
    private final VampireManager vampireManager;

    public TomeAbilityCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.tomeManager = plugin.getTomeManager();
        this.vampireManager = plugin.getVampireManager();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use tome abilities.");
            return true;
        } else if (!this.vampireManager.isHuman(player)) {
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

    private boolean handleListCommand(Player player) {
        Set<String> playerAbilities = this.tomeManager.getPlayerAbilities(player);
        if (playerAbilities.isEmpty()) {
            player.sendMessage("§7You have not learned any tome abilities yet.");
            player.sendMessage("§7Find ancient tomes scattered throughout the world to learn new abilities.");
            return true;
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
            return true;
        }
    }

    private boolean handleAbilityUse(Player player, String abilityName) {
        boolean success = this.tomeManager.useAbility(player, abilityName);
        return !success ? true : true;
    }

    private void sendUsage(Player player) {
        player.sendMessage("§6§l=== TOME ABILITIES ===");
        player.sendMessage("§e/pow tome list §7- Show your available abilities");
        player.sendMessage("§e/pow tome <ability> §7- Use a specific ability");
        player.sendMessage("");
        player.sendMessage("§7Find ancient tomes in the world to learn new abilities.");
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return new ArrayList();
        } else {
            List<String> completions = new ArrayList();
            if (args.length == 1) {
                completions.add("list");
                Set<String> playerAbilities = this.tomeManager.getPlayerAbilities(player);
                completions.addAll(playerAbilities);
                String input = args[0].toLowerCase();
                completions.removeIf((s) -> !s.toLowerCase().startsWith(input));
            }

            return completions;
        }
    }
}
