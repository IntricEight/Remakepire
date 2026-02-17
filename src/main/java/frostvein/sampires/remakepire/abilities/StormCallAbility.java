package frostvein.sampires.remakepire.abilities;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.VampireManager;

public class StormCallAbility extends VampireAbility {
    public String getName() {
        return "stormcall";
    }

    public String getDisplayName() {
        return "Call of the Storm";
    }

    public String getDescription() {
        return "Summon dark clouds to shroud the world in snow for 10 minutes. Only very powerful vampires can command the very skies.";
    }

    public int getCooldownSeconds(RemakepirePlugin plugin) {
        return plugin.getConfigManager().getVampireStormCallCooldown();
    }

    public int getMinimumStage() {
        return 3;
    }

    public boolean execute(Player player, VampireManager vampireManager, RemakepirePlugin plugin) {
        World world = player.getWorld();

        if (world.hasStorm()) {
            player.sendMessage("§8The skies are already under your influence...");
            return false;

        } else {
            this.createStormSummonEffects(player);
            this.sendStormCallMessage(player);
            this.playStormCallSound(player);

            world.setStorm(true);
            world.setThundering(false);

            this.broadcastStormArrival(world, player);
            this.scheduleStormClearing(world, player, plugin);

            return true;
        }
    }

    private void createStormSummonEffects(Player player) {
        if (player.getWorld() != null) {
            for(int i = 0; i < 50; ++i) {
                double angle = (double)i * 0.3, radius = 2.0;
                double x = Math.cos(angle) * radius;
                double y = Math.sin(angle) * radius;
                double z = (double)i * 0.1;

                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(x, z + 1.0, y), 1, 0.0, 0.0, 0.0, 0.05);
            }

            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0.0, 3.0, 0.0), 30, 3.0, 1.0, 3.0, 0.1);
            player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0.0, 2.0, 0.0), 40, 2.0, 2.0, 2.0, 0.5);
        }
    }

    private void sendStormCallMessage(Player player) {
        player.sendMessage("§7Rain will fall for the next 10 minutes.");
    }

    private void playStormCallSound(Player player) {
        player.playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.MASTER, 1.0F, 0.8F);
        player.playSound(player, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, SoundCategory.MASTER, 0.8F, 0.6F);
        player.playSound(player, Sound.ITEM_ELYTRA_FLYING, SoundCategory.MASTER, 0.6F, 0.5F);
        player.playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.MASTER, 1.0F, 0.7F);
    }

    private void broadcastStormArrival(World world, Player caster) {
        String message = "§8§lDark clouds gather across the sky...";
        String casterMessage = "§7§o A vampire has called upon an ancient storm...";

        for(Player player : world.getPlayers()) {
            player.sendMessage(message);

            if (!player.equals(caster)) {
                player.sendMessage(casterMessage);
                player.playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 0.3F, 1.2F);
            }
        }

    }

    private void scheduleStormClearing(World world, Player caster, RemakepirePlugin plugin) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (world.hasStorm()) {
                String warningMessage = "§7§oThe clouds are beginning to thin... The storm will pass soon.";

                for (Player worldPlayer : world.getPlayers()) {
                    worldPlayer.sendMessage(warningMessage);
                    worldPlayer.playSound(worldPlayer, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.WEATHER, 0.3F, 0.8F);
                }
            }
        }, 10800L);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (world.hasStorm()) {
                world.setStorm(false);
                world.setThundering(false);
                world.setClearWeatherDuration(6000);

                if (caster.isOnline()) {
                    this.createStormClearingEffects(caster);
                }

                this.broadcastStormClearing(world, caster);
            }
        }, 12000L);
    }

    private void createStormClearingEffects(Player caster) {
        if (caster.getWorld() != null) {
            caster.getWorld().spawnParticle(Particle.END_ROD, caster.getLocation().add(0.0, 1.0, 0.0), 20, 2.0, 3.0, 2.0, 0.1);
            caster.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, caster.getLocation().add(0.0, 1.0, 0.0), 15, 1.5, 1.0, 1.5, 0.1);
        }
    }

    private void broadcastStormClearing(World world, Player player) {
        String message = "§f§lThe storm clouds part, revealing clear skies once more...";
        String casterMessage = "§7§oYour dominion over the weather comes to an end.";

        for(Player worldPlayer : world.getPlayers()) {
            worldPlayer.sendMessage(message);

            if (worldPlayer.equals(player)) {
                worldPlayer.sendMessage(casterMessage);
                worldPlayer.playSound(worldPlayer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0F, 1.5F);
            } else {
                worldPlayer.playSound(worldPlayer, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.WEATHER, 0.4F, 1.8F);
            }
        }

    }
}
