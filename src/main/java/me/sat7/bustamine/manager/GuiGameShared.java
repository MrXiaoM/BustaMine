package me.sat7.bustamine.manager;

import me.sat7.bustamine.BustaMine;
import me.sat7.bustamine.config.Config;
import me.sat7.bustamine.data.User;
import me.sat7.bustamine.manager.enums.BustaState;
import me.sat7.bustamine.manager.enums.BustaType;
import me.sat7.bustamine.manager.gui.BustaGuiHolder;
import me.sat7.bustamine.manager.gui.IBustaMineGui;
import me.sat7.bustamine.utils.Item;
import me.sat7.bustamine.utils.Util;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static me.sat7.bustamine.BustaMine.log;
import static me.sat7.bustamine.config.Messages.*;
import static me.sat7.bustamine.utils.Util.*;

/**
 * 共享游戏菜单，打开方式如下
 * <ul>
 *     <li><code>/bm</code> 打开金币或经验股市</li>
 *     <li><code>/bm exp</code> 打开经验股市</li>
 *     <li><code>/bm money</code> 打开金币股市</li>
 * </ul>
 */
public class GuiGameShared implements Listener {
    private final BustaMine plugin;
    private final GameManager parent;
    private final Config config;

    private Inventory gameInventory;
    private Inventory gameInventory_exp;

    private BustaState bustaState = BustaState.BET;

    protected final List<Integer> history = new ArrayList<>();
    protected final Map<Integer, ItemStack> old = new HashMap<>();

    /**
     * 玩家当前的游戏类型，金币还是经验
     */
    private final Map<UUID, BustaType> playerMap = new HashMap<>();
    /**
     * 玩家下注数量
     */
    private final Map<UUID, Integer> activePlayerMap = new ConcurrentHashMap<>();
    /**
     * 玩家头颅物品在菜单上的位置索引
     */
    private final Map<UUID, Integer> headPos = new HashMap<>();

    int betExpSmall, betExpMedium, betExpBig;
    int betMoneySmall, betMoneyMedium, betMoneyBig;


