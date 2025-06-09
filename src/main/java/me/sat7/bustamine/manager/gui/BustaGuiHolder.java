package me.sat7.bustamine.manager.gui;

import me.sat7.bustamine.manager.enums.BustaType;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class BustaGuiHolder implements IBustaMineGui {
    private final BustaType type;
    private final Inventory inventory;
    public BustaGuiHolder(BustaType type, int size, String title) {
        this.type = type;
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    public BustaType getType() {
        return type;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
