package plugin.treasurehunt.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import plugin.treasurehunt.mapper.data.PlayerScore;

public interface PlayerScoreMapper {

  @Select("select * from player_score order by score DESC limit 5")
  List<PlayerScore> selectList();

  @Insert("insert player_score(player_name, score, registered_at) values (#{playerName}, #{score}, now())")
  int insert(PlayerScore playerScore);
}
