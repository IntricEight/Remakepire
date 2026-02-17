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
    private final Map<UUID, Long> levelChangeInProgress = new HashMap();
    private final Map<UUID, Long> lastLevelChange = new HashMap();
    private static final long LEVEL_CHANGE_COOLDOWN = 5000L;
    private static final long LEVEL_CHANGE_TIMEOUT = 10000L;
    private final Map<UUID, Integer> stageCaps = new HashMap();
    public static final String HUMAN_TAG = "human";
    public static final String VAMPIRE_STAGE1_TAG = "vampire_stage1";
    public static final String VAMPIRE_STAGE2_TAG = "vampire_stage2";
    public static final String VAMPIRE_STAGE3_TAG = "vampire_stage3";
    public static final String PROMOTION_BAN_TAG = "promotion_ban";
    private final Map<UUID, Double> lungingPlayers = new HashMap();
    private final Map<UUID, Long> lungeTimestamps = new HashMap();
    private static final long PROTECTION_DURATION = 10000L;
    private final NamespacedKey SUN_WEAKNESS_SPEED_KEY;
    private final NamespacedKey VAMPIRE_SAFE_FALL_KEY;

    public VampireManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.startLevelValidationTask();

        this.SUN_WEAKNESS_SPEED_KEY = new NamespacedKey(plugin, "sun_weakness_speed");
        this.VAMPIRE_SAFE_FALL_KEY = new NamespacedKey(plugin, "vampire_safe_fall");
    }

    public void addFallProtection(Player player) {
        this.lungingPlayers.put(player.getUniqueId(), player.getLocation().getY() - 5.0);
        this.lungeTimestamps.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public boolean shouldPreventFallDamage(Player player) {
        UUID playerId = player.getUniqueId();
        if (!this.lungingPlayers.containsKey(playerId)) {
            return false;

        } else {
            Long lungeTime = (Long)this.lungeTimestamps.get(playerId);

            if (lungeTime != null && System.currentTimeMillis() - lungeTime <= 10000L) {
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

    public void removeProtection(Player player) {
        UUID playerId = player.getUniqueId();
        this.lungingPlayers.remove(playerId);
        this.lungeTimestamps.remove(playerId);
    }

    public void setPlayerAsHuman(Player player) {
        this.removeAllVampireTags(player);

        player.removeScoreboardTag("vampire");
        player.addScoreboardTag("human");
        this.addPlayerToCorrectTeam(player);
        player.setInvulnerable(false);

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

    private void removeVampireAttributeModifiers(Player player) {

        // TODO: Decompilation Fixing, remove comments once success is confirmed

//        AttributeInstance safeFallAttr;
//        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
//
//        UUID sunWeaknessSpeedUUID = UUID.fromString("b8c2a3d4-5e6f-7890-a1b2-c3d4e5f67890");
//        UUID vampireSafeFallUUID = UUID.fromString("c9d1e2f3-6a7b-8901-2345-6789abcdef01");
//
//        if (speedAttr != null) {
//            speedAttr.getModifiers().stream().filter(modifier -> modifier.getUniqueId().equals(sunWeaknessSpeedUUID)).forEach(arg_0 -> ((AttributeInstance)speedAttr).removeModifier(arg_0));
//        }
//
//        if ((safeFallAttr = player.getAttribute(Attribute.SAFE_FALL_DISTANCE)) != null) {
//            safeFallAttr.getModifiers().stream().filter(modifier -> modifier.getUniqueId().equals(vampireSafeFallUUID)).forEach(arg_0 -> ((AttributeInstance)safeFallAttr).removeModifier(arg_0));
//        }

//        final NamespacedKey SUN_WEAKNESS_SPEED_KEY = new NamespacedKey(plugin, "sun_weakness_speed");
//        final NamespacedKey VAMPIRE_SAFE_FALL_KEY = new NamespacedKey(plugin, "vampire_safe_fall");

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

    public void setPlayerAsVampire(Player player, int stage) {
        this.setPlayerAsVampire(player, stage, false);
    }

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
                    player.addScoreboardTag("vampire_stage1");
                    player.setLevel(1);
                    break;
                case 2:
                    player.addScoreboardTag("vampire_stage2");
                    player.setLevel(2);
                    break;
                case 3:
                    player.addScoreboardTag("vampire_stage3");
                    player.setLevel(3);
                    break;
                default:
                    player.addScoreboardTag("vampire_stage1");
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

    public void performVampireTurning(Player target, Player turner) {
        this.setPlayerAsVampire(target, 1);
        if (!target.getScoreboardTags().contains("vampire")) {
            target.addScoreboardTag("vampire");
            this.plugin.getLogger().warning("Had to manually add 'vampire' tag during turning for " + target.getName());
        }

        if (turner != null && this.plugin.getSireManager() != null) {
            this.plugin.getSireManager().setSire(target.getName(), turner.getName());
            this.plugin.getLogger().info("Sire mapping recorded: " + target.getName() + " -> " + turner.getName());
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

    public void reduceVampireStage(Player player) {
        if (player.getScoreboardTags().contains("vampire_stage3")) {
            this.setPlayerAsVampire(player, 2);
            player.sendMessage("§6Your vampire power has diminished. You are now Stage 2.");
            this.plugin.getVampireAbilityManager().clearAllCooldowns(player);
            player.sendMessage("§dThough your essence grows weaker, your abilities cooldowns are renewed once more");
            if (this.plugin.getHolyWaterEffectManager() != null && this.plugin.getHolyWaterEffectManager().isAbilitiesDisabled(player)) {
                this.plugin.getHolyWaterEffectManager().removeHolyWaterEffect(player, true);
                player.sendMessage("§aThe holy water's grip on you has been shattered by your demotion.");
            }

            this.applyDemotionEffectsToNearbyHumans(player);
        } else if (player.getScoreboardTags().contains("vampire_stage2")) {
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

    private void applyDemotionEffectsToNearbyHumans(Player vampire) {
        Location vampireLocation = vampire.getLocation();

        for(Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
            if (this.isHuman(nearbyPlayer) && nearbyPlayer.getWorld().equals(vampire.getWorld())) {
                double distance = nearbyPlayer.getLocation().distance(vampireLocation);

                if (distance <= 10) {
                    nearbyPlayer.sendMessage("§8You feel a darkness lunge out at you, a vampire near you has lost a piece of their essence and grown weaker...");
                    nearbyPlayer.playSound(nearbyPlayer.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, SoundCategory.MASTER, 1.0F, 0.8F);
                    Vector direction = nearbyPlayer.getLocation().toVector().subtract(vampireLocation.toVector()).normalize();
                    nearbyPlayer.setVelocity(direction.multiply(2.4));
                }
            }
        }

    }

    public void killVampirePermanently(Player player) {
        this.removeAllVampireTags(player);
        player.addScoreboardTag("human");
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

    public boolean isHuman(Player player) {
        return player.getScoreboardTags().contains("human");
    }

    public boolean isVampire(Player player) {
        return this.isVampireStage1(player) || this.isVampireStage2(player) || this.isVampireStage3(player);
    }

    public boolean isIronAffected(Player player) {
        return this.isVampireStage2(player) || this.isVampireStage3(player);
    }

    public boolean isVampireStage2OrHigher(Player player) {
        return this.isVampireStage2(player) || this.isVampireStage3(player);
    }

    public boolean isVampireStage1(Player player) {
        return player.getScoreboardTags().contains("vampire_stage1");
    }

    public boolean isVampireStage2(Player player) {
        return player.getScoreboardTags().contains("vampire_stage2");
    }

    public boolean isVampireStage3(Player player) {
        return player.getScoreboardTags().contains("vampire_stage3");
    }

    public int getVampireStage(Player player) {
        if (this.isVampireStage1(player)) {
            return 1;
        } else if (this.isVampireStage2(player)) {
            return 2;
        } else {
            return this.isVampireStage3(player) ? 3 : 0;
        }
    }

    public boolean hasPromotionBan(Player player) {
        return player.getScoreboardTags().contains("promotion_ban");
    }

    public void applyPromotionBan(Player player) {
        if (this.isVampire(player)) {
            this.setPlayerAsVampire(player, 1);
            player.addScoreboardTag("promotion_ban");
            player.sendMessage("§4§lDEATH PENALTY");
            player.sendMessage("§c§lYour death has cursed you with weakness.");
            player.sendMessage("§c§lYou cannot grow stronger until the next session begins...");
        }

    }

    public void clearPromotionBan(Player player) {
        player.removeScoreboardTag("promotion_ban");
    }

    public void clearAllPromotionBans() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            this.clearPromotionBan(player);
        }

    }

    public boolean hasStageCap(Player player) {
        return this.stageCaps.containsKey(player.getUniqueId());
    }

    public int getStageCap(Player player) {
        return (Integer)this.stageCaps.getOrDefault(player.getUniqueId(), 3);
    }

    public void setStageCap(Player player, int maxStage) {
        if (maxStage >= 1 && maxStage <= 3) {
            this.stageCaps.put(player.getUniqueId(), maxStage);
            this.plugin.getLogger().info("Stage cap set for " + player.getName() + ": max stage " + maxStage);
        }

    }

    public void clearStageCap(Player player) {
        this.stageCaps.remove(player.getUniqueId());
        this.plugin.getLogger().info("Stage cap cleared for " + player.getName());
    }

    public void clearAllStageCaps() {
        this.stageCaps.clear();
        this.plugin.getLogger().info("All stage caps cleared");
    }

    public void ensureVampireTagConsistency(Player player) {
        if (this.isVampire(player) && !player.getScoreboardTags().contains("vampire")) {
            player.addScoreboardTag("vampire");
        } else if (!this.isVampire(player) && player.getScoreboardTags().contains("vampire")) {
            player.removeScoreboardTag("vampire");
        }

    }

    public double getWoodenWeaponMultiplier(Player player) {
        if (this.isVampireStage1(player)) {
            return 2.5;
        } else if (this.isVampireStage2(player)) {
            return 3.0;
        } else {
            return this.isVampireStage3(player) ? 4.0 : 1.0;
        }
    }

    public void initializeNewPlayer(Player player) {
        if (!this.isHuman(player) && !this.isVampire(player)) {
            this.setPlayerAsHuman(player);
        }

        if (this.isVampire(player) && !player.getScoreboardTags().contains("vampire")) {
            player.addScoreboardTag("vampire");
        }

    }

    private void removeAllVampireTags(Player player) {
        player.removeScoreboardTag("human");
        player.removeScoreboardTag("vampire_stage1");
        player.removeScoreboardTag("vampire_stage2");
        player.removeScoreboardTag("vampire_stage3");
    }

    private void applyTurningEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 0, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 400, 3, false, false, true));
        PotionEffect nightVision = new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 0, false, false, false);
        player.addPotionEffect(nightVision);
    }

    private void startLevelValidationTask() {
        this.levelValidationTask = (new BukkitRunnable() {
            public void run() {
                VampireManager.this.validateVampireLevels();
            }
        }).runTaskTimer(this.plugin, 2400L, 2400L);
        this.plugin.getLogger().info("VampireManager: Started level validation task (every 2 minutes)");
    }

    private void validateVampireLevels() {
        int corrections = 0;
        int skipped = 0;
        int maxCorrectionsPerRun = 3;

        for(Player player : Bukkit.getOnlinePlayers()) {
            if (corrections >= maxCorrectionsPerRun) {
                break;
            }

            UUID playerId = player.getUniqueId();
            if (!this.isLevelChangeInProgress(playerId) && !this.hadRecentLevelChange(playerId)) {
                if (this.isVampire(player)) {
                    int expectedLevel = this.getVampireStage(player);
                    int currentLevel = player.getLevel();
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

    public void validateLevelsNow() {
        this.validateVampireLevels();
    }

    public void stopLevelValidationTask() {
        if (this.levelValidationTask != null) {
            this.levelValidationTask.cancel();
            this.levelValidationTask = null;
            this.plugin.getLogger().info("VampireManager: Stopped level validation task");
        }

    }

    private void startLevelChange(UUID playerId) {
        long currentTime = System.currentTimeMillis();
        this.levelChangeInProgress.put(playerId, currentTime);
        this.lastLevelChange.put(playerId, currentTime);
    }

    private void completeLevelChange(UUID playerId) {
        this.levelChangeInProgress.remove(playerId);
    }

    private boolean isLevelChangeInProgress(UUID playerId) {
        Long startTime = (Long)this.levelChangeInProgress.get(playerId);
        if (startTime == null) {
            return false;
        } else if (System.currentTimeMillis() - startTime > 10000L) {
            this.plugin.getLogger().warning("Level change timed out for player " + String.valueOf(playerId) + ", removing lock");
            this.levelChangeInProgress.remove(playerId);
            return false;
        } else {
            return true;
        }
    }

    private boolean hadRecentLevelChange(UUID playerId) {
        Long lastChange = (Long)this.lastLevelChange.get(playerId);
        if (lastChange == null) {
            return false;
        } else {
            return System.currentTimeMillis() - lastChange < 5000L;
        }
    }

    public void shutdown() {
        this.stopLevelValidationTask();
        this.levelChangeInProgress.clear();
        this.lastLevelChange.clear();
        this.lungingPlayers.clear();
        this.lungeTimestamps.clear();
        this.stageCaps.clear();
        this.plugin.getLogger().info("VampireManager shutdown complete");
    }
}
