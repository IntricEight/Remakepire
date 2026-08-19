package frostvein.sampires.remakepire.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class BeaconMajorityManager {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final BeaconManager beaconManager;
    private final Map<UUID, AttributeModifier> healthModifiers = new HashMap<>();
    private final NamespacedKey HUMAN_MAJORITY_HEALTH_KEY;
    private final NamespacedKey VAMPIRE_MAJORITY_HEALTH_KEY;
    private final NamespacedKey DEATH_PENALTY_HEALTH_KEY;
    private int currentVampireBonus = 0, currentHumanBonus = 0;

    // Relic from before updating to newer content, left in case the UUID is needed to collaborate with
    //    VAMPIRE_MAJORITY_HEALTH_UUID was "a1b2c3d4-5e6f-7890-1234-567890abcdef"
    //    HUMAN_MAJORITY_HEALTH_UUID was "f1e2d3c4-b5a6-9870-4321-fedcba098765"
    //    DEATH_PENALTY_HEALTH_UUID was "d1e2a3d4-b5e6-7890-abcd-1234567890ef"

    /**
     * Create an instance of the Beacon Majority manager.
     *
     * @param plugin the host plugin object.
     */
    public BeaconMajorityManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.beaconManager = plugin.getBeaconManager();

        this.VAMPIRE_MAJORITY_HEALTH_KEY = new NamespacedKey(plugin, "vampire_majority_health");
        this.HUMAN_MAJORITY_HEALTH_KEY = new NamespacedKey(plugin, "human_majority_health");
        this.DEATH_PENALTY_HEALTH_KEY = new NamespacedKey(plugin, "death_penalty_health");
    }

    /**
     * Grant and remove bonuses from teams as beacons change hands.
     */
    public void updateBeaconMajorityBonuses() {
        if (!this.plugin.getSessionManager().isSessionActive()) {
            this.plugin.getLogger().fine("Session not active, skipping beacon majority bonus update");

        } else {
            int holyBeacons = this.beaconManager.getHolyBeacons().size();
            int evilBeacons = this.beaconManager.getAllEvilBeacons().size();
            int difference = Math.abs(holyBeacons - evilBeacons);

            this.plugin.logInfo("Beacon majority check: " + holyBeacons + " holy, " + evilBeacons + " evil (desecrated + permanently desecrated), difference: " + difference);

            if (holyBeacons > evilBeacons) {
                this.applyBonusToHumans(difference);
                this.removeBonusFromVampires();
                this.plugin.logInfo("Humans gain beacon majority bonus: +" + difference + " hearts");

            } else if (evilBeacons > holyBeacons) {
                this.applyBonusToVampires(difference);
                this.removeBonusFromHumans();
                this.plugin.logInfo("Vampires gain beacon majority bonus: +" + difference + " hearts");

            } else {
                this.removeAllBonuses();
            }
        }
    }

    /**
     * Give humans extra hearts.
     *
     * @param bonusHearts the extra hearts to give.
     */
    private void applyBonusToHumans(int bonusHearts) {
        this.currentHumanBonus = bonusHearts;
        this.currentVampireBonus = 0;
        double healthBonus = bonusHearts * 2.0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (this.vampireManager.isHuman(player)) {
                this.applyHealthModifier(player, healthBonus, HUMAN_MAJORITY_HEALTH_KEY);
                this.applyDeathPenalty(player);
            }
        }
    }

    /**
     * Give vampires extra hearts.
     *
     * @param bonusHearts the extra hearts to give.
     */
    private void applyBonusToVampires(int bonusHearts) {
        this.currentVampireBonus = bonusHearts;
        this.currentHumanBonus = 0;
        double healthBonus = bonusHearts * 2.0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (this.vampireManager.isVampire(player)) {
                this.applyHealthModifier(player, healthBonus, VAMPIRE_MAJORITY_HEALTH_KEY);
            }
        }
    }

    /**
     * Remove extra hearts from humans.
     */
    private void removeBonusFromHumans() {
        this.currentHumanBonus = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (this.vampireManager.isHuman(player)) {
                this.removeHealthModifier(player, HUMAN_MAJORITY_HEALTH_KEY);
                this.applyDeathPenalty(player);
            }
        }
    }

    /**
     * Remove extra hearts from vampires.
     */
    private void removeBonusFromVampires() {
        this.currentVampireBonus = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (this.vampireManager.isVampire(player)) {
                this.removeHealthModifier(player, VAMPIRE_MAJORITY_HEALTH_KEY);
            }
        }
    }

    /**
     * Remove extra hearts from both teams.
     */
    private void removeAllBonuses() {
        this.currentVampireBonus = 0;
        this.currentHumanBonus = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            this.removeHealthModifier(player, HUMAN_MAJORITY_HEALTH_KEY);
            this.removeHealthModifier(player, VAMPIRE_MAJORITY_HEALTH_KEY);

            if (this.vampireManager.isHuman(player)) {
                this.applyDeathPenalty(player);
            }
        }
    }

    /**
     * Apply extra maximum hearts to the player.
     *
     * @param player the player getting extra hearts.
     * @param healthBonus the health points to add.
     * @param modifierKey the {@code NamespacedKey} key of the team's health modifier.
     */
    private void applyHealthModifier(Player player, double healthBonus, NamespacedKey modifierKey) {
        AttributeInstance healthAttribute = player.getAttribute(Attribute.MAX_HEALTH);

        if (healthAttribute != null) {
            this.removeHealthModifier(player, modifierKey);
            AttributeModifier healthModifier = new AttributeModifier(modifierKey, healthBonus, Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);

            healthAttribute.addModifier(healthModifier);
            this.healthModifiers.put(player.getUniqueId(), healthModifier);

            final double currentHealth = player.getHealth();
            final double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();

            if (currentHealth > 0 && currentHealth >= maxHealth - healthBonus) {
                player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
            }

            this.plugin.getLogger().fine("Applied +" + healthBonus / 2 + " hearts bonus to " + player.getName());
        }
    }

    /**
     * Remove the extra hearts from the player.
     *
     * @param player the player losing the extra hearts.
     * @param modifierKey the {@code NamespacedKey} key of the team's health modifier.
     */
    private void removeHealthModifier(Player player, NamespacedKey modifierKey) {
        AttributeInstance healthAttribute = player.getAttribute(Attribute.MAX_HEALTH);

        if (healthAttribute != null) {
            AttributeModifier toRemove = healthAttribute.getModifiers().stream()
                    .filter(modifier -> modifier.getKey().equals(modifierKey))
                    .findFirst().orElse(null);

            if (toRemove != null) {
                healthAttribute.removeModifier(toRemove);
                this.healthModifiers.remove(player.getUniqueId());

                if (player.getHealth() > player.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                    player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
                }

                this.plugin.getLogger().fine("Removed health modifier from " + player.getName());
            }
        }
    }

    /**
     * Apply the health bonus to players.
     *
     * @param player the player gaining bonus hearts.
     */
    public void applyBonusesToPlayer(Player player) {
        if (this.plugin.getSessionManager().isSessionActive()) {
            if (this.vampireManager.isVampire(player) && this.currentVampireBonus > 0) {
                this.applyHealthModifier(player, this.currentVampireBonus * 2.0, VAMPIRE_MAJORITY_HEALTH_KEY);

            } else if (this.vampireManager.isHuman(player)) {
                if (this.currentHumanBonus > 0) {
                    this.applyHealthModifier(player, this.currentHumanBonus * 2.0, HUMAN_MAJORITY_HEALTH_KEY);
                }

                this.applyDeathPenalty(player);
            }
        }
    }

    /**
     * Remove the health bonus from players.
     *
     * @param player the player losing their bonus hearts.
     */
    public void removeBonusesFromPlayer(Player player) {
        this.removeHealthModifier(player, VAMPIRE_MAJORITY_HEALTH_KEY);
        this.removeHealthModifier(player, HUMAN_MAJORITY_HEALTH_KEY);
        this.removeHealthModifier(player, DEATH_PENALTY_HEALTH_KEY);
    }

    /**
     * Reduce the player's maximum hearts based on their death penalty.
     *
     * @param player the player who is losing hearts.
     */
    private void applyDeathPenalty(Player player) {
        if (this.vampireManager.isHuman(player)) {
            int deathCount = this.getPlayerDeathCount(player);

            if (deathCount > 0) {
                this.applyHealthModifier(player, -1 * deathCount * 2.0, DEATH_PENALTY_HEALTH_KEY);
                this.plugin.getLogger().fine("Applied -" + deathCount + " hearts death penalty to " + player.getName());

            } else {
                this.removeHealthModifier(player, DEATH_PENALTY_HEALTH_KEY);
            }
        }
    }

    /**
     * Retrieve the death count for the player.
     *
     * @param player the player that is being evaluated.
     * @return The number of uncured deaths the player has experienced.
     */
    private int getPlayerDeathCount(Player player) {
        try {
            Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Objective deathObjective = mainScoreboard.getObjective("vsmp_death");

            if (deathObjective != null) {
                return deathObjective.getScore(player.getName()).getScore();
            }
        } catch (Exception e) {
            this.plugin.getLogger().warning("Failed to get death count for " + player.getName() + ": " + e.getMessage());
        }

        return 0;
    }

    /**
     * Retrieve the status description of the beacon alignments and health bonuses.
     *
     * @return A description of the beacon majority status.
     */
    public String getBonusStatus() {
        int holyBeacons = this.beaconManager.getHolyBeacons().size();
        int evilBeacons = this.beaconManager.getAllEvilBeacons().size();
        int neutralBeacons = this.beaconManager.getNeutralBeacons().size();

        String status = "§6=== Beacon Majority Status ===\n";
        status = status + "§f  Holy Beacons: §a" + holyBeacons + "\n";
        status = status + "§f  Evil Beacons: §c" + evilBeacons + "\n";
        status = status + "§f  Neutral Beacons: §7" + neutralBeacons + "\n";

        if (this.currentHumanBonus > 0) {
            status = status + "§a  Human Bonus: +" + this.currentHumanBonus + " hearts\n";
        } else if (this.currentVampireBonus > 0) {
            status = status + "§c  Vampire Bonus: +" + this.currentVampireBonus + " hearts\n";
        } else {
            status = status + "§7  No bonuses active\n";
        }

        return status;
    }

    /**
     * Remove the health modifiers before shutting down the manager.
     */
    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.removeBonusesFromPlayer(player);
        }

        this.healthModifiers.clear();
        this.currentVampireBonus = 0;
        this.currentHumanBonus = 0;

        this.plugin.logInfo("BeaconMajorityManager shutdown - all health bonuses removed");
    }
}
