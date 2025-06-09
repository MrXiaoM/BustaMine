package me.sat7.bustamine.listeners;

import me.sat7.bustamine.BustaMine;
import me.sat7.bustamine.data.User;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class OnJoinLeave implements Listener {
    private final BustaMine plugin;
    public OnJoinLeave(BustaMine plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        User user = plugin.getUserManager().get(e.getPlayer());
        user.setLastJoin(System.currentTimeMillis());
    }
}
