package frostvein.sampires.remakepire.abilities;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.VampireManager;

public class LungeAbility extends VampireAbility {
    public String getName() {
        return "lunge";
    }

    public String getDisplayName() {
        return "Vampiric Lunge";
    }

    public String getDescription() {
        return "Launch yourself forward and upward with supernatural force. Power increases with vampire stage.";
    }

    public int getCooldownSeconds(RemakepirePlugin plugin) {
        return plugin.getConfigManager().getVampireLungeCooldown();
    }

    public int getMinimumStage() {
        return 2;
    }

    public boolean execute(Player player, VampireManager vampireManager, RemakepirePlugin plugin) {
        int stage = vampireManager.getVampireStage(player);
        double lungePower = this.getLungePower(stage);

        vampireManager.addFallProtection(player);
        Vector direction = player.getLocation().getDirection().normalize();
        Vector lungeVector = direction.multiply(lungePower);
        player.setVelocity(lungeVector);

        this.sendLungeMessage(player, stage);
        this.playLungeSound(player, stage);
        return true;
    }

    private double getLungePower(int stage) {
        switch (stage) {
            case 1 -> {
                return 1.6;
            }
            case 2 -> {
                return 2.0;
            }
            case 3 -> {
                return 2.5;
            }
            default -> {
                return 1.0;
            }
        }
    }

    private void sendLungeMessage(Player player, int stage) {
        player.sendMessage("§c§lYou leap forward with vampiric strength.");
    }

    private void playLungeSound(Player player, int stage) {
        player.playSound(player, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.MASTER, 0.3F, 1.5F);
    }
}
