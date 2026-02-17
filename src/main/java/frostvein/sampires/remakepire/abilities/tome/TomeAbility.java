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
    private static final Map<UUID, Map<String, Long>> playerCooldowns = new HashMap();
    private static final Map<String, BukkitTask> cooldownNotificationTasks = new HashMap();

    public TomeAbility(RemakepirePlugin plugin, String name, String[] descriptionLines, int cooldownSeconds) {
        this.plugin = plugin;
        this.name = name;
        this.descriptionLines = descriptionLines;
        this.cooldownSeconds = cooldownSeconds;
    }

    public String getName() {
        return this.name;
    }

    public String[] getDescriptionLines() {
        return this.descriptionLines;
    }

    public String getDescription() {
        return String.join(" ", this.descriptionLines);
    }

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

    protected abstract boolean useAbility(Player player);

    protected boolean canUse(Player player) {
        return this.plugin.getVampireManager().isHuman(player);
    }

    protected void sendCannotUseMessage(Player player, String reason) {
        player.sendMessage("§cCannot use " + this.name + ": " + reason);
    }

    protected void sendSuccessMessage(Player player, String message) {
        player.sendMessage("§a" + message);
    }

    protected boolean isOnCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> cooldowns = (Map)playerCooldowns.get(playerId);

        if (cooldowns != null && cooldowns.containsKey(this.name)) {
            long cooldownEnd = (Long)cooldowns.get(this.name);
            return System.currentTimeMillis() < cooldownEnd;

        } else {
            return false;
        }
    }

    protected long getRemainingCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> cooldowns = (Map)playerCooldowns.get(playerId);
        if (cooldowns != null && cooldowns.containsKey(this.name)) {
            long cooldownEnd = (Long)cooldowns.get(this.name);
            long remaining = cooldownEnd - System.currentTimeMillis();
            return Math.max(0L, remaining / 1000L);
        } else {
            return 0L;
        }
    }

    protected void setCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> cooldowns = (Map)playerCooldowns.computeIfAbsent(playerId, (k) -> new HashMap());
        long cooldownEnd = System.currentTimeMillis() + (long)this.cooldownSeconds * 1000L;
        cooldowns.put(this.name, cooldownEnd);
        this.scheduleCooldownNotification(player, this.cooldownSeconds);
    }

    private void scheduleCooldownNotification(Player player, int cooldownSeconds) {
        String taskKey = String.valueOf(player.getUniqueId()) + ":" + this.name;
        BukkitTask existingTask = (BukkitTask)cooldownNotificationTasks.get(taskKey);

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

    private void notifyAbilityReady(Player player) {
        player.sendMessage("§a§l⚡ TOME ABILITY READY ⚡");
        player.sendMessage("§a" + this.name + " is now available.");
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.MASTER, 0.5F, 1.5F);
    }

    public static void clearCooldown(Player player, String abilityName) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> cooldowns = (Map)playerCooldowns.get(playerId);
        if (cooldowns != null) {
            cooldowns.remove(abilityName);
        }

        String taskKey = String.valueOf(playerId) + ":" + abilityName;
        BukkitTask task = (BukkitTask)cooldownNotificationTasks.get(taskKey);
        if (task != null && !task.isCancelled()) {
            task.cancel();
            cooldownNotificationTasks.remove(taskKey);
        }

    }

    public static void clearAllCooldowns(Player player) {
        UUID playerId = player.getUniqueId();
        playerCooldowns.remove(playerId);
        String playerPrefix = String.valueOf(playerId) + ":";
        cooldownNotificationTasks.entrySet().removeIf((entry) -> {
            if (((String)entry.getKey()).startsWith(playerPrefix)) {
                BukkitTask task = (BukkitTask)entry.getValue();
                if (task != null && !task.isCancelled()) {
                    task.cancel();
                }

                return true;
            } else {
                return false;
            }
        });
    }

    public static void cancelAllNotificationTasks() {
        for(BukkitTask task : cooldownNotificationTasks.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }

        cooldownNotificationTasks.clear();
    }
}
