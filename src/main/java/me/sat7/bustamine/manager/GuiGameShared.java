package me.sat7.bustamine.manager;

import com.google.common.collect.Lists;
import me.sat7.bustamine.config.Config;
import me.sat7.bustamine.data.User;
import me.sat7.bustamine.manager.enums.BustaState;
import me.sat7.bustamine.manager.enums.BustaType;
import me.sat7.bustamine.manager.gui.BustaGuiHolder;
import me.sat7.bustamine.manager.gui.IBustaMineGui;
import me.sat7.bustamine.utils.*;
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
import static me.sat7.bustamine.utils.BustaIcon.*;
import static me.sat7.bustamine.utils.Property.property;
import static me.sat7.bustamine.utils.Util.*;

/**
 * 共享游戏菜单，打开方式如下
 * <ul>
 *     <li><code>/bm</code> 打开金币或经验股市</li>
 *     <li><code>/bm exp</code> 打开经验股市</li>
 *     <li><code>/bm money</code> 打开金币股市</li>
 * </ul>
 */
public class GuiGameShared extends CustomConfig implements Listener, Property.IPropertyRegistry {
    private final List<Property<?>> registeredProperties = new ArrayList<>();
    private final GameManager parent;
    private final Config config;

    public final Property<String> titleMoney = property(this, "title-money", "&2[ Busta Mine ] &3金币");
    public final Property<String> titleExp = property(this, "title-exp", "&2[ Busta Mine ] &3经验");
    public final Property<Integer> inventorySize = property(this, "inventory-size", 54);
    private final Property<List<String>> holdPlayerArea = property(this, "hold-player-area", Lists.newArrayList(
            "#########",
            "#########",
            "#########",
            "#########",
            "#########",
            "         "
    ));
    private boolean[] holdPlayerSlots;
    private int maxPlayers;
    public final Property<BustaIcon> btnBankroll = propertyIcon(this, "icons.bankroll", def()
            .slot(45)
            .material(Material.DIAMOND)
            .display("&6&l资金")
            .lore(
                    "&e%bank_money%K",
                    "&eXp%bank_exp%K"
            ));
    public final Property<BustaIcon> btnWinChance = propertyIcon(this, "icons.win-chance", def()
            .slot(46)
            .material(Material.PAPER)
            .display("&6&l获胜几率")
            .lore(
                    "&ex2: %x2%%",
                    "&ex3: %x3%%",
                    "&ex7: %x7%%",
                    "&ex12: %x12%%",
                    "&e立即归零: %instant_bust%%",
                    "&e最大倍数: x%max_multi%"
            ));
    public final Property<BustaIcon> btnPlayerHeadMoney = propertyIcon(this, "icons.player-head-money", def()
            .material(Material.BLUE_STAINED_GLASS_PANE)
            .display("&6%player%")
            .lore("&f赌注:&e %amount%金币"));
    public final Property<BustaIcon> btnPlayerHeadExp = propertyIcon(this, "icons.player-head-exp", def()
            .material(Material.BLUE_STAINED_GLASS_PANE)
            .display("&6%player%")
            .lore("&f赌注:&e %amount%经验"));
    public final Property<List<String>> btnPlayerHeadMoneyInGame = property(this, "icons.player-head-money.lore-in-game", Lists.newArrayList(
            "&f赌注:&e %amount%金币",
            "&f出售: x%cur_num%"
    ));
    public final Property<List<String>> btnPlayerHeadExpInGame = property(this, "icons.player-head-money.lore-in-game", Lists.newArrayList(
            "&f赌注:&e %amount%经验",
            "&f出售: x%cur_num%"
    ));
    public final Property<BustaIcon> btnMyState = propertyIcon(this, "icons.my-state", def()
            .slot(47)
            .material(Material.PAPER)
            .display("&6&l我的交易状态")
            .lore());
    public final Property<BustaIcon> btnHistory = propertyIcon(this, "icons.history", def()
            .slot(48)
            .material(Material.PAPER)
            .display("&6&l历史倍数")
            .lore());
    public final Property<BustaIcon> btnCashOut = propertyIcon(this, "icons.cash-out", def()
            .slot(49)
            .material(Material.EMERALD)
            .display("&6&l抛售")
            .lore());
    public final Property<List<String>> btnCashOutLoreBet = property(this, "icons.cash-out.lore-bet", Lists.newArrayList(
            "&e&l>> 在右边的图标买入"
    ));
    public final Property<List<String>> btnCashOutLoreGame = property(this, "icons.cash-out.lore-game", Lists.newArrayList(
            "&c&lx%cur_num%"
    ));
    public final Property<BustaIcon> btnCashOutSetting = propertyIcon(this, "icons.cash-out-setting", def()
            .slot(50)
            .material(Material.PAPER)
            .display("&6&l自动抛售")
            .lore());
    public final Property<BustaIcon> btnBetMoneySmall = propertyIcon(this, "icons.bet-money-small", def()
            .slot(51)
            .material(Material.GOLD_NUGGET)
            .display("&6&l买入 &e%money%金币")
            .lore());
    public final Property<BustaIcon> btnBetMoneyMedium = propertyIcon(this, "icons.bet-money-medium", def()
            .slot(52)
            .material(Material.GOLD_INGOT)
            .display("&6&l买入 &e%money%金币")
            .lore());
    public final Property<BustaIcon> btnBetMoneyBig = propertyIcon(this, "icons.bet-money-big", def()
            .slot(53)
            .material(Material.GOLD_BLOCK)
            .display("&6&l买入 &e%money%金币")
            .lore());
    public final Property<BustaIcon> btnBetExpSmall = propertyIcon(this, "icons.bet-exp-small", def()
            .slot(51)
            .material(Material.GOLD_NUGGET)
            .display("&6&l买入 &e%money%经验")
            .lore());
    public final Property<BustaIcon> btnBetExpMedium = propertyIcon(this, "icons.bet-exp-medium", def()
            .slot(52)
            .material(Material.GOLD_INGOT)
            .display("&6&l买入 &e%money%经验")
            .lore());
    public final Property<BustaIcon> btnBetExpBig = propertyIcon(this, "icons.bet-exp-big", def()
            .slot(53)
            .material(Material.GOLD_BLOCK)
            .display("&6&l买入 &e%money%经验")
            .lore());

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
        super(parent.plugin());
        this.parent = parent;
        this.config = parent.plugin().config();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void registerProperty(Property<?> property) {
        registeredProperties.add(property);
    }

