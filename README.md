# BustaMine

Unofficial continued plugin of BustaMine from [SpigotMC](https://www.spigotmc.org/resources/66139).

> [!WARNING]
>
> 请勿将游戏内的金币、经验等数值与现实世界中的货币进行关联，否则，您将可能违反[《中华人民共和国刑法》](https://flk.npc.gov.cn/detail2.html?ZmY4MDgxODE3OTZhNjM2YTAxNzk4MjJhMTk2NDBjOTI)(2021年3月1日施行) 第三百零三条:
> > 以营利为目的，聚众赌博或者以赌博为业的，处三年以下有期徒刑、拘役或者管制，并处罚金‌。
> 
> 本插件仅供模拟娱乐，使用本插件即代表你已收到上述警告和声明。  
> 请遵守你所在地区的法律法规，本插件作者不为使用本插件的任何部分（包括但不限于文档、源代码、二进制文件等）所产生的后果负责。

## 简介

消耗玩家金币的简单方式。

## 如何游玩

简单概括：在回合开始之前买入，在归零之前抛售。  
如果在归零之前没有抛售，买入所花费的金币/经验将会消失!  
并且，有一定的概率会在回合开始时立即归零。

在回合开始之前，玩家可以买入自定义数量的金币或经验。  
在回合开始之后，停止继续买入，倍率会从 `x0.00` 随时间慢慢提升，时间越长，提升速度越快。  
插件会生成一个归零阈值，当倍率提升到这个阈值的时候，这一回合将归零。  
归零阈值不会告知玩家，玩家要做的，就是猜什么时候会归零，尽可能在归零前抛售。  
抛售后，将获得玩家 `买入数量 × 抛售时倍率` 的金币/经验。

## 权限

- `bm.admin` 管理员权限
- `bm.user.money`
- `bm.user.exp`
- `bm.user.stats`
- `bm.user.top`

## PAPI 变量

```
%bustamine_stat_income_money%
%bustamine_stat_expense_money%
%bustamine_stat_income_expy%
%bustamine_stat_expense_money%

%bustamine_top_net_profit:<top>:<type>%
%bustamine_top_net_profit_exp:<top>:<type>%
%bustamine_top_games_played:<top>:<type>%
- top:  1-10
- type: ['name', 'value']
示例: %bustamine_top_net_profit:1:name%
```
