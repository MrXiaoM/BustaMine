package me.sat7.bustamine.utils;

import com.google.common.collect.Lists;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unused"})
public class PAPI {
    private static boolean isEnabled = false;
    protected static void init() {
        isEnabled = Util.isPresent("me.clip.placeholderapi.PlaceholderAPI");
    }

    public static boolean isEnabled() {
        return isEnabled;
    }

    public static String setPlaceholders(String s) {
        return setPlaceholders((OfflinePlayer) null, s);
    }
    public static String setPlaceholders(OfflinePlayer player, String s) {
        if (!isEnabled) {
            if (player == null) return s;
            return s.replace("%player_name%", String.valueOf(player.getName()));
        }
        return PlaceholderAPI.setPlaceholders(player, s);
    }
    public static String setPlaceholders(Player player, String s) {
        if (!isEnabled) {
            if (player == null) return s;
            return s.replace("%player_name%", player.getName());
        }
        return PlaceholderAPI.setPlaceholders(player, s);
    }

    public static List<String> setPlaceholders(List<String> s) {
        return setPlaceholders((OfflinePlayer) null, s);
    }
    public static List<String> setPlaceholders(OfflinePlayer player, List<String> list) {
        if (!isEnabled) {
            if (player == null) return Lists.newArrayList(list);
            List<String> result = new ArrayList<>();
            String playerName = String.valueOf(player.getName());
            for (String s : list) {
                result.add(s.replace("%player_name%", playerName));
            }
            return result;
        }
        return PlaceholderAPI.setPlaceholders(player, list);
    }
    public static List<String> setPlaceholders(Player player, List<String> list) {
        if (!isEnabled) {
            if (player == null) return Lists.newArrayList(list);
            List<String> result = new ArrayList<>();
            String playerName = player.getName();
            for (String s : list) {
                result.add(s.replace("%player_name%", playerName));
            }
            return result;
        }
        return PlaceholderAPI.setPlaceholders(player, list);
    }
}