    public void setup() {
        setup("gui/shared", config -> {
            for (Property<?> property : registeredProperties) {
                property.setup();
            }
            config.options().copyDefaults(true);
        });
        save();
    }

    public void reset() {
        playerMap.clear();
        activePlayerMap.clear();
        headPos.clear();

        bustaState = BustaState.BET;
    }

    public void reloadConfig() {
        reload();
        for (Property<?> property : registeredProperties) {
            property.reload();
        }
        char[] inventory = String.join("", holdPlayerArea.val()).toCharArray();
        holdPlayerSlots = new boolean[inventory.length];
        maxPlayers = 0;
        for (int i = 0; i < inventory.length; i++) {
            boolean isInArea = inventory[i] == '#';
            holdPlayerSlots[i] = isInArea;
            if (isInArea) {
                maxPlayers++;
            }
        }

        betExpSmall = config.betExpSmall.val();
        betExpMedium = config.betExpMedium.val();
        betExpBig = config.betExpBig.val();
        betMoneySmall = config.betSmall.val();
        betMoneyMedium = config.betMedium.val();
        betMoneyBig = config.betBig.val();
    }

    public void gameGUISetup() {
        IBustaMineGui.closeAllGameUI();
        createGameGUI(BustaType.MONEY);
        createGameGUI(BustaType.EXP);
    }

    public void setBustaState(BustaState bustaState) {
        this.bustaState = bustaState;
    }

    private void createGameGUI(BustaType type) {
        String title;
        if (type == BustaType.MONEY) {
            title = titleMoney.val();
        } else {
            title = titleExp.val();
        }
        Inventory inv = new BustaGuiHolder(type, inventorySize.val(), color(title)).getInventory();

        btnMyState.val().set(inv, null, "my state");
        btnHistory.val().set(inv, null, "history");
        btnCashOut.val().set(inv, null, "cash out");
        btnCashOutSetting.val().set(inv, null, "show bet setting");

        if (type == BustaType.MONEY) {
            gameInventory = inv;
        } else {
            gameInventory_exp = inv;
        }
    }