    public GuiGameShared(GameManager parent) {
        this.parent = parent;
        this.plugin = parent.plugin();
        this.config = parent.plugin().config();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void reset() {
        playerMap.clear();
        activePlayerMap.clear();
        headPos.clear();

        bustaState = BustaState.BET;
    }

    public void reload() {
        betExpSmall = config.betExpSmall.val();
        betExpMedium = config.betExpMedium.val();
        betExpBig = config.betExpBig.val();
        betMoneySmall = config.betSmall.val();
        betMoneyMedium = config.betMedium.val();
        betMoneyBig = config.betBig.val();

        refreshIcons();
    }

    public void gameGUISetup() {
        IBustaMineGui.closeAllGameUI();
        createGameGUI(BustaType.MONEY);
        createGameGUI(BustaType.EXP);
    }

    public void updateBothIcon(int index, ItemMeta meta) {
        ItemStack item1 = gameInventory.getItem(index);
        ItemStack item2 = gameInventory_exp.getItem(index);
        if (item1 != null) item1.setItemMeta(meta);
        if (item2 != null) item2.setItemMeta(meta);
    }

    public void updateBothIcon(int index, List<String> lore) {
        ItemStack item1 = gameInventory.getItem(index);
        ItemStack item2 = gameInventory_exp.getItem(index);
        ItemMeta meta1 = item1 == null ? null : item1.getItemMeta();
        ItemMeta meta2 = item2 == null ? null : item2.getItemMeta();
        if (meta1 != null) {
            meta1.setLore(lore);
            item1.setItemMeta(meta1);
        }
        if (meta2 != null) {
            meta2.setLore(lore);
            item2.setItemMeta(meta2);
        }
    }

    public void setBothIcon(int index, ItemStack item) {
        setMoneyIcon(index, item);
        setExpIcon(index, item);
    }

    public ItemStack getMoneyIcon(int index) {
        return gameInventory.getItem(index);
    }

    public void setMoneyIcon(int index, ItemStack item) {
        gameInventory.setItem(index, item);
    }

    public void setExpIcon(int index, ItemStack item) {
        gameInventory_exp.setItem(index, item);
    }

    public Set<UUID> activePlayers() {
        return activePlayerMap.keySet();
    }

    public Set<UUID> inGamePlayers() {
        return playerMap.keySet();
    }

    public void restoreOldIcons() {
        for (Integer key : old.keySet()) {
            setBothIcon(key, old.get(key));
        }
    }

    public boolean containsHeadSlot(int slot) {
        return headPos.containsValue(slot);
    }

    public void setBustaState(BustaState bustaState) {
        this.bustaState = bustaState;
    }

    private void createGameGUI(BustaType type) {
        String title = UI_Title.get();
        if (type == BustaType.MONEY) {
            title = title + " " + Money.get();
        } else {
            title = title + " " + Exp.get();
        }
        Inventory inv = new BustaGuiHolder(type, 54, title).getInventory();

        if (config.isShowBankroll.val()) {
            ItemStack bankrollBtn = createItemStack(config.btnBankroll, null,
                    UI_Bankroll.get(), null, 1);
            flag(bankrollBtn, "bankroll");
            inv.setItem(45, bankrollBtn);
        }

        ArrayList<String> myStateLore = new ArrayList<>();
        myStateLore.add(UI_Click.get());
        ItemStack myStateBtn = createItemStack(config.btnMyState, null,
                UI_MyState.get(), myStateLore, 1);
        flag(myStateBtn, "my state");
        inv.setItem(47, myStateBtn);

        ItemStack historyBtn = createItemStack(config.btnHistory, null,
                UI_History.get(), null, 1);
        flag(historyBtn, "history");
        inv.setItem(48, historyBtn);

        ItemStack cashOutBtn = createItemStack(config.btnCashOut, null,
                UI_CashOut.get(), null, 1);
        flag(cashOutBtn, "cash out");
        inv.setItem(49, cashOutBtn);

        ItemStack showBetSettingBtn = createItemStack(config.btnCashOutSetting, null,
                UI_CashOutSetting.get(), null, 1);
        flag(showBetSettingBtn, "show bet setting");
        inv.setItem(50, showBetSettingBtn);

        if (type == BustaType.MONEY) {
            gameInventory = inv;
        } else {
            gameInventory_exp = inv;
        }
    }

    public void openGameGUI(Player p, BustaType type) {
        if (type == BustaType.MONEY) {
            p.openInventory(gameInventory);
        } else {
            p.openInventory(gameInventory_exp);
        }
    }

    private void refreshIcons() {
        String[] iconIds = new String[]{"Bankroll", "WinChance", "MyState", "History", "CashOut"};
        //45 46 47 48 49
        for (int i = 45; i <= 49; i++) {
            ItemStack oldItem = getMoneyIcon(i);
            if (oldItem == null) continue;

            ItemMeta meta = oldItem.getItemMeta();

            String material = plugin.config().getString("BtnIcon." + iconIds[i - 45]);
            ItemStack item = Item.fromString(material, Material.PAPER).newItem(oldItem.getAmount());
            item.setItemMeta(meta);

            setBothIcon(i, item);
        }
    }

    void drawNumber(int num) {
        for (Integer key : old.keySet()) {
            setBothIcon(key, old.get(key));
        }
        old.clear();

        int[] slots;
        if (num == 5) {
            slots = new int[] { 5, 4, 3, 12, 21, 22, 23, 32, 39, 40, 41 };
        } else if (num == 4) {
            slots = new int[] { 3, 12, 21, 22, 23, 14, 5, 32, 41 };
        } else if (num == 3) {
            slots = new int[] { 3, 4, 5, 14, 21, 22, 23, 32, 39, 40, 41 };
        } else if (num == 2) {
            slots = new int[] { 3, 4, 5, 14, 23, 22, 21, 30, 39, 40, 41 };
        } else if (num == 1) {
            slots = new int[] { 5, 14, 23, 32, 41 };
        } else {
            slots = new int[] { 3, 4, 5, 14, 23, 32, 41, 40, 39, 30, 21, 12 };
        }

        for (int j : slots) {
            old.put(j, getMoneyIcon(j));
            setBothIcon(j, getGlassItem(0));
        }
    }

    @EventHandler
    @SuppressWarnings({"IfCanBeSwitch", "UnnecessaryReturnStatement"})
    public void onClick(InventoryClickEvent e) {
        if (e.isCancelled() || !(e.getWhoClicked() instanceof Player)) return;
        InventoryView view = e.getView();
        Player player = (Player) e.getWhoClicked();
        if (view.getTopInventory().getHolder() instanceof BustaGuiHolder) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            if (e.getClickedInventory() == null || item == null) return;
            BustaType type = ((BustaGuiHolder) view.getTopInventory().getHolder()).getType();

            plugin.sounds().play(player, "Click");

            String flag = flag(item);
            if (flag.startsWith("bet:")) { // 51, 52, 53
                int amount;
                String button = flag.substring(4);
                if (type.equals(BustaType.EXP)) switch (button) {
                    case "small":  amount = betExpSmall;  break;
                    case "medium": amount = betExpMedium; break;
                    case "big":    amount = betExpBig;    break;
                    default:       return;
                } else switch (button) {
                    case "small":  amount = betMoneySmall;  break;
                    case "medium": amount = betMoneyMedium; break;
                    case "big":    amount = betMoneyBig;    break;
                    default:       return;
                }
                bet(player, type, amount);
                return;
            }
            if (flag.equals("show bet setting")) { // 50
                parent.guiBetSettings().showBetSettingUI(player);
                return;
            }
            if (flag.equals("cash out")) { // 49
                cashOut(player);
                return;
            }
            if (flag.equals("my state")) { // 47
                plugin.users().showPlayerInfo(player, player);
                return;
            }
            if (flag.startsWith("show player info:")) { // < 45
                Player p = Bukkit.getPlayer(flag.substring(17));
                if (p != null) plugin.users().showPlayerInfo(player, p);
                return;
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof BustaGuiHolder) {
            e.setCancelled(true);
        }
    }

    /**
     * 归零
     * @param instantBust 是否立即归零
     */
    void bust(boolean instantBust) {
        bustaState = BustaState.BUSTED;
        if (instantBust) parent.curNum(100);

        parent.runCommandRoundEnd(parent.curNum());

        if (!Bukkit.getOnlinePlayers().isEmpty()) {
            if (instantBust && config.isBroadcastInstaBust.val()) {
                Bukkit.getServer().broadcastMessage(prefix() + Message_InstantBust.get());
            }
            if (config.broadcastJackpot.val() * 100 <= parent.curNum()) {
                Bukkit.getServer().broadcastMessage(prefix() + "§a§lBusted! : x" + parent.curNumFormatted());
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
        newLore.add("§c§lx" + parent.curNumFormatted());

        updateBothIcon(49, newLore);

        for (int i = 0; i < 45; i++) {
            if (!headPos.containsValue(i)) {
                setBothIcon(i, getGlassItem(14));
            }
        }

        for (int i = 51; i <= 53; i++) {
            setBothIcon(i, getGlassItem(14));
        }

        history.add(parent.curNum());
        if (history.size() > 16) history.remove(0);

        ArrayList<String> historyLore = new ArrayList<>();
        for (int i : history) {
            if (i >= 200) {
                historyLore.add("§ax" + doubleFormat.format(i / 100.0));
            } else {
                historyLore.add("§cx" + doubleFormat.format(i / 100.0));
            }
        }

        updateBothIcon(48, historyLore);

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
        if (bustaState != BustaState.BET) return;

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
                parent.runCommandBet(p, amount);
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

                parent.runCommandBet(p, amount);
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

        parent.updateBankroll(type, amount);
        parent.updateNetProfit(p, type, -amount);

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

                setBothIcon(idx, skull);

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
                updateBothIcon(idx, lore);
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
        if (bustaState != BustaState.GAME) return;

        // 要求已下注，获取并清空玩家的下注金额
        Integer bet = activePlayerMap.remove(p.getUniqueId());
        if (bet == null) return;

        double prize = bet * (parent.curNum() / 100.0);

        parent.runCommandCashOut(p, bet, parent.curNum(), prize);
        plugin.sounds().play(p, "CashOut");

        // 进行抛售操作，给予玩家奖励的金币或经验
        Message_DivUpper.t(p);
        p.sendMessage("   §f" + CashedOut.get() + ": x" + parent.curNumFormatted());
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
            ItemStack head = getMoneyIcon(headPos.get(p.getUniqueId()));
            ItemMeta headMeta = head.getItemMeta();
            List<String> oldLore = headMeta != null ? headMeta.getLore() : null;

            List<String> lore = new ArrayList<>();
            if (oldLore != null) lore.addAll(oldLore);
            lore.add("§f" + CashedOut.get() + ": x" + parent.curNumFormatted());

            ItemMeta meta = out.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(head.getItemMeta().getDisplayName());
                meta.setLore(lore);
                out.setItemMeta(meta);
            }

            flag(out, "show player info:" + p.getName());
            setBothIcon(headPos.get(p.getUniqueId()), out);
            headPos.remove(p.getUniqueId());
        }

        // 向游戏中所有玩家广播 抛售通知
        for (UUID uuid : playerMap.keySet()) {
            if (p.getUniqueId().equals(uuid)) continue;
            try {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.sendMessage("§6♣ " + p.getName() + " " + CashedOut.get() + " x" + parent.curNumFormatted());
                }
            } catch (Exception ignored) {
            }
        }

        parent.updateBankroll(playerMap.get(p.getUniqueId()), -prize);
        parent.updateNetProfit(p, playerMap.get(p.getUniqueId()), prize);
    }
}
