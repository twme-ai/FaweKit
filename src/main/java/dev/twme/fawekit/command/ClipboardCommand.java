package dev.twme.fawekit.command;

import com.fastasyncworldedit.core.extent.clipboard.URIClipboardHolder;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.ChatColor;

import java.util.Arrays;
import java.util.List;

public final class ClipboardCommand extends PlayerFaweCommand {
    @Override
    protected void execute(org.bukkit.entity.Player player, Player actor, String[] args) throws Exception {
        LocalSession session = session(actor);
        ClipboardHolder holder = session.getClipboard();
        if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
            list(player, holder, Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("select")) {
            select(session, holder, Arrays.copyOfRange(args, 1, args.length));
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Primary clipboard selected.");
            return;
        }
        Clipboard source = holder.getClipboard();
        if (args.length == 0 || args[0].equalsIgnoreCase("size")) {
            BlockVector3 dimensions = source.getDimensions();
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Clipboard: " + dimensions.x() + " x " + dimensions.y()
                    + " x " + dimensions.z() + ", origin " + source.getOrigin());
            return;
        }
        switch (args[0].toLowerCase()) {
            case "stretch", "compress" -> stretch(session, source, Arrays.copyOfRange(args, 1, args.length));
            case "crop" -> crop(session, source, Arrays.copyOfRange(args, 1, args.length));
            default -> throw new IllegalArgumentException("Usage: //clipboard <size|stretch|compress|crop|list|select>");
        }
        BlockVector3 dimensions = session.getClipboard().getClipboard().getDimensions();
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Clipboard resized to " + dimensions.x() + " x "
                + dimensions.y() + " x " + dimensions.z() + '.');
    }

    private static void stretch(LocalSession session, Clipboard source, String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: //clipboard stretch <width|%> <height|%> <length|%>");
        }
        BlockVector3 old = source.getDimensions();
        BlockVector3 size = BlockVector3.at(size(args[0], old.x()), size(args[1], old.y()), size(args[2], old.z()));
        BlockArrayClipboard target = emptyClipboard(size);
        BlockVector3 sourceMin = source.getMinimumPoint();
        for (BlockVector3 destination : target.getRegion()) {
            int x = nearest(destination.x(), size.x(), old.x());
            int y = nearest(destination.y(), size.y(), old.y());
            int z = nearest(destination.z(), size.z(), old.z());
            target.setBlock(destination, source.getFullBlock(sourceMin.add(x, y, z)));
        }
        target.setOrigin(scaleOrigin(source, sourceMin, size, old));
        target.flush();
        session.setClipboard(new ClipboardHolder(target));
    }

    private static void crop(LocalSession session, Clipboard source, String[] args) throws Exception {
        int width = source.getDimensions().x();
        int height = source.getDimensions().y();
        int length = source.getDimensions().z();
        int ox = 0;
        int oy = 0;
        int oz = 0;
        for (int i = 0; i < args.length; i += 2) {
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("Crop options require values.");
            }
            switch (args[i]) {
                case "-w" -> width = size(args[i + 1], width);
                case "-h" -> height = size(args[i + 1], height);
                case "-l" -> length = size(args[i + 1], length);
                case "-x" -> ox = Integer.parseInt(args[i + 1]);
                case "-y" -> oy = Integer.parseInt(args[i + 1]);
                case "-z" -> oz = Integer.parseInt(args[i + 1]);
                default -> throw new IllegalArgumentException("Unknown crop option: " + args[i]);
            }
        }
        BlockVector3 size = BlockVector3.at(width, height, length);
        BlockArrayClipboard target = emptyClipboard(size);
        BlockVector3 sourceMin = source.getMinimumPoint();
        for (BlockVector3 destination : target.getRegion()) {
            BlockVector3 sourcePoint = sourceMin.add(destination.x() + ox, destination.y() + oy, destination.z() + oz);
            if (source.getRegion().contains(sourcePoint)) {
                target.setBlock(destination, source.getFullBlock(sourcePoint));
            }
        }
        target.setOrigin(source.getOrigin().subtract(sourceMin).subtract(ox, oy, oz));
        target.flush();
        session.setClipboard(new ClipboardHolder(target));
    }

    private static BlockArrayClipboard emptyClipboard(BlockVector3 size) {
        if (size.x() < 1 || size.y() < 1 || size.z() < 1) {
            throw new IllegalArgumentException("Clipboard dimensions must be positive.");
        }
        return new BlockArrayClipboard(new CuboidRegion(BlockVector3.ZERO, size.subtract(1, 1, 1)));
    }

    static int size(String input, int oldSize) {
        boolean relative = input.startsWith("~");
        String valueInput = relative ? input.substring(1) : input;
        if (valueInput.isEmpty()) return oldSize;
        double parsed = valueInput.endsWith("%")
                ? oldSize * Double.parseDouble(valueInput.substring(0, valueInput.length() - 1)) / 100.0
                : Double.parseDouble(valueInput);
        double value = relative ? oldSize + parsed : parsed;
        return Math.max(1, (int) Math.round(value));
    }

    private static void list(org.bukkit.entity.Player player, ClipboardHolder holder, String[] args) {
        int page = optionInt(args, "-p", 0);
        int pageSize = 10;
        List<ClipboardHolder> holders = holder.getHolders();
        int pages = Math.max(1, (holders.size() + pageSize - 1) / pageSize);
        if (page < 0 || page >= pages) throw new IllegalArgumentException("Clipboard page must be 0-" + (pages - 1) + '.');
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Clipboards (page " + page + '/' + (pages - 1) + "):" );
        for (int i = page * pageSize; i < Math.min(holders.size(), (page + 1) * pageSize); i++) {
            ClipboardHolder entry = holders.get(i);
            String name = entry instanceof URIClipboardHolder uri && !uri.getUri().toString().isEmpty()
                    ? uri.getUri().toString() : "clipboard-" + i;
            BlockVector3 dimensions = entry.getClipboard().getDimensions();
            player.sendMessage(ChatColor.GRAY + " " + i + ". " + name + " (" + dimensions.x() + "x"
                    + dimensions.y() + "x" + dimensions.z() + ")");
        }
    }

    private static void select(LocalSession session, ClipboardHolder holder, String[] args) {
        int index;
        if (args.length == 2 && args[0].equals("-n")) index = Integer.parseInt(args[1]);
        else if (args.length == 1) index = Integer.parseInt(args[0]);
        else throw new IllegalArgumentException("Usage: //clipboard select [-n] <index>");
        List<ClipboardHolder> holders = holder.getHolders();
        if (index < 0 || index >= holders.size()) throw new IllegalArgumentException("Clipboard index is out of range.");
        session.setClipboard(holders.get(index));
    }

    private static int optionInt(String[] args, String option, int fallback) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(option)) {
                if (++i >= args.length) throw new IllegalArgumentException("Missing value for " + option + '.');
                return Integer.parseInt(args[i]);
            }
            if (!args[i].equals("-d") && !args[i].equals("-n")) {
                throw new IllegalArgumentException("Unknown list option: " + args[i]);
            }
        }
        return fallback;
    }

    static int nearest(int destination, int destinationSize, int sourceSize) {
        if (destinationSize == 1 || sourceSize == 1) {
            return 0;
        }
        return (int) Math.round(destination * (sourceSize - 1.0) / (destinationSize - 1.0));
    }

    private static BlockVector3 scaleOrigin(Clipboard source, BlockVector3 min, BlockVector3 size, BlockVector3 old) {
        BlockVector3 relative = source.getOrigin().subtract(min);
        return BlockVector3.at(nearest(relative.x(), old.x(), size.x()), nearest(relative.y(), old.y(), size.y()),
                nearest(relative.z(), old.z(), size.z()));
    }
}
