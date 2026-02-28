package frostvein.sampires.remakepire.managers;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class BloodMoonManager {
    private boolean isBloodMoonActive = false;
    private RemakepirePlugin plugin;
    private BukkitTask vampireBuffTask;

    /**
     * Create an instance of the Blood Moon manager.
     *
     * @param plugin the host plugin object.
     */
    public BloodMoonManager(RemakepirePlugin plugin) {
        this.plugin = plugin;

        (new BukkitRunnable() {
            public void run() {
                BloodMoonManager.this.checkTimeAndMoon();
            }
        }).runTaskTimer(this.plugin, 0L, 20L);
    }

    /**
     * Begin or end the blood moon depending on the time of day and moon phase
     */
    private void checkTimeAndMoon() {
        long time = this.plugin.getWorld().getTime();
        long fullTime = this.plugin.getWorld().getFullTime();
        boolean isNight = time >= 12000L && time < 24000L;
        boolean isFullMoon = fullTime % 192000L < 24000L;

        if (isNight && isFullMoon && !this.isBloodMoonActive) {
            this.startBloodMoon(this.plugin.getWorld());
        }

        if (this.isBloodMoonActive && (!isNight || !isFullMoon)) {
            this.endBloodMoon();
        }
    }

    /**
     * Begin applying blood moon buffs
     *
     * @param world the world hosting the plugin interactions.
     */
    private void startBloodMoon(World world) {
        if (!this.isBloodMoonActive) {
            this.isBloodMoonActive = true;
            this.plugin.getLogger().info("Blood moon started!");
            this.announceBloodMoon(world);

            this.vampireBuffTask = (new BukkitRunnable() {
                public void run() {
                    BloodMoonManager.this.applyVampireBuffs();
                }
            }).runTaskTimer(this.plugin, 0L, 20L);
        }
    }

    /**
     *
     */
    private void endBloodMoon() {
        if (this.isBloodMoonActive) {
            this.isBloodMoonActive = false;
            this.plugin.getLogger().info("Blood moon ended!");

            if (this.vampireBuffTask != null && !this.vampireBuffTask.isCancelled()) {
                this.vampireBuffTask.cancel();
                this.vampireBuffTask = null;
            }

            this.plugin.getWorld().getPlayers().forEach((player) -> player.sendMessage("§7The blood moon fades away..."));
        }
    }

    private void announceBloodMoon(World world) {
        world.getPlayers().forEach((player) -> {
            player.sendMessage("§c§lA blood moon rises...");
            player.playSound(player, Sound.AMBIENT_CRIMSON_FOREST_MOOD, 1.0F, 1.0F);
        });
    }

    private void applyVampireBuffs() {
        if (this.isBloodMoonActive) {
            for(Player player : Bukkit.getOnlinePlayers()) {
                if (this.plugin.getVampireManager().isVampireStage2(player) || this.plugin.getVampireManager().isVampireStage3(player)) {

                    if (this.canPlayerSeeSky(player)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, 240, 0, false, false), false);

                        if (!player.getScoreboardTags().contains("informed_blood_moon")) {
                            player.addScoreboardTag("informed_blood_moon");
                            player.playSound(player, Sound.AMBIENT_NETHER_WASTES_MOOD, 1.0F, 1.0F);
                            player.sendMessage("§4You feel the Blood Moon's power coursing through your veins...");
                        }
                    } else {
                        player.removePotionEffect(PotionEffectType.UNLUCK);
                    }
                }
            }
        }
    }

    private boolean canPlayerSeeSky(Player player) {
        Block highestBlock = player.getWorld().getHighestBlockAt(player.getLocation());
        return player.getLocation().getBlockY() >= highestBlock.getY();
    }

    public boolean isActive() {
        return this.isBloodMoonActive;
    }

    public void forceStart() {
        if (!this.isBloodMoonActive) {
            this.startBloodMoon(this.plugin.getWorld());
        }

    }

    public void forceStop() {
        if (this.isBloodMoonActive) {
            this.endBloodMoon();
        }
    }

    public String getCurrentMoonPhase() {
        return this.getMoonPhaseName(this.plugin.getWorld().getFullTime());
    }

    private String getMoonPhaseName(long fullTime) {
        long moonCycle = fullTime % 192000L;

        if (moonCycle < 24000L) {
            return "Full Moon";
        } else if (moonCycle < 48000L) {
            return "Waning Gibbous";
        } else if (moonCycle < 72000L) {
            return "Third Quarter";
        } else if (moonCycle < 96000L) {
            return "Waning Crescent";
        } else if (moonCycle < 120000L) {
            return "New Moon";
        } else if (moonCycle < 144000L) {
            return "Waxing Crescent";
        } else {
            return moonCycle < 168000L ? "First Quarter" : "Waxing Gibbous";
        }
    }

    public void shutdown() {
        if (this.vampireBuffTask != null && !this.vampireBuffTask.isCancelled()) {
            this.vampireBuffTask.cancel();
        }
    }
}
