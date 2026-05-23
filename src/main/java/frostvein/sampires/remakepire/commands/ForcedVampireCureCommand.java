package frostvein.sampires.remakepire.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.beacons.BeaconSite;
import frostvein.sampires.remakepire.listeners.CureBookReadingListener;
import frostvein.sampires.remakepire.managers.BeaconManager;
import frostvein.sampires.remakepire.managers.VampireManager;
import frostvein.sampires.remakepire.managers.VampireSireManager;

public class ForcedVampireCureCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final BeaconManager beaconManager;
    private final VampireSireManager sireManager;

    public ForcedVampireCureCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.beaconManager = plugin.getBeaconManager();
        this.sireManager = plugin.getSireManager();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player caster)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;

        } else if (!CureBookReadingListener.hasReadAllCureBooks(caster)) {
            caster.sendMessage("§cYou do not know these holy words...");
            caster.sendMessage("§7You must first read all three cure books to understand this power.");
            return true;

        } else if (!CureBookReadingListener.hasReadFourthBook(caster)) {
            caster.sendMessage("§cYou do not know the words of retribution...");
            caster.sendMessage("§7You must read the fourth book to learn how to force cure others.");
            return true;

        } else if (args.length < 1) {
            caster.sendMessage("§cYou must specify the name of the vampire to sanctify.");
            caster.sendMessage("§7Usage: /hoc-vinculum-tibi-dirumpo-mala-creatura <player-name>");
            return true;

        } else {
            String targetName = args[0];
            Player target = Bukkit.getPlayer(targetName);

            if (target == null) {
                caster.sendMessage("§cPlayer '" + targetName + "' is not online or does not exist.");
                return true;

            } else if (target.equals(caster)) {
                caster.sendMessage("§cYou cannot use these holy words upon yourself. The ritual must be performed by another.");
                return true;

            } else if (!this.vampireManager.isVampire(target)) {
                caster.sendMessage("§c" + target.getName() + " is not a vampire. The holy words have no power over them.");
                return true;

            } else {
                // Only allow a cure during the day
                if (!this.plugin.getEffectManager().isDaytime(caster.getWorld())) {
                    caster.sendMessage("§cThe holy words can only be spoken during the day, when the sun's light empowers them.");

                } else {
                    ItemStack holyWater = this.plugin.getHolyWaterEffectManager().findHolyWater(caster);

                    // Ensure the caster has holy water in their inventory
                    if (holyWater == null) {
                        caster.sendMessage("§cYou need holy water to sanctify the creature with these words.");

                    } else {
                        // Ensure both caster and target are within cure range of a holy beacon
                        double cureDistance = this.plugin.getConfigManager().getCureBeaconDistance();
                        BeaconSite nearestHolyBeacon = this.beaconManager.getNearestHolyBeacon(caster.getLocation(), cureDistance);

                        // Ensure the caster is within cure range of a holy beacon
                        if (nearestHolyBeacon == null) {
                            caster.sendMessage("§cYou must be close to a holy beacon to channel the divine power of these words.");

                        } else {
                            BeaconSite targetNearestBeacon = this.beaconManager.getNearestHolyBeacon(target.getLocation(), cureDistance);

                            // Ensure the caster and target are within cure range of the same holy beacon
                            if (targetNearestBeacon != null && targetNearestBeacon.equals(nearestHolyBeacon)) {
                                String sireName = this.sireManager.getSire(target);

                                if (sireName != null && !this.sireManager.canBeCured(target)) {
                                    caster.sendMessage("§4The curse cannot be broken while " + target.getName() + "'s sire, " + sireName + ", still walks the world in mortal form...");
                                    caster.sendMessage("§4The blood bond must be severed through the maker's true death.");

                                } else {
                                    holyWater.setAmount(holyWater.getAmount() - 1);

                                    caster.sendMessage("§6You speak the holy words of retribution...");
                                    caster.sendMessage("§7Divine light tears through the creature's cursed form...");
                                    caster.sendMessage("§e" + target.getName() + " must now choose their fate...");

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
                                caster.sendMessage("§cThe creature must also be within the holy beacon's divine light for the ritual to work.");
                                caster.sendMessage("§7Both you and " + target.getName() + " must be near the same holy beacon.");
                            }
                        }
                    }
                }

                return true;
            }
        }
    }
}