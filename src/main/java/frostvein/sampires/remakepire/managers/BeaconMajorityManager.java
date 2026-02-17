package frostvein.sampires.remakepire.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class BeaconMajorityManager {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final BeaconManager beaconManager;
    private final Map<UUID, AttributeModifier> healthModifiers = new HashMap();
    private static final UUID VAMPIRE_MAJORITY_HEALTH_UUID = UUID.fromString("a1b2c3d4-5e6f-7890-1234-567890abcdef");
    private static final UUID HUMAN_MAJORITY_HEALTH_UUID = UUID.fromString("f1e2d3c4-b5a6-9870-4321-fedcba098765");
    private static final UUID DEATH_PENALTY_HEALTH_UUID = UUID.fromString("d1e2a3d4-b5e6-7890-abcd-1234567890ef");
    private int currentVampireBonus = 0;
    private int currentHumanBonus = 0;

    public BeaconMajorityManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.beaconManager = plugin.getBeaconManager();
    }

    public void updateBeaconMajorityBonuses() {
        if (!this.plugin.getSessionManager().isSessionActive()) {
            this.plugin.getLogger().fine("Session not active, skipping beacon majority bonus update");

        } else {
            int holyBeacons = this.beaconManager.getHolyBeacons().size();
            int evilBeacons = this.beaconManager.getAllEvilBeacons().size();
            int difference = Math.abs(holyBeacons - evilBeacons);

            this.plugin.getLogger().info("Beacon majority check: " + holyBeacons + " holy, " + evilBeacons + " evil (desecrated + permanently desecrated), difference: " + difference);

            if (holyBeacons > evilBeacons) {
                this.applyBonusToHumans(difference);
                this.removeBonusFromVampires();
                this.plugin.getLogger().info("Humans gain beacon majority bonus: +" + difference + " hearts");

            } else if (evilBeacons > holyBeacons) {
                this.applyBonusToVampires(difference);
                this.removeBonusFromHumans();
                this.plugin.getLogger().info("Vampires gain beacon majority bonus: +" + difference + " hearts");

            } else {
                this.removeAllBonuses();
            }

        }
    }

    private void applyBonusToHumans(int bonusHearts) {
        this.currentHumanBonus = bonusHearts;
        this.currentVampireBonus = 0;
        double healthBonus = (double)bonusHearts * 2.0;

        for(Player player : Bukkit.getOnlinePlayers()) {
            if (this.vampireManager.isHuman(player)) {
                this.applyHealthModifier(player, healthBonus, HUMAN_MAJORITY_HEALTH_UUID, "Beacon Majority (Human)");
                this.applyDeathPenalty(player);
            }
        }

    }

    private void applyBonusToVampires(int bonusHearts) {
        this.currentVampireBonus = bonusHearts;
        this.currentHumanBonus = 0;
        double healthBonus = (double)bonusHearts * 2.0;

        for(Player player : Bukkit.getOnlinePlayers()) {
            if (this.vampireManager.isVampire(player)) {
                this.applyHealthModifier(player, healthBonus, VAMPIRE_MAJORITY_HEALTH_UUID, "Beacon Majority (Vampire)");
            }
        }

    }

    private void removeBonusFromHumans() {
        this.currentHumanBonus = 0;

        for(Player player : Bukkit.getOnlinePlayers()) {
            if (this.vampireManager.isHuman(player)) {
                this.removeHealthModifier(player, HUMAN_MAJORITY_HEALTH_UUID);
                this.applyDeathPenalty(player);
            }
        }
    }

    private void removeBonusFromVampires() {
        this.currentVampireBonus = 0;

        for(Player player : Bukkit.getOnlinePlayers()) {
            if (this.vampireManager.isVampire(player)) {
                this.removeHealthModifier(player, VAMPIRE_MAJORITY_HEALTH_UUID);
            }
        }
    }

    private void removeAllBonuses() {
        this.currentVampireBonus = 0;
        this.currentHumanBonus = 0;

        for(Player player : Bukkit.getOnlinePlayers()) {
            this.removeHealthModifier(player, VAMPIRE_MAJORITY_HEALTH_UUID);
            this.removeHealthModifier(player, HUMAN_MAJORITY_HEALTH_UUID);
            if (this.vampireManager.isHuman(player)) {
                this.applyDeathPenalty(player);
            }
        }
    }

    private void applyHealthModifier(Player player, double healthBonus, UUID modifierUUID, String name) {
        AttributeInstance healthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttribute != null) {
            this.removeHealthModifier(player, modifierUUID);
            AttributeModifier healthModifier = new AttributeModifier(modifierUUID, name, healthBonus, Operation.ADD_NUMBER);

            healthAttribute.addModifier(healthModifier);
            this.healthModifiers.put(player.getUniqueId(), healthModifier);
            double currentHealth = player.getHealth();
            double maxHealth = player.getMaxHealth();

            if (currentHealth > (double)0.0F && currentHealth >= maxHealth - healthBonus) {
                player.setHealth(player.getMaxHealth());
            }

            this.plugin.getLogger().fine("Applied +" + healthBonus / 2.0 + " hearts bonus to " + player.getName());
        }
    }

    private void removeHealthModifier(Player player, UUID modifierUUID) {
        AttributeInstance healthAttribute = player.getAttribute(Attribute.MAX_HEALTH);

        if (healthAttribute != null) {
            AttributeModifier toRemove = (AttributeModifier)healthAttribute.getModifiers().stream().filter((modifier) -> modifier.getUniqueId().equals(modifierUUID)).findFirst().orElse(null);

            if (toRemove != null) {
                healthAttribute.removeModifier(toRemove);
                this.healthModifiers.remove(player.getUniqueId());

                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }

                this.plugin.getLogger().fine("Removed health modifier from " + player.getName());
            }
        }
    }

    public void applyBonusesToPlayer(Player player) {
        if (this.plugin.getSessionManager().isSessionActive()) {
            if (this.vampireManager.isVampire(player) && this.currentVampireBonus > 0) {
                double healthBonus = (double)this.currentVampireBonus * (double)2.0F;
                this.applyHealthModifier(player, healthBonus, VAMPIRE_MAJORITY_HEALTH_UUID, "Beacon Majority (Vampire)");
            } else if (this.vampireManager.isHuman(player)) {
                if (this.currentHumanBonus > 0) {
                    double healthBonus = (double)this.currentHumanBonus * (double)2.0F;
                    this.applyHealthModifier(player, healthBonus, HUMAN_MAJORITY_HEALTH_UUID, "Beacon Majority (Human)");
                }

                this.applyDeathPenalty(player);
            }

        }
    }

    public void removeBonusesFromPlayer(Player player) {
        this.removeHealthModifier(player, VAMPIRE_MAJORITY_HEALTH_UUID);
        this.removeHealthModifier(player, HUMAN_MAJORITY_HEALTH_UUID);
        this.removeHealthModifier(player, DEATH_PENALTY_HEALTH_UUID);
    }

    private void applyDeathPenalty(Player player) {
        if (this.vampireManager.isHuman(player)) {
            int deathCount = this.getPlayerDeathCount(player);
            if (deathCount > 0) {
                double healthPenalty = -((double)deathCount * 2.0);
                this.applyHealthModifier(player, healthPenalty, DEATH_PENALTY_HEALTH_UUID, "Death Penalty");
                this.plugin.getLogger().fine("Applied -" + deathCount + " hearts death penalty to " + player.getName());
            } else {
                this.removeHealthModifier(player, DEATH_PENALTY_HEALTH_UUID);
            }

        }
    }

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

    public void shutdown() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            this.removeBonusesFromPlayer(player);
        }

        this.healthModifiers.clear();
        this.currentVampireBonus = 0;
        this.currentHumanBonus = 0;
        this.plugin.getLogger().info("BeaconMajorityManager shutdown - all health bonuses removed");
    }
}
