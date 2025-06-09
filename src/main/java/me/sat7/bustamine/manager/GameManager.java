package me.sat7.bustamine.manager;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import me.sat7.bustamine.BustaMine;
import me.sat7.bustamine.config.Config;
import me.sat7.bustamine.data.User;
import me.sat7.bustamine.manager.enums.BustaState;
import me.sat7.bustamine.manager.enums.BustaType;
import me.sat7.bustamine.utils.Util;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static me.sat7.bustamine.BustaMine.log;
import static me.sat7.bustamine.config.Messages.*;
import static me.sat7.bustamine.utils.Util.*;

public class GameManager {
    private final BustaMine plugin;
    private WrappedTask bustaTask;
    private BustaState bState = BustaState.BET;
    private boolean gameEnable;
    private int betTimeLeft;

    private int bustNum;
    private int curNum;

    private int maxMulti = 150;
    private double baseInstaBust = 0;

    protected final List<Integer> history = new ArrayList<>();
    protected final Map<Integer, ItemStack> old = new HashMap<>();

    /**
     * 玩家当前的游戏类型，金币还是经验
     */
    final Map<UUID, BustaType> playerMap = new HashMap<>();
    /**
     * 玩家下注数量
     */
    final Map<UUID, Integer> activePlayerMap = new ConcurrentHashMap<>();
    /**
     * 玩家头颅物品在菜单上的位置索引
     */
    final Map<UUID, Integer> headPos = new HashMap<>();

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
        baseInstaBust = Math.max(0, config.probabilityOfInstaBust.val() / 100 - odd(maxMulti - 1));
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

    private int gameLoop() {
        int tempOld = curNum;
        int mod;

        if (curNum < 115) {
            mod = 1;
        } else if (curNum < 180) {
            mod = 2;
        } else if (curNum < 360) {
            mod = 4;
        } else if (curNum < 720) {
            mod = 8;
        } else if (curNum < 1440) {
            mod = 16;
        } else {
            mod = 32;
        }

        for (UUID uuid : activePlayerMap.keySet()) {
            try {
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                User user = plugin.users().get(player);
                if (user.getCashOut() >= 0) {
                    if (tempOld + mod >= user.getCashOut()) {
                        Player p = player.isOnline() ? player.getPlayer() : null;
                        if (p != null) {
                            cashOut(p);
                        }
                    }
                }
            } catch (Exception ignore) {
            }
        }

        curNum = tempOld + mod;
        return curNum;
    }

    public int genBustNum() {
        return Util.genBustNum(baseInstaBust, maxMulti);
    }

    public void startGame() {
        if (bustaTask != null) bustaTask.cancel();

        playerMap.clear();
        activePlayerMap.clear();
        headPos.clear();
        // 将 1-5 行填满玻璃板
        for (int i = 0; i < 45; i++) {
            guiGameShared().setBothIcon(i, getGlassItem(9));
        }

        bState = BustaState.BET;
        betTimeLeft = config.roundInterval.val() + 1;

        if (config.isShowWinChance.val()) {
            ItemStack winChance = createItemStack(config.btnWinChance, null,
                    UI_WinChance.get(), null, 1);

            // TODO: 添加到配置文件
            double bustChance = odd(maxMulti - 1);
            ArrayList<String> winChanceArr = new ArrayList<>();
            winChanceArr.add("§ex2: " + doubleFormat.format((odd(1) - bustChance) * 100 * (1 - baseInstaBust)) + "%");
            winChanceArr.add("§ex3: " + doubleFormat.format((odd(2) - bustChance) * 100 * (1 - baseInstaBust)) + "%");
            winChanceArr.add("§ex7: " + doubleFormat.format((odd(6) - bustChance) * 100 * (1 - baseInstaBust)) + "%");
            winChanceArr.add("§ex12: " + doubleFormat.format((odd(11) - bustChance) * 100 * (1 - baseInstaBust)) + "%");
            winChanceArr.add("§eInstaBust: " + doubleFormat.format((bustChance + baseInstaBust) * 100) + "%");
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

        {
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
        }

        ItemMeta im = guiGameShared().getMoneyIcon(49).getItemMeta();
        if (im != null) {
            ArrayList<String> betLore = new ArrayList<>();
            betLore.add("§b§l" + "Bet >>>");
            im.setLore(betLore);
            guiGameShared().updateBothIcon(49, im);
        }

        bustaTask = plugin.getScheduler().runTimer(() -> {
            if (--betTimeLeft <= 0) {
                bustaTask.cancel();

                for (Integer key : old.keySet()) {
                    guiGameShared().setBothIcon(key, old.get(key));
                }

                runGame();

                for (int i = 0; i < 45; i++) {
                    if (headPos.containsValue(i)) continue;
                    guiGameShared().setBothIcon(i, getGlassItem(13));
                }

                for (int i = 51; i <= 53; i++) {
                    guiGameShared().setBothIcon(i, getGlassItem(13));
                }
            } else {
                ArrayList<String> nextRoundLore = new ArrayList<>();
                nextRoundLore.add("§b§l" + "Next round in " + betTimeLeft + "s");

                if (betTimeLeft <= 5) guiGameShared().drawNumber(betTimeLeft);

                for (int i = 51; i <= 53; i++) {
                    guiGameShared().updateBothIcon(i, nextRoundLore);
                }
            }
        }, 0, 20);
    }

    private void runGame() {
        bState = BustaState.GAME;
        curNum = 100;
        int gameLoopDelay = 4;

        bustNum = Util.genBustNum(baseInstaBust, maxMulti);

        runCommandRoundStart();

        bustaTask = plugin.getScheduler().runTimer(() ->
        {
            boolean instaBust = (bustNum == 100);

            if (gameLoop() > bustNum) {
                bustaTask.cancel();

                bust(instaBust);

                if (gameEnable) {
                    bustaTask = plugin.getScheduler().runLater(this::startGame, 80L);
                }
                return;
            }

            ArrayList<String> newLore = new ArrayList<>();
            newLore.add("§a§lx" + doubleFormat.format(curNum / 100.0));

            guiGameShared().updateBothIcon(49, newLore);

            if (config.isForceUpdateUI.val()) {
                for (UUID uuid : playerMap.keySet()) {
                    if (uuid == null)
                        continue;

                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null)
                        Util.updateInventory(player);
                }
            }

        }, 0, gameLoopDelay);
    }

