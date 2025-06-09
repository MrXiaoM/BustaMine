package me.sat7.bustamine.config;

import me.sat7.bustamine.BustaMine;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static me.sat7.bustamine.BustaMine.log;

public class Sounds {
    private final BustaMine plugin;
    private final File file;
    private final Map<String, Sound> sounds = new HashMap<>();
    public Sounds(BustaMine plugin) {
        this.plugin = plugin;
        this.file = plugin.resolve("sounds.yml");
    }

    public void reload() {
        if (!file.exists()) {
            plugin.saveResource("sounds.yml", true);
        }
        sounds.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            String str = config.getString(key);
            if (str == null) continue;
            try {
                Sound sound = Sound.valueOf(str);
                sounds.put(key, sound);
            } catch (Throwable ignored) {}
        }
    }

    public void play(Player player, String key) {
        Sound sound = sounds.get(key);
        if (sound == null) return;
        try {
            player.playSound(player.getLocation(), sound, 1, 1);
        } catch (Exception e) {
            if (sounds.containsKey(key)) {
                log("Sound play failed: " + key + "/" + sound);
            } else {
                log("Sound play failed. Path is missing: " + key);
            }
        }
    }
}
