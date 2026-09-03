package frostvein.sampires.remakepire.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.beacons.BeaconSite;
import frostvein.sampires.remakepire.listeners.CureBookReadingListener;

public class ForcedVampireCureCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;

    /**
     * Create an instance of the plugin's forced cure command handler.
     *
     * @param plugin the host plugin object.
     */
    public ForcedVampireCureCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle the command execution of triggering the force cure.
     *
     * @return {@code true} if the command didn't trigger a fatal error.
     */
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player caster)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;

        } else if (!CureBookReadingListener.hasReadAllCureBooks(caster)) {
            caster.sendMessage(Component.text("You do not know these holy words...", NamedTextColor.RED));
            caster.sendMessage(Component.text("You must first read all three cure books to understand this power.", NamedTextColor.GRAY));
            return true;

        } else if (!CureBookReadingListener.hasReadFourthBook(caster)) {
            caster.sendMessage(Component.text("You do not know the words of retribution...", NamedTextColor.RED));
            caster.sendMessage(Component.text("You must read the fourth book to learn how to force cure others.", NamedTextColor.GRAY));
            return true;

        } else if (args.length < 1) {
            caster.sendMessage(Component.text("You must specify the name of the vampire to sanctify.", NamedTextColor.RED));
            caster.sendMessage(Component.text("Usage: /hoc-vinculum-tibi-dirumpo-mala-creatura <player-name>", NamedTextColor.GRAY));
            return true;

        } else {
            final String targetName = args[0];
            Player target = Bukkit.getPlayerExact(targetName);

            if (target == null) {
                caster.sendMessage(Component.text("Player '" + targetName + "' is not online or does not exist.", NamedTextColor.RED));
                return true;

            } else if (target.equals(caster)) {
                caster.sendMessage(Component.text("You cannot use these holy words upon yourself. The ritual must be performed by another.", NamedTextColor.RED));
                return true;

            } else if (!this.plugin.getVampireManager().isVampire(target)) {
                caster.sendMessage(Component.text(target.getName() + " is not a vampire. The holy words have no power over them.", NamedTextColor.RED));
                return true;

            } else {
                // Only allow a cure during the day (if this setting is enabled)
                if (this.plugin.getConfigManager().doCuresRequireDaytime() && !this.plugin.getEffectManager().isDaytime(caster.getWorld())) {
                    caster.sendMessage(Component.text("The holy words can only be spoken during the day, when the sun's light empowers them.", NamedTextColor.RED));

                } else {
                    ItemStack holyWater = this.plugin.getHolyWaterEffectManager().findHolyWater(caster);

                    // Ensure the caster has holy water in their inventory and check if the target is affected by holy water
                    if (holyWater == null && !this.plugin.getHolyWaterEffectManager().isAbilitiesDisabled(caster)) {
                        caster.sendMessage(Component.text("You need holy water to sanctify the creature with these words.", NamedTextColor.RED));

                    } else {
                        // Ensure both caster and target are within cure range of a holy beacon
                        final double cureDistance = this.plugin.getConfigManager().getCureBeaconDistance();
                        BeaconSite nearestHolyBeacon = this.plugin.getBeaconManager().getNearestHolyBeacon(caster.getLocation(), cureDistance);

                        // Ensure the caster is within cure range of a holy beacon
                        if (nearestHolyBeacon == null) {
                            caster.sendMessage(Component.text("You must be close to a holy beacon to channel the divine power of these words.", NamedTextColor.RED));

                        } else {
                            BeaconSite targetNearestBeacon = this.plugin.getBeaconManager().getNearestHolyBeacon(target.getLocation(), cureDistance);

                            // Ensure the caster and target are within cure range of the same holy beacon
                            if (targetNearestBeacon != null && targetNearestBeacon.equals(nearestHolyBeacon)) {
                                if (!this.plugin.getSireManager().canBeCured(target)) {
                                    caster.sendMessage(Component.text("The curse cannot be broken while " + target.getName() + "'s sire still walks the world in mortal form...", NamedTextColor.DARK_RED));
                                    caster.sendMessage(Component.text("The blood bond must be severed through the maker's true death.", NamedTextColor.DARK_RED));

                                } else {
                                    // If holyWater is null, then the target must be affected by an active holy water effect
                                    if (holyWater != null) {
                                        holyWater.setAmount(holyWater.getAmount() - 1);
                                    }

                                    caster.sendMessage(Component.text("You speak the holy words of retribution...", NamedTextColor.GOLD));
                                    caster.sendMessage(Component.text("Divine light tears through the creature's cursed form...", NamedTextColor.GRAY));
                                    caster.sendMessage(Component.text(target.getName() + " must now choose their fate...", NamedTextColor.YELLOW));

                                    Location targetLoc = target.getLocation();

                                    targetLoc.getWorld().spawnParticle(Particle.END_ROD, targetLoc.clone().add(0.0, 1.0, 0.0), 50, 0.3, 1.0, 0.3, 0.1);
                                    targetLoc.getWorld().spawnParticle(Particle.ENCHANT, targetLoc.clone().add(0.0, 1.0, 0.0), 60, 0.5, 1.5, 0.5, 0.5);
                                    targetLoc.getWorld().spawnParticle(Particle.WHITE_ASH, targetLoc.clone().add(0.0, 1.0, 0.0), 40, 0.4, 1.2, 0.4, 0.05);
                                    targetLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, targetLoc, 1, 0.0, 0.0, 0.0, 0.0);

                                    targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1.5F, 1.0F);
                                    targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0F, 1.2F);
                                    targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.5F, 1.5F);

                                    this.plugin.getForcedCureChoiceManager().openChoiceGUI(caster, target, nearestHolyBeacon);
                                }
                            } else {
                                caster.sendMessage(Component.text("The creature must also be within the holy beacon's divine light for the ritual to work.", NamedTextColor.RED));
                                caster.sendMessage(Component.text("Both you and " + target.getName() + " must be near the same holy beacon.", NamedTextColor.GRAY));
                            }
                        }
                    }
                }

                return true;
            }
        }
    }
}