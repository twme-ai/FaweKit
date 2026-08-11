package dev.twme.fawekit.listener;

import dev.twme.fawekit.service.ShortcutService;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class ShortcutListener implements Listener {
    private final ShortcutService shortcuts;

    public ShortcutListener(ShortcutService shortcuts) {
        this.shortcuts = shortcuts;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void expand(PlayerCommandPreprocessEvent event) {
        String lower = event.getMessage().toLowerCase();
        if (lower.startsWith("//shortcut ") || lower.startsWith("//sc ")) return;
        try {
            event.setMessage(shortcuts.expandHashShortcuts(event.getPlayer().getUniqueId(), event.getMessage()));
        } catch (IllegalArgumentException exception) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + exception.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void remember(PlayerCommandPreprocessEvent event) {
        shortcuts.record(event.getPlayer().getUniqueId(), event.getMessage());
    }
}