    public void updateStartGameUI() {
        // 显示胜率公示
        BustaIcon btnWinChance = this.btnWinChance.val();
        if (config.isShowWinChance.val()) {
            int maxMulti = parent.maxMulti;
            double baseInstantBust = parent.baseInstantBust;
            double bustChance = odd(maxMulti - 1);
            ListPair<String, Object> replacements = new ListPair<>();
            replacements.add("%x2%", doubleFormat.format((odd(1) - bustChance) * 100 * (1 - baseInstantBust)));
            replacements.add("%x3%", doubleFormat.format((odd(2) - bustChance) * 100 * (1 - baseInstantBust)));
            replacements.add("%x7%", doubleFormat.format((odd(6) - bustChance) * 100 * (1 - baseInstantBust)));
            replacements.add("%x12%", doubleFormat.format((odd(11) - bustChance) * 100 * (1 - baseInstantBust)));
            replacements.add("%instant_bust%", doubleFormat.format((bustChance + baseInstantBust) * 100));
            replacements.add("%max_multi%", maxMulti);
            ItemStack winChance = btnWinChance.generateItem(replacements, "win chance");

            setBothIcon(btnWinChance.getSlot(), winChance);
        } else {
            setBothIcon(btnWinChance.getSlot(), null);
        }

        // 显示资金
        if (config.isShowBankroll.val()) {
            parent.updateBankroll(BustaType.MONEY, 0);
            parent.updateBankroll(BustaType.EXP, 0);
        } else {
            setBothIcon(btnBankroll.val().getSlot(), null);
        }

        // 添加下注按钮
        // 10
        btnBetMoneySmall.val().set(gameInventory,
                Lists.newArrayList(Pair.of("%money%", config.betSmall)),
                "bet:small");
        btnBetExpSmall.val().set(gameInventory_exp,
                Lists.newArrayList(Pair.of("%money%", config.betExpSmall)),
                "bet:small");

        // 100
        btnBetMoneyMedium.val().set(gameInventory,
                Lists.newArrayList(Pair.of("%money%", config.betMedium)),
                "bet:medium");
        btnBetExpMedium.val().set(gameInventory_exp,
                Lists.newArrayList(Pair.of("%money%", config.betExpMedium)),
                "bet:medium");

        // 1000
        btnBetMoneyBig.val().set(gameInventory,
                Lists.newArrayList(Pair.of("%money%", config.betBig)),
                "bet:big");
        btnBetExpBig.val().set(gameInventory_exp,
                Lists.newArrayList(Pair.of("%money%", config.betExpBig)),
                "bet:big");

        // 更新抛售图标的 lore 为买入提示
        updateBothIcon(btnCashOut.val().getSlot(), btnCashOutLoreBet.val());
    }

