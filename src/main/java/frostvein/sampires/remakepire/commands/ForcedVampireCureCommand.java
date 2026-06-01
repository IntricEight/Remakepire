package frostvein.sampires.remakepire.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.listeners.CureBookReadingListener;
import frostvein.sampires.remakepire.managers.VampireAbilityManager;
import frostvein.sampires.remakepire.managers.VampireManager;
import frostvein.sampires.remakepire.managers.VampireSireManager;

public class ForcedVampireCureCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final VampireSireManager sireManager;

    public ForcedVampireCureCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
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
                /*
                * Try out Cleo's idea for the new curing feature
                   Item to check for: Prismarine shard

                    Use a method similar to stop the bleeding, where they need to use a keybind while crouching. COpy the checking process,
                    and then bring over the force cure menu / process
                    No beacon or daylight or sire death requirement: Only requires the syringe item
                */

//                ItemStack holyWater = this.plugin.getHolyWaterEffectManager().findHolyWater(caster);

                // Retrieve the prismarine shard in either hand, prioritizing one held in the main hand
                ItemStack mainHandSyringe = caster.getInventory().getItemInMainHand(), offHandSyringe = null;

                if (mainHandSyringe.getType() != Material.PRISMARINE_SHARD) {
                    mainHandSyringe = null;

                    offHandSyringe = caster.getInventory().getItemInOffHand();

                    if (offHandSyringe.getType() != Material.PRISMARINE_SHARD) {
                        offHandSyringe = null;
                    }
                }

                /* Logic following this:
                    If the main hand has prismarine, then offhand will be null
                    If main hand does not, then main is set to null and offhand might or might not hold it

                    Conclusions:
                      - if both main and off are empty, no prismarine found
                      - if main is not null, then main is holding prismarine. No check is made for if offhand is holding prismarine, and it remains null
                      - if main is null and offhand is not null, then main is not holding prismarine and offhand is holding prismarine
                      - is both are null, then neither are holding prismarine
                 */

                // Ensure the caster has a syringe (prismarine shard) in their hands
                if (mainHandSyringe == null && offHandSyringe == null) {
                    caster.sendMessage("§cYou must hold a syringe of the sire's blood to enact the cure.");

                } else {

                    // Request that the game runner turns off sire death requirements for running the expected version of it. I won't be changing this here
                    String sireName = this.sireManager.getSire(target);
                    if (sireName != null && !this.sireManager.canBeCured(target)) {
                        caster.sendMessage("§4The curse cannot be broken while " + target.getName() + "'s sire, " + sireName + ", still walks the world in mortal form...");
                        caster.sendMessage("§4The blood bond must be severed through the maker's true death.");

                    } else {
                        // Remove the prismarine from the hand holding it, prioritizing the main hand
                        if (mainHandSyringe == null) {
                            // Remove the prismarine stack from the offhand
                            caster.getInventory().setItemInOffHand(null);
                        } else {
                            // Remove the prismarine stack from the main hand
                            caster.getInventory().setItemInMainHand(null);
                        }

                        // Logic for this: Use the feeding or Stop the Bleeding timer logic to determine if the caster is crouched next to the vampire for X time (from config)
                        // Once that time has elapsed, run the force cure
                        this.plugin.getForcedCureChoiceListener().startCureSession(caster, target);
                    }
                }

                return true;
            }
        }
    }
}