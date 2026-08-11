package dev.twme.fawekit.listener;

import com.fastasyncworldedit.core.configuration.Settings;
import com.sk89q.worldedit.WorldEdit;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class SchematicSearchListener implements Listener {
    private static final int PAGE_SIZE = 10;

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void search(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (!message.toLowerCase(Locale.ROOT).startsWith("//schematic search ")) return;
        event.setCancelled(true);
        if (!event.getPlayer().hasPermission("worldedit.schematic.load")) {
            event.getPlayer().sendMessage(org.bukkit.ChatColor.RED + "You lack worldedit.schematic.load.");
            return;
        }
        try {
            Query query = Query.parse(message.substring("//schematic search ".length()).trim());
            Path root = schematicRoot(event.getPlayer().getUniqueId().toString());
            List<Result> results = find(root, query.text());
            int pages = Math.max(1, (results.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            if (query.page() < 0 || query.page() >= pages) {
                throw new IllegalArgumentException("Schematic page must be 0-" + (pages - 1) + '.');
            }
            event.getPlayer().sendMessage(org.bukkit.ChatColor.LIGHT_PURPLE + "Schematic matches (page "
                    + query.page() + '/' + (pages - 1) + "):");
            for (int i = query.page() * PAGE_SIZE; i < Math.min(results.size(), (query.page() + 1) * PAGE_SIZE); i++) {
                Result result = results.get(i);
                TextComponent line = new TextComponent(" " + result.name());
                line.setColor(ChatColor.GRAY);
                line.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "//schematic load " + result.loadName()));
                line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder("Load " + result.name()).color(ChatColor.LIGHT_PURPLE).create()));
                event.getPlayer().spigot().sendMessage(line);
            }
        } catch (Exception exception) {
            event.getPlayer().sendMessage(org.bukkit.ChatColor.RED + exception.getMessage());
        }
    }

    private static Path schematicRoot(String playerId) {
        WorldEdit worldEdit = WorldEdit.getInstance();
        Path root = worldEdit.getWorkingDirectoryPath(worldEdit.getConfiguration().saveDir);
        return Settings.settings().PATHS.PER_PLAYER_SCHEMATICS ? root.resolve(playerId) : root;
    }

    private static List<Result> find(Path root, String needle) throws IOException {
        if (!Files.isDirectory(root)) return List.of();
        String lowerNeedle = needle.toLowerCase(Locale.ROOT);
        try (var files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                    .filter(SchematicSearchListener::isSchematic)
                    .map(path -> result(root, path, lowerNeedle))
                    .sorted(Comparator.comparingInt(Result::score).thenComparing(Result::name))
                    .toList();
        }
    }

    private static Result result(Path root, Path path, String needle) {
        String relative = root.relativize(path).toString().replace(path.getFileSystem().getSeparator(), "/");
        String loadName = stripExtension(relative);
        return new Result(relative, loadName, fuzzyScore(loadName.toLowerCase(Locale.ROOT), needle));
    }

    private static boolean isSchematic(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".schem") || name.endsWith(".schematic") || name.endsWith(".mcedit");
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    static int fuzzyScore(String candidate, String query) {
        if (query.isEmpty()) return 0;
        int score = 0;
        int cursor = 0;
        int previous = -1;
        for (int i = 0; i < query.length(); i++) {
            int found = candidate.indexOf(query.charAt(i), cursor);
            if (found < 0) return 10_000 + levenshtein(candidate, query);
            score += previous < 0 ? found : found - previous - 1;
            previous = found;
            cursor = found + 1;
        }
        return score + Math.max(0, candidate.length() - query.length());
    }

    private static int levenshtein(String left, String right) {
        int[] costs = new int[right.length() + 1];
        for (int j = 0; j < costs.length; j++) costs[j] = j;
        for (int i = 1; i <= left.length(); i++) {
            int diagonal = costs[0];
            costs[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int old = costs[j];
                costs[j] = Math.min(Math.min(costs[j] + 1, costs[j - 1] + 1),
                        diagonal + (left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1));
                diagonal = old;
            }
        }
        return costs[right.length()];
    }

    private record Result(String name, String loadName, int score) {}

    private record Query(int page, String text) {
        private static Query parse(String input) {
            String[] args = input.split("\\s+");
            int page = 0;
            List<String> words = new ArrayList<>();
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-p")) {
                    if (++i >= args.length) throw new IllegalArgumentException("Missing page number.");
                    page = Integer.parseInt(args[i]);
                } else if (args[i].matches("-[dfn]+")) {
                    // Presentation flags are accepted for compatibility with the proposed syntax.
                } else words.add(args[i]);
            }
            if (words.isEmpty()) throw new IllegalArgumentException("Usage: //schematic search [-dfn] [-p page] <text>");
            return new Query(page, String.join(" ", words));
        }
    }
}
