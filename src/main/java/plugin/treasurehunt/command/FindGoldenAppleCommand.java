package plugin.treasurehunt.command;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FindGoldenAppleCommand implements CommandExecutor {

  @Override
  public boolean onCommand(CommandSender commandSender, Command command, String s,
      String[] strings) {
    if (commandSender instanceof Player player) {
      Block block = getDecoratedPotLocation(player).getBlock();
      block.setType(Material.DECORATED_POT);
    }
    return false;
  }

  /**
   * 飾り壺の出現を取得します。 出現エリアのX軸は3。 Y軸はプレイヤーと同じ位置になります。
   *
   * @param player 　コマンドを実行したプレイヤー
   * @return　飾り壺の出現場所
   */
  private Location getDecoratedPotLocation(Player player) {
    Location playerlocation = player.getLocation();

    double x = playerlocation.getX() + 3;
    double y = playerlocation.getY();
    double z = playerlocation.getZ();

    return new Location(player.getWorld(), x, y, z);
  }
}
