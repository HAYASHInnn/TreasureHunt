package plugin.treasurehunt.data;

import lombok.Getter;
import lombok.Setter;

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
