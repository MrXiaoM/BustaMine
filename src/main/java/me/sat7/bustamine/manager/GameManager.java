package me.sat7.bustamine.manager;

import me.sat7.bustamine.BustaMine;
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
import static me.sat7.bustamine.Game.doubleFormat;
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

    final Map<String, String> playerMap = new HashMap<>();
    final Map<String, Integer> activePlayerMap = new ConcurrentHashMap<>();
    final Map<String, Integer> headPos = new HashMap<>();
    
    final GuiGame guiGame;
    final GuiBetSettings guiBetSettings;

    private String commandRoundStart, commandPlayerBet, commandPlayerCashOut, commandRoundEnd;

    public GameManager(BustaMine plugin) {
        this.plugin = plugin;
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
        maxMulti = plugin.ccConfig.get().getInt("MultiplierMax");
        baseInstaBust = Math.max(0, plugin.ccConfig.get().getDouble("ProbabilityOfInstaBust") / 100 - odd(maxMulti - 1));
        commandRoundStart = plugin.ccConfig.get().getString("Command.WhenRoundStart", "");
        commandPlayerBet = plugin.ccConfig.get().getString("Command.WhenPlayerBet", "");
        commandPlayerCashOut = plugin.ccConfig.get().getString("Command.WhenPlayerCashOut", "");
        commandRoundEnd = plugin.ccConfig.get().getString("Command.WhenRoundEnd", "");
        gui().reload();
        betSettings().reload();
    }

    public void setGameEnable(boolean gameEnable) {
        this.gameEnable = gameEnable;
    }

    public void runCommandRoundStart() {
        if (commandRoundStart.isEmpty()) return;
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), commandRoundStart);
    }

    public void runCommandBet(Player p, int amount) {
        if (commandPlayerBet.isEmpty()) return;
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), commandPlayerBet
                .replace("{player}", p.getName()).replace("{amount}", amount + ""));
    }

    public void runCommandCashOut(Player p, double amount, int multiplier, double prize) {
        if (commandPlayerCashOut.isEmpty()) return;
        double temp = multiplier / 100.0f;
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), commandPlayerCashOut
                .replace("{player}", p.getName())
                .replace("{amount}", amount + "")
                .replace("{multiplier}", temp + "")
                .replace("{prize}", prize + "")
        );
    }

    public void runCommandRoundEnd(int multiplier) {
        if (commandRoundEnd.isEmpty()) return;
        double temp = multiplier / 100.0f;
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), commandRoundEnd
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

        for (String s : activePlayerMap.keySet()) {
            try {
                UUID uuid = UUID.fromString(s);
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
        betTimeLeft = plugin.ccConfig.get().getInt("RoundInterval") + 1;

        if (plugin.ccConfig.get().getBoolean("ShowWinChance")) {
            ItemStack winChance = createItemStack(Material.getMaterial(plugin.ccConfig.get().getString("BtnIcon.WinChance")), null,
                    UI_WinChance.get(), null, 1);

            double bustchance = odd(plugin.ccConfig.get().getInt("MultiplierMax") - 1);
            ArrayList<String> winChanceArr = new ArrayList<>();
            winChanceArr.add("§ex2: " + doubleFormat.format((odd(1) - bustchance) * 100 * (1 - baseInstaBust)) + "%");
            winChanceArr.add("§ex3: " + doubleFormat.format((odd(2) - bustchance) * 100 * (1 - baseInstaBust)) + "%");
            winChanceArr.add("§ex7: " + doubleFormat.format((odd(6) - bustchance) * 100 * (1 - baseInstaBust)) + "%");
            winChanceArr.add("§ex12: " + doubleFormat.format((odd(11) - bustchance) * 100 * (1 - baseInstaBust)) + "%");
            winChanceArr.add("§eInstaBust: " + doubleFormat.format((bustchance + baseInstaBust) * 100) + "%");
            winChanceArr.add("§e" + MaximumMultiplier.get() + ": x" + plugin.ccConfig.get().getInt("MultiplierMax"));

            ItemMeta tempMeta = winChance.getItemMeta();
            tempMeta.setLore(winChanceArr);
            winChance.setItemMeta(tempMeta);
            gui().setBothIcon(46, winChance);
        } else {
            gui().setBothIcon(46, null);
        }

        if (plugin.ccConfig.get().getBoolean("ShowBankroll")) {
            if (gui().getMoneyIcon(45) == null) {
                //System.out.println("뱅크롤버튼 재생성");
                ItemStack bankrollBtn = createItemStack(Material.getMaterial(gui().btnBankroll), null,
                        UI_Bankroll.get(), null, 1);
                gui().setBothIcon(45, bankrollBtn);
            }
            updateBankroll(BustaType.MONEY, 0);
            updateBankroll(BustaType.EXP, 0);
        } else {
            gui().setBothIcon(45, null);
        }

        {
            ItemStack bet10Btn = createItemStack(Material.getMaterial(betSettings().btnBetSmall), null,
                    UI_BetBtn.get() + " §e" + plugin.ccConfig.get().getString("CurrencySymbol") + plugin.ccConfig.get().getInt("Bet.Small"), null, 1);
            gui().setMoneyIcon(51, bet10Btn);
            ItemStack betE1Btn = createItemStack(Material.getMaterial(betSettings().btnBetSmall), null,
                    UI_BetBtn.get() + " §eXp" + plugin.ccConfig.get().getInt("Bet.ExpSmall"), null, 1);
            gui().setExpIcon(51, betE1Btn);

            // 100
            ItemStack bet100Btn = createItemStack(Material.getMaterial(betSettings().btnBetMedium), null,
                    UI_BetBtn.get() + " §e" + plugin.ccConfig.get().getString("CurrencySymbol") + plugin.ccConfig.get().getInt("Bet.Medium"), null, 1);
            gui().setMoneyIcon(52, bet100Btn);
            ItemStack betE2Btn = createItemStack(Material.getMaterial(betSettings().btnBetMedium), null,
                    UI_BetBtn.get() + " §eXp" + plugin.ccConfig.get().getInt("Bet.ExpMedium"), null, 1);
            gui().setExpIcon(52, betE2Btn);

            // 1000
            ItemStack bet1000Btn = createItemStack(Material.getMaterial(betSettings().btnBetBig), null,
                    UI_BetBtn.get() + " §e" + plugin.ccConfig.get().getString("CurrencySymbol") + plugin.ccConfig.get().getInt("Bet.Big"), null, 1);
            gui().setMoneyIcon(53, bet1000Btn);
            ItemStack betE3Btn = createItemStack(Material.getMaterial(betSettings().btnBetBig), null,
                    UI_BetBtn.get() + " §eXp" + plugin.ccConfig.get().getInt("Bet.ExpBig"), null, 1);
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

            if (plugin.ccConfig.get().getBoolean("UIForceUpdate", false)) {
                for (String p : playerMap.keySet()) {
                    if (p == null)
                        continue;

                    Player player = Bukkit.getPlayer(p);
                    if (player != null)
                        player.updateInventory();
                }
            }

        }, 0, gameLoopDelay);
    }

    private void bust(boolean instaBust) {
        bState = BustaState.BUSTED;
        if (instaBust) curNum = 100;

        runCommandRoundEnd(curNum);

        if (instaBust && plugin.ccConfig.get().getBoolean("Broadcast.InstaBust")) {
            Bukkit.getServer().broadcastMessage(prefix() + Message_Instabust.get());
        }
        if (plugin.ccConfig.get().getInt("Broadcast.Jackpot") * 100 <= curNum) {
            Bukkit.getServer().broadcastMessage(prefix() + "§a§lBusted! : x" + doubleFormat.format(curNum / 100.0));
        }

        for (String s : playerMap.keySet()) {
            try {
                for (String bustP : activePlayerMap.keySet()) {
                    try {
                        UUID uuid = UUID.fromString(s);
                        UUID uuidBust = UUID.fromString(bustP);
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
        for (String bustP : activePlayerMap.keySet()) {
            try {
                UUID uuidBust = UUID.fromString(bustP);
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

        // 데이터 저장
        plugin.ccBank.save();
        plugin.users().save();
    }

    public void bet(Player p, BustaType type, int amount) {
        if (bState != BustaState.BET) return;

        boolean firstBet = !activePlayerMap.containsKey(p.getUniqueId().toString());
        int old = 0;
        if (activePlayerMap.containsKey(p.getUniqueId().toString()))
            old = activePlayerMap.get(p.getUniqueId().toString());

        if (playerMap.containsKey(p.getUniqueId().toString()) && !playerMap.get(p.getUniqueId().toString()).equals(type.toString())) {
            return;
        }

        if (type == BustaType.MONEY) {
            if (old + amount > plugin.ccConfig.get().getInt("Bet.Max")) {
                BettingLimit.t(p);
                return;
            }

            Economy economy = plugin.getEconomy();
            if (economy.getBalance(p) >= amount) {
                EconomyResponse r = economy.withdrawPlayer(p, amount);

                if (!r.transactionSuccess()) {
                    p.sendMessage(String.format("An error occured: %s", r.errorMessage));
                    return;
                }

                plugin.sounds().play(p, "Bet");
                runCommandBet(p, amount);
                activePlayerMap.put(p.getUniqueId().toString(), old + amount);
                Message_DivUpper.t(p);
                p.sendMessage("   §f" + Bet.get() + plugin.ccConfig.get().getString("CurrencySymbol") + (old + amount));
                p.sendMessage("   §e" + MyBal.get() + ": " + plugin.ccConfig.get().getString("CurrencySymbol") + doubleFormat.format(economy.getBalance(p)));
                Message_DivLower.t(p);

                if (firstBet) {
                    for (String s : playerMap.keySet()) {
                        if (p.getUniqueId().toString().equals(s)) continue;
                        try {
                            UUID uuid = UUID.fromString(s);
                            Bukkit.getPlayer(uuid).sendMessage("§6♣ " + p.getName() + " " + Bet.get() + plugin.ccConfig.get().getString("CurrencySymbol") + doubleFormat.format(old + amount));
                        } catch (Exception ignored) {
                        }
                    }
                }
            } else {
                Message_NotEnoughMoney.t(p);
                p.sendMessage(MyBal.get() + ": " + plugin.ccConfig.get().getString("CurrencySymbol") + doubleFormat.format(plugin.getEconomy().getBalance(p)));
                return;
            }
        }
        else {
            if (old + amount > plugin.ccConfig.get().getInt("Bet.ExpMax")) {
                BettingLimit.t(p);
                return;
            }

            if (calcTotalExp(p) >= amount) {
                p.giveExp(-amount);

                runCommandBet(p, amount);
                activePlayerMap.put(p.getUniqueId().toString(), old + amount);
                Message_DivUpper.t(p);
                p.sendMessage("   §f" + Bet.get() + " Xp" + (old + amount));
                p.sendMessage("   §e" + MyBal.get() + ": Xp" + calcTotalExp(p));
                Message_DivLower.t(p);

                if (firstBet) {
                    for (String s : playerMap.keySet()) {
                        if (p.getUniqueId().toString().equals(s)) continue;
                        try {
                            UUID uuid = UUID.fromString(s);
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

        updateBankroll(BustaType.valueOf(type.toString()), amount);
        updateNetProfit(p, BustaType.valueOf(type.toString()), -amount);

        if (firstBet) {
            playerMap.put(p.getUniqueId().toString(), type.toString());

            User user = plugin.users().get(p);
            user.setGamesPlayed(user.getGamesPlayed() + 1);

            if (playerMap.size() < 43) {
                Material m = Material.PLAYER_HEAD;

                ItemStack skull = new ItemStack(m, 1);
                ItemMeta meta = skull.getItemMeta();

                if (plugin.ccConfig.get().getBoolean("LoadPlayerSkin")) {
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadAndSetSkin(p, playerMap.size() - 1));
                }

                if (meta != null) {
                    meta.setDisplayName("§6" + p.getName());
                    ArrayList<String> lore = new ArrayList<>();
                    if (type == BustaType.MONEY) {
                        lore.add(UI_PlayerInfo.get().replace("{amount}", plugin.ccConfig.get().getString("CurrencySymbol") + amount));
                    } else {
                        lore.add(UI_PlayerInfo.get().replace("{amount}", "Xp" + amount));
                    }
                    meta.setLore(lore);
                    skull.setItemMeta(meta);
                }
                gui().setBothIcon(playerMap.size() - 1, skull);

                headPos.put(p.getUniqueId().toString(), playerMap.size() - 1);
            }
        } else {
            try {
                if (headPos.containsKey(p.getUniqueId().toString())) {
                    int idx = headPos.get(p.getUniqueId().toString());
                    ItemStack item = gui().getMoneyIcon(idx);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        ArrayList<String> lore = new ArrayList<>();
                        if (type == BustaType.MONEY) {
                            lore.add(UI_PlayerInfo.get().replace("{amount}", plugin.ccConfig.get().getString("CurrencySymbol") + (old + amount)));
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
        if (!activePlayerMap.containsKey(p.getUniqueId().toString())) return;
        if (bState != BustaState.GAME) return;

        double bet = activePlayerMap.get(p.getUniqueId().toString());
        double prize = bet * (curNum / 100.0);

        runCommandCashOut(p, bet, curNum, prize);
        plugin.sounds().play(p, "CashOut");

        Message_DivUpper.t(p);
        p.sendMessage("   §f" + CashedOut.get() + ": x" + doubleFormat.format(curNum / 100.0));
        if (BustaType.valueOf(playerMap.get(p.getUniqueId().toString())) == BustaType.MONEY) {
            p.sendMessage("   §3" + Profit.get() + ": " + plugin.ccConfig.get().getString("CurrencySymbol") + doubleFormat.format(prize - bet));
            plugin.getEconomy().depositPlayer(p, prize);
            p.sendMessage("   §e" + MyBal.get() + ": " + plugin.ccConfig.get().getString("CurrencySymbol") + doubleFormat.format(plugin.getEconomy().getBalance(p)));
        } else {
            p.sendMessage("   §3" + Profit.get() + ": Xp" + (int) ((int) prize - bet));
            p.giveExp((int) prize);
            p.sendMessage("   §e" + MyBal.get() + ": Xp" + calcTotalExp(p));
        }
        Message_DivLower.t(p);

        activePlayerMap.remove(p.getUniqueId().toString());

        if (headPos.containsKey(p.getUniqueId().toString())) {
            ItemStack out = new ItemStack(getGlass(11));
            ItemStack head = gui().getMoneyIcon(headPos.get(p.getUniqueId().toString()));

            ArrayList<String> lore = new ArrayList<>(head.getItemMeta().getLore());
            lore.add("§f" + CashedOut.get() + ": x" + doubleFormat.format(curNum / 100.0));

            ItemMeta meta = out.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(head.getItemMeta().getDisplayName());
                meta.setLore(lore);
                out.setItemMeta(meta);
            }

            gui().setBothIcon(headPos.get(p.getUniqueId().toString()), out);
            headPos.remove(p.getUniqueId().toString());
        }

        for (String s : playerMap.keySet()) {
            if (p.getUniqueId().toString().equals(s)) continue;
            try {
                UUID uuid = UUID.fromString(s);
                Bukkit.getPlayer(uuid).sendMessage("§6♣ " + p.getName() + " " + CashedOut.get() + " x" + doubleFormat.format(curNum / 100.0));
            } catch (Exception ignored) {
            }
        }

        updateBankroll(BustaType.valueOf(playerMap.get(p.getUniqueId().toString())), -prize);
        updateNetProfit(p, BustaType.valueOf(playerMap.get(p.getUniqueId().toString())), prize);
    }

    private void updateBankroll(BustaType type, double amount) {
        double old;
        if (type == BustaType.MONEY) {
            old = plugin.ccBank.get().getDouble("Bankroll.Money");
            plugin.ccBank.get().set("Bankroll.Money", old + amount);
        } else {
            old = plugin.ccBank.get().getInt("Bankroll.Exp");
            plugin.ccBank.get().set("Bankroll.Exp", (int) (old + amount));
        }

        if (plugin.ccConfig.get().getBoolean("ShowBankroll")) {
            ArrayList<String> lore = new ArrayList<>();
            lore.add("§e" + plugin.ccConfig.get().getString("CurrencySymbol") + (int) (plugin.ccBank.get().getDouble("Bankroll.Money") / 1000) + "K");
            lore.add("§eXp" + (plugin.ccBank.get().getInt("Bankroll.Exp") / 1000) + "K");

            gui().updateBothIcon(45, lore);
        }

        if (amount > 0) {
            if (type == BustaType.MONEY) {
                plugin.ccBank.get().set("Statistics.Income.Money", plugin.ccBank.get().getDouble("Statistics.Income.Money") + amount);
            } else {
                plugin.ccBank.get().set("Statistics.Income.Exp", (int) (plugin.ccBank.get().getInt("Statistics.Income.Exp") + amount));
            }
        } else {
            if (type == BustaType.MONEY) {
                plugin.ccBank.get().set("Statistics.Expense.Money", plugin.ccBank.get().getDouble("Statistics.Expense.Money") + amount);
            } else {
                plugin.ccBank.get().set("Statistics.Expense.Exp", (int) (plugin.ccBank.get().getInt("Statistics.Expense.Exp") + amount));
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
