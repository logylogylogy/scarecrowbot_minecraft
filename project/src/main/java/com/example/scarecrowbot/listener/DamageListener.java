package com.example.scarecrowbot.listener;

import com.example.scarecrowbot.ScarecrowBotPlugin;
import com.example.scarecrowbot.manager.ScarecrowManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Handles damage and death events for the scarecrow
 */
public class DamageListener implements Listener {

    private final ScarecrowBotPlugin plugin;
    private final ScarecrowManager scarecrowManager;

    public DamageListener() {
        this.plugin = ScarecrowBotPlugin.getInstance();
        this.scarecrowManager = this.plugin.getScarecrowManager();
    }

    /**
     * Handle entity damage events
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(final EntityDamageEvent event) {
        final Entity entity = event.getEntity();

        if (!this.scarecrowManager.isScarecrow(entity)) {
            return;
        }

        final FileConfiguration config = this.plugin.getConfig();

        // If invulnerable in config, cancel all damage
        if (config.getBoolean("scarecrow.invulnerable", false)) {
            event.setCancelled(true);
            return;
        }

        final double currentHp = this.scarecrowManager.getCurrentHp();
        final double damage = event.getFinalDamage();
        final double newHp = currentHp - damage;
        final double minHp = config.getDouble("scarecrow.minHP", 1.0);

        // Prevent death by keeping HP at minHP
        if (newHp <= minHp) {
            this.scarecrowManager.setHp(minHp);
            event.setCancelled(true); // Cancel event to prevent vanilla death

            // Log low HP event
            this.plugin.getLogger().info("Scarecrow reached minimum HP (" + minHp + ")");
        } else {
            // Apply damage normally
            this.scarecrowManager.damage(damage);
            event.setDamage(0); // Set damage to 0 since we handle it manually via PDC
        }
    }

    /**
     * Handle entity death events (should never happen, but extra safety)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(final EntityDeathEvent event) {
        final Entity entity = event.getEntity();

        if (!this.scarecrowManager.isScarecrow(entity)) {
            return;
        }

        // Cancel all drops and XP
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Log unexpected death
        this.plugin.getLogger().warning("Scarecrow death event triggered (should not happen!)");

        // Restore HP to minHP
        final FileConfiguration config = this.plugin.getConfig();
        final double minHp = config.getDouble("scarecrow.minHP", 1.0);
        this.scarecrowManager.setHp(minHp);
    }
}
