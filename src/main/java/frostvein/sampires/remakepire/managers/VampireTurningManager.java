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
    private final Map<UUID, Boolean> turningEnabled = new HashMap();

    public VampireTurningManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isTurningEnabled(Player vampire) {
        return (Boolean)this.turningEnabled.getOrDefault(vampire.getUniqueId(), true);
    }

    public boolean toggleTurning(Player vampire) {
        boolean currentState = this.isTurningEnabled(vampire);
        boolean newState = !currentState;
        this.turningEnabled.put(vampire.getUniqueId(), newState);
        this.updateLuckEffect(vampire, newState);
        return newState;
    }

    public void setTurningEnabled(Player vampire, boolean enabled) {
        this.turningEnabled.put(vampire.getUniqueId(), enabled);
        this.updateLuckEffect(vampire, enabled);
    }

    public void disableAllVampireTurning() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            if (this.plugin.getVampireManager().isVampire(player)) {
                this.turningEnabled.put(player.getUniqueId(), false);
                this.updateLuckEffect(player, false);
            }
        }

    }

    public void enableAllVampireTurning() {
        this.turningEnabled.clear();

        for(Player player : Bukkit.getOnlinePlayers()) {
            if (this.plugin.getVampireManager().isVampire(player)) {
                this.updateLuckEffect(player, true);
            }
        }

    }

    private void updateLuckEffect(Player vampire, boolean turningEnabled) {
        if (turningEnabled) {
            vampire.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, -1, 0, false, false, true));
        } else {
            vampire.removePotionEffect(PotionEffectType.LUCK);
        }

    }

    public void applyLuckEffectIfEnabled(Player vampire) {
        if (this.isTurningEnabled(vampire)) {
            this.updateLuckEffect(vampire, true);
        }

    }

    public void shutdown() {
        this.turningEnabled.clear();
    }
}
