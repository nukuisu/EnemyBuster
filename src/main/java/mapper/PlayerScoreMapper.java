package mapper;

import java.util.List;
import mapper.data.PlayerScore;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

public interface PlayerScoreMapper {

  @Select("select*from player_score;")
  List<PlayerScore> selectList();

  @Insert("insert into player_score(player_name,score,difficulty,registered_at) values (#{playerNane}, #{score}, #{difficulty},now()" )
  int insert(PlayerScore playerScore);
}
