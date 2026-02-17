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
import frostvein.sampires.remakepire.managers.VampireAbilityManager;
import frostvein.sampires.remakepire.managers.VampireManager;

public class CombatListener implements Listener {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final VampireAbilityManager vampireAbilityManager;
    private final BeetrootManager beetrootManager;
    private final Random random;

    public CombatListener(RemakepirePlugin plugin, VampireManager vampireManager) {
        this.plugin = plugin;
        this.vampireManager = vampireManager;
        this.vampireAbilityManager = plugin.getVampireAbilityManager();
        this.beetrootManager = plugin.getBeetrootManager();
        this.random = new Random();
    }

    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity reducedDamage = event.getEntity();
        if (reducedDamage instanceof Player victim) {
            if (this.vampireManager.isVampire(victim)) {
                if (!(event.getDamager() instanceof Player)) {
                    event.setDamage(event.getDamage() * 0.05);
                }

                if (victim.getScoreboardTags().contains("skin_strength")) {
                    event.setDamage(event.getDamage() * 0.9);
                }
            }
        }

        reducedDamage = event.getEntity();
        if (reducedDamage instanceof Player victim) {
            if (this.vampireManager.isHuman(victim) && !(event.getDamager() instanceof Player)) {
                event.setDamage(event.getDamage() * (double)0.5F);
            }
        }

