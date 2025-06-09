package me.sat7.bustamine.manager.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

import static me.sat7.bustamine.config.Messages.msg;

public interface IBustaMineGui extends InventoryHolder {
    static void closeAllGameUI() {
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof IBustaMineGui) {
                msg(p, "Game was terminated by server");
                p.closeInventory();
            }
        }
    }
}
