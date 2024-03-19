package com.axpplt.cooldownplugin;


import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Plugin extends JavaPlugin implements Listener {

    private Material targetMaterial = Material.DIAMOND_BLOCK; // Default material
    private int levelCap = 5; // Maximum experience level
    private BukkitRunnable animationTask = null; // Store animation task
    private long startTimeMillis; // Store start time of animation

    @Override
    public void onEnable() {
        getLogger().info("Plugin successfully enabled!");
        getServer().getPluginManager().registerEvents(this, this);
        loadConfig();
    }

    @Override
    public void onDisable() {
        getLogger().info("Plugin disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("myplugin")) {
            if (sender instanceof Player) {
                sender.sendMessage("Example plugin command.");
                return true;
            } else {
                sender.sendMessage("This command can only be used by players.");
                return false;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction().name().contains("RIGHT")) {
            ItemStack itemStack = player.getInventory().getItemInMainHand();
            if (itemStack != null && itemStack.getType() == targetMaterial) {
                if (animationTask == null || animationTask.isCancelled()) { // Check if animation task is not running
                    startTimeMillis = System.currentTimeMillis(); // Store start time
                    animateExperience(player);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        if (player.getLevel() >= levelCap && event.getAmount() > 0) {
            event.setAmount(0);
            player.setExp(0);
            player.setLevel(0);
        }
    }

    private void loadConfig() {
        getConfig().options().copyDefaults(true);
        saveConfig();
        FileConfiguration config = getConfig();
        String targetMaterialName = config.getString("targetMaterial", "DIAMOND_BLOCK");
        try {
            targetMaterial = Material.valueOf(targetMaterialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid material specified in the config. Using default: DIAMOND_BLOCK");
            targetMaterial = Material.DIAMOND_BLOCK;
        }
        levelCap = config.getInt("levelCap", 5);
        config.getInt("animationDuration", 5);
    }
    
    private void animateExperience(Player player) {
        final int[] totalExp = {0}; // Declare as final array to allow modification
    
        int expToAdd = 1;
        int ticksPerSecond = 20; // 20 ticks per second
    
        int animationSpeed = getConfig().getInt("animationSpeed", 5);
        int ticks = animationSpeed * ticksPerSecond;
    
        int maxLevel = getConfig().getInt("maxLevel", 5);
    
        animationTask = new BukkitRunnable() {
            int count = 0;
    
            @Override
            public void run() {
                if (count < ticks && player.getLevel() < maxLevel) {
                    if (totalExp[0] < maxLevel * player.getExpToLevel()) {
                        totalExp[0] += expToAdd;
                        player.giveExp(expToAdd);
                    }
                    count++;
                } else {
                    player.setLevel(0);
                    player.setExp(0);
                    animationTask = null; // Reset animation task
                    cancel();
                    long endTimeMillis = System.currentTimeMillis(); // Get end time
                    long durationSeconds = (endTimeMillis - startTimeMillis) / 1000; // Calculate duration in seconds
                    getLogger().info("Animation completed in " + durationSeconds + " seconds."); // Output duration to console
                }
            }
        };
        animationTask.runTaskTimer(this, 0L, 2L); // Run task every tick
    }
}