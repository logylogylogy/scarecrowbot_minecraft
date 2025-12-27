package com.example.scarecrowbot;

import com.example.scarecrowbot.command.ScarecrowCommand;
import com.example.scarecrowbot.command.ScarecrowTabCompleter;
import com.example.scarecrowbot.listener.ChatListener;
import com.example.scarecrowbot.listener.DamageListener;
import com.example.scarecrowbot.manager.CooldownManager;
import com.example.scarecrowbot.manager.ScarecrowManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for ScarecrowBot
 */
public final class ScarecrowBotPlugin extends JavaPlugin {

    @Getter
    private static ScarecrowBotPlugin instance;

    @Getter
    private ScarecrowManager scarecrowManager;

    @Getter
    private CooldownManager cooldownManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        this.saveDefaultConfig();

        // Initialize managers
        this.cooldownManager = new CooldownManager();
        this.scarecrowManager = new ScarecrowManager();

        // Load scarecrow data
        this.scarecrowManager.loadFromFile();

        // Register listeners
        this.getServer().getPluginManager().registerEvents(new DamageListener(), this);
        this.getServer().getPluginManager().registerEvents(new ChatListener(), this);

        // Register command
        final ScarecrowCommand scarecrowCommand = new ScarecrowCommand();
        this.getCommand("scarecrow").setExecutor(scarecrowCommand);
        this.getCommand("scarecrow").setTabCompleter(new ScarecrowTabCompleter());

        // Start position lock task
        this.scarecrowManager.startPositionLockTask();

        this.getLogger().info("ScarecrowBot has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save scarecrow data
        if (this.scarecrowManager != null) {
            this.scarecrowManager.saveToFile();
            this.scarecrowManager.stopPositionLockTask();
        }

        this.getLogger().info("ScarecrowBot has been disabled!");
    }
}
