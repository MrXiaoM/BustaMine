package me.sat7.bustamine.manager;

import com.google.common.collect.Lists;
import me.sat7.bustamine.config.Config;
import me.sat7.bustamine.data.User;
import me.sat7.bustamine.manager.enums.BustaType;
import me.sat7.bustamine.manager.gui.BetGuiHolder;
import me.sat7.bustamine.utils.BustaIcon;
import me.sat7.bustamine.utils.CustomConfig;
import me.sat7.bustamine.utils.ListPair;
import me.sat7.bustamine.utils.Property;
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
import static me.sat7.bustamine.utils.BustaIcon.def;
import static me.sat7.bustamine.utils.BustaIcon.propertyIcon;
import static me.sat7.bustamine.utils.Property.property;
import static me.sat7.bustamine.utils.Util.*;

public class GuiBetSettings extends CustomConfig implements Listener, Property.IPropertyRegistry {
    private final List<Property<?>> registeredProperties = new ArrayList<>();
    private final GameManager parent;
    private final Config config;

    public final Property<BustaIcon> btnStateEnabled = propertyIcon(this, "icons.state-enabled", def()
            .material(Material.GREEN_STAINED_GLASS_PANE)
            .display("&6&l开/关")
            .lore(
                    "&f开启: x%auto_cash_out%",
                    "&e点击进行操作"
            ));
    public final Property<BustaIcon> btnStateDisabled = propertyIcon(this, "icons.state-enabled", def()
            .material(Material.RED_STAINED_GLASS_PANE)
            .display("&6&l开/关")
            .lore(
                    "&f关闭",
                    "&e点击进行操作"
            ));
    public final Property<BustaIcon> btnMinus1000 = propertyIcon(this, "icons.minus-1000", def()
            .material(Material.GOLD_NUGGET)
            .display("&6&l-10"));
    public final Property<BustaIcon> btnMinus100 = propertyIcon(this, "icons.minus-100", def()
            .material(Material.GOLD_NUGGET)
            .display("&6&l-1"));
    public final Property<BustaIcon> btnMinus10 = propertyIcon(this, "icons.minus-10", def()
            .material(Material.GOLD_NUGGET)
            .display("&6&l-0.1"));
    public final Property<BustaIcon> btnPlus10 = propertyIcon(this, "icons.plus-10", def()
            .material(Material.GOLD_NUGGET)
            .display("&6&l+0.1"));
    public final Property<BustaIcon> btnPlus100 = propertyIcon(this, "icons.plus-100", def()
            .material(Material.GOLD_NUGGET)
            .display("&6&l+1"));
    public final Property<BustaIcon> btnPlus1000 = propertyIcon(this, "icons.plus-1000", def()
            .material(Material.GOLD_NUGGET)
            .display("&6&l+10"));
    public final Property<List<String>> btnLoreEnabled = property(this, "btn-lore-enabled", Lists.newArrayList(
            "&f开启: x%auto_cash_out%",
            "&e点击进行操作"
    ));
    public final Property<List<String>> btnLoreDisabled = property(this, "btn-lore-disabled", Lists.newArrayList(
            "&f关闭",
            "&e点击进行操作"
    ));

    private int multiplierMax;

    public GuiBetSettings(GameManager parent) {
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
        setup("gui/bet-settings", config -> {
            for (Property<?> property : registeredProperties) {
                property.setup();
            }
            config.options().copyDefaults(true);
        });
        save();
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

        ListPair<String, Object> replacements = new ListPair<>();
        List<String> btnLore;
        User user = plugin.users().get(p);
        if (user.getAutoCashOut() >= 0) {
            replacements.add("%auto_cash_out%", String.format("%.2f", (user.getAutoCashOut() / 100.0)));
            btnLore = btnLoreEnabled.val();
            inv.setItem(13, btnStateEnabled.val().generateItem(replacements, "state"));
        } else {
            btnLore = btnLoreDisabled.val();
            inv.setItem(13, btnStateDisabled.val().generateItem(replacements, "state"));
        }

        inv.setItem(10, btnMinus1000.val().generateItem(replacements, btnLore, "mod:-1000"));
        inv.setItem(11, btnMinus100.val().generateItem(replacements, btnLore, "mod:-100"));
        inv.setItem(12, btnMinus10.val().generateItem(replacements, btnLore, "mod:-10"));
        inv.setItem(14, btnPlus10.val().generateItem(replacements, btnLore, "mod:+10"));
        inv.setItem(15, btnPlus100.val().generateItem(replacements, btnLore, "mod:+100"));
        inv.setItem(16, btnPlus1000.val().generateItem(replacements, btnLore, "mod:+1000"));

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
