package me.sat7.bustamine.data;

import org.bukkit.OfflinePlayer;

public class User {
    private final OfflinePlayer player;
    /**
     * 自动抛售倍率阈值 (<code>100 == x1.00</code>)，<code>-1</code> 禁用自动抛售
     */
    int autoCashOut = -1;
    /**
     * 进行游戏的次数
     */
    int gamesPlayed;
    /**
     * 净利润金币
     */
    double netProfit;
    /**
     * 净利润经验
     */
    int netProfitExp;
    /**
     * 上次加入游戏时间
     */
    long lastJoin;
    /**
     * 最后一次登录的游戏名
     */
    String lastName;

    public User(OfflinePlayer player) {
        this.player = player;
    }

    public OfflinePlayer getPlayer() {
        return player;
    }

    public int getAutoCashOut() {
        return autoCashOut;
    }

    public void setAutoCashOut(int autoCashOut) {
        this.autoCashOut = autoCashOut;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public double getNetProfit() {
        return netProfit;
    }

    public void setNetProfit(double netProfit) {
        this.netProfit = netProfit;
    }

    public int getNetProfitExp() {
        return netProfitExp;
    }

    public void setNetProfitExp(int netProfitExp) {
        this.netProfitExp = netProfitExp;
    }

    public long getLastJoin() {
        return lastJoin;
    }

    public void setLastJoin(long lastJoin) {
        this.lastJoin = lastJoin;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
