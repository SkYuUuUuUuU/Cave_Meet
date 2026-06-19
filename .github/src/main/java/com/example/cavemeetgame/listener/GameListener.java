package com.example.cavemeetgame.listener;
import com.example.cavemeetgame.manager.GameManager;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.Random;
public class GameListener implements Listener {
    private final GameManager gameManager;
    private final Random random = new Random();
    public GameListener(GameManager gameManager) { this.gameManager = gameManager; }
    @EventHandler
    public void onPvP(EntityDamageByEntityEvent event) {
        if (gameManager.isGameRunning() && event.getEntity() instanceof Player && event.getDamager() instanceof Player) event.setCancelled(true);
    }
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // ※元のコードのPlayerDeathEventの型エラー対策で修正
        if (gameManager.isGameRunning() && gameManager.getActivePlayers().contains(event.getEntity().getUniqueId())) gameManager.handlePlayerDeath(event.getEntity());
    }
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!gameManager.isGameRunning() || !(event.getRightClicked() instanceof Player target)) return;
        if (target.getUniqueId().equals(gameManager.getLeaderUUID())) {
            ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
            if (item.isSimilar(gameManager.createMemberCard())) {
                event.setCancelled(true);
                item.setAmount(item.getAmount() - 1);
                gameManager.submitCard(event.getPlayer());
            }
        }
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item != null && item.isSimilar(gameManager.createHeartUp())) {
            event.setCancelled(true);
            item.setAmount(item.getAmount() - 1);
            AttributeInstance attr = event.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attr != null) { attr.setBaseValue(attr.getBaseValue() + 2.0); event.getPlayer().heal(2.0); }
        }
    }
    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        String key = switch (event.getEntityType()) {
            case ZOMBIE -> "zombie"; case SKELETON -> "skeleton"; case SPIDER -> "spider";
            case CREEPER -> "creeper"; case WITCH -> "witch"; case ENDERMAN -> "enderman"; default -> null;
        };
        if (key != null && !gameManager.getPlugin().getConfig().getBoolean("monsters." + key, true)) event.setCancelled(true);
    }
    @EventHandler
    public void onChunkPopulate(ChunkPopulateEvent event) {
        if (random.nextDouble() > gameManager.getPlugin().getConfig().getDouble("chest.generation-chance", 0.05)) return;
        int x = (event.getChunk().getX() << 4) + random.nextInt(16);
        int z = (event.getChunk().getZ() << 4) + random.nextInt(16);
        int y = random.nextInt(40) + 20;
        Block block = event.getWorld().getBlockAt(x, y, z);
        block.setType(Material.CHEST);
        if (block.getState() instanceof Chest chest) {
            Inventory inv = chest.getInventory();
            inv.setItem(random.nextInt(inv.getSize()), new ItemStack(Material.COOKED_BEEF, 4));
            if (random.nextDouble() <= gameManager.getPlugin().getConfig().getDouble("chest.heart-up-chance", 0.10)) {
                inv.setItem(random.nextInt(inv.getSize()), gameManager.createHeartUp());
            }
        }
    }
}
