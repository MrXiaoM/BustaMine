package me.sat7.bustamine.manager;

import me.sat7.bustamine.BustaMine;
import me.sat7.bustamine.data.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static me.sat7.bustamine.BustaMine.log;

public class UserManager implements Listener {
    private final BustaMine plugin;
    private final File file;
    private final Map<UUID, User> users = new HashMap<>();
    public UserManager(BustaMine plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "users.yml");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @NotNull
    public User get(OfflinePlayer player) {
        UUID uuid = player.getUniqueId();
        User user = users.get(uuid);
        if (user != null) {
            return user;
        } else {
            User newUser = new User(player);
            users.put(uuid, newUser);
            return newUser;
        }
    }

    public Collection<User> users() {
        return users.values();
    }

    public void reload() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("users");
        if (section != null) for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;
            UUID uuid = UUID.fromString(key);
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            User user = new User(player);
            user.setCashOut(entry.getInt("CashOut", -1));
            user.setGamesPlayed(entry.getInt("GamesPlayed"));
            user.setNetProfit(entry.getDouble("NetProfit"));
            user.setNetProfitExp(entry.getInt("NetProfit_Exp"));
            user.setLastJoin(entry.getLong("LastJoin"));
            users.put(uuid, user);
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (User user : users.values()) {
            String prefix = "users." + user.getPlayer().getUniqueId() + ".";
            config.set(prefix + "CashOut", user.getCashOut());
            config.set(prefix + "GamesPlayed", user.getGamesPlayed());
            config.set(prefix + "NetProfit", user.getNetProfit());
            config.set(prefix + "NetProfit_Exp", user.getNetProfitExp());
            config.set(prefix + "LastJoin", user.getLastJoin());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            log(e);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        User user = get(e.getPlayer());
        user.setLastJoin(System.currentTimeMillis());
    }
}
