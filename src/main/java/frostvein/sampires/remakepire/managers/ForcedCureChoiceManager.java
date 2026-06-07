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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.listeners.DeathHandler;

public class ForcedCureChoiceManager {
    private final RemakepirePlugin plugin;
    private final Map<UUID, ForcedCureData> pendingCures = new HashMap<>();

    /**
     * Create an instance of the Force Cure Choice manager.
     *
     * @param plugin the host plugin object.
     */
    public ForcedCureChoiceManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Freeze the target and begin the force cure process.
     *
     * @param caster the player forcing the cure.
     * @param target the player who must make the decision.
     */
    public void openChoiceGUI(Player caster, Player target) {
        boolean hadFlight = target.getAllowFlight();
        boolean wasInvulnerable = target.isInvulnerable();
        this.pendingCures.put(target.getUniqueId(), new ForcedCureData(caster.getUniqueId(), target.getUniqueId(), hadFlight, wasInvulnerable));
        this.applyEffectsAndOpenGUI(target);
    }

    /**
     * Reopen the forced cure choice GUI for the target.
     *
     * @param target the player who must make the decision.
     */
    public void reopenChoiceGUI(Player target) {
        ForcedCureData data = this.pendingCures.get(target.getUniqueId());

        if (data != null) {
            this.applyEffectsAndOpenGUI(target);
        }
    }

    /**
     * Open up the forced cure choice GUI for the target.
     *
     * @param target the player who must make the decision.
     */
    private void applyEffectsAndOpenGUI(Player target) {
        target.setAllowFlight(true);
        target.setFlying(true);
        target.setInvulnerable(true);

        Inventory gui = Bukkit.createInventory(null, 27, "§4§lYour Fate Awaits...");
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

    /**
     * Retrieve if the target must make a choice on whether to be cured or killed.
     *
     * @param player the player who must make the cure decision.
     * @return {@code true} if the target has an active cure choice.
     */
    public boolean hasPendingCure(Player player) {
        return this.pendingCures.containsKey(player.getUniqueId());
    }

    /**
     * Retrieve the data about a cure choice in progress.
     *
     * @param player the player who must make the cure decision.
     * @return The target's cure decision data.
     */
    public ForcedCureData getPendingCure(Player player) {
        return this.pendingCures.get(player.getUniqueId());
    }

    /**
     * Remove the pending cure choice's information from the active cure choices.
     *
     * @param player the player who no longer needs to make a cure decision.
     */
    public void removePendingCure(Player player) {
        this.pendingCures.remove(player.getUniqueId());
    }

    /**
     * Handle the player's decision to be cured.
     *
     * @param target the player who must make the decision.
     */
    public void handleHumanityChoice(Player target) {
        ForcedCureData data = this.pendingCures.get(target.getUniqueId());

        if (data != null) {
            target.closeInventory();
            target.setInvulnerable(data.wasInvulnerableBefore);
            target.setAllowFlight(data.hadFlightBefore);

            if (!data.hadFlightBefore) {
                target.setFlying(false);
            }

            this.removePendingCure(target);
            Player caster = data.getCaster();
            this.plugin.logInfo("FORCED CURE CHOICE: " + target.getName() + " chose to return to humanity");
            this.performCure(caster, target);
        }
    }

    /**
     * Handle the player's decision to die.
     *
     * @param target the player who must make the decision.
     */
    public void handleDeathChoice(Player target) {
        ForcedCureData data = this.pendingCures.get(target.getUniqueId());

        if (data != null) {
            target.closeInventory();
            target.setInvulnerable(data.wasInvulnerableBefore);
            target.setAllowFlight(data.hadFlightBefore);

            if (!data.hadFlightBefore) {
                target.setFlying(false);
            }

            this.removePendingCure(target);
            Player caster = data.getCaster();
            this.plugin.logInfo("FORCED CURE CHOICE: " + target.getName() + " chose permadeath over cure");
            this.performPermadeath(caster, target);
        }
    }

    /**
     * Cure the player of vampirism.
     *
     * @param player the vampire being cured.
     */
    public void performCure(Player player) {
        player.sendTitle("§6§lCURED", "§eThe curse is lifted", 10, 60, 20);
        player.sendMessage("§7The mixture burns through your veins...");
        player.sendMessage("§7The corrupted blood boils away...");
        player.sendMessage("§aYou feel your humanity returning...");
        player.sendMessage("§aYou are cured. You are human once more.");

        // Alert all players that a vampire has been cured
        for(Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(player)) {
                if (this.plugin.getVampireManager().isVampire(onlinePlayer)) {
                    onlinePlayer.sendMessage("§4A disturbance ripples through the darkness... One of your kind has abandoned the gift of immortality...");
                } else {
                    onlinePlayer.sendMessage("§aA vampire has rejected the curse of immortality and has been cured.");
                }
            }
        }

        this.plugin.getVampireManager().setPlayerAsHuman(player);
        player.getActivePotionEffects().forEach((effect) -> player.removePotionEffect(effect.getType()));
        player.addScoreboardTag("CuredVampire");

        // Check for and apply the effects of beacon control
        if (this.plugin.getSessionManager().isHumansFinalStandActive()) {
            // Restore the human's health when humans control all beacons
            this.plugin.getEffectManager().removeHumansFinalStandHealthReduction(player);

        } else if (this.plugin.getSessionManager().isVampiresEternalNightActive()) {
            // Apply blindness to the human if vampires control all beacons
            this.plugin.getEffectManager().applyEternalNightDarkness(player);
        }

        // Create the visual and audio effects of the cure working on the vampire
        this.plugin.getForcedCureChoiceManager().createCureEffects(player);

        if (this.plugin.getVampireTurningManager() != null) {
            this.plugin.getVampireTurningManager().disableAllVampireTurning();
        }

        this.plugin.logInfo("VAMPIRE CURE: " + player.getName() + " has been cured");
        DeathHandler.checkAndAnnounceTeamElimination(this.plugin, false, true);
    }

