package frostvein.sampires.remakepire.abilities;

import org.bukkit.entity.Player;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.VampireManager;

public abstract class VampireAbility {
    /**
     * Retrieve the vampire ability's minimal name.
     *
     * @return A {@code String} of the ability name.
     */
    public abstract String getName();

    /**
     * Retrieve the vampire ability's descriptive name.
     *
     * @return A {@code String} of the ability name.
     */
    public abstract String getDisplayName();

    /**
     * Retrieve the vampire ability's description.
     *
     * @return A {@code String} of the ability description.
     */
    public abstract String getDescription();

    /**
     * Retrieve the cooldown of the vampire ability.
     *
     * @param plugin the host plugin object.
     * @return The {@code Integer} seconds between ability uses.
     */
    public abstract int getCooldownSeconds(RemakepirePlugin plugin);

    /**
     * The lowest vampire stage a player must be to use a vampire ability.
     *
     * @return The {@code Integer} vampire stage required to use the ability.
     */
    public abstract int getMinimumStage();

    /**
     * Determines if the player can use the vampire ability.
     *
     * @param player the player using the ability.
     * @param vampireManager the manager for generic vampire traits.
     * @return {@code true} if the {@code player} is a vampire of the proper stage.
     */
    public boolean canUse(Player player, VampireManager vampireManager) {
        if (!vampireManager.isVampire(player)) {
            return false;

        } else {
            int playerStage = vampireManager.getVampireStage(player);
            return playerStage >= this.getMinimumStage() && this.canUseAdditionalRequirements(player, vampireManager);
        }
    }

    /**
     * Determines if the player can use the vampire ability after considering extra requirements.
     *
     * @param player the player using the ability.
     * @param vampireManager the manager for generic vampire traits.
     * @return {@code true} if the {@code player} fulfills the additional requirements.
     */
    protected boolean canUseAdditionalRequirements(Player player, VampireManager vampireManager) {
        return true;
    }

    /**
     * Retrieve the requirements for the ability.
     *
     * @param player the player attempting to use the ability.
     * @param vampireManager the manager for generic vampire traits.
     * @return The {@code String} of the ability requirements.
     */
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

    /**
     * Retrieve the additional requirements for the ability.
     *
     * @param player the player attempting to use the ability.
     * @param vampireManager the manager for generic vampire traits.
     * @return The {@code String} of the additional ability requirements.
     */
    protected String getAdditionalRequirementMessage(Player player, VampireManager vampireManager) {
        return "You cannot use this ability right now!";
    }

    /**
     * Attempt to use the vampire ability.
     *
     * @param player the player attempting to use the ability.
     * @param vampireManager the manager for generic vampire traits.
     * @param plugin the host plugin object.
     * @return {@code true} if the ability was used and the cooldown started.
     */
    public abstract boolean execute(Player player, VampireManager vampireManager, RemakepirePlugin plugin);
}
