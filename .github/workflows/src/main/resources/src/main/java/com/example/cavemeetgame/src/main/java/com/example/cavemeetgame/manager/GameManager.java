package com.example.cavemeetgame.manager;
import com.example.cavemeetgame.CaveMeetGame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;
public class GameManager {
    private final CaveMeetGame plugin;
    private boolean gameRunning = false;
    private UUID leaderUUID = null;
    private final Set<UUID> activePlayers = new HashSet<>();
    private final Set<UUID> submittedPlayers = new HashSet<>();
    public GameManager(CaveMeetGame plugin) { this.plugin = plugin; }
    public CaveMeetGame getPlugin() { return plugin; }
    public boolean isGameRunning() { return gameRunning; }
    public UUID getLeaderUUID() { return leaderUUID; }
    public void setLeaderUUID(UUID leaderUUID) { this.leaderUUID = leaderUUID; }
    public Set<UUID> getActivePlayers() { return activePlayers; }
    public Set<UUID> getSubmittedPlayers() { return submittedPlayers; }
    public void startGame() {
        if (gameRunning) return;
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (onlinePlayers.isEmpty() || onlinePlayers.size() > 4) return;
        if (leaderUUID == null || Bukkit.getPlayer(leaderUUID) == null) {
            leaderUUID = onlinePlayers.get(new Random().nextInt(onlinePlayers.size())).getUniqueId();
        }
        gameRunning = true;
        activePlayers.clear();
        submittedPlayers.clear();
        FileConfiguration config = plugin.getConfig();
        int minTicks = config.getInt("spawn.min-distance", 200);
        int maxTicks = config.getInt("spawn.max-distance", 1000);
        Random random = new Random();
        broadcastMessage(Component.text("========== ゲーム開始 ==========", NamedTextColor.GOLD));
        for (Player player : onlinePlayers) {
            activePlayers.add(player.getUniqueId());
            player.setGameMode(GameMode.SURVIVAL);
            AttributeInstance maxHealthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxHealthAttr != null) maxHealthAttr.setBaseValue(20.0);
            player.setHealth(20.0);
            player.getInventory().clear();
            if (!player.getUniqueId().equals(leaderUUID)) {
                player.getInventory().addItem(createMemberCard());
            }
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = minTicks + (maxTicks - minTicks) * random.nextDouble();
            int x = (int) (player.getWorld().getSpawnLocation().getX() + Math.cos(angle) * distance);
            int z = (int) (player.getWorld().getSpawnLocation().getZ() + Math.sin(angle) * distance);
            int y = player.getWorld().getHighestBlockYAt(x, z);
            player.teleport(new Location(player.getWorld(), x + 0.5, y + 1, z + 0.5));
        }
    }
    public void stopGame() {
        if (!gameRunning) return;
        gameRunning = false;
        broadcastMessage(Component.text("ゲームが強制終了されました。", NamedTextColor.RED));
    }
    public void handlePlayerDeath(Player player) {
        if (!gameRunning) return;
        activePlayers.remove(player.getUniqueId());
        String mode = plugin.getConfig().getString("gameOverMode", "ANY_DEATH");
        if (mode.equalsIgnoreCase("ANY_DEATH")) {
            endGame(false, player.getName() + " が死亡したため終了。");
        } else if (activePlayers.isEmpty() || player.getUniqueId().equals(leaderUUID)) {
            endGame(false, "全滅またはリーダー死亡。");
        }
    }
    public void submitCard(Player player) {
        if (!gameRunning || submittedPlayers.contains(player.getUniqueId())) return;
        submittedPlayers.add(player.getUniqueId());
        long requiredCount = activePlayers.stream().filter(uuid -> !uuid.equals(leaderUUID)).count();
        if (submittedPlayers.size() >= requiredCount) {
            endGame(true, "全員合流完了！ゲームクリア！");
        }
    }
    private void endGame(boolean isClear, String reason) {
        gameRunning = false;
        broadcastMessage(Component.text(reason, isClear ? NamedTextColor.GREEN : NamedTextColor.RED));
    }
    public void broadcastMessage(Component component) {
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(component);
    }
    public ItemStack createMemberCard() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.displayName(Component.text("隊員証")); item.setItemMeta(meta); }
        return item;
    }
    public ItemStack createHeartUp() {
        ItemStack item = new ItemStack(Material.NETHER_WART);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.displayName(Component.text("ハートアップ")); item.setItemMeta(meta); }
        return item;
    }
}
