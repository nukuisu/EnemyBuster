package data;

import lombok.Getter;
import lombok.Setter;

/**
 * EnemyBusterのゲームを実行する際のプレイヤー情報を扱うオブジェクト
 * プレイヤー名、合計点数、日時などを持つ
 */
@Getter
@Setter

public class ExecutingPlayer {

  private String playerName;
  private int score;
  private int gameTime;


}
