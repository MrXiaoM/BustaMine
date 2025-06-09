package me.sat7.bustamine.config;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public enum Messages {
    Message_Prefix("§7[§6§lBustaMine§7]§r "),
    Message_InstantBust("§4立即归零!"),
    Message_Start("§f游戏开始."),
    Message_Stop("§f游戏即将停止."),
    Message_NotEnoughMoney("§e没有足够的金钱."),
    Message_NotEnoughExp("§e没有足够的经验."),
    Message_DivUpper("§6╔══════════════╗"),
    Message_DivLower("§6╚══════════════╝"),
    Message_NoPermission("§e你无权使用此命令."),
    Message_Reload_Normal("§f插件配置已重载."),
    Message_Reload_FromNextRound("§f插件配置已重载，将在下一轮游戏生效."),
    Message_PlayerNotExist("§f玩家不存在."),
    Message_LastUpdate("最后更新: {sec} 秒前"),

    UI_Title("§2[ Busta Mine ]"),
    UI_BetBtn("§6§l买入"),
    UI_CashOut("§6§l抛售"),
    UI_History("§6§l历史倍数"),
    UI_Bankroll("§6§l资金"),
    UI_PlayerInfo("§f赌注: {amount}"),
    UI_MyState("§6§l我的交易状态"),
    UI_Click("§e点击"),
    UI_WinChance("§6§l获胜几率"),
    UI_Close("§6§l关闭"),
    UI_CashOutSetting("§6§l自动抛售"),

    CO_Title("§2[ 自动抛售 ]"),
    CO_Minus10("§6§l-10"),
    CO_Minus1("§6§l-1"),
    CO_Minus01("§6§l-0.1"),
    CO_Plus10("§6§l+10"),
    CO_Plus1("§6§l+1"),
    CO_Plus01("§6§l+0.1"),
    CO_x("§fx"),
    CO_Enabled("§f开启"),
    CO_Disabled("§f关闭"),
    CO_OnOff("§6§l开/关"),
    CO_PlayMoneyGame("§6§l返回金钱交易"),
    CO_PlayExpGame("§6§l返回经验交易"),

    Help_BmGo("/bm go   §e开始游戏 (自动循环开盘)"),
    Help_BmStop("/bm stop   §e停止一盘游戏."),
    Help_BmStatistics("/bm statistics   §e展示统计信息."),
    Help_BmReloadConfig("/bm reloadConfig   §e重载配置文件."),
    Help_BmReloadLang("/bm reloadLang   §e重载语言文件"),
    Help_BmReloadLangWarning("§c警告! 重启服务器或使用 '/bm reloadLang' 将停止当前游戏回合."),
    Help_BmTest("/bm test   §e随机生成倍数 100000 次 (用于测试概率)."),

    MyBal("我的资金"),
    Bal("资金"),
    Money("金币"),
    Exp("经验"),
    MaximumMultiplier("最大倍数"),
    Bet("下注"),
    CashedOut("出售"),
    Busted("归零"),
    Income("收入"),
    Expense("费用"),
    Profit("利润"),
    NetProfit("净利润"),
    GamesPlayed("游戏次数"),
    Leaderboard("排行榜"),
    BettingLimit("§f超过了下注限制"),

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
