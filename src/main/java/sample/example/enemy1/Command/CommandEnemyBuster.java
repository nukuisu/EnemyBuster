package sample.example.enemy1.Command;

import data.ExecutingPlayer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import mapper.data.PlayerScore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Listener;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import sample.example.enemy1.PlayerScoreData;
import sample.example.enemy1.EnemyBuster;

/**
 * 制限時間内にランダムで出現する敵を倒して、スコアを獲得するゲームを起動するコマンドです
 * スコアは敵によって変わり、倒した敵によってスコアが変動します
 */
public class CommandEnemyBuster extends BaseCommand implements  Listener {

  public static final int GAME_TIME = 20;
  public static final String EASY = "easy";
  public static final String NORMAL = "normal";
  public static final String HARD = "hard";
  public static final String NONE = "none";
  public static final String ListA = "list_a";


  private final EnemyBuster enemyBuster;
  private final PlayerScoreData playerScoreData = new PlayerScoreData();

  private final List<ExecutingPlayer> executingPlayerList = new ArrayList<>();
  private final List<Entity> spawnEntityList = new ArrayList<>();





  public CommandEnemyBuster(EnemyBuster enemyBuster) {
    this.enemyBuster=enemyBuster;

  }


  @Override
  public boolean onExecutePlayerCommand(Player player,Command command,
      String label, String[] args) {
    //最初の引数が「list」だったらスコアを一覧して処理を終了する。
    if (args.length == 1 && ListA.equals(args[0])) {
      return sendPlayerScoreList(player);
    }

    String difficulty = getDifficulty(player, args);
    if (difficulty.equals(NONE)){
      return  false;
    }
    // プレイヤーがリストに存在するかチェック
    ExecutingPlayer nowPlayer = getPlayerScore(player);

    //プレイヤーの状態を初期化する（体力と空腹度を最大化する）
    player.setHealth(20);
    player.setFoodLevel(20);
    PlayerInventory inventory = player.getInventory();
    inventory.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
    inventory.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
    inventory.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
    inventory.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
    inventory.setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));

    nowPlayer.setGameTime(GAME_TIME);
    nowPlayer.setScore(0);
    removePotionEffects(player);

    gamePlay(player, nowPlayer,difficulty);
    return true;
  }



  /**
   *
   * @param player コマンドを実行したプレイヤー
   * @param args　コマンド引数
   * &#064;return　難易度
   */
  @NotNull
   String getDifficulty(Player player, String[] args) {
    if (args.length == 1 && EASY.equals(args[0]) || NORMAL.equals(args[0]) || HARD.equals(args[0])) {
      return  args[0];
    }
    player.sendMessage(ChatColor.RED+"実行できません。コマンド引数1つ目に難易度設定が必要です。[easy.normal.hard]");
    return NONE;
  }

  /**
   * プレイヤーに設定されている特殊状態を除外します。
   *
   * @param player　コマンドを実行したプレイヤー
   */
  private void removePotionEffects(Player player) {
    player.getActivePotionEffects().stream()
        .map(PotionEffect::getType)
        .forEach(player::removePotionEffect);
    player.getActivePotionEffects();
  }


  /**
   * ゲームを実行します。規定の時間内に敵を倒すとスコアが表示されます
   * @param player　コマンドを実行したプレイヤー
   * @param nowPlayer　プレイヤーのスコア状況
   * @param difficulty  難易度
   */
  private void gamePlay(Player player, ExecutingPlayer nowPlayer, String difficulty) {
    new BukkitRunnable() {
      @Override
      public void run() {
        if (nowPlayer.getGameTime() <= 0) {
          // タイマー停止
          cancel();

          player.sendTitle(
              "ゲームが終了しました！",
              nowPlayer.getPlayerName() + "合計" + nowPlayer.getScore() + "点！",
              0, 60, 0
          );



          try (Connection con = DriverManager.getConnection(
              "jdbc:mysql://localhost:3306/spigot_server"+ "?useSSL=false&allowPublicKeyRetrieval=true"
                  + "&serverTimezone=Asia/Tokyo&useUnicode=true&characterEncoding=utf8",
              "nukuisu",
              "1467Quinn" );
            Statement statement = con.createStatement()){

            statement.executeUpdate(
                "insert into player_score(player_name,score,difficulty,registered_at)"
                + "values ('"+nowPlayer.getPlayerName()+"',"+ nowPlayer.getScore()+",'"
                +difficulty+"',now());" );
          }catch (SQLException e){
            e.printStackTrace();
          }

          // 生成した敵を全消し＆一覧クリア
          spawnEntityList.forEach(Entity::remove);
          spawnEntityList.clear();

          removePotionEffects(player);

          playerScoreData.insert(
              new PlayerScore(nowPlayer.getPlayerName()
                  , nowPlayer.getScore()
                  , difficulty));

          return;
        }

        // 敵出現
        Entity spawnEntity = player.getWorld()
            .spawnEntity(getEnemySpawnLocation(player), getEnemy(difficulty));
        spawnEntityList.add(spawnEntity);

        // 残り時間を減らす（5秒ごと）
        nowPlayer.setGameTime(nowPlayer.getGameTime() - 5);
      }
    }.runTaskTimer(enemyBuster, 0L, 5L * 20L); // 0秒後に開始、5秒周期
  }

  // 敵の抽選（※クラス直下：ラムダの外に置く）
  private EntityType getEnemy(String difficulty) {
    String key = Objects.toString(difficulty, "").toUpperCase(Locale.ROOT);

    List<EntityType> pool = switch (key) {
      case "NORMAL" -> List.of(EntityType.ZOMBIE, EntityType.SKELETON);
      case "HARD"   -> List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.WITCH);
      default       -> List.of(EntityType.ZOMBIE);
    };

    return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
  }

  @Override
  public boolean onExecuteNPCCommand(CommandSender sender, Command command,
      String label, String[] args) {
    return false;
  }

  private ExecutingPlayer getPlayerScore(Player player) {
    long dupCount = executingPlayerList.stream()
        .filter(ps -> ps.getPlayerName().equals(player.getName()))
        .count();

    Bukkit.getLogger().info("[DEBUG] " + player.getName() + " entries = " + dupCount);

    ExecutingPlayer found = executingPlayerList.stream()
        .filter(executingPlayer ->
            executingPlayer.getPlayerName().
                equals(player.getName())).
        findFirst()
        .orElse(null);

    // プレイヤーが存在しない場合のみ追加
    if (found != null)
      return found;
    found = new ExecutingPlayer();
    found.setPlayerName(player.getName());
    executingPlayerList.add(found);
    return found;
  }


  /**
   * 現在登録されているスコアの一覧をメッセージに送る。
   *
   * @param player　プレイヤー
   *
   */
  private boolean sendPlayerScoreList(Player player) {
    List<PlayerScore> playerScoreList = playerScoreData.selectList();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    for (PlayerScore playerScore : playerScoreList) {

      player.sendMessage(
          playerScore.getId() + " | " +
              playerScore.getPlayerName() + " | " +
              playerScore.getScore() + " | " +
              playerScore.getDifficulty() + " | " +
              playerScore.getRegisteredAt().format(formatter)
      );
    }
    return false;
  }


  @EventHandler
  public void onEnemyDeath(EntityDeathEvent e) {
    LivingEntity enemy = e.getEntity();
    Player player = enemy.getKiller();



    if (Objects.isNull(player) || spawnEntityList.stream()
        .noneMatch(entity -> entity.equals(enemy))) {
      return;
    }

     executingPlayerList.stream()
        .filter(executingPlayer -> executingPlayer.getPlayerName().equals(player.getName()))
        .findFirst()
         .ifPresent(p -> {
           int point = switch (enemy.getType()) {
             case ZOMBIE -> 10;
             case SKELETON -> 15;
             case WITCH-> 20;
             default -> 0;
           };

           p.setScore(p.getScore() + point);
           player.sendMessage("敵を倒した！現在のスコアは" + p.getScore() + "点");

         });

  }

  /**
   * 敵の出現エリアを取得します 出現エリアはX軸とZ軸は自分の位置からプラス、ランダムで-10~9の値が設定されます・ Y軸はプレイヤーと同じ場所になります
   *
   * @param player 　コマンドを実行したプレイヤー
   * &#064;return　敵の出現場所
   */
  private Location getEnemySpawnLocation(Player player) {
    Location playerLocation = player.getLocation();
    int randomX = new SplittableRandom().nextInt(20) - 10;
    int randomZ = new SplittableRandom().nextInt(20) - 10;

    double x = playerLocation.getX() + randomX;
    double y = playerLocation.getY();
    double z = playerLocation.getZ() + randomZ;

    return new Location(player.getWorld(), x, y, z);
  }
}