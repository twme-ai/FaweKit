package dev.twme.fawekit.command;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import dev.twme.fawekit.service.PinService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class PinCommand implements CommandExecutor {
    private final PinService pins;
    private final boolean pin;

    public PinCommand(PinService pins, boolean pin) {
        this.pins = pins;
        this.pin = pin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof org.bukkit.entity.Player player)) {
            sender.sendMessage(ChatColor.RED + "This command requires an in-game player.");
            return true;
        }
        if (pin) {
            var actor = BukkitAdapter.adapt(player);
            pins.pin(player.getUniqueId(), actor.getLocation());
            player.sendMessage(ChatColor.LIGHT_PURPLE + "FAWE command location pinned at " + actor.getBlockLocation() + '.');
        } else {
            pins.unpin(player.getUniqueId());
            player.sendMessage(ChatColor.LIGHT_PURPLE + "FAWE command location unpinned.");
        }
        return true;
    }
}
