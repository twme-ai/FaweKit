package dev.twme.fawekit.command;

import dev.twme.fawekit.service.ShortcutService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

public final class ShortcutCommand implements CommandExecutor {
    private final ShortcutService shortcuts;

    public ShortcutCommand(ShortcutService shortcuts) {
        this.shortcuts = shortcuts;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof org.bukkit.entity.Player player)) {
            sender.sendMessage(ChatColor.RED + "This command requires an in-game player.");
            return true;
        }
        try {
            if (args.length == 0) throw new IllegalArgumentException("Usage: //sc <new|delete|move|list|search|history|export|name>");
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "new" -> shortcuts.put(player.getUniqueId(), required(args, 1), join(args, 2));
                case "delete" -> shortcuts.delete(player.getUniqueId(), required(args, 1));
                case "move" -> shortcuts.move(player.getUniqueId(), required(args, 1), required(args, 2));
                case "list", "search" -> list(player, args.length > 1 ? args[1] : "");
                case "history" -> history(player, args.length > 1 ? args[1] : "");
                case "export" -> player.sendMessage(ChatColor.LIGHT_PURPLE + "Exported to " + shortcuts.export(player.getUniqueId()));
                case "import" -> throw new IllegalArgumentException("URL import is disabled; review and place shortcuts in the exported YAML file.");
                default -> execute(player, args[0], Arrays.copyOfRange(args, 1, args.length));
            }
            if (!args[0].equalsIgnoreCase("list") && !args[0].equalsIgnoreCase("search")
                    && !args[0].equalsIgnoreCase("history") && !args[0].equalsIgnoreCase("export")) {
                player.sendMessage(ChatColor.LIGHT_PURPLE + "Shortcut operation completed.");
            }
        } catch (RuntimeException exception) {
            player.sendMessage(ChatColor.RED + exception.getMessage());
        }
        return true;
    }

    private void execute(org.bukkit.entity.Player player, String name, String[] arguments) {
        String expanded = shortcuts.command(player.getUniqueId(), name, arguments);
        String dispatch = expanded.startsWith("/") ? expanded.substring(1) : expanded;
        if (!Bukkit.dispatchCommand(player, dispatch)) throw new IllegalArgumentException("Expanded command was not recognized: " + expanded);
    }

    private void list(org.bukkit.entity.Player player, String filter) {
        String needle = filter.toLowerCase(Locale.ROOT);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Shortcuts:");
        shortcuts.shortcuts(player.getUniqueId()).entrySet().stream()
                .filter(entry -> entry.getKey().contains(needle) || entry.getValue().toLowerCase(Locale.ROOT).contains(needle))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> player.sendMessage(ChatColor.GRAY + " " + entry.getKey() + " = " + entry.getValue()));
    }

    private void history(org.bukkit.entity.Player player, String filter) {
        String needle = filter.toLowerCase(Locale.ROOT);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Command history:");
        shortcuts.history(player.getUniqueId()).stream().filter(value -> value.toLowerCase(Locale.ROOT).contains(needle))
                .limit(20).forEach(value -> player.sendMessage(ChatColor.GRAY + " " + value));
    }

    private static String join(String[] args, int start) {
        if (args.length <= start) throw new IllegalArgumentException("Missing shortcut text.");
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }

    private static String required(String[] args, int index) {
        if (args.length <= index) throw new IllegalArgumentException("Missing command argument.");
        return args[index];
    }
}
