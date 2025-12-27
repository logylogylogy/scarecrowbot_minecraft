package com.example.scarecrowbot.listener;

import com.example.scarecrowbot.ScarecrowBotPlugin;
import com.example.scarecrowbot.manager.CooldownManager;
import com.example.scarecrowbot.manager.ScarecrowManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Random;

/**
 * Handles player chat and bot responses
 */
public class ChatListener implements Listener {

    private final ScarecrowBotPlugin plugin;
    private final ScarecrowManager scarecrowManager;
    private final CooldownManager cooldownManager;
    private final Random random;

    public ChatListener() {
        this.plugin = ScarecrowBotPlugin.getInstance();
        this.scarecrowManager = this.plugin.getScarecrowManager();
        this.cooldownManager = this.plugin.getCooldownManager();
        this.random = new Random();
    }

    /**
     * Handle player chat events
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncChat(final AsyncChatEvent event) {
        final FileConfiguration config = this.plugin.getConfig();

        if (!config.getBoolean("bot.respondToChat", true)) {
            return;
        }

        final Player player = event.getPlayer();
        final Component messageComponent = event.message();
        final String message = PlainTextComponentSerializer.plainText().serialize(messageComponent).toLowerCase();

        // Try keyword replies first
        final String keywordReply = this.checkKeywordReplies(player, message);
        if (keywordReply != null) {
            this.broadcastBotMessage(keywordReply);
            return;
        }

        // Try random replies
        final String randomReply = this.checkRandomReplies(player);
        if (randomReply != null) {
            this.broadcastBotMessage(randomReply);
        }
    }

    /**
     * Check if message triggers keyword replies
     *
     * @param player Player who sent the message
     * @param message Message content (lowercase)
     * @return Reply message or null
     */
    private String checkKeywordReplies(final Player player, final String message) {
        final FileConfiguration config = this.plugin.getConfig();

        if (!config.getBoolean("bot.keywordReplies.enabled", true)) {
            return null;
        }

        final int globalCooldown = config.getInt("bot.keywordReplies.cooldownSecondsGlobal", 2);
        final int playerCooldown = config.getInt("bot.keywordReplies.cooldownSecondsPerPlayer", 6);

        // Check global cooldown
        if (this.cooldownManager.isOnGlobalCooldown("keyword")) {
            return null;
        }

        // Check player cooldown
        if (this.cooldownManager.isOnPlayerCooldown(player.getUniqueId(), "keyword")) {
            return null;
        }

        final List<?> rules = config.getList("bot.keywordReplies.rules");
        if (rules == null || rules.isEmpty()) {
            return null;
        }

        for (final Object ruleObj : rules) {
            if (!(ruleObj instanceof ConfigurationSection rule)) {
                continue;
            }

            final List<String> keywords = rule.getStringList("keywords");
            final List<String> replies = rule.getStringList("replies");

            if (keywords.isEmpty() || replies.isEmpty()) {
                continue;
            }

            // Check if message contains any keyword
            boolean keywordFound = false;
            for (final String keyword : keywords) {
                if (message.contains(keyword.toLowerCase())) {
                    keywordFound = true;
                    break;
                }
            }

            if (keywordFound) {
                // Set cooldowns
                this.cooldownManager.setGlobalCooldown("keyword", globalCooldown);
                this.cooldownManager.setPlayerCooldown(player.getUniqueId(), "keyword", playerCooldown);

                // Return random reply from this rule
                return replies.get(this.random.nextInt(replies.size()));
            }
        }

        return null;
    }

    /**
     * Check if bot should send random reply
     *
     * @param player Player who sent the message
     * @return Reply message or null
     */
    private String checkRandomReplies(final Player player) {
        final FileConfiguration config = this.plugin.getConfig();

        if (!config.getBoolean("bot.randomReply.enabled", true)) {
            return null;
        }

        final int chancePercent = config.getInt("bot.randomReply.chancePercent", 10);
        final int globalCooldown = config.getInt("bot.randomReply.cooldownSecondsGlobal", 3);
        final int playerCooldown = config.getInt("bot.randomReply.cooldownSecondsPerPlayer", 8);

        // Check global cooldown
        if (this.cooldownManager.isOnGlobalCooldown("random")) {
            return null;
        }

        // Check player cooldown
        if (this.cooldownManager.isOnPlayerCooldown(player.getUniqueId(), "random")) {
            return null;
        }

        // Roll chance
        final int roll = this.random.nextInt(100);
        if (roll >= chancePercent) {
            return null;
        }

        // Get random message
        final List<String> messages = config.getStringList("bot.randomReply.messages");
        if (messages.isEmpty()) {
            return null;
        }

        // Set cooldowns
        this.cooldownManager.setGlobalCooldown("random", globalCooldown);
        this.cooldownManager.setPlayerCooldown(player.getUniqueId(), "random", playerCooldown);

        return messages.get(this.random.nextInt(messages.size()));
    }

    /**
     * Broadcast a bot message (must be called from sync context)
     *
     * @param message Message to broadcast
     */
    private void broadcastBotMessage(final String message) {
        // Schedule sync task since AsyncChatEvent is async
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            final FileConfiguration config = this.plugin.getConfig();
            final String botName = this.scarecrowManager.getBotName();
            final String chatFormat = config.getString("bot.chatFormat", "<{botName}> {message}");

            final String formattedMessage = chatFormat
                    .replace("{botName}", botName)
                    .replace("{message}", message);

            Bukkit.broadcast(Component.text(formattedMessage));
        });
    }
}
