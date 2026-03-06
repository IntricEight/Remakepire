package frostvein.sampires.remakepire.managers;

import java.util.HashMap;
import java.util.Map;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import frostvein.sampires.remakepire.RemakepirePlugin;

public class PlayerChatManager implements Listener {
    private RemakepirePlugin plugin;
    private final Map<Player, String> pendingMessages = new HashMap<>();

    /**
     * Create an instance of the Player Chat manager.
     *
     * @param plugin the host plugin object.
     */
    public PlayerChatManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Retrieve the held messages.
     *
     * @return A {@code Map} of messages the players have attempted to send.
     */
    public Map<Player, String> getPendingMessages() {
        return this.pendingMessages;
    }

    /**
     * Remove the pending messages sent by the player.
     *
     * @param player the player who has sent messages.
     */
    public void removePlayersPendingMessages(Player player) {
        this.pendingMessages.remove(player);
    }

    /**
     * Queue up messages sent until a player chooses to let them through.
     *
     * @param event a player sends a chat message.
     */
    @EventHandler(
            ignoreCancelled = true
    )
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (this.plugin.getConfigManager().isFirstMessageBlockingEnabled()) {
            if (event.getPlayer().getScoreboardTags().contains("ChatPrevented")) {
                event.setCancelled(true);
                player.getServer().broadcastMessage("<" + player.getName() + "> " + event.getMessage());

            } else {
                event.setCancelled(true);
                String originalMessage = event.getMessage();
                this.pendingMessages.put(player, originalMessage);
                this.sendPreventionMessage(player, originalMessage);
            }
        }
    }

    /**
     * Warn the user about the risks of sending their message, but provide them with a prompt to send it through.
     *
     * @param player the player who sent the message.
     * @param originalMessage the message the player was trying to send.
     */
    private void sendPreventionMessage(Player player, String originalMessage) {
        String configMessage = this.plugin.getConfigManager().getFirstMessageBlockedMessage();
        String translatedMessage = ChatColor.translateAlternateColorCodes('&', configMessage);

        if (translatedMessage.contains("[Click Here]")) {
            String[] parts = translatedMessage.split("\\[Click Here\\]", 2);
            TextComponent message = new TextComponent("\n" + parts[0]);
            TextComponent clickHere = new TextComponent(String.valueOf(ChatColor.AQUA) + "[Click Here]");
            clickHere.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/pow sendmessage"));
            clickHere.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, (new ComponentBuilder(String.valueOf(ChatColor.GREEN) + "Click to send your message: " + String.valueOf(ChatColor.WHITE) + originalMessage)).create()));
            message.addExtra(clickHere);

            if (parts.length > 1) {
                message.addExtra(new TextComponent(parts[1]));
            }

            player.spigot().sendMessage(message);
        } else {
            player.sendMessage("\n" + translatedMessage);
        }
    }

    /**
     * Release the message that had been held back.
     *
     * @param player the player attempting to send a message.
     */
    public void handleSendPendingMessage(Player player) {
        String pendingMessage = this.pendingMessages.get(player);

        if (pendingMessage != null) {
            player.getServer().broadcastMessage("<" + player.getName() + "> " + pendingMessage);
            this.pendingMessages.remove(player);
        }
    }
}
