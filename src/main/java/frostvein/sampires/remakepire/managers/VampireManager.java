package frostvein.sampires.remakepire.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.abilities.tome.TomeAbility;
import frostvein.sampires.remakepire.listeners.DeathHandler;

public class VampireManager {
    private final RemakepirePlugin plugin;
    private BukkitTask levelValidationTask;
    private final Map<UUID, Long> levelChangeInProgress = new HashMap<>(), lastLevelChange = new HashMap<>(), lungeTimestamps = new HashMap<>();
    private final Map<UUID, Double> lungingPlayers = new HashMap<>();
    private final Map<UUID, Integer> stageCaps = new HashMap<>();
    private static final long LEVEL_CHANGE_COOLDOWN = 5000L, LEVEL_CHANGE_TIMEOUT = 10000L, PROTECTION_DURATION = 10000L;
    public static final String HUMAN_TAG = "human";
    public static final String VAMPIRE_STAGE1_TAG = "vampire_stage1", VAMPIRE_STAGE2_TAG = "vampire_stage2", VAMPIRE_STAGE3_TAG = "vampire_stage3";
    public static final String PROMOTION_BAN_TAG = "promotion_ban";
    private final NamespacedKey SUN_WEAKNESS_SPEED_KEY, VAMPIRE_SAFE_FALL_KEY;

    /**
     * Create an instance of the Vampire manager.
     *
     * @param plugin the host plugin object.
     */
    public VampireManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.startLevelValidationTask();

