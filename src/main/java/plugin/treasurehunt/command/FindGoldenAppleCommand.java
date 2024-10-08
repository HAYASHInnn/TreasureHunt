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
import org.bukkit.inventory.ItemStack;
import plugin.treasurehunt.TreasureHunt;
import plugin.treasurehunt.data.PlayerScore;

public class FindGoldenAppleCommand extends BaseCommand implements Listener {


  public static final int POT_AMOUNT = 5;
  public static final int APPLE_AMOUNT = 2;

  // GAME_TIMEの単位は秒
  public static final int GAME_TIME = 40;

  public static int COUNTDOWN = 6;

  public static final String GOLDEN_APPLE_ITEM_DROP = "golden_apple";
  public static final String APPLE_ITEM_DROP = "apple";
  public static final String NONE_ITEM_DROP = "none";

  // 金のりんごを発見したときのボーナススコア
  public static final int BONUS_SCORE = 50;

  private BossBar bossBar;


  private final TreasureHunt treasurehunt;

  private final List<PlayerScore> playerScoreList = new ArrayList<>();
  private final Map<Block, String> potIDMap = new HashMap<>();


  public FindGoldenAppleCommand(TreasureHunt treasurehunt) {
    this.treasurehunt = treasurehunt;
  }

  @Override
  public boolean onExecutePlayerCommand(Player player, Command command, String label,
      String[] args) {

    PlayerScore nowPlayer = getPlayerScore(player);
    nowPlayer.setGameTime(GAME_TIME);
    nowPlayer.setScore(0);

    // ボスバーを作成し、残り時間を表示
    bossBar = Bukkit.createBossBar("残り時間: " + GAME_TIME + "秒", BarColor.BLUE, BarStyle.SOLID);
    bossBar.setProgress(1.0); // ボスバーの進行度を100%に設定
    bossBar.addPlayer(player);

    potIDMap.clear();
    player.sendTitle("START", "飾り壺を割って金のりんごを探せ！", 0, 30, 10);
    spawnedPotRegistry(player);

    Bukkit.getScheduler().runTaskTimer(treasurehunt, Runnable -> {
      if (nowPlayer.getGameTime() <= 0) {
        Runnable.cancel();

        for (PlayerScore playerScore : playerScoreList) {
          if (playerScore.getPlayerName().equals(player.getName())) {
            bossBar.removeAll();
            finishGame(playerScore, player);
          }
        }
        return;
      }
      bossBar.setTitle("残り時間: " + nowPlayer.getGameTime() + "秒");
      bossBar.setProgress((double) nowPlayer.getGameTime() / GAME_TIME);

      nowPlayer.setGameTime(nowPlayer.getGameTime() - 1);
    }, 0, 1 * 20);

    return true;
  }


  @Override
  public boolean onExecuteNPCCommand(CommandSender sender, Command command, String label,
      String[] args) {
    return false;
  }


  /**
   * ゲーム終了処理。FINISHメッセージを表示し、スコアを表示。
   *
   * @param playerScore
   * @param player
   */
  private void finishGame(PlayerScore playerScore, Player player) {

    // プレイヤーにメッセージを送る
    player.sendTitle("FINISH", "TOTAL SCORE：" + playerScore.getScore(), 0, 60, 10);

    // 壊れていない飾り壺を消す
    potIDMap.entrySet().stream().filter(
            entry -> entry.getValue().equals(NONE_ITEM_DROP)
                || entry.getValue().equals(GOLDEN_APPLE_ITEM_DROP)
                || entry.getValue().equals(APPLE_ITEM_DROP)
        ).map(Entry::getKey)
        .forEach(key -> key.setType(Material.AIR));
  }


