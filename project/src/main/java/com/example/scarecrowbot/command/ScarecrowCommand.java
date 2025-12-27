package com.example.scarecrowbot.command;

import com.example.scarecrowbot.ScarecrowBotPlugin;
import com.example.scarecrowbot.manager.ScarecrowManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Main command handler for /scarecrow
 */
public class ScarecrowCommand implements CommandExecutor {

    private final ScarecrowBotPlugin plugin;
    private final ScarecrowManager scarecrowManager;

    public ScarecrowCommand() {
        this.plugin = ScarecrowBotPlugin.getInstance();
        this.scarecrowManager = this.plugin.getScarecrowManager();
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command,
                             @NotNull final String label, @NotNull final String[] args) {
        if (args.length == 0) {
            this.sendUsage(sender);
            return true;
        }

        final String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create" -> {
                return this.handleCreate(sender, args);
            }
            case "remove" -> {
                return this.handleRemove(sender);
            }
            case "move" -> {
                return this.handleMove(sender);
            }
            case "tp", "teleport" -> {
                return this.handleTeleport(sender);
            }
            case "heal" -> {
                return this.handleHeal(sender, args);
            }
            case "damage" -> {
                return this.handleDamage(sender, args);
            }
            case "status" -> {
                return this.handleStatus(sender);
            }
            case "say" -> {
                return this.handleSay(sender, args);
            }
            case "togglechat" -> {
                return this.handleToggleChat(sender);
            }
            default -> {
                this.sendUsage(sender);
                return true;
            }
        }
    }

    private boolean handleCreate(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("scarecrow.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        if (this.scarecrowManager.getScarecrowEntity() != null && !this.scarecrowManager.getScarecrowEntity().isDead()) {
            sender.sendMessage(Component.text("A scarecrow already exists! Remove it first with /scarecrow remove", NamedTextColor.RED));
            return true;
        }

        final String botName = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))
                : this.plugin.getConfig().getString("bot.name", "Scarecrow");

        final Location location = player.getLocation();
        final boolean success = this.scarecrowManager.createScarecrow(location, botName);

        if (success) {
            sender.sendMessage(Component.text("Scarecrow created: " + botName, NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Failed to create scarecrow.", NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleRemove(final CommandSender sender) {
        if (!sender.hasPermission("scarecrow.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (this.scarecrowManager.getScarecrowEntity() == null || this.scarecrowManager.getScarecrowEntity().isDead()) {
            sender.sendMessage(Component.text("No scarecrow exists.", NamedTextColor.RED));
            return true;
        }

        this.scarecrowManager.removeScarecrow();
        sender.sendMessage(Component.text("Scarecrow removed.", NamedTextColor.GREEN));

        return true;
    }

    private boolean handleMove(final CommandSender sender) {
        if (!sender.hasPermission("scarecrow.manage")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        if (this.scarecrowManager.getScarecrowEntity() == null || this.scarecrowManager.getScarecrowEntity().isDead()) {
            sender.sendMessage(Component.text("No scarecrow exists.", NamedTextColor.RED));
            return true;
        }

        final Location location = player.getLocation();
        this.scarecrowManager.moveScarecrow(location);
        sender.sendMessage(Component.text("Scarecrow moved to your location.", NamedTextColor.GREEN));

        return true;
    }

    private boolean handleTeleport(final CommandSender sender) {
        if (!sender.hasPermission("scarecrow.manage")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        final LivingEntity scarecrow = this.scarecrowManager.getScarecrowEntity();
        if (scarecrow == null || scarecrow.isDead()) {
            sender.sendMessage(Component.text("No scarecrow exists.", NamedTextColor.RED));
            return true;
        }

        player.teleport(scarecrow.getLocation());
        sender.sendMessage(Component.text("Teleported to scarecrow.", NamedTextColor.GREEN));

        return true;
    }

    private boolean handleHeal(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("scarecrow.manage")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (this.scarecrowManager.getScarecrowEntity() == null || this.scarecrowManager.getScarecrowEntity().isDead()) {
            sender.sendMessage(Component.text("No scarecrow exists.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /scarecrow heal <amount>", NamedTextColor.RED));
            return true;
        }

        try {
            final double amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                sender.sendMessage(Component.text("Amount must be positive.", NamedTextColor.RED));
                return true;
            }

            final double oldHp = this.scarecrowManager.getCurrentHp();
            this.scarecrowManager.heal(amount);
            final double newHp = this.scarecrowManager.getCurrentHp();

            sender.sendMessage(Component.text(String.format("Scarecrow healed: %.1f → %.1f HP", oldHp, newHp), NamedTextColor.GREEN));
        } catch (final NumberFormatException exception) {
            sender.sendMessage(Component.text("Invalid number: " + args[1], NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleDamage(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("scarecrow.manage")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (this.scarecrowManager.getScarecrowEntity() == null || this.scarecrowManager.getScarecrowEntity().isDead()) {
            sender.sendMessage(Component.text("No scarecrow exists.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /scarecrow damage <amount>", NamedTextColor.RED));
            return true;
        }

        try {
            final double amount = Double.parseDouble(args[1]);
            if (amount <= 0) {
                sender.sendMessage(Component.text("Amount must be positive.", NamedTextColor.RED));
                return true;
            }

            final double oldHp = this.scarecrowManager.getCurrentHp();
            this.scarecrowManager.damage(amount);
            final double newHp = this.scarecrowManager.getCurrentHp();

            sender.sendMessage(Component.text(String.format("Scarecrow damaged: %.1f → %.1f HP", oldHp, newHp), NamedTextColor.GREEN));
        } catch (final NumberFormatException exception) {
            sender.sendMessage(Component.text("Invalid number: " + args[1], NamedTextColor.RED));
        }

        return true;
    }

    private boolean handleStatus(final CommandSender sender) {
        if (!sender.hasPermission("scarecrow.manage")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        final LivingEntity scarecrow = this.scarecrowManager.getScarecrowEntity();
        if (scarecrow == null || scarecrow.isDead()) {
            sender.sendMessage(Component.text("No scarecrow exists.", NamedTextColor.RED));
            return true;
        }

        final String botName = this.scarecrowManager.getBotName();
        final double currentHp = this.scarecrowManager.getCurrentHp();
        final double maxHp = this.scarecrowManager.getMaxHp();
        final Location location = this.scarecrowManager.getSpawnLocation();

        sender.sendMessage(Component.text("=== Scarecrow Status ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Name: " + botName, NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(String.format("HP: %.1f / %.1f", currentHp, maxHp), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(String.format("Location: %.1f, %.1f, %.1f in %s",
                location.getX(), location.getY(), location.getZ(),
                location.getWorld() != null ? location.getWorld().getName() : "unknown"), NamedTextColor.YELLOW));

        return true;
    }

    private boolean handleSay(final CommandSender sender, final String[] args) {
        if (!sender.hasPermission("scarecrow.say")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /scarecrow say <message>", NamedTextColor.RED));
            return true;
        }

        final String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        final FileConfiguration config = this.plugin.getConfig();
        final String botName = this.scarecrowManager.getBotName();
        final String chatFormat = config.getString("bot.chatFormat", "<{botName}> {message}");

        final String formattedMessage = chatFormat
                .replace("{botName}", botName)
                .replace("{message}", message);

        Bukkit.broadcast(Component.text(formattedMessage));

        return true;
    }

    private boolean handleToggleChat(final CommandSender sender) {
        if (!sender.hasPermission("scarecrow.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        final FileConfiguration config = this.plugin.getConfig();
        final boolean currentValue = config.getBoolean("bot.respondToChat", true);
        final boolean newValue = !currentValue;

        config.set("bot.respondToChat", newValue);
        this.plugin.saveConfig();

        sender.sendMessage(Component.text("Chat responses " + (newValue ? "enabled" : "disabled"), NamedTextColor.GREEN));

        return true;
    }

    private void sendUsage(final CommandSender sender) {
        sender.sendMessage(Component.text("=== Scarecrow Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/scarecrow create [name] - Create scarecrow", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/scarecrow remove - Remove scarecrow", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/scarecrow move - Move to your location", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/scarecrow tp - Teleport to scarecrow", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/scarecrow heal <amount> - Heal scarecrow", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/scarecrow damage <amount> - Damage scarecrow", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/scarecrow status - Show status", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/scarecrow say <message> - Make scarecrow speak", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/scarecrow togglechat - Toggle chat responses", NamedTextColor.YELLOW));
    }
}
