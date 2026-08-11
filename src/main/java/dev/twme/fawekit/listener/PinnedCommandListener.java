package dev.twme.fawekit.listener;

import com.fastasyncworldedit.core.wrappers.LocationMaskedPlayerWrapper;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.platform.CommandEvent;
import dev.twme.fawekit.service.PinService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Set;

public final class PinnedCommandListener implements Listener {
    private static final Set<String> PLUGIN_COMMANDS = Set.of(
            "tpsel", "seltp", "multireplace", "multirepl", "clipboard", "autorotatepaste", "arp",
            "msel", "ssel", "bmask", "help-masks", "help-patterns", "echo", "shortcut", "sc", "pin", "unpin",
            "copynear");
    private final PinService pins;

    public PinnedCommandListener(PinService pins) {
        this.pins = pins;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        var pinned = pins.get(event.getPlayer().getUniqueId());
        if (pinned.isEmpty() || !event.getMessage().startsWith("//")) return;
        String message = event.getMessage();
        int end = message.indexOf(' ');
        String root = message.substring(2, end < 0 ? message.length() : end).toLowerCase();
        if (PLUGIN_COMMANDS.contains(root)) return;

        event.setCancelled(true);
        var player = BukkitAdapter.adapt(event.getPlayer());
        var masked = new LocationMaskedPlayerWrapper(player, pinned.get());
        WorldEdit.getInstance().getEventBus().post(new CommandEvent(masked, message.substring(1)));
    }
}
