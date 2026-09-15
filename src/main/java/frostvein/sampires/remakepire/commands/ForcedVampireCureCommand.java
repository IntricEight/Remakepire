package frostvein.sampires.remakepire.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import frostvein.sampires.remakepire.RemakepirePlugin;
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
        }

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

        } else if (!this.plugin.getSireManager().canBeCured(target)) {
            caster.sendMessage(Component.text("The curse cannot be broken while " + target.getName() + "'s sire still walks the world in mortal form...", NamedTextColor.DARK_RED));
            caster.sendMessage(Component.text("The blood bond must be severed through the maker's true death.", NamedTextColor.DARK_RED));
            return true;
        }

        // Only allow a cure during the day (if this setting is enabled)
        if (this.plugin.getConfigManager().doCuresRequireDaytime() && !this.plugin.getEffectManager().isDaytime(caster.getWorld())) {
            caster.sendMessage(Component.text("The holy words can only be spoken during the day, when the sun's light empowers them.", NamedTextColor.RED));
            return true;
        }

        /*
        Try out Cleo's idea for the new curing feature
          Item to check for: Prismarine shard
            Use a method similar to stop the bleeding, where they need to use a keybind while crouching. COpy the checking process,
            and then bring over the force cure menu / process
            No beacon or daylight or sire death requirement: Only requires the syringe item
        */

        // Retrieve the prismarine shard in either hand, prioritizing one held in the main hand
        ItemStack mainHandSyringe = null, offHandSyringe = null;

        if (caster.getInventory().getItemInMainHand().getType() == Material.PRISMARINE_SHARD) {
            mainHandSyringe = caster.getInventory().getItemInMainHand();

        } else if (caster.getInventory().getItemInOffHand().getType() == Material.PRISMARINE_SHARD) {
            offHandSyringe = caster.getInventory().getItemInOffHand();
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
            caster.sendMessage(Component.text("You must hold a syringe of the sire's blood to enact the cure.", NamedTextColor.RED));

        } else {
            // Request that the game runner turns off sire death requirements for running the expected version of it. I won't be changing this here
            String sireName = this.plugin.getSireManager().getSire(target);
            if (sireName != null && !this.plugin.getSireManager().canBeCured(target)) {
                caster.sendMessage(Component.text("The curse cannot be broken while " + target.getName() + "'s sire, " + sireName + ", still walks the world in mortal form...", NamedTextColor.DARK_RED));
                caster.sendMessage(Component.text("The blood bond must be severed through the maker's true death.", NamedTextColor.DARK_RED));

            } else {
                // Once that time has elapsed, run the force cure
                this.plugin.getForcedCureChoiceListener().startForceCureSession(caster, target);
            }
        }

        return true;
    }
}