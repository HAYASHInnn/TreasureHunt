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

  private List<PlayerData> playerDataList = new ArrayList<>();
  private final Map<Location, Integer> potIDMap = new HashMap<>();


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
        }
      }

      player.sendTitle("START", "りんごを探せ！", 0, 30, 0);

      for (int id = 1; id <= POT_AMOUNT; id++) {
        potIDMap.put(getDecoratedPotLocation(player), id);
        Block treasurePot = getDecoratedPotLocation(player).getBlock();
        treasurePot.setType(Material.DECORATED_POT);
      }
    }
    return false;
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
      block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.APPLE));
      breakEvent.setDropItems(false);
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
