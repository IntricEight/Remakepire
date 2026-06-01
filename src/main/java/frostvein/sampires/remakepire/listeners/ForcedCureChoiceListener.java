package frostvein.sampires.remakepire.listeners;

import frostvein.sampires.remakepire.commands.ForcedVampireCureCommand;
import frostvein.sampires.remakepire.managers.VampireAbilityManager;
import frostvein.sampires.remakepire.managers.VampireFeedingManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import frostvein.sampires.remakepire.RemakepirePlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.units.qual.C;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ForcedCureChoiceListener implements Listener {
    private final RemakepirePlugin plugin;

    private final Map<UUID, CureSession> activeForcedCureSessions = new HashMap<>();
    private final int cureSeconds;

    /**
     * Create an instance of the Forced Cure Choice listener.
     *
     * @param plugin the host plugin object.
     */
    public ForcedCureChoiceListener(RemakepirePlugin plugin) {
        this.plugin = plugin;

        this.cureSeconds = this.plugin.getConfigManager().getCureApplicationSeconds();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.startCureDetectionTask();
    }

    /**
     * Control interactions with the cure choice UI.
     *
     * @param event a player clicks inside an inventory menu.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        event.getView().getTitle();

        if (event.getView().getTitle().equals("§4§lYour Fate Awaits...")) {
            event.setCancelled(true);

            if (event.getWhoClicked() instanceof Player player) {
                if (!this.plugin.getForcedCureChoiceManager().hasPendingCure(player)) {
                    player.closeInventory();
                } else {
                    ItemStack clickedItem = event.getCurrentItem();

                    if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                        if (clickedItem.getType() == Material.PLAYER_HEAD) {
                            this.plugin.getForcedCureChoiceManager().handleHumanityChoice(player);
                        } else if (clickedItem.getType() == Material.SKELETON_SKULL) {
                            this.plugin.getForcedCureChoiceManager().handleDeathChoice(player);
                        }
                    }
                }
            }
        }
    }

    /**
     * Give the player the option to reopen the forced cure choice window.
     *
     * @param event a player closes an inventory window.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        event.getView().getTitle();

        if (event.getView().getTitle().equals("§4§lYour Fate Awaits...")) {
            if (event.getPlayer() instanceof Player player) {
                if (this.plugin.getForcedCureChoiceManager().hasPendingCure(player)) {
                    player.sendMessage("");
                    player.sendMessage("§4This is a decision you cannot run from monster.");
                    player.sendMessage("§7The spirits have come knocking, and they are joined by death.");
                    player.sendMessage("§7Say your peace, and when you ready to make your decision,");

                    TextComponent message = new TextComponent("§e§l[CLICK HERE]");
                    message.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/pow reopen"));
                    message.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder("§6Click to reopen the choice menu")).create()));

                    player.spigot().sendMessage(message);
                    player.sendMessage("");
                }
            }
        }
    }

    /**
     * Give the player the option to reopen the forced cure choice window when they rejoin the game.
     *
     * @param event a player joining the world.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (this.plugin.getForcedCureChoiceManager().hasPendingCure(player)) {
            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
                if (player.isOnline() && this.plugin.getForcedCureChoiceManager().hasPendingCure(player)) {
                    player.sendMessage("");
                    player.sendMessage("§4This is a decision you cannot run from monster.");
                    player.sendMessage("§7The spirits have come knocking, and they are joined by death.");
                    player.sendMessage("§7Say your peace, and when you ready to make your decision,");

                    TextComponent message = new TextComponent("§e§l[CLICK HERE]");
                    message.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/pow reopen"));
                    message.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder("§6Click to reopen the choice menu")).create()));
                    player.spigot().sendMessage(message);
                    player.sendMessage("");
                }
            }, 20L);
        }
    }

    /**
     * Prevent the player from moving once a force cure has been used on them until a choice is maded.
     *
     * @param event a player moving.
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (this.plugin.getForcedCureChoiceManager().hasPendingCure(player) && (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getY() != event.getTo().getY() || event.getFrom().getZ() != event.getTo().getZ())) {
            event.setCancelled(true);
        }
    }

    /**
     * Check if a vampire is attempting to feed on another player whenever a vampire sneaks.
     *
     * @param event a player beginning or stopping to sneak.
     */
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        CureSession session = this.activeForcedCureSessions.get(player.getUniqueId());

        if (!event.isSneaking() && session != null) {
            this.cancelCureSession(session);
        }
    }

    /**
     * Begin checking if a player is attempting to force cure another player.
     */
    private void startCureDetectionTask() {
        (new BukkitRunnable() {
            public void run() {
                ForcedCureChoiceListener.this.checkCureSessions();
            }
        }).runTaskTimer(this.plugin, 20L, 20L);
    }

    /**
     * Begin the process of force curing a player
     *
     * @param healer the player curing.
     * @param target the player about to be cured.
     */
    public void startCureSession(Player healer, Player target) {
        CureSession session = new CureSession(healer.getUniqueId(), target.getUniqueId());

        this.activeForcedCureSessions.put(healer.getUniqueId(), session);
    }

    /**
     * Manage the forced cure attempts of players
     */
    private void checkCureSessions() {
        for (CureSession session : this.activeForcedCureSessions.values().toArray(new CureSession[0])) {
            Player healer = Bukkit.getPlayer(session.healerId), target = Bukkit.getPlayer(session.vampireId);

            if (healer != null && target != null &&
                    healer.isOnline() && target.isOnline() &&
                    healer.getGameMode() != GameMode.SPECTATOR && target.getGameMode() != GameMode.SPECTATOR) {

                if (!this.plugin.getVampireManager().isVampire(target)) {
                    // Do another check that the target is a vampire
                    healer.sendMessage("§cThe target must be a vampire to be cured.");

                } else if (!this.plugin.getSessionManager().isSessionActive()) {
                    // Make sure no curing is happening outside of session
                    this.cancelCureSession(session);

                } else if (healer.isSneaking() && this.isInCureRange(healer, target)) {
                    // Implement timer feature here?
                    // Follow the processPreparationPhase logic
                    this.processPreparationPhase(session, healer, target);
                } else {
                    this.cancelCureSession(session);
                }
            } else {
                this.cancelCureSession(session);
            }
        }
    }

    /**
     * Manage the countdown until the forced cure is enacted upon the target.
     *
     * @param session the cure session.
     * @param healer the player curing.
     * @param target the player about to be cured.
     */
    private void processPreparationPhase(CureSession session, Player healer, Player target) {
        --session.preparationSecondsRemaining;
        String preparationMessage = "§8Preparing to inject... " + VampireAbilityManager.formatTime(session.preparationSecondsRemaining) + " remaining";
        this.plugin.getSessionManager().sendActionBar(healer, preparationMessage);

        // Alert the players that the cure has begun.
        if (session.preparationSecondsRemaining <= 0) {
            // Reset the action bar of the healing player
            this.plugin.getSessionManager().sendActionBar(healer, "");

            // Force cure the targeted vampire
            this.plugin.getForcedCureChoiceManager().executeForceVampireCure(healer, target);
        }
    }

    /**
     * Stop the vampire feeding process.
     *
     * @param session the blood feeding session.
     */
    private void cancelCureSession(CureSession session) {
        Player healer = Bukkit.getPlayer(session.healerId), target = Bukkit.getPlayer(session.vampireId);

        if (target != null && target.isOnline()) {
            target.sendMessage("§aYou no longer feel a vampire draining your life force");
        }

        this.activeForcedCureSessions.remove(session.healerId);
    }

    /**
     * Determine if the target is in curing range of the healer.
     *
     * @param healer the player using the cure.
     * @param target the player who might be cured.
     * @return {@code true} if the healer is close enough to the target.
     */
    private boolean isInCureRange(Player healer, Player target) {
        if (!healer.getWorld().equals(target.getWorld())) {
            return false;
        } else {
            return healer.getLocation().distance(target.getLocation()) <= 3;
        }
    }





    private class CureSession {
        public  final UUID healerId, vampireId;
        public final long startTime;
        public BukkitTask task;
        public int preparationSecondsRemaining;


        public CureSession(UUID healerId, UUID vampireId) {
            this.healerId = healerId;
            this.vampireId = vampireId;
            this.startTime = System.currentTimeMillis();

            this.preparationSecondsRemaining = cureSeconds;
        }
    }
}
