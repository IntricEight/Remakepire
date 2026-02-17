package frostvein.sampires.remakepire.abilities.tome;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class TurnUndeadTomeAbility extends TomeAbility {
    private static final int EFFECT_DURATION = 300;

    public TurnUndeadTomeAbility(RemakepirePlugin plugin) {
        super(plugin, "TurnUndead", new String[]{"You die. Temporarily.", "For the duration of this ability, undead mobs see you as one of their own,", "and do not attack you."}, plugin.getConfigManager().getTomeTurnUndeadCooldown());
    }

    protected boolean useAbility(Player player) {
        if (!this.canUse(player)) {
            this.sendCannotUseMessage(player, "Only humans can use tome abilities!");
            return false;
        } else {
            Team vampireCastTeam = this.plugin.getVampireCastTeam();
            if (vampireCastTeam == null) {
                this.sendCannotUseMessage(player, "VampireCastTeam is not available!");
                return false;
            } else {
                vampireCastTeam.addEntry(player.getName());
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 200, 0, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1, false, false));
                this.plugin.getWorld().playSound(player.getLocation(), "minecraft:ambient.warped_forest.mood", 1.0F, 1.0F);
                this.sendSuccessMessage(player, "You feel the cold embrace of death wash over you...");
                player.sendMessage("§7Undead creatures now see you as one of their own.");
                player.sendMessage("§8This effect will last for 5 minutes.");
                Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                    if (player.isOnline()) {
                        this.removeFromVampireCastTeam(player);
                        player.sendMessage("§aThe deathly aura fades... You feel alive once more.");
                        player.sendMessage("§7Undead creatures will now see you as a threat again.");
                        this.plugin.getWorld().playSound(player.getLocation(), "minecraft:block.beacon.activate", 1.0F, 1.2F);
                    } else {
                        vampireCastTeam.removeEntry(player.getName());
                    }

                }, 6000L);
                return true;
            }
        }
    }

    private void removeFromVampireCastTeam(Player player) {
        Team vampireCastTeam = this.plugin.getVampireCastTeam();
        Team regularCastTeam = this.plugin.getCastTeam();
        if (vampireCastTeam != null) {
            vampireCastTeam.removeEntry(player.getName());
        }

        if (regularCastTeam != null) {
            regularCastTeam.addEntry(player.getName());
        }

    }

    public static void cleanupHumanOnVampireCastTeam(RemakepirePlugin plugin, Player player) {
        if (plugin.getVampireManager().isHuman(player)) {
            Team vampireCastTeam = plugin.getVampireCastTeam();
            Team regularCastTeam = plugin.getCastTeam();
            if (vampireCastTeam != null && vampireCastTeam.hasEntry(player.getName())) {
                vampireCastTeam.removeEntry(player.getName());
                if (regularCastTeam != null) {
                    regularCastTeam.addEntry(player.getName());
                }

                player.sendMessage("§7Your undead disguise has faded during your absence.");
                plugin.getLogger().info("Moved human player " + player.getName() + " from VampireCastTeam to CastTeam (login cleanup)");
            }

        }
    }
}
