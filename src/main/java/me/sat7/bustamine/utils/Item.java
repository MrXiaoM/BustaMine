package me.sat7.bustamine.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

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

    @SuppressWarnings({"deprecation"})
    public ItemStack newItem(int amount) {
        Material material = getMaterial();
        Integer dataValue = getDataValue();
        if (dataValue != null) {
            return new ItemStack(material, amount, dataValue.shortValue());
        } else {
            return new ItemStack(material, amount);
        }
    }

    public static Item fromString(String string, Material def) {
        String str;
        Integer dataValue;
        if (string.contains(":")) {
            String[] split = string.split(":", 2);
            str = split[0];
            dataValue = Util.parseInt(split[1]).orElse(null);
        } else {
            str = string;
            dataValue = null;
        }
        Material material = Util.getMaterial(str, def);
        return new Item(material, dataValue);
    }
}
