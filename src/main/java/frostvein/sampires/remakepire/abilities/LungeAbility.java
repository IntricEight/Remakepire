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
        return "Launch yourself forward and upward with supernatural force. Power increases with Mimic stage.";
    }

    public int getCooldownSeconds(RemakepirePlugin plugin) {
        return plugin.getConfigManager().getVampireLungeCooldown();
    }

    public int getMinimumStage() {
        return 2;
    }

    public boolean execute(Player player, VampireManager vampireManager, RemakepirePlugin plugin) {
        int stage = vampireManager.getVampireStage(player);

        vampireManager.addFallProtection(player);
        Vector direction = player.getLocation().getDirection().normalize();
        Vector lungeVector = direction.multiply(this.getLungePower(stage));
        player.setVelocity(lungeVector);

        this.sendLungeMessage(player, stage);
        this.playLungeSound(player, stage);
        return true;
    }

    /**
     * Determine the intensity of the ability based on the vampire's stage.
     *
     * @param stage the vampire stage of the ability user.
     * @return the intensity of the lunge.
     */
    private double getLungePower(int stage) {
        return switch (stage) {
            case 2 -> 2.0;
            case 3 -> 2.5;
            default ->  1.6;
        };
    }

    /**
     * Inform the player of the ability's success.
     *
     * @param player the player using the ability.
     * @param stage the vampire stage of the ability user.
     */
    private void sendLungeMessage(Player player, int stage) {
        player.sendMessage("§b§lYou leap forward with unnatural strength.");
    }

    /**
     * Alert the player of the ability activation with a sound cue.
     *
     * @param player the player using the ability.
     * @param stage the vampire stage of the ability user.
     */
    private void playLungeSound(Player player, int stage) {
        player.playSound(player, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.MASTER, 0.3F, 1.5F);
    }
}
