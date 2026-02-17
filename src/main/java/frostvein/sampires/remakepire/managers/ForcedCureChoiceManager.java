package frostvein.sampires.remakepire.managers;

import java.util.Arrays;
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
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.beacons.BeaconSite;
import frostvein.sampires.remakepire.beacons.BeaconSite.BeaconState;
import frostvein.sampires.remakepire.listeners.DeathHandler;

public class ForcedCureChoiceManager {
    private final RemakepirePlugin plugin;
    private final Map<UUID, ForcedCureData> pendingCures = new HashMap();

    public ForcedCureChoiceManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    public void openChoiceGUI(Player caster, Player target, BeaconSite holyBeacon) {
        boolean hadFlight = target.getAllowFlight();
        boolean wasInvulnerable = target.isInvulnerable();
        this.pendingCures.put(target.getUniqueId(), new ForcedCureData(caster.getUniqueId(), target.getUniqueId(), holyBeacon, hadFlight, wasInvulnerable));
        this.applyEffectsAndOpenGUI(target);
    }

    public void reopenChoiceGUI(Player target) {
        ForcedCureData data = (ForcedCureData)this.pendingCures.get(target.getUniqueId());
        if (data != null) {
            this.applyEffectsAndOpenGUI(target);
        }
    }

    private void applyEffectsAndOpenGUI(Player target) {
        target.setAllowFlight(true);
        target.setFlying(true);
        target.setInvulnerable(true);
        Inventory gui = Bukkit.createInventory((InventoryHolder)null, 27, "§4§lYour Fate Awaits...");
        ItemStack humanityButton = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta humanityMeta = humanityButton.getItemMeta();
        if (humanityMeta != null) {
            humanityMeta.setDisplayName("§a§lReturn to Humanity");
            humanityMeta.setLore(Arrays.asList("§7The holy words have broken your curse.", "§7You can feel your humanity returning...", "", "§eClick to accept your return to mortality."));
            humanityButton.setItemMeta(humanityMeta);
        }

        ItemStack deathButton = new ItemStack(Material.SKELETON_SKULL);
        ItemMeta deathMeta = deathButton.getItemMeta();
        if (deathMeta != null) {
            deathMeta.setDisplayName("§4§lFinally Accept Death");
            deathMeta.setLore(Arrays.asList("§7You have lived too long as a creature", "§7of darkness. Perhaps it is time to rest...", "", "§c§lWARNING: This will result in permadeath!", "§eClick to embrace the eternal sleep."));
            deathButton.setItemMeta(deathMeta);
        }

        gui.setItem(11, humanityButton);
        gui.setItem(15, deathButton);
        target.openInventory(gui);
        target.sendMessage("");
        target.sendMessage("§4§l§m                                                    ");
        target.sendMessage("§4§lHOLY WORDS PIERCE YOUR SOUL");
        target.sendMessage("§7You feel the curse tearing away from your being...");
        target.sendMessage("§7But you have a choice to make...");
        target.sendMessage("§4§l§m                                                    ");
        target.sendMessage("");
    }

    public boolean hasPendingCure(Player player) {
        return this.pendingCures.containsKey(player.getUniqueId());
    }

    public ForcedCureData getPendingCure(Player player) {
        return (ForcedCureData)this.pendingCures.get(player.getUniqueId());
    }

    public void removePendingCure(Player player) {
        this.pendingCures.remove(player.getUniqueId());
    }

    public void handleHumanityChoice(Player target) {
        ForcedCureData data = (ForcedCureData)this.pendingCures.get(target.getUniqueId());
        if (data != null) {
            target.closeInventory();
            target.setInvulnerable(data.wasInvulnerableBefore);
            target.setAllowFlight(data.hadFlightBefore);
            if (!data.hadFlightBefore) {
                target.setFlying(false);
            }

            this.removePendingCure(target);
            Player caster = data.getCaster();
            this.plugin.getLogger().info("FORCED CURE CHOICE: " + target.getName() + " chose to return to humanity");
            this.performCure(caster, target, data.holyBeacon);
        }
    }

    public void handleDeathChoice(Player target) {
        ForcedCureData data = (ForcedCureData)this.pendingCures.get(target.getUniqueId());
        if (data != null) {
            target.closeInventory();
            target.setInvulnerable(data.wasInvulnerableBefore);
            target.setAllowFlight(data.hadFlightBefore);
            if (!data.hadFlightBefore) {
                target.setFlying(false);
            }

            this.removePendingCure(target);
            Player caster = data.getCaster();
            this.plugin.getLogger().info("FORCED CURE CHOICE: " + target.getName() + " chose permadeath over cure");
            this.performPermadeath(caster, target, data.holyBeacon);
        }
    }

