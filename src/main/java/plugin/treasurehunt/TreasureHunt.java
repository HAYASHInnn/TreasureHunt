package plugin.treasurehunt;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import plugin.treasurehunt.command.FindGoldenAppleCommand;

public final class TreasureHunt extends JavaPlugin implements Listener {

  @Override
  public void onEnable() {
    Bukkit.getPluginManager().registerEvents(this, this);
    getCommand("findGoldenApple").setExecutor(new FindGoldenAppleCommand());
  }
}
