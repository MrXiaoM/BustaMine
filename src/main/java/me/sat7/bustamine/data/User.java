package me.sat7.bustamine.data;

import org.bukkit.OfflinePlayer;

public class User {
    private final OfflinePlayer player;
    int cashOut = -1;
    int gamesPlayed;
    double netProfit;
    int netProfitExp;
    long lastJoin;
    String lastName;

    public User(OfflinePlayer player) {
        this.player = player;
    }

    public OfflinePlayer getPlayer() {
        return player;
    }

    public int getCashOut() {
        return cashOut;
    }

    public void setCashOut(int cashOut) {
        this.cashOut = cashOut;
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
