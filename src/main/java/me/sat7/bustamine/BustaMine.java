package me.sat7.bustamine;

import com.google.common.collect.Lists;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import me.sat7.bustamine.commands.CommandMain;
import me.sat7.bustamine.config.Messages;
import me.sat7.bustamine.config.Sounds;
import me.sat7.bustamine.manager.GameManager;
import me.sat7.bustamine.manager.UserManager;
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

    public BustaMine() {
        plugin = this;
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

    private CustomConfig config;
    private CustomConfig bank;
    private UserManager userManager;
    private GameManager gameManager;
    private Sounds sounds;

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

    public CustomConfig config() {
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

        Bukkit.getScheduler().runTaskLater(this, () -> setupEconomy(next), 30L);
    }

    private void init() {
        Util.init();
        config = new CustomConfig(this);
        bank = new CustomConfig(this);
        userManager = new UserManager(this);
        gameManager = new GameManager(this);
        sounds = new Sounds(this);

        new CommandMain(this);

        setupConfig();
        setupBank();
        reloadMessages();
        reloadConfig();

        log("Enabled! :)");

        gameManager.gui().gameGUISetup();

        gameManager.startGame();
        gameManager.setGameEnable(true);
    }

    @SuppressWarnings("deprecation")
    private void setupConfig() {
        config.setup("Config", config -> {
            String header = "Changes will take effect from the next round." +
                    "\nRoundInterval: 3~ (real time second) / Default: 5" +
                    "\nMultiplierMax: 30~150 / Default: 120" +
                    "\nProbabilityOfInstaBust: 0.8~20.0 / Default: 1.0 (%) / The final value may vary depending on the MultiplierMax." +
                    "\n" +
                    "\nCommand.WhenRoundStart: placeholder: n/a" +
                    "\nCommand.WhenPlayerBet: placeholder: {player} {amount}" +
                    "\nCommand.WhenPlayerCashOut: placeholder: {player} {amount} {multiplier} {prize}" +
                    "\nCommand.WhenRoundEnd: placeholder: {multiplier}";
            try {
                config.options().setHeader(Lists.newArrayList(header.split("\n")));
            } catch (Throwable t) {
                config.options().header(header);
            }
            config.addDefault("CurrencySymbol", "$");
            config.addDefault("RoundInterval", 5);
            config.addDefault("MultiplierMax", 120);
            config.addDefault("ProbabilityOfInstaBust", 2.0);
            config.addDefault("ShowWinChance", true);
            config.addDefault("ShowBankroll", true);
            config.addDefault("LoadPlayerSkin", true);
            config.addDefault("UIForceUpdate", false);

            config.addDefault("Bet.Small", 10);
            config.addDefault("Bet.Medium", 100);
            config.addDefault("Bet.Big", 1000);
            config.addDefault("Bet.Max", 5000);
            config.addDefault("Bet.ExpSmall", 10);
            config.addDefault("Bet.ExpMedium", 100);
            config.addDefault("Bet.ExpBig", 1000);
            config.addDefault("Bet.ExpMax", 5000);

            config.addDefault("Broadcast.Jackpot", 30);
            config.addDefault("Broadcast.InstaBust", true);

            config.addDefault("Command.WhenRoundStart", "");
            config.addDefault("Command.WhenPlayerBet", "");
            config.addDefault("Command.WhenPlayerCashOut", "");
            config.addDefault("Command.WhenRoundEnd", "");

            config.addDefault("BtnIcon.Bankroll", "DIAMOND");
            config.addDefault("BtnIcon.WinChance", "PAPER");
            config.addDefault("BtnIcon.MyState", "PAPER");
            config.addDefault("BtnIcon.History", "PAPER");
            config.addDefault("BtnIcon.CashOut", "EMERALD");
            config.addDefault("BtnIcon.CashOutSetting", "PAPER");
            config.addDefault("BtnIcon.BetSmall", "GOLD_NUGGET");
            config.addDefault("BtnIcon.BetMedium", "GOLD_INGOT");
            config.addDefault("BtnIcon.BetBig", "GOLD_BLOCK");

            config.options().copyDefaults(true);
        });
        config.save();
    }

    private void setupBank() {
        bank.setup("Bank", config -> {
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

    public void updateConfig() {
        if (config.contains("Bankroll")) {
            bank.set("Bankroll.Money", config.getDouble("Bankroll"));
            bank.save();
            config.remove("Bankroll");
        }

        config.rangeInt("MultiplierMax", 30, 150);
        config.rangeDouble("ProbabilityOfInstaBust", 0.8, 20.0);

        config.rangeInt("RoundInterval", 3, null);

        gameManager.reload();
        config.save();
    }

    @Override
    public void onDisable() {
        log("Disabled");
    }
}
