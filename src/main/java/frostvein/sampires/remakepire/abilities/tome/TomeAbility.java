package frostvein.sampires.remakepire.abilities.tome;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.VampireAbilityManager;

public abstract class TomeAbility {
    protected final RemakepirePlugin plugin;
    protected final String name;
    protected final String[] descriptionLines;
    protected final int cooldownSeconds;
    private static final Map<UUID, Map<String, Long>> playerCooldowns = new HashMap<>();
    private static final Map<String, BukkitTask> cooldownNotificationTasks = new HashMap<>();

    /**
     * Create an instance of a Tome Ability.
     *
     * @param plugin the host plugin object.
     * @param name the name of the ability.
     * @param descriptionLines the description of the ability.
     * @param cooldownSeconds the seconds between repeated ability uses (per player).
     */
    public TomeAbility(RemakepirePlugin plugin, String name, String[] descriptionLines, int cooldownSeconds) {
        this.plugin = plugin;
        this.name = name;
        this.descriptionLines = descriptionLines;
        this.cooldownSeconds = cooldownSeconds;
    }

    /**
     * Retrieve the tome ability's name.
     *
     * @return A {@code String} of the ability name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Retrieve the tome ability's array-separated description.
     *
     * @return A {@code String[]} of the ability description.
     */
    public String[] getDescriptionLines() {
        return this.descriptionLines;
    }

    /**
     * Retrieve the tome ability's description in a single line.
     *
     * @return A {@code String} of the ability description.
     */
    public String getDescription() {
        return String.join(" ", this.descriptionLines);
    }

    /**
     * Attempt to use the tome ability if its cooldown has elapsed.
     *
     * @param player the player attempting to use the ability.
     * @return {@code true} if the {@code player} successfully used the ability.
     */
    public final boolean use(Player player) {
        if (this.isOnCooldown(player)) {
            long remainingTime = this.getRemainingCooldown(player);
            this.sendCannotUseMessage(player, "ability is on cooldown! " + VampireAbilityManager.formatTime(remainingTime) + " remaining.");
            return false;

        } else {
            if (this.useAbility(player)) {
                this.setCooldown(player);
                return true;

            } else {
                return false;
            }
        }
    }

    /**
     * Use the tome ability using the child class {@code useAbility} implementation.
     *
     * @param player the player using the ability.
     * @return {@code true} if the ability cooldown should be activated.
     */
    protected abstract boolean useAbility(Player player);

    /**
     * Determines if the player can use the human ability.
     *
     * @param player the player attempting to use the ability.
     * @return {@code true} if the {@code player} is human.
     */
    protected boolean canUse(Player player) {
        return this.plugin.getVampireManager().isHuman(player);
    }

    /**
     * Inform the user that they cannot use this tome ability.
     *
     * @param player the player attempting to use the ability.
     * @param reason the reason that the player cannot use this ability.
     */
    protected void sendCannotUseMessage(Player player, String reason) {
        player.sendMessage("§cCannot use " + this.name + ": " + reason);
    }

    /**
     * Inform the user that they successfully used this tome ability.
     *
     * @param player the player attempting to use the ability.
     * @param message the ability's successful use message.
     */
    protected void sendSuccessMessage(Player player, String message) {
        player.sendMessage("§a" + message);
    }

    /**
     * Determine if the tome ability is currently on a cooldown.
     *
     * @param player the player attempting to use the ability.
     * @return {@code true} if the ability's cooldown has not yet elapsed.
     */
    protected boolean isOnCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> cooldowns = playerCooldowns.get(playerId);

        if (cooldowns != null && cooldowns.containsKey(this.name)) {
            long cooldownEnd = cooldowns.get(this.name);
            return System.currentTimeMillis() < cooldownEnd;

        } else {
            return false;
        }
    }

    /**
     * Calculate how long the tome ability will remain on cooldown.
     *
     * @param player the player attempting to use the ability.
     * @return the seconds remaining until the cooldown has elapsed.
     */
    protected long getRemainingCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> cooldowns = playerCooldowns.get(playerId);

        if (cooldowns != null && cooldowns.containsKey(this.name)) {
            long cooldownEnd = cooldowns.get(this.name);
            long remaining = cooldownEnd - System.currentTimeMillis();
            return Math.max(0L, remaining / 1000L);

        } else {
            return 0L;
        }
    }

    /**
     * Set the cooldown on the tome ability.
     *
     * @param player the player attempting to use the ability.
     */
    protected void setCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> cooldowns = playerCooldowns.computeIfAbsent(playerId, (k) -> new HashMap<>());

        long cooldownEnd = System.currentTimeMillis() + (long)this.cooldownSeconds * 1000L;
        cooldowns.put(this.name, cooldownEnd);
        this.scheduleCooldownNotification(player, this.cooldownSeconds);
    }

    /**
     * Schedule a notification to inform the user when the tome ability's cooldown has elapsed.
     *
     * @param player the player that used the ability.
     * @param cooldownSeconds the ability cooldown.
     */
    private void scheduleCooldownNotification(Player player, int cooldownSeconds) {
        String taskKey = player.getUniqueId() + ":" + this.name;
        BukkitTask existingTask = cooldownNotificationTasks.get(taskKey);

        if (existingTask != null && !existingTask.isCancelled()) {
            existingTask.cancel();
        }

        BukkitTask notificationTask = Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            cooldownNotificationTasks.remove(taskKey);
            if (player.isOnline() && !this.plugin.getVampireManager().isVampire(player)) {
                this.notifyAbilityReady(player);
            }

        }, (long)cooldownSeconds * 20L);

        cooldownNotificationTasks.put(taskKey, notificationTask);
    }

    /**
     * Inform the player that the tome ability's cooldown has elapsed.
     *
     * @param player the player that used the ability.
     */
    private void notifyAbilityReady(Player player) {
        player.sendMessage("§a§l⚡ TOME ABILITY READY ⚡");
        player.sendMessage("§a" + this.name + " is now available.");
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.MASTER, 0.5F, 1.5F);
    }

    /**
     * Clear the cooldown on the {@code player}'s tome ability.
     *
     * @param player the player that used the ability.
     * @param abilityName the ability that will have its cooldown cleared.
     */
    public static void clearCooldown(Player player, String abilityName) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> cooldowns = playerCooldowns.get(playerId);

        if (cooldowns != null) {
            cooldowns.remove(abilityName);
        }

        String taskKey = playerId + ":" + abilityName;
        BukkitTask task = cooldownNotificationTasks.get(taskKey);

        if (task != null && !task.isCancelled()) {
            task.cancel();
            cooldownNotificationTasks.remove(taskKey);
        }
    }

    /**
     * Clear the cooldown on all of the {@code player}'s tome abilities.
     *
     * @param player the player that used the abilities.
     */
    public static void clearAllCooldowns(Player player) {
        UUID playerId = player.getUniqueId();
        playerCooldowns.remove(playerId);
        String playerPrefix = playerId + ":";

        cooldownNotificationTasks.entrySet().removeIf((entry) -> {
            if ((entry.getKey()).startsWith(playerPrefix)) {
                BukkitTask task = entry.getValue();
                if (task != null && !task.isCancelled()) {
                    task.cancel();
                }

                return true;

            } else {
                return false;
            }
        });
    }

    /**
     * Cancel the scheduled notifications regarding tome abilities.
     */
    public static void cancelAllNotificationTasks() {
        for (BukkitTask task : cooldownNotificationTasks.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }

        cooldownNotificationTasks.clear();
    }
}
