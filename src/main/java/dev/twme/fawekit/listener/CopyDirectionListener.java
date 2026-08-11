package dev.twme.fawekit.listener;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import dev.twme.fawekit.service.CopyDirectionStore;
import dev.twme.fawekit.service.SelectionDirection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class CopyDirectionListener implements Listener {
    private final CopyDirectionStore store;

    public CopyDirectionListener(CopyDirectionStore store) {
        this.store = store;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().stripLeading().toLowerCase();
        if (!command.equals("//copy") && !command.startsWith("//copy ")) {
            return;
        }
        try {
            var actor = BukkitAdapter.adapt(event.getPlayer());
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
            store.put(event.getPlayer().getUniqueId(), SelectionDirection.get(session, actor.getWorld()));
        } catch (Exception ignored) {
            // WorldEdit reports incomplete selections to the player when //copy executes.
        }
    }
}
