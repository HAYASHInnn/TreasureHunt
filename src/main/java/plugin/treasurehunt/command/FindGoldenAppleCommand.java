package plugin.treasurehunt.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SplittableRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import plugin.treasurehunt.TreasureHunt;
import plugin.treasurehunt.data.PlayerData;

/**
 * 制限時間内にランダムに出現した飾り壺を割り、金のりんご、りんごを見つけてスコアを獲得するゲームを起動するコマンドです。
 * スコアはりんごを見つけた時点のゲームの残り時間によって変動します。また金のりんごを見つけるとボーナススコアが加点されます。 *
 */

public class FindGoldenAppleCommand extends BaseCommand implements Listener {

  public static final int POT_AMOUNT = 5;
  public static final int APPLE_AMOUNT = 2;

  // TIMEの単位は秒
  public static final int GAME_TIME = 40;
  public static int COUNTDOWN_TIME = 5;

  public static final String GOLDEN_APPLE_ITEM_DROP = "golden_apple";
  public static final String APPLE_ITEM_DROP = "apple";
  public static final String NONE_ITEM_DROP = "none";

  // 金のりんごを発見したときのボーナススコア
  public static final int BONUS_SCORE = 50;

  private BossBar bossBar;
  private ScoreboardManager scoreboardManager;
  private Scoreboard scoreboard;
  private Objective objective;

  // カウントダウン中のフラグ
  private boolean isCountdownActive = false;

  private final TreasureHunt treasurehunt;
  private final List<PlayerData> playerDataList = new ArrayList<>();
  private final Map<Block, String> potIDMap = new HashMap<>();


  public FindGoldenAppleCommand(TreasureHunt treasurehunt) {
    this.treasurehunt = treasurehunt;
  }

  @Override
  public boolean onExecutePlayerCommand(Player player, Command command, String label,
      String[] args) {

    PlayerData nowPlayer = getPlayerData(player);
    sendHintToPlayer(player);
    isCountdownActive = true;
    startCountdown(player, nowPlayer);

    return true;
  }


