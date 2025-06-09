package me.sat7.bustamine.config;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public enum Messages {
    Message_Prefix("§6§l[BustaMine]§r "),
    Message_Instabust("§4Instabust!"),
    Message_Start("§fGame started."),
    Message_Stop("§fGame will be stopped."),
    Message_NotEnoughMoney("§eNot Enough Money."),
    Message_NotEnoughExp("§eNot Enough Exp."),
    Message_DivUpper("§6╔══════════════╗"),
    Message_DivLower("§6╚══════════════╝"),
    Message_NoPermission("§eYou do not have permission to use this command."),
    Message_Reload_Normal("§fReloaded."),
    Message_Reload_FromNextRound("§fReloaded. Changes will take effect from the next round."),
    Message_PlayerNotExist("§fPlayer does not exist."),
    Message_LastUpdate("Last update: {sec} seconds ago"),

    UI_Title("§2[ Busta Mine ]"),
    UI_BetBtn("§6§lBet"),
    UI_CashOut("§6§lCash Out"),
    UI_History("§6§lHistory"),
    UI_Bankroll("§6§lBankroll"),
    UI_PlayerInfo("§fBet: {amount}"),
    UI_MyState("§6§lMy State"),
    UI_Click("§eClick"),
    UI_WinChance("§6§lWin chance"),
    UI_Close("§6§lClose"),
    UI_CashOutSetting("§6§lAuto Cash Out"),

    CO_Title("§2[ Auto Cash Out ]"),
    CO_Minus10("§6§l-10"),
    CO_Minus1("§6§l-1"),
    CO_Minus01("§6§l-0.1"),
    CO_Plus10("§6§l+10"),
    CO_Plus1("§6§l+1"),
    CO_Plus01("§6§l+0.1"),
    CO_x("§fx"),
    CO_Enabled("§fEnabled"),
    CO_Disabled("§fDisabled"),
    CO_OnOff("§6§lOn/Off"),
    CO_PlayMoneyGame("§6§lGo to Money Game"),
    CO_PlayExpGame("§6§lGo to Exp Game"),

    Help_BmGo("/bm go   §eStart game (Auto repeat)"),
    Help_BmStop("/bm stop   §eThe game will be stopped."),
    Help_BmStatistics("/bm statistics   §eShow statistics."),
    Help_BmReloadConfig("/bm reloadConfig   §eReload config files."),
    Help_BmReloadLang("/bm reloadLang   §eReload Lang.yml"),
    Help_BmReloadLangWarning("§cWarning! Reload server or '/bm reloadLang' will terminate current round."),
    Help_BmTest("/bm test   §eGenerate random bust numbers 100000 times."),

    MyBal("My Balance"),
    Bal("Balance"),
    Money("Money"),
    Exp("Exp"),
    MaximumMultiplier("Max"),
    Bet("Bet"),
    CashedOut("Cashed Out"),
    Busted("Busted"),
    Income("Income"),
    Expense("Expense"),
    Profit("Profit"),
    NetProfit("Net Profit"),
    GamesPlayed("Games Played"),
    Leaderboard("Leaderboard"),
    BettingLimit("§fYou've exceeded betting limit"),

    ;
    private static String prefix = "";
    private final String key;
    private final String defaultValue;
    private String currentValue;
    Messages(String defaultValue) {
        this.key = name().replace("_", ".");
        this.defaultValue = defaultValue;
        this.currentValue = defaultValue;
    }

    public String get() {
        return currentValue;
    }

    public boolean t(@NotNull CommandSender sender) {
        return msg(sender, get());
    }

    public static String prefix() {
        return prefix;
    }

    public static boolean reload(ConfigurationSection config) {
        boolean updated = false;
        for (Messages message : values()) {
            if (config.contains(message.key)) {
                message.currentValue = config.getString(message.key, message.defaultValue);
            } else {
                message.currentValue = message.defaultValue;
                config.set(message.key, message.defaultValue);
                updated = true;
            }
        }
        prefix = Message_Prefix.get();
        return updated;
    }

    public static boolean msg(CommandSender sender, String message) {
        sender.sendMessage(prefix + message);
        return true;
    }
}
