package sample.example.enemy1;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import mapper.PlayerScoreMapper;
import mapper.data.PlayerScore;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

/**
 * DB接続やそれに付随する登録や更新処理を行うクラスです
 */
public class PlayerScoreData {

  private final PlayerScoreMapper mapper;

  public PlayerScoreData() {

    try {
      InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
      SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
      SqlSession session = sqlSessionFactory.openSession(true);
      this.mapper = session.getMapper(PlayerScoreMapper.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * プレイヤースコアテーブルから一覧でスコア情報を取得する
   * &#064;return　スコア情報の一覧
   */
  public List<PlayerScore> selectList() {
      return mapper.selectList();
  }

  /**
   * プレイヤースコアテーブルにスコア情報を登録する。
   *
   * @param playerScore　プレイヤースコア情報
   */
  public void insert(PlayerScore playerScore) {
    //スコア登録処理
      mapper.insert(playerScore);
  }
}
