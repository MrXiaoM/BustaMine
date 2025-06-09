package me.sat7.bustamine.manager;

import me.sat7.bustamine.BustaMine;
import me.sat7.bustamine.config.Config;
import me.sat7.bustamine.manager.enums.BustaType;
import me.sat7.bustamine.manager.gui.BustaGuiHolder;
import me.sat7.bustamine.manager.gui.IBustaMineGui;
import me.sat7.bustamine.utils.Item;
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

import java.util.ArrayList;
import java.util.List;

import static me.sat7.bustamine.config.Messages.*;
import static me.sat7.bustamine.utils.Util.*;

public class GuiGame implements Listener {
    private final BustaMine plugin;
    private final GameManager parent;
    private final Config config;

    private Inventory gameInventory;
    private Inventory gameInventory_exp;

    int betExpSmall, betExpMedium, betExpBig;
    int betMoneySmall, betMoneyMedium, betMoneyBig;


    public GuiGame(GameManager parent) {
        this.parent = parent;
        this.plugin = parent.plugin();
        this.config = parent.plugin().config();
        Bukkit.getPluginManager().registerEvents(this, plugin);
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
        for (Integer key : parent.old.keySet()) {
            setBothIcon(key, parent.old.get(key));
        }
        parent.old.clear();

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
            parent.old.put(j, getMoneyIcon(j));
            setBothIcon(j, getGlassItem(0));
        }
    }

    @EventHandler
    @SuppressWarnings("IfCanBeSwitch")
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
                parent.bet(player, type, amount);
                return;
            }
            if (flag.equals("show bet setting")) { // 50
                parent.betSettings().showBetSettingUI(player);
                return;
            }
            if (flag.equals("cash out")) { // 49
                parent.cashOut(player);
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
}
