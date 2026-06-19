package com.example.cavemeetgame;
import com.example.cavemeetgame.command.MeetCommand;
import com.example.cavemeetgame.listener.GameListener;
import com.example.cavemeetgame.manager.GameManager;
import org.bukkit.plugin.java.JavaPlugin;
public final class CaveMeetGame extends JavaPlugin {
    private GameManager gameManager;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.gameManager = new GameManager(this);
        getServer().getPluginManager().registerEvents(new GameListener(gameManager), this);
        if (getCommand("meet") != null) {
            getCommand("meet").setExecutor(new MeetCommand(gameManager));
            getCommand("meet").setTabCompleter(new MeetCommand(gameManager));
        }
    }
    @Override
    public void onDisable() {
        if (gameManager != null && gameManager.isGameRunning()) {
            gameManager.stopGame();
        }
    }
}
