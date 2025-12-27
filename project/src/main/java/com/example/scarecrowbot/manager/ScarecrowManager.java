package com.example.scarecrowbot.manager;

import com.example.scarecrowbot.ScarecrowBotPlugin;
import com.example.scarecrowbot.util.PDCKeys;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.UUID;

/**
 * Manages the scarecrow entity lifecycle, persistence, and behavior
 */
public class ScarecrowManager {

    private static final DecimalFormat HP_FORMAT = new DecimalFormat("0.0");

    private final ScarecrowBotPlugin plugin;
    private final File dataFile;

    @Getter
    private LivingEntity scarecrowEntity;

    @Getter
    private Location spawnLocation;

    private BukkitTask positionLockTask;

    public ScarecrowManager() {
        this.plugin = ScarecrowBotPlugin.getInstance();
        this.dataFile = new File(this.plugin.getDataFolder(), "data.yml");
    }

    /**
     * Create a new scarecrow at the specified location
     *
     * @param location Spawn location
     * @param botName  Custom name for the bot
     * @return True if created successfully
     */
    public boolean createScarecrow(final Location location, final String botName) {
        if (this.scarecrowEntity != null && !this.scarecrowEntity.isDead()) {
            return false; // Already exists
        }

        final FileConfiguration config = this.plugin.getConfig();
        final String entityTypeString = config.getString("scarecrow.entityType", "VILLAGER").toUpperCase();
        final EntityType entityType;

        try {
            entityType = EntityType.valueOf(entityTypeString);
        } catch (final IllegalArgumentException exception) {
            this.plugin.getLogger().warning("Invalid entity type in config: " + entityTypeString + ", using VILLAGER");
            return this.createScarecrowWithType(location, botName, EntityType.VILLAGER);
        }

        return this.createScarecrowWithType(location, botName, entityType);
    }

    private boolean createScarecrowWithType(final Location location, final String botName, final EntityType entityType) {
        final World world = location.getWorld();
        if (world == null) {
            return false;
        }

        final Entity entity = world.spawnEntity(location, entityType);
        if (!(entity instanceof LivingEntity livingEntity)) {
            entity.remove();
            return false;
        }

        this.scarecrowEntity = livingEntity;
        this.spawnLocation = location.clone();

        // Configure entity
        final FileConfiguration config = this.plugin.getConfig();
        this.scarecrowEntity.setAI(false);
        this.scarecrowEntity.setPersistent(true);
        this.scarecrowEntity.setRemoveWhenFarAway(false);
        this.scarecrowEntity.setSilent(config.getBoolean("scarecrow.silent", true));
        this.scarecrowEntity.setInvulnerable(config.getBoolean("scarecrow.invulnerable", false));
        this.scarecrowEntity.setCollidable(true);

        // Set max health
        final double maxHp = config.getDouble("scarecrow.maxHP", 100.0);
        final AttributeInstance maxHealthAttribute = this.scarecrowEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttribute != null) {
            maxHealthAttribute.setBaseValue(maxHp);
        }
        this.scarecrowEntity.setHealth(maxHp);

        // Store data in PDC
        final PersistentDataContainer pdc = this.scarecrowEntity.getPersistentDataContainer();
        pdc.set(PDCKeys.SCARECROW_MARKER, PersistentDataType.BOOLEAN, true);
        pdc.set(PDCKeys.SCARECROW_HP, PersistentDataType.DOUBLE, maxHp);
        pdc.set(PDCKeys.SCARECROW_MAX_HP, PersistentDataType.DOUBLE, maxHp);
        pdc.set(PDCKeys.SCARECROW_NAME, PersistentDataType.STRING, botName);

        // Update name display
        this.updateNameDisplay();

        // Save to file
        this.saveToFile();

        this.plugin.getLogger().info("Scarecrow created at " + this.formatLocation(location) + " with name: " + botName);

