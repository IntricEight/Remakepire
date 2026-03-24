package frostvein.sampires.remakepire.abilities.tome;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class LanternThrashTomeAbility extends TomeAbility {
    // Controls the maximum size of the ability (in blocks)
    private static final int FIRE_OUTER_RADIUS = 6;
    // Controls the minimum size of the ability (in blocks)
    private static final int FIRE_INNER_RADIUS = 2;
    // Controls the duration the caster is immune to the ability (in ticks)
    private static final int FIRE_RESISTANCE_DURATION = 6000;
    // Controls the intensity of the caster's immunity to the ability
    private static final int FIRE_RESISTANCE_AMPLIFIER = 0;

    /**
     * Create an instance of the Lantern Thrash tome ability.
     *
     * @param plugin the host plugin object.
     */
    public LanternThrashTomeAbility(RemakepirePlugin plugin) {
        super(plugin, "LanternThrash", new String[]{"You learn how to thrash your lantern around you in a wide circle,", "igniting the ground beneath you,", "while coating yourself in a flame resistant ichor."}, plugin.getConfigManager().getTomeLanternThrashCooldown());
    }

    protected boolean useAbility(Player player) {
        if (!this.canUse(player)) {
            this.sendCannotUseMessage(player, "Only humans can use tome abilities!");
            return false;

        } else if (!this.hasLanternInInventory(player)) {
            this.sendCannotUseMessage(player, "You need a regular lantern in your inventory to thrash!");
            return false;

        } else {
            Location playerLoc = player.getLocation();
            double playerYaw = Math.toRadians((playerLoc.getYaw() + 90.0F));

            List<Location> fireLocations = this.calculateFireLocations(playerLoc);
            this.sortLocationsByAngle(fireLocations, playerLoc, playerYaw);

            player.playSound(player.getLocation(), "minecraft:item.firecharge.use", 1.0F, 1.0F);
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, FIRE_RESISTANCE_DURATION, FIRE_RESISTANCE_AMPLIFIER, false, false));
            this.sendSuccessMessage(player, "You thrash your lantern wildly, igniting the ground around you!");
            this.startFireSpread(player, fireLocations);

            return true;
        }
    }

    /**
     * Determine where the ability's fire should be spawned.
     *
     * @param playerLoc the player's casting location.
     * @return a {@code List} of block {@code Location}s that will be set on fire.
     */
    private List<Location> calculateFireLocations(Location playerLoc) {
        List<Location> locations = new ArrayList<>();
        int playerX = playerLoc.getBlockX();
        int playerY = playerLoc.getBlockY();
        int playerZ = playerLoc.getBlockZ();

        // Use the FIRE_INNER_RADIUS and FIRE_OUTER_RADIUS values to create a hollow ring of fire around the caster
        for(int x = playerX - FIRE_OUTER_RADIUS; x <= playerX + FIRE_OUTER_RADIUS; ++x) {
            for(int z = playerZ - FIRE_OUTER_RADIUS; z <= playerZ + FIRE_OUTER_RADIUS; ++z) {
                double distance = Math.sqrt(Math.pow((x - playerX), FIRE_INNER_RADIUS) + Math.pow((z - playerZ), FIRE_INNER_RADIUS));

                // Prevent the creation of fire outside the ability ring
                if (distance <= FIRE_OUTER_RADIUS && distance >= FIRE_INNER_RADIUS) {
                    for(int yOffset = -1; yOffset <= 0; ++yOffset) {
                        Location blockLocation = new Location(playerLoc.getWorld(), x, playerY + yOffset, z);
                        Block block = blockLocation.getBlock();
                        Block blockAbove = blockLocation.clone().add(0.0, 1.0, 0.0).getBlock();

                        if (blockAbove.getType() == Material.AIR && block.getType() != Material.AIR) {
                            locations.add(blockAbove.getLocation());
                        }
                    }
                }
            }
        }

        return locations;
    }

    /**
     * Organize the fire locations by their angle.
     *
     * @param locations the list of fire locations.
     * @param playerLoc the player's casting location.
     * @param startAngle the pivot angle to sort with.
     */
    private void sortLocationsByAngle(List<Location> locations, Location playerLoc, double startAngle) {
        locations.sort((loc1, loc2) -> {
            double angle1 = Math.atan2(loc1.getZ() - playerLoc.getZ(), loc1.getX() - playerLoc.getX());
            double angle2 = Math.atan2(loc2.getZ() - playerLoc.getZ(), loc2.getX() - playerLoc.getX());
            angle1 = this.normalizeAngleRelativeToStart(angle1, startAngle);
            angle2 = this.normalizeAngleRelativeToStart(angle2, startAngle);

            int angleComparison = Double.compare(angle1, angle2);
            if (angleComparison != 0) {
                return angleComparison;
            } else {
                double dist1 = playerLoc.distance(loc1);
                double dist2 = playerLoc.distance(loc2);
                return Double.compare(dist1, dist2);
            }
        });
    }

    /**
     * Normalize an angle value using a pivot angle.
     *
     * @param angle the current angle.
     * @param startAngle the pivot angle to normalize around.
     * @return A normalized {@code Double} angle.
     */
    private double normalizeAngleRelativeToStart(double angle, double startAngle) {
        double normalizedAngle;

        for(normalizedAngle = angle - startAngle; normalizedAngle < 0; normalizedAngle += (Math.PI * 2D)) {}

        while(normalizedAngle >= (Math.PI * 2D)) {
            normalizedAngle -= (Math.PI * 2D);
        }

        return normalizedAngle;
    }

    /**
     * Create a ring of fire around the player who cast the ability.
     *
     * @param player the player who cast the ability.
     * @param fireLocations the locations where fire will be created.
     */
    private void startFireSpread(final Player player, final List<Location> fireLocations) {
        if (!fireLocations.isEmpty()) {
            final int totalTicks = 20;

            (new BukkitRunnable() {
                int currentIndex = 0;
                int tickCounter = 0;

                public void run() {
                    ++this.tickCounter;
                    int totalRemaining = fireLocations.size() - this.currentIndex;
                    int ticksRemaining = totalTicks - this.tickCounter + 1;
                    int locationsThisTick = Math.max(1, (int)Math.ceil((double)totalRemaining / (double)ticksRemaining));

                    for(int locationsSet = 0; this.currentIndex < fireLocations.size() && locationsSet < locationsThisTick; ++locationsSet) {
                        Location fireLoc = (Location)fireLocations.get(this.currentIndex);
                        Block fireBlock = fireLoc.getBlock();

                        if (fireBlock.getType() == Material.AIR) {
                            fireBlock.setType(Material.FIRE);

                            if (this.currentIndex % 8 == 0) {
                                player.playSound(fireLoc, "minecraft:item.flintandsteel.use", 0.3F, 1.2F);
                            }
                        }

                        ++this.currentIndex;
                    }

                    if (this.currentIndex >= fireLocations.size() || this.tickCounter >= totalTicks) {
                        this.cancel();
                    }

                }
            }).runTaskTimer(this.plugin, 1L, 1L);
        }
    }

    /**
     * Determine if the player has a lantern in their inventory.
     *
     * @param player the player who cast the ability.
     * @return {@code true} if the {@code player} has a lantern in their inventory.
     */
    private boolean hasLanternInInventory(Player player) {
        for(ItemStack item : player.getInventory().getContents()) {
            if (item != null && this.isLantern(item.getType())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determine if the item is a lantern.
     *
     * @param material the item being checked.
     * @return {@code text} if this item is a lantern.
     */
    private boolean isLantern(Material material) {
        return material == Material.LANTERN;
    }
}
