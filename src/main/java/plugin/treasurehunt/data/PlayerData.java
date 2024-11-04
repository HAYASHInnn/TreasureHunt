package plugin.treasurehunt.data;

import lombok.Getter;
import lombok.Setter;

/**
 * EnemyDownのゲームを実行する際のプレイヤー情報を扱うオブジェクト。 プレイヤー名、合計点数、日時などの情報を持つ。
 */
@Getter
@Setter

public class PlayerData {

  private String playerName;
  private int score;
  private int gameTime;

  public PlayerData(String playerName) {
    this.playerName = playerName;
  }
}
