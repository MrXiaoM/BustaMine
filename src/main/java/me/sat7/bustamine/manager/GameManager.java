package me.sat7.bustamine.manager;

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
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static me.sat7.bustamine.BustaMine.log;
import static me.sat7.bustamine.config.Messages.*;
import static me.sat7.bustamine.config.Messages.UI_BetBtn;
import static me.sat7.bustamine.utils.Util.*;

public class GameManager {
    private final BustaMine plugin;
    private BukkitTask bustaTask;
    private BustaState bState = BustaState.BET;
    private boolean gameEnable;
    private int betTimeLeft;

    private int bustNum;
    private int curNum;

    private int maxMulti = 150;
    private double baseInstaBust = 0;

    protected final List<Integer> history = new ArrayList<>();
    protected final Map<Integer, ItemStack> old = new HashMap<>();

    final Map<UUID, BustaType> playerMap = new HashMap<>();
    final Map<UUID, Integer> activePlayerMap = new ConcurrentHashMap<>();
    final Map<UUID, Integer> headPos = new HashMap<>();
    
    private final GuiGame guiGame;
    private final GuiBetSettings guiBetSettings;
    private final Config config;

    public GameManager(BustaMine plugin) {
        this.plugin = plugin;
        this.config = plugin.config();
        this.guiGame = new GuiGame(this);
        this.guiBetSettings = new GuiBetSettings(this);
    }

    public GuiGame gui() {
        return guiGame;
    }

    public GuiBetSettings betSettings() {
        return guiBetSettings;
    }

    public BustaMine plugin() {
        return plugin;
    }

    public void reload() {
        maxMulti = config.multiplierMax.val();
        baseInstaBust = Math.max(0, config.probabilityOfInstaBust.val() / 100 - odd(maxMulti - 1));
        gui().reload();
        betSettings().reload();
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
            gui().setBothIcon(i, getGlassItem(9));
        }

        bState = BustaState.BET;
        betTimeLeft = config.roundInterval.val() + 1;

        if (config.isShowWinChance.val()) {
            ItemStack winChance = createItemStack(config.btnWinChance, null,
                    UI_WinChance.get(), null, 1);

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
            gui().setBothIcon(46, winChance);
        } else {
            gui().setBothIcon(46, null);
        }

        if (config.isShowBankroll.val()) {
            if (gui().getMoneyIcon(45) == null) {
                ItemStack bankrollBtn = createItemStack(config.btnBankroll, null,
                        UI_Bankroll.get(), null, 1);
                gui().setBothIcon(45, bankrollBtn);
            }
            updateBankroll(BustaType.MONEY, 0);
            updateBankroll(BustaType.EXP, 0);
        } else {
            gui().setBothIcon(45, null);
        }

        {
            ItemStack bet10Btn = createItemStack(config.btnBetSmall, null,
                    UI_BetBtn.get() + " §e" + config.currencySymbol + config.betSmall, null, 1);
            gui().setMoneyIcon(51, bet10Btn);
            ItemStack betE1Btn = createItemStack(config.btnBetSmall, null,
                    UI_BetBtn.get() + " §eXp" + config.betExpSmall, null, 1);
            gui().setExpIcon(51, betE1Btn);

            // 100
            ItemStack bet100Btn = createItemStack(config.btnBetMedium, null,
                    UI_BetBtn.get() + " §e" + config.currencySymbol + config.betMedium, null, 1);
            gui().setMoneyIcon(52, bet100Btn);
            ItemStack betE2Btn = createItemStack(config.btnBetMedium, null,
                    UI_BetBtn.get() + " §eXp" + config.betExpMedium, null, 1);
            gui().setExpIcon(52, betE2Btn);

            // 1000
            ItemStack bet1000Btn = createItemStack(config.btnBetBig, null,
                    UI_BetBtn.get() + " §e" + config.currencySymbol + config.betBig, null, 1);
            gui().setMoneyIcon(53, bet1000Btn);
            ItemStack betE3Btn = createItemStack(config.btnBetBig, null,
                    UI_BetBtn.get() + " §eXp" + config.betExpBig, null, 1);
            gui().setExpIcon(53, betE3Btn);
        }

