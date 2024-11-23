package plugin.treasurehunt.task;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.*;
import plugin.treasurehunt.TreasureHunt;
import plugin.treasurehunt.command.DropItem;
import plugin.treasurehunt.constants.FindGoldenAppleConstants;
import plugin.treasurehunt.data.PlayerData;
import plugin.treasurehunt.mapper.data.PlayerScore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;

import static plugin.treasurehunt.constants.FindGoldenAppleConstants.*;
import static plugin.treasurehunt.constants.FindGoldenAppleConstants.GAME_TIME;

public class CountdownTask implements Runnable {
    private final TreasureHunt treasurehunt;
    private final Player player;
    private final PlayerData nowPlayerData;
    private int countdownTime = COUNTDOWN_TIME;
    private boolean isRunning;
    private final Map<Block, DropItem> potIDMap = new HashMap<>();

    public CountdownTask(TreasureHunt treasurehunt, Player player, PlayerData nowPlayerData) {
        this.treasurehunt = treasurehunt;
        this.player = player;
        this.nowPlayerData = nowPlayerData;
    }

    public void start(boolean isCountdownActive) {
        isRunning = true;
        Bukkit.getScheduler().runTaskTimer(treasurehunt, Runnable -> {
            if (COUNTDOWN_TIME > 0) {
                player.sendTitle(
                        "ゲーム開始まで" + COUNTDOWN_TIME-- + " 秒",
                        "ルール: 飾り壺を割って りんごを見つけよう！",
                        0, 20, 0);

            } else {
                Runnable.cancel();

                isCountdownActive = false;
                COUNTDOWN_TIME += 5;

                potIDMap.clear();
                nowPlayerData.setScore(0);

                player.sendTitle("START", "", 0, 30, 10);

                setUpPots(player);
                timeLeftOnBossBar(player);

                runGameTimer(player, nowPlayerData);
            }
        }, 0, 20);
    }

    @Override
    public void run() {
        if (!isRunning) return;

        if (countdownTime > 0) {
            player.sendTitle(
                    "ゲーム開始まで" + countdownTime-- + " 秒",
                    "ルール: 飾り壺を割って りんごを見つけよう！",
                    0, 20, 0
            );
        } else {
            stop();
            new GameTask(treasurehunt, player, nowPlayerData).start();
        }
    }

    public void stop() {
        isRunning = false;
        countdownTime = COUNTDOWN_TIME;
    }

    /**
     * 飾り壺を出現させ、出現した飾り壺をドロップアイテムの種類と併せてMap登録する。
     *
     * @param player コマンドを実行したプレイヤー
     */
    private void setUpPots(Player player) {
        for (int i = 1; i <= POT_AMOUNT; i++) {
            Block block = findEmptyLocation(player);

            block.setType(Material.DECORATED_POT);

            DropItem itemDrop = getDropItemById(i);
            potIDMap.put(block, itemDrop);
        }
    }

    /**
     * 空いている位置を見つけるための再帰メソッド
     *
     * @param player コマンドを実行したプレイヤー
     * @return 空いているブロック位置
     */
    private Block findEmptyLocation(Player player) {
        Block block = getDecoratedPotLocation(player).getBlock();

        if (block.getType() != Material.AIR) {
            return findEmptyLocation(player);
        }
        return block;
    }

    /**
     * 飾り壺の出現場所を取得します。 出現エリアのX軸とZ軸は自分の位置からプラスランダムで-5〜4の値が設定されます。 Y軸はプレイヤーと同じ位置になります。
     *
     * @param player コマンドを実行したプレイヤー
     * @return　　 　 飾り壺の出現場所
     */
    private Location getDecoratedPotLocation(Player player) {
        Location playerlocation = player.getLocation();
        int randomX = new SplittableRandom().nextInt(30) - 15;
        int randomZ = new SplittableRandom().nextInt(30) - 15;

        double x = playerlocation.getX() + randomX;
        double y = playerlocation.getY();
        double z = playerlocation.getZ() + randomZ;

        return new Location(player.getWorld(), x, y, z);
    }


    /**
     * IDに基づいて、金のりんご、りんご、ドロップなしを決定する。金のりんごは1個。りんごは2個。IDが振り分けられる。
     *
     * @param id 出現した飾り壺のID
     * @return ドロップアイテムの種類
     */
    private DropItem getDropItemById(int id) {
        if (id == 1) {
            return DropItem.GOLDEN_APPLE_DROP;
        } else if (id >= 2 && id <= 2 + APPLE_AMOUNT - 1) {
            return DropItem.APPLE_DROP;
        } else {
            return DropItem.NONE_DROP;
        }
    }


    /**
     * ボスバーでゲームの残り時間を表示する。
     *
     * @param player 　コマンドを実行したプレイヤー
     */
    private void timeLeftOnBossBar(Player player) {
        bossBar = Bukkit.createBossBar("残り時間: " + GAME_TIME + "秒", BarColor.BLUE, BarStyle.SOLID);
        bossBar.setProgress(1.0); // ボスバーの進行度を100%に設定
        bossBar.addPlayer(player);
    }


