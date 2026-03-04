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
        return "Transform into a vulnerable slime to travel freely. Use again to transform back. Duration: 2 minutes. WARNING: If your true form dies, you die!";
    }

    public int getCooldownSeconds(RemakepirePlugin plugin) {
        return plugin.getConfigManager().getVampireBatCooldown();
    }

    public int getMinimumStage() {
        return 1;
    }

    public boolean execute(Player player, VampireManager vampireManager, RemakepirePlugin plugin) {
        if (plugin.getBatTransformationManager().isInBatForm(player)) {
            if (plugin.getBatTransformationManager().transformToHuman(player)) {
                player.sendMessage("§6You transform back into your humanoid form.");
                player.playSound(player, Sound.ENTITY_SLIME_JUMP_SMALL, SoundCategory.MASTER, 0.8F, 0.8F);
                return true;

            } else {
                player.sendMessage("§cFailed to transform back to human form.");
                return false;
            }
        } else {
            if (plugin.getBatTransformationManager().transformToBat(player)) {
                player.sendMessage("§cYou shed your human frame and return to a small amorphous blob, you can now sneak into vents and through small gaps.");
                player.sendMessage("§cBe warned, if you die in your true form, your human form dies too");
                player.sendMessage("§7Use §e/pow vability bat §7again to transform back early.");

                player.playSound(player, Sound.ENTITY_SLIME_SQUISH, SoundCategory.MASTER, 1.0F, 1.2F);
                return true;

            } else {
                player.sendMessage("§cFailed to transform into your blob form.");
                return false;
            }
        }
    }

    protected String getAdditionalRequirementMessage(Player player, VampireManager vampireManager) {
        return "Your alien powers are not strong enough for transformation!";
    }
}