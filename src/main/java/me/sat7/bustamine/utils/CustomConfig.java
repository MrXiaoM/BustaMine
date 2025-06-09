package me.sat7.bustamine.utils;

import me.sat7.bustamine.BustaMine;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public class CustomConfig {
    protected final BustaMine plugin;
    protected File file;
    protected FileConfiguration customFile;
    public CustomConfig(BustaMine plugin) {
        this.plugin = plugin;
    }

    public void setup(String name, Consumer<FileConfiguration> consumer) {
        file = plugin.resolve(name + ".yml");
        reload();
        consumer.accept(customFile);
    }

    public FileConfiguration get() {
        return customFile;
    }

    public void plusDouble(String key, double amount) {
        customFile.set(key, customFile.getDouble(key) + amount);
    }

    public void plusInteger(String key, int amount) {
        customFile.set(key, customFile.getInt(key) + amount);
    }

    public boolean contains(String key) {
        return customFile.contains(key);
    }

    public String getString(String key) {
        return customFile.getString(key);
    }

    public String getString(String key, String def) {
        return customFile.getString(key, def);
    }

    public Item getItem(String key) {
        return Item.fromString(getString(key), Material.PAPER);
    }

    public int getInt(String key) {
        return customFile.getInt(key);
    }

    public int getInt(String key, int def) {
        return customFile.getInt(key, def);
    }

    public double getDouble(String key) {
        return customFile.getDouble(key);
    }

    public double getDouble(String key, double def) {
        return customFile.getDouble(key, def);
    }

    public boolean getBoolean(String key) {
        return customFile.getBoolean(key);
    }

    public boolean getBoolean(String key, boolean def) {
        return customFile.getBoolean(key, def);
    }

    public void set(String key, Object value) {
        customFile.set(key, value);
    }

    public void rangeInt(String key, Integer min, Integer max) {
        int value = customFile.getInt(key);
        if (min != null && value < min) customFile.set(key, min);
        if (max != null && value > max) customFile.set(key, max);
    }

    public void rangeDouble(String key, Double min, Double max) {
        double value = customFile.getDouble(key);
        if (min != null && value < min) customFile.set(key, min);
        if (max != null && value > max) customFile.set(key, max);
    }

    public void remove(String key) {
        customFile.set(key, null);
    }

    public void save() {
        try {
            customFile.save(file);
        } catch (IOException e) {
            System.out.println("Couldn't save file: " + file.getName());
        }
    }

    public void reload() {
        if (file.exists()) {
            customFile = YamlConfiguration.loadConfiguration(file);
        } else {
            customFile = new YamlConfiguration();
        }
    }
}
