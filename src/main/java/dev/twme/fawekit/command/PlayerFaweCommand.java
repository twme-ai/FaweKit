package dev.twme.fawekit.command;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.ParserContext;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public abstract class PlayerFaweCommand implements CommandExecutor {
    @Override
    public final boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof org.bukkit.entity.Player bukkitPlayer)) {
            sender.sendMessage(ChatColor.RED + "This command requires an in-game player.");
            return true;
        }
        try {
            execute(bukkitPlayer, BukkitAdapter.adapt(bukkitPlayer), args);
        } catch (Exception exception) {
            sender.sendMessage(ChatColor.RED + exception.getMessage());
        }
        return true;
    }

    protected abstract void execute(org.bukkit.entity.Player bukkitPlayer, Player actor, String[] args) throws Exception;

    protected static LocalSession session(Player actor) {
        return WorldEdit.getInstance().getSessionManager().get(actor);
    }

    protected static ParserContext parserContext(Player actor, LocalSession session) {
        ParserContext context = new ParserContext();
        context.setActor(actor);
        context.setWorld(actor.getWorld());
        context.setSession(session);
        context.setRestricted(false);
        return context;
    }
}
