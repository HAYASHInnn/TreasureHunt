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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import plugin.treasurehunt.data.PlayerData;

public class FindGoldenAppleCommand implements CommandExecutor, Listener {

  public static final int POT_AMOUNT = 3;
  public static final int APPLE_AMOUNT = 1;

  public static final String GOLDEN_APPLE = "golden_apple";
  public static final String APPLE = "apple";
  public static final String NONE = "none";


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

      player.sendTitle("START", "りんごを探せ！", 0, 30, 0);

      for (int i = 1; i <= POT_AMOUNT; i++) {
        Block block = getDecoratedPotLocation(player).getBlock();
        block.setType(Material.DECORATED_POT);

        String dropType = idItemDrop(i);
        potIDMap.put(block, dropType);
      }
    }
    return false;
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
   * 飾り壺を壊すとりんごがドロップする。デフォルトでドロップするアイテムはドロップしないようにする。
   *
   * @param breakEvent 飾り壺を壊したときのイベント
   * @return　飾り壺のドロップアイテム
   */
  @EventHandler
  public void onPotBreak(BlockBreakEvent breakEvent) {
    Block block = breakEvent.getBlock();

    if (block.getType() == Material.DECORATED_POT) {
      String dropItem = potIDMap.get(block);

      switch (dropItem) {
        case GOLDEN_APPLE -> block.getWorld()
            .dropItemNaturally(block.getLocation(), new ItemStack(Material.GOLDEN_APPLE));
        case APPLE ->
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.APPLE));
        default -> breakEvent.setDropItems(false);
      }

      potIDMap.remove(block);
    }
  }


  @EventHandler
  public void onEntityPickupItem(EntityPickupItemEvent itemEvent) {
    Entity entity = itemEvent.getEntity();
    Item item = itemEvent.getItem();

    if (playerDataList.isEmpty()) {
      return;
    }

    for (PlayerData playerData : playerDataList) {
      if (item.getItemStack().getType() == Material.APPLE && playerData.getPlayerName()
          .equals(entity.getName())) {
        playerData.setScore(playerData.getScore() + 10);
        entity.sendMessage(
            "おめでとう！りんごを獲得しました(TOTAL：" + playerData.getScore() + "点)");
      }
    }
  }


  /**
   * 飾り壺の出現場所を取得します。 出現エリアのX軸は...Y軸はプレイヤーと同じ位置になります。
   *
   * @param player 　コマンドを実行したプレイヤー
   * @return　飾り壺の出現場所
   */
  private Location getDecoratedPotLocation(Player player) {
    Location playerlocation = player.getLocation();
    int randomZ = new SplittableRandom().nextInt(10) - 5;

    double x = playerlocation.getX() + 3;
    double y = playerlocation.getY();
    double z = playerlocation.getZ() + randomZ;

    return new Location(player.getWorld(), x, y, z);
  }


  /**
   * 新規のプレイヤー情報をリストに追加する
   *
   * @param player 　コマンドを実行したプレイヤー
   */
  private void addNewPlayer(Player player) {
    PlayerData newplayerData = new PlayerData();
    newplayerData.setPlayerName(player.getName());
    playerDataList.add(newplayerData);
  }
}
