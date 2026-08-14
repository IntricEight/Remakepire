package frostvein.sampires.remakepire.commands;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.abilities.StormCallAbility;
import frostvein.sampires.remakepire.abilities.VampireAbility;
import frostvein.sampires.remakepire.beacons.BeaconSite;
import frostvein.sampires.remakepire.managers.BeaconManager;
import frostvein.sampires.remakepire.managers.VampireAbilityManager;

public class VampireAbilityCommand implements CommandExecutor, TabCompleter {
    private final RemakepirePlugin plugin;
    private final VampireAbilityManager abilityManager;
    private final BeaconManager beaconManager;

    /**
     * Create an instance of the plugin's vampire ability command handler.
     *
     * @param plugin the host plugin object.
     */
    public VampireAbilityCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.abilityManager = plugin.getVampireAbilityManager();
        this.beaconManager = plugin.getBeaconManager();
    }

    /**
     * Handle the command execution of triggering a vampire ability.
     *
     * @return {@code true}
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use vampire abilities.", NamedTextColor.RED));
            return true;

        } else if (args.length == 0) {
            this.sendHelpMessage(player);
            return true;

        } else {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("list")) {
                this.listAbilities(player);
                return true;

            } else if (subCommand.equals("all")) {
                this.listAllAbilities(player);
                return true;

            } else if (!this.plugin.getVampireManager().isVampire(player)) {
                player.sendMessage(Component.text("Only vampires can use vampire abilities.", NamedTextColor.RED));
                return true;

            } else if (player.getGameMode() == GameMode.SPECTATOR) {
                player.sendMessage(Component.text("You cannot use vampire abilities while in spectator mode.", NamedTextColor.RED));
                return true;

            } else {
                BeaconSite suppressingBeacon = this.beaconManager.checkHolySuppression(player.getLocation());

                if (suppressingBeacon == null || subCommand.equals("vanish") && player.hasPotionEffect(PotionEffectType.INVISIBILITY) || subCommand.equals("bat") && this.plugin.getBatTransformationManager().isInBatForm(player)) {
                    if (subCommand.equals("bat") && this.plugin.getBatTransformationManager().isInBatForm(player)) {
                        VampireAbility batAbility = this.abilityManager.getAbility("bat");

                        if (batAbility != null && batAbility.canUse(player, this.plugin.getVampireManager())) {
                            batAbility.execute(player, this.plugin.getVampireManager(), this.plugin);
                            return true;
                        }
                    }

                    if (!this.abilityManager.useAbility(player, subCommand) && this.abilityManager.getAbility(subCommand) == null) {
                        player.sendMessage(Component.text("Unknown ability: " + subCommand, NamedTextColor.RED));
                        player.sendMessage(Component.text("Use '/pow vability list' to see available abilities.", NamedTextColor.YELLOW));
                    }
                } else {
                    Location beaconLoc = suppressingBeacon.getLocation();
                    beaconLoc.distance(player.getLocation());
                    player.sendMessage(Component.text("A divine energy interferes with your dark powers, it should be snuffed out.", NamedTextColor.GRAY));
                }

                return true;
            }
        }
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(Component.text("=== VAMPIRE ABILITIES ===", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
        player.sendMessage("§e/pow vability list §7- Show your available abilities");
        player.sendMessage("§e/pow vability all §7- Show all abilities (including locked ones)");
        player.sendMessage("§e/pow vability <ability> §7- Use an ability");
        player.sendMessage(Component.text("Example: ", NamedTextColor.GRAY)
                .append(Component.text("/pow vability lunge", NamedTextColor.YELLOW)));

        if (this.plugin.getVampireManager().isVampire(player)) {
            BeaconSite suppressingBeacon = this.beaconManager.checkHolySuppression(player.getLocation());

            if (suppressingBeacon != null) {
                player.sendMessage("");
                player.sendMessage(Component.text("WARNING:", NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
                        .append(Component.text(" Holy beacon nearby - abilities suppressed.", NamedTextColor.RED)
                                .decoration(TextDecoration.BOLD, false))
                );
                player.sendMessage(Component.text("Move away from ", NamedTextColor.GRAY)
                        .append(Component.text(suppressingBeacon.getName(), NamedTextColor.WHITE))
                        .append(Component.text(" to use your abilities.", NamedTextColor.GRAY))
                );
            }
        }
    }

    /**
     * Send the user a list of vampire abilities the player can use at their stage.
     *
     * @param player The player checking their available abilities.
     */
    private void listAbilities(Player player) {
        if (!this.plugin.getVampireManager().isVampire(player)) {
            player.sendMessage(Component.text("You must be a vampire to see abilities.", NamedTextColor.RED));

        } else {
            List<VampireAbility> availableAbilities = this.abilityManager.getAvailableAbilities(player);
            int playerStage = this.plugin.getVampireManager().getVampireStage(player);

            if (availableAbilities.isEmpty()) {
                player.sendMessage(Component.text("No abilities available for Stage " + playerStage + " vampires.", NamedTextColor.RED));
                player.sendMessage(Component.text("Use '/pow vability all' to see what abilities you could unlock.", NamedTextColor.GRAY));

            } else {
                player.sendMessage(Component.text("=== YOUR VAMPIRE ABILITIES ===", NamedTextColor.DARK_RED)
                        .decorate(TextDecoration.BOLD));
                player.sendMessage(Component.text("Your Stage: ", NamedTextColor.GRAY)
                        .append(Component.text(playerStage, NamedTextColor.YELLOW)));
                BeaconSite suppressingBeacon = this.beaconManager.checkHolySuppression(player.getLocation());

                if (suppressingBeacon != null) {
                    player.sendMessage(Component.text(" SUPPRESSED by holy beacon: ", NamedTextColor.RED)
                            .append(Component.text(suppressingBeacon.getName(), NamedTextColor.WHITE)));
                }

                player.sendMessage("");

                for (VampireAbility ability : availableAbilities) {
                    this.displayAbility(player, ability, true);
                }

                player.sendMessage(Component.text("Use '/pow vability all' to see locked abilities.", NamedTextColor.GRAY));
            }
        }
    }

    /**
     * Send the user a list of all vampire abilities available in the plugin.
     *
     * @param player The player checking their available abilities.
     */
    private void listAllAbilities(Player player) {
        if (!this.plugin.getVampireManager().isVampire(player)) {
            player.sendMessage(Component.text("You must be a vampire to see abilities.", NamedTextColor.RED));

        } else {
            player.sendMessage(Component.text("=== ALL VAMPIRE ABILITIES ===", NamedTextColor.DARK_RED)
                    .decorate(TextDecoration.BOLD));
            player.sendMessage(Component.text("Your Stage: ", NamedTextColor.GRAY)
                    .append(Component.text(this.plugin.getVampireManager().getVampireStage(player), NamedTextColor.YELLOW)));
            BeaconSite suppressingBeacon = this.beaconManager.checkHolySuppression(player.getLocation());

            if (suppressingBeacon != null) {
                player.sendMessage(Component.text(" SUPPRESSED by holy beacon: ", NamedTextColor.YELLOW)
                        .append(Component.text(suppressingBeacon.getName(), NamedTextColor.WHITE)));
            }

            player.sendMessage("");

            for (VampireAbility ability : this.abilityManager.getAllAbilities()) {
                this.displayAbility(player, ability, ability.canUse(player, this.plugin.getVampireManager()));
            }
        }
    }

    /**
     * Inform the player of an ability's current status.
     *
     * @param player The vampire checking the ability.
     * @param ability The ability being displayed.
     * @param canUse {@code true} if the player use this ability.
     */
    private void displayAbility(Player player, VampireAbility ability, boolean canUse) {
        String status, nameColor = canUse ? "§e" : "§8";
        BeaconSite suppressingBeacon = this.beaconManager.checkHolySuppression(player.getLocation());
        boolean suppressed = suppressingBeacon != null && this.plugin.getVampireManager().isVampire(player);

        if (canUse && !suppressed) {
            if (ability.getName().equals("bat") && this.plugin.getBatTransformationManager().isInBatForm(player)) {
                int remainingTime = this.plugin.getBatTransformationManager().getRemainingTime(player);
                status = " §a(In Bat Form - " + VampireAbilityManager.formatTime(remainingTime) + " remaining)";

            } else if (ability instanceof StormCallAbility) {   // If more global abilities are introduced, this will have to change from being hard-coded
                if (this.abilityManager.isOnGlobalCooldown(ability.getName())) {
                    long remaining = this.abilityManager.getRemainingGlobalCooldown(ability.getName());
                    String globalInfo = this.abilityManager.getGlobalCooldownInfo(ability.getName());
                    status = " §c(Global Cooldown: " + VampireAbilityManager.formatTime(remaining) + ")";

                    if (globalInfo != null) {
                        String[] parts = globalInfo.split("\\(last used by ");

                        if (parts.length > 1) {
                            String lastUser = parts[1].replace(")", "");
                            status = " §c(Global: " + VampireAbilityManager.formatTime(remaining) + " - " + lastUser + ")";
                        }
                    }
                } else {
                    status = " §a(Ready - Global Ability)";
                }

            } else if (this.abilityManager.isOnCooldown(player, ability.getName())) {
                long remaining = this.abilityManager.getRemainingCooldown(player, ability.getName());
                status = " §c(Cooldown: " + VampireAbilityManager.formatTime(remaining) + ")";

            } else {
                status = " §a(Ready)";
            }

        } else if (suppressed && canUse) {
            status = " §c(SUPPRESSED by Holy Power)";
            nameColor = "§8";

        } else {
            status = " §c(Locked - Requires Stage " + ability.getMinimumStage() + ")";
        }

        player.sendMessage(nameColor + ability.getDisplayName() + status);
        player.sendMessage("  §7" + ability.getDescription());
        String cooldownInfo = "  §7Required Stage: §e" + ability.getMinimumStage() + " §7| Cooldown: §e" + VampireAbilityManager.formatTime(ability.getCooldownSeconds(this.plugin));

        // If more global abilities are introduced, this will have to change from being hard coded
        if (ability instanceof StormCallAbility) {
            cooldownInfo = cooldownInfo + " §c(Global)";
        }

        player.sendMessage(cooldownInfo);

        if (canUse && !suppressed) {
            if (ability.getName().equals("bat") && this.plugin.getBatTransformationManager().isInBatForm(player)) {
                player.sendMessage("  §7Usage: §e/pow vability " + ability.getName() + " §7(to transform back)");

            } else {
                player.sendMessage("  §7Usage: §e/pow vability " + ability.getName());
            }
        } else if (suppressed) {
            player.sendMessage(Component.text("  ✦ Blocked by holy beacon within " + (this.plugin.getConfigManager().getCureBeaconDistance()) + " blocks", NamedTextColor.RED));
        }

        player.sendMessage("");
    }

    /**
     * Create the list of autocorrecting options for vability commands as they are written out in the command line.
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
                completions.add("all");

                if (this.plugin.getVampireManager().isVampire(player)) {
                    List<VampireAbility> availableAbilities = this.abilityManager.getAvailableAbilities(player);
                    completions.addAll(availableAbilities.stream().map(VampireAbility::getName).toList());
                }
            }

            if (args.length > 0) {
                String input = args[args.length - 1].toLowerCase();
                completions.removeIf((s) -> !s.toLowerCase().startsWith(input));
            }

            return completions;
        }
    }
}