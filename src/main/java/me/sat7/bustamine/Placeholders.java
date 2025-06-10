package me.sat7.bustamine;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.sat7.bustamine.manager.UserManager;
import me.sat7.bustamine.utils.Util;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static me.sat7.bustamine.utils.Util.doubleFormat;
import static me.sat7.bustamine.utils.Util.integerFormat;

public class Placeholders extends PlaceholderExpansion {
    private final BustaMine plugin;
    public Placeholders(BustaMine plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean register() {
        try {
            this.unregister();
        } catch (Throwable ignored) {}
        return super.register();
    }

    @Override
    public @NotNull String getIdentifier() {
        return plugin.getDescription().getName().toLowerCase();
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    @SuppressWarnings("IfCanBeSwitch")
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equals("stat_income_money")) {
            double moneyIn = plugin.bank().getDouble("Statistics.Income.Money");
            return doubleFormat.format(moneyIn);
        }
        if (params.equals("stat_expense_money")) {
            double moneyOut = plugin.bank().getDouble("Statistics.Expense.Money");
            return doubleFormat.format(moneyOut);
        }
        if (params.equals("stat_income_exp")) {
            int expIn = plugin.bank().getInt("Statistics.Income.Exp");
            return String.valueOf(expIn);
        }
        if (params.equals("stat_expense_exp")) {
            int expOut = plugin.bank().getInt("Statistics.Expense.Exp");
            return String.valueOf(expOut);
        }
        if (params.equals("stat_net_profit_money")) {
            double moneyIn = plugin.bank().getDouble("Statistics.Income.Money");
            double moneyOut = plugin.bank().getDouble("Statistics.Expense.Money");
            return doubleFormat.format(moneyIn + moneyOut);
        }
        if (params.equals("stat_net_profit_exp")) {
            int expIn = plugin.bank().getInt("Statistics.Income.Exp");
            int expOut = plugin.bank().getInt("Statistics.Expense.Exp");
            return String.valueOf(expIn + expOut);
        }
        if (params.startsWith("top_")) {
            String substring = params.substring(4);
            String[] split = substring.split(substring.contains(";") ? ";" : ":", 3);
            Integer top = split.length == 3 ? Util.parseInt(split[1]).orElse(null) : null;
            if (top != null) {
                String type = split[0];
                String returnType = split[2];
                String realType;
                UserManager manager = plugin.users();
                switch (type) {
                    case "net_profit":
                        realType = "NetProfit";
                        break;
                    case "net_profit_exp":
                        realType = "NetProfit_Exp";
                        break;
                    case "games_played":
                        realType = "GamesPlayed";
                        break;
                    default:
                        realType = null;
                        break;
                }
                if (realType != null) {
                    manager.checkSortMapUpdate(realType);
                    Map<String, Double> sort = manager.getSort(realType);
                    String key = getKeyByIndex(sort, top - 1);
                    switch (returnType) {
                        case "name": {
                            return key == null ? "" : key;
                        }
                        case "value": {
                            double value = sort.get(key);
                            if (realType.equals("NetProfit")) {
                                return doubleFormat.format(value);
                            } else {
                                return integerFormat.format(value);
                            }
                        }
                    }
                }
            }
        }
        return super.onRequest(player, params);
    }

    @Nullable
    private static <T> T getKeyByIndex(Map<T, ?> map, int index) {
        int i = 0;
        for (T t : map.keySet()) {
            if (i == index) {
                return t;
            }
            i++;
        }
        return null;
    }
}