  /**
   * 飾り壺を出現させ、出現した飾り壺をドロップアイテムの種類と併せてMap登録する。
   *
   * @param player コマンドを実行したプレイヤー
   */
  private void spawnedPotRegistry(Player player) {
    for (int i = 1; i <= POT_AMOUNT; i++) {
      Block block = getDecoratedPotLocation(player).getBlock();
      block.setType(Material.DECORATED_POT);

      String itemDrop = idItemDrop(i);
      potIDMap.put(block, itemDrop);
    }
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
   * 飾り壺を壊すとMapに登録されている情報をに基づいて、アイテムがドロップする。 デフォルトのドロップアイテムはドロップしないようにする。ドロップアイテムに応じてスコアを加算する
   *
   * @param breakEvent 飾り壺を壊したときのイベント
   */
  @EventHandler
  public void onPotBreak(BlockBreakEvent breakEvent) {

    Block block = breakEvent.getBlock();
    Player player = breakEvent.getPlayer();

    if (Objects.isNull(player) || playerScoreList.isEmpty()) {
      return;
    }

    if (block.getType() == Material.DECORATED_POT) {
      String dropItem = potIDMap.get(block);

      handleBlockDrop(breakEvent, dropItem, block);

      // デフォルトのドロップアイテムを無効にする
      breakEvent.setDropItems(false);

      playerScoreList.forEach(playerScore -> {
        if (playerScore.getPlayerName().equals(player.getName())) {
          Integer addScore = getAddScore(playerScore, dropItem, player);
          if (addScore == null) {
            return;  // スコア加算しない
          }

          // ポットを削除し、残りのりんごの数を更新
          potIDMap.remove(block);
          appleCountLeft(player);

          messageOnFound(dropItem, player, addScore);
        }
      });
    }
  }


  /**
   * 指定されたアイテムのドロップと残り時間に基づいて、プレイヤーに追加されるスコアを計算する。
   *
   * @param playerScore 　プレイヤー情報
   * @param dropItem    　ドロップアイテムの種類
   * @param player      　コマンドを実行したプレイヤー
   * @return　　　　　　　　追加されるスコア
   */
  private Integer getAddScore(PlayerScore playerScore, String dropItem, Player player) {
    int addScore = 0;

    if (dropItem.equals(NONE_ITEM_DROP)) {
      messageOnFound(dropItem, player, addScore);
      return null;
    }
    if (dropItem.equals(GOLDEN_APPLE_ITEM_DROP)) {
      addScore += BONUS_SCORE;
    }
    int remainingTime = playerScore.getGameTime();

    if (remainingTime > 30) {
      addScore += 100;
    } else if (remainingTime > 20) {
      addScore += 50;
    } else if (remainingTime > 0) {
      addScore += 10;
    }
    playerScore.setScore(playerScore.getScore() + addScore);
    return addScore;
  }


  /**
   * 獲得できるりんごが残り何個あるかをカウントする。りんごが0個になった場合、ゲームを終了する
   *
   * @param player コマンドを実行したプレイヤー
   */
  private void appleCountLeft(Player player) {
    PlayerScore nowPlayer = getPlayerScore(player);

    int count = (int) potIDMap.entrySet().stream()
        .filter(
            entry -> entry.getValue().equals(GOLDEN_APPLE_ITEM_DROP)
                || entry.getValue().equals(APPLE_ITEM_DROP))
        .count();

    if (count == 0) {
      nowPlayer.setGameTime(0);
    } else {

      player.sendTitle("", "りんごは残り" + count + "個", 0, 30, 10);
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
   * 飾り壺を壊してドロップアイテムが判明後、ドロップアイテムの結果とプレイヤーに追加されたスコアを送る
   *
   * @param dropItem 飾り壺を壊した後のドロップアイテム
   * @param player   コマンドを実行したプレイヤー
   * @param addScore 　追加されたスコア
   */
  private void messageOnFound(String dropItem, Player player, int addScore) {
    switch (dropItem) {
      case GOLDEN_APPLE_ITEM_DROP -> player.sendMessage(
          "金のりんごを見つけた！（＋" + addScore + "点）");
      case APPLE_ITEM_DROP -> player.sendMessage(
          "りんごを見つけた！（＋" + addScore + "点）");
      default -> player.sendMessage(
          "ざんねん！はずれ！");
    }
  }


  /**
   * 現在実行しているプレイヤーのスコア情報を取得する
   *
   * @param player 　コマンドを実行したプレイヤー
   * @return　現在実行しているプレイヤーのスコア情報
   */
  private PlayerScore getPlayerScore(Player player) {
    if (playerScoreList.isEmpty()) {
      return addNewPlayer(player);
    } else {
      for (PlayerScore playerScore : playerScoreList) {
        if (!playerScore.getPlayerName().equals(player.getName())) {
          return addNewPlayer(player);
        } else {
          return playerScore;
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
  private PlayerScore addNewPlayer(Player player) {
    PlayerScore newPlayer = new PlayerScore();
    newPlayer.setPlayerName(player.getName());
    playerScoreList.add(newPlayer);
    return newPlayer;
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
}


