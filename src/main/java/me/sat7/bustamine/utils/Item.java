package me.sat7.bustamine.utils;

import org.bukkit.Material;

public class Item {
    private final Material material;
    private final Integer dataValue;
    public Item(Material material, Integer dataValue) {
        this.material = material;
        this.dataValue = dataValue;
    }

    public Material getMaterial() {
        return material;
    }

    public Integer getDataValue() {
        return dataValue;
    }
}
