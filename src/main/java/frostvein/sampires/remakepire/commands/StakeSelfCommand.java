package frostvein.sampires.remakepire.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.listeners.DeathHandler;
import frostvein.sampires.remakepire.utils.ItemTypeChecking;

public class StakeSelfCommand {
    private final RemakepirePlugin plugin;
    private final Map<UUID, BukkitTask> stakingTasks = new HashMap<>();
    private final int SUICIDE_PERIOD_SECONDS = 5;

    /**
     * Create an instance of the plugin's vampire suicide command handler.
     *
     * @param plugin the host plugin object.
     */
    public StakeSelfCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle the command execution of attempting to stake yourself as a vampire.
     *
     * @return {@code true}
     */
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        /*
        If the sender is not a vampire, tell them that only vampires can stake themselves
        If the player is not holding a stake when the command is run, cancel it
        Require the player to run this command twice in a 5-second period
        Permakill the player regardless of their stage
         */

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));

        } else if (this.plugin.getVampireManager().isHuman(player)) {
            sender.sendMessage(Component.text("Only vampires can stake themselves.", NamedTextColor.RED));

        } else {
            // Check if the player is holding a stake
            if (!(ItemTypeChecking.isStake(player.getInventory().getItemInMainHand().getType()) || ItemTypeChecking.isStake(player.getInventory().getItemInOffHand().getType())) ) {
                sender.sendMessage(Component.text("You must hold a stake in your hand.", NamedTextColor.RED));

            } else {
                this.triggerCommand(player);
            }
        }

        return true;
    }

    /**
     * Handle the logic of determining whether this is the player's first or second time running the suicide command within the time frame.
     *
     * @param vampire the vampire staking themselves.
     */
    private void triggerCommand(Player vampire) {
        if (this.stakingTasks.containsKey(vampire.getUniqueId())) {
            this.secondTrigger(vampire);
        } else {
            this.firstTrigger(vampire);
        }
    }

    /**
     * Begin watching for the player to enter the command again.
     *
     * @param vampire the vampire attempting to stake themselves.
     */
    private void firstTrigger(Player vampire) {
        UUID playerId = vampire.getUniqueId();

        // Let the vampire know of the validation process
        vampire.sendMessage(Component.text("Run this command again within 5 seconds to confirm.", NamedTextColor.GRAY)
                .decorate(TextDecoration.ITALIC));

        // Set a timer to remove the player from the staking list if they don't send the confirmation command
        BukkitTask task = this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            this.stakingTasks.remove(playerId);
            vampire.sendMessage(Component.text("You hands begin to shake, and the stake wavers.", NamedTextColor.WHITE));
        }, SUICIDE_PERIOD_SECONDS * 20L);

        // Add the countdown tasks to the list
        this.stakingTasks.put(playerId, task);
    }

    /**
     * Permakill the player when the command is run a second time.
     *
     * @param vampire the vampire staking themselves.
     */
    private void secondTrigger(Player vampire) {
        UUID playerId = vampire.getUniqueId();

        // Cancel the timer when the player confirms
        BukkitTask task = this.stakingTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }

        vampire.sendMessage(Component.text("You steel your nerves, and drive the stake into your chest!", NamedTextColor.DARK_RED));

        // Permakill the vampire with a combat death
        this.plugin.getVampireManager().setPlayerAsVampire(vampire, 1, true);
        this.plugin.getDeathHandler().registerWoodenStakeKill(vampire, vampire);
        vampire.addScoreboardTag(DeathHandler.PERMAKILL_PROCESSING_TAG);
        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> vampire.setHealth(0.0));
    }
}