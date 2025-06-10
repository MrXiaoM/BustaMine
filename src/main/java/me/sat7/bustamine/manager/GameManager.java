package me.sat7.bustamine.manager;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import me.sat7.bustamine.BustaMine;
import me.sat7.bustamine.config.Config;
import me.sat7.bustamine.data.User;
import me.sat7.bustamine.manager.enums.BustaState;
import me.sat7.bustamine.manager.enums.BustaType;
import me.sat7.bustamine.manager.gui.IBustaMineGui;
import me.sat7.bustamine.utils.BustaIcon;
import me.sat7.bustamine.utils.ListPair;
import me.sat7.bustamine.utils.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

import static me.sat7.bustamine.utils.Util.*;

public class GameManager implements Listener {
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

    protected int maxMulti = 150;
    protected double baseInstantBust = 0;

    private final GuiGameShared guiGameShared;
    private final GuiBetSettings guiBetSettings;
    private final Config config;

    public GameManager(BustaMine plugin) {
        this.plugin = plugin;
        this.config = plugin.config();
        this.guiGameShared = new GuiGameShared(this);
        this.guiBetSettings = new GuiBetSettings(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void setup() {
        File folder = plugin.resolve("gui");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        guiGameShared().setup();
        guiBetSettings().setup();
    }

    public void reload() {
        maxMulti = config.multiplierMax.val();
        baseInstantBust = Math.max(0, config.probabilityOfInstaBust.val() / 100 - odd(maxMulti - 1));
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
                if (user.getAutoCashOut() >= 0) { // 如果玩家自动抛售倍率大于0，且当前倍率大于等于自动抛售倍率，则执行抛售
                    if (newNum >= user.getAutoCashOut()) {
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

        guiGameShared().updateStartGameUI();

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
        }, 1L, 20L);
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

            // 更新当前倍率到 lore
            guiGameShared().updateCurNumToCashIcon();

            if (config.isForceUpdateUI.val()) {
                // 强制更新玩家界面
                for (UUID uuid : guiGameShared().inGamePlayers()) {
                    Player player = uuid == null ? null : Bukkit.getPlayer(uuid);
                    if (player != null) {
                        Util.updateInventory(player);
                    }
                }
            }

        }, 1L, gameLoopDelay);
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
            ListPair<String, Object> replacements = new ListPair<>();
            replacements.add("%bank_money%", plugin.bank().getDouble("Bankroll.Money"));
            replacements.add("%bank_exp%", plugin.bank().getInt("Bankroll.Exp"));
            BustaIcon btnBankroll = guiGameShared().btnBankroll.val();
            ItemStack item = btnBankroll.generateItem(replacements, "bankroll");
            guiGameShared().setBothIcon(btnBankroll.getSlot(), item);
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

    private boolean isBustaGui(InventoryView view) {
        return view.getTopInventory().getHolder() instanceof IBustaMineGui;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.isCancelled()) return;
        ItemStack item = e.getCurrentItem();
        if (item == null || isBustaGui(e.getView())) return;
        if (!flag(item).isEmpty()) {
            item.setType(Material.AIR);
            e.setCurrentItem(null);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        ItemStack item = e.getItem();
        if (item != null && !isBustaGui(player.getOpenInventory())) {
            if (!flag(item).isEmpty()) {
                item.setType(Material.AIR);
            }
        }
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent e) {
        if (e.isCancelled()) return;
        Player player = e.getPlayer();
        if (isBustaGui(player.getOpenInventory())) return;
        ItemStack item = e.getItemDrop().getItemStack();
        if (!flag(item).isEmpty()) {
            item.setType(Material.AIR);
            e.getItemDrop().remove();
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerPickup(EntityPickupItemEvent e) {
        if (e.isCancelled()) return;
        Entity entity = e.getEntity();
        if (entity instanceof Player) {
            if (isBustaGui(((Player) entity).getOpenInventory())) return;
        }
        ItemStack item = e.getItem().getItemStack();
        if (!flag(item).isEmpty()) {
            item.setType(Material.AIR);
            e.getItem().remove();
            e.setCancelled(true);
        }
    }
}
