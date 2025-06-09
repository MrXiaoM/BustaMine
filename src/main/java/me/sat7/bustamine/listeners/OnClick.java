package me.sat7.bustamine.listeners;

import me.sat7.bustamine.BustaMine;
import me.sat7.bustamine.Game;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import static me.sat7.bustamine.config.Messages.Message_NoPermission;

public class OnClick implements Listener {

    @EventHandler
    public void onDragInGUI(InventoryDragEvent event) {
        if (checkIsGUI(event.getInventory())) {
            event.setCancelled(true);
        } else if (event.getView().getTitle().equals(BustaMine.ccLang.get().getString("CO.Title"))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;

        Player player = (Player) e.getWhoClicked();

        if (e.getClickedInventory() != player.getInventory()) {
            if (checkIsGUI(e.getInventory())) {
                e.setCancelled(true);

                Game.playSoundEffect(player, "Click");

                if (e.getSlot() == 51 || e.getSlot() == 52 || e.getSlot() == 53) {
                    String temp = e.getView().getTitle();
                    temp = temp.replace(BustaMine.ccLang.get().getString("UI.Title") + " ", "");
                    temp = ChatColor.stripColor(temp);

                    if (temp.equals(ChatColor.stripColor(BustaMine.ccLang.get().getString("Exp")))) {
                        int amount = BustaMine.ccConfig.get().getInt("Bet.ExpSmall");
                        if (e.getSlot() == 52) amount = BustaMine.ccConfig.get().getInt("Bet.ExpMedium");
                        if (e.getSlot() == 53) amount = BustaMine.ccConfig.get().getInt("Bet.ExpBig");
                        Game.bet(player, Game.bustaType.exp, amount);
                    } else {
                        int amount = BustaMine.ccConfig.get().getInt("Bet.Small");
                        if (e.getSlot() == 52) amount = BustaMine.ccConfig.get().getInt("Bet.Medium");
                        if (e.getSlot() == 53) amount = BustaMine.ccConfig.get().getInt("Bet.Big");
                        Game.bet(player, Game.bustaType.money, amount);
                    }
                }
                else if (e.getSlot() == 50) {
                    Game.showBetSettingUI(player);
                }
                else if (e.getSlot() == 49) {
                    Game.cashOut(player);
                }
                else if (e.getSlot() == 47) {
                    Game.showPlayerInfo(player, player);
                } else if (e.getSlot() < 45) {
                    String str = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());

                    if (str.isEmpty()) return;

                    Player p = Bukkit.getPlayer(str);
                    if (p != null) Game.showPlayerInfo(player, p);
                }
            } else if (e.getView().getTitle().equals(BustaMine.ccLang.get().getString("CO.Title"))) {
                e.setCancelled(true);

                if (e.getSlot() == 18) {
                    if (player.hasPermission("bm.user.money")) {
                        Game.openGameGUI(player, Game.bustaType.money);
                    } else {
                        Message_NoPermission.t(player);
                    }
                } else if (e.getSlot() == 26) {
                    if (player.hasPermission("bm.user.exp")) {
                        Game.openGameGUI(player, Game.bustaType.exp);
                    } else {
                        Message_NoPermission.t(player);
                    }
                }
                else if (e.getSlot() >= 10 || e.getSlot() <= 16) {
                    String uuid = player.getUniqueId().toString();
                    int mod = 0;

                    if (e.getSlot() == 13) {
                        if (BustaMine.ccUser.get().contains(uuid + ".CashOut")) {
                            BustaMine.ccUser.get().set(uuid + ".CashOut", null);
                        } else {
                            BustaMine.ccUser.get().set(uuid + ".CashOut", 200);
                        }
                        Game.showBetSettingUI(player);
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

                    int temp = BustaMine.ccUser.get().getInt(uuid + ".CashOut");
                    int target = temp + mod;
                    if (target < 110) {
                        target = 110;
                    }
                    if (target > BustaMine.ccConfig.get().getInt("MultiplierMax") * 100) {
                        target = BustaMine.ccConfig.get().getInt("MultiplierMax") * 100;
                    }

                    BustaMine.ccUser.get().set(uuid + ".CashOut", target);
                    Game.showBetSettingUI(player);
                }
            }
        }
        else if (e.isShiftClick()) {
            if (e.getView().getTitle().equals(BustaMine.ccLang.get().getString("CO.Title"))) e.setCancelled(true);
            if (checkIsGUI(e.getView().getInventory(0))) e.setCancelled(true);
        }
    }

    private boolean checkIsGUI(Inventory inv) {
        if (inv.getSize() == 54 && inv.getItem(49) != null) {
            return (inv.getItem(49).getItemMeta().getDisplayName().equals(BustaMine.ccLang.get().getString("UI.CashOut")));
        } else {
            return false;
        }
    }
}
