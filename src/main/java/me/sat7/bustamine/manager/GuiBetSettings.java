package me.sat7.bustamine.manager;

import me.sat7.bustamine.BustaMine;
import me.sat7.bustamine.config.Config;
import me.sat7.bustamine.data.User;
import me.sat7.bustamine.manager.enums.BustaType;
import me.sat7.bustamine.manager.gui.BetGuiHolder;
import org.bukkit.Bukkit;
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

import static me.sat7.bustamine.config.Messages.*;
import static me.sat7.bustamine.utils.Util.*;

public class GuiBetSettings implements Listener {
    private final BustaMine plugin;
    private final GameManager parent;
    private final Config config;
    private int multiplierMax;

    public GuiBetSettings(GameManager parent) {
        this.parent = parent;
        this.plugin = parent.plugin();
        this.config = parent.plugin().config();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void reload() {
        multiplierMax = config.multiplierMax.val();
    }

    public void showBetSettingUI(Player p) {
        Inventory inv = new BetGuiHolder(27, CO_Title.get()).getInventory();

        ArrayList<String> btnGameLore = new ArrayList<>();
        btnGameLore.add(UI_Click.get());
        ItemStack btnMoneyGame = new ItemStack(getGlass(11));
        {
            ItemMeta meta = btnMoneyGame.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(CO_PlayMoneyGame.get());
                meta.setLore(btnGameLore);
                btnMoneyGame.setItemMeta(meta);
            }
            flag(btnMoneyGame, "back:money");
            inv.setItem(18, btnMoneyGame);
        }

        ItemStack btnExpGame = new ItemStack(getGlass(11));
        {
            ItemMeta meta = btnExpGame.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(CO_PlayExpGame.get());
                meta.setLore(btnGameLore);
                btnExpGame.setItemMeta(meta);
            }
            flag(btnMoneyGame, "back:exp");
            inv.setItem(26, btnExpGame);
        }

        ItemStack state;
        ArrayList<String> btnLore = new ArrayList<>();
        User user = plugin.users().get(p);
        if (user.getAutoCashOut() >= 0) {
            btnLore.add(CO_Enabled.get() + ": " + CO_x.get() + (user.getAutoCashOut() / 100.0));
            btnLore.add(UI_Click.get());

            state = new ItemStack(getGlass(13));
        } else {
            btnLore.add(CO_Disabled.get());
            btnLore.add(UI_Click.get());

            state = new ItemStack(getGlass(14));
        }
        ItemMeta stateMeta = state.getItemMeta();
        if (stateMeta != null) {
            stateMeta.setDisplayName(CO_OnOff.get());
            stateMeta.setLore(btnLore);
            state.setItemMeta(stateMeta);
        }
        flag(state, "state");
        inv.setItem(13, state);

        // -10
        ItemStack btnBigMinus10 = createItemStack(config.btnBetBig, null,
                CO_Minus10.get(), btnLore, 1);
        flag(btnBigMinus10, "mod:-1000");
        inv.setItem(10, btnBigMinus10);
        // -1
        ItemStack btnMediumMinus1 = createItemStack(config.btnBetMedium, null,
                CO_Minus1.get(), btnLore, 1);
        flag(btnMediumMinus1, "mod:-100");
        inv.setItem(11, btnMediumMinus1);
        // -0.1
        ItemStack BtnSmallMinus01 = createItemStack(config.btnBetSmall, null,
                CO_Minus01.get(), btnLore, 1);
        flag(BtnSmallMinus01, "mod:-10");
        inv.setItem(12, BtnSmallMinus01);
        // +0.1
        ItemStack btnSmallPlus01 = createItemStack(config.btnBetSmall, null,
                CO_Plus01.get(), btnLore, 1);
        flag(btnSmallPlus01, "mod:+10");
        inv.setItem(14, btnSmallPlus01);
        // +1
        ItemStack btnMediumPlus1 = createItemStack(config.btnBetMedium, null,
                CO_Plus1.get(), btnLore, 1);
        flag(btnMediumPlus1, "mod:+100");
        inv.setItem(15, btnMediumPlus1);
        // +10
        ItemStack btnBigPlus10 = createItemStack(config.btnBetBig, null,
                CO_Plus10.get(), btnLore, 1);
        flag(btnBigPlus10, "mod:+1000");
        inv.setItem(16, btnBigPlus10);

        p.openInventory(inv);
    }


    @EventHandler
    @SuppressWarnings({"IfCanBeSwitch", "UnnecessaryReturnStatement"})
    public void onClick(InventoryClickEvent e) {
        if (e.isCancelled() || !(e.getWhoClicked() instanceof Player)) return;
        InventoryView view = e.getView();
        Player player = (Player) e.getWhoClicked();
        if (view.getTopInventory().getHolder() instanceof BetGuiHolder) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            if (e.getClickedInventory() == null || item == null) return;

            String flag = flag(item);
            if (flag.equals("back:money")) { // 18
                if (player.hasPermission("bm.user.money")) {
                    parent.guiGameShared().openGameGUI(player, BustaType.MONEY);
                } else {
                    Message_NoPermission.t(player);
                }
                return;
            }
            if (flag.equals("back:exp")) { // 26
                if (player.hasPermission("bm.user.exp")) {
                    parent.guiGameShared().openGameGUI(player, BustaType.EXP);
                } else {
                    Message_NoPermission.t(player);
                }
                return;
            }
            if (flag.equals("state")) { // 13
                User user = plugin.users().get(player);
                if (user.getAutoCashOut() >= 0) {
                    user.setAutoCashOut(-1);
                } else {
                    user.setAutoCashOut(200);
                }
                showBetSettingUI(player);
                return;
            }
            if (flag.startsWith("mod:")) { // 10, 11, 12  |  14, 15, 16
                Integer mod = parseInt(flag.substring(4)).orElse(null);
                if (mod != null) {
                    User user = plugin.users().get(player);

                    int target = user.getAutoCashOut() + mod;
                    if (target < 110) {
                        target = 110;
                    }
                    if (target > multiplierMax * 100) {
                        target = multiplierMax * 100;
                    }

                    user.setAutoCashOut(target);
                    showBetSettingUI(player);
                }
                return;
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof BetGuiHolder) {
            e.setCancelled(true);
        }
    }
}
