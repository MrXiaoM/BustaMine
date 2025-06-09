package me.sat7.bustamine.manager;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import me.sat7.bustamine.BustaMine;
import me.sat7.bustamine.config.Config;
import me.sat7.bustamine.data.User;
import me.sat7.bustamine.manager.enums.BustaState;
import me.sat7.bustamine.manager.enums.BustaType;
import me.sat7.bustamine.utils.Util;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import static me.sat7.bustamine.config.Messages.*;
import static me.sat7.bustamine.utils.Util.*;

public class GameManager {
    private final BustaMine plugin;
    private WrappedTask bustaTask;
    private boolean gameEnable;
    private int betTimeLeft;

    /**
     * 归零计数，除以 <code>100.0</code> 即为玩家最高可得倍率，当 <code>curNum > bustNum</code> 时，进行归零操作
     */
    private int bustNum;
    /**
     * 当前计数，除以 <code>100.0</code> 即为当前倍率，当 <code>curNum > bustNum</code> 时，进行归零操作
     */
    private int curNum;

    private int maxMulti = 150;
    private double baseInstantBust = 0;

    private final GuiGameShared guiGameShared;
    private final GuiBetSettings guiBetSettings;
    private final Config config;

    public GameManager(BustaMine plugin) {
        this.plugin = plugin;
        this.config = plugin.config();
        this.guiGameShared = new GuiGameShared(this);
        this.guiBetSettings = new GuiBetSettings(this);
    }

    public GuiGameShared guiGameShared() {
        return guiGameShared;
    }

    public GuiBetSettings guiBetSettings() {
        return guiBetSettings;
    }

    public BustaMine plugin() {
        return plugin;
    }

    public void reload() {
        maxMulti = config.multiplierMax.val();
        baseInstantBust = Math.max(0, config.probabilityOfInstaBust.val() / 100 - odd(maxMulti - 1));
        guiGameShared().reload();
        guiBetSettings().reload();
    }

    public void setGameEnable(boolean gameEnable) {
        this.gameEnable = gameEnable;
    }

