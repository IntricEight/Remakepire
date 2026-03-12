package frostvein.sampires.remakepire.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class VampireTurningManager {
    private final RemakepirePlugin plugin;
    private final Map<UUID, Boolean> turningEnabled = new HashMap<>();

    /**
     * Create an instance of the Vampire Turning manager.
     *
     * @param plugin the host plugin object.
     */
    public VampireTurningManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Check if the vampire has turning off.
     *
     * @param vampire the player being checked.
     * @return {@code true} if the vampire has turned enabled.
     */
    public boolean isTurningEnabled(Player vampire) {
        return this.turningEnabled.getOrDefault(vampire.getUniqueId(), true);
    }

    /**
     * Toggle the vampire's turning on or off.
     *
     * @param vampire the player toggling their preference.
     * @return {@code true} if turning is now enabled, {@code false} if turning is now disabled.
     */
    public boolean toggleTurning(Player vampire) {
        boolean currentState = this.isTurningEnabled(vampire);
        boolean newState = !currentState;

        this.turningEnabled.put(vampire.getUniqueId(), newState);
        this.updateLuckEffect(vampire, newState);

        return newState;
    }

    /**
     * Enable or disable the vampire's turning ability.
     *
     * @param vampire the player setting their preference.
     * @param enabled {@code true} if turning should be enabled.
     */
    public void setTurningEnabled(Player vampire, boolean enabled) {
        this.turningEnabled.put(vampire.getUniqueId(), enabled);
        this.updateLuckEffect(vampire, enabled);
    }

    /**
     * Turn off all the vampires' turning ability.
     */
    public void disableAllVampireTurning() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            if (this.plugin.getVampireManager().isVampire(player)) {
                this.turningEnabled.put(player.getUniqueId(), false);
                this.updateLuckEffect(player, false);
            }
        }
    }

    /**
     * Turn on all the vampires' turning ability.
     */
    public void enableAllVampireTurning() {
        this.turningEnabled.clear();

        for(Player player : Bukkit.getOnlinePlayers()) {
            if (this.plugin.getVampireManager().isVampire(player)) {
                this.updateLuckEffect(player, true);
            }
        }
    }

    /**
     * Update the potion effect icon of the turning ability.
     *
     * @param vampire the player gaining the effect.
     * @param turningEnabled {@code true} if turning is enabled.
     */
    private void updateLuckEffect(Player vampire, boolean turningEnabled) {
        if (turningEnabled) {
            vampire.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, -1, 0, false, false, true));
        } else {
            vampire.removePotionEffect(PotionEffectType.LUCK);
        }
    }

    /**
     * Apply the potion effect icon of vampire turning.
     *
     * @param vampire the player gaining the effect.
     */
    public void applyLuckEffectIfEnabled(Player vampire) {
        if (this.isTurningEnabled(vampire)) {
            this.updateLuckEffect(vampire, true);
        }
    }

    /**
     * Clear the list of vampires with turning enabled before shutting down the manager.
     */
    public void shutdown() {
        this.turningEnabled.clear();
    }
}
