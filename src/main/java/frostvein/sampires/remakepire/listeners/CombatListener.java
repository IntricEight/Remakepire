package frostvein.sampires.remakepire.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.SessionManager;
import frostvein.sampires.remakepire.managers.VampireAbilityManager;
import frostvein.sampires.remakepire.managers.VampireManager;
import frostvein.sampires.remakepire.utils.ItemTypeChecking;

public class CombatListener implements Listener {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final VampireAbilityManager vampireAbilityManager;
    private final Random random;
    private final int CLAW_KILL_HITS;
    private final Map<UUID, VampireClawHitCounter> vampireClawHitCounters = new HashMap<>();

    /**
     * Create an instance of the Combat listener.
     *
     * @param plugin the host plugin object.
     */
    public CombatListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.vampireAbilityManager = plugin.getVampireAbilityManager();
        this.random = new Random();

        this.CLAW_KILL_HITS = this.plugin.getConfigManager().getClawHitKillRequirement();
        this.beginClawHitCounterRefresh();
    }

    /**
     * Begin checking the active claw hit counters to determine if they need clearing
     */
    private void beginClawHitCounterRefresh() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!vampireClawHitCounters.isEmpty()) {
                vampireClawHitCounters.entrySet().removeIf(entry -> entry.getValue().hasTimerElapsed());
            }
        }, 600L, 600L);
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
        // Stop all damage if the session is not active
        if (!this.plugin.getSessionManager().isSessionActive()) {
            event.setCancelled(true);
            return;
        }

        // Apply the generic damage reduction to players
        this.applyDamageReduction(event);

        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player attacker) {
            final boolean isVictimHuman = this.vampireManager.isHuman(victim), isVictimVampire = this.vampireManager.isVampire(victim);
            final boolean isAttackerHuman = this.vampireManager.isHuman(attacker), isAttackerVampire = this.vampireManager.isVampire(attacker);

            // Prevent vampires from attacking in bat form
            if (isAttackerVampire && this.plugin.getBatTransformationManager().isInBatForm(attacker)) {
                event.setCancelled(true);
                attacker.sendMessage(Component.text("You cannot damage entities while in bat form", NamedTextColor.RED));
                return;

            } else if (isVictimVampire && this.plugin.getBatTransformationManager().isInBatForm(victim)) {
                this.plugin.getBatTransformationManager().transformToHuman(victim);
                victim.sendMessage(Component.text("You were hit and forced out of bat form.", NamedTextColor.RED));
            }

            // Check if the vampire is attacking or being attacked while being invisible
            if (isAttackerVampire && attacker.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                this.attackingWhileInvisible(attacker);

            } else if (isVictimVampire && victim.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                // Knock vampires out of invisibility if they get hit too much
                this.attackedWhileInvisible(victim);
            }

            Material attackerWeaponType = attacker.getInventory().getItemInMainHand().getType();

            // Prevent stakes from being used during their cooldown
            if (ItemTypeChecking.isStake(attackerWeaponType) && attacker.hasCooldown(Material.WOODEN_SWORD)) {
                event.setCancelled(true);
                return;
            }

            if (isAttackerVampire) {
                // Create the vampire claw effect
                if (ItemTypeChecking.isBareFist(attackerWeaponType)) {
                    final int vampireStage = this.vampireManager.getVampireStage(attacker);
                    double multiplier = this.getVampireFistMultiplier(vampireStage);

                    if (multiplier > 1) {
                        // Create the damage of a claw
                        multiplier = multiplier * 0.9 * 2;
                        event.setDamage(event.getDamage() * multiplier);

                        if (vampireStage == 2 || vampireStage == 3) {
                            this.playCrimsonSwipeSound(attacker);
                            this.createSweepAttackEffect(event.getEntity());

                            // Apply a bleeding effect from a critical hit to the victim
                            if (event.getCause() == DamageCause.ENTITY_ATTACK && attacker.getFallDistance() > 0.0F && !attacker.isOnGround() && !attacker.hasPotionEffect(PotionEffectType.BLINDNESS)) {
                                this.applyVampireClawEffects(attacker, event.getEntity(), vampireStage);
                            }
                        }
                    }
                // Prevent vampires from using proper weapons while they have access to their claws
                } else if (this.isWeaponAffectedByWeakness(attackerWeaponType) && (this.vampireManager.isVampireStage2(attacker) || this.vampireManager.isVampireStage3(attacker))) {
                    event.setDamage(event.getDamage() * 0.1);

                    if (!attacker.getScoreboardTags().contains(SessionManager.INFORMED_WEAPON_WEAKNESS)) {
                        attacker.addScoreboardTag(SessionManager.INFORMED_WEAPON_WEAKNESS);
                        attacker.sendMessage(Component.text("Your elongated claws make it difficult to use this tool effectively... As a creature of the night, you would be better tearing at your enemies with your hands than a weapon.", NamedTextColor.RED));
                    }
                }

                // Apply damage reduction from the effects of sun weakness
                if (attacker.hasPotionEffect(PotionEffectType.TRIAL_OMEN)) {
                    event.setDamage(event.getDamage() * 0.5);
                }
            }

            // Apply the impact of a stake to a vampire
            if (ItemTypeChecking.isStake(attackerWeaponType) && isVictimVampire) {
                final double WOODEN_STAKE_DAMAGE = 8.0;
                event.setDamage(WOODEN_STAKE_DAMAGE);

                this.plugin.getDeathHandler().registerWoodenStakeKill(victim, attacker);

                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0F, 1.0F);
                this.breakStake(attacker);

                return;
            }

            // Create the stake breaking effect when used on a human
            if (ItemTypeChecking.isStake(attackerWeaponType) && isVictimHuman) {
                this.breakStake(attacker);
            }

            // If the config is set to allow non-vampire kill sources on humans, check if the human has run out of lives
            if (plugin.getConfigManager().isLifeLimitEnforced() && victim.getHealth() - event.getFinalDamage() <= 0) {
                if (this.plugin.getDeathHandler().shouldHumanPermadie(victim)) {
                    event.setCancelled(true);
                    this.plugin.getDeathHandler().triggerHumanPermadeath(attacker, victim, false);
                    return;
                }
            }

            // Reduce a lower stage vampire's weapon damage by 10%
            if (this.vampireManager.isVampireStage1(attacker) && this.isWeaponAffectedByWeakness(attackerWeaponType)) {
                event.setDamage(event.getDamage() * 0.9);
            }

            // Manage vampire on human violence
            if (isAttackerVampire && isVictimHuman) {
                // Manage vampire on human murder
                if (victim.getHealth() - event.getFinalDamage() <= 0) {
                    this.plugin.getVampireFeedingManager().cancelFeedingSessionByTarget(victim);

                    // Apply the effect of a chosen absolute permadeath on death
                    if (this.plugin.getPermadeathManager().hasAbsolutePermadeathEnabled(victim)) {
                        event.setCancelled(true);
                        this.plugin.getDeathHandler().triggerHumanPermadeath(attacker, victim, true);

                        int killThirst = this.plugin.getThirstManager().getKillThirstReward(attacker, victim);
                        this.plugin.getThirstManager().modifyQuench(attacker, killThirst, true);
                        return;
                    }

                    // Apply the effect of active garlic on death
                    if (this.plugin.getBeetrootManager().hasBeetrootImmunity(victim)) {
                        event.setCancelled(true);
                        attacker.sendMessage(Component.text("The sting of garlic sears at your gums, protecting your meal from your bite.", NamedTextColor.RED));

                        if (this.plugin.getVampireTurningManager().isTurningEnabled(attacker)) {
                            attacker.sendMessage(Component.text("You have failed to turn " + victim.getName() + " - they will respawn as a human, wounded.", NamedTextColor.RED));
                        } else {
                            attacker.sendMessage(Component.text("You have killed " + victim.getName() + " - they will respawn as a human, wounded.", NamedTextColor.RED));
                        }

                        attacker.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, this.plugin.getConfigManager().getGarlicWeaknessDuration() * 20, 9, false, false));

                        victim.sendMessage(Component.text("Your garlic immunity protects you from turning.", NamedTextColor.GREEN)
                                .decorate(TextDecoration.BOLD));
                        victim.sendMessage(Component.text("You will respawn as a human, not as a cursed creature.", NamedTextColor.GREEN));

                        victim.setHealth(0.0);
                        return;
                    }

                    if (!this.plugin.getVampireTurningManager().isTurningEnabled(attacker)) {
                        event.setCancelled(true);

                        if (this.plugin.getDeathHandler().shouldHumanPermadie(victim)) {
                            this.plugin.getDeathHandler().triggerHumanPermadeath(attacker, victim, true);

                            final int killThirst = this.plugin.getThirstManager().getKillThirstReward(attacker, victim);
                            this.plugin.getThirstManager().modifyQuench(attacker, killThirst, true);
                            return;
                        }

                        // Apply the effects of killing a human without turning them
                        final int killThirst = this.plugin.getThirstManager().getKillThirstReward(attacker, victim);
                        this.plugin.getThirstManager().modifyQuench(attacker, killThirst, true);
                        attacker.sendMessage(Component.text("You have killed " + victim.getName() + ". They will respawn as a human, wounded.", NamedTextColor.RED));
                        victim.sendMessage(Component.text("You have been slain by a vampire, but they do not turn you...", NamedTextColor.GRAY));

                        victim.setHealth(0.0);
                        return;
                    }

                    // Apply the effects of turning a cured vampire
                    if (victim.getScoreboardTags().contains(VampireManager.CURED_VAMPIRE_TAG)) {
                        event.setCancelled(true);

                        attacker.sendMessage(Component.text("You taste the blood of " + victim.getName() + ", but it rejects your curse...", NamedTextColor.DARK_RED));
                        attacker.sendMessage(Component.text("They have been cleansed by holy power - their soul slips beyond your grasp, lost forever.", NamedTextColor.DARK_RED));

                        victim.sendMessage(Component.text("The darkness reaches for you again, but the holy blessing protects your soul...", NamedTextColor.GRAY));
                        victim.sendMessage(Component.text("Your past as a creature of the night cannot reclaim you. You slip into eternal peace...", NamedTextColor.GRAY));

                        victim.addScoreboardTag(DeathHandler.PERMADEATH_CHOSEN_TAG);
                        victim.setHealth(0.0);

                        final int killThirst = this.plugin.getThirstManager().getKillThirstReward(attacker, victim);
                        this.plugin.getThirstManager().modifyQuench(attacker, killThirst, true);
                        return;
                    }

                    // Apply the effects of a chosen permadeath on death
                    if (this.plugin.getPermadeathManager().hasPermadeathEnabled(victim)) {
                        event.setCancelled(true);
                        this.plugin.getDeathHandler().triggerHumanPermadeath(attacker, victim, true);

                        final int killThirst = this.plugin.getThirstManager().getKillThirstReward(attacker, victim);
                        this.plugin.getThirstManager().modifyQuench(attacker, killThirst, true);
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

                        final double maxHealth = victim.getAttribute(Attribute.MAX_HEALTH).getValue();
                        victim.setHealth(maxHealth);
                        this.plugin.logInfo(victim.getName() + " turned into vampire with " + maxHealth + " HP (full health)");
                    }, 5L);

                    victim.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1, false, false));
                    attacker.sendMessage(Component.text("You have turned " + victim.getName() + " into a vampire.", NamedTextColor.DARK_PURPLE));
                    final int killThirst = this.plugin.getThirstManager().getKillThirstReward(attacker, victim);
                    this.plugin.getThirstManager().modifyQuench(attacker, killThirst, true);

                    attacker.sendMessage(Component.text("The taste of fresh blood coats your throat as you feed, you have successfully turned " + victim.getName() + " into a creature of the night", NamedTextColor.RED));
                    attacker.playSound(attacker, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.MASTER, 1.0F, 0.7F);

                    if (this.plugin.getVampireTrackingManager() != null) {
                        this.plugin.getVampireTrackingManager().startTrackingNewVampire(victim);
                    }

                    return;
                }
            }

            // Monitor if a vampire is being damaged by a valid killing tool
            if (isVictimVampire) {
                if (victim.getHealth() - event.getFinalDamage() <= 0.0) {
                    final int vampireStage = this.vampireManager.getVampireStage(victim), maximumStakeableStage = this.plugin.getConfigManager().getPermadeathMinimumStage();
                    final boolean canBeStaked = ItemTypeChecking.isStake(attackerWeaponType) && vampireStage <= maximumStakeableStage;

                    // Check the number of times they have been lethally hit by claws
                    if (ItemTypeChecking.isBareFist(attackerWeaponType) && this.vampireManager.isVampireStage2OrHigher(attacker)) {
                        if (this.vampireClawHitCounters.isEmpty() || this.vampireClawHitCounters.get(victim.getUniqueId()) == null) {
                            VampireClawHitCounter newHitCounter = new VampireClawHitCounter();
                            this.vampireClawHitCounters.put(victim.getUniqueId(), newHitCounter);
                            newHitCounter.currentHits++;
                            newHitCounter.updateHitTimer();

                            if (!newHitCounter.shouldVampireDie()) {
                                event.setCancelled(true);
                                victim.setHealth(1.0);
                            }
                        } else {
                            VampireClawHitCounter hitCounter = this.vampireClawHitCounters.get(victim.getUniqueId());
                            hitCounter.currentHits++;
                            hitCounter.updateHitTimer();

                            if (!hitCounter.shouldVampireDie()) {
                                event.setCancelled(true);
                                victim.setHealth(1.0);
                            }
                        }
                    } else if (!ItemTypeChecking.isIronWeapon(attackerWeaponType) && !canBeStaked) {
                        event.setCancelled(true);
                        victim.setHealth(1.0);
                    }
                }
            }
        }
    }

    /**
     * Manage a player taking damage outside dof active sessions, or when damage was not caused by another player.
     *
     * @param event an entity receives damage.
     */
    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onEntityDamage(EntityDamageEvent event) {
        // Stop all damage if the session is not active
        if (!this.plugin.getSessionManager().isSessionActive()) {
            event.setCancelled(true);
            return;
        }

        Entity entity = event.getEntity();

        if (entity instanceof Player player) {
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
                return;
            }

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
                // If the config is set to allow non-vampire kill sources on humans, check if the human has run out of lives
                if (plugin.getConfigManager().isLifeLimitEnforced() && player.getHealth() - event.getFinalDamage() <= 0) {
                    if (this.plugin.getDeathHandler().shouldHumanPermadie(player)) {
                        event.setCancelled(true);
                        player.sendMessage(Component.text("The world grows dim, blurry... the light which drew you back so many times beckons once more, but it seems fainter now, out of reach... You lose your grip, and slip under the veil of the afterlife.", NamedTextColor.GRAY));
                        player.addScoreboardTag(DeathHandler.PERMADEATH_CHOSEN_TAG);
                        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> player.setHealth(0.0));
                    }
                }
            }
        }
    }

    /**
     * Prevent players from losing hunger while the session is inactive.
     *
     * @param event a player's food level changes.
     */
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            // Prevent losing food bars if session is not active
            if (!this.plugin.getSessionManager().isSessionActive()) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Determine if an item should be weakened when a higher vampire uses it.
     *
     * @param type the item being checked.
     * @return {@code true} if the weapon should be weakened.
     */
    private boolean isWeaponAffectedByWeakness(Material type) {
        return ItemTypeChecking.isWeapon(type);
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
        final String soundKey = "crimson:crimson.sound.crimson_swipe_" + this.random.nextInt(4) + 1;
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
            final short maxDurability = woodenSword.getType().getMaxDurability();
            final int currentDamage = damageable.getDamage(), additionalDamage = maxDurability / 2;
            final int newDamage = currentDamage + additionalDamage;

            if (newDamage >= maxDurability) {
                attacker.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                attacker.sendMessage(Component.text("Your wooden stake breaks apart on impact.", NamedTextColor.RED));
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
                humanVictim.sendMessage(Component.text("The creatures claws rip your skin open, you are bleeding!", NamedTextColor.RED));
            }
        }
    }

    /**
     * Apply the initial damage reduction to an entity-inflicted damage.
     *
     * @param event an entity has damaged another entity.
     */
    private void applyDamageReduction(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim) {
            // Modify the damage dealt to humans
            if (this.vampireManager.isHuman(victim)) {
                // Reduce the damage done by mobs
                if (!(event.getDamager() instanceof Player)) {
                    event.setDamage(event.getDamage() * 0.5);
                }

                // Modify the damage dealt to vampires
            } else if (this.vampireManager.isVampire(victim)) {
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
    }

    /**
     * Check if the vampire has made too many attacks to remain invisible.
     *
     * @param attacker an invisible vampire who is attacking another player.
     */
    private void attackingWhileInvisible(Player attacker) {
        // Remove vampire invisibility after too many attacks are made
        if (this.vampireAbilityManager.trackInvisibilityAttack(attacker)) {
            attacker.removePotionEffect(PotionEffectType.INVISIBILITY);
            attacker.sendMessage(Component.text("Your invisibility fades after making too many attacks.", NamedTextColor.RED));

        } else {
            final int remaining = 3 - this.vampireAbilityManager.getInvisibilityAttackCount(attacker);
            attacker.sendMessage(Component.text("Warning: " + remaining + " attack(s) remaining before invisibility ends.", NamedTextColor.GOLD));
        }
    }

    /**
     * Check if the vampire has been hit too many times to remain visible.
     *
     * @param victim an invisible vampire who is being hit.
     */
    private void attackedWhileInvisible(Player victim) {
        // Knock vampires out of invisibility if they get hit too much
        if (this.vampireAbilityManager.trackInvisibilityAttack(victim)) {
            victim.removePotionEffect(PotionEffectType.INVISIBILITY);
            victim.sendMessage(Component.text("Your invisibility fades after being hit too many times.", NamedTextColor.RED));

        } else {
            final int remaining = 3 - this.vampireAbilityManager.getInvisibilityAttackCount(victim);
            victim.sendMessage(Component.text("Warning: " + remaining + " hit(s) remaining before invisibility ends.", NamedTextColor.GOLD));
        }
    }

    /**
     * Create the effects of breaking a stake.
     *
     * @param attacker the player who used a stake.
     */
    private void breakStake(Player attacker) {
        attacker.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        attacker.sendMessage(Component.text("Your wooden stake breaks apart on impact.", NamedTextColor.RED));
        attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0F, 1.0F);
        attacker.setCooldown(Material.WOODEN_SWORD, this.plugin.getConfigManager().getWoodenStakeCooldownTicks());
    }

    /**
     * Notify the log that this listener is shut down.
     */
    public void shutdown() {
        if (!this.vampireClawHitCounters.isEmpty()) {
            this.vampireClawHitCounters.clear();
        }

        this.plugin.logInfo("CombatListener: Shutdown complete");
    }

    /**
     * Clear the claw hit counter of the player.
     *
     * @param playerUUID the UUID of the player.
     */
    public void clearClawCounter(UUID playerUUID) {
        this.vampireClawHitCounters.remove(playerUUID);
    }

    private class VampireClawHitCounter {
        public int currentHits = 0;
        private long timeOfLastHit;

        /**
         * Create an instance of the claw hit tracker.
         */
        public VampireClawHitCounter() {
            this.updateHitTimer();
            this.currentHits = 0;
        }

        /**
         * Update the recorded last time that the player was hit by a claw and took no damage.
         */
        public void updateHitTimer() {
            timeOfLastHit = System.currentTimeMillis();
        }

        /**
         * Determine if the vampire should die after being hit by claws.
         *
         * @return {@code true} if the vampire has been hit enough times to die from claw hits.
         */
        public boolean shouldVampireDie() {
            return currentHits >= CLAW_KILL_HITS;
        }

        /**
         * Determine if the hit counter should be refreshed.
         *
         * @return {@code true} if 5 minutes have passed since the player was last hit.
         */
        public boolean hasTimerElapsed () {
            return System.currentTimeMillis() - timeOfLastHit >= (5 * 60 * 1000L);
        }

    }
}
