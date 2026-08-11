package dev.twme.fawekit.listener;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;
import java.util.Map;

public final class CompatibilityAliasListener implements Listener {
    private static final Map<String, String> DOUBLE_SLASH_ALIASES = Map.of(
            "repeat", "stack",
            "unextend", "contract",
            "seldraw", "drawsel",
            "upload", "download"
    );

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void rewrite(PlayerCommandPreprocessEvent event) {
        try {
            event.setMessage(rewrite(event.getMessage()));
        } catch (IllegalArgumentException exception) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + exception.getMessage());
        }
    }

    static String rewrite(String message) {
        if (!message.startsWith("//")) return message;
        String[] split = message.substring(2).trim().split("\\s+");
        if (split.length == 0 || split[0].isEmpty()) return message;
        String root = split[0].toLowerCase(Locale.ROOT);
        if (root.equals("tracemask")) {
            return '/' + message.substring(2);
        }
        String alias = DOUBLE_SLASH_ALIASES.get(root);
        if (alias != null) {
            return "//" + alias + message.substring(2 + split[0].length());
        }
        if ((root.equals("sel") || root.equals("gmask")) && split.length == 2
                && split[1].equalsIgnoreCase("clear")) {
            return "//" + root;
        }
        if (root.equals("rotate") && split.length >= 3) {
            String direction = split[2].toLowerCase(Locale.ROOT);
            if (direction.equals("clockwise") || direction.equals("counterclockwise")) {
                double degrees;
                try {
                    degrees = Double.parseDouble(split[1]);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("The clockwise form requires a numeric Y rotation.");
                }
                if (direction.equals("counterclockwise")) degrees = -degrees;
                StringBuilder rewritten = new StringBuilder("//rotate ").append(format(degrees));
                for (int i = 3; i < split.length; i++) rewritten.append(' ').append(split[i]);
                return rewritten.toString();
            }
        }
        return message;
    }

    private static String format(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }
}
