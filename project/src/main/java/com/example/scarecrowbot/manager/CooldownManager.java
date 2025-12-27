package com.example.scarecrowbot.manager;

import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages cooldowns for chat responses (global and per-player)
 */
@NoArgsConstructor
public class CooldownManager {

    private final Map<String, Long> globalCooldowns = new ConcurrentHashMap<>();
    private final Map<String, Map<UUID, Long>> playerCooldowns = new ConcurrentHashMap<>();

    /**
     * Check if a global cooldown is active
     *
     * @param key Cooldown identifier
     * @return True if on cooldown
     */
    public boolean isOnGlobalCooldown(final String key) {
        final Long expirationTime = this.globalCooldowns.get(key);
        if (expirationTime == null) {
            return false;
        }
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
        final Map<UUID, Long> playerMap = this.playerCooldowns.get(key);
        if (playerMap == null) {
            return false;
        }

        final Long expirationTime = playerMap.get(playerUuid);
        if (expirationTime == null) {
            return false;
        }
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
        final Map<UUID, Long> playerMap = this.playerCooldowns.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
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