        ItemMeta im = gui().getMoneyIcon(49).getItemMeta();
        if (im != null) {
            ArrayList<String> betLore = new ArrayList<>();
            betLore.add("§b§l" + "Bet >>>");
            im.setLore(betLore);
            gui().updateBothIcon(49, im);
        }

        bustaTask = Bukkit.getScheduler().runTaskTimer(plugin, () ->
        {
            if (--betTimeLeft <= 0) {
                bustaTask.cancel();

                for (Integer key : old.keySet()) {
                    gui().setBothIcon(key, old.get(key));
                }

                runGame();

                for (int i = 0; i < 45; i++) {
                    if (headPos.containsValue(i)) continue;
                    gui().setBothIcon(i, getGlassItem(13));
                }

                for (int i = 51; i <= 53; i++) {
                    gui().setBothIcon(i, getGlassItem(13));
                }
            } else {
                ArrayList<String> nextRoundLore = new ArrayList<>();
                nextRoundLore.add("§b§l" + "Next round in " + betTimeLeft + "s");

                if (betTimeLeft <= 5) gui().drawNumber(betTimeLeft);

                for (int i = 51; i <= 53; i++) {
                    gui().updateBothIcon(i, nextRoundLore);
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

        bustaTask = Bukkit.getScheduler().runTaskTimer(plugin, () ->
        {
            boolean instaBust = (bustNum == 100);

            if (gameLoop() > bustNum) {
                bustaTask.cancel();

                bust(instaBust);

                if (gameEnable) {
                    bustaTask = Bukkit.getScheduler().runTaskLater(plugin, this::startGame, 80);
                }

                return;
            }

            ArrayList<String> newLore = new ArrayList<>();
            newLore.add("§a§lx" + doubleFormat.format(curNum / 100.0));

            gui().updateBothIcon(49, newLore);

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

    private void bust(boolean instaBust) {
        bState = BustaState.BUSTED;
        if (instaBust) curNum = 100;

        runCommandRoundEnd(curNum);

        if (instaBust && config.isBroadcastInstaBust.val()) {
            Bukkit.getServer().broadcastMessage(prefix() + Message_Instabust.get());
        }
        if (config.broadcastJackpot.val() * 100 <= curNum) {
            Bukkit.getServer().broadcastMessage(prefix() + "§a§lBusted! : x" + doubleFormat.format(curNum / 100.0));
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

        gui().updateBothIcon(49, newLore);

        for (int i = 0; i < 45; i++) {
            if (!headPos.containsValue(i)) {
                gui().setBothIcon(i, getGlassItem(14));
            }
        }

        for (int i = 51; i <= 53; i++) {
            gui().setBothIcon(i, getGlassItem(14));
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

        gui().updateBothIcon(48, historyLore);

        plugin.bank().save();
        plugin.users().save();
    }

    public void bet(Player p, BustaType type, int amount) {
        if (bState != BustaState.BET) return;

        boolean firstBet = !activePlayerMap.containsKey(p.getUniqueId());
        int old = 0;
        if (activePlayerMap.containsKey(p.getUniqueId()))
            old = activePlayerMap.get(p.getUniqueId());

        if (playerMap.containsKey(p.getUniqueId()) && !playerMap.get(p.getUniqueId()).equals(type)) {
            return;
        }

        if (type == BustaType.MONEY) {
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
        }
        else {
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
            playerMap.put(p.getUniqueId(), type);

            User user = plugin.users().get(p);
            user.setGamesPlayed(user.getGamesPlayed() + 1);

            if (playerMap.size() < 43) {
                Material m = Material.PLAYER_HEAD;

                ItemStack skull = new ItemStack(m, 1);
                ItemMeta meta = skull.getItemMeta();

                if (config.isLoadPlayerSkin.val()) {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadAndSetSkin(p, playerMap.size() - 1));
                }

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
                gui().setBothIcon(playerMap.size() - 1, skull);

                headPos.put(p.getUniqueId(), playerMap.size() - 1);
            }
        } else {
            try {
                if (headPos.containsKey(p.getUniqueId())) {
                    int idx = headPos.get(p.getUniqueId());
                    ItemStack item = gui().getMoneyIcon(idx);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        ArrayList<String> lore = new ArrayList<>();
                        if (type == BustaType.MONEY) {
                            lore.add(UI_PlayerInfo.get().replace("{amount}", config.currencySymbol.val() + (old + amount)));
                        } else {
                            lore.add(UI_PlayerInfo.get().replace("{amount}", "Xp" + (old + amount)));
                        }
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                    gui().setBothIcon(idx, item);
                }
            } catch (Exception e) {
                log("Failed to update UI. Game/Bet/!firstBet", e);
            }
        }
    }

    private void loadAndSetSkin(Player p, int idx) {
        ItemStack item = gui().getMoneyIcon(idx);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta) {
            try {
                ((SkullMeta) meta).setOwningPlayer(p);
            } catch (Exception e) {
                log("Failed to load player skin");
            }
        }

        item.setItemMeta(meta);
        gui().setBothIcon(idx, item);
    }

    public void cashOut(Player p) {
        if (!activePlayerMap.containsKey(p.getUniqueId())) return;
        if (bState != BustaState.GAME) return;

        double bet = activePlayerMap.get(p.getUniqueId());
        double prize = bet * (curNum / 100.0);

        runCommandCashOut(p, bet, curNum, prize);
        plugin.sounds().play(p, "CashOut");

        Message_DivUpper.t(p);
        p.sendMessage("   §f" + CashedOut.get() + ": x" + doubleFormat.format(curNum / 100.0));
        if (playerMap.get(p.getUniqueId()) == BustaType.MONEY) {
            p.sendMessage("   §3" + Profit.get() + ": " + config.currencySymbol + doubleFormat.format(prize - bet));
            plugin.getEconomy().depositPlayer(p, prize);
            p.sendMessage("   §e" + MyBal.get() + ": " + config.currencySymbol + doubleFormat.format(plugin.getEconomy().getBalance(p)));
        } else {
            p.sendMessage("   §3" + Profit.get() + ": Xp" + (int) ((int) prize - bet));
            p.giveExp((int) prize);
            p.sendMessage("   §e" + MyBal.get() + ": Xp" + calcTotalExp(p));
        }
        Message_DivLower.t(p);

        activePlayerMap.remove(p.getUniqueId());

        if (headPos.containsKey(p.getUniqueId())) {
            ItemStack out = new ItemStack(getGlass(11));
            ItemStack head = gui().getMoneyIcon(headPos.get(p.getUniqueId()));
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

            gui().setBothIcon(headPos.get(p.getUniqueId()), out);
            headPos.remove(p.getUniqueId());
        }

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

    private void updateBankroll(BustaType type, Number amount) {
        if (type == BustaType.MONEY) {
            plugin.bank().plusDouble("Bankroll.Money", amount.doubleValue());
        } else {
            plugin.bank().plusInteger("Bankroll.Exp", amount.intValue());
        }

        if (config.isShowBankroll.val()) {
            ArrayList<String> lore = new ArrayList<>();
            double bankMoney = plugin.bank().getDouble("Bankroll.Money");
            int bankExp = plugin.bank().getInt("Bankroll.Exp");
            lore.add("§e" + config.currencySymbol + String.format("%.1f", bankMoney / 1000.0) + "K");
            lore.add("§eXp" + String.format("%.1f", bankExp / 1000.0) + "K");

            gui().updateBothIcon(45, lore);
        }

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