    public void runCommandRoundStart() {
        if (config.commandRoundStart.isEmpty()) return;
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), config.commandRoundStart.val());
    }

    public void runCommandBet(Player p, int amount) {
        if (config.commandPlayerBet.isEmpty()) return;
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), config.commandPlayerBet
                .replace("{player}", p.getName()).replace("{amount}", amount + ""));
    }

    public void runCommandCashOut(Player p, double amount, int multiplier, double prize) {
        if (config.commandPlayerCashOut.isEmpty()) return;
        double temp = multiplier / 100.0f;
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), config.commandPlayerCashOut
                .replace("{player}", p.getName())
                .replace("{amount}", amount + "")
                .replace("{multiplier}", temp + "")
                .replace("{prize}", prize + "")
        );
    }

    public void runCommandRoundEnd(int multiplier) {
        if (config.commandRoundEnd.isEmpty()) return;
        double temp = multiplier / 100.0f;
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), config.commandRoundEnd
                .replace("{multiplier}", temp + "")
        );
    }

    /**
     * 执行游戏循环
     * @return 返回执行后的 curNum 值
     */
    private int gameLoop() {
        int old = curNum();
        int mod;

        // 当前倍率越高，增速越高
        if (old < 115) {
            mod = 1;
        } else if (old < 180) {
            mod = 2;
        } else if (old < 360) {
            mod = 4;
        } else if (old < 720) {
            mod = 8;
        } else if (old < 1440) {
            mod = 16;
        } else {
            mod = 32;
        }
        int newNum = old + mod;

        for (UUID uuid : guiGameShared().activePlayers()) {
            try {
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                User user = plugin.users().get(player);
                if (user.getCashOut() >= 0) { // 如果玩家抛售数量大于0，且当前数量大于抛售数量，则执行抛售
                    if (newNum >= user.getCashOut()) {
                        Player p = player.isOnline() ? player.getPlayer() : null;
                        if (p != null) {
                            guiGameShared().cashOut(p);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        curNum(newNum);
        return curNum();
    }

    /**
     * @see GameManager#curNum
     */
    public int curNum() {
        return curNum;
    }

    /**
     * @see GameManager#curNum
     */
    public void curNum(int value) {
        curNum = value;
    }

    /**
     * @see GameManager#curNum
     */
    public String curNumFormatted() {
        return doubleFormat.format(curNum / 100.0);
    }

    public int generateBustNum() {
        return Util.generateBustNum(baseInstantBust, maxMulti);
    }

    /**
     * 开始游戏
     */
    public void startGame() {
        if (bustaTask != null) bustaTask.cancel();

        // 清空数据，将游戏状态设为 BET
        guiGameShared().reset();
        // 将 1-5 行填满玻璃板
        for (int i = 0; i < 45; i++) {
            guiGameShared().setBothIcon(i, getGlassItem(9));
        }

        // 设定下注倒计时
        betTimeLeft = config.roundInterval.val() + 1;

        // 显示胜率公示
        if (config.isShowWinChance.val()) {
            ItemStack winChance = createItemStack(config.btnWinChance, null,
                    UI_WinChance.get(), null, 1);

            // TODO: 添加到配置文件
            double bustChance = odd(maxMulti - 1);
            ArrayList<String> winChanceArr = new ArrayList<>();
            winChanceArr.add("§ex2: " + doubleFormat.format((odd(1) - bustChance) * 100 * (1 - baseInstantBust)) + "%");
            winChanceArr.add("§ex3: " + doubleFormat.format((odd(2) - bustChance) * 100 * (1 - baseInstantBust)) + "%");
            winChanceArr.add("§ex7: " + doubleFormat.format((odd(6) - bustChance) * 100 * (1 - baseInstantBust)) + "%");
            winChanceArr.add("§ex12: " + doubleFormat.format((odd(11) - bustChance) * 100 * (1 - baseInstantBust)) + "%");
            winChanceArr.add("§eInstaBust: " + doubleFormat.format((bustChance + baseInstantBust) * 100) + "%");
            winChanceArr.add("§e" + MaximumMultiplier.get() + ": x" + maxMulti);

            ItemMeta meta = winChance.getItemMeta();
            if (meta != null) {
                meta.setLore(winChanceArr);
                winChance.setItemMeta(meta);
            }
            flag(winChance, "win chance");
            guiGameShared().setBothIcon(46, winChance);
        } else {
            guiGameShared().setBothIcon(46, null);
        }

        // 显示资金
        if (config.isShowBankroll.val()) {
            if (guiGameShared().getMoneyIcon(45) == null) {
                ItemStack bankrollBtn = createItemStack(config.btnBankroll, null,
                        UI_Bankroll.get(), null, 1);
                flag(bankrollBtn, "bankroll");
                guiGameShared().setBothIcon(45, bankrollBtn);
            }
            updateBankroll(BustaType.MONEY, 0);
            updateBankroll(BustaType.EXP, 0);
        } else {
            guiGameShared().setBothIcon(45, null);
        }

        // 添加下注按钮
        ItemStack bet10Btn = createItemStack(config.btnBetSmall, null,
                UI_BetBtn.get() + " §e" + config.currencySymbol + config.betSmall, null, 1);
        flag(bet10Btn, "bet:small");
        guiGameShared().setMoneyIcon(51, bet10Btn);
        ItemStack betE1Btn = createItemStack(config.btnBetSmall, null,
                UI_BetBtn.get() + " §eXp" + config.betExpSmall, null, 1);
        flag(betE1Btn, "bet:small");
        guiGameShared().setExpIcon(51, betE1Btn);

        // 100
        ItemStack bet100Btn = createItemStack(config.btnBetMedium, null,
                UI_BetBtn.get() + " §e" + config.currencySymbol + config.betMedium, null, 1);
        flag(bet100Btn, "bet:medium");
        guiGameShared().setMoneyIcon(52, bet100Btn);
        ItemStack betE2Btn = createItemStack(config.btnBetMedium, null,
                UI_BetBtn.get() + " §eXp" + config.betExpMedium, null, 1);
        flag(betE2Btn, "bet:medium");
        guiGameShared().setExpIcon(52, betE2Btn);

        // 1000
        ItemStack bet1000Btn = createItemStack(config.btnBetBig, null,
                UI_BetBtn.get() + " §e" + config.currencySymbol + config.betBig, null, 1);
        flag(bet100Btn, "bet:big");
        guiGameShared().setMoneyIcon(53, bet1000Btn);
        ItemStack betE3Btn = createItemStack(config.btnBetBig, null,
                UI_BetBtn.get() + " §eXp" + config.betExpBig, null, 1);
        flag(betE3Btn, "bet:big");
        guiGameShared().setExpIcon(53, betE3Btn);

        // 更新抛售图标为下注
        ItemMeta im = guiGameShared().getMoneyIcon(49).getItemMeta();
        if (im != null) {
            ArrayList<String> betLore = new ArrayList<>();
            betLore.add("§b§l" + "Bet >>>");
            im.setLore(betLore);
            guiGameShared().updateBothIcon(49, im);
        }

        // 开启定时器，进行倒计时
        bustaTask = plugin.getScheduler().runTimer(() -> {
            if (--betTimeLeft <= 0) {
                // 时间到，停止定时器，开始游戏
                bustaTask.cancel();

                guiGameShared().restoreOldIcons();

                runGame();

                for (int i = 0; i < 45; i++) {
                    if (guiGameShared().containsHeadSlot(i)) continue;
                    guiGameShared().setBothIcon(i, getGlassItem(13));
                }
                for (int i = 51; i <= 53; i++) {
                    guiGameShared().setBothIcon(i, getGlassItem(13));
                }
            } else {
                // 显示距离下一轮游戏开始的剩余时间，更新到 lore
                List<String> nextRoundLore = new ArrayList<>();
                nextRoundLore.add("§b§l" + "Next round in " + betTimeLeft + "s");

                if (betTimeLeft <= 5) guiGameShared().drawNumber(betTimeLeft);

                for (int i = 51; i <= 53; i++) {
                    guiGameShared().updateBothIcon(i, nextRoundLore);
                }
            }
        }, 0, 20);
    }

    /**
     * 下注结束，开始一轮游戏
     */
    private void runGame() {
        // 设置当前阶段为游戏进行中
        guiGameShared().setBustaState(BustaState.GAME);

        curNum(100);
        int gameLoopDelay = 4; // 定时器循环时间 (4/20=0.2s)

        bustNum = generateBustNum();

        runCommandRoundStart();

        // 开启定时器
        bustaTask = plugin.getScheduler().runTimer(() -> {
            // 归零数量 == 100 时，立即归零
            boolean instantBust = (bustNum == 100);

            if (gameLoop() > bustNum) { // 如果 当前数量 大于 归零数量，则执行归零操作
                bustaTask.cancel();

                guiGameShared().bust(instantBust);

                if (gameEnable) {
                    bustaTask = plugin.getScheduler().runLater(this::startGame, 80L);
                }
                return;
            }

            // 更新当前数量到 lore
            List<String> newLore = new ArrayList<>();
            newLore.add("§a§lx" + curNumFormatted());

            guiGameShared().updateBothIcon(49, newLore);

            if (config.isForceUpdateUI.val()) {
                // 强制更新玩家界面
                for (UUID uuid : guiGameShared().inGamePlayers()) {
                    Player player = uuid == null ? null : Bukkit.getPlayer(uuid);
                    if (player != null) {
                        Util.updateInventory(player);
                    }
                }
            }

        }, 0, gameLoopDelay);
    }

    /**
     * 更新总资金数据
     * @param type 游戏类型
     * @param amount 增加数量
     */
    void updateBankroll(BustaType type, Number amount) {
        // 更新当前资金数据
        if (type == BustaType.MONEY) {
            plugin.bank().plusDouble("Bankroll.Money", amount.doubleValue());
        } else {
            plugin.bank().plusInteger("Bankroll.Exp", amount.intValue());
        }

        // 显示新的资金数据到 lore
        if (config.isShowBankroll.val()) {
            List<String> lore = new ArrayList<>();
            double bankMoney = plugin.bank().getDouble("Bankroll.Money");
            int bankExp = plugin.bank().getInt("Bankroll.Exp");
            lore.add("§e" + config.currencySymbol + String.format("%.1f", bankMoney / 1000.0) + "K");
            lore.add("§eXp" + String.format("%.1f", bankExp / 1000.0) + "K");

            guiGameShared().updateBothIcon(45, lore);
        }

        // 更新统计数据
        if (amount.doubleValue() > 0) {
            if (type == BustaType.MONEY) {
                plugin.bank().plusDouble("Statistics.Income.Money", amount.doubleValue());
            } else {
                plugin.bank().plusInteger("Statistics.Income.Exp", amount.intValue());
            }
        } else {
            if (type == BustaType.MONEY) {
                plugin.bank().plusDouble("Statistics.Expense.Money", amount.doubleValue());
            } else {
                plugin.bank().plusInteger("Statistics.Expense.Exp", amount.intValue());
            }
        }
    }

    /**
     * 更新净利润信息
     * @param p 玩家
     * @param type 游戏类型
     * @param amount 增加数量
     */
    void updateNetProfit(Player p, BustaType type, double amount) {
        User user = plugin.users().get(p);
        if (type == BustaType.MONEY) {
            double old = user.getNetProfit();
            user.setNetProfit(old + amount);
        } else {
            double old = user.getNetProfitExp();
            user.setNetProfitExp((int) (old + amount));
        }
    }
}
