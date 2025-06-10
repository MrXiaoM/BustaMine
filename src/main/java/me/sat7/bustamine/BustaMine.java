package me.sat7.bustamine;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.impl.PlatformScheduler;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import me.sat7.bustamine.commands.CommandMain;
import me.sat7.bustamine.config.Config;
import me.sat7.bustamine.config.Messages;
import me.sat7.bustamine.config.Sounds;
import me.sat7.bustamine.manager.GameManager;
import me.sat7.bustamine.manager.UserManager;
import me.sat7.bustamine.utils.CustomConfig;
import me.sat7.bustamine.utils.Util;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class BustaMine extends JavaPlugin {
    private static BustaMine plugin;
    public static BustaMine inst() {
        return plugin;
    }
    private final FoliaLib foliaLib;
    public BustaMine() {
        plugin = this;
        this.foliaLib = new FoliaLib(this);
    }

    public static void log(String info) {
        log(info, null);
    }
    public static void log(Throwable t) {
        log(null, t);
    }
    public static void log(String info, Throwable t) {
        CommandSender console = Bukkit.getConsoleSender();
        String prefix = "§6[BustaMine]§f ";
        if (info != null) {
            console.sendMessage(prefix + info);
        }
        if (t != null) {
            StringWriter sw = new StringWriter();
            try (PrintWriter pw = new PrintWriter(sw)) {
                t.printStackTrace(pw);
            }
            for (String s : sw.toString().split("\n")) {
                console.sendMessage(prefix + s);
            }
        }
    }

    private Economy econ = null;

    private Config config;
    private CustomConfig bank;
    private UserManager userManager;
    private GameManager gameManager;
    private Sounds sounds;

    public PlatformScheduler getScheduler() {
        return foliaLib.getScheduler();
    }

    public Economy getEconomy() {
        return econ;
    }

    public UserManager users() {
        return userManager;
    }

    public GameManager game() {
        return gameManager;
    }

    public Sounds sounds() {
        return sounds;
    }

    public Config config() {
        return config;
    }

    public CustomConfig bank() {
        return bank;
    }

    @Override
    public void onLoad() {
        MinecraftVersion.replaceLogger(getLogger());
        MinecraftVersion.disableUpdateCheck();
        MinecraftVersion.disableBStats();
        MinecraftVersion.getVersion();
    }

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            log("Disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        } else {
            log("Vault Found");
        }

        setupEconomy(0);
    }

    private void setupEconomy(int retriedCount) {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        Economy economy = rsp == null ? null : rsp.getProvider();
        if (economy != null) {
            econ = economy;
            init();
            return;
        }
        if (retriedCount >= 3) {
            log("Disabled due to no Vault Economy instance found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        int next = retriedCount + 1;
        log("Economy provider not found. Retrying... " + next + "/3");

        getScheduler().runLater(t -> setupEconomy(next), 30L);
    }

    private void init() {
        Util.init();
        config = new Config(this);
        bank = new CustomConfig(this);
        userManager = new UserManager(this);
        gameManager = new GameManager(this);
        sounds = new Sounds(this);

        new CommandMain(this);

        config.setup();
        gameManager.setup();
        setupBank();
        reloadMessages();
        reloadConfig();
        reloadGUI();

        log("Enabled! :)");

        gameManager.guiGameShared().gameGUISetup();

        gameManager.startGame();
        gameManager.setGameEnable(true);
    }

    private void setupBank() {
        bank.setup("bank", config -> {
            config.addDefault("Bankroll.Money", 500000);
            config.addDefault("Bankroll.Exp", 500000);
            config.addDefault("Statistics.Income.Money", 0);
            config.addDefault("Statistics.Expense.Money", 0);
            config.addDefault("Statistics.Income.Exp", 0);
            config.addDefault("Statistics.Expense.Exp", 0);

            config.options().copyDefaults(true);
        });
        bank.save();
    }

    @NotNull
    @Override
    public FileConfiguration getConfig() {
        return config.get();
    }

    @Override
    public void reloadConfig() {
        config.reload();
        bank.reload();
        userManager.reload();
        sounds.reload();
        updateConfig();
    }

    public File resolve(String fileName) {
        return new File(getDataFolder(), fileName);
    }

    public void reloadMessages() {
        File file = resolve("messages.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (Messages.reload(config)) try {
            config.save(file);
        } catch (IOException e) {
            log(e);
        }
    }
    public void reloadGUI() {
        game().guiGameShared().reloadConfig();
        game().guiBetSettings().reloadConfig();
    }

    public void updateConfig() {
        config.updateConfig();
    }

    @Override
    public void onDisable() {
        getScheduler().cancelAllTasks();
        bank().save();
        users().save();
        log("Disabled");
    }
}
