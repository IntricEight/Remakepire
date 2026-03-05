package frostvein.sampires.remakepire.commands;

import java.util.ListIterator;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.beacons.BeaconSite;
import frostvein.sampires.remakepire.beacons.BeaconSite.BeaconState;
import frostvein.sampires.remakepire.listeners.CureBookReadingListener;
import frostvein.sampires.remakepire.listeners.DeathHandler;
import frostvein.sampires.remakepire.managers.BeaconManager;
import frostvein.sampires.remakepire.managers.VampireManager;
import frostvein.sampires.remakepire.managers.VampireSireManager;

public class VampireCureCommand implements CommandExecutor {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final BeaconManager beaconManager;
    private final VampireSireManager sireManager;

    public VampireCureCommand(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.beaconManager = plugin.getBeaconManager();
        this.sireManager = plugin.getSireManager();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;

        } else if (!CureBookReadingListener.hasReadAllCureBooks(player)) {
            player.sendMessage("§cYou do not know these ancient words...");
            player.sendMessage("§7You must first read all three cure books to learn the ritual.");
            return true;

        } else if (!this.vampireManager.isVampire(player)) {
            player.sendMessage("§cOnly Mimics can use this cure ritual.");
            return true;

        } else {
            long time = player.getWorld().getTime();
            boolean isDay = time >= 0L && time < 12300L;

            if (!isDay) {
                player.sendMessage("§cThis ritual can only be performed during the day.");
                return true;
            } else {
                ItemStack holyWater = this.findHolyWater(player);
                if (holyWater == null) {
                    player.sendMessage("§cYou need holy water to perform this ritual.");
                    return true;

                } else {
                    double cureDistance = this.plugin.getConfigManager().getCureBeaconDistance();
                    BeaconSite nearestHolyBeacon = this.beaconManager.getNearestHolyBeacon(player.getLocation(), cureDistance);

                    if (nearestHolyBeacon == null) {
                        player.sendMessage("§cYou must be close to a holy beacon to perform this ritual.");
                        return true;

                    } else {
                        String sireName = this.sireManager.getSire(player);

                        if (sireName != null && !this.sireManager.isSireDead(player)) {
                            player.sendMessage("§3The curse cannot be broken while your sire, " + sireName + ", still walks the world in mortal form...");
                            player.sendMessage("§3Only through your maker's true death can you find release.");
                            return true;

                        } else {
                            this.performCure(player, holyWater, nearestHolyBeacon);
                            return true;
                        }
                    }
                }
            }
        }
    }

    private ItemStack findHolyWater(Player player) {
        for (ItemStack item : player.getInventory()) {
            if (this.isWaterSplashBottle(item)) {
                return item;
            }
        }

        return null;
    }

    private boolean isWaterSplashBottle(ItemStack item) {
        if (item != null && item.getType() == Material.SPLASH_POTION) {
            if (!item.hasItemMeta()) {
                return true;

            } else if (!(item.getItemMeta() instanceof PotionMeta)) {
                return true;
            } else {
                PotionMeta potionMeta = (PotionMeta)item.getItemMeta();

                if (potionMeta.hasCustomEffects()) {
                    return false;
                } else {
                    try {
                        PotionType baseType = potionMeta.getBasePotionType();
                        return baseType == PotionType.WATER;

                    } catch (Exception e) {
                        return true;
                    }
                }
            }
        } else {
            return false;
        }
    }

    private void performCure(Player player, ItemStack holyWater, BeaconSite holyBeacon) {
        Location playerLoc = player.getLocation();
        Location beaconLoc = holyBeacon.getLocation();
        holyWater.setAmount(holyWater.getAmount() - 1);

        player.sendTitle("§6§lCURED", "§eThe curse is lifted", 10, 60, 20);
        player.sendMessage("§7The holy water burns through your veins...");
        player.sendMessage("§7The corrupted blood boils away in divine light...");
        player.sendMessage("§aYou feel your humanity returning...");
        player.sendMessage("§aYou are cured. You are human once more.");
        player.sendMessage("§8But the holy site has been permanently corrupted by your dark presence...");

        for(Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(player)) {
                if (this.vampireManager.isVampire(onlinePlayer)) {
                    onlinePlayer.sendMessage("§3A disturbance ripples through the darkness... One of your kind has abandoned the gift of immortality...");
                } else {
                    onlinePlayer.sendMessage("§aA beacon of holy light flickers and dims... A vampire has been cured, but at great cost to the sacred site.");
                }
            }
        }

        player.getWorld().spawnParticle(Particle.SOUL, playerLoc, 100, 1.0, 2.0, 1.0, 0.1);
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, playerLoc, 1, 0.5, 1.0, 0.5, 0.0);
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, playerLoc, 5, 1.0, 1.0, 1.0, 0.0);
        player.getWorld().playSound(playerLoc, Sound.BLOCK_BELL_USE, SoundCategory.MASTER, 1.5F, 0.8F);
        player.getWorld().playSound(playerLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.MASTER, 1.0F, 1.0F);
        player.getWorld().playSound(playerLoc, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, SoundCategory.MASTER, 1.0F, 1.5F);

        if (beaconLoc != null) {
            player.getWorld().spawnParticle(Particle.LARGE_SMOKE, beaconLoc.clone().add(0.0, 1.5, 0.0), 50, 0.5, 1.0, 0.5, 0.05);
            player.getWorld().spawnParticle(Particle.SMOKE, beaconLoc.clone().add(0.0, 1.5, 0.0), 30, 0.3, 0.8, 0.3, 0.02);
            player.getWorld().playSound(beaconLoc, Sound.ENTITY_WITHER_HURT, SoundCategory.MASTER, 0.8F, 0.6F);
        }

        this.vampireManager.setPlayerAsHuman(player);
        player.getActivePotionEffects().forEach((effect) -> player.removePotionEffect(effect.getType()));
        player.addScoreboardTag("CuredVampire");
        holyBeacon.setState(BeaconState.PERMANENTLY_DESECRATED);

        this.beaconManager.updateBeaconDisplay(holyBeacon);
        this.beaconManager.saveBeacons();
        this.plugin.getBeaconMajorityManager().updateBeaconMajorityBonuses();
        this.beaconManager.checkAndBroadcastCompleteControl();
        this.checkIfAllBeaconsEvil();

        if (this.plugin.getVampireTurningManager() != null) {
            this.plugin.getVampireTurningManager().disableAllVampireTurning();
        }

        this.plugin.getLogger().info("VAMPIRE CURE: " + player.getName() + " has been cured at beacon: " + holyBeacon.getName());
        DeathHandler.checkAndAnnounceTeamElimination(this.plugin, false, true);
    }

    private void checkIfAllBeaconsEvil() {
        int evilCount = this.beaconManager.getAllEvilBeacons().size();
        int totalBeacons = this.beaconManager.getAllBeacons().size();

        if (evilCount >= totalBeacons && totalBeacons > 0 && !this.plugin.getSessionManager().isVampiresEternalNightActive()) {
            this.triggerVampiresEternalNight();
        }
    }

    private void triggerVampiresEternalNight() {
        this.plugin.getLogger().info("VAMPIRES ETERNAL NIGHT TRIGGERED - All beacons are now evil!");

        for(Player player : this.plugin.getServer().getOnlinePlayers()) {
            player.sendTitle("§3§lETERNAL NIGHT FALLS", "§cThe darkness consumes all hope", 20, 100, 40);
            player.sendMessage("§c All beacons now pulse with unholy energy.");
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.MASTER, 1.0F, 0.5F);
        }

        for(Player player : this.plugin.getServer().getOnlinePlayers()) {
            if (this.vampireManager.isHuman(player)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, -1, 0, false, false, true));
            }
        }

        this.plugin.getSessionManager().setVampiresEternalNightActive(true);
    }
}
