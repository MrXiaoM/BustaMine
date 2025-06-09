package me.sat7.bustamine.manager.gui;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class BetGuiHolder implements IBustaMineGui {
    private final Inventory inventory;
    public BetGuiHolder(int size, String title) {
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
