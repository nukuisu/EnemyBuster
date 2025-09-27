package sample.example.enemy1;

import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import sample.example.enemy1.Command.CommandEnemyBuster;


public final class EnemyBuster extends JavaPlugin {

  @Override
  public void onEnable() {
    CommandEnemyBuster commandEnemyBuster =new CommandEnemyBuster(this);
    Bukkit.getPluginManager().registerEvents(commandEnemyBuster, this);
    Objects.requireNonNull(getCommand("EnemyBuster")).setExecutor(commandEnemyBuster);
  }


  }
