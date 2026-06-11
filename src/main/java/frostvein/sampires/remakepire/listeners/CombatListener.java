package frostvein.sampires.remakepire.listeners;

import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.BeetrootManager;
import frostvein.sampires.remakepire.managers.SessionManager;
import frostvein.sampires.remakepire.managers.VampireAbilityManager;
import frostvein.sampires.remakepire.managers.VampireManager;

public class CombatListener implements Listener {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final VampireAbilityManager vampireAbilityManager;
    private final BeetrootManager beetrootManager;
    private final Random random;
    // Set the number of lives that humans have
    private static final int humanLifeCount = 5;

    /**
     * Create an instance of the Combat listener.
     *
     * @param plugin the host plugin object.
     */
    public CombatListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.vampireAbilityManager = plugin.getVampireAbilityManager();
        this.beetrootManager = plugin.getBeetrootManager();
        this.random = new Random();
    }

    /**
     * Manage the special interactions that occur during combat. This includes knocking vampires out of bat form, managing damage resistance, turnings and permadeaths, and more.
     *
     * @param event an entity being damaged by another entity.
     */
    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity reducedDamage = event.getEntity();

        // Modify the damage dealt to vampires
        if (reducedDamage instanceof Player victim) {
            if (this.vampireManager.isVampire(victim)) {
                // Reduce the damage done by mobs
                if (!(event.getDamager() instanceof Player)) {
                    event.setDamage(event.getDamage() * 0.05);
                }

                // Modify the damage using the vampiric innate resistance
                if (victim.getScoreboardTags().contains("skin_strength")) {
                    event.setDamage(event.getDamage() * 0.9);
                }
            }
        }

        // Modify the damage dealt to humans
        reducedDamage = event.getEntity();
        if (reducedDamage instanceof Player victim) {
            // Reduce the damage done by mobs
            if (this.vampireManager.isHuman(victim) && !(event.getDamager() instanceof Player)) {
                event.setDamage(event.getDamage() * 0.5);
            }
        }

        reducedDamage = event.getDamager();
        if (reducedDamage instanceof Player attacker) {
            if (!this.plugin.getSessionManager().isSessionActive()) {
                event.setCancelled(true);

            // Prevent vampires from attacking in bat form
            } else if (this.plugin.getBatTransformationManager().isInBatForm(attacker)) {
                event.setCancelled(true);
                attacker.sendMessage("§cYou cannot damage entities while in bat form");

            } else {
                // Remove vampire invisibility after too many attacks are made
                if (this.vampireManager.isVampire(attacker) && event.getEntity() instanceof Player && attacker.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                    if (this.vampireAbilityManager.trackInvisibilityAttack(attacker)) {
                        attacker.removePotionEffect(PotionEffectType.INVISIBILITY);
                        attacker.sendMessage("§cYour invisibility fades after making too many attacks.");

                    } else {
                        int attackCount = this.vampireAbilityManager.getInvisibilityAttackCount(attacker);
                        int remaining = 3 - attackCount;
                        attacker.sendMessage("§6Warning: " + remaining + " attack(s) remaining before invisibility ends.");
                    }
                }

                // Cancel vampire forms after the vampire is hit
                Entity shouldRemoveInvisibility = event.getEntity();
                if (shouldRemoveInvisibility instanceof Player victim) {
                    if (this.vampireManager.isVampire(victim)) {
                        if (this.plugin.getBatTransformationManager().isInBatForm(victim)) {
                            this.plugin.getBatTransformationManager().transformToHuman(victim);
                            victim.sendMessage("§cYou were hit and forced out of bat form.");

                        } else if (victim.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                            if (this.vampireAbilityManager.trackInvisibilityAttack(victim)) {
                                victim.removePotionEffect(PotionEffectType.INVISIBILITY);
                                victim.sendMessage("§cYour invisibility fades after being hit too many times.");

                            } else {
                                int attackCount = this.vampireAbilityManager.getInvisibilityAttackCount(victim);
                                int remaining = 3 - attackCount;
                                victim.sendMessage("§6Warning: " + remaining + " hit(s) remaining before invisibility ends.");
                            }
                        }
                    }
                }

                ItemStack attackerWeapon = attacker.getInventory().getItemInMainHand();
                Material weaponType = attackerWeapon != null ? attackerWeapon.getType() : Material.AIR;

                // Prevent stakes from being used during their cooldown
                if (weaponType == Material.WOODEN_SWORD && attacker.hasCooldown(Material.WOODEN_SWORD)) {
                    event.setCancelled(true);

                } else {
                    if (this.vampireManager.isVampire(attacker)) {
                        ItemStack weapon = attacker.getInventory().getItemInMainHand();

                        // Create the vampire claw effect
                        if (this.isBareFist(weapon)) {
                            int vampireStage = this.vampireManager.getVampireStage(attacker);
                            double multiplier = this.getVampireFistMultiplier(vampireStage);

                            if (multiplier > 1) {
                                multiplier = multiplier * 0.9 * 2;
                                event.setDamage(event.getDamage() * multiplier);

                                boolean isCriticalHit = event.getCause() == DamageCause.ENTITY_ATTACK && attacker.getFallDistance() > 0.0F && !attacker.isOnGround() && !attacker.hasPotionEffect(PotionEffectType.BLINDNESS);

                                if (vampireStage == 2 || vampireStage == 3) {
                                    this.playCrimsonSwipeSound(attacker);
                                    this.createSweepAttackEffect(event.getEntity());

                                    if (isCriticalHit) {
                                        this.applyVampireClawEffects(attacker, event.getEntity(), vampireStage);
                                    }
                                }
                            }
                        // Prevent vampires from using proper weapons while they have access to their claws
                        } else if (this.isWeaponAffectedByWeakness(weapon) && (this.vampireManager.isVampireStage2(attacker) || this.vampireManager.isVampireStage3(attacker))) {
                            event.setDamage(event.getDamage() * 0.1);

                            if (!attacker.getScoreboardTags().contains(SessionManager.INFORMED_WEAPON_WEAKNESS)) {
                                attacker.addScoreboardTag(SessionManager.INFORMED_WEAPON_WEAKNESS);
                                attacker.sendMessage("§cYour elongated claws make it difficult to use this tool effectively... As a creature of the night, you would be better tearing at your enemies with your hands than a weapon.");
                            }
                        }

                        // Apply damage reduction from the effects of sun weakness
                        if (attacker.hasPotionEffect(PotionEffectType.TRIAL_OMEN)) {
                            event.setDamage(event.getDamage() * 0.5);
                        }
                    }

                    Entity entity = event.getEntity();

                    if (!(entity instanceof Player victim)) {
                        ItemStack weapon = attacker.getInventory().getItemInMainHand();

                        // Create the effect of the one-time use stake
                        if (weapon != null && weapon.getType() == Material.WOODEN_SWORD) {
                            attacker.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                            attacker.sendMessage("§cYour wooden stake breaks apart on impact.");
                            attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0F, 1.0F);
                            attacker.setCooldown(Material.WOODEN_SWORD, this.plugin.getConfigManager().getWoodenStakeCooldownTicks());
                        }

                    } else {
                        ItemStack weaponCheck = attacker.getInventory().getItemInMainHand();

                        // Apply the impact of a stake to a vampire
                        if (weaponCheck != null && weaponCheck.getType() == Material.WOODEN_SWORD && this.vampireManager.isVampire(victim)) {
                            final double WOODEN_STAKE_DAMAGE = 8.0;
                            event.setDamage(WOODEN_STAKE_DAMAGE);

                            this.plugin.getDeathHandler().registerWoodenStakeKill(victim, attacker);

                            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0F, 1.0F);

                            attacker.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                            attacker.sendMessage("§cYour wooden stake breaks apart on impact.");
                            attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0F, 1.0F);
                            attacker.setCooldown(Material.WOODEN_SWORD, this.plugin.getConfigManager().getWoodenStakeCooldownTicks());

                        } else {
                            // Create the stake breaking effect when used on a human
                            if (weaponCheck != null && weaponCheck.getType() == Material.WOODEN_SWORD && this.vampireManager.isHuman(victim)) {
                                attacker.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                                attacker.sendMessage("§cYour wooden stake breaks apart on impact.");
                                attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0F, 1.0F);
                                attacker.setCooldown(Material.WOODEN_SWORD, this.plugin.getConfigManager().getWoodenStakeCooldownTicks());
                            }

                            // If the config is set to allow non-vampire kill sources on humans, check if the human has run out of lives
                            if (plugin.getConfigManager().isLifeLimitEnforced() && victim.getHealth() - event.getFinalDamage() <= 0) {
                                try {
                                    Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                                    Objective deathObjective = mainScoreboard.getObjective("vsmp_death");

                                    if (deathObjective != null) {
                                        int deaths = deathObjective.getScore(victim.getName()).getScore();

                                        // Only force the perma death if the human has run out of lives OR permadeath is set to ABSOLUTE
                                        if (deaths >= humanLifeCount || this.plugin.getPermadeathManager().hasAbsolutePermadeathEnabled(victim)) {
                                            event.setCancelled(true);

                                            attacker.sendMessage("§4You watch the light of " + victim.getName() + "'s eyes fade, and extinguish. Lost forever.");
                                            victim.sendMessage("§7The world grows dim, blurry... the light which drew you back so many times beckons once more, but it seems fainter now, out of reach... You lose your grip, and slip under the veil of the afterlife.");

                                            victim.addScoreboardTag(DeathHandler.PERMADEATH_CHOSEN_TAG);

                                            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> victim.setHealth(0.0));
                                            return;
                                        }
                                    }
                                } catch (Exception e) {
                                    this.plugin.getLogger().warning("Failed to check death count for " + victim.getName() + ": " + e.getMessage());
                                }
                            }

                            // Reduce a lower stage vampire's weapon damage by 10%
                            if (this.vampireManager.isVampireStage1(attacker) && this.isSwordOrAxe(attackerWeapon)) {
                                event.setDamage(event.getDamage() * 0.9);
                            }

                            // Manage vampire on human violence
                            if (this.vampireManager.isVampire(attacker) && this.vampireManager.isHuman(victim)) {
                                // Manage vampire on human murder
                                if (victim.getHealth() - event.getFinalDamage() <= 0) {
                                    this.plugin.getVampireFeedingManager().cancelFeedingSessionByTarget(victim);

                                    // Apply the effect of a chosen absolute permadeath on death
                                    if (this.plugin.getPermadeathManager().hasAbsolutePermadeathEnabled(victim)) {
                                        event.setCancelled(true);
                                        attacker.sendMessage("§4You watch the light of " + victim.getName() + "'s eyes fade, and extinguish. Lost forever.");
                                        victim.sendMessage("§7The world grows dim, blurry, you feel a darkness reach out, offering you one last chance to live, as a creature of the night... But you refuse... And slip under the veil of the afterlife.");
                                        victim.addScoreboardTag(DeathHandler.PERMADEATH_CHOSEN_TAG);

                                        int killThirst = this.plugin.getThirstManager().getKillThirstReward(attacker, victim);
                                        this.plugin.getThirstManager().modifyQuench(attacker, killThirst, true);
                                        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> victim.setHealth(0.0));
                                        return;
                                    }

                                    // Apply the effect of active garlic on death
                                    if (this.beetrootManager.hasBeetrootImmunity(victim)) {
                                        event.setCancelled(true);
                                        attacker.sendMessage("§cThe sting of garlic sears at your gums, protecting your meal from your bite.");

                                        if (this.plugin.getVampireTurningManager().isTurningEnabled(attacker)) {
                                            attacker.sendMessage("§cYou have failed to turn " + victim.getName() + " - they will respawn as a human, wounded.");
                                        } else {
                                            attacker.sendMessage("§cYou have killed " + victim.getName() + " - they will respawn as a human, wounded.");
                                        }

                                        attacker.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, this.plugin.getConfigManager().getGarlicWeaknessDuration() * 20, 9, false, false));

                                        if (this.vampireManager.isHuman(victim)) {
                                            victim.sendMessage("§a§lYour garlic immunity protects you from turning.");
                                            victim.sendMessage("§aYou will respawn as a human, not as a cursed creature.");
                                        }

                                        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> victim.setHealth(0.0));
                                        return;
                                    }

                                    if (!this.plugin.getVampireTurningManager().isTurningEnabled(attacker)) {
                                        event.setCancelled(true);

                                        try {
                                            Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                                            Objective deathObjective = mainScoreboard.getObjective("vsmp_death");

                                            // Apply the effect of a chosen permadeath on death
                                            if (deathObjective != null) {
                                                if (deathObjective.getScore(victim.getName()).getScore() >= humanLifeCount) {
                                                    attacker.sendMessage("§4You watch the light of " + victim.getName() + "'s eyes fade, and extinguish. Lost forever.");
                                                    victim.sendMessage("§7The world grows dim, blurry, you feel a darkness reach out, offering you one last chance to live, as a creature of the night... But you refuse... And slip under the veil of the afterlife.");
                                                    victim.addScoreboardTag(DeathHandler.PERMADEATH_CHOSEN_TAG);

                                                    int killThirst = this.plugin.getThirstManager().getKillThirstReward(attacker, victim);
                                                    this.plugin.getThirstManager().modifyQuench(attacker, killThirst, true);
                                                    this.plugin.getServer().getScheduler().runTask(this.plugin, () -> victim.setHealth(0.0));
                                                    return;
                                                }
                                            }
                                        } catch (Exception e) {
                                            this.plugin.getLogger().warning("Failed to check death count for " + victim.getName() + ": " + e.getMessage());
                                        }

                                        // Apply the effects of killing a human without turning them
                                        int killThirst = this.plugin.getThirstManager().getKillThirstReward(attacker, victim);
                                        this.plugin.getThirstManager().modifyQuench(attacker, killThirst, true);
                                        attacker.sendMessage("§cYou have killed " + victim.getName() + ". They will respawn as a human, wounded.");
                                        victim.sendMessage("§7You have been slain by a vampire, but they do not turn you...");

                                        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> victim.setHealth(0.0));
                                        return;
                                    }

                                    // Apply the effects of turning a cured vampire
                                    if (victim.getScoreboardTags().contains(VampireManager.CURED_VAMPIRE_TAG)) {
                                        event.setCancelled(true);
                                        attacker.sendMessage("§4You taste the blood of " + victim.getName() + ", but it rejects your curse...");
                                        attacker.sendMessage("§4They have been cleansed by holy power - their soul slips beyond your grasp, lost forever.");
                                        victim.sendMessage("§7The darkness reaches for you again, but the holy blessing protects your soul...");
                                        victim.sendMessage("§7Your past as a creature of the night cannot reclaim you. You slip into eternal peace...");
                                        victim.addScoreboardTag(DeathHandler.PERMADEATH_CHOSEN_TAG);

                                        int killThirst = this.plugin.getThirstManager().getKillThirstReward(attacker, victim);
                                        this.plugin.getThirstManager().modifyQuench(attacker, killThirst, true);
                                        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> victim.setHealth(0.0));
                                        return;
                                    }

                                    // Apply the effects of a chosen permadeath on death
                                    if (this.plugin.getPermadeathManager().hasPermadeathEnabled(victim)) {
                                        event.setCancelled(true);
                                        attacker.sendMessage("§4You watch the light of " + victim.getName() + "'s eyes fade, and extinguish. Lost forever.");
                                        victim.sendMessage("§7The world grows dim, blurry, you feel a darkness reach out, offering you one last chance to live, as a creature of the night... But you refuse... And slip under the veil of the afterlife.");
                                        victim.addScoreboardTag(DeathHandler.PERMADEATH_CHOSEN_TAG);

                                        int killThirst = this.plugin.getThirstManager().getKillThirstReward(attacker, victim);
                                        this.plugin.getThirstManager().modifyQuench(attacker, killThirst, true);
                                        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> victim.setHealth(0.0));
                                        return;
                                    }

                                    // Apply the effects of turning a human
                                    event.setCancelled(true);
                                    victim.setHealth(2.0);
                                    this.plugin.getVampireManager().performVampireTurning(victim, attacker);
                                    victim.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 300, 2, false, false));

                                    this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                                        if (this.plugin.getBeaconMajorityManager() != null) {
                                            this.plugin.getBeaconMajorityManager().removeBonusesFromPlayer(victim);
                                        }

                                        if (this.plugin.getBeaconMajorityManager() != null) {
                                            this.plugin.getBeaconMajorityManager().updateBeaconMajorityBonuses();
                                        }

                                        double maxHealth = victim.getAttribute(Attribute.MAX_HEALTH).getValue();
                                        victim.setHealth(maxHealth);
                                        this.plugin.logInfo(victim.getName() + " turned into vampire with " + maxHealth + " HP (full health)");
                                    }, 5L);

                                    victim.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1, false, false));
                                    attacker.sendMessage("§5You have turned " + victim.getName() + " into a vampire.");
                                    int killThirst = this.plugin.getThirstManager().getKillThirstReward(attacker, victim);
                                    this.plugin.getThirstManager().modifyQuench(attacker, killThirst, true);

                                    attacker.sendMessage("§cThe taste of fresh blood coats your throat as you feed, you have successfully turned " + victim.getName() + " into a creature of the night");
                                    attacker.playSound(attacker, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.MASTER, 1.0F, 0.7F);
                                    if (this.plugin.getVampireTrackingManager() != null) {
                                        this.plugin.getVampireTrackingManager().startTrackingNewVampire(victim);
                                    }

                                    return;
                                }
                            }

                            // Monitor if a vampire is being damaged by a valid killing tool
                            if (this.vampireManager.isVampire(victim)) {
                                boolean killedWithIronWeapon = weaponType == Material.IRON_SWORD || weaponType == Material.IRON_AXE;
                                boolean killedWithWoodenSword = weaponType == Material.WOODEN_SWORD;

                                if (victim.getHealth() - event.getFinalDamage() <= 0.0) {
                                    int vampireStage = this.vampireManager.getVampireStage(victim);
                                    int maximumStakeableStage = this.plugin.getConfigManager().getPermadeathMinimumStage();

                                    boolean canBeStaked = killedWithWoodenSword && vampireStage <= maximumStakeableStage;
                                    if (!killedWithIronWeapon && !canBeStaked) {
                                        event.setCancelled(true);
                                        victim.setHealth(1.0);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     *
     *
     * @param event an entity receives damage.
     */
    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Player player) {
            if (!this.plugin.getSessionManager().isSessionActive()) {
                event.setCancelled(true);
            } else {
                // Manage the vampire bleeding effect from claw swipes
                if (event.getCause() == DamageCause.WITHER && this.vampireManager.isHuman(player)) {
                    double currentHealth = player.getHealth(), finalDamage = event.getFinalDamage();

                    if (currentHealth - finalDamage < 10) {
                        event.setDamage(currentHealth - 10.0);
                        player.removePotionEffect(PotionEffectType.WITHER);
                    }
                }

                // Prevent vampires from suffocating
                if (event.getCause() == DamageCause.SUFFOCATION && this.vampireManager.isVampire(player)) {
                    event.setCancelled(true);
                } else {
                    if (this.vampireManager.isVampire(player)) {
                        EntityDamageEvent.DamageCause cause = event.getCause();

                        // Modify the fire damage dealt to vampires
                        if (cause == DamageCause.FIRE || cause == DamageCause.FIRE_TICK || cause == DamageCause.LAVA) {
                            int vampireStage = this.vampireManager.getVampireStage(player);
                            double multiplier = this.getFireDamageMultiplier(vampireStage);
                            double newDamage = event.getDamage() * multiplier;
                            event.setDamage(newDamage);
                        }

                        // Process the fire damage with the default response
                        if (event.getCause() == DamageCause.ENTITY_ATTACK) {
                            return;
                        }

                        // Ensure vampires can be killed by the void
                        if (event.getCause() == DamageCause.KILL || event.getCause() == DamageCause.VOID) {
                            return;
                        }

                        // Prevent vampires from dropping below half a heart
                        if (player.getHealth() - event.getFinalDamage() <= 0.0) {
                            event.setCancelled(true);
                            player.setHealth(1.0);
                        }
                    } else if (this.vampireManager.isHuman(player)) {
                        // If the config is set to allow non-vampire kill sources on humans, check if the human has ran out of lives
                        if (plugin.getConfigManager().isLifeLimitEnforced() && player.getHealth() - event.getFinalDamage() <= 0) {
                            try {
                                Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                                Objective deathObjective = mainScoreboard.getObjective("vsmp_death");

                                if (deathObjective != null) {
                                    int deaths = deathObjective.getScore(player.getName()).getScore();

                                    // Only force the perma death if the human has run out of lives OR permadeath is set to ABSOLUTE
                                    if (deaths >= humanLifeCount || this.plugin.getPermadeathManager().hasAbsolutePermadeathEnabled(player)) {
                                        event.setCancelled(true);

                                        player.sendMessage("§7The world grows dim, blurry... the light which drew you back so many times beckons once more, but it seems fainter now, out of reach... You lose your grip, and slip under the veil of the afterlife.");

                                        player.addScoreboardTag(DeathHandler.PERMADEATH_CHOSEN_TAG);

                                        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> player.setHealth(0.0));
                                    }
                                }
                            } catch (Exception e) {
                                this.plugin.getLogger().warning("Failed to check death count for " + player.getName() + ": " + e.getMessage());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Prevent players from losing hunger while the session is inactive
     *
     * @param event a player's food level changes.
     */
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            if (!this.plugin.getSessionManager().isSessionActive()) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Determine if the item is a wooden weapon.
     *
     * @param item the item being checked.
     * @return {@code true} if the item is a wooden sword or axe.
     */
    private boolean isWoodenWeapon(ItemStack item) {
        if (item == null) {
            return false;
        } else {
            Material type = item.getType();
            return type == Material.WOODEN_SWORD || type == Material.WOODEN_AXE;
        }
    }

    /**
     * Determine if the item is an iron weapon.
     *
     * @param item the item being checked.
     * @return {@code true} if the item is a wooden sword or axe.
     */
    private boolean isIronWeapon(ItemStack item) {
        if (item == null) {
            return false;
        } else {
            Material type = item.getType();
            return type == Material.IRON_SWORD || type == Material.IRON_AXE;
        }
    }

    /**
     * Determine if the item is an empty hand.
     *
     * @param item the item being checked.
     * @return {@code true} if the item is air or nonexistent.
     */
    private boolean isBareFist(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    /**
     * Determine if the item is a sword or axe.
     *
     * @param item the item being checked.
     * @return {@code true} if the item is a sword or axe.
     */
    private boolean isSwordOrAxe(ItemStack item) {
        if (item == null) {
            return false;
        } else {
            Material type = item.getType();

            if (type != Material.WOODEN_SWORD && type != Material.STONE_SWORD && type != Material.IRON_SWORD && type != Material.GOLDEN_SWORD && type != Material.DIAMOND_SWORD && type != Material.NETHERITE_SWORD) {
                return type == Material.WOODEN_AXE || type == Material.STONE_AXE || type == Material.IRON_AXE || type == Material.GOLDEN_AXE || type == Material.DIAMOND_AXE || type == Material.NETHERITE_AXE;
            } else {
                return true;
            }
        }
    }

    /**
     * Determine if an item should be weakened when a higher vampire uses it.
     *
     * @param item the item being checked.
     * @return {@code true} if the weapon should be weakened.
     */
    private boolean isWeaponAffectedByWeakness(ItemStack item) {
        if (item == null) {
            return false;
        } else {
            Material type = item.getType();
            if (type != Material.WOODEN_SWORD && type != Material.STONE_SWORD && type != Material.IRON_SWORD && type != Material.GOLDEN_SWORD && type != Material.DIAMOND_SWORD && type != Material.NETHERITE_SWORD) {
                return type == Material.WOODEN_AXE || type == Material.STONE_AXE || type == Material.IRON_AXE || type == Material.GOLDEN_AXE || type == Material.DIAMOND_AXE || type == Material.NETHERITE_AXE;
            } else {
                return true;
            }
        }
    }

    /**
     * Retrieve a damage multiplier for vampire claw attacks.
     *
     * @param stage the vampire stage of the attacker.
     * @return the damage multiplier of the vampire stage.
     */
    private double getVampireFistMultiplier(int stage) {
        return switch (stage) {
            case 1 -> 1.0;
            case 2 -> 2.0;
            case 3 -> 3.0;
            default -> 1.0;
        };
    }

    /**
     * Retrieve a damage multiplier for vampires receiving fire damage.
     *
     * @param stage the vampire stage of the receiver.
     * @return the damage multiplier of the vampire stage.
     */
    private double getFireDamageMultiplier(int stage) {
        return switch (stage) {
            case 1 -> 1.5;
            case 2, 3 -> 2.0;
            default -> 1.0;
        };
    }

    /**
     * Play the sound effect of a vampire claw attack.
     *
     * @param vampire the player who made the attack.
     */
    private void playCrimsonSwipeSound(Player vampire) {
        int soundNumber = this.random.nextInt(4) + 1;
        String soundKey = "crimson:crimson.sound.crimson_swipe_" + soundNumber;
        vampire.getWorld().playSound(vampire.getLocation(), soundKey, SoundCategory.PLAYERS, 0.5F, 1.0F);
    }

    /**
     * Create the visual effect of a vampire claw attack.
     *
     * @param target the player who was attacked.
     */
    private void createSweepAttackEffect(Entity target) {
        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0.0, target.getHeight() / 2, 0.0), 3, 0.5, 0.3, 0.5, 0.0);
    }

    /**
     * Apply the effects of using a stake on an attacker.
     *
     * @param attacker the player staking a victim.
     * @param woodenSword a wooden stake item.
     */
    private void applyWoodenStakeDurabilityDamage(Player attacker, ItemStack woodenSword) {
        if (woodenSword.getItemMeta() instanceof Damageable damageable) {
            short maxDurability = woodenSword.getType().getMaxDurability();
            int currentDamage = damageable.getDamage();
            int additionalDamage = maxDurability / 2;
            int newDamage = currentDamage + additionalDamage;

            if (newDamage >= maxDurability) {
                attacker.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                attacker.sendMessage("§cYour wooden stake breaks apart on impact.");
                attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0F, 1.0F);
                attacker.setCooldown(Material.WOODEN_SWORD, this.plugin.getConfigManager().getWoodenStakeCooldownTicks());

            } else {
                damageable.setDamage(newDamage);
                woodenSword.setItemMeta(damageable);
            }
        }
    }

    /**
     * Apply the effects of bleeding to a victim.
     *
     * @param attacker the player attacking the victim.
     * @param victim the player who has been hit.
     * @param vampireStage the vampire stage of the attacker.
     */
    private void applyVampireClawEffects(Player attacker, Entity victim, int vampireStage) {
        boolean witherApplied = false;

        if (victim instanceof Player livingVictim) {
            double bleedingChance = 0.0;
            int witherLevel = 0;

            if (vampireStage == 2) {
                bleedingChance = 0.33;
                witherLevel = 0;

            } else if (vampireStage == 3) {
                bleedingChance = 0.66;
                witherLevel = 1;
            }

            if (bleedingChance > 0 && this.random.nextDouble() < bleedingChance) {
                livingVictim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 140, witherLevel, false, false));
                witherApplied = true;
            }
        }

        // Create the particle effects of bleeding on the victim
        if (witherApplied) {
            Location particleLocation = victim.getLocation().add(0.0, victim.getHeight() / 2, 0.0);
            victim.getWorld().spawnParticle(Particle.DUST, particleLocation, 20, 0.5, 0.3, 0.5, 0.0, new Particle.DustOptions(Color.RED, 1.0F));
            victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, particleLocation, 8, 0.3, 0.2, 0.3, 0.0);
        }

        if (victim instanceof Player humanVictim && this.vampireManager.isHuman(humanVictim)) {
            if (!humanVictim.getScoreboardTags().contains(SessionManager.INFORMED_VAMPIRE_CLAWS)) {
                humanVictim.addScoreboardTag(SessionManager.INFORMED_VAMPIRE_CLAWS);
                humanVictim.sendMessage("§cThe creatures claws rip your skin open, you are bleeding!");
            }
        }
    }
}
