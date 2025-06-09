package me.sat7.bustamine;

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
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class BustaMine extends JavaPlugin {

    private static BustaMine plugin;
    public static BustaMine inst() {
        return plugin;
    }

    private Economy econ = null;

    public Economy getEconomy() {
        return econ;
    }

    public CustomConfig ccConfig;
    public CustomConfig ccBank;

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

    private UserManager userManager;
    private GameManager gameManager;
    private Sounds sounds;

    public UserManager users() {
        return userManager;
    }

    public GameManager game() {
        return gameManager;
    }

    public Sounds sounds() {
        return sounds;
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
        setupVault();
    }

    private void setupVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            log("Disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        } else {
            log("Vault Found");
        }

        setupRSP();
    }

    private int setupRspRetryCount = 0;

    private void setupRSP() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            econ = rsp.getProvider();

            init();
        } else {
            if (setupRspRetryCount >= 3) {
                log("Disabled due to no Vault dependency found!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            setupRspRetryCount++;
            log("Economy provider not found. Retry... " + setupRspRetryCount + "/3");

            Bukkit.getScheduler().runTaskLater(this, this::setupRSP, 30L);
        }
    }

    private void init() {
        Util.init();
        Game.init();
        ccConfig = new CustomConfig(this);
        ccBank = new CustomConfig(this);
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

    private void setupConfig() {
        // 컨픽
        ccConfig.setup("Config");
        // 주석
        ccConfig.get().options().header("Changes will take effect from the next round." +
                "\nRoundInterval: 3~ (real time second) / Default: 5" +
                "\nMultiplierMax: 30~150 / Default: 120" +
                "\nProbabilityOfInstaBust: 0.8~20.0 / Default: 1.0 (%) / The final value may vary depending on the MultiplierMax." +
                "\n" +
                "\nCommand.WhenRoundStart: placeholder: n/a" +
                "\nCommand.WhenPlayerBet: placeholder: {player} {amount}" +
                "\nCommand.WhenPlayerCashOut: placeholder: {player} {amount} {multiplier} {prize}" +
                "\nCommand.WhenRoundEnd: placeholder: {multiplier}"
        );
        ccConfig.get().addDefault("CurrencySymbol", "$");
        ccConfig.get().addDefault("RoundInterval", 5);
        ccConfig.get().addDefault("MultiplierMax", 120);
        ccConfig.get().addDefault("ProbabilityOfInstaBust", 2.0);
        ccConfig.get().addDefault("ShowWinChance", true);
        ccConfig.get().addDefault("ShowBankroll", true);
        ccConfig.get().addDefault("LoadPlayerSkin", true);
        ccConfig.get().addDefault("UIForceUpdate", false);

        ccConfig.get().addDefault("Bet.Small", 10);
        ccConfig.get().addDefault("Bet.Medium", 100);
        ccConfig.get().addDefault("Bet.Big", 1000);
        ccConfig.get().addDefault("Bet.Max", 5000);
        ccConfig.get().addDefault("Bet.ExpSmall", 10);
        ccConfig.get().addDefault("Bet.ExpMedium", 100);
        ccConfig.get().addDefault("Bet.ExpBig", 1000);
        ccConfig.get().addDefault("Bet.ExpMax", 5000);

        ccConfig.get().addDefault("Broadcast.Jackpot", 30);
        ccConfig.get().addDefault("Broadcast.InstaBust", true);

        ccConfig.get().addDefault("Command.WhenRoundStart", "");
        ccConfig.get().addDefault("Command.WhenPlayerBet", "");
        ccConfig.get().addDefault("Command.WhenPlayerCashOut", "");
        ccConfig.get().addDefault("Command.WhenRoundEnd", "");

        ccConfig.get().addDefault("BtnIcon.Bankroll", "DIAMOND");
        ccConfig.get().addDefault("BtnIcon.WinChance", "PAPER");
        ccConfig.get().addDefault("BtnIcon.MyState", "PAPER");
        ccConfig.get().addDefault("BtnIcon.History", "PAPER");
        ccConfig.get().addDefault("BtnIcon.CashOut", "EMERALD");
        ccConfig.get().addDefault("BtnIcon.CashOutSetting", "PAPER");
        ccConfig.get().addDefault("BtnIcon.BetSmall", "GOLD_NUGGET");
        ccConfig.get().addDefault("BtnIcon.BetMedium", "GOLD_INGOT");
        ccConfig.get().addDefault("BtnIcon.BetBig", "GOLD_BLOCK");

        ccConfig.get().options().copyDefaults(true);
        ccConfig.save();
    }

    private void setupBank() {
        // 컨픽
        ccBank.setup("Bank");
        ccBank.get().addDefault("Bankroll.Money", 500000);
        ccBank.get().addDefault("Bankroll.Exp", 500000);
        ccBank.get().addDefault("Statistics.Income.Money", 0);
        ccBank.get().addDefault("Statistics.Expense.Money", 0);
        ccBank.get().addDefault("Statistics.Income.Exp", 0);
        ccBank.get().addDefault("Statistics.Expense.Exp", 0);

        ccBank.get().options().copyDefaults(true);
        ccBank.save();
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();

        ccConfig.reload();
        ccBank.reload();
        userManager.reload();
        sounds.reload();
        updateConfig();
        gameManager.gui().refreshIcons();
    }

    public void reloadMessages() {
        File file = new File(getDataFolder(), "messages.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (Messages.reload(config)) try {
            config.save(file);
        } catch (IOException e) {
            log(e);
        }
    }

    public void updateConfig() {
        if (ccConfig.get().contains("Bankroll")) {
            ccBank.get().set("Bankroll.Money", ccConfig.get().getDouble("Bankroll"));
            ccBank.save();
            ccConfig.get().set("Bankroll", null);
        }

        if (ccConfig.get().getInt("MultiplierMax") > 150) ccConfig.get().set("MultiplierMax", 150);
        if (ccConfig.get().getInt("MultiplierMax") < 30) ccConfig.get().set("MultiplierMax", 30);

        if (ccConfig.get().getDouble("ProbabilityOfInstaBust") > 20) ccConfig.get().set("ProbabilityOfInstaBust", 20.0);
        if (ccConfig.get().getDouble("ProbabilityOfInstaBust") < 0.8) ccConfig.get().set("ProbabilityOfInstaBust", 0.8);

        if (ccConfig.get().getInt("RoundInterval") < 3) ccConfig.get().set("RoundInterval", 3);

        gameManager.reload();

        ccConfig.save();
    }

    @Override
    public void onDisable() {
        log("Disabled");
    }

}
