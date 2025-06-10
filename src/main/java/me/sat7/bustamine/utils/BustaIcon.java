package me.sat7.bustamine.utils;

import com.google.common.collect.Lists;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BustaIcon {
    private final int slot;
    private final Item material;
    private final int amount;
    private final Integer customModelData;
    private final String display;
    private final List<String> lore;

    public BustaIcon(int slot, Item material, int amount, Integer customModelData, String display, List<String> lore) {
        this.slot = slot;
        this.material = material;
        this.amount = amount;
        this.customModelData = customModelData;
        this.display = display;
        this.lore = lore;
    }

    public int getSlot() {
        return slot;
    }

    public Item getMaterial() {
        return material;
    }

    public ItemStack newItem() {
        return material.newItem(amount);
    }

    public String getDisplay() {
        return display;
    }

    public List<String> getLore() {
        return lore;
    }

    public void set(Inventory inv, List<Pair<String, Object>> replacements, String flag) {
        ItemStack item = generateItem(replacements, flag);
        inv.setItem(slot, item);
    }

    public void set(Inventory inv, List<Pair<String, Object>> replacements, @Nullable List<String> newLore, String flag) {
        ItemStack item = generateItem(replacements, newLore, flag);
        inv.setItem(slot, item);
    }

    public ItemStack generateItem(List<Pair<String, Object>> replacements, String flag) {
        return generateItem(replacements, null, flag);
    }

    public ItemStack generateItem(List<Pair<String, Object>> replacements, @Nullable List<String> newLore, String flag) {
        ItemStack item = material.newItem(amount);
        return generateItem(item, replacements, newLore, flag);
    }

    @SuppressWarnings({"deprecation"})
    public ItemStack generateItem(ItemStack item, List<Pair<String, Object>> replacements, @Nullable List<String> newLore, String flag) {
        if (item.getType().equals(Material.AIR)) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (customModelData != null) {
                try {
                    meta.setCustomModelData(customModelData);
                } catch (Throwable ignored) {
                }
            }
            // TODO: 支持 MiniMessage
            if (display != null) {
                meta.setDisplayName(color(Pair.replace(display, replacements)));
            }
            List<String> lore = newLore != null ? newLore : this.lore;
            if (!lore.isEmpty()) {
                meta.setLore(color(Pair.replace(lore, replacements)));
            }
            item.setItemMeta(meta);
        }
        if (flag != null) {
            Util.flag(item, flag);
        }
        return item;
    }

    public static String color(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    public static List<String> color(List<String> list) {
        list.replaceAll(BustaIcon::color);
        return list;
    }

    public static class Builder {
        private final ConfigurationSection section = new MemoryConfiguration();
        public Builder slot(int slot) {
            section.set("slot", slot);
            return this;
        }
        public Builder material(Material material) {
            section.set("material", material.name().toUpperCase());
            return this;
        }
        public Builder display(String display) {
            section.set("display", display);
            return this;
        }
        public Builder lore(String... lore) {
            section.set("lore", Lists.newArrayList(lore));
            return this;
        }
        public ConfigurationSection build() {
            return section;
        }
    }

    public static Builder def() {
        return new Builder();
    }

    public static Property<BustaIcon> propertyIcon(CustomConfig cfg, String key, Builder def) {
        return new Property<>(cfg, key, property -> {
            CustomConfig config = property.getConfig();
            int slot = config.getInt(key + ".slot");
            Item material = Item.fromString(config.getString(key + ".material"), Material.PAPER);
            String display = config.getString(key + ".display");
            int amount = config.getInt(key + ".amount", 1);
            Integer customModelData = config.contains(key + ".custom-model-data")
                    ? config.getInt(key + ".custom-model-data")
                    : null;
            List<String> lore = config.get().getStringList(key + ".lore");
            return new BustaIcon(slot, material, amount, customModelData, display, lore);
        }, def.build());
    }
}
