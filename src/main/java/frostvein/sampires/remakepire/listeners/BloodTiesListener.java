/* BLOOD TIES (Listener) - Vampire Ability by IntricEight
 *
 * This ability gives vampires a directional indicator of one of their fledgling's locations.
 *
 * Implementation Requirements:
 * Implement the BloodTiesAbility.java file and follow its instructions
 * Add an instance of BloodTiesListener to RemakepirePlugin.java, register it inside onEnable(), and call the shutdown function inside onDisable()
 */

package frostvein.sampires.remakepire.listeners;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.abilities.BloodTiesAbility;
import frostvein.sampires.remakepire.utils.ConversionAssistant;

public class BloodTiesListener implements Listener {
    private final RemakepirePlugin plugin;
    private final Map<UUID, BukkitTask> activeTrackingSessions = new ConcurrentHashMap<>();
    private static final int TRACKING_DURATION_SECONDS = 30;
    private static final int UPDATE_INTERVAL_TICKS = 4;

    /**
     * Create an instance of the Blood Ties listener.
     *
     * @param plugin the host plugin object.
     */
    public BloodTiesListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Control interactions with the fledgling options UI.
     *
     * @param event a player clicks inside an inventory menu.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().title().equals(BloodTiesAbility.INVENTORY_GUI_TITLE)) {
            event.setCancelled(true);

            if (event.getWhoClicked() instanceof Player player) {
                ItemStack clickedItem = event.getCurrentItem();

                // Check that the player clicked a valid slot in the inventory GUI
                if (clickedItem != null && clickedItem.getType() == Material.PLAYER_HEAD) {
                    ItemMeta meta = clickedItem.getItemMeta();

                    if (meta != null && meta.customName() != null) {
                        // Remove the § code from the front of the player's name
                        String fledglingName = PlainTextComponentSerializer.plainText().serialize(meta.customName());
                        Player fledgling = Bukkit.getPlayerExact(fledglingName);

                        if (fledgling == null) {
                            player.sendMessage(Component.text("Player not found: " + fledglingName, NamedTextColor.RED));
                            player.closeInventory();

                        } else if (!this.plugin.getVampireManager().isVampire(fledgling)) {
                            player.sendMessage(Component.text("Your ties to this player have been severed.", NamedTextColor.RED));
                            player.closeInventory();

                        } else if (!this.plugin.getVampireManager().isVampire(player)) {
                            player.sendMessage(Component.text("Only vampires can track their fledglings.", NamedTextColor.RED));
                            player.closeInventory();

                        } else {
                            //  Begin tracking the fledgling
                            this.beginTrackingFledgling(player, fledgling);
                            player.closeInventory();
                        }
                    }
                }
            }
        }
    }

    /**
     * Begin directing the player toward their fledgling.
     *
     * @param sire the player using the ability.
     * @param fledgling the player being tracked by the sire
     */
    private void beginTrackingFledgling(Player sire, Player fledgling) {
        final UUID fledglingId = fledgling.getUniqueId();

        // If a tracking task already exists, clear it before adding the new one
        this.stopTracking(fledglingId);

        // Create a new tracking task
        BukkitTask trackingTask = (new BukkitRunnable() {
            int ticksRemaining = TRACKING_DURATION_SECONDS * 20;

            public void run() {
                if (this.ticksRemaining <= 0) {
                    BloodTiesListener.this.stopTracking(fledglingId);

                } else {
                    if (BloodTiesListener.this.eligibleToTrack(sire, fledgling)) {
                        BloodTiesListener.this.updateTracking(sire, fledgling);
                        this.ticksRemaining -= UPDATE_INTERVAL_TICKS;

                    } else {
                        BloodTiesListener.this.stopTracking(fledglingId);
                        plugin.getLogger().info("A player is found to be offline during the Blood Ties tracking process");
                    }
                }
            }
        }).runTaskTimer(this.plugin, 0L, UPDATE_INTERVAL_TICKS);

        this.activeTrackingSessions.put(fledglingId, trackingTask);

        // Set the ability's cooldown once it successfully triggers
        if (this.plugin.getVampireAbilityManager().applyCooldownForAbility(sire, "bloodties")) {
            this.plugin.logInfo("Applied beacon travel cooldown for player: " + sire.getName());
        } else {
            this.plugin.getLogger().warning("Failed to apply beacon travel cooldown for player: " + sire.getName());
        }

        // Alert the players of the ability use
        sire.sendMessage(Component.text(" Red mist gathers at the edge of your vision, pointing the way to ." + fledgling.getName(), NamedTextColor.RED));
        fledgling.sendMessage(Component.text("A shiver runs down your spine.", NamedTextColor.GRAY));

        // Play the ability sound effects
        sire.playSound(sire, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.MASTER, 0.5F, 0.3F);
        fledgling.playSound(fledgling, Sound.ENTITY_WARDEN_AGITATED, SoundCategory.MASTER, 0.8F, 0.6F);
    }

    /**
     * Direct the other vampires in the direction of the new fledgling.
     *
     * @param trackedVampire the fledgling being tracked.
     */
    private void updateTracking(Player sire, Player trackedVampire) {
        Location trackedLocation = trackedVampire.getLocation();

        // Make sure nothing is distracting the player or overriding the action bar
        if (this.plugin.getVampireManager().isVampire(sire) && !sire.getUniqueId().equals(trackedVampire.getUniqueId())
                && this.plugin.getVampireManager().isVampire(trackedVampire)
                && sire.getWorld().equals(trackedVampire.getWorld())
                && (this.plugin.getVampireFeedingManager() == null || !this.plugin.getVampireFeedingManager().isFeeding(sire))
                && this.plugin.getVampireTrackingManager().getActiveTrackingCount() == 0
        ) {
            Location vampireLocation = sire.getLocation();

            final double deltaX = trackedLocation.getX() - vampireLocation.getX();
            final double deltaZ = trackedLocation.getZ() - vampireLocation.getZ();

            final String direction = ConversionAssistant.getRelativeDirection(deltaX, deltaZ, vampireLocation.getYaw());

            sire.sendActionBar(Component.text(trackedVampire.getName() + " ", NamedTextColor.DARK_RED)
                    .append(Component.text(direction, NamedTextColor.WHITE)));
        }
    }

    /**
     * Check that the two vampires are eligible to track each other each time the directional indicator updates.
     *
     * @param sire the player using the ability.
     * @param trackedVampire the fledgling being tracked.
     * @return {@code true} if the sire can track the vampire.
     */
    private boolean eligibleToTrack(Player sire, Player trackedVampire) {
        return sire != null && trackedVampire != null
                && trackedVampire.isOnline() && sire.isOnline()
                && !this.plugin.getHolyWaterEffectManager().isAbilitiesDisabled(sire)
                && !this.plugin.getHolyWaterEffectManager().isAbilitiesDisabled(trackedVampire)
                && sire.getGameMode() != GameMode.SPECTATOR || !sire.isDead() || this.plugin.getVampireManager().isVampire(sire)
                && trackedVampire.getGameMode() != GameMode.SPECTATOR || !trackedVampire.isDead() || this.plugin.getVampireManager().isVampire(trackedVampire);
    }

    /**
     * Stop tracking the fledgling.
     *
     * @param fledglingId the UUID of the fledgling.
     */
    public void stopTracking(UUID fledglingId) {
        BukkitTask task = this.activeTrackingSessions.remove(fledglingId);

        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Stop tracking all fledglings through the ability.
     */
    public void stopAllTracking() {
        for (BukkitTask task : this.activeTrackingSessions.values()) {
            task.cancel();
        }

        this.activeTrackingSessions.clear();
        this.plugin.logInfo("Stopped all vampire fledgling tracking sessions");
    }

    /**
     * Retrieve the number of vampires being tracked through the ability.
     *
     * @return The number of ongoing trackers.
     */
    public int getActiveTrackingCount() {
        return this.activeTrackingSessions.size();
    }

    /**
     * Retrieve whether the player is being tracked by other vampires.
     *
     * @param vampireId the UUID of a player.
     * @return {@code true} if the vampire is being tracked.
     */
    public boolean isBeingTracked(UUID vampireId) {
        return this.activeTrackingSessions.containsKey(vampireId);
    }

    /**
     * Stop tracking any fledglings before shutting down the listener.
     */
    public void shutdown() {
        this.stopAllTracking();
        this.plugin.logInfo("BloodTiesListener shutdown complete");
    }
}