        this.SUN_WEAKNESS_SPEED_KEY = new NamespacedKey(plugin, "sun_weakness_speed");
        this.VAMPIRE_SAFE_FALL_KEY = new NamespacedKey(plugin, "vampire_safe_fall");
    }

    /**
     * Protect lunging vampires from fall damage up to a certain distance from their starting height.
     *
     * @param player the vampire who lunged.
     */
    public void addFallProtection(Player player) {
        this.lungingPlayers.put(player.getUniqueId(), player.getLocation().getY() - 5.0);
        this.lungeTimestamps.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Determine if a lunging vampire should receive fall damage when they hit the ground.
     *
     * @param player the vampire who lunged.
     * @return {@code true} if the player hits the ground high enough and soon enough.
     */
    public boolean shouldPreventFallDamage(Player player) {
        UUID playerId = player.getUniqueId();

        if (!this.lungingPlayers.containsKey(playerId)) {
            return false;

        } else {
            Long lungeTime = this.lungeTimestamps.get(playerId);

            if (lungeTime != null && System.currentTimeMillis() - lungeTime <= PROTECTION_DURATION) {
                Double startingY = this.lungingPlayers.get(playerId);

                if (startingY != null && player.getLocation().getY() >= startingY) {
                    this.lungingPlayers.remove(playerId);
                    this.lungeTimestamps.remove(playerId);
                    return true;

                } else {
                    return false;
                }
            } else {
                this.lungingPlayers.remove(playerId);
                this.lungeTimestamps.remove(playerId);
                return false;
            }
        }
    }

    /**
     * Remove the player from the list of lunging vampires.
     *
     * @param player the player whose protections are being removed.
     */
    public void removeProtection(Player player) {
        UUID playerId = player.getUniqueId();
        this.lungingPlayers.remove(playerId);
        this.lungeTimestamps.remove(playerId);
    }

    /**
     * Turn the player into a fresh human.
     *
     * @param player the player becoming a human.
     */
    public void setPlayerAsHuman(Player player) {
        this.removeAllVampireTags(player);

        player.removeScoreboardTag("vampire");
        player.addScoreboardTag(HUMAN_TAG);
        this.addPlayerToCorrectTeam(player);
        player.setInvulnerable(false);

        // Remove the effects of the force cure lock if the player was healed through this method
        if (this.plugin.getForcedCureChoiceManager() != null && this.plugin.getForcedCureChoiceManager().hasPendingCure(player)) {
            this.plugin.getForcedCureChoiceManager().removePendingCure(player);
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        }

        if (this.plugin.getBatTransformationManager() != null && this.plugin.getBatTransformationManager().isInBatForm(player)) {
            this.plugin.getBatTransformationManager().transformToHuman(player);
        }

        this.removeVampireAttributeModifiers(player);
        AttributeInstance healthAttribute = player.getAttribute(Attribute.MAX_HEALTH);

        if (healthAttribute != null) {
            healthAttribute.setBaseValue(20.0);
        }

        player.removeScoreboardTag("ImmuneToThirst");

        if (this.plugin.getHolyWaterEffectManager() != null) {
            this.plugin.getHolyWaterEffectManager().removeHolyWaterEffect(player, false);
        }

        if (this.plugin.getVampireAbilityManager() != null) {
            this.plugin.getVampireAbilityManager().clearAllCooldowns(player);
        }

        this.removeProtection(player);
        this.clearStageCap(player);

        if (this.plugin.getBloodMoonAttributeListener() != null) {
            this.plugin.getBloodMoonAttributeListener().forceRemoveBloodMoonAttributes(player);
        }

        this.plugin.getBeaconMajorityManager().removeBonusesFromPlayer(player);
        this.plugin.getBeaconMajorityManager().applyBonusesToPlayer(player);

        player.setLevel(0);
        player.setExp(0.0F);

        if (this.plugin.getVampireTexturePackManager() != null) {
            this.plugin.getVampireTexturePackManager().onPlayerBecomeHuman(player);
        }
    }

    /**
     * Remove the vampire attributes of sun weakness and fall damage resistance from the player.
     *
     * @param player the player losing their passive vampire effects.
     */
    private void removeVampireAttributeModifiers(Player player) {
        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.getModifiers().stream().filter(modifier -> SUN_WEAKNESS_SPEED_KEY.equals(modifier.getKey()))
                    .forEach(speedAttr::removeModifier);
        }

        AttributeInstance safeFallAttr = player.getAttribute(Attribute.SAFE_FALL_DISTANCE);
        if (safeFallAttr != null) {
            safeFallAttr.getModifiers().stream().filter(modifier -> VAMPIRE_SAFE_FALL_KEY.equals(modifier.getKey()))
                    .forEach(safeFallAttr::removeModifier);
        }
    }

    /**
     * Assign the player to a team depending on their silver weakness.
     *
     * @param player the player being assigned.
     */
    private void addPlayerToCorrectTeam(Player player) {
        try {
            Team teamToJoin;
            if (this.plugin.getVampireManager().isIronAffected(player)) {
                teamToJoin = this.plugin.getVampireCastTeam();
            } else {
                teamToJoin = this.plugin.getCastTeam();
            }

            if (teamToJoin != null) {
                if (!teamToJoin.hasPlayer(player)) {
                    teamToJoin.addPlayer(player);
                }
            } else {
                this.plugin.getLogger().warning("CastTeam is null - cannot add player " + player.getName());
            }
        } catch (Exception e) {
            this.plugin.getLogger().severe("Failed to add player " + player.getName() + " to CastTeam: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Turn the player into a vampire of the provided stage.
     *
     * @param player the player being turned.
     * @param stage the new vampire stage of the player.
     */
    public void setPlayerAsVampire(Player player, int stage) {
        this.setPlayerAsVampire(player, stage, false);
    }

    /**
     * Turn the player into a vampire of the provided stage.
     *
     * @param player the player being turned.
     * @param stage the new vampire stage of the player.
     * @param adminOverride {@code true} if an admin caused this change.
     */
    public void setPlayerAsVampire(Player player, int stage, boolean adminOverride) {
        UUID playerId = player.getUniqueId();

        if (!adminOverride && this.hasPromotionBan(player) && stage > 1) {
            stage = 1;
        }

        this.startLevelChange(playerId);
        boolean isInBatForm = this.plugin.getBatTransformationManager() != null && this.plugin.getBatTransformationManager().isInBatForm(player);

        try {
            this.removeAllVampireTags(player);
            player.addScoreboardTag("vampire");
            if (this.plugin.getTomeManager() != null) {
                this.plugin.getTomeManager().removeAllAbilities(player);
            }

            TomeAbility.clearAllCooldowns(player);
            switch (stage) {
                case 1:
                    player.addScoreboardTag(VAMPIRE_STAGE1_TAG);
                    player.setLevel(1);
                    break;
                case 2:
                    player.addScoreboardTag(VAMPIRE_STAGE2_TAG);
                    player.setLevel(2);
                    break;
                case 3:
                    player.addScoreboardTag(VAMPIRE_STAGE3_TAG);
                    player.setLevel(3);
                    break;
                default:
                    player.addScoreboardTag(VAMPIRE_STAGE1_TAG);
                    player.setLevel(1);
            }

            long baseDelay = isInBatForm ? 5L : 2L;
            final int CURRENT_STAGE = stage;   // Copy the current stage for use within the lambda

            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                if (player.isOnline()) {
                    this.addPlayerToCorrectTeam(player);

                    Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                        if (player.isOnline()) {
                            this.plugin.getBeaconMajorityManager().applyBonusesToPlayer(player);

                            if (CURRENT_STAGE >= 2) {
                                long tomeDelay = isInBatForm ? 3L : 1L;
                                Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                                    if (player.isOnline()) {
                                        this.plugin.getTomeVampireRestrictionListener().forceDropTomesForPlayer(player);
                                    }

                                    this.completeLevelChange(playerId);
                                }, tomeDelay);
                            } else {
                                this.completeLevelChange(playerId);
                            }
                        } else {
                            this.completeLevelChange(playerId);
                        }
                    }, 2L);
                } else {
                    this.completeLevelChange(playerId);
                }

            }, baseDelay);
        } catch (Exception e) {
            this.completeLevelChange(playerId);
            this.plugin.getLogger().severe("Error in setPlayerAsVampire for " + player.getName() + ": " + e.getMessage());
            throw e;
        }

        if (this.plugin.getVampireTexturePackManager() != null) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                if (player.isOnline() && this.isVampire(player)) {
                    this.plugin.getVampireTexturePackManager().onVampireTransformation(player);
                }

            }, 40L);
        }

        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (player.isOnline()) {
                this.ensureVampireTagConsistency(player);
            }

        }, 60L);
    }

    /**
     * Turn a player into a new vampire and log the sire relationship.
     *
     * @param target the player turning into a vampire.
     * @param turner the player who turned the target.
     */
    public void performVampireTurning(Player target, Player turner) {
        this.setPlayerAsVampire(target, 1);

        if (!target.getScoreboardTags().contains("vampire")) {
            target.addScoreboardTag("vampire");
            this.plugin.getLogger().warning("Had to manually add 'vampire' tag during turning for " + target.getName());
        }

        if (turner != null && this.plugin.getSireManager() != null) {
            this.plugin.getSireManager().setSire(target.getName(), turner.getName());
            this.plugin.logInfo("Sire mapping recorded: " + target.getName() + " -> " + turner.getName());
        }

        target.setExp(0.5F);
        target.addScoreboardTag("ImmuneToThirst");
        target.setRespawnLocation(this.plugin.getVampireRespawnLocation());
        this.applyTurningEffects(target);
        target.sendTitle("§4§lTURNED", "", 10, 60, 20);

        if (turner != null) {
            turner.sendTitle("§4§lNEW BLOOD", "", 10, 60, 20);
            turner.playSound(turner.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.5F, 1.2F);
        }

        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (target.isOnline() && this.isVampire(target)) {
                target.removeScoreboardTag("ImmuneToThirst");
                target.sendMessage("§4§lThe Thirst Awakens");
                target.sendMessage("§cYour first feeling as a vampire, is the need to feed... Your thirst will now start depleting over time.");
                target.playSound(target, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, SoundCategory.MASTER, 1.0F, 1.2F);
            }

        }, 42000L);

        this.sendTurningMessages(target, turner);
        target.playSound(target, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, SoundCategory.MASTER, 0.5F, 1.5F);

        if (this.plugin.getVampireTurningManager() != null) {
            this.plugin.getVampireTurningManager().disableAllVampireTurning();
        }

        Bukkit.getScheduler().runTaskLater(this.plugin, () -> DeathHandler.checkAndAnnounceTeamElimination(this.plugin, true, false), 10L);
    }

    /**
     * Inform the target that they have been turned into a new vampire.
     *
     * @param target the player turning into a vampire.
     * @param turner the player who turned the target.
     */
    private void sendTurningMessages(Player target, Player turner) {
        target.sendMessage("§4THE TURNING");
        target.sendMessage("");

        if (turner != null) {
            target.sendMessage("§cThe bite of " + turner.getName() + " courses through your veins...");
        } else {
            target.sendMessage("§cDark forces flow through your veins...");
        }

        target.sendMessage("§cYour heart slows... then stops...");
        target.sendMessage("§cDarkness consumes your vision...");
        target.sendMessage("");
        target.sendMessage("§cYou awaken... different. Changed. Cursed.");
        target.sendMessage("§cYou are now a Stage 1 vampire.");
        target.sendMessage("");

        TextComponent prefixText = new TextComponent("§7When you are ready to accept your new self, ");
        TextComponent clickableText = new TextComponent("§e§n[CLICK HERE]");
        clickableText.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/pow texture"));
        clickableText.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder("§7Click to apply the Creature Of The Night texture pack")).create()));
        TextComponent suffixText = new TextComponent("§7 to have the Creature Of The Night texture pack applied.");
        TextComponent fullMessage = new TextComponent("");

        fullMessage.addExtra(prefixText);
        fullMessage.addExtra(clickableText);
        fullMessage.addExtra(suffixText);

        target.spigot().sendMessage(fullMessage);
        target.sendMessage("");
    }

    /**
     * Reduce the vampire's stage by one level.
     *
     * @param player the vampire dropping a stage.
     */
    public void reduceVampireStage(Player player) {
        if (player.getScoreboardTags().contains(VAMPIRE_STAGE3_TAG)) {
            this.setPlayerAsVampire(player, 2);
            player.sendMessage("§6Your vampire power has diminished. You are now Stage 2.");
            this.plugin.getVampireAbilityManager().clearAllCooldowns(player);
            player.sendMessage("§dThough your essence grows weaker, your abilities cooldowns are renewed once more");

            if (this.plugin.getHolyWaterEffectManager() != null && this.plugin.getHolyWaterEffectManager().isAbilitiesDisabled(player)) {
                this.plugin.getHolyWaterEffectManager().removeHolyWaterEffect(player, true);
                player.sendMessage("§aThe holy water's grip on you has been shattered by your demotion.");
            }

            this.applyDemotionEffectsToNearbyHumans(player);

        } else if (player.getScoreboardTags().contains(VAMPIRE_STAGE2_TAG)) {
            this.setPlayerAsVampire(player, 1);
            player.sendMessage("§6Your vampire power has diminished. You are now Stage 1.");
            this.plugin.getVampireAbilityManager().clearAllCooldowns(player);
            player.sendMessage("§dThough your essence grows weaker, your abilities cooldowns are renewed once more");

            if (this.plugin.getHolyWaterEffectManager() != null && this.plugin.getHolyWaterEffectManager().isAbilitiesDisabled(player)) {
                this.plugin.getHolyWaterEffectManager().removeHolyWaterEffect(player, true);
                player.sendMessage("§aThe holy water's grip on you has been shattered by your demotion.");
            }

            this.applyDemotionEffectsToNearbyHumans(player);
        }
    }

    /**
     * Throw human players away from the vampire who has dropped a stage.
     *
     * @param vampire the vampire who has been demoted.
     */
    private void applyDemotionEffectsToNearbyHumans(Player vampire) {
        Location vampireLocation = vampire.getLocation();

        for(Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
            if (this.isHuman(nearbyPlayer) && nearbyPlayer.getWorld().equals(vampire.getWorld())) {
                if (nearbyPlayer.getLocation().distance(vampireLocation) <= 10) {
                    nearbyPlayer.sendMessage("§8You feel a darkness lunge out at you, a vampire near you has lost a piece of their essence and grown weaker...");
                    nearbyPlayer.playSound(nearbyPlayer.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, SoundCategory.MASTER, 1.0F, 0.8F);
                    Vector direction = nearbyPlayer.getLocation().toVector().subtract(vampireLocation.toVector()).normalize();
                    nearbyPlayer.setVelocity(direction.multiply(2.4));
                }
            }
        }
    }

    /**
     * Permakill a vampire and inform them of their final death.
     *
     * @param player the vampire who has been permakilled.
     */
    public void killVampirePermanently(Player player) {
        this.removeAllVampireTags(player);
        player.addScoreboardTag(HUMAN_TAG);

        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            player.setGameMode(GameMode.SPECTATOR);

            player.sendTitle("§4§lFINAL DEATH", "§7Your journey has ended", 10, 100, 30);
            player.sendMessage("");
            player.sendMessage("§4§lPERMANENTLY KILLED");
            player.sendMessage("");
            player.sendMessage("§7Your soul has been released from this mortal realm.");
            player.sendMessage("§7You are now in spectator mode.");
            player.sendMessage("");
            player.sendMessage("§8Watch over the remaining survivors...");
            player.sendMessage("");

            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, SoundCategory.MASTER, 0.5F, 0.5F);
            player.playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, SoundCategory.MASTER, 1.0F, 0.5F);
        }, 5L);
    }

    /**
     * Retrieve if the player is a human.
     *
     * @param player the player being checked.
     * @return {@code true} if the player is a human.
     */
    public boolean isHuman(Player player) {
        return player.getScoreboardTags().contains(HUMAN_TAG);
    }

    /**
     * Retrieve if the player is a vampire.
     *
     * @param player the player being checked.
     * @return {@code true} if the player is a vampire.
     */
    public boolean isVampire(Player player) {
        return this.isVampireStage1(player) || this.isVampireStage2(player) || this.isVampireStage3(player);
    }

    /**
     * Retrieve if the player should be affected by silver.
     *
     * @param player the player being checked.
     * @return {@code true} if the player is affected by silver.
     */
    public boolean isIronAffected(Player player) {
        return this.isVampireStage2(player) || this.isVampireStage3(player);
    }

    /**
     * Retrieve if the player is a higher vampire.
     *
     * @param player the player being checked.
     * @return {@code true} if the player is a stage 2 or 3 vampire.
     */
    public boolean isVampireStage2OrHigher(Player player) {
        return this.isVampireStage2(player) || this.isVampireStage3(player);
    }

    /**
     * Retrieve if the player is a stage 1 vampire.
     *
     * @param player the player being checked.
     * @return {@code true} if the player is a stage 1 vampire.
     */
    public boolean isVampireStage1(Player player) {
        return player.getScoreboardTags().contains(VAMPIRE_STAGE1_TAG);
    }

    /**
     * Retrieve if the player is a stage 2 vampire.
     *
     * @param player the player being checked.
     * @return {@code true} if the player is a stage 2 vampire.
     */
    public boolean isVampireStage2(Player player) {
        return player.getScoreboardTags().contains(VAMPIRE_STAGE2_TAG);
    }

    /**
     * Retrieve if the player is a stage 3 vampire.
     *
     * @param player the player being checked.
     * @return {@code true} if the player is a stage 3 vampire.
     */
    public boolean isVampireStage3(Player player) {
        return player.getScoreboardTags().contains(VAMPIRE_STAGE3_TAG);
    }

    /**
     * Retrieve the vampire stage of the player.
     *
     * @param player the player being checked.
     * @return The player's vampire stage number.
     */
    public int getVampireStage(Player player) {
        if (this.isVampireStage1(player)) {
            return 1;
        } else if (this.isVampireStage2(player)) {
            return 2;
        } else if (this.isVampireStage3(player)) {
            return 3;
        } else {
            return 0;
        }
    }

    /**
     * Retrieve if the player is currently under a promotion ban.
     *
     * @param player the player being checked.
     * @return {@code true} if the player cannot increase their vampire stage through blood consumption.
     */
    public boolean hasPromotionBan(Player player) {
        return player.getScoreboardTags().contains(PROMOTION_BAN_TAG);
    }

    /**
     * Prevent the player from increasing their vampire stage until the next session.
     *
     * @param player the player receiving the promotion ban tag.
     */
    public void applyPromotionBan(Player player) {
        if (this.isVampire(player)) {
            this.setPlayerAsVampire(player, 1);
            player.addScoreboardTag(PROMOTION_BAN_TAG);

            player.sendMessage("§4§lDEATH PENALTY");
            player.sendMessage("§c§lYour death has cursed you with weakness.");
            player.sendMessage("§c§lYou cannot grow stronger until the next session begins...");
        }
    }

    /**
     * Remove the vampire stage promotion ban from the player.
     *
     * @param player the player with the ban.
     */
    public void clearPromotionBan(Player player) {
        player.removeScoreboardTag(PROMOTION_BAN_TAG);
    }

    /**
     * Remove the vampire stage promotion bans from all online players.
     */
    public void clearAllPromotionBans() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            this.clearPromotionBan(player);
        }
    }

    /**
     * Retrieve if the player is currently locked out of a stage.
     *
     * @param player the player being checked.
     * @return {@code true} if the player has an active stage cap.
     */
    public boolean hasStageCap(Player player) {
        return this.stageCaps.containsKey(player.getUniqueId());
    }

    /**
     * Retrieve the highest vampire stage that the player is allowed to access.
     *
     * @param player the player being checked.
     * @return The player's maximum allowed stage.
     */
    public int getStageCap(Player player) {
        return this.stageCaps.getOrDefault(player.getUniqueId(), 3);
    }

    /**
     * Add a cap on the player's vampire stage.
     *
     * @param player the player who is being capped.
     * @param maxStage the stage that the player is capped at.
     */
    public void setStageCap(Player player, int maxStage) {
        if (maxStage >= 1 && maxStage <= 3) {
            this.stageCaps.put(player.getUniqueId(), maxStage);
            this.plugin.logInfo("Stage cap set for " + player.getName() + ": max stage " + maxStage);
        }
    }

    /**
     * Remove the vampire stage cap from the player.
     *
     * @param player the player with a stage cap.
     */
    public void clearStageCap(Player player) {
        this.stageCaps.remove(player.getUniqueId());
        this.plugin.logInfo("Stage cap cleared for " + player.getName());
    }

    /**
     * Remove the vampire stage cap from all online players.
     */
    public void clearAllStageCaps() {
        this.stageCaps.clear();
        this.plugin.logInfo("All stage caps cleared");
    }

    /**
     * Apply and remove the vampire tag based on the player's state.
     *
     * @param player the player being checked.
     */
    public void ensureVampireTagConsistency(Player player) {
        if (this.isVampire(player) && !player.getScoreboardTags().contains("vampire")) {
            player.addScoreboardTag("vampire");
        } else if (!this.isVampire(player) && player.getScoreboardTags().contains("vampire")) {
            player.removeScoreboardTag("vampire");
        }
    }

    /**
     * Retrieve the damage multiplier of wooden weapons against vampires.
     *
     * @param player the player taking damage from the wooden weapon.
     * @return The wooden weapon's damage multiplier.
     */
    public double getWoodenWeaponMultiplier(Player player) {
        if (this.isVampireStage1(player)) {
            return 2.5;
        } else if (this.isVampireStage2(player)) {
            return 3.0;
        } else if (this.isVampireStage3(player)) {
            return 4.0;
        } else {
            return 1.0;
        }
    }

    /**
     * Apply the proper tags to new players.
     *
     * @param player the player being set up.
     */
    public void initializeNewPlayer(Player player) {
        if (!this.isHuman(player) && !this.isVampire(player)) {
            this.setPlayerAsHuman(player);
        }

        if (this.isVampire(player) && !player.getScoreboardTags().contains("vampire")) {
            player.addScoreboardTag("vampire");
        }
    }

    /**
     * Remove all vampire stage indication tags from the player.
     *
     * @param player the player losing their stage tag.
     */
    private void removeAllVampireTags(Player player) {
        player.removeScoreboardTag(HUMAN_TAG);
        player.removeScoreboardTag(VAMPIRE_STAGE1_TAG);
        player.removeScoreboardTag(VAMPIRE_STAGE2_TAG);
        player.removeScoreboardTag(VAMPIRE_STAGE3_TAG);
    }

    /**
     * Apply the potion effects to newly turned vampires.
     *
     * @param player the player being turned.
     */
    private void applyTurningEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 0, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 400, 3, false, false, true));

        PotionEffect nightVision = new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 0, false, false, false);
        player.addPotionEffect(nightVision);
    }

    /**
     * Begin validating that players are set to the correct vampire stage.
     */
    private void startLevelValidationTask() {
        this.levelValidationTask = (new BukkitRunnable() {
            public void run() {
                VampireManager.this.validateVampireLevels();
            }
        }).runTaskTimer(this.plugin, 2400L, 2400L);

        this.plugin.logInfo("VampireManager: Started level validation task (every 2 minutes)");
    }

    /**
     * Ensure that all players are set to the correct vampire stage.
     */
    private void validateVampireLevels() {
        int corrections = 0, skipped = 0;
        final int maxCorrectionsPerRun = 3;

        for(Player player : Bukkit.getOnlinePlayers()) {
            if (corrections >= maxCorrectionsPerRun) {
                break;
            }

            UUID playerId = player.getUniqueId();

            if (!this.isLevelChangeInProgress(playerId) && !this.hadRecentLevelChange(playerId)) {
                if (this.isVampire(player)) {
                    int expectedLevel = this.getVampireStage(player), currentLevel = player.getLevel();

                    if (currentLevel != expectedLevel) {
                        player.setLevel(expectedLevel);
                        this.lastLevelChange.put(playerId, System.currentTimeMillis());
                        ++corrections;
                    }
                }
            } else {
                ++skipped;
            }
        }
    }

    /**
     * Ensure that all players are set to the correct vampire stage.
     */
    public void validateLevelsNow() {
        this.validateVampireLevels();
    }

    /**
     * Stop validating that players are set to the correct vampire stage.
     */
    public void stopLevelValidationTask() {
        if (this.levelValidationTask != null) {
            this.levelValidationTask.cancel();
            this.levelValidationTask = null;
            this.plugin.logInfo("VampireManager: Stopped level validation task");
        }
    }

    /**
     * Record when the player changed their vampire stage.
     *
     * @param playerId the UUID of the player changing their stage.
     */
    private void startLevelChange(UUID playerId) {
        long currentTime = System.currentTimeMillis();
        this.levelChangeInProgress.put(playerId, currentTime);
        this.lastLevelChange.put(playerId, currentTime);
    }

    /**
     * Mark the player's stage change as completed.
     *
     * @param playerId the UUID of the player being updated.
     */
    private void completeLevelChange(UUID playerId) {
        this.levelChangeInProgress.remove(playerId);
    }

    /**
     * Validate that the player's vampire stage successfully changed within a proper time.
     *
     * @param playerId the UUID of the player being validated.
     * @return {@code true} if the expected time has not yet passed.
     */
    private boolean isLevelChangeInProgress(UUID playerId) {
        Long startTime = this.levelChangeInProgress.get(playerId);

        if (startTime == null) {
            return false;
        } else if (System.currentTimeMillis() - startTime > LEVEL_CHANGE_TIMEOUT) {
            this.plugin.getLogger().warning("Level change timed out for player " + String.valueOf(playerId) + ", removing lock");
            this.levelChangeInProgress.remove(playerId);
            return false;

        } else {
            return true;
        }
    }

    /**
     * Check if the player changed their vampire stage recently.
     *
     * @param playerId the UUID of the vampire being checked.
     * @return {@code true} if the player's vampire stage was changed recently enough.
     */
    private boolean hadRecentLevelChange(UUID playerId) {
        Long lastChange = this.lastLevelChange.get(playerId);

        if (lastChange == null) {
            return false;
        } else {
            return System.currentTimeMillis() - lastChange < LEVEL_CHANGE_COOLDOWN;
        }
    }

    /**
     * Clear all the vampire state trackers before shutting down the manager.
     */
    public void shutdown() {
        this.stopLevelValidationTask();
        this.levelChangeInProgress.clear();
        this.lastLevelChange.clear();
        this.lungingPlayers.clear();
        this.lungeTimestamps.clear();
        this.stageCaps.clear();
        this.plugin.logInfo("VampireManager shutdown complete");
    }
}
