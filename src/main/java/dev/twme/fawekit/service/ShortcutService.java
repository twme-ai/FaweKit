package dev.twme.fawekit.service;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ShortcutService {
    private static final Pattern NAME = Pattern.compile("#?[A-Za-z0-9._-]{1,64}");
    private static final Pattern PARAMETER = Pattern.compile("\\$\\{(\\d+|@)(?::([-+?])([^}]*))?}");
    private static final Pattern HASH_TOKEN = Pattern.compile("(?<![A-Za-z0-9_])#[A-Za-z0-9._-]+");
    private final Path directory;
    private final Map<UUID, Data> cache = new ConcurrentHashMap<>();

    public ShortcutService(Path dataFolder) {
        this.directory = dataFolder.resolve("shortcuts");
    }

    public Map<String, String> shortcuts(UUID playerId) {
        return Map.copyOf(data(playerId).shortcuts);
    }

    public List<String> history(UUID playerId) {
        return List.copyOf(data(playerId).history);
    }

    public void put(UUID playerId, String name, String template) {
        validateName(name);
        if (template.isBlank()) throw new IllegalArgumentException("Shortcut text cannot be empty.");
        Data data = data(playerId);
        data.shortcuts.put(name.toLowerCase(Locale.ROOT), template);
        save(playerId, data);
    }

    public void delete(UUID playerId, String name) {
        Data data = data(playerId);
        if (data.shortcuts.remove(name.toLowerCase(Locale.ROOT)) == null) {
            throw new IllegalArgumentException("Unknown shortcut: " + name);
        }
        save(playerId, data);
    }

    public void move(UUID playerId, String oldName, String newName) {
        validateName(newName);
        Data data = data(playerId);
        String value = data.shortcuts.remove(oldName.toLowerCase(Locale.ROOT));
        if (value == null) throw new IllegalArgumentException("Unknown shortcut: " + oldName);
        data.shortcuts.put(newName.toLowerCase(Locale.ROOT), value);
        save(playerId, data);
    }

    public String command(UUID playerId, String name, String[] arguments) {
        String template = data(playerId).shortcuts.get(name.toLowerCase(Locale.ROOT));
        if (template == null || name.startsWith("#")) throw new IllegalArgumentException("Unknown command shortcut: " + name);
        return parameters(template, arguments);
    }

    public String expandHashShortcuts(UUID playerId, String command) {
        String result = command;
        Map<String, String> shortcuts = data(playerId).shortcuts;
        for (int depth = 0; depth < 5; depth++) {
            Matcher matcher = HASH_TOKEN.matcher(result);
            StringBuffer expanded = new StringBuffer();
            boolean changed = false;
            while (matcher.find()) {
                String replacement = shortcuts.get(matcher.group().toLowerCase(Locale.ROOT));
                if (replacement == null) continue;
                matcher.appendReplacement(expanded, Matcher.quoteReplacement(replacement));
                changed = true;
            }
            if (!changed) return result;
            matcher.appendTail(expanded);
            result = expanded.toString();
        }
        throw new IllegalArgumentException("Shortcut expansion is recursive beyond five levels.");
    }

    public void record(UUID playerId, String command) {
        Data data = data(playerId);
        if (!data.history.isEmpty() && data.history.peekFirst().equals(command)) return;
        data.history.addFirst(command);
        while (data.history.size() > 200) data.history.removeLast();
        save(playerId, data);
    }

    public Path export(UUID playerId) {
        Data data = data(playerId);
        Path exports = directory.resolve("exports");
        try {
            Files.createDirectories(exports);
            Path target = exports.resolve(playerId + ".yml");
            YamlConfiguration yaml = new YamlConfiguration();
            data.shortcuts.forEach((name, value) -> yaml.set("shortcuts." + name, value));
            yaml.save(target.toFile());
            return target;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not export shortcuts: " + exception.getMessage(), exception);
        }
    }

    static String parameters(String template, String[] arguments) {
        Matcher matcher = PARAMETER.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = key.equals("@") ? String.join(" ", arguments) : argument(arguments, Integer.parseInt(key));
            String operator = matcher.group(2);
            String operand = matcher.group(3);
            if (operator != null) {
                value = switch (operator) {
                    case "-" -> value.isEmpty() ? operand : value;
                    case "+" -> value.isEmpty() ? "" : operand;
                    case "?" -> {
                        if (value.isEmpty()) throw new IllegalArgumentException(operand.isEmpty() ? "Missing shortcut argument." : operand);
                        yield value;
                    }
                    default -> value;
                };
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private Data data(UUID playerId) {
        return cache.computeIfAbsent(playerId, this::load);
    }

    private Data load(UUID playerId) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file(playerId).toFile());
        Data data = new Data();
        var section = yaml.getConfigurationSection("shortcuts");
        if (section != null) section.getKeys(false).forEach(name -> data.shortcuts.put(name, section.getString(name, "")));
        data.history.addAll(yaml.getStringList("history"));
        return data;
    }

    private void save(UUID playerId, Data data) {
        try {
            Files.createDirectories(directory);
            YamlConfiguration yaml = new YamlConfiguration();
            data.shortcuts.forEach((name, value) -> yaml.set("shortcuts." + name, value));
            yaml.set("history", new ArrayList<>(data.history));
            yaml.save(file(playerId).toFile());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save shortcuts: " + exception.getMessage(), exception);
        }
    }

    private Path file(UUID playerId) {
        return directory.resolve(playerId + ".yml");
    }

    private static String argument(String[] arguments, int oneBased) {
        return oneBased > 0 && oneBased <= arguments.length ? arguments[oneBased - 1] : "";
    }

    private static void validateName(String name) {
        if (!NAME.matcher(name).matches()) throw new IllegalArgumentException("Invalid shortcut name.");
    }

    private static final class Data {
        private final Map<String, String> shortcuts = new LinkedHashMap<>();
        private final Deque<String> history = new ArrayDeque<>();
    }
}
