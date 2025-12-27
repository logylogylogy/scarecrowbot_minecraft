package com.example.scarecrowbot.manager;

import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages cooldowns for chat responses (global and per-player)
 */
@NoArgsConstructor
public class CooldownManager {

    private final Map<String, Long> globalCooldowns = new HashMap<>();
    private final Map<String, Map<UUID, Long>> playerCooldowns = new HashMap<>();

    /**
     * Check if a global cooldown is active
     *
     * @param key Cooldown identifier
     * @return True if on cooldown
     */
    public boolean isOnGlobalCooldown(final String key) {
        if (!this.globalCooldowns.containsKey(key)) {
            return false;
        }

        final long expirationTime = this.globalCooldowns.get(key);
        final long currentTime = System.currentTimeMillis();

        if (currentTime >= expirationTime) {
            this.globalCooldowns.remove(key);
            return false;
        }

        return true;
    }

    /**
     * Check if a player-specific cooldown is active
     *
     * @param playerUuid Player UUID
     * @param key Cooldown identifier
     * @return True if on cooldown
     */
    public boolean isOnPlayerCooldown(final UUID playerUuid, final String key) {
        if (!this.playerCooldowns.containsKey(key)) {
            return false;
        }

        final Map<UUID, Long> playerMap = this.playerCooldowns.get(key);
        if (!playerMap.containsKey(playerUuid)) {
            return false;
        }

        final long expirationTime = playerMap.get(playerUuid);
        final long currentTime = System.currentTimeMillis();

        if (currentTime >= expirationTime) {
            playerMap.remove(playerUuid);
            return false;
        }

        return true;
    }

    /**
     * Set a global cooldown
     *
     * @param key Cooldown identifier
     * @param seconds Duration in seconds
     */
    public void setGlobalCooldown(final String key, final int seconds) {
        final long expirationTime = System.currentTimeMillis() + (seconds * 1000L);
        this.globalCooldowns.put(key, expirationTime);
    }

    /**
     * Set a player-specific cooldown
     *
     * @param playerUuid Player UUID
     * @param key Cooldown identifier
     * @param seconds Duration in seconds
     */
    public void setPlayerCooldown(final UUID playerUuid, final String key, final int seconds) {
        final Map<UUID, Long> playerMap = this.playerCooldowns.computeIfAbsent(key, k -> new HashMap<>());
        final long expirationTime = System.currentTimeMillis() + (seconds * 1000L);
        playerMap.put(playerUuid, expirationTime);
    }

    /**
     * Clear all cooldowns (useful for reload)
     */
    public void clearAll() {
        this.globalCooldowns.clear();
        this.playerCooldowns.clear();
    }
}
