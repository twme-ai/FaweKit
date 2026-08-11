package dev.twme.fawekit.command;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionIntersection;
import dev.twme.fawekit.selection.RegionSelectors;
import dev.twme.fawekit.service.SelectionStackService;
import org.bukkit.ChatColor;

import java.util.List;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public final class MultiSelectionCommand extends PlayerFaweCommand {
    private final SelectionStackService selections;

    public MultiSelectionCommand(SelectionStackService selections) {
        this.selections = selections;
    }

    @Override
    protected void execute(org.bukkit.entity.Player player, Player actor, String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("Usage: //msel <push|pop|combine|delete|clear|list|undo|redo>");
        }
        LocalSession session = session(actor);
        SelectionStackService.History history = selections.history(player.getUniqueId());
        switch (args[0].toLowerCase()) {
            case "push" -> {
                int index = args.length > 1 ? Integer.parseInt(args[1]) : 0;
                history.push(session.getSelection(actor.getWorld()), index);
            }
            case "pop" -> {
                int count = args.length == 1 ? 1
                        : args[1].equalsIgnoreCase("all") ? history.stack().size() : Integer.parseInt(args[1]);
                restore(session, actor, history.pop(count));
            }
            case "combine" -> restore(session, actor, history.pop(history.stack().size()));
            case "delete" -> history.delete(Integer.parseInt(requireArg(args, 1, "selection index")));
            case "clear" -> history.clear();
            case "list" -> list(player, history.stack(), page(args));
            case "undo" -> {
                if (!history.undo()) throw new IllegalArgumentException("Nothing to undo.");
            }
            case "redo" -> {
                if (!history.redo()) throw new IllegalArgumentException("Nothing to redo.");
            }
            default -> throw new IllegalArgumentException("Unknown msel operation: " + args[0]);
        }
        if (!args[0].equalsIgnoreCase("list")) {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Selection stack now contains " + history.stack().size() + " item(s).");
        }
    }

    private static void restore(LocalSession session, Player actor, List<Region> regions) {
        Region region = regions.size() == 1 ? regions.get(0) : new RegionIntersection(actor.getWorld(), regions);
        session.setRegionSelector(actor.getWorld(), RegionSelectors.of(region));
        session.dispatchCUISelection(actor);
    }

    private static void list(org.bukkit.entity.Player player, List<Region> stack, int page) {
        int pageSize = 10;
        int pages = Math.max(1, (stack.size() + pageSize - 1) / pageSize);
        if (page < 0 || page >= pages) throw new IllegalArgumentException("Selection page must be 0-" + (pages - 1) + '.');
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Selection stack (page " + page + '/' + (pages - 1) + "):");
        for (int i = page * pageSize; i < Math.min(stack.size(), (page + 1) * pageSize); i++) {
            Region region = stack.get(i);
            TextComponent line = new TextComponent(" " + (i + 1) + ". " + region.getClass().getSimpleName()
                    + " " + region.getMinimumPoint() + " to " + region.getMaximumPoint());
            line.setColor(net.md_5.bungee.api.ChatColor.GRAY);
            line.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "//tpsel -s " + (i + 1)));
            line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("Teleport to this selection").color(net.md_5.bungee.api.ChatColor.LIGHT_PURPLE).create()));
            player.spigot().sendMessage(line);
        }
    }

    private static int page(String[] args) {
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("-p") && i + 1 < args.length) return Integer.parseInt(args[i + 1]);
            if (!args[i].matches("-[dn]+")) throw new IllegalArgumentException("Usage: //msel list [-dn] [-p page]");
        }
        return 0;
    }

    private static String requireArg(String[] args, int index, String name) {
        if (args.length <= index) {
            throw new IllegalArgumentException("Missing " + name + '.');
        }
        return args[index];
    }
}
