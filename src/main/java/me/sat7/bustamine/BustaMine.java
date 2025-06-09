package me.sat7.bustamine;

import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import me.sat7.bustamine.commands.CommandMain;
import me.sat7.bustamine.config.Messages;
import me.sat7.bustamine.listeners.OnClick;
import me.sat7.bustamine.listeners.OnJoinLeave;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Random;

public final class BustaMine extends JavaPlugin implements Listener {

    public static BustaMine plugin;
    public static String prefix = "";

    private static Economy econ = null;

    public static Economy getEconomy() {
        return econ;
    }

    public static final Random generator = new Random();

    public static CustomConfig ccConfig;
    public static CustomConfig ccBank;
    public static CustomConfig ccUser;
    public static CustomConfig ccLang;
    public static CustomConfig ccSound;

    public BustaMine() {
        plugin = this;
    }

    public static void log(String info) {
        log(info, null);
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
        ccConfig = new CustomConfig(this);
        ccBank = new CustomConfig(this);
        ccUser = new CustomConfig(this);
        ccSound = new CustomConfig(this);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new OnClick(), this);
        getServer().getPluginManager().registerEvents(new OnJoinLeave(), this);

        new CommandMain(this);

        Game.calcOdds();
        setupConfig();
        setupBank();
        setupUser();
        setupSound();
        reloadMessages();
        reloadConfig();

        log("Enabled! :)");

        Game.setupGlass();
        Game.setupSortedMap();
        Game.gameGUISetup();

        Game.startGame();
        Game.gameEnable = true;
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

    private void setupUser() {
        ccUser.setup("User");
        ccUser.get().options().copyDefaults(true);
        ccUser.save();
    }

    private void setupSound() {
        ccSound.setup("Sound");
        ccSound.get().options().header("Enter 0 to play nothing.\nhttps://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html");
        //ccSound.get().addDefault("RoundStart","0");
        ccSound.get().addDefault("Bet", "ENTITY_PAINTING_PLACE");
        ccSound.get().addDefault("CashOut", "ENTITY_EXPERIENCE_ORB_PICKUP");
        ccSound.get().addDefault("Bust", "ENTITY_PLAYER_HURT");
        //ccSound.get().addDefault("RoundEnd","ENTITY_CHICKEN_EGG");
        ccSound.get().addDefault("Click", "0");
        ccSound.get().options().copyDefaults(true);
        ccSound.save();
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();

        ccConfig.reload();
        ccBank.reload();
        ccUser.reload();
        ccSound.reload();
        updateConfig();
        Game.refreshIcons();
    }

    public void reloadMessages() {
        File file = new File(getDataFolder(), "messages.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (Messages.reload(config)) try {
            config.save(file);
        } catch (IOException e) {
            log(null, e);
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

        Game.maxMulti = ccConfig.get().getInt("MultiplierMax");

        Game.baseInstabust = ccConfig.get().getDouble("ProbabilityOfInstaBust") / 100 - Game.oddList[Game.maxMulti - 1];
        if (Game.baseInstabust < 0) Game.baseInstabust = 0;

        ccConfig.save();
    }

    @Override
    public void onDisable() {
        log("Disabled");
    }

}