    /**
     * Create the visual and auditory effects of initiating the force cure, and open the force cure choice GUI.
     *
     * @param healer the player enacting the force cure.
     * @param target the vampire being cured.
     */
    public void executeForceVampireCure(Player healer, Player target) {
        healer.sendMessage("§6Your syringe empties into the vampire's veins");
        healer.sendMessage("§7Divine light tears through the creature's cursed form...");
        healer.sendMessage("§e" + target.getName() + " must now choose their fate...");

        Location targetLoc = target.getLocation();

        targetLoc.getWorld().spawnParticle(Particle.END_ROD, targetLoc.clone().add(0.0, 1.0, 0.0), 50, 0.3, 1.0, 0.3, 0.1);
        targetLoc.getWorld().spawnParticle(Particle.ENCHANT, targetLoc.clone().add(0.0, 1.0, 0.0), 60, 0.5, 1.5, 0.5, 0.5);
        targetLoc.getWorld().spawnParticle(Particle.WHITE_ASH, targetLoc.clone().add(0.0, 1.0, 0.0), 40, 0.4, 1.2, 0.4, 0.05);
        targetLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, targetLoc, 1, 0.0, 0.0, 0.0, 0.0);

        targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1.5F, 1.0F);
        targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0F, 1.2F);
        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.5F, 1.5F);

        this.openChoiceGUI(healer, target);
    }

    /**
     * Cure the target of vampirism forcefully.
     *
     * @param caster the player forcing the cure.
     * @param target the player who must make the decision.
     */
    private void performCure(Player caster, Player target) {
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

        // Alert all players that a vampire has been cured
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

        // Check for and apply the effects of beacon control
        if (this.plugin.getSessionManager().isHumansFinalStandActive()) {
            // Restore the human's health when humans control all beacons
            this.plugin.getEffectManager().removeHumansFinalStandHealthReduction(target);

        } else if (this.plugin.getSessionManager().isVampiresEternalNightActive()) {
            // Apply blindness to the human if vampires control all beacons
            this.plugin.getEffectManager().applyEternalNightDarkness(target);
        }

        // Create the visual and audio effects of the cure working on the vampire
        this.createCureEffects(target);

        this.plugin.getBeaconManager().saveBeacons();
        this.plugin.getBeaconMajorityManager().updateBeaconMajorityBonuses();
        this.plugin.getBeaconManager().checkAndBroadcastCompleteControl();
        this.plugin.getBeaconConversionListener().triggerIfAllBeaconsEvil();

        if (this.plugin.getVampireTurningManager() != null) {
            this.plugin.getVampireTurningManager().disableAllVampireTurning();
        }

        DeathHandler.checkAndAnnounceTeamElimination(this.plugin, false, true);
    }

    /**
     * Perform the visual and message effects of a vampire permadeath.
     *
     * @param caster the player forcing the cure.
     * @param target the player who must make the decision.
     */
    private void performPermadeath(Player caster, Player target) {
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

        this.plugin.getDeathHandler().createVampireDeathEffects(target.getLocation());
        target.setGameMode(GameMode.SPECTATOR);
        target.sendMessage("");
        target.sendMessage("§4§l§m                                                    ");
        target.sendMessage("§4§lYOU HAVE CHOSEN PERMADEATH");
        target.sendMessage("§7Your journey has ended.");
        target.sendMessage("§4§l§m                                                    ");
        target.sendMessage("");

        DeathHandler.checkAndAnnounceTeamElimination(this.plugin, false, true);
    }

    /**
     * Create the visual and audio effects of a successful vampire cure.
     *
     * @param player the player being cured.
     */
    public void createCureEffects(Player player) {
        Location playerLocation = player.getLocation();

        // Create the visual and audio effects of the cure working on the vampire
        player.getWorld().spawnParticle(Particle.SOUL, playerLocation, 100, 1.0, 2.0, 1.0, 0.1);
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, playerLocation, 1, 0.5, 1.0, 0.5, 0.0);
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, playerLocation, 5, 1.0, 1.0, 1.0, 0.0);
        player.getWorld().playSound(playerLocation, Sound.BLOCK_BELL_USE, SoundCategory.MASTER, 1.5F, 0.8F);
        player.getWorld().playSound(playerLocation, Sound.BLOCK_GLASS_BREAK, SoundCategory.MASTER, 1.0F, 1.0F);
        player.getWorld().playSound(playerLocation, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, SoundCategory.MASTER, 1.0F, 1.5F);
    }

    /**
     * Clear the list of pending cures before shutting down the manager.
     */
    public void shutdown() {
        this.pendingCures.clear();
    }

    public static class ForcedCureData {
        public final UUID casterUUID, targetUUID;
        public final boolean hadFlightBefore, wasInvulnerableBefore;

        /**
         * Create an instance of the forced cure's data record.
         *
         * @param casterUUID the UUID of the player forcing the cure.
         * @param targetUUID the UUID of the player who must make the decision.
         * @param hadFlightBefore {@code true} if the playr
         * @param wasInvulnerableBefore {@code true} if
         */
        public ForcedCureData(UUID casterUUID, UUID targetUUID, boolean hadFlightBefore, boolean wasInvulnerableBefore) {
            this.casterUUID = casterUUID;
            this.targetUUID = targetUUID;
            this.hadFlightBefore = hadFlightBefore;
            this.wasInvulnerableBefore = wasInvulnerableBefore;
        }

        /**
         * Retrieve the UUID of the player forcing the cure.
         *
         * @return A UUID of the caster.
         */
        public Player getCaster() {
            return Bukkit.getPlayer(this.casterUUID);
        }

        /**
         * Retrieve the UUID of the player who must make the decision.
         *
         * @return A UUID of the target.
         */
        public Player getTarget() {
            return Bukkit.getPlayer(this.targetUUID);
        }
    }
}
