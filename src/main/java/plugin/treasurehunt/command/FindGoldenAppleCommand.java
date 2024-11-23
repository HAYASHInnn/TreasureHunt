package plugin.treasurehunt.command;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import plugin.treasurehunt.PlayerScoreData;
import plugin.treasurehunt.TreasureHunt;
import plugin.treasurehunt.data.PlayerData;
import plugin.treasurehunt.mapper.data.PlayerScore;
import plugin.treasurehunt.task.CountdownTask;

import static plugin.treasurehunt.constants.FindGoldenAppleConstants.*;

/**
 * 制限時間内にランダムに出現した飾り壺を割り、金のりんご、りんごを見つけてスコアを獲得するゲームを起動するコマンドです。
 * スコアはりんごを見つけた時点のゲームの残り時間によって変動します。また金のりんごを見つけるとボーナススコアが加点されます。 結果はプレイヤー名、スコア、日時などで保存されます。
 */

public class FindGoldenAppleCommand extends BaseCommand implements Listener {
    // カウントダウン中のフラグ
    private boolean isCountdownActive = false;

    private final TreasureHunt treasureHunt;
    private PlayerScoreData playerScoreData = new PlayerScoreData();

    private final List<PlayerData> playerDataList = new ArrayList<>();

    public FindGoldenAppleCommand(TreasureHunt treasurehunt, PlayerScoreData playerScoreData) {
        this.treasureHunt = treasurehunt;
        this.playerScoreData = playerScoreData;
    }

    @Override
    protected boolean onExecutePlayerCommand(Player player, Command command, String label,
                                          String[] args) {
        // 最初の引数が「list」だったらスコアを一覧表示して処理を終了する
        if (args.length == 1 && LIST.equals(args[0])) {
            sendPlayerScoreRank(player);
            return false;
        }

        PlayerData nowPlayerData = getPlayerData(player);

        player.sendMessage(
                "ヒント: 金のりんごは +" + BONUS_SCORE + "点！");
        player.sendMessage(
                "ヒント: 見つける時間が早いほどスコアは高くなります！");


        new CountdownTask(treasureHunt, player, nowPlayerData).start(isCountdownActive);
        return true;
    }

    @Override
    protected boolean onExecuteNPCCommand(CommandSender sender, Command command, String label,
                                       String[] args) {
        return false;
    }

    //TODO: 以下のメソッドは、PlayerHandlerなどのクラスに移動することを検討してください。
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if (isCountdownActive) {
            Player player = e.getPlayer();
            Location from = e.getFrom();
            Location to = e.getTo();

            // 実際に移動が発生しようとした場合、位置を元に戻す
            if (to == null || (from.getX() == to.getX() && from.getZ() == to.getZ())) {
                return;
            }
            player.teleport(from);
        }
    }
    //TODO: 以下のメソッドは、PlayerHandlerなどのクラスに移動することを検討してください。
    @EventHandler
    public void onPotBreak(BlockBreakEvent breakEvent) {
        Block block = breakEvent.getBlock();
        Player player = breakEvent.getPlayer();

        if (Objects.isNull(player) || playerDataList.isEmpty()) {
            return;
        }

        potIDMap.entrySet().stream()
                .filter(entry -> entry.getKey().equals(block)
                        && entry.getKey().getType() == Material.DECORATED_POT)
                .findFirst()
                .ifPresent(entry -> {

                    DropItem dropItem = potIDMap.get(block);
                    dropItemOnPotBreak(breakEvent, dropItem, block);

                    if ("NONE_ITEM_DROP".equals(dropItem)) {
                        return;
                    }

                    playerDataList.forEach(playerData -> {
                        Integer addScore = getAddScore(playerData, dropItem, player);
                        if (addScore == null) {
                            return;
                        }
                        potIDMap.remove(block);
                        finishGameIfApplesGone(player);
                        messageOnFound(dropItem, player, addScore);

                    });

                    breakEvent.setDropItems(false);
                });
    }

    /**
     * 現在登録されているスコアの一覧をメッセージに送る。
     *
     * @param player 　プレイヤー
     */
    private void sendPlayerScoreRank(Player player) {
        List<PlayerScore> playerScoreList = playerScoreData.selectList();

        player.sendMessage("======== 🏆 現在のランキング Top 5 🏆 ========");
        player.sendMessage("順位 | プレイヤー名 | スコア | 登録日時");

        int rank = 1;
        for (PlayerScore playerScore : playerScoreList) {
            player.sendMessage(
                    String.format("%2d位 | %-10s | %5d | %s",
                            rank++,
                            playerScore.getPlayerName(),
                            playerScore.getScore(),
                            playerScore.getRegisteredAt()
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    )
            );
        }
        player.sendMessage("=======================================");
    }

    /**
     * 現在実行しているプレイヤーのスコア情報を取得する
     *
     * @param player 　コマンドを実行したプレイヤー
     * @return　現在実行しているプレイヤーのスコア情報
     */
    private PlayerData getPlayerData(Player player) {
        PlayerData playerData = new PlayerData(player.getName());

        if (playerDataList.isEmpty()) {
            playerData = addNewPlayer(player);
        } else {
            playerData = playerDataList.stream()
                    .findFirst()
                    .map(pd -> pd.getPlayerName().equals(player.getName())
                            ? pd
                            : addNewPlayer(player)).orElse(playerData);
        }

        playerData.setGameTime(GAME_TIME);
        return playerData;
    }

    /**
     * 新規のプレイヤー情報をリストに追加する
     *
     * @param player コマンドを実行したプレイヤー
     * @return　新規プレイヤー
     */
    private PlayerData addNewPlayer(Player player) {
        PlayerData newPlayer = new PlayerData(player.getName());
        playerDataList.add(newPlayer);
        return newPlayer;
    }
}