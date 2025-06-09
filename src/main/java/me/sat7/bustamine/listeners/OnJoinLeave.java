package me.sat7.bustamine.listeners;

import me.sat7.bustamine.BustaMine;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class OnJoinLeave implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        BustaMine.ccUser.get().set(e.getPlayer().getUniqueId() + ".LastJoin", System.currentTimeMillis());
    }
}
