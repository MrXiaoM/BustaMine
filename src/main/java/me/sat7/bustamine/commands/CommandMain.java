package me.sat7.bustamine.commands;

import me.sat7.bustamine.BustaMine;
import me.sat7.bustamine.utils.CustomConfig;
import me.sat7.bustamine.manager.enums.BustaType;
import me.sat7.bustamine.utils.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static me.sat7.bustamine.config.Messages.*;
import static me.sat7.bustamine.utils.Util.doubleFormat;

public class CommandMain implements CommandExecutor, TabCompleter {
    private final BustaMine plugin;
    public CommandMain(BustaMine plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("BustaMine");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;

        if (args.length == 0) {
            if (player == null) return true;
            if (player.hasPermission("bm.user.money")) {
                plugin.game().guiGameShared().openGameGUI(player, BustaType.MONEY);
            } else if (player.hasPermission("bm.user.exp")) {
                plugin.game().guiGameShared().openGameGUI(player, BustaType.EXP);
            } else {
                return Message_NoPermission.t(player);
            }
        }

        if (args.length >= 1) {
            switch (args[0]) {
                case "help":
                case "?":
                    if (player == null) return true;
                    player.sendMessage(prefix() + "Help");
                    player.sendMessage("/bm [money | exp]");
                    player.sendMessage("/bm stats [player]");
                    player.sendMessage("/bm top [NetProfit | NetProfit_Exp | GamesPlayed]");
                    if (player.hasPermission("bm.admin")) {
                        Help_BmGo.t(player);
                        Help_BmStop.t(player);
                        Help_BmStatistics.t(player);
                        Help_BmTest.t(player);
                        Help_BmReloadConfig.t(player);
                        Help_BmReloadLang.t(player);
                        Help_BmReloadLangWarning.t(player);
                    }
                    break;
                case "money":
                    if (player == null) return true;
                    if (!player.hasPermission("bm.user.money")) {
                        return Message_NoPermission.t(player);
                    }

                    plugin.game().guiGameShared().openGameGUI(player, BustaType.MONEY);
                    break;
                case "exp":
                    if (player == null) return true;
                    if (!player.hasPermission("bm.user.exp")) {
                        return Message_NoPermission.t(player);
                    }

                    plugin.game().guiGameShared().openGameGUI(player, BustaType.EXP);
                    break;
                case "stats":
                    if (!sender.hasPermission("bm.user.stats")) {
                        return Message_NoPermission.t(sender);
                    }

                    if (args.length > 1) {
                        Player p = Bukkit.getPlayer(args[1]);
                        if (p != null) {
                            plugin.users().showPlayerInfo(sender, p);
                        } else {
                            Message_PlayerNotExist.t(sender);
                        }
                    } else {
                        if (player == null) {
                            Message_PlayerNotExist.t(sender);
                            break;
                        }
                        plugin.users().showPlayerInfo(player, player);
                    }
                    break;
                case "top":
                    if (!sender.hasPermission("bm.user.top")) {
                        return Message_NoPermission.t(sender);
                    }

                    if (args.length >= 2) {
                        if (args[1].equals("NetProfit") || args[1].equals("NetProfit_Exp") || args[1].equals("GamesPlayed")) {
                            plugin.users().top(sender, args[1]);
                        } else {
                            msg(sender, "[NetProfit | NetProfit_Exp | GamesPlayed]");
                        }
                    } else {
                        plugin.users().top(sender, "NetProfit");
                    }
                    break;
                case "reloadConfig":
                    if (!sender.hasPermission("bm.admin")) {
                        return Message_NoPermission.t(sender);
                    }

                    plugin.reloadConfig();

                    Message_Reload_FromNextRound.t(sender);
                    break;
                case "reloadLang":
                    if (!sender.hasPermission("bm.admin")) {
                        return Message_NoPermission.t(sender);
                    }

                    plugin.reloadMessages();
                    plugin.reloadGUI();
                    plugin.updateConfig();

                    plugin.game().guiGameShared().gameGUISetup();
                    plugin.game().startGame();
                    plugin.game().setGameEnable(true);

                    msg(sender, "Game was terminated by server");
                    Message_Reload_Normal.t(sender);
                    break;
                case "go":
                    if (!sender.hasPermission("bm.admin")) {
                        return Message_NoPermission.t(sender);
                    }

                    plugin.game().startGame();
                    plugin.game().setGameEnable(true);
                    Message_Start.t(sender);
                    break;
                case "stop":
                    if (!sender.hasPermission("bm.admin")) {
                        return Message_NoPermission.t(sender);
                    }

                    plugin.game().setGameEnable(false);
                    Message_Stop.t(sender);
                    break;
                case "test":
                    if (!sender.hasPermission("bm.admin")) {
                        return Message_NoPermission.t(sender);
                    }

                    String name = "TestResult_" + System.currentTimeMillis();
                    CustomConfig debugResult = new CustomConfig(plugin);
                    debugResult.setup(name, config -> {
                        ArrayList<Integer> tempList = new ArrayList<>();
                        for (int i = 0; i < 100000; i++) {
                            tempList.add(plugin.game().generateBustNum());
                        }
                        config.set("result", tempList);
                    });
                    debugResult.save();

                    msg(sender, "File generated. plugins/" + plugin.getDescription().getName() + "/" + name + ".yml");
                    break;
                case "statistics":
                    if (!sender.hasPermission("bm.admin")) {
                        return Message_NoPermission.t(sender);
                    }

                    plugin.users().showStatistics(sender);
                    break;
                case "bust":
                    if (!sender.hasPermission("bm.admin") || args.length < 2) {
                        return true;
                    }

                    if ("infinite".equals(args[1])) {
                        Bust_Infinite.t(sender);
                        plugin.game().infinite(true);
                        return true;
                    }
                    if ("restore".equals(args[1])) {
                        Bust_Restore.t(sender);
                        plugin.game().infinite(false);
                        return true;
                    }
                    if ("set".equals(args[1]) && args.length > 2) {
                        Integer bust = Util.parseInt(args[2]).orElse(null);
                        if (bust == null) {
                            return NotNumber.t(sender);
                        }
                        String formatted = doubleFormat.format(bust / 100.0);
                        sender.sendMessage(prefix() + Bust_Set.get().replace("%num%", formatted));
                        plugin.game().bustNum(bust);
                        return true;
                    }
                    if ("insta".equals(args[1])) {
                        Bust_Insta.t(sender);
                        plugin.game().bustNum(plugin.game().curNum());
                        return true;
                    }
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> temp = new ArrayList<>();
            List<String> list = new ArrayList<>();

            temp.add("help");

            if (sender.hasPermission("bm.admin")) {
                temp.add("go");
                temp.add("stop");
                temp.add("statistics");
                temp.add("reloadConfig");
                temp.add("reloadLang");
                temp.add("test");
                temp.add("bust");
            }
            if (sender instanceof Player) {
                if (sender.hasPermission("bm.user.money")) {
                    temp.add("money");
                }
                if (sender.hasPermission("bm.user.exp")) {
                    temp.add("exp");
                }
            }
            if (sender.hasPermission("bm.user.stats")) {
                temp.add("stats");
            }
            if (sender.hasPermission("bm.user.top")) {
                temp.add("top");
            }
            for (String s : temp) {
                if (s.startsWith(args[0])) list.add(s);
            }

            return list;
        } else if (args.length > 1) {
            List<String> temp = new ArrayList<>();
            List<String> list = new ArrayList<>();
            if (args[0].equals("top") && (sender.hasPermission("bm.user.money") || sender.hasPermission("bm.user.exp"))) {
                temp.add("NetProfit");
                temp.add("NetProfit_Exp");
                temp.add("GamesPlayed");
            }
            if (args[0].equals("bust") && sender.hasPermission("bm.admin")) {
                temp.add("infinite");
                temp.add("restore");
                temp.add("set");
                temp.add("insta");
            }
            for (String s : temp) {
                if (s.startsWith(args[1])) list.add(s);
            }
            return list;
        }
        return null;
    }
}