    private void performCure(Player caster, Player target, BeaconSite holyBeacon) {
        caster.sendMessage("§6" + target.getName() + " has chosen to return to humanity...");
        caster.sendMessage("§7The creature of darkness accepts their redemption...");
        caster.sendMessage("§aYou have sanctified " + target.getName() + ", and they have accepted.");
        target.sendTitle("§6§lREDEEMED", "§eYou have chosen humanity", 10, 60, 20);
        target.sendMessage("§aYou accept the holy words and choose to return...");
        target.sendMessage("§7The holy water burns through your veins...");
        target.sendMessage("§7Your corrupted blood boils away in divine light...");
        target.sendMessage("§aYou feel your humanity returning...");
        target.sendMessage("§aYou are human once more.");
        target.sendMessage("§8But the holy site has been permanently corrupted by your dark presence...");

        for(Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(caster) && !onlinePlayer.equals(target)) {
                if (this.plugin.getVampireManager().isVampire(onlinePlayer)) {
                    onlinePlayer.sendMessage("§4A disturbance ripples through the darkness... One of your kind has chosen humanity over eternal damnation...");
                } else {
                    onlinePlayer.sendMessage("§6A beacon of holy light erupts with righteous fury... Someone has sanctified a vampire forcibly... The curing comes at a great cost to a sacred site...");
                }
            }
        }

