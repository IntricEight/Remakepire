package frostvein.sampires.remakepire.abilities;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.Player;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.VampireManager;

public class RaiseUndeadAbility extends VampireAbility {
    public String getName() {
        return "raiseundead";
    }

    public String getDisplayName() {
        return "Raise Undead";
    }

    public String getDescription() {
        return "Reach into your undead lineage and summon forth zombies to your side.";
    }

    public int getCooldownSeconds(RemakepirePlugin plugin) {
        return plugin.getConfigManager().getRaiseUndeadCooldown();
    }

    public int getMinimumStage() {
        return 2;
    }

    public boolean execute(Player player, VampireManager vampireManager, RemakepirePlugin plugin) {
        int summonCount = this.getSummonCount(vampireManager.getVampireStage(player));

        // Spawn the zombies around the caster
        this.spawnZombies(player, summonCount);

        // Assign the mobs to the vampire team after spawning
        plugin.getMobTeamManager().assignMobsNow();

        // Signal the ability's successful usage
        this.createRaiseUndeadEffects(player);
        this.sendRaiseUndeadMessage(player);
        this.playRaiseUndeadSound(player);

        return true;
    }

    /**
     * Retrieve the number of zombies the vampire will raise.
     *
     * @param stage the vampire stage of the ability user.
     * @return the number of spawned zombies.
     */
    private int getSummonCount(int stage) {
        return switch (stage) {
            case 2 -> 2;
            case 3 -> 5;
            default -> 0;
        };
    }

    /**
     * Spawn zombies around the vampire caster.
     *
     * @param player the player using the ability.
     * @param count the number of zombies to spawn (5 or less).
     */
    private void spawnZombies(Player player, int count) {
        World world = player.getWorld();
        Location center = player.getLocation();

        // Create a list of spawn locations around the vampire
        List<Location> spawnLocations = new ArrayList<>();
        spawnLocations.add(center.clone().add(2, 1, 1));
        spawnLocations.add(center.clone().add(-1, 1, 2));
        spawnLocations.add(center.clone().add(-1, 1, -2));
        spawnLocations.add(center.clone().add(-2, 1, -1));
        spawnLocations.add(center.clone().add(2, 1, -1));

        /*
        Z----
        ----Z
        --O--
        Z---Z
        -Z---
         */

        int summonCount = Math.min(count, spawnLocations.size()); // Ensure we don't try to spawn more zombies than we have created locations

        // Attempt to spawn zombies at set locations
        for (int i = 0; i < summonCount; i++) {
            Location spawnLoc = spawnLocations.get(i);

            // If the location is not safe, use the player's location as a fallback
            if (spawnLoc == null) {
                spawnLoc = center.clone().add(0, 1, 0);
            } else {
                // Check if the location is inside a solid block
                Material feet = spawnLoc.getBlock().getType();
                Material head = spawnLoc.clone().add(0, 1, 0).getBlock().getType();

                if (feet.isSolid() || head.isSolid()) {
                    spawnLoc = center.clone().add(0, 1, 0);
                }
            }

            // Spawn a zombie at this location.
            Zombie zombie = world.spawn(spawnLoc, Zombie.class);
            zombie.setCanBreakDoors(false);
            zombie.setCanPickupItems(false);
            zombie.setAdult();
            zombie.clearLootTable();

            // Clear the zombie of any equipment
            zombie.getEquipment().clear();
            zombie.getEquipment().setItemInMainHand(null);
            zombie.getEquipment().setItemInOffHand(null);
        }
    }

    /**
     * Create the particle effects of the ability.
     *
     * @param player the player using the ability.
     */
    private void createRaiseUndeadEffects(Player player) {
        if (player.getWorld() != null) {
            for(int i = 0; i < 50; ++i) {
                double angle = i * 0.3, radius = 2.0;
                double x = Math.cos(angle) * radius;
                double y = Math.sin(angle) * radius;
                double z = i * 0.1;

                player.getWorld().spawnParticle(Particle.COPPER_FIRE_FLAME, player.getLocation().add(x, z + 1.0, y), 1, 0.0, 0.0, 0.0, 0.05);
            }
        }
    }


    /**
     * Inform the player of the ability's success.
     *
     * @param player the player using the ability.
     */
    private void sendRaiseUndeadMessage(Player player) {
        player.sendMessage("§cThe ground around you crumbles as the dead emerge.");
    }

    /**
     * Alert the player of the ability activation with a sound cue.
     *
     * @param player the player using the ability.
     */
    private void playRaiseUndeadSound(Player player) {
        player.playSound(player, Sound.ENTITY_WARDEN_EMERGE, SoundCategory.MASTER, 0.5F, 1.5F);
    }
}
