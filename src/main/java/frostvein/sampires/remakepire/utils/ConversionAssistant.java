package frostvein.sampires.remakepire.utils;

import org.bukkit.Location;

public class ConversionAssistant {
    /**
     * Create an instance of the Conversion tool that contains conversion and calculation tools used by multiple classes around the project.
     */
    public ConversionAssistant() {}

    /**
     * Determine the direction between the player and a target set of coordinates.
     *
     * @param deltaX the difference between the player's X coordinate and the target's.
     * @param deltaZ the difference between the player's Y coordinate and the target's.
     * @param playerYaw the angle that the player is looking.
     * @return A Unicode value of the direction.
     */
    public String getRelativeDirection(double deltaX, double deltaZ, float playerYaw) {
        double targetAngle = Math.atan2(deltaX, -deltaZ);
        double targetDegrees = Math.toDegrees(targetAngle);

        // Normalize the degrees
        if (targetDegrees < 0) {
            targetDegrees += 360;
        }

        double playerFacing = (playerYaw + 180.0F) % 360.0F;

        if (playerFacing < 0) {
            playerFacing += 360;
        }

        double relativeAngle = (targetDegrees - playerFacing + 360) % 360.0;

        if (!(relativeAngle >= 337.5) && !(relativeAngle < 22.5)) {
            if (relativeAngle >= 22.5 && relativeAngle < 67.5) {
                return "\ue00b";
            } else if (relativeAngle >= 67.5 && relativeAngle < 112.5) {
                return "\ue00c";
            } else if (relativeAngle >= 112.5 && relativeAngle < 157.5) {
                return "\ue00d";
            } else if (relativeAngle >= 157.5 && relativeAngle < 202.5) {
                return "\ue00e";
            } else if (relativeAngle >= 202.5 && relativeAngle < 247.5) {
                return "\ue00f";
            } else if (relativeAngle >= 247.5 && relativeAngle < 292.5) {
                return "\ue010";
            } else {
                return relativeAngle >= 292.5 && relativeAngle < 337.5 ? "\ue011" : "\ue00a";
            }
        } else {
            return "\ue00a";
        }
    }

    /**
     * Convert a {@code Location} into a {@code String} format.
     *
     * @param location a location to convert.
     * @return A {@code String} of the location's coordinates.
     */
    public String locationToString(Location location) {
        return location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

}
