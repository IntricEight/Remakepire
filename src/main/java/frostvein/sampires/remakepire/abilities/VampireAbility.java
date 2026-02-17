package frostvein.sampires.remakepire.abilities;

import org.bukkit.entity.Player;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.VampireManager;

public abstract class VampireAbility {
    public abstract String getName();

    public abstract String getDisplayName();

    public abstract String getDescription();

    public abstract int getCooldownSeconds(RemakepirePlugin plugin);

    public abstract int getMinimumStage();

    public boolean canUse(Player player, VampireManager vampireManager) {
        if (!vampireManager.isVampire(player)) {
            return false;
        } else {
            int playerStage = vampireManager.getVampireStage(player);
            return playerStage < this.getMinimumStage() ? false : this.canUseAdditionalRequirements(player, vampireManager);
        }
    }

    protected boolean canUseAdditionalRequirements(Player player, VampireManager vampireManager) {
        return true;
    }

    public String getRequirementMessage(Player player, VampireManager vampireManager) {
        if (!vampireManager.isVampire(player)) {
            return "You must be a vampire to use this ability!";

        } else {
            int playerStage = vampireManager.getVampireStage(player);

            if (playerStage < this.getMinimumStage()) {
                return "You must be at least a Stage " + this.getMinimumStage() + " vampire to use " + this.getDisplayName() + "! (You are Stage " + playerStage + ")";

            } else {
                return this.getAdditionalRequirementMessage(player, vampireManager);
            }
        }
    }

    protected String getAdditionalRequirementMessage(Player player, VampireManager vampireManager) {
        return "You cannot use this ability right now!";
    }

    public abstract boolean execute(Player player, VampireManager vampireManager, RemakepirePlugin plugin);
}
