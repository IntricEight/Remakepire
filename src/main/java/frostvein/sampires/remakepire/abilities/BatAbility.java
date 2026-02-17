package frostvein.sampires.remakepire.abilities;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.VampireManager;

public class BatAbility extends VampireAbility {
    public String getName() {
        return "bat";
    }

    public String getDisplayName() {
        return "Bat Transformation";
    }

    public String getDescription() {
        return "Transform into a vulnerable bat to fly freely. Use again to transform back. Duration: 2 minutes. WARNING: If your bat dies, you die!";
    }

    public int getCooldownSeconds(RemakepirePlugin plugin) {
        return plugin.getConfigManager().getVampireBatCooldown();
    }

    public int getMinimumStage() {
        return 1;
    }

    public boolean execute(Player player, VampireManager vampireManager, RemakepirePlugin plugin) {
        if (plugin.getBatTransformationManager().isInBatForm(player)) {
            boolean success = plugin.getBatTransformationManager().transformToHuman(player);
            if (success) {
                player.sendMessage("§6You transform back into your vampiric form.");
                player.playSound(player, Sound.ENTITY_BAT_TAKEOFF, SoundCategory.MASTER, 0.8F, 0.8F);
            } else {
                player.sendMessage("§cFailed to transform back to human form.");
            }

            return success;
        } else {
            boolean success = plugin.getBatTransformationManager().transformToBat(player);
            if (success) {
                player.sendMessage("§cIn a flurry of wings, you transform into a bat");
                player.sendMessage("§cBe warned, if you die in bat form, your human form dies too");
                player.sendMessage("§7Use §e/pow vability bat §7again to transform back early.");
                player.playSound(player, Sound.ENTITY_BAT_AMBIENT, SoundCategory.MASTER, 1.0F, 1.2F);
            } else {
                player.sendMessage("§cFailed to transform into bat form.");
            }

            return success;
        }
    }

    protected String getAdditionalRequirementMessage(Player player, VampireManager vampireManager) {
        return "Your vampiric powers are not strong enough for transformation!";
    }
}