        reducedDamage = event.getDamager();
        if (reducedDamage instanceof Player attacker) {
            if (!this.plugin.getSessionManager().isSessionActive()) {
                event.setCancelled(true);

            } else if (this.plugin.getBatTransformationManager().isInBatForm(attacker)) {
                event.setCancelled(true);
                attacker.sendMessage("§cYou cannot damage entities while in bat form");

            } else {
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

                Entity shouldRemoveInvisibility = event.getEntity();
                if (shouldRemoveInvisibility instanceof Player) {
                    Player victim = (Player)shouldRemoveInvisibility;
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
                if (weaponType == Material.WOODEN_SWORD && attacker.hasCooldown(Material.WOODEN_SWORD)) {
                    event.setCancelled(true);

                } else {
                    if (this.vampireManager.isVampire(attacker)) {
                        ItemStack weapon = attacker.getInventory().getItemInMainHand();

                        if (this.isBareFist(weapon)) {
                            int vampireStage = this.vampireManager.getVampireStage(attacker);
                            double multiplier = this.getVampireFistMultiplier(vampireStage);

                            if (multiplier > 1.0) {
                                multiplier = multiplier * 0.9 * 2.0;
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
                        } else if (this.isWeaponAffectedByWeakness(weapon) && (this.vampireManager.isVampireStage2(attacker) || this.vampireManager.isVampireStage3(attacker))) {
                            event.setDamage(event.getDamage() * 0.1);

                            if (!attacker.getScoreboardTags().contains("informed_weapon_weakness")) {
                                attacker.sendMessage("§cYour elongated claws make it difficult to use this tool effectively... As a creature of the night, you would be better tearing at your enemies with your hands than a weapon.");
                                attacker.addScoreboardTag("informed_weapon_weakness");
                            }
                        }

                        if (attacker.hasPotionEffect(PotionEffectType.TRIAL_OMEN)) {
                            event.setDamage(event.getDamage() * 0.5);
                        }
                    }

                    Entity entity = event.getEntity();
                    if (!(entity instanceof Player)) {
                        ItemStack weapon = attacker.getInventory().getItemInMainHand();
                        if (weapon != null && weapon.getType() == Material.WOODEN_SWORD) {
                            attacker.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                            attacker.sendMessage("§cYour wooden stake breaks apart on impact.");
                            attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0F, 1.0F);
                            attacker.setCooldown(Material.WOODEN_SWORD, this.plugin.getConfigManager().getWoodenStakeCooldownTicks());
                        }

                    } else {
                        Player victim = (Player)entity;
                        ItemStack weaponCheck = attacker.getInventory().getItemInMainHand();

                        if (weaponCheck != null && weaponCheck.getType() == Material.WOODEN_SWORD && this.vampireManager.isVampire(victim)) {
                            final double WOODEN_STAKE_DAMAGE = 8.0;
                            event.setDamage(WOODEN_STAKE_DAMAGE);

                            this.plugin.getDeathHandler().registerWoodenStakeKill(victim, attacker);

                            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0F, 1.0F);

                            // TODO: Decompilation Fixing, remove comments once success is confirmed

//                            event.setCancelled(true);
//
//                            EntityDamageByEntityEvent fakeDamageEvent = new EntityDamageByEntityEvent(attacker, victim, DamageCause.ENTITY_ATTACK, 8.0);
//                            victim.setLastDamageCause(fakeDamageEvent);
//                            this.plugin.getDeathHandler().registerWoodenStakeKill(victim, attacker);
//
//                            double WOODEN_STAKE_DAMAGE = 8.0, currentHealth = victim.getHealth();
//                            double newHealth = Math.max(0.0, currentHealth - WOODEN_STAKE_DAMAGE);
//
//                            victim.setHealth(newHealth);
//                            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0F, 1.0F);
//                            victim.damage(0.0);

                            attacker.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                            attacker.sendMessage("§cYour wooden stake breaks apart on impact.");
                            attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0F, 1.0F);
                            attacker.setCooldown(Material.WOODEN_SWORD, this.plugin.getConfigManager().getWoodenStakeCooldownTicks());

                        } else {
                            if (weaponCheck != null && weaponCheck.getType() == Material.WOODEN_SWORD && this.vampireManager.isHuman(victim)) {
                                attacker.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                                attacker.sendMessage("§cYour wooden stake breaks apart on impact.");
                                attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0F, 1.0F);
                                attacker.setCooldown(Material.WOODEN_SWORD, this.plugin.getConfigManager().getWoodenStakeCooldownTicks());
                            }

                            if (this.vampireManager.isVampireStage1(attacker) && this.isSwordOrAxe(attackerWeapon)) {
                                event.setDamage(event.getDamage() * 0.9);
                            }

                            if (this.vampireManager.isVampire(attacker) && this.vampireManager.isHuman(victim)) {
                                if (victim.getHealth() - event.getFinalDamage() <= 0.0) {
                                    this.plugin.getVampireFeedingManager().cancelFeedingSessionByTarget(victim);

                                    if (this.plugin.getPermadeathManager().hasAbsolutePermadeathEnabled(victim)) {
                                        event.setCancelled(true);
                                        attacker.sendMessage("§4You watch the light of " + victim.getName() + "'s eyes fade, and extinguish. Lost forever.");
                                        victim.sendMessage("§7The world grows dim, blurry, you feel a darkness reach out, offering you one last chance to live, as a creature of the night... But you refuse... And slip under the veil of the afterlife.");
                                        victim.addScoreboardTag("PermadeathChosen");

                                        int killThirst = this.plugin.getThirstManager().getKillThirstReward(attacker, victim);
                                        this.plugin.getThirstManager().modifyQuench(attacker, killThirst, true);
                                        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> victim.setHealth(0.0));
                                        return;
                                    }

                                    if (this.beetrootManager.hasBeetrootImmunity(victim)) {
                                        event.setCancelled(true);
                                        attacker.sendMessage("§cThe sting of garlic sears at your gums, protecting your meal from your bite.");

                                        if (this.plugin.getVampireTurningManager().isTurningEnabled(attacker)) {
                                            attacker.sendMessage("§cYou have failed to turn " + victim.getName() + " - they will respawn as a human, wounded.");
                                        } else {
                                            attacker.sendMessage("§cYou have killed " + victim.getName() + " - they will respawn as a human, wounded.");
                                        }

                                        attacker.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 3600, 9, false, false));

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

                                            if (deathObjective != null) {
                                                int currentDeaths = deathObjective.getScore(victim.getName()).getScore();
                                                if (currentDeaths >= 5) {
                                                    attacker.sendMessage("§4You watch the light of " + victim.getName() + "'s eyes fade, and extinguish. Lost forever.");
                                                    victim.sendMessage("§7The world grows dim, blurry, you feel a darkness reach out, offering you one last chance to live, as a creature of the night... But you refuse... And slip under the veil of the afterlife.");
                                                    victim.addScoreboardTag("PermadeathChosen");

                                                    int killThirst = this.plugin.getThirstManager().getKillThirstReward(attacker, victim);
                                                    this.plugin.getThirstManager().modifyQuench(attacker, killThirst, true);
                                                    this.plugin.getServer().getScheduler().runTask(this.plugin, () -> victim.setHealth(0.0));
                                                    return;
                                                }
                                            }
                                        } catch (Exception e) {
                                            this.plugin.getLogger().warning("Failed to check death count for " + victim.getName() + ": " + e.getMessage());
                                        }

                                        int killThirst = this.plugin.getThirstManager().getKillThirstReward(attacker, victim);
                                        this.plugin.getThirstManager().modifyQuench(attacker, killThirst, true);
                                        attacker.sendMessage("§cYou have killed " + victim.getName() + ". They will respawn as a human, wounded.");
                                        victim.sendMessage("§7You have been slain by a vampire, but they do not turn you...");
                                        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> victim.setHealth((double)0.0F));
                                        return;
                                    }

