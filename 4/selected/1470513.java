package me.nareshkumarrao.IRCraft;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;

public class EventPlayerListener extends PlayerListener {

    public static IRCraft plugin;

    private void sendAllMessage(Player player, String message) {
        for (int i = 0; i < plugin.bot.getChannels().length; i++) {
            plugin.bot.sendMessage(plugin.bot.getChannels()[i], "<" + player.getDisplayName() + ">" + message);
        }
    }

    public EventPlayerListener(IRCraft instance) {
        plugin = instance;
    }

    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        sendAllMessage(player, " " + event.getMessage());
    }

    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        sendAllMessage(player, " has joined the server.");
    }

    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        sendAllMessage(player, " is now in his bed.");
    }

    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        Player player = event.getPlayer();
        sendAllMessage(player, " has left his bed.");
    }

    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        sendAllMessage(player, " has quit the game.");
    }
}
