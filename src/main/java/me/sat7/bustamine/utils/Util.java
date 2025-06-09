package me.sat7.bustamine.utils;

import de.tr7zw.changeme.nbtapi.NBT;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.*;

public class Util {
    public static final DecimalFormat integerFormat = new DecimalFormat("0");
    public static final DecimalFormat doubleFormat = new DecimalFormat("0.00");

    private static final Random generator = new Random();
    private static final double[] oddList = new double[150];

    public static void init() {
        // calcOdds
        for (int i = 1; i <= 150; i++) {
            oddList[i - 1] = 1 / Math.pow(i, 1.01);
        }
    }

    public static double odd(int index) {
        return oddList[index];
    }

    public static int genBustNum(double baseInstaBust, int maxMulti) {
        double randD = generator.nextDouble();

        if (randD < baseInstaBust) return 100;

        randD = generator.nextDouble();

        for (int j = maxMulti; j > 0; j--) {
            if (randD < oddList[j - 1]) {
                if (j == maxMulti) return 100;

                double temp = generator.nextDouble();
                if (j <= 3 && generator.nextBoolean()) {
                    temp *= 0.6;
                    if (generator.nextBoolean()) temp *= 0.4;
                }

                int tempInt = (int) ((j + temp) * 100);
                if (tempInt == 100) tempInt = 101;
                return tempInt;
            }
        }

        return 101;
    }

    public static ItemStack getGlassItem(int dataValue) {
        ItemStack glass = new ItemStack(getGlass(dataValue));
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        return glass;
    }

    public static Material getGlass(int dataValue) {
        switch (dataValue) {
            case 0:
                return Material.WHITE_STAINED_GLASS_PANE;
            case 1:
                return Material.ORANGE_STAINED_GLASS_PANE;
            case 2:
                return Material.MAGENTA_STAINED_GLASS_PANE;
            case 3:
                return Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case 4:
                return Material.YELLOW_STAINED_GLASS_PANE;
            case 5:
                return Material.LIME_STAINED_GLASS_PANE;
            case 6:
                return Material.PINK_STAINED_GLASS_PANE;
            case 7:
                return Material.GRAY_STAINED_GLASS_PANE;
            case 8:
                return Material.LIGHT_GRAY_STAINED_GLASS_PANE;
            case 9:
                return Material.CYAN_STAINED_GLASS_PANE;
            case 10:
                return Material.PURPLE_STAINED_GLASS_PANE;
            case 11:
                return Material.BLUE_STAINED_GLASS_PANE;
            case 12:
                return Material.BROWN_STAINED_GLASS_PANE;
            case 13:
                return Material.GREEN_STAINED_GLASS_PANE;
            case 14:
                return Material.RED_STAINED_GLASS_PANE;
            case 15:
                return Material.BLACK_STAINED_GLASS_PANE;
            default:
                return Material.GLASS_PANE;
        }
    }

    public static ItemStack createItemStack(Property<Item> item, ItemMeta _meta, String name, List<String> lore, int amount) {
        return createItemStack(item.val(), _meta, name, lore, amount);
    }
    public static ItemStack createItemStack(Item item, ItemMeta _meta, String name, List<String> lore, int amount) {
        return createItemStack(item.newItem(amount), _meta, name, lore);
    }
    public static ItemStack createItemStack(Material material, ItemMeta _meta, String name, List<String> lore, int amount) {
        return createItemStack(new ItemStack(material, amount), _meta, name, lore);
    }
    public static ItemStack createItemStack(ItemStack baseItem, ItemMeta _meta, String name, List<String> lore) {
        ItemMeta meta = _meta;
        if (_meta == null) meta = baseItem.getItemMeta();
        if (meta != null) {
            if (!name.isEmpty()) meta.setDisplayName(name);
            meta.setLore(lore);
            baseItem.setItemMeta(meta);
        }
        return baseItem;
    }

    public static int calcTotalExp(Player p) {
        int lv = p.getLevel();
        int sub = (int) (p.getExp() * p.getExpToLevel());
        double temp;

        if (lv <= 16) {
            temp = Math.pow(lv, 2) + (6 * lv);
            return (int) (temp + sub);
        } else if (lv <= 31) {
            temp = (2.5 * Math.pow(lv, 2)) - (40.5 * lv) + 360;
            return (int) (temp + sub);
        } else {
            temp = (4.5 * Math.pow(lv, 2)) - (162.5 * lv) + 2220;
            return (int) (temp + sub);
        }
    }

    public static <K, V extends Comparable<? super V>> HashMap<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());
        Collections.reverse(list);

        HashMap<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void updateInventory(Player player) {
        player.updateInventory();
    }

    public static Optional<Integer> parseInt(String str) {
        if (str == null || str.isEmpty()) return Optional.empty();
        try {
            return Optional.of(Integer.parseInt(str));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static Material getMaterial(String str, Material def) {
        String upper = str.toUpperCase();
        Material material = Material.getMaterial(upper);
        if (material != null) {
            return material;
        }
        return def;
    }

    private static final String FLAG = "BustaMine_Icon";
    public static String flag(ItemStack item) {
        return NBT.get(item, nbt -> nbt.hasTag(FLAG) ? nbt.getString(FLAG) : "");
    }
    public static void flag(ItemStack item, String flag) {
        NBT.modify(item, nbt -> {
            nbt.setString(FLAG, flag);
        });
    }
}
