package me.sat7.bustamine;

import de.tr7zw.changeme.nbtapi.NBT;
import me.sat7.bustamine.data.User;
import me.sat7.bustamine.manager.UserManager;
import me.sat7.bustamine.manager.gui.IBustaMineGui;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.*;

import static me.sat7.bustamine.config.Messages.*;
import static me.sat7.bustamine.utils.Util.*;

public class Game {
    public static final DecimalFormat integerFormat = new DecimalFormat("0");
    public static final DecimalFormat doubleFormat = new DecimalFormat("0.00");

    static final Map<String, HashMap<String, Double>> sortedMap = new HashMap<>();
    static final Map<String, Long> sortedTime = new HashMap<>();

    public static void init() {
        sortedMap.put("NetProfit", new HashMap<>());
        sortedMap.put("NetProfit_Exp", new HashMap<>());
        sortedMap.put("GamesPlayed", new HashMap<>());
        sortedTime.put("NetProfit", 0L);
        sortedTime.put("NetProfit_Exp", 0L);
        sortedTime.put("GamesPlayed", 0L);
    }

    //=====================================================

    public static void showPlayerInfo(BustaMine plugin, Player to, Player data) {
        User user = plugin.users().get(data);
        Message_DivUpper.t(to);
        to.sendMessage("   §6§l" + data.getName());
        to.sendMessage("   §3" + NetProfit.get());
        to.sendMessage("     §3" + plugin.ccConfig.get().getString("CurrencySymbol") + "  " + doubleFormat.format(user.getNetProfit()));
        to.sendMessage("     §3Xp " + user.getNetProfitExp());

        if (to == data) {
            to.sendMessage("   §e" + Bal.get());
            to.sendMessage("     §e" + plugin.ccConfig.get().getString("CurrencySymbol") + "  " + doubleFormat.format(plugin.getEconomy().getBalance(data)));
            to.sendMessage("     §eXp " + calcTotalExp(data));
        }

        to.sendMessage("   §f" + GamesPlayed.get() + ": " + user.getGamesPlayed());
        Message_DivLower.t(to);
    }

    public static void showStatistics(BustaMine plugin, Player p) {
        double moneyIn = plugin.ccBank.get().getDouble("Statistics.Income.Money");
        double moneyOut = plugin.ccBank.get().getDouble("Statistics.Expense.Money");
        int expIn = plugin.ccBank.get().getInt("Statistics.Income.Exp");
        int expOut = plugin.ccBank.get().getInt("Statistics.Expense.Exp");

        Message_DivUpper.t(p);
        p.sendMessage("   §3" + Income.get());
        p.sendMessage("     §3" + plugin.ccConfig.get().getString("CurrencySymbol") + "  " + doubleFormat.format(moneyIn) + "  Xp " + expIn);
        p.sendMessage("   §3" + Expense.get());
        p.sendMessage("     §3" + plugin.ccConfig.get().getString("CurrencySymbol") + "  " + doubleFormat.format(moneyOut) + "  Xp " + expOut);
        p.sendMessage("   §e" + NetProfit.get());
        p.sendMessage("     §e" + plugin.ccConfig.get().getString("CurrencySymbol") + "  " + doubleFormat.format(moneyIn + moneyOut));
        p.sendMessage("     §eXp " + (expIn + expOut));
        Message_DivLower.t(p);
    }

    //---------------------------------------------------------------------------

    public static void top(UserManager manager, Player p, String type) {
        if (sortedTime.get(type) + (1000L * 60) < System.currentTimeMillis()) {
            sortedMap.get(type).clear();
            for (User user : manager.users()) {
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

                if (type.equals("NetProfit")) {
                    p.sendMessage("  " + (i + 1) + ". " + of.getName() + "   " + BustaMine.inst().ccConfig.get().getString("CurrencySymbol") + doubleFormat.format(sortedMap.get(type).get(s)));
                } else if (type.equals("NetProfit_Exp")) {
                    p.sendMessage("  " + (i + 1) + ". " + of.getName() + "   Xp" + integerFormat.format(sortedMap.get(type).get(s)));
                } else {
                    p.sendMessage("  " + (i + 1) + ". " + of.getName() + "   " + integerFormat.format(sortedMap.get(type).get(s)));
                }

                i++;
                if (i >= 10) break;
            } catch (Exception e) {
                msg(p, "Failed to load player data. " + s);
            }
        }

        p.sendMessage("  §7" + Message_LastUpdate.get().
                replace("{sec}", (System.currentTimeMillis() - sortedTime.get(type)) / 1000 + ""));
        p.sendMessage("");
    }

    public static void closeAllGameUI() {
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof IBustaMineGui) {
                msg(p, "Game was terminated by server");
                p.closeInventory();
            }
        }
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
