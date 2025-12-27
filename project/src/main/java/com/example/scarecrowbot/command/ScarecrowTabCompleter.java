package com.example.scarecrowbot.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tab completion for /scarecrow command
 */
public class ScarecrowTabCompleter implements TabCompleter {

    private static final List<String> SUB_COMMANDS = Arrays.asList(
            "create", "remove", "move", "tp", "heal", "damage", "status", "say", "togglechat"
    );

    private static final List<String> ENTITY_TYPES = Arrays.asList("VILLAGER", "ZOMBIE");

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command command,
                                      @NotNull final String label, @NotNull final String[] args) {
        final List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Complete subcommands
            final String input = args[0].toLowerCase();
            for (final String subCommand : SUB_COMMANDS) {
                if (subCommand.startsWith(input)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            final String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "create" -> {
                    // Suggest bot name
                    completions.add("<bot_name>");
                }
                case "heal", "damage" -> {
                    // Suggest amount
                    completions.addAll(Arrays.asList("10", "25", "50", "100"));
                }
                case "say" -> {
                    // Suggest message start
                    completions.add("<message>");
                }
            }
        } else if (args.length > 2 && args[0].equalsIgnoreCase("say")) {
            // Continue message suggestion
            completions.add("<message>");
        }

        return completions;
    }
}
