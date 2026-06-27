package frostvein.sampires.remakepire.commands;

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
import frostvein.sampires.remakepire.managers.VampireSireManager;

public class VampireCureCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final BeaconManager beaconManager;
    private final VampireSireManager sireManager;

    /**
     * Create an instance of the plugin's self cure command handler.
     *
     * @param plugin the host plugin object.
     */
    public VampireCureCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.beaconManager = plugin.getBeaconManager();
        this.sireManager = plugin.getSireManager();
    }

    /**
     * Handle the command execution of the self cure.
     *
     * @return {@code true}
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");

        } else if (!CureBookReadingListener.hasReadAllCureBooks(player)) {
            player.sendMessage("§cYou do not know these ancient words...");
            player.sendMessage("§7You must first read all three cure books to learn the ritual.");

        } else if (!this.vampireManager.isVampire(player)) {
            player.sendMessage("§cOnly vampires can use this cure ritual.");

        } else {
            // Only allow a cure during the day (if this setting is enabled)
            if (this.plugin.getConfigManager().doCuresRequireDaytime() && !this.plugin.getEffectManager().isDaytime(player.getWorld())) {
                player.sendMessage("§cThis ritual can only be performed during the day.");

            } else {
                ItemStack holyWater = this.plugin.getHolyWaterEffectManager().findHolyWater(player);

                // Ensure the caster has holy water in their inventory
                if (holyWater == null) {
                    player.sendMessage("§cYou need holy water to perform this ritual.");

                } else {
                    // Ensure the caster is within cure range of a holy beacon
                    double cureDistance = this.plugin.getConfigManager().getCureBeaconDistance();
                    BeaconSite nearestHolyBeacon = this.beaconManager.getNearestHolyBeacon(player.getLocation(), cureDistance);

                    if (nearestHolyBeacon == null) {
                        player.sendMessage("§cYou must be close to a holy beacon to perform this ritual.");

                    } else {
                        if (!this.sireManager.canBeCured(player)) {
                            player.sendMessage("§4The curse cannot be broken while your sire still walks the world in mortal form...");
                            player.sendMessage("§4Only through your maker's true death can you find release.");
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
        holyWater.setAmount(holyWater.getAmount() - 1);

        player.sendTitle("§6§lCURED", "§eThe curse is lifted", 10, 60, 20);
        player.sendMessage("§7The holy water burns through your veins...");
        player.sendMessage("§7The corrupted blood boils away in divine light...");
        player.sendMessage("§aYou feel your humanity returning...");
        player.sendMessage("§aYou are cured. You are human once more.");
        player.sendMessage("§8But the holy site has been permanently corrupted by your dark presence...");

        // Retrieve the messages to announce to the server population
        final String messageToHumans = this.plugin.getCureBookManager().getSelfCureAnnouncementMessage(true);
        final String messageToVampires = this.plugin.getCureBookManager().getSelfCureAnnouncementMessage(false);

        // Alert all players that a vampire has been cured
        for(Player onlinePlayer : Bukkit.getOnlinePlayers()) {
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