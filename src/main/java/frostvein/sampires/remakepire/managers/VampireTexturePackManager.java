package frostvein.sampires.remakepire.managers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class VampireTexturePackManager {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    // Control the vampire texture pack access details.
    private static final String VAMPIRE_TEXTURE_PACK_URL = "https://download.mc-packs.net/pack/506aa543b0cd39302fbda398cc83453429b3c3b5.zip";
    private static final String VAMPIRE_TEXTURE_PACK_SHA1_STRING = "506aa543b0cd39302fbda398cc83453429b3c3b5";
    private static final String VAMPIRE_TEXTURE_PACK_PROMPT = "§5VampireSMP Vampire Pack\n§7This pack enhances your vampire experience!";
    private static final UUID VAMPIRE_TEXTURE_PACK_UUID = UUID.randomUUID();
    // Control the human texture pack access details.
    private static final String HUMAN_TEXTURE_PACK_URL = "https://download.mc-packs.net/pack/176c1083aa793b5324ddc604c02ac2309398d6b4.zip";
    private static final String HUMAN_TEXTURE_PACK_SHA1_STRING = "176c1083aa793b5324ddc604c02ac2309398d6b4";
    private static final String HUMAN_TEXTURE_PACK_PROMPT = "§aVampireSMP Human Pack\n§7This pack enhances your human experience!";
    private static final UUID HUMAN_TEXTURE_PACK_UUID = UUID.randomUUID();
    private final Set<UUID> playersWithVampireTexturePack = new HashSet<>(), playersWithHumanTexturePack = new HashSet<>();

    /**
     * Create an instance of the Vampire Texture manager.
     *
     * @param plugin the host plugin object.
     */
    public VampireTexturePackManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        plugin.logInfo("VampireTexturePackManager initialized");
    }

    /**
     * Apply the vampire texture pack to the player.
     *
     * @param player the player receiving the texture pack.
     * @param reason the reason why the texture pack is being applied.
     */
    public void applyVampireTexturePack(Player player, String reason) {
        try {
            byte[] sha1Bytes = hexStringToByteArray(VAMPIRE_TEXTURE_PACK_SHA1_STRING);
            player.addResourcePack(VAMPIRE_TEXTURE_PACK_UUID, VAMPIRE_TEXTURE_PACK_URL, sha1Bytes, VAMPIRE_TEXTURE_PACK_PROMPT, true);
            this.playersWithVampireTexturePack.add(player.getUniqueId());

            player.sendMessage(Component.text("Applying vampire texture pack...", NamedTextColor.GRAY));
            this.plugin.logInfo("Sent vampire texture pack request to " + player.getName() + " - " + reason);
            this.plugin.logInfo("Pack URL: " + VAMPIRE_TEXTURE_PACK_URL);
            this.plugin.logInfo("Pack SHA1: " + VAMPIRE_TEXTURE_PACK_SHA1_STRING);

        } catch (Exception e) {
            this.plugin.getLogger().severe("Failed to apply vampire texture pack to " + player.getName() + ": " + e.getMessage());
            player.sendMessage(Component.text("Failed to apply texture pack. Check server logs for details.", NamedTextColor.RED));
        }
    }

    /**
     * Attempt to apply the vampire texture pack to the player after a delay.
     *
     * @param player the player receiving the texture pack.
     * @param delayTicks the time to wait before attempting.
     * @param reason the reason why the texture pack is being applied.
     */
    public void applyVampireTexturePackDelayed(Player player, long delayTicks, String reason) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (player.isOnline() && this.vampireManager.isVampire(player)) {
                try {
                    this.applyVampireTexturePack(player, reason + " (delayed)");
                } catch (Exception e) {
                    this.plugin.getLogger().warning("Failed to apply delayed vampire texture pack to " + player.getName() + ": " + e.getMessage());
                }
            } else if (player.isOnline()) {
                this.plugin.logInfo("Skipped vampire texture pack for " + player.getName() + " - no longer a vampire (" + reason + ")");
            }
        }, delayTicks);

        this.plugin.logInfo("Scheduled vampire texture pack for " + player.getName() + " in " + delayTicks / 20.0 + " seconds - " + reason);
    }

    /**
     * Log in the console that the vampire texture pack was applied because of a new vampire's creation.
     *
     * @param player the player who became a vampire.
     */
    public void onVampireTransformation(Player player) {
        this.plugin.logInfo("Vampire transformation completed for " + player.getName() + " - awaiting voluntary texture pack application");
    }

    /**
     * Apply the vampire texture pack after a delay when the player logs in.
     *
     * @param player the player joining the world.
     */
    public void onVampireLogin(Player player) {
        this.applyVampireTexturePackDelayed(player, 100L, "vampire login");
    }

    /**
     * Remove the player from the list of those using the vampire texture pack.
     *
     * @param player the player becoming human.
     */
    public void onPlayerBecomeHuman(Player player) {
        this.playersWithVampireTexturePack.remove(player.getUniqueId());
    }

    /**
     * Apply the human texture pack to the player.
     *
     * @param player the player becoming human.
     * @param reason the reason why the texture pack is being applied.
     */
    public void applyHumanTexturePack(Player player, String reason) {
        try {
            byte[] sha1Bytes = hexStringToByteArray(HUMAN_TEXTURE_PACK_SHA1_STRING);
            player.addResourcePack(HUMAN_TEXTURE_PACK_UUID, HUMAN_TEXTURE_PACK_URL, sha1Bytes, HUMAN_TEXTURE_PACK_PROMPT, true);
            player.removeResourcePack(VAMPIRE_TEXTURE_PACK_UUID);
            this.playersWithHumanTexturePack.add(player.getUniqueId());
            this.playersWithVampireTexturePack.remove(player.getUniqueId());

            player.sendMessage(Component.text("Applying human texture pack...", NamedTextColor.GRAY));
            this.plugin.logInfo("Sent human texture pack request to " + player.getName() + " - " + reason);
            this.plugin.logInfo("Pack URL: " + HUMAN_TEXTURE_PACK_URL);
            this.plugin.logInfo("Pack SHA1: " + HUMAN_TEXTURE_PACK_SHA1_STRING);

        } catch (Exception e) {
            this.plugin.getLogger().severe("Failed to apply human texture pack to " + player.getName() + ": " + e.getMessage());
            player.sendMessage(Component.text("Failed to apply texture pack. Check server logs for details.", NamedTextColor.RED));
        }
    }

    /**
     * Attempt to apply the human texture pack to the player after a delay.
     *
     * @param player the player receiving the texture pack.
     * @param delayTicks the time to wait before attempting.
     * @param reason the reason why the texture pack is being applied.
     */
    public void applyHumanTexturePackDelayed(Player player, long delayTicks, String reason) {
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (player.isOnline()) {
                this.applyHumanTexturePack(player, reason + " (delayed)");
            }
        }, delayTicks);
    }

    /**
     * Check if the player has the vampire texture pack active.
     *
     * @param player the player being checked.
     * @return {@code true} if the player is on the list of those using the vampire texture pack.
     */
    public boolean hasVampireTexturePack(Player player) {
        return this.playersWithVampireTexturePack.contains(player.getUniqueId());
    }

    /**
     * Check if the player has the human texture pack active.
     *
     * @param player the player being checked.
     * @return {@code true} if the player is on the list of those using the human texture pack.
     */
    public boolean hasHumanTexturePack(Player player) {
        return this.playersWithHumanTexturePack.contains(player.getUniqueId());
    }

    /**
     * Remove the player from the lists of those using either texture pack.
     *
     * @param player a player leaving the world.
     */
    public void onPlayerQuit(Player player) {
        this.playersWithVampireTexturePack.remove(player.getUniqueId());
        this.playersWithHumanTexturePack.remove(player.getUniqueId());
    }

    /**
     * Apply the vampire texture pack to the player.
     *
     * @param player the player given the texture pack.
     */
    public void manualApplication(Player player) {
        this.applyVampireTexturePack(player, "admin command");
    }

    /**
     * Refresh the vampire texture pack on the player.
     *
     * @param player the player being refreshed.
     * @param reason the reason why the texture pack is being applied.
     */
    public void forceApplyVampireTexturePack(Player player, String reason) {
        this.playersWithVampireTexturePack.remove(player.getUniqueId());
        this.applyVampireTexturePack(player, reason + " (forced)");
    }

    /**
     * Apply the texture pack to all vampires.
     */
    public void ensureAllVampiresHaveTexturePack() {
        int applied = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (this.vampireManager.isVampire(player)) {
                long delay = (long)applied * 20L;
                this.applyVampireTexturePackDelayed(player, delay, "ensure all vampires");
                ++applied;
            }
        }

        if (applied > 0) {
            this.plugin.logInfo("Ensured vampire texture pack for " + applied + " online vampires");
        }
    }

    /**
     * Convert a string into an array of bytes.
     *
     * @param s the string to convert.
     * @return A {@code byte} array.
     */
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte)((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }

        return data;
    }

    /**
     * Clear the list of players using either texture pack before shutting down the manager.
     */
    public void shutdown() {
        this.playersWithVampireTexturePack.clear();
        this.playersWithHumanTexturePack.clear();
        this.plugin.logInfo("VampireTexturePackManager shutdown complete");
    }
}
