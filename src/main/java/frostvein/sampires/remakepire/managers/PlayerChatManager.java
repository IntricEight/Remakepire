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
    private final Map<Player, String> pendingMessages = new HashMap();

    public PlayerChatManager(RemakepirePlugin plugin) {
        this.plugin = plugin;
    }

    public Map<Player, String> getPendingMessages() {
        return this.pendingMessages;
    }

    public void removePlayersPendingMessages(Player player) {
        this.pendingMessages.remove(player);
    }

    @EventHandler(
            ignoreCancelled = true
    )
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (this.plugin.getConfigManager().isFirstMessageBlockingEnabled()) {
            if (event.getPlayer().getScoreboardTags().contains("ChatPrevented")) {
                event.setCancelled(true);
                Server var10000 = player.getServer();
                String var10001 = player.getName();
                var10000.broadcastMessage("<" + var10001 + "> " + event.getMessage());
            } else {
                event.setCancelled(true);
                String originalMessage = event.getMessage();
                this.pendingMessages.put(player, originalMessage);
                this.sendPreventionMessage(player, originalMessage);
            }
        }
    }

    private void sendPreventionMessage(Player player, String originalMessage) {
        String configMessage = this.plugin.getConfigManager().getFirstMessageBlockedMessage();
        String translatedMessage = ChatColor.translateAlternateColorCodes('&', configMessage);
        if (translatedMessage.contains("[Click Here]")) {
            String[] parts = translatedMessage.split("\\[Click Here\\]", 2);
            TextComponent message = new TextComponent("\n" + parts[0]);
            TextComponent clickHere = new TextComponent(String.valueOf(ChatColor.AQUA) + "[Click Here]");
            clickHere.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/pow sendmessage"));
            HoverEvent.Action var10003 = net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT;
            String var10006 = String.valueOf(ChatColor.GREEN);
            clickHere.setHoverEvent(new HoverEvent(var10003, (new ComponentBuilder(var10006 + "Click to send your message: " + String.valueOf(ChatColor.WHITE) + originalMessage)).create()));
            message.addExtra(clickHere);
            if (parts.length > 1) {
                message.addExtra(new TextComponent(parts[1]));
            }

            player.spigot().sendMessage(message);
        } else {
            player.sendMessage("\n" + translatedMessage);
        }

    }

    public void handleSendPendingMessage(Player player) {
        String pendingMessage = (String)this.pendingMessages.get(player);
        if (pendingMessage != null) {
            Server var10000 = player.getServer();
            String var10001 = player.getName();
            var10000.broadcastMessage("<" + var10001 + "> " + pendingMessage);
            this.pendingMessages.remove(player);
        }

    }
}
