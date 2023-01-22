package me.sat7.bustaMine;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Game
{
    private static BukkitTask bustaTask;
    private enum bustaState{bet,game,busted}
    private static bustaState bState = bustaState.bet;
    public enum bustaType{money,exp}

    public static boolean gameEnable;

    private static int betTimeLeft;
    private static int BetCountDown()
    {
        //Bukkit.getServer().broadcastMessage(BustaMine.prefix + betTimeLeft + "초 후 새 게임 시작");
        betTimeLeft -= 1;
        return betTimeLeft;
    }

    private static int bustNum;
    private static int curNum;
    private static int gameLoop()
    {
        int tempOld = curNum;
        int mod = 0;

        if(curNum < 115)
        {
            mod = 1;
        }
        else if(curNum < 180)
        {
            mod = 2;
        }
        else if(curNum < 360)
        {
            mod = 4;
        }
        else if(curNum < 720)
        {
            mod = 8;
        }
        else if(curNum < 1440)
        {
            mod = 16;
        }
        else
        {
            mod = 32;
        }

        for (String s:activePlayerMap.keySet()) {
            try {
                UUID uuid = UUID.fromString(s);
                if(BustaMine.ccUser.get().contains(uuid+".CashOut"))
                {
                    if(tempOld+mod >= BustaMine.ccUser.get().getInt(uuid + ".CashOut"))
                    {
                        Player p = Bukkit.getPlayer(uuid);
                        CashOut(p);
                    }
                }
            }catch (Exception ignore){}
        }

        curNum = tempOld+mod;
        return curNum;
    }
    private static int gameLoopDelay;

    static Inventory gameInven;
    static Inventory gameInven_exp;

    public static int maxMulti = 150;
    public static final double[] oddList = new double[150];
    public static double baseInstabust = 0;

    static final ArrayList<Integer> history = new ArrayList<>();

    static final DecimalFormat intf = new DecimalFormat("0");
    static final DecimalFormat df = new DecimalFormat("0.00");

    public static final HashMap<String,HashMap<String,Double>> sortedMap = new HashMap<>();
    public static final HashMap<String,Long> sortedTime = new HashMap<>();

    public static final HashMap<String, String> playerMap = new HashMap<>(); // uuid, 베팅타입
    public static final ConcurrentHashMap<String, Integer> activePlayerMap = new ConcurrentHashMap<>(); // uuid, 베팅금액
    public static final HashMap<String, Integer> headPos = new HashMap<>();

    private static final List<ItemStack> coloredGlass = new ArrayList<>();
    public static void SetupGlass()
    {
        for (short i = 0; i < 16; i++)
        {
            coloredGlass.add(new ItemStack(getGlass(i)));
        }
    }

    public static Material getGlass(int dataValue) {
        switch (dataValue) {
            case 0:
                return Material.WHITE_STAINED_GLASS_PANE;
            case 1:
                return Material.ORANGE_STAINED_GLASS_PANE;
            case 2:
                return Material.MAGENTA_STAINED_GLASS_PANE;
            case 3:
                return Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case 4:
                return Material.YELLOW_STAINED_GLASS_PANE;
            case 5:
                return Material.LIME_STAINED_GLASS_PANE;
            case 6:
                return Material.PINK_STAINED_GLASS_PANE;
            case 7:
                return Material.GRAY_STAINED_GLASS_PANE;
            case 8:
                return Material.LIGHT_GRAY_STAINED_GLASS_PANE;
            case 9:
                return Material.CYAN_STAINED_GLASS_PANE;
            case 10:
                return Material.PURPLE_STAINED_GLASS_PANE;
            case 11:
                return Material.BLUE_STAINED_GLASS_PANE;
            case 12:
                return Material.BROWN_STAINED_GLASS_PANE;
            case 13:
                return Material.GREEN_STAINED_GLASS_PANE;
            case 14:
                return Material.RED_STAINED_GLASS_PANE;
            case 15:
                return Material.BLACK_STAINED_GLASS_PANE;
            default:
                return null;
        }
    }

    public static void SetupSortedMap()
    {
        sortedMap.put("NetProfit",new HashMap<>());
        sortedMap.put("NetProfit_Exp",new HashMap<>());
        sortedMap.put("GamesPlayed",new HashMap<>());
        sortedTime.put("NetProfit",0L);
        sortedTime.put("NetProfit_Exp",0L);
        sortedTime.put("GamesPlayed",0L);
    }

    public static void CalcOdds()
    {
        for(int i = 1; i<=150; i++)
        {
            oddList[i-1] = 1/Math.pow(i,1.01);
            //BustaMine.console.sendMessage(i + ": "+oddList[i-1]);
        }
    }

    public static void StartGame()
    {
        if(bustaTask != null) bustaTask.cancel();

        playerMap.clear();
        activePlayerMap.clear();
        headPos.clear();
        for(int i = 0; i<45; i++)
        {
            ItemStack glass = new ItemStack(coloredGlass.get(9));
            ItemMeta tempM = glass.getItemMeta();
            tempM.setDisplayName(" ");
            glass.setItemMeta(tempM);

            gameInven.setItem(i,glass);
            gameInven_exp.setItem(i,glass);
        }

        bState = bustaState.bet;
        betTimeLeft = BustaMine.ccConfig.get().getInt("RoundInterval") + 1;

        // 확률정보 표시
        if(BustaMine.ccConfig.get().getBoolean("ShowWinChance"))
        {
            ItemStack winChance = CreateItemStack(Material.getMaterial(BustaMine.ccConfig.get().getString("BtnIcon.WinChance")),null,
                    BustaMine.ccLang.get().getString("UI.WinChance"), null,1);

            double bustchance = oddList[BustaMine.ccConfig.get().getInt("MultiplierMax")-1];
            ArrayList<String> winChanceArr = new ArrayList<>();
            winChanceArr.add("§ex2: " + df.format((oddList[1]-bustchance)*100*(1-baseInstabust)) + "%");
            winChanceArr.add("§ex3: " + df.format((oddList[2]-bustchance)*100*(1-baseInstabust)) + "%");
            winChanceArr.add("§ex7: " + df.format((oddList[6]-bustchance)*100*(1-baseInstabust)) + "%");
            winChanceArr.add("§ex12: " + df.format((oddList[11]-bustchance)*100*(1-baseInstabust)) + "%");
            winChanceArr.add("§eInstaBust: " + df.format((bustchance+baseInstabust)*100) + "%");
            winChanceArr.add("§e"+ BustaMine.ccLang.get().getString("MaximumMultiplier") +": x" + BustaMine.ccConfig.get().getInt("MultiplierMax"));

            ItemMeta tempMeta = winChance.getItemMeta();
            tempMeta.setLore(winChanceArr);
            winChance.setItemMeta(tempMeta);
            gameInven.setItem(46,winChance);
            gameInven_exp.setItem(46,winChance);
        }
        else
        {
            gameInven.setItem(46,null);
            gameInven_exp.setItem(46,null);
        }

        // 뱅크롤
        if(BustaMine.ccConfig.get().getBoolean("ShowBankroll"))
        {
            if(gameInven.getItem(45) == null)
            {
                //System.out.println("뱅크롤버튼 재생성");
                ItemStack bankrollBtn = CreateItemStack(Material.getMaterial(BustaMine.ccConfig.get().getString("BtnIcon.Bankroll")),null,
                        BustaMine.ccLang.get().getString("UI.Bankroll"), null,1);
                gameInven.setItem(45,bankrollBtn);
                gameInven_exp.setItem(45,bankrollBtn);
            }
            UpdateBankroll(bustaType.money,0);
            UpdateBankroll(bustaType.exp,0);
        }
        else
        {
            gameInven.setItem(45,null);
            gameInven_exp.setItem(45,null);
        }

        // 베팅 버튼들
        {
            ItemStack bet10Btn = CreateItemStack(Material.getMaterial(BustaMine.ccConfig.get().getString("BtnIcon.BetSmall")),null,
                    BustaMine.ccLang.get().getString("UI.BetBtn")+" §e" + BustaMine.ccConfig.get().getString("CurrencySymbol") + BustaMine.ccConfig.get().getInt("Bet.Small"), null,1);
            gameInven.setItem(51,bet10Btn);
            ItemStack betE1Btn = CreateItemStack(Material.getMaterial(BustaMine.ccConfig.get().getString("BtnIcon.BetSmall")),null,
                    BustaMine.ccLang.get().getString("UI.BetBtn")+" §eXp"+BustaMine.ccConfig.get().getInt("Bet.ExpSmall"), null,1);
            gameInven_exp.setItem(51,betE1Btn);

            // 100
            ItemStack bet100Btn = CreateItemStack(Material.getMaterial(BustaMine.ccConfig.get().getString("BtnIcon.BetMedium")),null,
                    BustaMine.ccLang.get().getString("UI.BetBtn")+" §e"+BustaMine.ccConfig.get().getString("CurrencySymbol") + BustaMine.ccConfig.get().getInt("Bet.Medium"), null,1);
            gameInven.setItem(52,bet100Btn);
            ItemStack betE2Btn = CreateItemStack(Material.getMaterial(BustaMine.ccConfig.get().getString("BtnIcon.BetMedium")),null,
                    BustaMine.ccLang.get().getString("UI.BetBtn")+" §eXp"+BustaMine.ccConfig.get().getInt("Bet.ExpMedium"), null,1);
            gameInven_exp.setItem(52,betE2Btn);

            // 1000
            ItemStack bet1000Btn = CreateItemStack(Material.getMaterial(BustaMine.ccConfig.get().getString("BtnIcon.BetBig")),null,
                    BustaMine.ccLang.get().getString("UI.BetBtn")+" §e"+BustaMine.ccConfig.get().getString("CurrencySymbol") + BustaMine.ccConfig.get().getInt("Bet.Big"), null,1);
            gameInven.setItem(53,bet1000Btn);
            ItemStack betE3Btn = CreateItemStack(Material.getMaterial(BustaMine.ccConfig.get().getString("BtnIcon.BetBig")),null,
                    BustaMine.ccLang.get().getString("UI.BetBtn")+" §eXp"+BustaMine.ccConfig.get().getInt("Bet.ExpBig"), null,1);
            gameInven_exp.setItem(53,betE3Btn);
        }

        ItemMeta im = gameInven.getItem(49).getItemMeta();
        ArrayList<String> betLore = new ArrayList<>();
        betLore.add("§b§l" + "Bet >>>");
        im.setLore(betLore);
        gameInven.getItem(49).setItemMeta(im);
        gameInven_exp.getItem(49).setItemMeta(im);

        // 베팅대기, 게임 시작
        bustaTask = Bukkit.getScheduler().runTaskTimer(BustaMine.plugin, () ->
        {
            if(BetCountDown() <= 0)
            {
                // 게임 시작
                bustaTask.cancel();

                for (Integer key:old.keySet()) {
                    gameInven.setItem(key,old.get(key));
                    gameInven_exp.setItem(key,old.get(key));
                }

                RunGame();

                for(int i = 0; i<45; i++)
                {
                    if(headPos.containsValue(i)) continue;

                    ItemStack glass = new ItemStack(coloredGlass.get(13));
                    ItemMeta tempM = glass.getItemMeta();
                    tempM.setDisplayName(" ");
                    glass.setItemMeta(tempM);

                    gameInven.setItem(i,glass);
                    gameInven_exp.setItem(i,glass);
                }

                for(int i = 51; i<=53; i++)
                {
                    ItemStack glass = new ItemStack(coloredGlass.get(13));
                    ItemMeta tempM = glass.getItemMeta();
                    tempM.setDisplayName(" ");
                    glass.setItemMeta(tempM);

                    gameInven.setItem(i,glass);
                    gameInven_exp.setItem(i,glass);
                }
            }
            else
            {
                // 카운트 다운
                ArrayList<String> nextRoundLore = new ArrayList<>();
                nextRoundLore.add("§b§l" + "Next round in " + betTimeLeft + "s");

                if(betTimeLeft <= 5) DrawNumber(betTimeLeft);

                for(int i = 51; i<=53; i++)
                {
                    ItemMeta tempIm = gameInven.getItem(i).getItemMeta();
                    tempIm.setLore(nextRoundLore);
                    gameInven.getItem(i).setItemMeta(tempIm);
                    ItemMeta tempIm2 = gameInven_exp.getItem(i).getItemMeta();
                    tempIm2.setLore(nextRoundLore);
                    gameInven_exp.getItem(i).setItemMeta(tempIm2);
                }
            }
        },0,20);
    }

    private static void RunGame()
    {
        bState = bustaState.game;
        curNum = 100;
        gameLoopDelay = 4;

        bustNum = GenBustNum();

        RunCmb_RoundStart();

        bustaTask = Bukkit.getScheduler().runTaskTimer(BustaMine.plugin, () ->
        {
            boolean instaBust = (bustNum == 100);

            if(gameLoop() > bustNum)
            {
                // 버스트!
                bustaTask.cancel();

                Bust(instaBust);

                // 뱅크롤 확인. 필요시 게임 정지
//                if(BustaMine.ccBank.get().getDouble("Bankroll.Money") < 0)
//                {
//                    gameEnable = false;
//                }

                // 다음게임 시작 대기
                if(gameEnable)
                {
                    bustaTask = Bukkit.getScheduler().runTaskLater(BustaMine.plugin, Game::StartGame,80);
                }

                return;
            }

            // 캐시아웃 버튼 lore 업데이트 (ex. x2.34)
            ArrayList<String> tempArr = new ArrayList<>();
            tempArr.add("§a§lx" + df.format(curNum / 100.0));

            ItemMeta im = gameInven.getItem(49).getItemMeta();
            im.setLore(tempArr);
            gameInven.getItem(49).setItemMeta(im);
            gameInven_exp.getItem(49).setItemMeta(im);

            if(BustaMine.ccConfig.get().getBoolean("UIForceUpdate", false))
            {
                for(String p : playerMap.keySet())
                {
                    if(p == null)
                        continue;

                    Player player = Bukkit.getPlayer(p);
                    if(player != null)
                        player.updateInventory();
                }
            }

        },0,gameLoopDelay);
    }

    //=====================================================

    public static int GenBustNum()
    {
        double randD = BustaMine.generator.nextDouble();

        if(randD < baseInstabust) return 100;

        randD = BustaMine.generator.nextDouble();

        for(int j = maxMulti; j >0; j--)
        {
            if(randD < oddList[j-1])
            {
                if(j == maxMulti) return 100;

                double temp = BustaMine.generator.nextDouble();
                if(j<=3 && BustaMine.generator.nextBoolean())
                {
                    temp *= 0.6;
                    if(BustaMine.generator.nextBoolean()) temp *= 0.4;
                }

                int tempInt = (int)((j+temp)*100);
                if(tempInt == 100) tempInt = 101;
                return tempInt;
            }
        }

        return 101;
    }

    private static void Bust(boolean instaBust)
    {
        bState = bustaState.busted;
        if(instaBust) curNum = 100;

        RunCmb_RoundEnd(curNum);

        if(instaBust && BustaMine.ccConfig.get().getBoolean("Broadcast.InstaBust"))
        {
            Bukkit.getServer().broadcastMessage(BustaMine.prefix + BustaMine.ccLang.get().getString("Message.Instabust"));
        }
        if(BustaMine.ccConfig.get().getInt("Broadcast.Jackpot")*100 <= curNum)
        {
            Bukkit.getServer().broadcastMessage(BustaMine.prefix + "§a§lBusted! : x" + df.format(curNum / 100.0));
        }

        // 버스트된 사람들 채팅창에 띄우기
        for(String s : playerMap.keySet())
        {
            try {
                for (String bustP:activePlayerMap.keySet()) {
                    try
                    {
                        UUID uuid = UUID.fromString(s);
                        UUID uuidBust = UUID.fromString(bustP);
                        BustaMine.plugin.getServer().getPlayer(uuid).sendMessage("§6♣ " + BustaMine.plugin.getServer().getPlayer(uuidBust).getName() + " §4" + BustaMine.ccLang.get().getString("Busted"));
                    }catch (Exception ignored){}
                }
            }catch (Exception ignored){}
        }
        for(String bustP:activePlayerMap.keySet())
        {
            try
            {
                UUID uuidBust = UUID.fromString(bustP);
                PlayerSoundEffect(BustaMine.plugin.getServer().getPlayer(uuidBust),"Bust");
            }catch (Exception ignored){}
        }

        // 캐시아웃버튼 lore 빨간색으로 업데이트
        ArrayList<String> tempArr = new ArrayList<>();
        tempArr.add("§c§lx" + df.format(curNum / 100.0));
        ItemMeta im = gameInven.getItem(49).getItemMeta();
        im.setLore(tempArr);
        gameInven.getItem(49).setItemMeta(im);
        gameInven_exp.getItem(49).setItemMeta(im);

        // 배경
        for(int i = 0; i<45; i++)
        {
            if(!headPos.containsValue(i))
            {
                ItemStack glass = new ItemStack(coloredGlass.get(14));
                ItemMeta tempM = glass.getItemMeta();
                tempM.setDisplayName(" ");
                glass.setItemMeta(tempM);

                gameInven.setItem(i,glass);
                gameInven_exp.setItem(i,glass);
            }
        }

        for(int i = 51; i<=53; i++)
        {
            ItemStack glass = new ItemStack(coloredGlass.get(14));
            ItemMeta tempM = glass.getItemMeta();
            tempM.setDisplayName(" ");
            glass.setItemMeta(tempM);

            gameInven.setItem(i,glass);
            gameInven_exp.setItem(i,glass);
        }

        // 히스토리 추가
        history.add(curNum);
        if(history.size()>16)history.remove(0);

        // 히스토리 갱신
        ArrayList<String> historyArr = new ArrayList<>();
        for(int i : history)
        {
            if(i>=200)
            {
                historyArr.add("§ax" + df.format(i / 100.0));
            }
            else
            {
                historyArr.add("§cx" + df.format(i / 100.0));
            }
        }

        ItemMeta im2 = gameInven.getItem(48).getItemMeta();
        im2.setLore(historyArr);
        gameInven.getItem(48).setItemMeta(im2);
        gameInven_exp.getItem(48).setItemMeta(im2);

        // 데이터 저장
        BustaMine.ccBank.save();
        BustaMine.ccUser.save();
    }

    public static void Bet(Player p, bustaType type, int amount)
    {
        // 이미 게임진행중.
        if(bState != bustaState.bet) return;

        // 이미 참여함
        boolean firstBet = !activePlayerMap.containsKey(p.getUniqueId().toString());
        int old = 0;
        if(activePlayerMap.containsKey(p.getUniqueId().toString())) old = activePlayerMap.get(p.getUniqueId().toString());

        // 다른 유형으로 참가중
        if(playerMap.containsKey(p.getUniqueId().toString()) && !playerMap.get(p.getUniqueId().toString()).equals(type.toString()))
        {
            //p.sendMessage("다른 유형으로 참가중");
            return;
        }

        // 돈으로 참가
        if(type == bustaType.money)
        {
            if(old+amount > BustaMine.ccConfig.get().getInt("Bet.Max"))
            {
                p.sendMessage(BustaMine.prefix+BustaMine.ccLang.get().getString("BettingLimit"));
                return;
            }

            if(BustaMine.getEconomy().getBalance(p) >= amount)
            {
                EconomyResponse r = BustaMine.getEconomy().withdrawPlayer(p, amount);

                if(!r.transactionSuccess())
                {
                    p.sendMessage(String.format("An error occured: %s", r.errorMessage));
                    return;
                }

                PlayerSoundEffect(p,"Bet");
                RunCmd_Bet(p,amount);
                activePlayerMap.put(p.getUniqueId().toString(), old+amount);
                p.sendMessage(BustaMine.ccLang.get().getString("Message.DivUpper"));
                p.sendMessage("   §f"+BustaMine.ccLang.get().getString("Bet") + BustaMine.ccConfig.get().getString("CurrencySymbol") + (old+amount));
                p.sendMessage("   §e"+BustaMine.ccLang.get().getString("MyBal") + ": " + BustaMine.ccConfig.get().getString("CurrencySymbol") + df.format(BustaMine.getEconomy().getBalance(p)));
                p.sendMessage(BustaMine.ccLang.get().getString("Message.DivLower"));

                // 브로드캐스트
                if(firstBet)
                {
                    for(String s : playerMap.keySet())
                    {
                        if(p.getUniqueId().toString().equals(s)) continue;
                        try {
                            UUID uuid = UUID.fromString(s);
                            BustaMine.plugin.getServer().getPlayer(uuid).sendMessage(
                                    "§6♣ " + p.getName() + " " + BustaMine.ccLang.get().getString("Bet") + BustaMine.ccConfig.get().getString("CurrencySymbol") +  df.format(old+amount) );
                        }catch (Exception ignored){}
                    }
                }
            }
            else
            {
                p.sendMessage(BustaMine.prefix + BustaMine.ccLang.get().getString("Message.NotEnoughMoney"));
                p.sendMessage(BustaMine.ccLang.get().getString("MyBal")+": "+BustaMine.ccConfig.get().getString("CurrencySymbol") + df.format(BustaMine.getEconomy().getBalance(p)));
                return;
            }
        }
        // 경험치로 참가
        else
        {
            if(old+amount > BustaMine.ccConfig.get().getInt("Bet.ExpMax"))
            {
                p.sendMessage(BustaMine.prefix+BustaMine.ccLang.get().getString("BettingLimit"));
                return;
            }

            if(CalcTotalExp(p) >= amount)
            {
                p.giveExp(-amount);

                RunCmd_Bet(p,amount);
                activePlayerMap.put(p.getUniqueId().toString(), old + amount);
                p.sendMessage(BustaMine.ccLang.get().getString("Message.DivUpper"));
                p.sendMessage("   §f"+BustaMine.ccLang.get().getString("Bet") + " Xp" + (old+amount));
                p.sendMessage("   §e"+BustaMine.ccLang.get().getString("MyBal") + ": Xp" + CalcTotalExp(p));
                p.sendMessage(BustaMine.ccLang.get().getString("Message.DivLower"));

                // 브로드캐스트
                if(firstBet)
                {
                    for(String s : playerMap.keySet())
                    {
                        if(p.getUniqueId().toString().equals(s)) continue;
                        try {
                            UUID uuid = UUID.fromString(s);
                            BustaMine.plugin.getServer().getPlayer(uuid).sendMessage(
                                    "§6♣ " + p.getName() + " " + BustaMine.ccLang.get().getString("Bet") + " Xp" + (old+amount));
                        }catch (Exception ignored){}
                    }
                }
            }
            else
            {
                p.sendMessage(BustaMine.prefix + BustaMine.ccLang.get().getString("Message.NotEnoughExp"));
                p.sendMessage(BustaMine.ccLang.get().getString("MyBal")+": Xp"+CalcTotalExp(p));
                return;
            }
        }

        UpdateBankroll(bustaType.valueOf(type.toString()), amount);
        UpdateNetProfit(p,bustaType.valueOf(type.toString()), -amount);

        if(firstBet)
        {
            playerMap.put(p.getUniqueId().toString(), type.toString());
            BustaMine.ccUser.get().set(p.getUniqueId() +".GamesPlayed",BustaMine.ccUser.get().getInt(p.getUniqueId() +".GamesPlayed")+1);

            // 머리 추가
            if(playerMap.size()<43)
            {
                Material m = Material.PLAYER_HEAD;

                ItemStack skull = new ItemStack(m, 1);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();

                if(BustaMine.ccConfig.get().getBoolean("LoadPlayerSkin"))
                {
                    Bukkit.getScheduler().runTaskAsynchronously(BustaMine.plugin,()-> LoadAndSetSkin(p,playerMap.size()-1));
                }

                meta.setDisplayName("§6"+p.getName());
                ArrayList<String> lore = new ArrayList<>();
                if(type == bustaType.money)
                {
                    lore.add(BustaMine.ccLang.get().getString("UI.PlayerInfo").replace("{amount}",BustaMine.ccConfig.get().getString("CurrencySymbol") + amount));
                }
                else
                {
                    lore.add(BustaMine.ccLang.get().getString("UI.PlayerInfo").replace("{amount}","Xp"+amount));
                }
                meta.setLore(lore);
                skull.setItemMeta(meta);

                gameInven.setItem(playerMap.size()-1,skull);
                gameInven_exp.setItem(playerMap.size()-1,skull);

                headPos.put(p.getUniqueId().toString(),playerMap.size()-1);
            }
        }
        else
        {
            try
            {
                // 머리 갱신
                if(headPos.containsKey(p.getUniqueId().toString()))
                {
                    int idx = headPos.get(p.getUniqueId().toString());
                    ItemStack tempIS = gameInven.getItem(idx);
                    ItemMeta tempMeta = tempIS.getItemMeta();
                    ArrayList<String> lore = new ArrayList<>();
                    if(type == bustaType.money)
                    {
                        lore.add(BustaMine.ccLang.get().getString("UI.PlayerInfo").replace("{amount}",BustaMine.ccConfig.get().getString("CurrencySymbol") + (old+amount)));
                    }
                    else
                    {
                        lore.add(BustaMine.ccLang.get().getString("UI.PlayerInfo").replace("{amount}","Xp"+(old+amount)));
                    }
                    tempMeta.setLore(lore);
                    tempIS.setItemMeta(tempMeta);
                    gameInven.setItem(idx,tempIS);
                    gameInven_exp.setItem(idx,tempIS);
                }
            }catch (Exception e)
            {
                BustaMine.console.sendMessage(BustaMine.consolePrefix+"Failed to update UI. Game/Bet/!firstBet");
                BustaMine.console.sendMessage(BustaMine.consolePrefix+e);
            }
        }
    }

    private static void LoadAndSetSkin(Player p, int idx)
    {
        ItemStack tempIs = gameInven.getItem(idx);
        SkullMeta meta = (SkullMeta)tempIs.getItemMeta();
        try
        {
            meta.setOwningPlayer(p);
        }catch (Exception e)
        {
            BustaMine.console.sendMessage(BustaMine.prefix + "Failed to load player skin");
        }

        tempIs.setItemMeta(meta);
        gameInven.setItem(idx,tempIs);
        gameInven_exp.setItem(idx,tempIs);
    }

    public static void CashOut(Player p)
    {
        // 참여 안함
        if(!activePlayerMap.containsKey(p.getUniqueId().toString())) return;
        // 게임 진행중이 아님
        if(bState != bustaState.game) return;

        double bet = activePlayerMap.get(p.getUniqueId().toString());
        double prize = bet * (curNum/100.0);

        RunCmd_Cashout(p,bet,curNum,prize);
        PlayerSoundEffect(p,"CashOut");

        p.sendMessage(BustaMine.ccLang.get().getString("Message.DivUpper"));
        p.sendMessage("   §f"+BustaMine.ccLang.get().getString("CashedOut") + ": x" + df.format(curNum / 100.0));
        if(bustaType.valueOf(playerMap.get(p.getUniqueId().toString())) == bustaType.money)
        {
            p.sendMessage("   §3"+BustaMine.ccLang.get().getString("Profit") + ": " + BustaMine.ccConfig.get().getString("CurrencySymbol") + df.format(prize - bet));
            BustaMine.getEconomy().depositPlayer(p,prize);
            p.sendMessage("   §e"+BustaMine.ccLang.get().getString("MyBal") + ": " + BustaMine.ccConfig.get().getString("CurrencySymbol") + df.format(BustaMine.getEconomy().getBalance(p)));
        }
        else
        {
            p.sendMessage("   §3"+BustaMine.ccLang.get().getString("Profit") + ": Xp" + (int)((int)prize - bet));
            p.giveExp((int)prize);
            p.sendMessage("   §e"+BustaMine.ccLang.get().getString("MyBal") + ": Xp" + CalcTotalExp(p));
        }
        p.sendMessage(BustaMine.ccLang.get().getString("Message.DivLower"));

        activePlayerMap.remove(p.getUniqueId().toString());

        if(headPos.containsKey(p.getUniqueId().toString()))
        {
            ItemStack out = new ItemStack(coloredGlass.get(11));
            ItemStack head = gameInven.getItem(headPos.get(p.getUniqueId().toString()));

            ArrayList<String> lore = new ArrayList<>(head.getItemMeta().getLore());
            lore.add("§f"+BustaMine.ccLang.get().getString("CashedOut") + ": x" + df.format(curNum / 100.0));

            ItemMeta outMeta = out.getItemMeta();
            outMeta.setDisplayName(head.getItemMeta().getDisplayName());
            outMeta.setLore(lore);
            out.setItemMeta(outMeta);

            gameInven.setItem(headPos.get(p.getUniqueId().toString()),out);
            gameInven_exp.setItem(headPos.get(p.getUniqueId().toString()),out);
            headPos.remove(p.getUniqueId().toString());
        }

        for(String s : playerMap.keySet())
        {
            if(p.getUniqueId().toString().equals(s)) continue;
            try {
                UUID uuid = UUID.fromString(s);
                BustaMine.plugin.getServer().getPlayer(uuid).sendMessage(
                        "§6♣ " + p.getName() + " " + BustaMine.ccLang.get().getString("CashedOut") + " x" + df.format(curNum / 100.0) );
            }catch (Exception ignored){}
        }

        UpdateBankroll(bustaType.valueOf(playerMap.get(p.getUniqueId().toString())),-prize);
        UpdateNetProfit(p,bustaType.valueOf(playerMap.get(p.getUniqueId().toString())),prize);
    }

    private static void UpdateBankroll(bustaType type, double amount)
    {
        double old;
        if(type == bustaType.money)
        {
            old = BustaMine.ccBank.get().getDouble("Bankroll.Money");
            BustaMine.ccBank.get().set("Bankroll.Money",old + amount);
        }
        else
        {
            old = BustaMine.ccBank.get().getInt("Bankroll.Exp");
            BustaMine.ccBank.get().set("Bankroll.Exp",(int)(old + amount));
        }

        if(BustaMine.ccConfig.get().getBoolean("ShowBankroll"))
        {
            ArrayList<String> tempArr = new ArrayList<>();
            tempArr.add("§e"+BustaMine.ccConfig.get().getString("CurrencySymbol") +  (int)(BustaMine.ccBank.get().getDouble("Bankroll.Money")/1000) + "K");
            tempArr.add("§eXp"+ (BustaMine.ccBank.get().getInt("Bankroll.Exp")/1000) + "K");

            ItemMeta tempIm = gameInven.getItem(45).getItemMeta();
            tempIm.setLore(tempArr);
            gameInven.getItem(45).setItemMeta(tempIm);
            gameInven_exp.getItem(45).setItemMeta(tempIm);
        }

        if(amount > 0)
        {
            if(type == bustaType.money)
            {
                BustaMine.ccBank.get().set("Statistics.Income.Money", BustaMine.ccBank.get().getDouble("Statistics.Income.Money") + amount);
            }
            else
            {
                BustaMine.ccBank.get().set("Statistics.Income.Exp", (int)(BustaMine.ccBank.get().getInt("Statistics.Income.Exp") + amount));
            }
        }
        else
        {
            if(type == bustaType.money)
            {
                BustaMine.ccBank.get().set("Statistics.Expense.Money", BustaMine.ccBank.get().getDouble("Statistics.Expense.Money") + amount);
            }
            else
            {
                BustaMine.ccBank.get().set("Statistics.Expense.Exp", (int)(BustaMine.ccBank.get().getInt("Statistics.Expense.Exp") + amount));
            }
        }
    }

    private static void UpdateNetProfit(Player p, bustaType type, double amount)
    {
        double old;
        if(type == bustaType.money)
        {
            old = BustaMine.ccUser.get().getDouble(p.getUniqueId() + ".NetProfit");
            BustaMine.ccUser.get().set(p.getUniqueId() + ".NetProfit", old + amount);
        }
        else
        {
            old = BustaMine.ccUser.get().getInt(p.getUniqueId() + ".NetProfit_Exp");
            BustaMine.ccUser.get().set(p.getUniqueId() + ".NetProfit_Exp", (int)(old + amount));
        }
    }

    public static void ShowPlayerInfo(Player to,Player data)
    {
        to.sendMessage(BustaMine.ccLang.get().getString("Message.DivUpper"));
        to.sendMessage("   §6§l"+data.getName());
        to.sendMessage("   §3"+BustaMine.ccLang.get().getString("NetProfit"));
        to.sendMessage("     §3"+BustaMine.ccConfig.get().getString("CurrencySymbol") + "  " + df.format(BustaMine.ccUser.get().getDouble(data.getUniqueId() +".NetProfit")));
        to.sendMessage("     §3Xp "+BustaMine.ccUser.get().getInt(data.getUniqueId() +".NetProfit_Exp"));

        if(to==data)
        {
            to.sendMessage("   §e"+BustaMine.ccLang.get().getString("Bal"));
            to.sendMessage("     §e"+BustaMine.ccConfig.get().getString("CurrencySymbol") + "  " + df.format(BustaMine.getEconomy().getBalance(data)));
            to.sendMessage("     §eXp "+  CalcTotalExp(data));
        }

        to.sendMessage("   §f"+BustaMine.ccLang.get().getString("GamesPlayed")+": "+BustaMine.ccUser.get().getInt(data.getUniqueId() +".GamesPlayed"));
        to.sendMessage(BustaMine.ccLang.get().getString("Message.DivLower"));
    }

    public static void ShowStatistics(Player p)
    {
        double moneyIn = BustaMine.ccBank.get().getDouble("Statistics.Income.Money");
        double moneyOut = BustaMine.ccBank.get().getDouble("Statistics.Expense.Money");
        int expIn = BustaMine.ccBank.get().getInt("Statistics.Income.Exp");
        int expOut = BustaMine.ccBank.get().getInt("Statistics.Expense.Exp");

        p.sendMessage(BustaMine.ccLang.get().getString("Message.DivUpper"));
        p.sendMessage("   §3"+BustaMine.ccLang.get().getString("Income"));
        p.sendMessage("     §3"+ BustaMine.ccConfig.get().getString("CurrencySymbol") + "  " + df.format(moneyIn) + "  Xp "+ expIn);
        p.sendMessage("   §3"+BustaMine.ccLang.get().getString("Expense"));
        p.sendMessage("     §3"+ BustaMine.ccConfig.get().getString("CurrencySymbol") + "  " + df.format(moneyOut) + "  Xp " + expOut);
        p.sendMessage("   §e"+BustaMine.ccLang.get().getString("NetProfit"));
        p.sendMessage("     §e"+ BustaMine.ccConfig.get().getString("CurrencySymbol") + "  " + df.format(moneyIn+moneyOut));
        p.sendMessage("     §eXp "+ (expIn+expOut));
        p.sendMessage(BustaMine.ccLang.get().getString("Message.DivLower"));
    }

    // 소리 재생
    public static void PlayerSoundEffect(Player player, String key)
    {
        try
        {
            player.playSound(player.getLocation(), Sound.valueOf(BustaMine.ccSound.get().getString(key)),1,1);
        }
        catch (Exception e)
        {
            if(BustaMine.ccSound.get().contains(key))
            {
                if(BustaMine.ccSound.get().getString(key).length() > 1)
                {
                    BustaMine.console.sendMessage(BustaMine.consolePrefix + " Sound play failed: " + key + "/" + BustaMine.ccSound.get().getString(key));
                }
            }
            else
            {
                BustaMine.console.sendMessage(BustaMine.consolePrefix + " Sound play failed. Path is missing: " + key);
            }
        }
    }
    //---------------------------------------------------------------------------

    public static void RunCmb_RoundStart()
    {
        if(BustaMine.ccConfig.get().getString("Command.WhenRoundStart").length()==0)return;

        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),BustaMine.ccConfig.get().getString("Command.WhenRoundStart"));
    }

    public static void RunCmd_Bet(Player p, int amount)
    {
        if(BustaMine.ccConfig.get().getString("Command.WhenPlayerBet").length()==0)return;

        //BustaMine.console.sendMessage(BustaMine.consolePrefix+BustaMine.ccConfig.get().getString("Command.WhenPlayerBet").replace("{player}",p.getName()).replace("{amount}",amount+""));
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),BustaMine.ccConfig.get().getString("Command.WhenPlayerBet")
                .replace("{player}",p.getName()).replace("{amount}",amount+""));
    }

    public static void RunCmd_Cashout(Player p, double amount, int multiplier, double prize)
    {
        if(BustaMine.ccConfig.get().getString("Command.WhenPlayerCashOut").length()==0)return;

        double temp = multiplier/100.0f;
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),BustaMine.ccConfig.get().getString("Command.WhenPlayerCashOut")
                .replace("{player}",p.getName())
                .replace("{amount}",amount+"")
                .replace("{multiplier}",temp+"")
                .replace("{prize}",prize+"")
        );
    }

    public static void RunCmb_RoundEnd(int multiplier)
    {
        if(BustaMine.ccConfig.get().getString("Command.WhenRoundEnd").length()==0)return;

        double temp = multiplier/100.0f;
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),BustaMine.ccConfig.get().getString("Command.WhenRoundEnd")
                .replace("{multiplier}",temp+"")
        );
    }

    //---------------------------------------------------------------------------

    public static void GameUISetup()
    {
        CloseAllGameUI();
        CreateGameInven(bustaType.money);
        CreateGameInven(bustaType.exp);
    }

    private static void CreateGameInven(bustaType type)
    {
        // UI 요소 생성
        String title = BustaMine.ccLang.get().getString("UI.Title");
        if(type == bustaType.money)
        {
            title = title+" "+BustaMine.ccLang.get().getString("Money");
        }
        else
        {
            title = title+" "+BustaMine.ccLang.get().getString("Exp");
        }
        Inventory inven = Bukkit.createInventory(null,54,title);

        // Bankroll
        if(BustaMine.ccConfig.get().getBoolean("ShowBankroll"))
        {
            ItemStack bankrollBtn = CreateItemStack(Material.getMaterial(BustaMine.ccConfig.get().getString("BtnIcon.Bankroll")),null,
                    BustaMine.ccLang.get().getString("UI.Bankroll"), null,1);
            inven.setItem(45,bankrollBtn);
        }

        // 내 정보
        ArrayList<String> myStateLore = new ArrayList<>();
        myStateLore.add(BustaMine.ccLang.get().getString("UI.Click"));
        ItemStack myStateBtn = CreateItemStack(Material.getMaterial(BustaMine.ccConfig.get().getString("BtnIcon.MyState")),null,
                BustaMine.ccLang.get().getString("UI.MyState"), myStateLore,1);
        inven.setItem(47,myStateBtn);

        // 기록 버튼
        ItemStack historyBtn = CreateItemStack(Material.getMaterial(BustaMine.ccConfig.get().getString("BtnIcon.History")),null,
                BustaMine.ccLang.get().getString("UI.History"), null,1);
        inven.setItem(48,historyBtn);

        // 스톱 버튼
        ItemStack closeBtn = CreateItemStack(Material.getMaterial(BustaMine.ccConfig.get().getString("BtnIcon.CashOut")),null,
                BustaMine.ccLang.get().getString("UI.CashOut"), null,1);
        inven.setItem(49,closeBtn);

        // 설정 버튼
        ItemStack cosBtn = CreateItemStack(Material.getMaterial(BustaMine.ccConfig.get().getString("BtnIcon.CashOutSetting")),null,
                BustaMine.ccLang.get().getString("UI.CashOutSetting"), null,1);
        inven.setItem(50,cosBtn);

        if(type == bustaType.money)
        {
            gameInven = inven;
        }
        else
        {
            gameInven_exp = inven;
        }
    }

    public static void OpenGameInven(Player p, bustaType type)
    {
        if(type == bustaType.money)
        {
            p.openInventory(gameInven);
        }
        else
        {
            p.openInventory(gameInven_exp);
        }
    }

    public static void Top(Player p,String type)
    {
        if(sortedTime.get(type) + (1000L*60) < System.currentTimeMillis())
        {
            sortedMap.get(type).clear();
            for(String s:BustaMine.ccUser.get().getKeys(false))
            {
                sortedMap.get(type).put(s,BustaMine.ccUser.get().getDouble(s+"."+type));
            }

            sortedMap.put(type,sortByValue(sortedMap.get(type)));
            sortedTime.put(type,System.currentTimeMillis());
        }

        p.sendMessage("§6§l[§e " + BustaMine.ccLang.get().getString("Leaderboard") + "/" + type + " §6§l]");

        int i = 0;
        for(String s: sortedMap.get(type).keySet())
        {
            try
            {
                OfflinePlayer of = Bukkit.getServer().getOfflinePlayer(UUID.fromString(s));

                if(type.equals("NetProfit"))
                {
                    p.sendMessage("  " + (i+1) + ". " + of.getName()+"   "+ BustaMine.ccConfig.get().getString("CurrencySymbol") + df.format(sortedMap.get(type).get(s)));
                }
                else if(type.equals("NetProfit_Exp"))
                {
                    p.sendMessage("  " +  (i+1) + ". " + of.getName()+"   Xp"+ intf.format(sortedMap.get(type).get(s)));
                }
                else
                {
                    p.sendMessage("  " +  (i+1) + ". " + of.getName()+"   "+ intf.format(sortedMap.get(type).get(s)));
                }

                i++;
                if(i>=10)break;
            }catch (Exception e){p.sendMessage(BustaMine.prefix+"Failed to load player data. " + s);}
        }

        p.sendMessage("  §7"+BustaMine.ccLang.get().getString("Message.LastUpdate").
                replace("{sec}",(System.currentTimeMillis()- sortedTime.get(type))/1000 +""));
        p.sendMessage("");
    }

    public static void CloseAllGameUI()
    {
        for (Player p:Bukkit.getServer().getOnlinePlayers()) {
            if(p.getOpenInventory().getTitle().contains(BustaMine.ccLang.get().getString("UI.Title")))
            {
                p.sendMessage(BustaMine.prefix+"Game was terminated by server");
                p.closeInventory();
            }
        }
    }

    public static void RefreshIcons()
    {
        String[] tempStr = new String[]{"Bankroll","WinChance","MyState","History","CashOut"};
        //45 46 47 48 49
        for(int i = 45; i<=49; i++)
        {
            if(gameInven.getItem(i)==null)continue;

            ItemStack tempIs = gameInven.getItem(i);
            ItemMeta tempMeta = tempIs.getItemMeta();

            ItemStack newIs = new ItemStack(Material.getMaterial(BustaMine.ccConfig.get().getString("BtnIcon."+tempStr[i-45])));
            newIs.setItemMeta(tempMeta);

            gameInven.setItem(i,newIs);
            gameInven_exp.setItem(i,newIs);
        }
    }

    public static void ShowBetSettingUI(Player p)
    {
        String title = BustaMine.ccLang.get().getString("CO.Title");
        Inventory inven = Bukkit.createInventory(p,27, title);

        // 닫기 버튼
        ArrayList<String> closeLore = new ArrayList<>();
        closeLore.add(BustaMine.ccLang.get().getString("UI.Click"));
        ItemStack clostBtn = new ItemStack(coloredGlass.get(11));
        ItemMeta tempM1 = clostBtn.getItemMeta();
        tempM1.setDisplayName(BustaMine.ccLang.get().getString("CO.PlayMoneyGame"));
        tempM1.setLore(closeLore);
        clostBtn.setItemMeta(tempM1);
        inven.setItem(18,clostBtn);

        ItemStack clostBtn2 = new ItemStack(coloredGlass.get(11));
        ItemMeta tempM2 = clostBtn2.getItemMeta();
        tempM2.setDisplayName(BustaMine.ccLang.get().getString("CO.PlayExpGame"));
        tempM2.setLore(closeLore);
        clostBtn2.setItemMeta(tempM2);
        inven.setItem(26,clostBtn2);

        ArrayList<String> btnLore = new ArrayList<>();
        if(BustaMine.ccUser.get().contains(p.getUniqueId() +".CashOut"))
        {
            btnLore.add(BustaMine.ccLang.get().getString("CO.Enabled") +": " + BustaMine.ccLang.get().getString("CO.x")+(BustaMine.ccUser.get().getInt(p.getUniqueId() +".CashOut")/100.0));
            btnLore.add(BustaMine.ccLang.get().getString("UI.Click"));

            ItemStack state = new ItemStack(coloredGlass.get(13));
            ItemMeta tempM = state.getItemMeta();
            tempM.setDisplayName(BustaMine.ccLang.get().getString("CO.OnOff"));
            tempM.setLore(btnLore);
            state.setItemMeta(tempM);
            inven.setItem(13,state);
        }
        else
        {
            btnLore.add(BustaMine.ccLang.get().getString("CO.Disabled"));
            btnLore.add(BustaMine.ccLang.get().getString("UI.Click"));

            ItemStack state = new ItemStack(coloredGlass.get(14));
            ItemMeta tempM = state.getItemMeta();
            tempM.setDisplayName(BustaMine.ccLang.get().getString("CO.OnOff"));
            tempM.setLore(btnLore);
            state.setItemMeta(tempM);
            inven.setItem(13,state);
        }

        // -10
        ItemStack m10Btn = CreateItemStack(Material.getMaterial(BustaMine.ccConfig.get().getString("BtnIcon.BetBig")),null,
                BustaMine.ccLang.get().getString("CO.-10"), btnLore,1);
        inven.setItem(10,m10Btn);
        // -1
        ItemStack m1Btn = CreateItemStack(Material.getMaterial(BustaMine.ccConfig.get().getString("BtnIcon.BetMedium")),null,
                BustaMine.ccLang.get().getString("CO.-1"), btnLore,1);
        inven.setItem(11,m1Btn);
        // -0.1
        ItemStack m01Btn = CreateItemStack(Material.getMaterial(BustaMine.ccConfig.get().getString("BtnIcon.BetSmall")),null,
                BustaMine.ccLang.get().getString("CO.-01"), btnLore,1);
        inven.setItem(12,m01Btn);
        // +0.1
        ItemStack p01Btn = CreateItemStack(Material.getMaterial(BustaMine.ccConfig.get().getString("BtnIcon.BetSmall")),null,
                BustaMine.ccLang.get().getString("CO.+01"), btnLore,1);
        inven.setItem(14,p01Btn);
        // +1
        ItemStack p1Btn = CreateItemStack(Material.getMaterial(BustaMine.ccConfig.get().getString("BtnIcon.BetMedium")),null,
                BustaMine.ccLang.get().getString("CO.+1"), btnLore,1);
        inven.setItem(15,p1Btn);
        // +10
        ItemStack p10Btn = CreateItemStack(Material.getMaterial(BustaMine.ccConfig.get().getString("BtnIcon.BetBig")),null,
                BustaMine.ccLang.get().getString("CO.+10"), btnLore,1);
        inven.setItem(16,p10Btn);

        p.openInventory(inven);
    }

    //---------------------------------------------------------------------------

    // 지정된 이름,lore,수량의 아이탬 스택 생성및 반환
    public static ItemStack CreateItemStack(Material material, ItemMeta _meta, String name, ArrayList<String> lore, int amount)
    {
        ItemStack istack = new ItemStack(material,amount);

        ItemMeta meta = _meta;
        if(_meta == null) meta = istack.getItemMeta();
        if(!name.equals("")) meta.setDisplayName(name);
        meta.setLore(lore);
        istack.setItemMeta(meta);
        return istack;
    }

    private static int CalcTotalExp(Player p)
    {
        int lv = p.getLevel();
        int sub = (int)(p.getExp() * p.getExpToLevel());
        double temp;

        if(lv <= 16)
        {
            temp = Math.pow(lv,2) + (6*lv);
            return (int)(temp + sub);
        }
        else if(lv <= 31)
        {
            temp = (2.5*Math.pow(lv,2)) - (40.5*lv) + 360;
            return (int)(temp + sub);
        }
        else
        {
            temp = (4.5*Math.pow(lv,2)) - (162.5*lv) + 2220;
            return (int)(temp + sub);
        }
    }

    static final HashMap<Integer,ItemStack> old = new HashMap<>();
    private static void DrawNumber(int num)
    {
        // 이전에 그린 숫자 지우기
        for (Integer key:old.keySet()) {
            gameInven.setItem(key,old.get(key));
            gameInven_exp.setItem(key,old.get(key));
        }
        old.clear();

        int[] intarr = null;
        if(num == 5)
        {
            intarr = new int[]{5,4,3,12,21,22,23,32,39,40,41};
        }
        else if(num == 4)
        {
            intarr = new int[]{3,12,21,22,23,14,5,32,41};
        }
        else if(num == 3)
        {
            intarr = new int[]{3,4,5,14,21,22,23,32,39,40,41};
        }
        else if(num == 2)
        {
            intarr = new int[]{3,4,5,14,23,22,21,30,39,40,41};
        }
        else if(num == 1)
        {
            intarr = new int[]{5,14,23,32,41};
        }
        else
        {
            intarr = new int[]{3,4,5,14,23,32,41,40,39,30,21,12};
        }

        for (int j : intarr)
        {
            old.put(j, gameInven.getItem(j));
            ItemStack temp = new ItemStack(coloredGlass.get(0));
            ItemMeta tempM = temp.getItemMeta();
            tempM.setDisplayName(" ");
            temp.setItemMeta(tempM);
            gameInven.setItem(j, temp);
            gameInven_exp.setItem(j, temp);
        }
    }

    public static <K, V extends Comparable<? super V>> HashMap<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());
        Collections.reverse(list);

        HashMap<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }
}
