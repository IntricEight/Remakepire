package frostvein.sampires.remakepire.managers;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
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
import frostvein.sampires.remakepire.beacons.BeaconSite;
import frostvein.sampires.remakepire.beacons.BeaconSite.BeaconState;
import frostvein.sampires.remakepire.listeners.DeathHandler;

public class ForcedCureChoiceManager {
    private final RemakepirePlugin plugin;
    private final Map<UUID, ForcedCureData> pendingCures = new HashMap<>();
    public static final Component CURE_CHOICE_GUI_TITLE = Component.text("Your Fate Awaits...", NamedTextColor.DARK_RED)
            .decorate(TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false);

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
     * @param holyBeacon the beacon being used for the cure.
     */
    public void openChoiceGUI(Player caster, Player target, BeaconSite holyBeacon) {
        boolean hadFlight = target.getAllowFlight();
        boolean wasInvulnerable = target.isInvulnerable();
        this.pendingCures.put(target.getUniqueId(), new ForcedCureData(caster.getUniqueId(), target.getUniqueId(), holyBeacon, hadFlight, wasInvulnerable));
        this.applyEffectsAndOpenGUI(target);
    }

    /**
     * Reopen the forced cure choice GUI for the target.
     *
     * @param target the player who must make the decision.
     */
    public void reopenChoiceGUI(Player target) {
        ForcedCureData data = this.getPendingCure(target);

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

        Inventory gui = Bukkit.createInventory(null, 27, CURE_CHOICE_GUI_TITLE);
        ItemStack humanityButton = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta humanityMeta = humanityButton.getItemMeta();

        if (humanityMeta != null) {
            humanityMeta.customName(
                    Component.text("Return to Humanity", NamedTextColor.GREEN)
                            .decorate(TextDecoration.BOLD)
                            .decoration(TextDecoration.ITALIC, false)
            );
            humanityMeta.lore(Arrays.asList(
                    Component.text("The holy words have broken your curse.", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("You can feel your humanity returning...", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Click to accept your return to mortality.", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            humanityButton.setItemMeta(humanityMeta);
        }

        ItemStack deathButton = new ItemStack(Material.SKELETON_SKULL);
        ItemMeta deathMeta = deathButton.getItemMeta();

        if (deathMeta != null) {
            deathMeta.customName(
                    Component.text("Finally Accept Death", NamedTextColor.DARK_RED)
                            .decorate(TextDecoration.BOLD)
                            .decoration(TextDecoration.ITALIC, false)
            );
            deathMeta.lore(Arrays.asList(
                    Component.text("You have lived too long as a creature", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("of darkness. Perhaps it is time to rest...", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("WARNING: This will result in permadeath!", NamedTextColor.RED)
                            .decorate(TextDecoration.BOLD)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Click to embrace the eternal sleep.", NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            deathButton.setItemMeta(deathMeta);
        }

        gui.setItem(11, humanityButton);
        gui.setItem(15, deathButton);
        target.openInventory(gui);

        target.sendMessage("");
        target.sendMessage(Component.text("                                                    ", NamedTextColor.DARK_RED)
                .decorate(TextDecoration.BOLD)
                .decorate(TextDecoration.STRIKETHROUGH)
        );
        target.sendMessage(Component.text("HOLY WORDS PIERCE YOUR SOUL", NamedTextColor.DARK_RED)
                .decorate(TextDecoration.BOLD));
        target.sendMessage(Component.text("You feel the curse tearing away from your being...", NamedTextColor.GRAY));
        target.sendMessage(Component.text("But you have a choice to make...", NamedTextColor.GRAY));
        target.sendMessage(Component.text("                                                    ", NamedTextColor.DARK_RED)
                .decorate(TextDecoration.BOLD)
                .decorate(TextDecoration.STRIKETHROUGH)
        );
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
        ForcedCureData data = this.getPendingCure(target);

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
            this.performCure(caster, target, data.holyBeacon);
        }
    }

    /**
     * Handle the player's decision to die.
     *
     * @param target the player who must make the decision.
     */
    public void handleDeathChoice(Player target) {
        ForcedCureData data = this.getPendingCure(target);

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
            this.performPermadeath(caster, target, data.holyBeacon);
        }
    }

    /**
     * Cure the target of vampirism forcefully.
     *
     * @param caster the player forcing the cure.
     * @param target the player who must make the decision.
     * @param holyBeacon the beacon being used for the cure.
     */
    private void performCure(Player caster, Player target, BeaconSite holyBeacon) {
        // Inform the cure caster that the forced cure was successful
        caster.sendMessage(Component.text(target.getName() + " has chosen to return to humanity...", NamedTextColor.GOLD));
        caster.sendMessage(Component.text("The creature of darkness accepts their redemption...", NamedTextColor.GRAY));
        caster.sendMessage(Component.text("You have sanctified " + target.getName() + ", and they have accepted.", NamedTextColor.GREEN));

        // Inform the former vampire that they are human once more
        target.showTitle(Title.title(
                Component.text("REDEEMED", NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD),
                Component.text("You have chosen humanity", NamedTextColor.YELLOW),
                Title.Times.times(
                        Duration.ofMillis(500),     // 1/2 second
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(1)
                )
        ));

        target.sendMessage(Component.text("You accept the holy words and choose to return...", NamedTextColor.GREEN));
        target.sendMessage(Component.text("The holy water burns through your veins...", NamedTextColor.GRAY));
        target.sendMessage(Component.text("Your corrupted blood boils away in divine light...", NamedTextColor.GRAY));
        target.sendMessage(Component.text("You feel your humanity returning...", NamedTextColor.GREEN));
        target.sendMessage(Component.text("You are human once more.", NamedTextColor.GREEN));
        target.sendMessage(Component.text("But the holy site has been permanently corrupted by your dark presence...", NamedTextColor.DARK_GRAY));

        // Retrieve the messages to announce to the server population
        final String messageToHumans = this.plugin.getCureBookManager().getForceCureAnnouncementMessage(true, true);
        final String messageToVampires = this.plugin.getCureBookManager().getForceCureAnnouncementMessage(false, true);

        // Alert all players that a vampire has been cured
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(caster) && !onlinePlayer.equals(target)) {
                if (this.plugin.getVampireManager().isVampire(onlinePlayer)) {
                    onlinePlayer.sendMessage(messageToVampires);
                } else {
                    onlinePlayer.sendMessage(messageToHumans);
                }
            }
        }

        this.plugin.getVampireManager().setPlayerAsHuman(target);
        target.getActivePotionEffects().forEach((effect) -> target.removePotionEffect(effect.getType()));
        target.addScoreboardTag(VampireManager.CURED_VAMPIRE_TAG);

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
        this.createBeaconCorruptionEffects(target, holyBeacon);
        holyBeacon.setState(BeaconState.PERMANENTLY_DESECRATED);

        this.plugin.getBeaconManager().updateBeaconDisplay(holyBeacon);
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
     * Perform the visual and message effects of a vampire permadeath.<br/>
     * The beacon will not break from a permadeath cure.
     *
     * @param caster the player forcing the cure.
     * @param target the player who must make the decision.
     * @param holyBeacon the beacon being used for the cure.
     */
    private void performPermadeath(Player caster, Player target, BeaconSite holyBeacon) {
        // Inform the cure caster that the forced cure was rejected
        caster.sendMessage(Component.text(target.getName() + " has refused redemption...", NamedTextColor.DARK_RED));
        caster.sendMessage(Component.text("The creature chooses death over humanity...", NamedTextColor.GRAY));
        caster.sendMessage(Component.text("Their wish is granted...", NamedTextColor.DARK_GRAY));

        // Inform the vampire that they are now dead
        target.showTitle(Title.title(
                Component.text("ETERNAL REST", NamedTextColor.DARK_RED)
                        .decorate(TextDecoration.BOLD),
                Component.text("You embrace the void", NamedTextColor.DARK_GRAY),
                Title.Times.times(
                        Duration.ofMillis(500),     // 1/2 second
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(1)
                )
        ));

        target.sendMessage(Component.text("You refuse the holy words and choose oblivion...", NamedTextColor.DARK_RED));
        target.sendMessage(Component.text("The darkness claims you one final time...", NamedTextColor.DARK_GRAY));
        target.sendMessage(Component.text("Your journey ends here...", NamedTextColor.DARK_GRAY));

        // Retrieve the messages to announce to the server population
        final String messageToHumans = this.plugin.getCureBookManager().getForceCureAnnouncementMessage(true, false);
        final String messageToVampires = this.plugin.getCureBookManager().getForceCureAnnouncementMessage(false, false);

        // Alert all players that a vampire has been killed by the cure
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(caster) && !onlinePlayer.equals(target)) {
                if (this.plugin.getVampireManager().isVampire(onlinePlayer)) {
                    onlinePlayer.sendMessage(messageToVampires);
                } else {
                    onlinePlayer.sendMessage(messageToHumans);
                }
            }
        }

        this.plugin.getDeathHandler().createVampireDeathEffects(target.getLocation());
        target.setGameMode(GameMode.SPECTATOR);
        target.addScoreboardTag(DeathHandler.PERMAKILLED_TAG);

        target.sendMessage("");
        target.sendMessage(Component.text("                                                    ", NamedTextColor.DARK_RED)
                .decorate(TextDecoration.BOLD)
                .decorate(TextDecoration.STRIKETHROUGH)
        );
        target.sendMessage(Component.text("YOU HAVE CHOSEN PERMADEATH", NamedTextColor.DARK_RED)
                .decorate(TextDecoration.BOLD));
        target.sendMessage(Component.text("Your journey has ended.", NamedTextColor.GRAY));
        target.sendMessage(Component.text("                                                    ", NamedTextColor.DARK_RED)
                .decorate(TextDecoration.BOLD)
                .decorate(TextDecoration.STRIKETHROUGH)
        );
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
     * Create the visual and audio effects of destroying a beacon.
     *
     * @param player the player being cured.
     * @param beacon the beacon being corrupted.
     */
    public void createBeaconCorruptionEffects(Player player, BeaconSite beacon) {
        Location beaconLocation = beacon.getLocation();

        if (beaconLocation != null) {
            player.getWorld().spawnParticle(Particle.LARGE_SMOKE, beaconLocation.clone().add(0.0, 1.5, 0.0), 50, 0.5, 1.0, 0.5, 0.05);
            player.getWorld().spawnParticle(Particle.SMOKE, beaconLocation.clone().add(0.0, 1.5, 0.0), 30, 0.3, 0.8, 0.3, 0.02);
            player.getWorld().playSound(beaconLocation, Sound.ENTITY_WITHER_HURT, SoundCategory.MASTER, 0.8F, 0.6F);
        }
    }

    /**
     * Clear the list of pending cures before shutting down the manager.
     */
    public void shutdown() {
        this.pendingCures.clear();
    }

    public static class ForcedCureData {
        public final UUID casterUUID, targetUUID;
        public final BeaconSite holyBeacon;
        public final boolean hadFlightBefore, wasInvulnerableBefore;

        /**
         * Create an instance of the forced cure's data record.
         *
         * @param casterUUID the UUID of the player forcing the cure.
         * @param targetUUID the UUID of the player who must make the decision.
         * @param holyBeacon the beacon being used for the cure.
         * @param hadFlightBefore {@code true} if the player previously had an attribute which granted them flight.
         * @param wasInvulnerableBefore {@code true} if the player previously had an attribute which made them invincible.
         */
        public ForcedCureData(UUID casterUUID, UUID targetUUID, BeaconSite holyBeacon, boolean hadFlightBefore, boolean wasInvulnerableBefore) {
            this.casterUUID = casterUUID;
            this.targetUUID = targetUUID;
            this.holyBeacon = holyBeacon;
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
