package me.sat7.bustamine;

import me.sat7.bustamine.commands.CommandMain;
import me.sat7.bustamine.listeners.OnClick;
import me.sat7.bustamine.listeners.OnJoinLeave;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public final class BustaMine extends JavaPlugin implements Listener {

    public static BustaMine plugin;
    public static ConsoleCommandSender console;
    public static final String consolePrefix = "§6[BustaMine]§f ";
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

    @Override
    public void onEnable() {
        plugin = this;
        console = plugin.getServer().getConsoleSender();

        setupVault();
    }

    private void setupVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            console.sendMessage(consolePrefix + " Disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        } else {
            console.sendMessage(consolePrefix + " Vault Found");
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
                console.sendMessage(consolePrefix + " Disabled due to no Vault dependency found!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            setupRspRetryCount++;
            console.sendMessage(consolePrefix + " Economy provider not found. Retry... " + setupRspRetryCount + "/3");

            Bukkit.getScheduler().runTaskLater(this, this::setupRSP, 30L);
        }
    }

    private void init() {
        ccConfig = new CustomConfig();
        ccBank = new CustomConfig();
        ccUser = new CustomConfig();
        ccLang = new CustomConfig();
        ccSound = new CustomConfig();

        // 이벤트 등록
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new OnClick(), this);
        getServer().getPluginManager().registerEvents(new OnJoinLeave(), this);

        new CommandMain(this);

        Game.calcOdds();
        setupConfig();
        setupBank();
        setupUser();
        setupLang();
        setupSound();
        updateConfig();

        console.sendMessage(consolePrefix + "Enabled! :)");

        Game.setupGlass();
        Game.setupSortedMap();
        Game.gameGUISetup();

        // 첫 게임 시작
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

    private void setupLang() {
        ccLang.setup("Lang");
        ccLang.get().addDefault("Message.Prefix", "§6§l[BustaMine]§r ");
        ccLang.get().addDefault("Message.Instabust", "§4Instabust!");
        ccLang.get().addDefault("Message.Start", "§fGame started.");
        ccLang.get().addDefault("Message.Stop", "§fGame will be stopped.");
        ccLang.get().addDefault("Message.NotEnoughMoney", "§eNot Enough Money.");
        ccLang.get().addDefault("Message.NotEnoughExp", "§eNot Enough Exp.");
        ccLang.get().addDefault("Message.DivUpper", "§6╔══════════════╗");
        ccLang.get().addDefault("Message.DivLower", "§6╚══════════════╝");
        ccLang.get().addDefault("Message.NoPermission", "§eYou do not have permission to use this command.");
        ccLang.get().addDefault("Message.Reload2", "§fReloaded.");
        ccLang.get().addDefault("Message.Reload_FromNextRound", "§fReloaded. Changes will take effect from the next round.");
        ccLang.get().addDefault("Message.PlayerNotExist", "§fPlayer does not exist.");
        ccLang.get().addDefault("Message.LastUpdate", "Last update: {sec} seconds ago");

        ccLang.get().addDefault("UI.Title", "§2[ Busta Mine ]");
        ccLang.get().addDefault("UI.BetBtn", "§6§lBet");
        ccLang.get().addDefault("UI.CashOut", "§6§lCash Out");
        ccLang.get().addDefault("UI.History", "§6§lHistory");
        ccLang.get().addDefault("UI.Bankroll", "§6§lBankroll");
        ccLang.get().addDefault("UI.PlayerInfo", "§fBet: {amount}");
        ccLang.get().addDefault("UI.MyState", "§6§lMy State");
        ccLang.get().addDefault("UI.Click", "§eClick");
        ccLang.get().addDefault("UI.WinChance", "§6§lWin chance");
        ccLang.get().addDefault("UI.Close", "§6§lClose");
        ccLang.get().addDefault("UI.CashOutSetting", "§6§lAuto Cash Out");

        ccLang.get().addDefault("CO.Title", "§2[ Auto Cash Out ]");
        ccLang.get().addDefault("CO.-10", "§6§l-10");
        ccLang.get().addDefault("CO.-1", "§6§l-1");
        ccLang.get().addDefault("CO.-01", "§6§l-0.1");
        ccLang.get().addDefault("CO.+10", "§6§l+10");
        ccLang.get().addDefault("CO.+1", "§6§l+1");
        ccLang.get().addDefault("CO.+01", "§6§l+0.1");
        ccLang.get().addDefault("CO.x", "§fx");
        ccLang.get().addDefault("CO.Enabled", "§fEnabled");
        ccLang.get().addDefault("CO.Disabled", "§fDisabled");
        ccLang.get().addDefault("CO.OnOff", "§6§lOn/Off");
        ccLang.get().addDefault("CO.PlayMoneyGame", "§6§lGo to Money Game");
        ccLang.get().addDefault("CO.PlayExpGame", "§6§lGo to Exp Game");

        ccLang.get().addDefault("Help.BmGo", "/bm go   §eStart game (Auto repeat)");
        ccLang.get().addDefault("Help.BmStop", "/bm stop   §eThe game will be stopped.");
        ccLang.get().addDefault("Help.BmStatistics", "/bm statistics   §eShow statistics.");
        ccLang.get().addDefault("Help.BmReloadConfig", "/bm reloadConfig   §eReload config files.");
        ccLang.get().addDefault("Help.BmReloadLang", "/bm reloadLang   §eReload Lang.yml");
        ccLang.get().addDefault("Help.BmReloadLangWarning", "§cWarning! Reload server or '/bm reloadLang' will terminate current round.");
        ccLang.get().addDefault("Help.BmTest", "/bm test   §eGenerate random bust numbers 100000 times.");

        ccLang.get().addDefault("MyBal", "My Balance");
        ccLang.get().addDefault("Bal", "Balance");
        ccLang.get().addDefault("Money", "Money");
        ccLang.get().addDefault("Exp", "Exp");
        ccLang.get().addDefault("MaximumMultiplier", "Max");
        ccLang.get().addDefault("Bet", "Bet");
        ccLang.get().addDefault("CashedOut", "Cashed Out");
        ccLang.get().addDefault("Busted", "Busted");
        ccLang.get().addDefault("Income", "Income");
        ccLang.get().addDefault("Expense", "Expense");
        ccLang.get().addDefault("Profit", "Profit");
        ccLang.get().addDefault("NetProfit", "Net Profit");
        ccLang.get().addDefault("GamesPlayed", "Games Played");
        ccLang.get().addDefault("Leaderboard", "Leaderboard");
        ccLang.get().addDefault("BettingLimit", "§fYou've exceeded betting limit");

        ccLang.get().options().copyDefaults(true);
        ccLang.save();
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

    public static void updateConfig() {
        if (ccConfig.get().contains("Bankroll")) {
            ccBank.get().set("Bankroll.Money", ccConfig.get().getDouble("Bankroll"));
            ccBank.save();
            ccConfig.get().set("Bankroll", null);
        }

        //--------------------------------

        prefix = ccLang.get().getString("Message.Prefix");

        if (ccConfig.get().getInt("MultiplierMax") > 150) ccConfig.get().set("MultiplierMax", 150);
        if (ccConfig.get().getInt("MultiplierMax") < 30) ccConfig.get().set("MultiplierMax", 30);

        if (ccConfig.get().getDouble("ProbabilityOfInstaBust") > 20) ccConfig.get().set("ProbabilityOfInstaBust", 20.0);
        if (ccConfig.get().getDouble("ProbabilityOfInstaBust") < 0.8) ccConfig.get().set("ProbabilityOfInstaBust", 0.8);

        if (ccConfig.get().getInt("RoundInterval") < 3) ccConfig.get().set("RoundInterval", 3);

        Game.maxMulti = ccConfig.get().getInt("MultiplierMax");

        Game.baseInstabust = ccConfig.get().getDouble("ProbabilityOfInstaBust") / 100 - Game.oddList[Game.maxMulti - 1];
        if (Game.baseInstabust < 0) Game.baseInstabust = 0;
        //System.out.println("base:"+Game.baseInstabust);

        ccConfig.save();
    }

    @Override
    public void onDisable() {
        console.sendMessage(consolePrefix + "Disabled");
    }

}
