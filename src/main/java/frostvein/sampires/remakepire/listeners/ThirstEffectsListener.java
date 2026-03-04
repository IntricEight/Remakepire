package frostvein.sampires.remakepire.listeners;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.ThirstManager;
import frostvein.sampires.remakepire.managers.VampireManager;

public class ThirstEffectsListener implements Listener {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    private final ThirstManager thirstManager;

    /**
     * Create an instance of the Thirst Effects listener.
     *
     * @param plugin the host plugin object.
     */
    public ThirstEffectsListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
        this.thirstManager = plugin.getThirstManager();
    }

    public void startTasks() {
        this.startFoodRegenerationTask();
    }

    /**
     * Replace when vampires eat vampiric food with an instant process.
     *
     * @param event a player interacts with an object.
     */
    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (this.vampireManager.isVampire(player) && this.plugin.getSessionManager().isSessionActive()) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                ItemStack item = event.getItem();

                if (item != null && item.getType() != Material.AIR) {
                    if (this.isActualFood(item) && this.isRaw(item)) {
                        this.consumeRawFood(player);
                    }
                }
            }
        }
    }

    /**
     * Apply adverse effects on vampires when they eat human food.
     *
     * @param event a player consumes an item.
     */
    @EventHandler(
            priority = EventPriority.HIGH
    )
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (this.vampireManager.isVampire(player) && this.plugin.getSessionManager().isSessionActive()) {
            if (this.isActualFood(item)) {
                if (!this.isRaw(item)) {
                    int vampireStage = this.vampireManager.getVampireStage(player);
                    this.applyFoodConsumptionEffects(player, vampireStage);
                    this.sendFoodConsumptionMessage(player, vampireStage);
                }
            }
        }
    }

    /**
     * Give vampires blood when they eat valid food items.
     *
     * @param vampire the player consuming the food.
     */
    public void consumeRawFood(Player vampire) {
        ItemStack itemInHand = vampire.getInventory().getItemInMainHand();

        if (itemInHand != null && itemInHand.getType() != Material.AIR && this.isRaw(itemInHand)) {
            if (itemInHand.getAmount() > 1) {
                itemInHand.setAmount(itemInHand.getAmount() - 1);
            } else {
                vampire.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            }

            vampire.playSound(vampire.getLocation(), Sound.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 1.0F, 1.0F);
            this.thirstManager.quenchThirst(vampire, 4);
            this.plugin.getSessionManager().sendActionBar(vampire, "§cThe raw flesh satisfies your alien hunger...");
        }
    }

    /**
     * Determine if an item is raw food.
     *
     * @param item the item being checked.
     * @return {@code true} if the item is raw meat.
     */
    private boolean isRaw(ItemStack item) {
        String itemName = item.getType().name();

        if (itemName.contains("RAW")) {
            return true;
        } else {
            return switch (itemName) {
                case "BEEF", "PORKCHOP", "CHICKEN", "RABBIT", "MUTTON" -> true;
                default -> false;
            };
        }
    }

    /**
     * Determine if the item is a proper food item.
     *
     * @param item the item being checked.
     * @return {@code true} if the item is regular food (Check this function for the list of "not-regular" foods)
     */
    private boolean isActualFood(ItemStack item) {
        Material type = item.getType();

        if (!type.isEdible()) {
            return false;
        } else {
            return !type.name().contains("POTION") && type != Material.ENDER_PEARL && type != Material.CHORUS_FRUIT && type != Material.ENCHANTED_GOLDEN_APPLE && type != Material.GOLDEN_APPLE && type != Material.BEETROOT;
        }
    }

    /**
     * Apply adverse food effects to vampires.
     *
     * @param vampire the player who ate improper food.
     * @param stage the vampire's stage.
     */
    private void applyFoodConsumptionEffects(Player vampire, int stage) {
        int hungerDuration = 200;
        int hungerAmplifier = Math.min(255, 33 * stage);
        vampire.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, hungerDuration, hungerAmplifier, true, false));

        if (stage >= 2) {
            vampire.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 1, true, false));
        }

        if (stage >= 3) {
            vampire.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 0, true, false));
        }
    }

    /**
     * Inform the user that they are eating an improper food item.
     *
     * @param vampire the player who ate improper food
     * @param stage the vampire's stage.
     */
    private void sendFoodConsumptionMessage(Player vampire, int stage) {
        vampire.sendMessage("§cThe food tastes like ash in your mouth, and you struggle not to retch...");
    }

    /**
     * Begin the vampiric regeneration process.
     */
    private void startFoodRegenerationTask() {
        this.scheduleVampireHealthCheck();
    }

    /**
     * Schedule the vampiric process of gaining hunger and saturation from the vampire blood bar.
     */
    private void scheduleVampireHealthCheck() {
        (new BukkitRunnable() {
            public void run() {
                if (!ThirstEffectsListener.this.plugin.getSessionManager().isSessionActive()) {
                    ThirstEffectsListener.this.scheduleVampireHealthCheck();
                } else {
                    for(Player player : ThirstEffectsListener.this.plugin.getServer().getOnlinePlayers()) {
                        if (ThirstEffectsListener.this.vampireManager.isVampire(player)) {
                            ThirstEffectsListener.this.processVampireFoodRegeneration(player);
                        }
                    }

                    ThirstEffectsListener.this.scheduleVampireHealthCheck();
                }
            }
        }).runTaskLater(this.plugin, this.plugin.getSessionManager().getVampireHealthCheckTicks());
    }

    /**
     * Regenerate the food and health of a vampire using their blood bar.
     *
     * @param vampire the player who will regenerate food bars.
     */
    private void processVampireFoodRegeneration(Player vampire) {
        int currentFoodLevel = vampire.getFoodLevel();
        double currentHealth = vampire.getHealth(), maxHealth = vampire.getAttribute(Attribute.MAX_HEALTH).getValue();

        if (currentFoodLevel < 20) {
            this.thirstManager.regenerateFood(vampire);
        } else {
            if (currentFoodLevel >= 20 && currentHealth < maxHealth && !vampire.isDead() && vampire.getHealth() > 0) {
                vampire.setFoodLevel(currentFoodLevel - 1);
                float currentSaturation = vampire.getSaturation();
                vampire.setSaturation(Math.max(0.0F, currentSaturation - 0.5F));
                double newHealth = Math.min(maxHealth, currentHealth + 1.0);
                vampire.setHealth(newHealth);
            }
        }
    }
}
