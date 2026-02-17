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
    private static final int FIRE_RADIUS = 6;
    private static final int FIRE_INNER_RADIUS = 2;
    private static final int FIRE_RESISTANCE_DURATION = 6000;
    private static final int FIRE_RESISTANCE_AMPLIFIER = 0;

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
            double playerYaw = Math.toRadians((double)(playerLoc.getYaw() + 90.0F));
            List<Location> fireLocations = this.calculateFireLocations(playerLoc);
            this.sortLocationsByAngle(fireLocations, playerLoc, playerYaw);
            player.playSound(player.getLocation(), "minecraft:item.firecharge.use", 1.0F, 1.0F);
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 6000, 0, false, false));
            this.sendSuccessMessage(player, "You thrash your lantern wildly, igniting the ground around you!");
            this.startFireSpread(player, fireLocations);
            return true;
        }
    }

    private List<Location> calculateFireLocations(Location playerLoc) {
        List<Location> locations = new ArrayList();
        int playerX = playerLoc.getBlockX();
        int playerY = playerLoc.getBlockY();
        int playerZ = playerLoc.getBlockZ();

        for(int x = playerX - 6; x <= playerX + 6; ++x) {
            for(int z = playerZ - 6; z <= playerZ + 6; ++z) {
                double distance = Math.sqrt(Math.pow((double)(x - playerX), (double)2.0F) + Math.pow((double)(z - playerZ), (double)2.0F));
                if (distance <= (double)6.0F && distance >= (double)2.0F) {
                    for(int yOffset = -1; yOffset <= 0; ++yOffset) {
                        Location blockLocation = new Location(playerLoc.getWorld(), (double)x, (double)(playerY + yOffset), (double)z);
                        Block block = blockLocation.getBlock();
                        Block blockAbove = blockLocation.clone().add((double)0.0F, (double)1.0F, (double)0.0F).getBlock();
                        if (blockAbove.getType() == Material.AIR && block.getType() != Material.AIR) {
                            locations.add(blockAbove.getLocation());
                        }
                    }
                }
            }
        }

        return locations;
    }

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

    private double normalizeAngleRelativeToStart(double angle, double startAngle) {
        double relativeAngle;
        for(relativeAngle = angle - startAngle; relativeAngle < (double)0.0F; relativeAngle += (Math.PI * 2D)) {
        }

        while(relativeAngle >= (Math.PI * 2D)) {
            relativeAngle -= (Math.PI * 2D);
        }

        return relativeAngle;
    }

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

    private boolean hasLanternInInventory(Player player) {
        for(ItemStack item : player.getInventory().getContents()) {
            if (item != null && this.isLantern(item.getType())) {
                return true;
            }
        }

        return false;
    }

    private boolean isLantern(Material material) {
        return material == Material.LANTERN;
    }
}
