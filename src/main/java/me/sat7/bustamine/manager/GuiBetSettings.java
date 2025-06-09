package me.sat7.bustamine.manager;

import me.sat7.bustamine.BustaMine;
import me.sat7.bustamine.CustomConfig;
import me.sat7.bustamine.data.User;
import me.sat7.bustamine.manager.enums.BustaType;
import me.sat7.bustamine.manager.gui.BetGuiHolder;
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

import static me.sat7.bustamine.config.Messages.*;
import static me.sat7.bustamine.utils.Util.createItemStack;
import static me.sat7.bustamine.utils.Util.getGlass;

public class GuiBetSettings implements Listener {
    private final BustaMine plugin;
    private final GameManager parent;
    protected String btnBetSmall, btnBetMedium, btnBetBig;
    private int multiplierMax;

    public GuiBetSettings(GameManager parent) {
        this.parent = parent;
        this.plugin = parent.plugin();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void reload() {
        CustomConfig config = plugin.config();
        btnBetSmall = config.getString("BtnIcon.BetSmall");
        btnBetMedium = config.getString("BtnIcon.BetMedium");
        btnBetBig = config.getString("BtnIcon.BetBig");
        multiplierMax = config.getInt("MultiplierMax");
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
            inv.setItem(26, btnExpGame);
        }

        ItemStack state;
        ArrayList<String> btnLore = new ArrayList<>();
        User user = plugin.users().get(p);
        if (user.getCashOut() >= 0) {
            btnLore.add(CO_Enabled.get() + ": " + CO_x.get() + (user.getCashOut() / 100.0));
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
        inv.setItem(13, state);

        // -10
        ItemStack btnBigMinus10 = createItemStack(Material.getMaterial(btnBetBig), null,
                CO_Minus10.get(), btnLore, 1);
        inv.setItem(10, btnBigMinus10);
        // -1
        ItemStack btnMediumMinus1 = createItemStack(Material.getMaterial(btnBetMedium), null,
                CO_Minus1.get(), btnLore, 1);
        inv.setItem(11, btnMediumMinus1);
        // -0.1
        ItemStack BtnSmallMinus01 = createItemStack(Material.getMaterial(btnBetSmall), null,
                CO_Minus01.get(), btnLore, 1);
        inv.setItem(12, BtnSmallMinus01);
        // +0.1
        ItemStack btnSmallPlus01 = createItemStack(Material.getMaterial(btnBetSmall), null,
                CO_Plus01.get(), btnLore, 1);
        inv.setItem(14, btnSmallPlus01);
        // +1
        ItemStack btnMediumPlus1 = createItemStack(Material.getMaterial(btnBetMedium), null,
                CO_Plus1.get(), btnLore, 1);
        inv.setItem(15, btnMediumPlus1);
        // +10
        ItemStack btnBigPlus10 = createItemStack(Material.getMaterial(btnBetBig), null,
                CO_Plus10.get(), btnLore, 1);
        inv.setItem(16, btnBigPlus10);

        p.openInventory(inv);
    }


    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.isCancelled() || !(e.getWhoClicked() instanceof Player)) return;
        InventoryView view = e.getView();
        Player player = (Player) e.getWhoClicked();
        if (view.getTopInventory().getHolder() instanceof BetGuiHolder) {
            e.setCancelled(true);
            if (e.getClickedInventory() == null) return;


            if (e.getSlot() == 18) {
                if (player.hasPermission("bm.user.money")) {
                    parent.gui().openGameGUI(player, BustaType.MONEY);
                } else {
                    Message_NoPermission.t(player);
                }
            } else if (e.getSlot() == 26) {
                if (player.hasPermission("bm.user.exp")) {
                    parent.gui().openGameGUI(player, BustaType.EXP);
                } else {
                    Message_NoPermission.t(player);
                }
            }
            else if (e.getSlot() >= 10 || e.getSlot() <= 16) {
                User user = plugin.users().get(player);
                int mod = 0;

                if (e.getSlot() == 13) {
                    if (user.getCashOut() >= 0) {
                        user.setCashOut(-1);
                    } else {
                        user.setLastJoin(200);
                    }
                    showBetSettingUI(player);
                    return;
                } else if (e.getSlot() == 10) {
                    mod = -1000;
                } else if (e.getSlot() == 11) {
                    mod = -100;
                } else if (e.getSlot() == 12) {
                    mod = -10;
                } else if (e.getSlot() == 14) {
                    mod = 10;
                } else if (e.getSlot() == 15) {
                    mod = 100;
                } else if (e.getSlot() == 16) {
                    mod = 1000;
                }

                int temp = user.getCashOut();
                int target = temp + mod;
                if (target < 110) {
                    target = 110;
                }
                if (target > multiplierMax * 100) {
                    target = multiplierMax * 100;
                }

                user.setCashOut(target);
                showBetSettingUI(player);
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