                                    if (victim.getScoreboardTags().contains("CuredVampire")) {
                                        event.setCancelled(true);
                                        attacker.sendMessage("§4You taste the blood of " + victim.getName() + ", but it rejects your curse...");
                                        attacker.sendMessage("§4They have been cleansed by holy power - their soul slips beyond your grasp, lost forever.");
                                        victim.sendMessage("§7The darkness reaches for you again, but the holy blessing protects your soul...");
                                        victim.sendMessage("§7Your past as a creature of the night cannot reclaim you. You slip into eternal peace...");
                                        victim.addScoreboardTag("PermadeathChosen");

                                        int killThirst = this.plugin.getThirstManager().getKillThirstReward(attacker, victim);
                                        this.plugin.getThirstManager().modifyQuench(attacker, killThirst, true);
                                        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> victim.setHealth(0.0));
                                        return;
                                    }

                                    if (this.plugin.getPermadeathManager().hasPermadeathEnabled(victim)) {
                                        event.setCancelled(true);
                                        attacker.sendMessage("§4You watch the light of " + victim.getName() + "'s eyes fade, and extinguish. Lost forever.");
                                        victim.sendMessage("§7The world grows dim, blurry, you feel a darkness reach out, offering you one last chance to live, as a creature of the night... But you refuse... And slip under the veil of the afterlife.");
                                        victim.addScoreboardTag("PermadeathChosen");

                                        int killThirst = this.plugin.getThirstManager().getKillThirstReward(attacker, victim);
                                        this.plugin.getThirstManager().modifyQuench(attacker, killThirst, true);
                                        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> victim.setHealth(0.0));
                                        return;
                                    }

                                    event.setCancelled(true);
                                    victim.setHealth((double)2.0F);
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
                                        this.plugin.getLogger().info(victim.getName() + " turned into vampire with " + maxHealth + " HP (full health)");
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

                            if (this.vampireManager.isVampire(victim)) {
                                boolean killedWithIronWeapon = weaponType == Material.IRON_SWORD || weaponType == Material.IRON_AXE;
                                boolean killedWithWoodenSword = weaponType == Material.WOODEN_SWORD;
                                double finalDamage = event.getFinalDamage();
                                if (victim.getHealth() - finalDamage <= (double)0.0F) {
                                    int vampireStage = this.vampireManager.getVampireStage(victim);
                                    int maximumStakeableStage = this.plugin.getConfigManager().getPermadeathMinimumStage();
                                    boolean canBeStaked = killedWithWoodenSword && vampireStage <= maximumStakeableStage;
                                    if (!killedWithIronWeapon && !canBeStaked) {
                                        event.setCancelled(true);
                                        victim.setHealth((double)1.0F);
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
    }

    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onEntityDamage(EntityDamageEvent event) {
        Entity var3 = event.getEntity();
        if (var3 instanceof Player player) {
            if (!this.plugin.getSessionManager().isSessionActive()) {
                event.setCancelled(true);
            } else {
                if (event.getCause() == DamageCause.WITHER && this.vampireManager.isHuman(player)) {
                    double currentHealth = player.getHealth();
                    double finalDamage = event.getFinalDamage();
                    if (currentHealth - finalDamage < (double)10.0F) {
                        event.setDamage(currentHealth - (double)10.0F);
                        player.removePotionEffect(PotionEffectType.WITHER);
                    }
                }

                if (event.getCause() == DamageCause.SUFFOCATION && this.vampireManager.isVampire(player)) {
                    event.setCancelled(true);
                } else {
                    if (this.vampireManager.isVampire(player)) {
                        EntityDamageEvent.DamageCause cause = event.getCause();
                        if (cause == DamageCause.FIRE || cause == DamageCause.FIRE_TICK || cause == DamageCause.LAVA) {
                            int vampireStage = this.vampireManager.getVampireStage(player);
                            double multiplier = this.getFireDamageMultiplier(vampireStage);
                            double newDamage = event.getDamage() * multiplier;
                            event.setDamage(newDamage);
                        }
                    }

                    if (this.vampireManager.isVampire(player)) {
                        if (event.getCause() == DamageCause.ENTITY_ATTACK) {
                            return;
                        }

                        if (event.getCause() == DamageCause.KILL || event.getCause() == DamageCause.VOID) {
                            return;
                        }

                        double finalDamage = event.getFinalDamage();
                        if (player.getHealth() - finalDamage <= (double)0.0F) {
                            event.setCancelled(true);
                            player.setHealth((double)1.0F);
                        }
                    }

                }
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            if (!this.plugin.getSessionManager().isSessionActive()) {
                event.setCancelled(true);
            }

        }
    }

    private boolean isWoodenWeapon(ItemStack item) {
        if (item == null) {
            return false;
        } else {
            Material type = item.getType();
            return type == Material.WOODEN_SWORD || type == Material.WOODEN_AXE;
        }
    }

    private boolean isIronWeapon(ItemStack item) {
        if (item == null) {
            return false;
        } else {
            Material type = item.getType();
            return type == Material.IRON_SWORD || type == Material.IRON_AXE;
        }
    }

    private boolean isBareFist(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

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

    private double getVampireFistMultiplier(int stage) {
        double var10000;
        switch (stage) {
            case 1 -> var10000 = (double)1.0F;
            case 2 -> var10000 = (double)2.0F;
            case 3 -> var10000 = (double)3.0F;
            default -> var10000 = (double)1.0F;
        }

        return var10000;
    }

    private double getFireDamageMultiplier(int stage) {
        double var10000;
        switch (stage) {
            case 1 -> var10000 = (double)1.5F;
            case 2 -> var10000 = (double)2.0F;
            case 3 -> var10000 = (double)2.0F;
            default -> var10000 = (double)1.0F;
        }

        return var10000;
    }

    private void playCrimsonSwipeSound(Player vampire) {
        int soundNumber = this.random.nextInt(4) + 1;
        String soundKey = "crimson:crimson.sound.crimson_swipe_" + soundNumber;
        vampire.getWorld().playSound(vampire.getLocation(), soundKey, SoundCategory.PLAYERS, 0.5F, 1.0F);
    }

    private void createSweepAttackEffect(Entity target) {
        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add((double)0.0F, target.getHeight() / (double)2.0F, (double)0.0F), 3, (double)0.5F, 0.3, (double)0.5F, (double)0.0F);
    }

    private void applyWoodenStakeDurabilityDamage(Player attacker, ItemStack woodenSword) {
        if (woodenSword.getItemMeta() instanceof Damageable) {
            Damageable damageable = (Damageable)woodenSword.getItemMeta();
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

    private void applyVampireClawEffects(Player attacker, Entity victim, int vampireStage) {
        boolean witherApplied = false;
        if (victim instanceof Player livingVictim) {
            double bleedingChance = (double)0.0F;
            int witherLevel = 0;
            if (vampireStage == 2) {
                bleedingChance = 0.33;
                witherLevel = 0;
            } else if (vampireStage == 3) {
                bleedingChance = 0.66;
                witherLevel = 1;
            }

            if (bleedingChance > (double)0.0F && this.random.nextDouble() < bleedingChance) {
                livingVictim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 140, witherLevel, false, false));
                witherApplied = true;
            }
        }

        if (witherApplied) {
            Location particleLocation = victim.getLocation().add((double)0.0F, victim.getHeight() / (double)2.0F, (double)0.0F);
            victim.getWorld().spawnParticle(Particle.DUST, particleLocation, 20, (double)0.5F, 0.3, (double)0.5F, (double)0.0F, new Particle.DustOptions(Color.RED, 1.0F));
            victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, particleLocation, 8, 0.3, 0.2, 0.3, (double)0.0F);
        }

        if (victim instanceof Player humanVictim && this.vampireManager.isHuman((Player)victim)) {
            if (!humanVictim.getScoreboardTags().contains("informed_vampire_claws")) {
                humanVictim.sendMessage("§cThe creatures claws rip your skin open, you are bleeding!");
                humanVictim.addScoreboardTag("informed_vampire_claws");
            }
        }

    }
}