    public void openGameGUI(Player p, BustaType type) {
        if (type == BustaType.MONEY) {
            p.openInventory(gameInventory);
        } else {
            p.openInventory(gameInventory_exp);
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

        // 更新当前倍率到 lore
        updateCurNumToCashIcon();

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

        updateHistoryIcon();

        plugin.bank().save();
        plugin.users().save();
    }

    public void updateCurNumToCashIcon() {
        ListPair<String, Object> replacements = new ListPair<>();
        replacements.add("%cur_num%", parent.curNumFormatted());
        updateBothIcon(btnCashOut.val().getSlot(), Pair.replace(btnCashOutLoreGame.val(), replacements));
    }

    public void updateHistoryIcon() {
        List<String> historyLore = new ArrayList<>();
        // TODO: 转移到配置文件
        for (int i : history) {
            if (i >= 200) {
                historyLore.add("§ax" + doubleFormat.format(i / 100.0));
            } else {
                historyLore.add("§cx" + doubleFormat.format(i / 100.0));
            }
        }
        updateBothIcon(btnHistory.val().getSlot(), historyLore);
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
        UUID playerId = p.getUniqueId();
        if (!type.equals(playerMap.getOrDefault(playerId, null))) {
            return;
        }

        // 超出游戏最大人数时提醒玩家
        if (!playerMap.containsKey(playerId) && playerMap.size() >= getMaxPlayers()) {
            PlayerCountLimit.t(p);
            return;
        }

        // 是否第一次下注
        boolean firstBet = !activePlayerMap.containsKey(playerId);
        // 之前已下注数量
        int old = firstBet ? 0 : activePlayerMap.get(playerId);

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
                activePlayerMap.put(playerId, old + amount);
                Message_DivUpper.t(p);
                p.sendMessage("   §f" + Bet.get() + config.currencySymbol + (old + amount));
                p.sendMessage("   §e" + MyBal.get() + ": " + config.currencySymbol + doubleFormat.format(economy.getBalance(p)));
                Message_DivLower.t(p);

                // 如果是第一次下注，向其它玩家广播 有人加入游戏的通知
                if (firstBet) {
                    for (UUID uuid : playerMap.keySet()) {
                        if (playerId.equals(uuid)) continue;
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
                activePlayerMap.put(playerId, old + amount);
                Message_DivUpper.t(p);
                p.sendMessage("   §f" + Bet.get() + " Xp" + (old + amount));
                p.sendMessage("   §e" + MyBal.get() + ": Xp" + calcTotalExp(p));
                Message_DivLower.t(p);

                // 如果是第一次下注，向其它玩家广播 有人加入游戏的通知
                if (firstBet) {
                    for (UUID uuid : playerMap.keySet()) {
                        if (playerId.equals(uuid)) continue;
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

        int bet = old + amount;
        ListPair<String, Object> replacements = new ListPair<>();
        replacements.add("%player%", p.getName());
        replacements.add("%amount%", bet);
        if (firstBet) {
            // 如果是第一次下注
            // 设置游戏类型，更新游玩次数，向界面添加头颅图标
            playerMap.put(playerId, type);

            User user = plugin.users().get(p);
            user.setGamesPlayed(user.getGamesPlayed() + 1);

            int idx = getNextPlayerSlot();
            if (idx >= 0) {
                ItemStack skull = Util.getPlayerHeadItem();
                String flag = "show player info:" + p.getName();
                if (type == BustaType.MONEY) {
                    btnPlayerHeadMoney.val().generateItem(skull, replacements, null, flag);
                } else {
                    btnPlayerHeadExp.val().generateItem(skull, replacements, null, flag);
                }

                if (config.isLoadPlayerSkin.val()) {
                    // 更新头颅物品皮肤
                    plugin.getScheduler().runAsync(t -> {
                        ItemMeta itemMeta = skull.getItemMeta();
                        if (itemMeta instanceof SkullMeta) try {
                            SkullMeta skullMeta = (SkullMeta) itemMeta;
                            skullMeta.setOwningPlayer(p);
                            skull.setItemMeta(skullMeta);
                        } catch (Exception e) {
                            log("Failed to load skull skin of player: " + p.getName());
                        }
                    });
                }

                setBothIcon(idx, skull);

                headPos.put(playerId, idx);
            }
        } else try {
            // 不是第一次下注，则更新头颅图标的 lore
            Integer idx = headPos.get(playerId);
            if (idx != null) {
                List<String> lore;
                if (type == BustaType.MONEY) {
                    lore = Pair.replace(btnPlayerHeadMoney.val().getLore(), replacements);
                } else {
                    lore = Pair.replace(btnPlayerHeadExp.val().getLore(), replacements);
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
        BustaType type = playerMap.get(p.getUniqueId());

        double prize = bet * (parent.curNum() / 100.0);
        String curNumFormatted = parent.curNumFormatted();

        parent.runCommandCashOut(p, bet, parent.curNum(), prize);
        plugin.sounds().play(p, "CashOut");

        ListPair<String, Object> replacements = new ListPair<>();
        replacements.add("%player%", p.getName());
        replacements.add("%amount%", bet);
        replacements.add("%cur_num%", curNumFormatted);

        // 进行抛售操作，给予玩家奖励的金币或经验
        Message_DivUpper.t(p);
        p.sendMessage("   §f" + CashedOut.get() + ": x" + curNumFormatted);
        if (type == BustaType.MONEY) {
            plugin.getEconomy().depositPlayer(p, prize);
            p.sendMessage("   §3" + Profit.get() + ": " + config.currencySymbol + doubleFormat.format(prize - bet));
            p.sendMessage("   §e" + MyBal.get() + ": " + config.currencySymbol + doubleFormat.format(plugin.getEconomy().getBalance(p)));
        } else {
            p.giveExp((int) prize);
            p.sendMessage("   §3" + Profit.get() + ": Xp" + ((int) prize - bet));
            p.sendMessage("   §e" + MyBal.get() + ": Xp" + calcTotalExp(p));
        }
        Message_DivLower.t(p);

        Integer headPos = this.headPos.get(p.getUniqueId());
        if (headPos != null) {
            // 更新玩家头颅 lore
            ItemStack out;
            String displayName;
            List<String> lore;
            if (type == BustaType.MONEY) {
                out = btnPlayerHeadMoney.val().newItem();
                displayName = Pair.replace(btnPlayerHeadMoney.val().getDisplay(), replacements);
                lore = Pair.replace(btnPlayerHeadMoneyInGame.val(), replacements);
            } else {
                out = btnPlayerHeadExp.val().newItem();
                displayName = Pair.replace(btnPlayerHeadExp.val().getDisplay(), replacements);
                lore = Pair.replace(btnPlayerHeadExpInGame.val(), replacements);
            }

            ItemMeta meta = out.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(displayName));
                meta.setLore(color(lore));
                out.setItemMeta(meta);
            }

            flag(out, "show player info:" + p.getName());
            setBothIcon(headPos, out);
            this.headPos.remove(p.getUniqueId());
        }

        // 向游戏中所有玩家广播 抛售通知
        for (UUID uuid : playerMap.keySet()) {
            if (p.getUniqueId().equals(uuid)) continue;
            try {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.sendMessage("§6♣ " + p.getName() + " " + CashedOut.get() + " x" + curNumFormatted);
                }
            } catch (Exception ignored) {
            }
        }

        parent.updateBankroll(type, -prize);
        parent.updateNetProfit(p, type, prize);
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

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public boolean isInPlayerArea(int slot) {
        return holdPlayerSlots[slot];
    }

    public int getNextPlayerSlot() {
        for (int i = 0; i < holdPlayerSlots.length; i++) {
            if (holdPlayerSlots[i] && !containsHeadSlot(i)) {
                return i;
            }
        }
        return -1;
    }
}
