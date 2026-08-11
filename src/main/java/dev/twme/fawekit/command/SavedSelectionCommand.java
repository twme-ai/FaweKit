package dev.twme.fawekit.command;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.regions.Region;
import dev.twme.fawekit.selection.RegionCodec;
import dev.twme.fawekit.selection.RegionSelectors;
import dev.twme.fawekit.service.SelectionStackService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SavedSelectionCommand extends PlayerFaweCommand {
    private static final Pattern SAFE_NAME = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private final Path root;
    private final SelectionStackService selections;

    public SavedSelectionCommand(Path dataFolder, SelectionStackService selections) {
        this.root = dataFolder.resolve("selections");
        this.selections = selections;
    }

    @Override
    protected void execute(org.bukkit.entity.Player player, Player actor, String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("Usage: //ssel <save|load|list|search|move|delete|clear|formats>");
        }
        Path directory = root.resolve(player.getUniqueId().toString());
        Files.createDirectories(directory);
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "save" -> save(player, actor, args, directory);
            case "load" -> load(player, actor, required(args, 1), directory);
            case "list" -> list(player, directory, args.length > 1 ? args[1] : "");
            case "search" -> list(player, directory, required(args, 1));
            case "move" -> move(directory, required(args, 1), required(args, 2));
            case "delete" -> Files.deleteIfExists(file(directory, required(args, 1)));
            case "clear" -> {
                LocalSession session = session(actor);
                session.getRegionSelector(actor.getWorld()).clear();
            }
            case "formats" -> player.sendMessage(ChatColor.GRAY + "Supported format: YAML (.sel.yml)");
            default -> throw new IllegalArgumentException("Unknown ssel operation: " + args[0]);
        }
        if (!args[0].equalsIgnoreCase("list") && !args[0].equalsIgnoreCase("search")
                && !args[0].equalsIgnoreCase("formats")) {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Saved selection operation completed.");
        }
    }

    private void save(org.bukkit.entity.Player player, Player actor, String[] args, Path directory) throws Exception {
        boolean includeStack = args.length > 1 && args[1].equalsIgnoreCase("-m");
        String name = required(args, includeStack ? 2 : 1);
        LocalSession session = session(actor);
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("version", 1);
        yaml.set("world", player.getWorld().getName());
        RegionCodec.write(yaml.createSection("selection"), session.getSelection(actor.getWorld()));
        if (includeStack) {
            RegionCodec.writeRegions(yaml, "stack", selections.history(player.getUniqueId()).stack());
        }
        yaml.save(file(directory, name).toFile());
    }

    private void load(org.bukkit.entity.Player player, Player actor, String name, Path directory) throws Exception {
        Path path = file(directory, name);
        if (!Files.isRegularFile(path)) throw new IllegalArgumentException("Saved selection does not exist: " + name);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(path.toFile());
        org.bukkit.World bukkitWorld = Bukkit.getWorld(yaml.getString("world", player.getWorld().getName()));
        if (bukkitWorld == null) throw new IllegalArgumentException("The saved selection world is not loaded.");
        var world = BukkitAdapter.adapt(bukkitWorld);
        Region region = RegionCodec.read(section(yaml, "selection"), world);
        LocalSession session = session(actor);
        session.setRegionSelector(world, RegionSelectors.of(region));
        ConfigurationSection stack = yaml.getConfigurationSection("stack");
        if (stack != null) {
            selections.history(player.getUniqueId()).replace(RegionCodec.readRegions(yaml, "stack", world));
        }
        session.dispatchCUISelection(actor);
    }

    private static void list(org.bukkit.entity.Player player, Path directory, String filter) throws IOException {
        String needle = filter.toLowerCase(Locale.ROOT);
        try (var files = Files.list(directory)) {
            List<String> names = files.filter(Files::isRegularFile).map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".sel.yml"))
                    .map(name -> name.substring(0, name.length() - 8))
                    .filter(name -> name.toLowerCase(Locale.ROOT).contains(needle))
                    .sorted(Comparator.naturalOrder()).toList();
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Saved selections: " + String.join(", ", names));
        }
    }

    private static void move(Path directory, String oldName, String newName) throws IOException {
        Files.move(file(directory, oldName), file(directory, newName));
    }

    private static Path file(Path directory, String name) {
        if (!SAFE_NAME.matcher(name).matches() || name.equals(".") || name.equals("..")) {
            throw new IllegalArgumentException("Selection names may only contain letters, digits, dot, underscore, and dash.");
        }
        return directory.resolve(name + ".sel.yml");
    }

    private static String required(String[] args, int index) {
        if (args.length <= index) throw new IllegalArgumentException("Missing command argument.");
        return args[index];
    }

    private static ConfigurationSection section(YamlConfiguration yaml, String key) {
        ConfigurationSection section = yaml.getConfigurationSection(key);
        if (section == null) throw new IllegalArgumentException("Invalid saved selection: missing " + key);
        return section;
    }
}
