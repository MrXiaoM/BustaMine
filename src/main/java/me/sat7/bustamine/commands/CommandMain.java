package me.sat7.bustamine.commands;

import me.sat7.bustamine.BustaMine;
import me.sat7.bustamine.CustomConfig;
import me.sat7.bustamine.Game;
import me.sat7.bustamine.manager.enums.BustaType;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static me.sat7.bustamine.config.Messages.*;

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
        if (!(sender instanceof Player)) {
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            if (player.hasPermission("bm.user.money")) {
                plugin.game().gui().openGameGUI(player, BustaType.MONEY);
            } else if (player.hasPermission("bm.user.exp")) {
                plugin.game().gui().openGameGUI(player, BustaType.EXP);
            } else {
                return Message_NoPermission.t(player);
            }
        }

        if (args.length >= 1) {
            switch (args[0]) {
                case "help":
                case "?":
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
                    if (!player.hasPermission("bm.user.money")) {
                        return Message_NoPermission.t(player);
                    }

                    plugin.game().gui().openGameGUI(player, BustaType.MONEY);
                    break;
                case "exp":
                    if (!player.hasPermission("bm.user.exp")) {
                        return Message_NoPermission.t(player);
                    }

                    plugin.game().gui().openGameGUI(player, BustaType.EXP);
                    break;
                case "stats":
                    if (!player.hasPermission("bm.user.stats")) {
                        return Message_NoPermission.t(player);
                    }

                    if (args.length > 1) {
                        Player p = Bukkit.getPlayer(args[1]);
                        if (p != null) {
                            Game.showPlayerInfo(plugin, player, p);
                        } else {
                            Message_PlayerNotExist.t(player);
                        }
                    } else {
                        Game.showPlayerInfo(plugin, player, player);
                    }
                    break;
                case "top":
                    if (!player.hasPermission("bm.user.top")) {
                        return Message_NoPermission.t(player);
                    }

                    if (args.length >= 2) {
                        if (args[1].equals("NetProfit") || args[1].equals("NetProfit_Exp") || args[1].equals("GamesPlayed")) {
                            Game.top(plugin.users(), player, args[1]);
                        } else {
                            msg(player, "[NetProfit | NetProfit_Exp | GamesPlayed]");
                        }
                    } else {
                        Game.top(plugin.users(), player, "NetProfit");
                    }
                    break;
                case "reloadConfig":
                    if (!player.hasPermission("bm.admin")) {
                        return Message_NoPermission.t(player);
                    }

                    plugin.reloadConfig();

                    Message_Reload_FromNextRound.t(player);
                    break;
                case "reloadLang":
                    if (!player.hasPermission("bm.admin")) {
                        return Message_NoPermission.t(player);
                    }

                    plugin.reloadMessages();
                    plugin.updateConfig();

                    plugin.game().gui().gameGUISetup();
                    plugin.game().startGame();
                    plugin.game().setGameEnable(true);

                    msg(player, "Game was terminated by server");
                    Message_Reload_Normal.t(player);
                    break;
                case "go":
                    if (!player.hasPermission("bm.admin")) {
                        return Message_NoPermission.t(player);
                    }

                    plugin.game().startGame();
                    plugin.game().setGameEnable(true);
                    Message_Start.t(player);
                    break;
                case "stop":
                    if (!player.hasPermission("bm.admin")) {
                        return Message_NoPermission.t(player);
                    }

                    plugin.game().setGameEnable(false);
                    Message_Stop.t(player);
                    break;
                case "test":
                    if (!player.hasPermission("bm.admin")) {
                        return Message_NoPermission.t(player);
                    }

                    CustomConfig debugResult = new CustomConfig(plugin);
                    String name = "TestResult_" + System.currentTimeMillis();
                    debugResult.setup(name);

                    ArrayList<Integer> tempList = new ArrayList<>();
                    for (int i = 0; i < 100000; i++) {
                        tempList.add(plugin.game().genBustNum());
                    }

                    debugResult.get().set("result", tempList);
                    debugResult.save();

                    msg(player, "File generated. plugins/BustaMine/" + name + ".yml");
                    break;
                case "statistics":
                    if (!player.hasPermission("bm.admin")) {
                        return Message_NoPermission.t(player);
                    }

                    Game.showStatistics(plugin, player);
                    break;
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            ArrayList<String> temp = new ArrayList<>();
            ArrayList<String> list = new ArrayList<>();

            temp.add("help");

            if (sender.hasPermission("bm.admin")) {
                temp.add("go");
                temp.add("stop");
                temp.add("statistics");
                temp.add("reloadConfig");
                temp.add("reloadLang");
                temp.add("test");
            }
            if (sender.hasPermission("bm.user.money") || sender.hasPermission("bm.user.exp")) {
                temp.add("money");
                temp.add("exp");
                temp.add("stats");
                temp.add("top");
            }

            for (String s : temp) {
                if (s.startsWith(args[0])) list.add(s);
            }

            return list;
        } else if (args.length > 1) {
            ArrayList<String> temp = new ArrayList<>();
            ArrayList<String> list = new ArrayList<>();
            if (args[0].equals("top") && (sender.hasPermission("bm.user.money") || sender.hasPermission("bm.user.exp"))) {
                temp.add("NetProfit");
                temp.add("NetProfit_Exp");
                temp.add("GamesPlayed");
            }
            for (String s : temp) {
                if (s.startsWith(args[1])) list.add(s);
            }
            return list;
        }
        return null;
    }
}
