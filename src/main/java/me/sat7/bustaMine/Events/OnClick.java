package me.sat7.bustaMine.Events;

import me.sat7.bustaMine.Game;
import me.sat7.bustaMine.BustaMine;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class OnClick implements Listener {

    // UI 인벤토리에 드래그로 아이탬 올리는것을 막음
    @EventHandler
    public void onDragInGUI(InventoryDragEvent event) {

        if (CheckInvenIsUI(event.getInventory())) { event.setCancelled(true); }
        else if(event.getView().getTitle().equals(BustaMine.ccLang.get().getString("CO.Title"))) { event.setCancelled(true); }
    }

    // 인벤토리 클릭
    @EventHandler
    public void OnInvenClick(InventoryClickEvent e)
    {
        if(e.getClickedInventory() == null) return;

        Player player = (Player)e.getWhoClicked();

        // 클릭된 인벤토리가 UI임
        if(e.getClickedInventory() != player.getInventory())
        {
            if(CheckInvenIsUI(e.getInventory()))
            {
                e.setCancelled(true);

                Game.PlayerSoundEffect(player,"Click");

                // 베팅 버튼
                if(e.getSlot() == 51 || e.getSlot() == 52 || e.getSlot() == 53)
                {
                    String temp = e.getView().getTitle();
                    temp = temp.replace(BustaMine.ccLang.get().getString("UI.Title")+" ","");
                    temp = ChatColor.stripColor(temp);

                    if(temp.equals(ChatColor.stripColor(BustaMine.ccLang.get().getString("Exp"))))
                    {
                        int amount = BustaMine.ccConfig.get().getInt("Bet.ExpSmall");
                        if(e.getSlot() == 52) amount = BustaMine.ccConfig.get().getInt("Bet.ExpMedium");
                        if(e.getSlot() == 53) amount = BustaMine.ccConfig.get().getInt("Bet.ExpBig");
                        Game.Bet(player, Game.bustaType.exp, amount);
                    }
                    else
                    {
                        int amount = BustaMine.ccConfig.get().getInt("Bet.Small");
                        if(e.getSlot() == 52) amount = BustaMine.ccConfig.get().getInt("Bet.Medium");
                        if(e.getSlot() == 53) amount = BustaMine.ccConfig.get().getInt("Bet.Big");
                        Game.Bet(player, Game.bustaType.money, amount);
                    }
                }
                // 캐시아웃 설정
                else if(e.getSlot() == 50)
                {
                    Game.ShowBetSettingUI(player);
                }
                // 캐시 아웃
                else if(e.getSlot() == 49)
                {
                    Game.CashOut(player);
                }
                // 내 정보
                else if(e.getSlot() == 47)
                {
                    Game.ShowPlayerInfo(player,player);
                }
                else if(e.getSlot() < 45)
                {
                    String str = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());

                    if(str.length()==0) return;

                    Player p = Bukkit.getPlayer(str);
                    if(p != null) Game.ShowPlayerInfo(player,p);
                }
            }
            else if(e.getView().getTitle().equals(BustaMine.ccLang.get().getString("CO.Title")))
            {
                e.setCancelled(true);

                if(e.getSlot() == 18)
                {
                    if(player.hasPermission("bm.user.money"))
                    {
                        Game.OpenGameInven(player, Game.bustaType.money);
                    }
                    else
                    {
                        player.sendMessage(BustaMine.prefix+BustaMine.ccLang.get().getString("Message.NoPermission"));
                    }
                }
                else if(e.getSlot()==26)
                {
                    if(player.hasPermission("bm.user.exp"))
                    {
                        Game.OpenGameInven(player, Game.bustaType.exp);
                    }
                    else
                    {
                        player.sendMessage(BustaMine.prefix+BustaMine.ccLang.get().getString("Message.NoPermission"));
                    }
                }
                // cashout지점 설정
                else if(e.getSlot() >= 10 || e.getSlot() <= 16)
                {
                    String uuid = player.getUniqueId().toString();
                    int mod = 0;

                    if(e.getSlot() == 13)
                    {
                        if(BustaMine.ccUser.get().contains(uuid+".CashOut"))
                        {
                            BustaMine.ccUser.get().set(uuid+".CashOut",null);
                        }
                        else
                        {
                            BustaMine.ccUser.get().set(uuid+".CashOut",200);
                        }
                        Game.ShowBetSettingUI(player);
                        return;
                    }
                    else if(e.getSlot() == 10)
                    {
                        mod = -1000;
                    }
                    else if(e.getSlot() == 11)
                    {
                        mod = -100;
                    }
                    else if(e.getSlot() == 12)
                    {
                        mod = -10;
                    }
                    else if(e.getSlot() == 14)
                    {
                        mod = 10;
                    }
                    else if(e.getSlot() == 15)
                    {
                        mod = 100;
                    }
                    else if(e.getSlot() == 16)
                    {
                        mod = 1000;
                    }

                    int temp = BustaMine.ccUser.get().getInt(uuid+".CashOut");
                    int target = temp + mod;
                    if(target<110)
                    {
                        target = 110;
                    }
                    if(target>BustaMine.ccConfig.get().getInt("MultiplierMax")*100)
                    {
                        target = BustaMine.ccConfig.get().getInt("MultiplierMax")*100;
                    }

                    BustaMine.ccUser.get().set(uuid+".CashOut",target);
                    Game.ShowBetSettingUI(player);
                }
            }
        }
        // Shift클릭으로 UI인벤에 아이탬 올리는것 막기
        else if(e.isShiftClick())
        {
            if(e.getView().getTitle().equals(BustaMine.ccLang.get().getString("CO.Title"))) e.setCancelled(true);
            if(CheckInvenIsUI(e.getView().getInventory(0))) e.setCancelled(true);
        }
    }

    private boolean CheckInvenIsUI(Inventory iv)
    {
        if(iv.getSize() == 54 && iv.getItem(49) != null)
        {
            return (iv.getItem(49).getItemMeta().getDisplayName().equals(BustaMine.ccLang.get().getString("UI.CashOut")));
        }
        else
        {
            return false;
        }
    }
}
