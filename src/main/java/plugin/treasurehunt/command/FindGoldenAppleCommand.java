package plugin.treasurehunt.command;

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

public class FindGoldenAppleCommand implements CommandExecutor, Listener {

  @Override
  public boolean onCommand(CommandSender commandSender, Command command, String s,
      String[] strings) {
    if (commandSender instanceof Player player) {
      Block treasurePot = getDecoratedPotLocation(player).getBlock();
      treasurePot.setType(Material.DECORATED_POT);
    }
    return false;
  }


  @EventHandler
  public void onBlockBreak(BlockBreakEvent b) {
    Block block = b.getBlock();
    Player breaker = b.getPlayer();

    if (block.getType() == Material.DECORATED_POT) {
      block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.APPLE));
      b.setDropItems(false);
    }
  }

  @EventHandler
  public void onEntityPickupItem(EntityPickupItemEvent itemEvent) {
    Entity entity = itemEvent.getEntity();

    if (entity instanceof Player player) {
      Item item = itemEvent.getItem();

      if (item.getItemStack().getType() == Material.APPLE) {
        player.sendMessage("おめでとう！りんごを獲得しました");
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

    double x = playerlocation.getX() + 3;
    double y = playerlocation.getY();
    double z = playerlocation.getZ();

    return new Location(player.getWorld(), x, y, z);
  }
}