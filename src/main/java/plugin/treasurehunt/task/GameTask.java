package plugin.treasurehunt.task;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import plugin.treasurehunt.TreasureHunt;
import plugin.treasurehunt.constants.FindGoldenAppleConstants;
import plugin.treasurehunt.data.PlayerData;

public class GameTask implements Runnable {
    private final TreasureHunt treasurehunt;
    private final Player player;
    private final PlayerData nowPlayerData;
    private int gameTime = FindGoldenAppleConstants.GAME_TIME;
    private BossBar bossBar;

    public GameTask(TreasureHunt treasurehunt, Player player, PlayerData nowPlayerData) {
        this.treasurehunt = treasurehunt;
        this.player = player;
        this.nowPlayerData = nowPlayerData;
    }

    public void start() {
        bossBar = Bukkit.createBossBar("残り時間: " + gameTime + "秒", BarColor.BLUE, BarStyle.SOLID);
        bossBar.addPlayer(player);

        Bukkit.getScheduler().runTaskTimer(treasurehunt, this, 0, 20);
    }

    @Override
    public void run() {
        if (gameTime <= 0) {
            endGame();
            return;
        }

        bossBar.setTitle("残り時間: " + gameTime-- + "秒");
        bossBar.setProgress((double) gameTime / FindGoldenAppleConstants.GAME_TIME);
    }

    private void endGame() {
        bossBar.removeAll();
        player.sendTitle("FINISH", "TOTAL SCORE：" + nowPlayerData.getScore(), 0, 60, 10);
        // その他の終了処理
    }
}