        this.plugin.getVampireManager().setPlayerAsHuman(target);
        target.getActivePotionEffects().forEach((effect) -> target.removePotionEffect(effect.getType()));
        target.addScoreboardTag("CuredVampire");
        Location targetLoc = target.getLocation();
        Location beaconLoc = holyBeacon.getLocation();
        target.getWorld().spawnParticle(Particle.SOUL, targetLoc, 100, (double)1.0F, (double)2.0F, (double)1.0F, 0.1);
        target.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, targetLoc, 1, (double)0.5F, (double)1.0F, (double)0.5F, (double)0.0F);
        target.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, targetLoc, 5, (double)1.0F, (double)1.0F, (double)1.0F, (double)0.0F);
        target.getWorld().playSound(targetLoc, Sound.BLOCK_BELL_USE, SoundCategory.MASTER, 1.5F, 0.8F);
        target.getWorld().playSound(targetLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.MASTER, 1.0F, 1.0F);
        target.getWorld().playSound(targetLoc, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, SoundCategory.MASTER, 1.0F, 1.5F);
        if (beaconLoc != null) {
            target.getWorld().spawnParticle(Particle.LARGE_SMOKE, beaconLoc.clone().add((double)0.0F, (double)1.5F, (double)0.0F), 50, (double)0.5F, (double)1.0F, (double)0.5F, 0.05);
            target.getWorld().spawnParticle(Particle.SMOKE, beaconLoc.clone().add((double)0.0F, (double)1.5F, (double)0.0F), 30, 0.3, 0.8, 0.3, 0.02);
            target.getWorld().playSound(beaconLoc, Sound.ENTITY_WITHER_HURT, SoundCategory.MASTER, 0.8F, 0.6F);
        }

        holyBeacon.setState(BeaconState.PERMANENTLY_DESECRATED);
        this.plugin.getBeaconManager().updateBeaconDisplay(holyBeacon);
        this.plugin.getBeaconManager().saveBeacons();
        this.plugin.getBeaconMajorityManager().updateBeaconMajorityBonuses();
        this.plugin.getBeaconManager().checkAndBroadcastCompleteControl();
        this.checkIfAllBeaconsEvil();
        if (this.plugin.getVampireTurningManager() != null) {
            this.plugin.getVampireTurningManager().disableAllVampireTurning();
        }

        DeathHandler.checkAndAnnounceTeamElimination(this.plugin, false, true);
    }

    private void checkIfAllBeaconsEvil() {
        int evilCount = this.plugin.getBeaconManager().getAllEvilBeacons().size();
        int totalBeacons = this.plugin.getBeaconManager().getAllBeacons().size();
        if (evilCount >= totalBeacons && totalBeacons > 0 && !this.plugin.getSessionManager().isVampiresEternalNightActive()) {
            this.triggerVampiresEternalNight();
        }

    }

    private void triggerVampiresEternalNight() {
        this.plugin.getLogger().info("VAMPIRES ETERNAL NIGHT TRIGGERED - All beacons are now evil!");

        for(Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle("§4§lETERNAL NIGHT FALLS", "§cThe darkness consumes all hope", 20, 100, 40);
            player.sendMessage("§c All beacons now pulse with unholy energy.");
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.MASTER, 1.0F, 0.5F);
        }

        for(Player player : Bukkit.getOnlinePlayers()) {
            if (this.plugin.getVampireManager().isHuman(player)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, -1, 0, false, false, true));
            }
        }

        this.plugin.getSessionManager().setVampiresEternalNightActive(true);
    }

    private void performPermadeath(Player caster, Player target, BeaconSite holyBeacon) {
        caster.sendMessage("§4" + target.getName() + " has refused redemption...");
        caster.sendMessage("§7The creature chooses death over humanity...");
        caster.sendMessage("§8Their wish is granted...");
        target.sendTitle("§4§lETERNAL REST", "§8You embrace the void", 10, 60, 20);
        target.sendMessage("§4You refuse the holy words and choose oblivion...");
        target.sendMessage("§8The darkness claims you one final time...");
        target.sendMessage("§8Your journey ends here...");

        for(Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(caster) && !onlinePlayer.equals(target)) {
                if (this.plugin.getVampireManager().isVampire(onlinePlayer)) {
                    onlinePlayer.sendMessage("§4One of your kind has chosen eternal death over forced redemption... The darkness mourns their passing...");
                } else {
                    onlinePlayer.sendMessage("§8A vampire has refused the light and embraced final death... Their essence fades from this world forever...");
                }
            }
        }

        this.createVampireDeathEffects(target.getLocation());
        target.setGameMode(GameMode.SPECTATOR);
        target.sendMessage("");
        target.sendMessage("§4§l§m                                                    ");
        target.sendMessage("§4§lYOU HAVE CHOSEN PERMADEATH");
        target.sendMessage("§7Your journey has ended.");
        target.sendMessage("§4§l§m                                                    ");
        target.sendMessage("");
        DeathHandler.checkAndAnnounceTeamElimination(this.plugin, false, true);
    }

    private void createVampireDeathEffects(Location deathLocation) {
        if (deathLocation.getWorld() != null) {
            Location centerLoc = deathLocation.clone().add((double)0.0F, (double)1.0F, (double)0.0F);
            deathLocation.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, centerLoc, 60, (double)1.5F, (double)1.0F, (double)1.5F, 0.1);
            deathLocation.getWorld().spawnParticle(Particle.FLAME, centerLoc, 40, 1.2, 0.8, 1.2, 0.08);
            deathLocation.getWorld().spawnParticle(Particle.WHITE_ASH, centerLoc, 50, (double)1.0F, (double)1.5F, (double)1.0F, 0.05);
            deathLocation.getWorld().spawnParticle(Particle.LARGE_SMOKE, centerLoc, 30, 1.8, 1.2, 1.8, 0.02);
            deathLocation.getWorld().playSound(deathLocation, Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.5F, 0.8F);
            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                deathLocation.getWorld().spawnParticle(Particle.WHITE_ASH, centerLoc, 30, (double)1.5F, (double)2.0F, (double)1.5F, 0.03);
                deathLocation.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, centerLoc, 20, (double)1.0F, (double)0.5F, (double)1.0F, 0.02);
                deathLocation.getWorld().playSound(deathLocation, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 1.0F, 1.2F);
            }, 20L);
            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                deathLocation.getWorld().spawnParticle(Particle.LARGE_SMOKE, centerLoc, 15, (double)2.0F, 1.8, (double)2.0F, 0.01);
                deathLocation.getWorld().spawnParticle(Particle.WHITE_ASH, centerLoc, 10, 1.8, (double)2.5F, 1.8, 0.02);
            }, 40L);
        }
    }

    public void shutdown() {
        this.pendingCures.clear();
    }

    public static class ForcedCureData {
        public final UUID casterUUID;
        public final UUID targetUUID;
        public final BeaconSite holyBeacon;
        public final boolean hadFlightBefore;
        public final boolean wasInvulnerableBefore;

        public ForcedCureData(UUID casterUUID, UUID targetUUID, BeaconSite holyBeacon, boolean hadFlightBefore, boolean wasInvulnerableBefore) {
            this.casterUUID = casterUUID;
            this.targetUUID = targetUUID;
            this.holyBeacon = holyBeacon;
            this.hadFlightBefore = hadFlightBefore;
            this.wasInvulnerableBefore = wasInvulnerableBefore;
        }

        public Player getCaster() {
            return Bukkit.getPlayer(this.casterUUID);
        }

        public Player getTarget() {
            return Bukkit.getPlayer(this.targetUUID);
        }
    }
}
