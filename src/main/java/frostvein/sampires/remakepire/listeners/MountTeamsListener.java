package frostvein.sampires.remakepire.listeners;

import java.util.Set;
import org.bukkit.entity.Camel;
import org.bukkit.entity.Donkey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Mule;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.TraderLlama;
import org.bukkit.entity.ZombieHorse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityMountEvent;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.VampireManager;

public class MountTeamsListener implements Listener {
    private final RemakepirePlugin plugin;
    private final VampireManager vampireManager;
    // List all the entities that are included in the alignment checks
    private final Set<Class<? extends Entity>> livingMountTypes = Set.of(Pig.class, Horse.class, Donkey.class, Mule.class, Llama.class, TraderLlama.class, Camel.class);
    private final Set<Class<? extends Entity>> undeadMountTypes = Set.of(SkeletonHorse.class, ZombieHorse.class);

    /**
     * Create an instance of the Mount Teams listener.
     *
     * @param plugin the host plugin object.
     */
    public MountTeamsListener(RemakepirePlugin plugin) {
        this.plugin = plugin;
        this.vampireManager = plugin.getVampireManager();
    }

    /**
     * Prevent higher vampires from mounting living animals, and prevent humans from mounting undead animals.<br/>
     * Stage 1 vampires can ride any mount.
     *
     * @param event an entity is being mounted.
     */
    @EventHandler
    public void onEntityMount(EntityMountEvent event) {
        // Only run these checks if the setting for mount team alignments is active (setting for allowing is disabled)
        if (this.plugin.getConfigManager().canVampiresRideLivingMounts()) {
            return;
        }

        // Only proceed if a player is attempting to mount an entity
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Entity mount = event.getMount();

        // Prevent humans from riding undead mounts and higher vampires from riding living mounts
        if (this.vampireManager.isHuman(player) && this.isUndeadMount(mount)) {
            event.setCancelled(true);
            player.sendMessage("§cThe animal recoils from your warm touch...");

        } else if (this.vampireManager.isVampireStage2OrHigher(player) && this.isLivingMount(mount)) {
            event.setCancelled(true);
            player.sendMessage("§cThe animal recoils from you as you extend a hand to it...");
        }
    }

    /**
     * Determine if an entity is in the list of living mounts.
     *
     * @param entity the mount being checked.
     * @return {@code true} if th entity is inside the set.
     */
    private boolean isLivingMount(Entity entity) {
        // Determine if the entity is a living mountable mob
        for(Class<? extends Entity> mobType : this.livingMountTypes) {
            if (mobType.isInstance(entity)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determine if an entity is in the list of undead mounts.
     *
     * @param entity the mount being checked.
     * @return {@code true} if th entity is inside the set.
     */
    private boolean isUndeadMount(Entity entity) {
        // Determine if the entity is an undead mountable mob
        for(Class<? extends Entity> mobType : this.undeadMountTypes) {
            if (mobType.isInstance(entity)) {
                return true;
            }
        }

        return false;
    }
}
