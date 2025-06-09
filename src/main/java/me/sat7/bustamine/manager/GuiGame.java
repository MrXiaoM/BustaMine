package me.sat7.bustamine.manager;

import me.sat7.bustamine.BustaMine;
import me.sat7.bustamine.Game;
import me.sat7.bustamine.manager.enums.BustaType;
import me.sat7.bustamine.manager.gui.BustaGuiHolder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
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

import static me.sat7.bustamine.Game.closeAllGameUI;
import static me.sat7.bustamine.config.Messages.*;
import static me.sat7.bustamine.utils.Util.*;

public class GuiGame implements Listener {
    private final BustaMine plugin;
    private final GameManager parent;

    private Inventory gameInventory;
    private Inventory gameInventory_exp;

    int betExpSmall, betExpMedium, betExpBig;
    int betMoneySmall, betMoneyMedium, betMoneyBig;

    boolean isShowBankroll;
    protected String btnBankroll, btnMyState, btnHistory, btnCashOut, btnCashOutSetting;

    public GuiGame(GameManager parent) {
        this.parent = parent;
        this.plugin = parent.plugin();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void reload() {
        FileConfiguration config = plugin.ccConfig.get();
        betExpSmall = config.getInt("Bet.ExpSmall");
        betExpMedium = config.getInt("Bet.ExpMedium");
        betExpBig = config.getInt("Bet.ExpBig");
        betMoneySmall = config.getInt("Bet.Small");
        betMoneyMedium = config.getInt("Bet.Medium");
        betMoneyBig = config.getInt("Bet.Big");

        isShowBankroll = config.getBoolean("ShowBankroll");
        btnBankroll = config.getString("BtnIcon.Bankroll");
        btnMyState = config.getString("BtnIcon.MyState");
        btnHistory = config.getString("BtnIcon.History");
        btnCashOut = config.getString("BtnIcon.CashOut");
        btnCashOutSetting = config.getString("BtnIcon.CashOutSetting");
    }

    public void gameGUISetup() {
        closeAllGameUI();
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

        // Bankroll
        if (isShowBankroll) {
            ItemStack bankrollBtn = createItemStack(Material.getMaterial(btnBankroll), null,
                    UI_Bankroll.get(), null, 1);
            inv.setItem(45, bankrollBtn);
        }

        // 내 정보
        ArrayList<String> myStateLore = new ArrayList<>();
        myStateLore.add(UI_Click.get());
        ItemStack myStateBtn = createItemStack(Material.getMaterial(btnMyState), null,
                UI_MyState.get(), myStateLore, 1);
        inv.setItem(47, myStateBtn);

        // 기록 버튼
        ItemStack historyBtn = createItemStack(Material.getMaterial(btnHistory), null,
                UI_History.get(), null, 1);
        inv.setItem(48, historyBtn);

        // 스톱 버튼
        ItemStack closeBtn = createItemStack(Material.getMaterial(btnCashOut), null,
                UI_CashOut.get(), null, 1);
        inv.setItem(49, closeBtn);

        // 설정 버튼
        ItemStack cosBtn = createItemStack(Material.getMaterial(btnCashOutSetting), null,
                UI_CashOutSetting.get(), null, 1);
        inv.setItem(50, cosBtn);

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

    public void refreshIcons() {
        String[] iconIds = new String[]{"Bankroll", "WinChance", "MyState", "History", "CashOut"};
        //45 46 47 48 49
        for (int i = 45; i <= 49; i++) {
            ItemStack oldItem = getMoneyIcon(i);
            if (oldItem == null) continue;

            ItemMeta meta = oldItem.getItemMeta();

            String material = plugin.ccConfig.get().getString("BtnIcon." + iconIds[i - 45]);
            ItemStack item = new ItemStack(Material.getMaterial(material));
            item.setItemMeta(meta);

            setBothIcon(i, item);
        }
    }

    void drawNumber(int num) {
        for (Integer key : parent.old.keySet()) {
            setBothIcon(key, parent.old.get(key));
        }
        parent.old.clear();

        int[] intarr = null;
        if (num == 5) {
            intarr = new int[]{5, 4, 3, 12, 21, 22, 23, 32, 39, 40, 41};
        } else if (num == 4) {
            intarr = new int[]{3, 12, 21, 22, 23, 14, 5, 32, 41};
        } else if (num == 3) {
            intarr = new int[]{3, 4, 5, 14, 21, 22, 23, 32, 39, 40, 41};
        } else if (num == 2) {
            intarr = new int[]{3, 4, 5, 14, 23, 22, 21, 30, 39, 40, 41};
        } else if (num == 1) {
            intarr = new int[]{5, 14, 23, 32, 41};
        } else {
            intarr = new int[]{3, 4, 5, 14, 23, 32, 41, 40, 39, 30, 21, 12};
        }

        for (int j : intarr) {
            parent.old.put(j, getMoneyIcon(j));
            setBothIcon(j, getGlassItem(0));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.isCancelled() || !(e.getWhoClicked() instanceof Player)) return;
        InventoryView view = e.getView();
        Player player = (Player) e.getWhoClicked();
        if (view.getTopInventory().getHolder() instanceof BustaGuiHolder) {
            e.setCancelled(true);
            if (e.getClickedInventory() == null) return;
            BustaType type = ((BustaGuiHolder) view.getTopInventory().getHolder()).getType();

            plugin.sounds().play(player, "Click");

            if (e.getSlot() == 51 || e.getSlot() == 52 || e.getSlot() == 53) {
                if (type.equals(BustaType.EXP)) {
                    int amount = betExpSmall;
                    if (e.getSlot() == 52) amount = betExpMedium;
                    if (e.getSlot() == 53) amount = betExpBig;
                    parent.bet(player, BustaType.EXP, amount);
                } else {
                    int amount = betMoneySmall;
                    if (e.getSlot() == 52) amount = betMoneyMedium;
                    if (e.getSlot() == 53) amount = betMoneyBig;
                    parent.bet(player, BustaType.MONEY, amount);
                }
            }
            else if (e.getSlot() == 50) {
                parent.betSettings().showBetSettingUI(player);
            }
            else if (e.getSlot() == 49) {
                parent.cashOut(player);
            }
            else if (e.getSlot() == 47) {
                Game.showPlayerInfo(plugin, player, player);
            } else if (e.getSlot() < 45) {
                String str = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());

                if (str.isEmpty()) return;

                Player p = Bukkit.getPlayer(str);
                if (p != null) Game.showPlayerInfo(plugin, player, p);
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
