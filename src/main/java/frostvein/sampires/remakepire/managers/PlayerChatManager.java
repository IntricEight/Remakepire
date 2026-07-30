package frostvein.sampires.remakepire.managers;

import java.util.HashMap;
import java.util.Map;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
    public void onAsyncPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (this.plugin.getConfigManager().isFirstMessageBlockingEnabled()) {
            if (event.getPlayer().getScoreboardTags().contains("ChatPrevented")) {
                event.setCancelled(true);
                String message = PlainTextComponentSerializer.plainText().serialize(event.message());

                player.getServer().broadcast(Component.text("<" + player.getName() + "> " + message));

            } else {
                event.setCancelled(true);
                String originalMessage = PlainTextComponentSerializer.plainText().serialize(event.message());

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
            Component message = Component.text("\n" + parts[0])
                    .append(Component.text("[Click Here]", NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.runCommand("/pow sendmessage"))
                            .hoverEvent(HoverEvent.showText(Component.text("Click to send your message: ", NamedTextColor.GREEN)
                                    .append(Component.text(originalMessage, NamedTextColor.WHITE))
                            )
                    )
            );

            if (parts.length > 1) {
                message = message.append(Component.text(parts[1]));
            }

            player.sendMessage(message);

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
            player.getServer().broadcast(Component.text("<" + player.getName() + "> " + pendingMessage));
            this.pendingMessages.remove(player);
        }
    }
}