    /**
     * ゲームタイマーの処理
     *
     * @param player        　コマンドを実行したプレイヤー
     * @param nowPlayerData 　 現在実行しているプレイヤー情報
     */
    private void runGameTimer(Player player, PlayerData nowPlayerData) {
        Bukkit.getScheduler().runTaskTimer(treasureHunt, gameTask -> {
            if (nowPlayerData.getGameTime() <= 0) {
                gameTask.cancel();

                player.sendTitle("FINISH", "TOTAL SCORE：" + nowPlayerData.getScore(), 0, 60, 10);

                // 空のスコアボードを設定して、ゲーム中のスコアボードを非表示にする
                player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());

                bossBar.removeAll();
                potIDMap.keySet().forEach(block -> block.setType(Material.AIR));

                playerScoreData.insert(
                        new PlayerScore(nowPlayerData.getPlayerName()
                                , nowPlayerData.getScore()));

                return;
            }

            bossBar.setTitle("残り時間: " + nowPlayerData.getGameTime() + "秒");
            bossBar.setProgress((double) nowPlayerData.getGameTime() / GAME_TIME);

            displayTotalScoreOnBoard(player, nowPlayerData);

            nowPlayerData.setGameTime(nowPlayerData.getGameTime() - 1);
        }, 0, 20);
    }


    /**
     * ゲーム中に現在のトータルスコアをスコアボードに表示する
     *
     * @param player    　コマンドを実行したプレイヤー
     * @param nowPlayer 　現在実行しているプレイヤー情報
     */
    private void displayTotalScoreOnBoard(Player player, PlayerData nowPlayer) {
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = scoreboardManager.getNewScoreboard();

        Objective objective = scoreboard.registerNewObjective(
                "GameStats",
                Criteria.DUMMY,
                "SCORE NOW"
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(scoreboard);

        Score score = objective.getScore("");
        score.setScore(nowPlayer.getScore());
    }


    /**
     * 飾り壺が壊されたときに、指定されたアイテムをドロップ。またはドロップを無効化します。
     *
     * @param breakEvent 飾り壺を壊したときのイベント
     * @param dropItem   飾り壺を壊した後のドロップアイテム
     * @param block      ゲーム開始時に出現した飾り壺
     */
    private static void dropItemOnPotBreak(BlockBreakEvent breakEvent, DropItem dropItem, Block block) {
        switch (dropItem) {
            case GOLDEN_APPLE_DROP -> block.getWorld()
                    .dropItemNaturally(block.getLocation(), new ItemStack(Material.GOLDEN_APPLE));
            case APPLE_DROP -> block.getWorld()
                    .dropItemNaturally(block.getLocation(), new ItemStack(Material.APPLE));
            case NONE_DROP -> breakEvent.setDropItems(false);
        }
    }


    /**
     * 指定されたアイテムのドロップと残り時間に基づいて、プレイヤーに追加されるスコアを計算する。
     *
     * @param playerData 　プレイヤー情報
     * @param dropItem   　ドロップアイテムの種類
     * @param player     　コマンドを実行したプレイヤー
     * @return　　　　　　　　追加されるスコア
     */
    private Integer getAddScore(PlayerData playerData, DropItem dropItem, Player player) {
        int addScore = 0;

        if (dropItem == DropItem.NONE_DROP) {
            messageOnFound(dropItem, player, addScore);
            return null;
        }

        int nowTime = playerData.getGameTime();
        addScore = (nowTime >= 40) ? 100
                : (nowTime >= 20) ? 50
                : 10;

        if (dropItem == DropItem.GOLDEN_APPLE_DROP) {
            addScore += BONUS_SCORE;
        }

        playerData.setScore(playerData.getScore() + addScore);
        return addScore;
    }


    /**
     * 飾り壺を壊してドロップアイテムが判明後、ドロップアイテムの結果とプレイヤーに追加されたスコアを送る
     *
     * @param dropItem 飾り壺を壊した後のドロップアイテム
     * @param player   コマンドを実行したプレイヤー
     * @param addScore 　追加されたスコア
     */
    private void messageOnFound(DropItem dropItem, Player player, int addScore) {
        switch (dropItem) {
            case GOLDEN_APPLE_DROP -> player.sendMessage(
                    "金のりんごを見つけた！（＋" + addScore + "点）　りんごは残り" + getAppleCount() + "個！");
            case APPLE_DROP -> player.sendMessage(
                    "りんごを見つけた！（＋" + addScore + "点）　　　りんごは残り" + getAppleCount() + "個！");
            case NONE_DROP -> player.sendMessage(
                    "ざんねん！はずれ！");
        }
    }


    /**
     * 獲得できるりんごが残り何個あるかをカウントする
     *
     * @return　残りのりんごの数
     */
    private long getAppleCount() {
        return potIDMap.entrySet().stream()
                .filter(
                        entry -> entry.getValue().equals(DropItem.GOLDEN_APPLE_DROP)
                                || entry.getValue().equals(DropItem.APPLE_DROP))
                .count();
    }


    /**
     * りんごが0個になった場合、ゲームタイムを0にしてゲームを終了する
     *
     * @param player コマンドを実行したプレイヤー
     */
    private void finishGameIfApplesGone(Player player) {
        long count = getAppleCount();
        if (count == 0) {
            getPlayerData(player).setGameTime(0);
        }
    }
}
