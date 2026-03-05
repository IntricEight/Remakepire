package frostvein.sampires.remakepire.managers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class VampireTexturePackManager {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;

    private static final String VAMPIRE_TEXTURE_PACK_URL = "https://download.mc-packs.net/pack/e139890dd34f56724efcd5becb476999651ca43c.zip";
    private static final String VAMPIRE_TEXTURE_PACK_SHA1_STRING = "e139890dd34f56724efcd5becb476999651ca43c";
    private static final String VAMPIRE_TEXTURE_PACK_PROMPT = "§5VampireSMP Vampire Pack\n§7This pack enhances your vampire experience!";
    private static final String HUMAN_TEXTURE_PACK_URL = "https://download.mc-packs.net/pack/b1fbd00667c6ad35c11967a385184aa336d605e1.zip";
    private static final String HUMAN_TEXTURE_PACK_SHA1_STRING = "b1fbd00667c6ad35c11967a385184aa336d605e1";
    private static final String HUMAN_TEXTURE_PACK_PROMPT = "§aVampireSMP Human Pack\n§7This pack enhances your human experience!";

    private final Set<UUID> playersWithVampireTexturePack = new HashSet<>();
    private final Set<UUID> playersWithHumanTexturePack = new HashSet<>();

    /**
     * Create an instance of the Vampire Texture manager.
     *
     * @param plugin the host plugin object.
     */
    public VampireTexturePackManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        plugin.getLogger().info("VampireTexturePackManager initialized");
    }

    public void applyVampireTexturePack(Player player, String reason) {
//        try {
//            byte[] sha1Bytes = hexStringToByteArray(VAMPIRE_TEXTURE_PACK_SHA1_STRING);
//            UUID packId = UUID.randomUUID();
//            player.addResourcePack(packId, VAMPIRE_TEXTURE_PACK_URL, sha1Bytes, VAMPIRE_TEXTURE_PACK_PROMPT, true);
            this.playersWithVampireTexturePack.add(player.getUniqueId());
//            player.sendMessage("§7Applying vampire texture pack...");
//
//            this.plugin.getLogger().info("Sent vampire texture pack request to " + player.getName() + " - " + reason);
//            this.plugin.getLogger().info("Pack URL: https://download.mc-packs.net/pack/e139890dd34f56724efcd5becb476999651ca43c.zip");
//            this.plugin.getLogger().info("Pack SHA1: e139890dd34f56724efcd5becb476999651ca43c");
//
//        } catch (Exception e) {
//            this.plugin.getLogger().severe("Failed to apply vampire texture pack to " + player.getName() + ": " + e.getMessage());
//            e.printStackTrace();
//            player.sendMessage("§cFailed to apply texture pack. Check server logs for details.");
//        }
    }

    public void applyVampireTexturePackDelayed(Player player, long delayTicks, String reason) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (player.isOnline() && this.vampireManager.isVampire(player)) {
                try {
                    this.applyVampireTexturePack(player, reason + " (delayed)");
                } catch (Exception e) {
                    this.plugin.getLogger().warning("Failed to apply delayed vampire texture pack to " + player.getName() + ": " + e.getMessage());
                }
            } else if (player.isOnline()) {
                this.plugin.getLogger().info("Skipped vampire texture pack for " + player.getName() + " - no longer a vampire (" + reason + ")");
            }
        }, delayTicks);

        this.plugin.getLogger().info("Scheduled vampire texture pack for " + player.getName() + " in " + delayTicks / 20.0 + " seconds - " + reason);
    }

    public void onVampireTransformation(Player player) {
        this.plugin.getLogger().info("Vampire transformation completed for " + player.getName() + " - awaiting voluntary texture pack application");
    }

    public void onVampireLogin(Player player) {
        this.applyVampireTexturePackDelayed(player, 100L, "vampire login");
    }

    public void onPlayerBecomeHuman(Player player) {
        this.playersWithVampireTexturePack.remove(player.getUniqueId());
    }

    public void applyHumanTexturePack(Player player, String reason) {
        try {
            byte[] sha1Bytes = hexStringToByteArray(HUMAN_TEXTURE_PACK_SHA1_STRING);
            UUID packId = UUID.randomUUID();
            player.addResourcePack(packId, HUMAN_TEXTURE_PACK_URL, sha1Bytes, HUMAN_TEXTURE_PACK_PROMPT, true);

            this.playersWithHumanTexturePack.add(player.getUniqueId());
            this.playersWithVampireTexturePack.remove(player.getUniqueId());
            player.sendMessage("§7Applying human texture pack...");
            this.plugin.getLogger().info("Sent human texture pack request to " + player.getName() + " - " + reason);

        } catch (Exception e) {
            this.plugin.getLogger().severe("Failed to apply human texture pack to " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            player.sendMessage("§cFailed to apply texture pack. Check server logs for details.");
        }
    }

    public void applyHumanTexturePackDelayed(Player player, long delayTicks, String reason) {
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (player.isOnline()) {
                this.applyHumanTexturePack(player, reason + " (delayed)");
            }
        }, delayTicks);
    }

    public boolean hasHumanTexturePack(Player player) {
        return this.playersWithHumanTexturePack.contains(player.getUniqueId());
    }

    /**
     *
     *
     * @param player a player leaving the world.
     */
    public void onPlayerQuit(Player player) {
        this.playersWithVampireTexturePack.remove(player.getUniqueId());
        this.playersWithHumanTexturePack.remove(player.getUniqueId());
    }

    public boolean hasVampireTexturePack(Player player) {
        return this.playersWithVampireTexturePack.contains(player.getUniqueId());
    }

    public void manualApplication(Player player) {
//        this.applyVampireTexturePack(player, "admin command");
    }

    public void forceApplyVampireTexturePack(Player player, String reason) {
        this.playersWithVampireTexturePack.remove(player.getUniqueId());
        this.applyVampireTexturePack(player, reason + " (forced)");
    }

    public void ensureAllVampiresHaveTexturePack() {
        int applied = 0;

        for(Player player : Bukkit.getOnlinePlayers()) {
            if (this.vampireManager.isVampire(player)) {
                long delay = (long)applied * 20L;
                this.applyVampireTexturePackDelayed(player, delay, "ensure all vampires");
                ++applied;
            }
        }

        if (applied > 0) {
            this.plugin.getLogger().info("Ensured vampire texture pack for " + applied + " online vampires");
        }
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];

        for(int i = 0; i < len; i += 2) {
            data[i / 2] = (byte)((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }

        return data;
    }

    public void shutdown() {
        this.playersWithVampireTexturePack.clear();
        this.playersWithHumanTexturePack.clear();
        this.plugin.getLogger().info("VampireTexturePackManager shutdown complete");
    }
}