    /**
     * 归零
     * @param instantBust 是否立即归零
     */
    private void bust(boolean instantBust) {
        bState = BustaState.BUSTED;
        if (instantBust) curNum = 100;

        runCommandRoundEnd(curNum);

        if (!Bukkit.getOnlinePlayers().isEmpty()) {
            if (instantBust && config.isBroadcastInstaBust.val()) {
                Bukkit.getServer().broadcastMessage(prefix() + Message_InstantBust.get());
            }
            if (config.broadcastJackpot.val() * 100 <= curNum) {
                Bukkit.getServer().broadcastMessage(prefix() + "§a§lBusted! : x" + doubleFormat.format(curNum / 100.0));
            }
        }

        for (UUID uuid : playerMap.keySet()) {
            try {
                for (UUID uuidBust : activePlayerMap.keySet()) {
                    try {
                        Player p1 = Bukkit.getPlayer(uuid);
                        Player p2 = Bukkit.getPlayer(uuidBust);
                        if (p1 != null && p2 != null) {
                            p1.sendMessage("§6♣ " + p2.getName() + " §4" + Busted.get());
                        }
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }
        }
        for (UUID uuidBust : activePlayerMap.keySet()) {
            try {
                plugin.sounds().play(Bukkit.getPlayer(uuidBust), "Bust");
            } catch (Exception ignored) {
            }
        }

        ArrayList<String> newLore = new ArrayList<>();
        newLore.add("§c§lx" + doubleFormat.format(curNum / 100.0));

        guiGameShared().updateBothIcon(49, newLore);

        for (int i = 0; i < 45; i++) {
            if (!headPos.containsValue(i)) {
                guiGameShared().setBothIcon(i, getGlassItem(14));
            }
        }

        for (int i = 51; i <= 53; i++) {
            guiGameShared().setBothIcon(i, getGlassItem(14));
        }

        history.add(curNum);
        if (history.size() > 16) history.remove(0);

        ArrayList<String> historyLore = new ArrayList<>();
        for (int i : history) {
            if (i >= 200) {
                historyLore.add("§ax" + doubleFormat.format(i / 100.0));
            } else {
                historyLore.add("§cx" + doubleFormat.format(i / 100.0));
            }
        }

        guiGameShared().updateBothIcon(48, historyLore);

        plugin.bank().save();
        plugin.users().save();
    }

    /**
     * 下注
     * @param p 下注的玩家
     * @param type 下注类型
     * @param amount 下注数量
     */
    public void bet(Player p, BustaType type, int amount) {
        // 要求游戏状态在下注阶段
        if (bState != BustaState.BET) return;

        // 如果下注的游戏不是玩家当前所在游戏，不进行任何操作
        if (!type.equals(playerMap.getOrDefault(p.getUniqueId(), null))) {
            return;
        }

        // 是否第一次下注
        boolean firstBet = !activePlayerMap.containsKey(p.getUniqueId());
        // 之前已下注数量
        int old = firstBet ? 0 : activePlayerMap.get(p.getUniqueId());

        if (type == BustaType.MONEY) {
            // 最大数量限制
            if (old + amount > config.betMax.val()) {
                BettingLimit.t(p);
                return;
            }

            Economy economy = plugin.getEconomy();
            if (economy.getBalance(p) >= amount) {
                EconomyResponse r = economy.withdrawPlayer(p, amount);

                if (!r.transactionSuccess()) {
                    p.sendMessage(String.format("An error occurred: %s", r.errorMessage));
                    return;
                }

                plugin.sounds().play(p, "Bet");
                runCommandBet(p, amount);
                activePlayerMap.put(p.getUniqueId(), old + amount);
                Message_DivUpper.t(p);
                p.sendMessage("   §f" + Bet.get() + config.currencySymbol + (old + amount));
                p.sendMessage("   §e" + MyBal.get() + ": " + config.currencySymbol + doubleFormat.format(economy.getBalance(p)));
                Message_DivLower.t(p);

                // 如果是第一次下注，向其它玩家广播 有人加入游戏的通知
                if (firstBet) {
                    for (UUID uuid : playerMap.keySet()) {
                        if (p.getUniqueId().equals(uuid)) continue;
                        try {
                            Player player = Bukkit.getPlayer(uuid);
                            if (player != null) {
                                player.sendMessage("§6♣ " + p.getName() + " " + Bet.get() + config.currencySymbol + doubleFormat.format(old + amount));
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            } else {
                Message_NotEnoughMoney.t(p);
                p.sendMessage(MyBal.get() + ": " + config.currencySymbol + doubleFormat.format(plugin.getEconomy().getBalance(p)));
                return;
            }
        } else {
            if (old + amount > config.betExpMax.val()) {
                BettingLimit.t(p);
                return;
            }

            if (calcTotalExp(p) >= amount) {
                p.giveExp(-amount);

                runCommandBet(p, amount);
                activePlayerMap.put(p.getUniqueId(), old + amount);
                Message_DivUpper.t(p);
                p.sendMessage("   §f" + Bet.get() + " Xp" + (old + amount));
                p.sendMessage("   §e" + MyBal.get() + ": Xp" + calcTotalExp(p));
                Message_DivLower.t(p);

                // 如果是第一次下注，向其它玩家广播 有人加入游戏的通知
                if (firstBet) {
                    for (UUID uuid : playerMap.keySet()) {
                        if (p.getUniqueId().equals(uuid)) continue;
                        try {
                            Player player = Bukkit.getPlayer(uuid);
                            if (player != null) {
                                player.sendMessage("§6♣ " + p.getName() + " " + Bet.get() + " Xp" + (old + amount));
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            } else {
                Message_NotEnoughExp.t(p);
                p.sendMessage(MyBal.get() + ": Xp" + calcTotalExp(p));
                return;
            }
        }

        updateBankroll(type, amount);
        updateNetProfit(p, type, -amount);

        if (firstBet) {
            // 如果是第一次下注
            // 设置游戏类型，更新游玩次数，向界面添加头颅图标
            playerMap.put(p.getUniqueId(), type);

            User user = plugin.users().get(p);
            user.setGamesPlayed(user.getGamesPlayed() + 1);

            // TODO: 虽然玩家不至于很多，但应该要做一个翻页机制
            if (playerMap.size() < 43) {
                int idx = playerMap.size() - 1;
                ItemStack skull = Util.getPlayerHeadItem();
                ItemMeta meta = skull.getItemMeta();

                if (meta != null) {
                    meta.setDisplayName("§6" + p.getName());
                    ArrayList<String> lore = new ArrayList<>();
                    if (type == BustaType.MONEY) {
                        lore.add(UI_PlayerInfo.get().replace("{amount}", config.currencySymbol.val() + amount));
                    } else {
                        lore.add(UI_PlayerInfo.get().replace("{amount}", "Xp" + amount));
                    }
                    meta.setLore(lore);
                    skull.setItemMeta(meta);
                }
                flag(skull, "show player info:" + p.getName());

                if (config.isLoadPlayerSkin.val()) {
                    // 更新头颅物品皮肤
                    plugin.getScheduler().runAsync(t -> {
                        ItemMeta itemMeta = skull.getItemMeta();
                        if (itemMeta instanceof SkullMeta) {
                            try {
                                SkullMeta skullMeta = (SkullMeta) itemMeta;
                                skullMeta.setOwningPlayer(p);
                                skull.setItemMeta(skullMeta);
                            } catch (Exception e) {
                                log("Failed to load skull skin of player: " + p.getName());
                            }
                        }
                    });
                }

                guiGameShared().setBothIcon(idx, skull);

                headPos.put(p.getUniqueId(), idx);
            }
        } else try {
            // 不是第一次下注，则更新头颅图标的 lore
            if (headPos.containsKey(p.getUniqueId())) {
                int idx = headPos.get(p.getUniqueId());
                List<String> lore = new ArrayList<>();
                if (type == BustaType.MONEY) {
                    lore.add(UI_PlayerInfo.get().replace("{amount}", config.currencySymbol.val() + (old + amount)));
                } else {
                    lore.add(UI_PlayerInfo.get().replace("{amount}", "Xp" + (old + amount)));
                }
                guiGameShared().updateBothIcon(idx, lore);
            }
        } catch (Exception e) {
            log("Failed to update UI. Game/Bet/!firstBet", e);
        }
    }

    /**
     * 抛售
     * @param p 要进行抛售操作的玩家
     */
    public void cashOut(Player p) {
        // 要求游戏状态不在游戏进行中阶段
        if (bState != BustaState.GAME) return;

        // 要求已下注，获取并清空玩家的下注金额
        Integer bet = activePlayerMap.remove(p.getUniqueId());
        if (bet == null) return;

        double prize = bet * (curNum / 100.0);

        runCommandCashOut(p, bet, curNum, prize);
        plugin.sounds().play(p, "CashOut");

        // 进行抛售操作，给予玩家奖励的金币或经验
        Message_DivUpper.t(p);
        p.sendMessage("   §f" + CashedOut.get() + ": x" + doubleFormat.format(curNum / 100.0));
        if (playerMap.get(p.getUniqueId()) == BustaType.MONEY) {
            plugin.getEconomy().depositPlayer(p, prize);
            p.sendMessage("   §3" + Profit.get() + ": " + config.currencySymbol + doubleFormat.format(prize - bet));
            p.sendMessage("   §e" + MyBal.get() + ": " + config.currencySymbol + doubleFormat.format(plugin.getEconomy().getBalance(p)));
        } else {
            p.giveExp((int) prize);
            p.sendMessage("   §3" + Profit.get() + ": Xp" + ((int) prize - bet));
            p.sendMessage("   §e" + MyBal.get() + ": Xp" + calcTotalExp(p));
        }
        Message_DivLower.t(p);

        if (headPos.containsKey(p.getUniqueId())) {
            // 更新玩家头颅 lore
            ItemStack out = new ItemStack(getGlass(11));
            ItemStack head = guiGameShared().getMoneyIcon(headPos.get(p.getUniqueId()));
            ItemMeta headMeta = head.getItemMeta();
            List<String> oldLore = headMeta != null ? headMeta.getLore() : null;

            List<String> lore = new ArrayList<>();
            if (oldLore != null) lore.addAll(oldLore);
            lore.add("§f" + CashedOut.get() + ": x" + doubleFormat.format(curNum / 100.0));

            ItemMeta meta = out.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(head.getItemMeta().getDisplayName());
                meta.setLore(lore);
                out.setItemMeta(meta);
            }

            flag(out, "show player info:" + p.getName());
            guiGameShared().setBothIcon(headPos.get(p.getUniqueId()), out);
            headPos.remove(p.getUniqueId());
        }

        // 向游戏中所有玩家广播 抛售通知
        for (UUID uuid : playerMap.keySet()) {
            if (p.getUniqueId().equals(uuid)) continue;
            try {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.sendMessage("§6♣ " + p.getName() + " " + CashedOut.get() + " x" + doubleFormat.format(curNum / 100.0));
                }
            } catch (Exception ignored) {
            }
        }

        updateBankroll(playerMap.get(p.getUniqueId()), -prize);
        updateNetProfit(p, playerMap.get(p.getUniqueId()), prize);
    }

    /**
     * 更新总资金数据
     * @param type 游戏类型
     * @param amount 增加数量
     */
    private void updateBankroll(BustaType type, Number amount) {
        // 更新当前资金数据
        if (type == BustaType.MONEY) {
            plugin.bank().plusDouble("Bankroll.Money", amount.doubleValue());
        } else {
            plugin.bank().plusInteger("Bankroll.Exp", amount.intValue());
        }

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
    private void updateNetProfit(Player p, BustaType type, double amount) {
        double old;
        User user = plugin.users().get(p);
        if (type == BustaType.MONEY) {
            old = user.getNetProfit();
            user.setNetProfit(old + amount);
        } else {
            old = user.getNetProfitExp();
            user.setNetProfitExp((int) (old + amount));
        }
    }

}