        return true;
    }

    /**
     * Remove the scarecrow entity
     */
    public void removeScarecrow() {
        if (this.scarecrowEntity != null && !this.scarecrowEntity.isDead()) {
            this.scarecrowEntity.remove();
        }

        this.scarecrowEntity = null;
        this.spawnLocation = null;

        // Delete data file
        if (this.dataFile.exists()) {
            this.dataFile.delete();
        }

        this.plugin.getLogger().info("Scarecrow removed");
    }

    /**
     * Move scarecrow to a new location
     *
     * @param newLocation New location
     */
    public void moveScarecrow(final Location newLocation) {
        if (this.scarecrowEntity == null || this.scarecrowEntity.isDead()) {
            return;
        }

        this.scarecrowEntity.teleport(newLocation);
        this.spawnLocation = newLocation.clone();
        this.saveToFile();
    }

    /**
     * Check if an entity is the scarecrow
     *
     * @param entity Entity to check
     * @return True if entity is the scarecrow
     */
    public boolean isScarecrow(final Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }

        final PersistentDataContainer pdc = entity.getPersistentDataContainer();
        return pdc.has(PDCKeys.SCARECROW_MARKER, PersistentDataType.BOOLEAN);
    }

    /**
     * Get current HP of the scarecrow
     *
     * @return Current HP, or 0 if not exists
     */
    public double getCurrentHp() {
        if (this.scarecrowEntity == null || this.scarecrowEntity.isDead()) {
            return 0.0;
        }

        final PersistentDataContainer pdc = this.scarecrowEntity.getPersistentDataContainer();
        return pdc.getOrDefault(PDCKeys.SCARECROW_HP, PersistentDataType.DOUBLE, 0.0);
    }

    /**
     * Get maximum HP of the scarecrow
     *
     * @return Maximum HP, or 0 if not exists
     */
    public double getMaxHp() {
        if (this.scarecrowEntity == null || this.scarecrowEntity.isDead()) {
            return 0.0;
        }

        final PersistentDataContainer pdc = this.scarecrowEntity.getPersistentDataContainer();
        return pdc.getOrDefault(PDCKeys.SCARECROW_MAX_HP, PersistentDataType.DOUBLE, 100.0);
    }

    /**
     * Set HP of the scarecrow
     *
     * @param hp New HP value
     */
    public void setHp(final double hp) {
        if (this.scarecrowEntity == null || this.scarecrowEntity.isDead()) {
            return;
        }

        final FileConfiguration config = this.plugin.getConfig();
        final double minHp = config.getDouble("scarecrow.minHP", 1.0);
        final double maxHp = this.getMaxHp();
        final double clampedHp = Math.max(minHp, Math.min(maxHp, hp));

        final PersistentDataContainer pdc = this.scarecrowEntity.getPersistentDataContainer();
        pdc.set(PDCKeys.SCARECROW_HP, PersistentDataType.DOUBLE, clampedHp);

        // Update entity health attribute
        final double healthRatio = clampedHp / maxHp;
        final AttributeInstance maxHealthAttribute = this.scarecrowEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttribute != null) {
            final double entityMaxHealth = maxHealthAttribute.getValue();
            this.scarecrowEntity.setHealth(Math.max(0.1, healthRatio * entityMaxHealth));
        }

        this.updateNameDisplay();
    }

    /**
     * Heal the scarecrow
     *
     * @param amount Amount to heal
     */
    public void heal(final double amount) {
        final double currentHp = this.getCurrentHp();
        final double newHp = currentHp + amount;
        this.setHp(newHp);
    }

    /**
     * Damage the scarecrow
     *
     * @param amount Amount of damage
     */
    public void damage(final double amount) {
        final double currentHp = this.getCurrentHp();
        final double newHp = currentHp - amount;
        this.setHp(newHp);

        // Play effects
        this.playDamageEffects();
    }

    /**
     * Play damage effects (sound and particles)
     */
    private void playDamageEffects() {
        if (this.scarecrowEntity == null || this.scarecrowEntity.isDead()) {
            return;
        }

        final FileConfiguration config = this.plugin.getConfig();
        final Location location = this.scarecrowEntity.getLocation();
        final World world = location.getWorld();

        if (world == null) {
            return;
        }

        if (config.getBoolean("scarecrow.hurtSound", true)) {
            world.playSound(location, Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
        }

        if (config.getBoolean("scarecrow.damageParticles", true)) {
            world.spawnParticle(Particle.DAMAGE_INDICATOR, location.add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0.1);
        }
    }

    /**
     * Update the custom name display with HP
     */
    public void updateNameDisplay() {
        if (this.scarecrowEntity == null || this.scarecrowEntity.isDead()) {
            return;
        }

        final FileConfiguration config = this.plugin.getConfig();
        final PersistentDataContainer pdc = this.scarecrowEntity.getPersistentDataContainer();
        final String botName = pdc.getOrDefault(PDCKeys.SCARECROW_NAME, PersistentDataType.STRING, "Scarecrow");

        if (!config.getBoolean("scarecrow.visibleName", true)) {
            this.scarecrowEntity.setCustomNameVisible(false);
            return;
        }

        this.scarecrowEntity.setCustomNameVisible(true);

        if (config.getBoolean("scarecrow.showHpInName", true)) {
            final double currentHp = this.getCurrentHp();
            final double maxHp = this.getMaxHp();
            final String nameFormat = config.getString("scarecrow.nameHpFormat", "{botName} §c[HP {hp}/{maxHp}]");
            final String displayName = nameFormat
                    .replace("{botName}", botName)
                    .replace("{hp}", HP_FORMAT.format(currentHp))
                    .replace("{maxHp}", HP_FORMAT.format(maxHp));

            this.scarecrowEntity.customName(Component.text(displayName));
        } else {
            this.scarecrowEntity.customName(Component.text(botName));
        }
    }

    /**
     * Get the bot name
     *
     * @return Bot name
     */
    public String getBotName() {
        if (this.scarecrowEntity != null && !this.scarecrowEntity.isDead()) {
            final PersistentDataContainer pdc = this.scarecrowEntity.getPersistentDataContainer();
            return pdc.getOrDefault(PDCKeys.SCARECROW_NAME, PersistentDataType.STRING, "Scarecrow");
        }

        return this.plugin.getConfig().getString("bot.name", "Scarecrow");
    }

    /**
     * Start the position lock task to keep scarecrow in place
     */
    public void startPositionLockTask() {
        final FileConfiguration config = this.plugin.getConfig();
        if (!config.getBoolean("scarecrow.lockToGround", true)) {
            return;
        }

        if (this.positionLockTask != null) {
            return; // Already running
        }

        this.positionLockTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            if (this.scarecrowEntity == null || this.scarecrowEntity.isDead() || this.spawnLocation == null) {
                return;
            }

            final Location currentLocation = this.scarecrowEntity.getLocation();
            final double distance = currentLocation.distance(this.spawnLocation);

            if (distance > 0.1) {
                this.scarecrowEntity.teleport(this.spawnLocation);
                this.scarecrowEntity.setVelocity(this.scarecrowEntity.getVelocity().zero());
            }
        }, 5L, 5L); // Run every 5 ticks (0.25 seconds)
    }

    /**
     * Stop the position lock task
     */
    public void stopPositionLockTask() {
        if (this.positionLockTask != null) {
            this.positionLockTask.cancel();
            this.positionLockTask = null;
        }
    }

    /**
     * Save scarecrow data to file
     */
    public void saveToFile() {
        final FileConfiguration data = new YamlConfiguration();

        if (this.scarecrowEntity == null || this.scarecrowEntity.isDead()) {
            data.set("scarecrow.exists", false);
        } else {
            data.set("scarecrow.exists", true);
            data.set("scarecrow.uuid", this.scarecrowEntity.getUniqueId().toString());
            data.set("scarecrow.world", this.spawnLocation.getWorld().getName());
            data.set("scarecrow.x", this.spawnLocation.getX());
            data.set("scarecrow.y", this.spawnLocation.getY());
            data.set("scarecrow.z", this.spawnLocation.getZ());
            data.set("scarecrow.yaw", this.spawnLocation.getYaw());
            data.set("scarecrow.pitch", this.spawnLocation.getPitch());
            data.set("scarecrow.hp", this.getCurrentHp());
            data.set("scarecrow.name", this.getBotName());
        }

        try {
            data.save(this.dataFile);
        } catch (final IOException exception) {
            this.plugin.getLogger().severe("Failed to save scarecrow data: " + exception.getMessage());
        }
    }

    /**
     * Load scarecrow data from file
     */
    public void loadFromFile() {
        if (!this.dataFile.exists()) {
            this.plugin.getLogger().info("No scarecrow data file found");
            return;
        }

        final FileConfiguration data = YamlConfiguration.loadConfiguration(this.dataFile);

        if (!data.getBoolean("scarecrow.exists", false)) {
            this.plugin.getLogger().info("No scarecrow exists in data file");
            return;
        }

        final String uuidString = data.getString("scarecrow.uuid");
        if (uuidString == null) {
            this.plugin.getLogger().warning("Invalid UUID in data file");
            return;
        }

        final UUID entityUuid = UUID.fromString(uuidString);
        final String worldName = data.getString("scarecrow.world");
        final World world = Bukkit.getWorld(worldName);

        if (world == null) {
            this.plugin.getLogger().warning("World not found: " + worldName);
            return;
        }

        // Try to find existing entity
        final Entity entity = Bukkit.getEntity(entityUuid);
        if (entity instanceof LivingEntity livingEntity && !entity.isDead()) {
            this.scarecrowEntity = livingEntity;

            // Restore spawn location
            final double x = data.getDouble("scarecrow.x");
            final double y = data.getDouble("scarecrow.y");
            final double z = data.getDouble("scarecrow.z");
            final float yaw = (float) data.getDouble("scarecrow.yaw");
            final float pitch = (float) data.getDouble("scarecrow.pitch");
            this.spawnLocation = new Location(world, x, y, z, yaw, pitch);

            // Restore HP
            final double savedHp = data.getDouble("scarecrow.hp", 100.0);
            this.setHp(savedHp);

            // Teleport to spawn location in case it moved
            this.scarecrowEntity.teleport(this.spawnLocation);

            this.plugin.getLogger().info("Loaded existing scarecrow entity: " + this.getBotName());
        } else {
            this.plugin.getLogger().warning("Scarecrow entity not found (may have been unloaded). Use /scarecrow create to spawn a new one.");
        }
    }

    private String formatLocation(final Location location) {
        return String.format("%.1f, %.1f, %.1f in %s",
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getWorld() != null ? location.getWorld().getName() : "unknown");
    }
}
