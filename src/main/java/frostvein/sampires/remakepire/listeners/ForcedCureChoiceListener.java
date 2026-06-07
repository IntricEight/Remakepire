package frostvein.sampires.remakepire.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import frostvein.sampires.remakepire.RemakepirePlugin;
import frostvein.sampires.remakepire.managers.VampireAbilityManager;


public class ForcedCureChoiceListener implements Listener {
    private final RemakepirePlugin plugin;
    private final Map<UUID, SelfCureSession> activeSelfCureSessions = new HashMap<>();
    private final Map<UUID, ForceCureSession> activeForcedCureSessions = new HashMap<>();
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
        this.startCureDetectionTasks();
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
     * Check if a player is attempting to execute a force cure whenever a player stops sneaking.
     *
     * @param event a player beginning or stopping to sneak.
     */
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        ForceCureSession session = this.activeForcedCureSessions.get(player.getUniqueId());

        if (!event.isSneaking() && session != null) {
            this.cancelForceCureSession(session, true);
        }
    }

    /**
     * Begin checking if a player is attempting to force cure another player.
     */
    private void startCureDetectionTasks() {
        (new BukkitRunnable() {
            public void run() {
                ForcedCureChoiceListener.this.checkCureSessions();
            }
        }).runTaskTimer(this.plugin, 20L, 20L);
    }

    /**
     * Begin the process of self curing a player of vampirism.
     *
     * @param vampire the player curing themselves.
     */
    public void startSelfCureSession(Player vampire) {
        SelfCureSession session = new SelfCureSession(vampire.getUniqueId());
        this.activeSelfCureSessions.put(vampire.getUniqueId(), session);
    }

    /**
     * Begin the process of force curing a player of vampirism.
     *
     * @param healer the player curing.
     * @param target the player about to be cured.
     */
    public void startForceCureSession(Player healer, Player target) {
        ForceCureSession session = new ForceCureSession(healer.getUniqueId(), target.getUniqueId());
        this.activeForcedCureSessions.put(healer.getUniqueId(), session);
    }

    /**
     * Manage the forced cure attempts of players
     */
    private void checkCureSessions() {
        // Check the condition of all the self cure sessions
        for (SelfCureSession session : this.activeSelfCureSessions.values().toArray(new SelfCureSession[0])) {
            Player healer = Bukkit.getPlayer(session.vampireId);

            if (healer != null && healer.isOnline() && healer.getGameMode() != GameMode.SPECTATOR) {
                if (!this.plugin.getVampireManager().isVampire(healer)) {
                    // Do another check that the target is a vampire
                    healer.sendMessage("§cYou must be a vampire to be cured.");
                    this.cancelSelfCureSession(session, false);

                } else if (!this.plugin.getSessionManager().isSessionActive()) {
                    // Make sure no curing is happening outside of session
                    this.cancelSelfCureSession(session, true);

                } else {
                    // Follow the processPreparationPhase logic
                    this.processPreparationPhaseSelfCure(session, healer);
                }
            } else {
                this.cancelSelfCureSession(session, false);
            }
        }

        // Check the condition of all the force cure sessions
        for (ForceCureSession session : this.activeForcedCureSessions.values().toArray(new ForceCureSession[0])) {
            Player healer = Bukkit.getPlayer(session.healerId), target = Bukkit.getPlayer(session.vampireId);

            if (healer != null && target != null &&
                    healer.isOnline() && target.isOnline() &&
                    healer.getGameMode() != GameMode.SPECTATOR && target.getGameMode() != GameMode.SPECTATOR) {

                if (!this.plugin.getVampireManager().isVampire(target)) {
                    // Do another check that the target is a vampire
                    healer.sendMessage("§cThe target must be a vampire to be cured.");
                    this.cancelForceCureSession(session, false);

                } else if (!this.plugin.getSessionManager().isSessionActive()) {
                    // Make sure no curing is happening outside of session
                    this.cancelForceCureSession(session, true);

                } else if (healer.isSneaking() && this.isInCureRange(healer, target)) {
                    // Follow the processPreparationPhase logic
                    this.processPreparationPhaseForceCure(session, healer, target);
                } else {
                    this.cancelForceCureSession(session, true);
                }
            } else {
                this.cancelForceCureSession(session, false);
            }
        }
    }

    /**
     * Manage the countdown until the cure is enacted upon the vampire.
     *
     * @param session the cure session.
     * @param healer the vampire curing themselves.
     */
    private void processPreparationPhaseSelfCure(SelfCureSession session, Player healer) {
        --session.preparationSecondsRemaining;
        String preparationMessage = "§8Preparing to inject... " + VampireAbilityManager.formatTime(session.preparationSecondsRemaining) + " remaining";
        this.plugin.getSessionManager().sendActionBar(healer, preparationMessage);

        // Check that the player is holding the syringe the entire time
        // Retrieve the prismarine shard in either hand, prioritizing one held in the main hand
        ItemStack mainHandSyringe = healer.getInventory().getItemInMainHand(), offHandSyringe = null;

        if (mainHandSyringe.getType() != Material.PRISMARINE_SHARD) {
            mainHandSyringe = null;

            offHandSyringe = healer.getInventory().getItemInOffHand();

            if (offHandSyringe.getType() != Material.PRISMARINE_SHARD) {
                offHandSyringe = null;
            }
        }

        // Check if the healer is still holding the syringe
        if (mainHandSyringe == null && offHandSyringe == null) {
            healer.sendMessage("§cYou must hold a syringe of your sire's blood to enact the cure.");
            this.cancelSelfCureSession(session,false);
            return;
        }

        // Alert the players that the cure has begun.
        if (session.preparationSecondsRemaining <= 0 && !session.cureCompleted) {
            // Remove the syringe from the hand holding it, prioritizing the main hand
            if (mainHandSyringe == null) {
                // Remove the prismarine stack from the offhand
                healer.getInventory().setItemInOffHand(null);
            } else {
                // Remove the prismarine stack from the main hand
                healer.getInventory().setItemInMainHand(null);
            }

            // Reset the action bar of the cured player
            this.plugin.getSessionManager().sendActionBar(healer, "");

            // Mark that the cure has been completed
            session.cureCompleted = true;
            this.cancelSelfCureSession(session, false);

            // Force cure the player
            this.plugin.getForcedCureChoiceManager().performCure(healer);
        }
    }

    /**
     * Manage the countdown until the forced cure is enacted upon the target.
     *
     * @param session the cure session.
     * @param healer the player curing.
     * @param target the player about to be cured.
     */
    private void processPreparationPhaseForceCure(ForceCureSession session, Player healer, Player target) {
        --session.preparationSecondsRemaining;
        String preparationMessage = "§8Preparing to inject... " + VampireAbilityManager.formatTime(session.preparationSecondsRemaining) + " remaining";
        this.plugin.getSessionManager().sendActionBar(healer, preparationMessage);

        // Check that the player is holding the syringe the entire time
        // Retrieve the prismarine shard in either hand, prioritizing one held in the main hand
        ItemStack mainHandSyringe = null, offHandSyringe = null;

        if (healer.getInventory().getItemInMainHand().getType() == Material.PRISMARINE_SHARD) {
            mainHandSyringe = healer.getInventory().getItemInMainHand();

        } else if (healer.getInventory().getItemInOffHand().getType() == Material.PRISMARINE_SHARD) {
            offHandSyringe = healer.getInventory().getItemInOffHand();
        }

        // Check if the healer is still holding the syringe
        if (mainHandSyringe == null && offHandSyringe == null) {
            healer.sendMessage("§cYou must hold a syringe of the sire's blood to enact the cure.");
            this.cancelForceCureSession(session,false);
            return;
        }

        // Alert the players that the cure has begun.
        if (session.preparationSecondsRemaining <= 0 && !session.choiceOffered) {
            // Remove the syringe from the hand holding it, prioritizing the main hand
            if (mainHandSyringe == null) {
                // Remove the prismarine stack from the offhand
                healer.getInventory().setItemInOffHand(null);
            } else {
                // Remove the prismarine stack from the main hand
                healer.getInventory().setItemInMainHand(null);
            }

            // Reset the action bar of the healing player
            this.plugin.getSessionManager().sendActionBar(healer, "");

            // Mark that the cure choice has been offered
            session.choiceOffered = true;
            this.cancelForceCureSession(session, false);

            // Force cure the targeted vampire
            this.plugin.getForcedCureChoiceManager().executeForceVampireCure(healer, target);
        }
    }

    /**
     * Stop the vampire feeding process.
     *
     * @param session the blood feeding session.
     */
    private void cancelSelfCureSession(SelfCureSession session, boolean sendMessage) {
        Player vampire = Bukkit.getPlayer(session.vampireId);

        if (sendMessage && vampire != null && vampire.isOnline()) {
            vampire.sendMessage("§aYou move the syringe away from your vein.");
        }

        this.activeSelfCureSessions.remove(session.vampireId);
    }

    /**
     * Stop the vampire feeding process.
     *
     * @param session the blood feeding session.
     */
    private void cancelForceCureSession(ForceCureSession session, boolean sendMessage) {
        Player healer = Bukkit.getPlayer(session.healerId), target = Bukkit.getPlayer(session.vampireId);

        if (sendMessage && target != null && target.isOnline()) {
            healer.sendMessage("§aYou move your syringe away from " + target.getName());
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

    private class SelfCureSession {
        public final UUID vampireId;
        public final long startTime;
        public int preparationSecondsRemaining;
        public boolean cureCompleted;

        public SelfCureSession(UUID vampireId) {
            this.vampireId = vampireId;
            this.startTime = System.currentTimeMillis();

            this.preparationSecondsRemaining = cureSeconds;
            this.cureCompleted = false;
        }
    }

    private class ForceCureSession {
        public final UUID healerId, vampireId;
        public final long startTime;
        public int preparationSecondsRemaining;
        public boolean choiceOffered;

        public ForceCureSession(UUID healerId, UUID vampireId) {
            this.healerId = healerId;
            this.vampireId = vampireId;
            this.startTime = System.currentTimeMillis();

            this.preparationSecondsRemaining = cureSeconds;
            this.choiceOffered = false;
        }
    }
}
