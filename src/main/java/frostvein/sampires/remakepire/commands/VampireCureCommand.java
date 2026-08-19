package frostvein.sampires.remakepire.commands;

import java.time.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.beacons.BeaconSite;
import frostvein.sampires.remakepire.beacons.BeaconSite.BeaconState;
import frostvein.sampires.remakepire.listeners.CureBookReadingListener;
import frostvein.sampires.remakepire.listeners.DeathHandler;
import frostvein.sampires.remakepire.managers.BeaconManager;
import frostvein.sampires.remakepire.managers.VampireManager;

public class VampireCureCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final BeaconManager beaconManager;

    /**
     * Create an instance of the plugin's self cure command handler.
     *
     * @param plugin the host plugin object.
     */
    public VampireCureCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.beaconManager = plugin.getBeaconManager();
    }

    /**
     * Handle the command execution of the self cure.
     *
     * @return {@code true}
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));

        } else if (!CureBookReadingListener.hasReadAllCureBooks(player)) {
            player.sendMessage(Component.text("You do not know these ancient words...", NamedTextColor.RED));
            player.sendMessage(Component.text("You must first read all three cure books to learn the ritual.", NamedTextColor.GRAY));

        } else if (!this.vampireManager.isVampire(player)) {
            player.sendMessage(Component.text("Only vampires can use this cure ritual.", NamedTextColor.RED));

        } else {
            // Only allow a cure during the day (if this setting is enabled)
            if (this.plugin.getConfigManager().doCuresRequireDaytime() && !this.plugin.getEffectManager().isDaytime(player.getWorld())) {
                player.sendMessage(Component.text("This ritual can only be performed during the day.", NamedTextColor.RED));

            } else {
                ItemStack holyWater = this.plugin.getHolyWaterEffectManager().findHolyWater(player);

                // Ensure the caster has holy water in their inventory and check if the player is affected by holy water
                if (holyWater == null && !this.plugin.getHolyWaterEffectManager().isAbilitiesDisabled(player)) {
                    player.sendMessage(Component.text("You need holy water to perform this ritual.", NamedTextColor.RED));

                } else {
                    // Ensure the caster is within cure range of a holy beacon
                    final double cureDistance = this.plugin.getConfigManager().getCureBeaconDistance();
                    BeaconSite nearestHolyBeacon = this.beaconManager.getNearestHolyBeacon(player.getLocation(), cureDistance);

                    if (nearestHolyBeacon == null) {
                        player.sendMessage(Component.text("You must be close to a holy beacon to perform this ritual.", NamedTextColor.RED));

                    } else {
                        if (!this.plugin.getSireManager().canBeCured(player)) {
                            player.sendMessage(Component.text("The curse cannot be broken while your sire still walks the world in mortal form...", NamedTextColor.DARK_RED));
                            player.sendMessage(Component.text("Only through your maker's true death can you find release.", NamedTextColor.DARK_RED));
                        } else {
                            this.performCure(player, holyWater, nearestHolyBeacon);
                        }
                    }
                }
            }
        }

        return true;
    }

    /**
     * Cure the player of vampirism and destroy the beacon and holy water used for the process.
     *
     * @param player the vampire being cured.
     * @param holyWater the bottle of holy water being expended.
     * @param holyBeacon the beacon being used for the cure.
     */
    private void performCure(Player player, ItemStack holyWater, BeaconSite holyBeacon) {
        // If holyWater is null, then this player must be affected by an active holy water effect
        if (holyWater != null) {
            holyWater.setAmount(holyWater.getAmount() - 1);
        }

        player.showTitle(Title.title(
                Component.text("CURED", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text("The curse is lifted", NamedTextColor.YELLOW),
                Title.Times.times(
                        // 50 milliseconds in a tick, 20 ticks in a second
                        Duration.ofMillis(10 * 50),     // 1/2 of a second
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(1)
                )
        ));

        player.sendMessage(Component.text("The holy water burns through your veins...", NamedTextColor.GRAY));
        player.sendMessage(Component.text("The corrupted blood boils away in divine light...", NamedTextColor.GRAY));
        player.sendMessage(Component.text("You feel your humanity returning...", NamedTextColor.GREEN));
        player.sendMessage(Component.text("You are cured. You are human once more.", NamedTextColor.GREEN));
        player.sendMessage(Component.text("But the holy site has been permanently corrupted by your dark presence...", NamedTextColor.DARK_GRAY));

        // Retrieve the messages to announce to the server population
        final String messageToHumans = this.plugin.getCureBookManager().getSelfCureAnnouncementMessage(true);
        final String messageToVampires = this.plugin.getCureBookManager().getSelfCureAnnouncementMessage(false);

        // Alert all players that a vampire has been cured
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(player)) {
                if (this.vampireManager.isVampire(onlinePlayer)) {
                    onlinePlayer.sendMessage(messageToVampires);
                } else {
                    onlinePlayer.sendMessage(messageToHumans);
                }
            }
        }

        this.vampireManager.setPlayerAsHuman(player);
        player.getActivePotionEffects().forEach((effect) -> player.removePotionEffect(effect.getType()));
        player.addScoreboardTag(VampireManager.CURED_VAMPIRE_TAG);

        // Check for and apply the effects of beacon control
        if (this.plugin.getSessionManager().isHumansFinalStandActive()) {
            // Restore the human's health when humans control all beacons
            this.plugin.getEffectManager().removeHumansFinalStandHealthReduction(player);

        } else if (this.plugin.getSessionManager().isVampiresEternalNightActive()) {
            // Apply blindness to the human if vampires control all beacons
            this.plugin.getEffectManager().applyEternalNightDarkness(player);
        }

        // Create the visual and audio effects of the cure working on the vampire
        this.plugin.getForcedCureChoiceManager().createCureEffects(player);
        this.plugin.getForcedCureChoiceManager().createBeaconCorruptionEffects(player, holyBeacon);
        holyBeacon.setState(BeaconState.PERMANENTLY_DESECRATED);

        this.beaconManager.updateBeaconDisplay(holyBeacon);
        this.beaconManager.saveBeacons();
        this.plugin.getBeaconMajorityManager().updateBeaconMajorityBonuses();
        this.beaconManager.checkAndBroadcastCompleteControl();
        this.plugin.getBeaconConversionListener().triggerIfAllBeaconsEvil();

        if (this.plugin.getVampireTurningManager() != null) {
            this.plugin.getVampireTurningManager().disableAllVampireTurning();
        }

        this.plugin.logInfo("VAMPIRE CURE: " + player.getName() + " has been cured at beacon: " + holyBeacon.getName());
        DeathHandler.checkAndAnnounceTeamElimination(this.plugin, false, true);
    }
}