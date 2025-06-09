package me.sat7.bustamine.manager;

import me.sat7.bustamine.BustaMine;
import me.sat7.bustamine.data.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
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
import static me.sat7.bustamine.config.Messages.*;
import static me.sat7.bustamine.utils.Util.*;

public class UserManager implements Listener {
    private final BustaMine plugin;
    private final File file;
    private final Map<UUID, User> users = new HashMap<>();
    private final Map<String, HashMap<String, Double>> sortedMap = new HashMap<>();
    private final Map<String, Long> sortedTime = new HashMap<>();
    public UserManager(BustaMine plugin) {
        this.plugin = plugin;
        this.file = plugin.resolve("users.yml");
        sortedMap.put("NetProfit", new HashMap<>());
        sortedMap.put("NetProfit_Exp", new HashMap<>());
        sortedMap.put("GamesPlayed", new HashMap<>());
        sortedTime.put("NetProfit", 0L);
        sortedTime.put("NetProfit_Exp", 0L);
        sortedTime.put("GamesPlayed", 0L);
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


    public void showPlayerInfo(Player to, Player data) {
        User user = plugin.users().get(data);
        Message_DivUpper.t(to);
        to.sendMessage("   §6§l" + data.getName());
        to.sendMessage("   §3" + NetProfit.get());
        to.sendMessage("     §3" + plugin.config().currencySymbol + "  " + doubleFormat.format(user.getNetProfit()));
        to.sendMessage("     §3Xp " + user.getNetProfitExp());

        if (to == data) {
            to.sendMessage("   §e" + Bal.get());
            to.sendMessage("     §e" + plugin.config().getString("CurrencySymbol") + "  " + doubleFormat.format(plugin.getEconomy().getBalance(data)));
            to.sendMessage("     §eXp " + calcTotalExp(data));
        }

        to.sendMessage("   §f" + GamesPlayed.get() + ": " + user.getGamesPlayed());
        Message_DivLower.t(to);
    }

    public void showStatistics(Player p) {
        double moneyIn = plugin.bank().getDouble("Statistics.Income.Money");
        double moneyOut = plugin.bank().getDouble("Statistics.Expense.Money");
        int expIn = plugin.bank().getInt("Statistics.Income.Exp");
        int expOut = plugin.bank().getInt("Statistics.Expense.Exp");

        Message_DivUpper.t(p);
        p.sendMessage("   §3" + Income.get());
        p.sendMessage("     §3" + plugin.config().currencySymbol + "  " + doubleFormat.format(moneyIn) + "  Xp " + expIn);
        p.sendMessage("   §3" + Expense.get());
        p.sendMessage("     §3" + plugin.config().currencySymbol + "  " + doubleFormat.format(moneyOut) + "  Xp " + expOut);
        p.sendMessage("   §e" + NetProfit.get());
        p.sendMessage("     §e" + plugin.config().currencySymbol + "  " + doubleFormat.format(moneyIn + moneyOut));
        p.sendMessage("     §eXp " + (expIn + expOut));
        Message_DivLower.t(p);
    }

    public void top(Player p, String type) {
        if (sortedTime.get(type) + (1000L * 60) < System.currentTimeMillis()) {
            sortedMap.get(type).clear();
            for (User user : users()) {
                double value;
                switch (type) {
                    case "NetProfit":
                        value = user.getNetProfit();
                        break;
                    case "NetProfit_Exp":
                        value = user.getNetProfitExp();
                        break;
                    case "GamesPlayed":
                        value = user.getGamesPlayed();
                        break;
                    default:
                        value = 0;
                        break;
                }
                sortedMap.get(type).put(user.getPlayer().getUniqueId().toString(), value);
            }

            sortedMap.put(type, sortByValue(sortedMap.get(type)));
            sortedTime.put(type, System.currentTimeMillis());
        }

        p.sendMessage("§6§l[§e " + Leaderboard.get() + "/" + type + " §6§l]");

        int i = 0;
        for (String s : sortedMap.get(type).keySet()) {
            try {
                OfflinePlayer of = Bukkit.getServer().getOfflinePlayer(UUID.fromString(s));

                String prefix = "  " + (i + 1) + ". " + of.getName() + "   ";
                if (type.equals("NetProfit")) {
                    p.sendMessage(prefix + plugin.config().currencySymbol + doubleFormat.format(sortedMap.get(type).get(s)));
                } else if (type.equals("NetProfit_Exp")) {
                    p.sendMessage(prefix + "Xp" + integerFormat.format(sortedMap.get(type).get(s)));
                } else {
                    p.sendMessage(prefix + integerFormat.format(sortedMap.get(type).get(s)));
                }

                i++;
                if (i >= 10) break;
            } catch (Exception e) {
                msg(p, "Failed to load player data. " + s);
            }
        }

        long seconds = (System.currentTimeMillis() - sortedTime.get(type)) / 1000;
        p.sendMessage("  §7" + Message_LastUpdate.get().replace("{sec}", String.valueOf(seconds)));
        p.sendMessage("");
    }
}