  @Override
  public boolean onExecuteNPCCommand(CommandSender sender, Command command, String label,
      String[] args) {
    return false;
  }


  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    if (isCountdownActive) {
      Player player = event.getPlayer();
      Location from = event.getFrom();
      Location to = event.getTo();

      // 実際に移動が発生しようとした場合、位置を元に戻す
      if (to != null && (from.getX() != to.getX() || from.getZ() != to.getZ())) {
        player.teleport(from);
      }
    }
  }


  /**
   * プレイヤーにゲームのヒントを送る
   *
   * @param player 　コマンドを実行したプレイヤー
   */
  private static void sendHintToPlayer(Player player) {
    player.sendMessage(
        "ヒント: 金のりんごは +" + BONUS_SCORE + "点！");
    player.sendMessage(
        "ヒント: 見つける時間が早いほどスコアは高くなります！");
  }


  /**
   * ゲーム開始前にルール説明をする。ルール説明時間は5秒間でカウントダウンする。
   *
   * @param player    　コマンドを実行したプレイヤー
   * @param nowPlayer 　 現在実行しているプレイヤー情報
   */
  private void startCountdown(Player player, PlayerData nowPlayer) {
    Bukkit.getScheduler().runTaskTimer(treasurehunt, Runnable -> {
      if (COUNTDOWN_TIME > 0) {
        player.sendTitle(
            "ゲーム開始まで" + COUNTDOWN_TIME + " 秒",
            "ルール: 飾り壺を割って りんごを見つけよう！",
            0, 20, 0);

        COUNTDOWN_TIME--;
      } else {
        Runnable.cancel();
        isCountdownActive = false;
        COUNTDOWN_TIME += 5;
        intiGameSetup(player, nowPlayer);
      }
    }, 0, 1 * 20);
  }


  /**
   * ゲーム開始時の処理設定
   *
   * @param player    　コマンドを実行したプレイヤー
   * @param nowPlayer 　 現在実行しているプレイヤー情報
   */
  private void intiGameSetup(Player player, PlayerData nowPlayer) {
    potIDMap.clear();
    player.sendTitle("START", "", 0, 30, 10);
    spawnedPotRegistry(player);

    nowPlayer.setGameTime(GAME_TIME);
    nowPlayer.setScore(0);
    timeLeftOnBossBar(player);

    runGameTimer(player, nowPlayer);
  }


  /**
   * 飾り壺を出現させ、出現した飾り壺をドロップアイテムの種類と併せてMap登録する。
   *
   * @param player コマンドを実行したプレイヤー
   */
  private void spawnedPotRegistry(Player player) {
    for (int i = 1; i <= POT_AMOUNT; i++) {
      Block block = findEmptyLocation(player);

      block.setType(Material.DECORATED_POT);

      String itemDrop = idItemDrop(i);
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
    Location location = getDecoratedPotLocation(player);
    Block block = location.getBlock();

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
    int randomX = new SplittableRandom().nextInt(10) - 5;
    int randomZ = new SplittableRandom().nextInt(10) - 5;

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
  private String idItemDrop(int id) {
    if (id == 1) {
      return GOLDEN_APPLE_ITEM_DROP;
    } else if (id >= 2 && id <= 2 + APPLE_AMOUNT - 1) {
      return APPLE_ITEM_DROP;
    } else {
      return NONE_ITEM_DROP;
    }
  }


  /**
   * ゲームタイマーの処理
   *
   * @param player    　コマンドを実行したプレイヤー
   * @param nowPlayer 　 現在実行しているプレイヤー情報
   */
  private void runGameTimer(Player player, PlayerData nowPlayer) {
    Bukkit.getScheduler().runTaskTimer(treasurehunt, gameTask -> {
      if (nowPlayer.getGameTime() <= 0) {
        gameTask.cancel();

        for (PlayerData playerData : playerDataList) {
          if (playerData.getPlayerName().equals(player.getName())) {
            bossBar.removeAll();
            finishGame(playerData, player);
          }
        }
        return;
      }
      updateBossBar(nowPlayer);
      displayTotalScoreOnBoard(player, nowPlayer);
      nowPlayer.setGameTime(nowPlayer.getGameTime() - 1);
    }, 0, 1 * 20);
  }


  /**
   * ゲーム終了処理。FINISHメッセージを表示し、スコアを表示。
   *
   * @param playerData 　 プレイヤー情報
   * @param player     　コマンドを実行したプレイヤー
   */
  private void finishGame(PlayerData playerData, Player player) {
    player.sendTitle("FINISH", "TOTAL SCORE：" + playerData.getScore(), 0, 60, 10);

    // 空のスコアボードを設定して、ゲーム中のスコアボードを非表示にする
    player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());

    // 壊れていない飾り壺を消す
    potIDMap.entrySet().stream().filter(
            entry -> entry.getValue().equals(NONE_ITEM_DROP)
                || entry.getValue().equals(GOLDEN_APPLE_ITEM_DROP)
                || entry.getValue().equals(APPLE_ITEM_DROP)
        ).map(Entry::getKey)
        .forEach(key -> key.setType(Material.AIR));
  }


  /**
   * ボスバーを更新する
   *
   * @param nowPlayer 　現在実行しているプレイヤー情報
   */
  private void updateBossBar(PlayerData nowPlayer) {
    bossBar.setTitle("残り時間: " + nowPlayer.getGameTime() + "秒");
    bossBar.setProgress((double) nowPlayer.getGameTime() / GAME_TIME);
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
   * ゲーム中に現在のトータルスコアをスコアボードに表示する
   *
   * @param player    　コマンドを実行したプレイヤー
   * @param nowPlayer 　現在実行しているプレイヤー情報
   */
  private void displayTotalScoreOnBoard(Player player, PlayerData nowPlayer) {
    scoreboardManager = Bukkit.getScoreboardManager();
    scoreboard = scoreboardManager.getNewScoreboard();

    Objective objective = scoreboard.registerNewObjective(
        "GameStats",
        Criteria.DUMMY,
        "TOTAL SCORE"
    );
    objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    player.setScoreboard(scoreboard);

    Score score = objective.getScore("");
    score.setScore(nowPlayer.getScore());
  }


  @EventHandler
  public void onPotBreak(BlockBreakEvent breakEvent) {

    Block block = breakEvent.getBlock();
    Player player = breakEvent.getPlayer();

    if (Objects.isNull(player) || playerDataList.isEmpty()) {
      return;
    }

    if (block.getType() == Material.DECORATED_POT) {
      String dropItem = potIDMap.get(block);

      handleBlockDrop(breakEvent, dropItem, block);

      // デフォルトのドロップアイテムを無効にする
      breakEvent.setDropItems(false);

      playerDataList.forEach(playerData -> {
        if (playerData.getPlayerName().equals(player.getName())) {
          Integer addScore = getAddScore(playerData, dropItem, player);
          if (addScore == null) {
            return;
          }

          // ポットを削除し、残りのりんごの数を更新
          potIDMap.remove(block);
          finishGameIfApplesGone(player);

          messageOnFound(dropItem, player, addScore);
        }
      });
    }
  }


  /**
   * 飾り壺が壊されたときに、指定されたアイテムをドロップ。またはドロップを無効化します。
   *
   * @param breakEvent 飾り壺を壊したときのイベント
   * @param dropItem   飾り壺を壊した後のドロップアイテム
   * @param block      ゲーム開始時に出現した飾り壺
   */
  private static void handleBlockDrop(BlockBreakEvent breakEvent, String dropItem, Block block) {
    switch (dropItem) {
      case GOLDEN_APPLE_ITEM_DROP -> block.getWorld()
          .dropItemNaturally(block.getLocation(), new ItemStack(Material.GOLDEN_APPLE));
      case APPLE_ITEM_DROP -> block.getWorld()
          .dropItemNaturally(block.getLocation(), new ItemStack(Material.APPLE));
      case NONE_ITEM_DROP -> breakEvent.setDropItems(false);
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
  private Integer getAddScore(PlayerData playerData, String dropItem, Player player) {
    int addScore = 0;

    if (dropItem.equals(NONE_ITEM_DROP)) {
      messageOnFound(dropItem, player, addScore);
      return null;
    }
    if (dropItem.equals(GOLDEN_APPLE_ITEM_DROP)) {
      addScore += BONUS_SCORE;
    }
    int remainingTime = playerData.getGameTime();

    if (remainingTime > 30) {
      addScore += 100;
    } else if (remainingTime > 20) {
      addScore += 50;
    } else if (remainingTime > 0) {
      addScore += 10;
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
  private void messageOnFound(String dropItem, Player player, int addScore) {
    switch (dropItem) {
      case GOLDEN_APPLE_ITEM_DROP -> player.sendMessage(
          "金のりんごを見つけた！（＋" + addScore + "点）　りんごは残り" + getAppleCount() + "個！");
      case APPLE_ITEM_DROP -> player.sendMessage(
          "りんごを見つけた！（＋" + addScore + "点）　　　りんごは残り" + getAppleCount() + "個！");
      default -> player.sendMessage(
          "ざんねん！はずれ！");
    }
  }


  /**
   * 獲得できるりんごが残り何個あるかをカウントする
   *
   * @return　残りのりんごの数
   */
  private int getAppleCount() {
    int count = (int) potIDMap.entrySet().stream()
        .filter(
            entry -> entry.getValue().equals(GOLDEN_APPLE_ITEM_DROP)
                || entry.getValue().equals(APPLE_ITEM_DROP))
        .count();
    return count;
  }


  /**
   * りんごが0個になった場合、ゲームを終了する
   *
   * @param player コマンドを実行したプレイヤー
   */
  private void finishGameIfApplesGone(Player player) {
    PlayerData nowPlayer = getPlayerData(player);
    int count = getAppleCount();

    if (count == 0) {
      nowPlayer.setGameTime(0);
    }
  }


  /**
   * 現在実行しているプレイヤーの情報を取得する
   *
   * @param player 　コマンドを実行したプレイヤー
   * @return　現在実行しているプレイヤーのスコア情報
   */
  private PlayerData getPlayerData(Player player) {
    if (playerDataList.isEmpty()) {
      return addNewPlayer(player);
    } else {
      for (PlayerData playerData : playerDataList) {
        if (!playerData.getPlayerName().equals(player.getName())) {
          return addNewPlayer(player);
        } else {
          return playerData;
        }
      }
    }
    return null;
  }


  /**
   * 新規のプレイヤー情報をリストに追加する
   *
   * @param player コマンドを実行したプレイヤー
   * @return　新規プレイヤー
   */
  private PlayerData addNewPlayer(Player player) {
    PlayerData newPlayer = new PlayerData();
    newPlayer.setPlayerName(player.getName());
    playerDataList.add(newPlayer);
    return newPlayer;
  }
}


