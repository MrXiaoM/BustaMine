package me.sat7.bustamine.config;

import com.google.common.collect.Lists;
import me.sat7.bustamine.BustaMine;
import me.sat7.bustamine.utils.CustomConfig;
import me.sat7.bustamine.utils.Property;

import java.util.ArrayList;
import java.util.List;

import static me.sat7.bustamine.utils.Property.property;

public class Config extends CustomConfig implements Property.IPropertyRegistry {
    private final List<Property<?>> registeredProperties = new ArrayList<>();
    public final Property<String> currencySymbol = property(this, "CurrencySymbol", "$");
    public final Property<Integer> roundInterval = property(this, "RoundInterval", 30);
    public final Property<Integer> multiplierMax = property(this, "MultiplierMax", 120);
    public final Property<Double> probabilityOfInstaBust = property(this, "ProbabilityOfInstaBust", 2.0);

    public final Property<Boolean> isShowWinChance = property(this, "ShowWinChance", true);
    public final Property<Boolean> isShowBankroll = property(this, "ShowBankroll", true);
    public final Property<Boolean> isLoadPlayerSkin = property(this, "LoadPlayerSkin", true);
    public final Property<Boolean> isForceUpdateUI = property(this, "UIForceUpdate", false);

    public final Property<Integer> betSmall = property(this, "Bet.Small", 10);
    public final Property<Integer> betMedium = property(this, "Bet.Medium", 100);
    public final Property<Integer> betBig = property(this, "Bet.Big", 1000);
    public final Property<Integer> betMax = property(this, "Bet.Max", 5000);
    public final Property<Integer> betExpSmall = property(this, "Bet.ExpSmall", 10);
    public final Property<Integer> betExpMedium = property(this, "Bet.ExpMedium", 100);
    public final Property<Integer> betExpBig = property(this, "Bet.ExpBig", 1000);
    public final Property<Integer> betExpMax = property(this, "Bet.ExpMax", 5000);

    public final Property<Integer> broadcastJackpot = property(this, "Broadcast.Jackpot", 30);
    public final Property<Boolean> isBroadcastInstaBust = property(this, "Broadcast.InstaBust", true);

    public final Property<String> commandRoundStart = property(this, "Command.WhenRoundStart", "");
    public final Property<String> commandPlayerBet = property(this, "Command.WhenPlayerBet", "");
    public final Property<String> commandPlayerCashOut = property(this, "Command.WhenPlayerCashOut", "");
    public final Property<String> commandRoundEnd = property(this, "Command.WhenRoundEnd", "");

    public Config(BustaMine plugin) {
        super(plugin);
    }

    @Override
    public void registerProperty(Property<?> property) {
        registeredProperties.add(property);
    }

    @SuppressWarnings("deprecation")
    public void setup() {
        setup("config", config -> {
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

            for (Property<?> property : registeredProperties) {
                property.setup();
            }

            config.options().copyDefaults(true);
        });
        save();
    }

    private void reloadConfig() {
        for (Property<?> property : registeredProperties) {
            property.reload();
        }
    }

    public void updateConfig() {
        if (contains("Bankroll")) {
            plugin.bank().set("Bankroll.Money", getDouble("Bankroll"));
            plugin.bank().save();
            remove("Bankroll");
        }

        rangeInt("MultiplierMax", 30, 150);
        rangeDouble("ProbabilityOfInstaBust", 0.8, 20.0);

        rangeInt("RoundInterval", 3, null);

        reloadConfig();

        plugin.game().reload();
        save();
    }
}
