package plugin.treasurehunt.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import plugin.treasurehunt.data.PlayerData;

public class FindGoldenAppleCommand implements CommandExecutor, Listener {

  public static final int POT_AMOUNT = 5;
  public static final int APPLE_AMOUNT = 2;

  public static final String GOLDEN_APPLE = "golden_apple";
  public static final String APPLE = "apple";
  public static final String NONE = "none";

  public static final int APPLE_SCORE = 10;
  public static final int BONUS_SCORE = 50;


  private final List<PlayerData> playerDataList = new ArrayList<>();
  private final Map<Block, String> potIDMap = new HashMap<>();


  @Override
  public boolean onCommand(CommandSender commandSender, Command command, String s,
      String[] strings) {

    if (commandSender instanceof Player player) {
      if (playerDataList.isEmpty()) {
        addNewPlayer(player);
      } else {
        for (PlayerData playerData : playerDataList) {
          if (!playerData.getPlayerName().equals(player.getName())) {
            addNewPlayer(player);
          }
          playerData.setScore(0);
          potIDMap.clear();
        }
      }

      player.sendTitle("START", "金のりんごを探せ！", 0, 30, 0);

      spawnedPotRegistry(player);
    }
    return false;
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
   * IDに基づいて、金のりんご、りんご、ドロップなしを決定する。
   *
   * @param id 出現した飾り壺のID
   * @return ドロップアイテムの種類
   */
  private String idItemDrop(int id) {
    if (id == 1) {
      return GOLDEN_APPLE;
    } else if (id >= 2 && id <= 2 + APPLE_AMOUNT - 1) {
      return APPLE;
    } else {
      return NONE;
    }
  }


  /**
   * 飾り壺を壊すとMapに登録されている情報をに基づいて、アイテムがドロップする。 デフォルトのドロップアイテムはドロップしないようにする。ドロップアイテムに応じてスコアを加算する
   *
   * @param breakEvent 飾り壺を壊したときのイベント
   * @return 飾り壺のドロップアイテム
   */
  @EventHandler
  public void onPotBreak(BlockBreakEvent breakEvent) {
    Block block = breakEvent.getBlock();
    Player player = breakEvent.getPlayer();

    if (block.getType() == Material.DECORATED_POT) {
      String dropItem = potIDMap.get(block);

      handleBlockDrop(breakEvent, dropItem, block);
      breakEvent.setDropItems(false);

      if (playerDataList.isEmpty()) {
        return;
      }
      for (PlayerData playerData : playerDataList) {
        if (playerData.getPlayerName().equals(player.getName())) {
          switch (dropItem) {
            case GOLDEN_APPLE -> playerData.setScore(playerData.getScore() + BONUS_SCORE);
            case APPLE -> playerData.setScore(playerData.getScore() + APPLE_SCORE);
            default -> playerData.setScore(playerData.getScore());
          }
        }

        potIDMap.remove(block);

        int count = 0;
        for (Map.Entry<Block, String> entry : potIDMap.entrySet()) {
          if (entry.getValue().equals(GOLDEN_APPLE) || entry.getValue().equals(APPLE)) {
            count++;
          }
        }
        player.sendTitle("", "りんごは残り" + count + "個", 0, 30, 10);

        messageOnFound(playerData, dropItem, player);

      }
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
      case GOLDEN_APPLE -> block.getWorld()
          .dropItemNaturally(block.getLocation(), new ItemStack(Material.GOLDEN_APPLE));
      case APPLE -> block.getWorld()
          .dropItemNaturally(block.getLocation(), new ItemStack(Material.APPLE));
      case NONE -> breakEvent.setDropItems(false);
    }
  }


  /**
   * 飾り壺を壊してドロップアイテムが判明後、プレイヤーに獲得スコア、現在のトータルスコア、残りんご数の情報を送る
   *
   * @param playerData プレイヤー情報リスト
   * @param dropItem   飾り壺を壊した後のドロップアイテム
   * @param player     コマンドを実行したプレイヤー
   */
  private void messageOnFound(PlayerData playerData, String dropItem, Player player) {
    switch (dropItem) {
      case GOLDEN_APPLE -> player.sendMessage(
          "金のりんごを見つけた！SCORE："
              + BONUS_SCORE
              + "　TOTAL：" + playerData.getScore());
      case APPLE -> player.sendMessage(
          "りんごを見つけた！SCORE："
              + APPLE_SCORE
              + "　TOTAL：" + playerData.getScore());
      default -> player.sendMessage(
          "ざんねん！はずれ！");
    }
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
   * 新規のプレイヤー情報をリストに追加する
   *
   * @param player コマンドを実行したプレイヤー
   */
  private void addNewPlayer(Player player) {
    PlayerData newplayerData = new PlayerData();
    newplayerData.setPlayerName(player.getName());
    playerDataList.add(newplayerData);
  }
}
