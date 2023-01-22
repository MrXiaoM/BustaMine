package me.sat7.bustaMine.Events;

import me.sat7.bustaMine.BustaMine;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class OnJoinLeave implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e)
    {
        BustaMine.ccUser.get().set(e.getPlayer().getUniqueId() +".LastJoin", System.currentTimeMillis());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e)
    {

    }
}
