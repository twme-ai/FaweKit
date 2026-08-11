package dev.twme.fawekit.command;

import com.fastasyncworldedit.core.util.MaskTraverser;
import com.fastasyncworldedit.core.extent.clipboard.MemoryOptimizedClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Location;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public final class CopyNearCommand extends PlayerFaweCommand {
    private static final int DEFAULT_DISTANCE = 64;
    private static final int MAX_DISTANCE = 256;

    @Override
    protected void execute(org.bukkit.entity.Player player, Player actor, String[] args) throws Exception {
        Options options = Options.parse(args);
        LocalSession session = session(actor);
        try (EditSession editSession = session.createEditSession(actor)) {
            var context = parserContext(actor, session);
            context.setExtent(editSession);
            Mask searchMask = WorldEdit.getInstance().getMaskFactory().parseFromInput(options.searchMask(), context);
            new MaskTraverser(searchMask).reset(editSession);

            BlockVector3 center = actor.getBlockLocation().toVector().toBlockPoint();
            long maxChecks = actor.getLimit().MAX_CHECKS.get();
            List<BlockVector3> matches = findMatches(editSession, searchMask, center, options.distance(), maxChecks);
            if (matches.isEmpty()) {
                throw new IllegalArgumentException("No matching blocks found within " + options.distance() + " blocks.");
            }

            Region region = regionAround(actor, matches);
            session.setRegionSelector(actor.getWorld(), dev.twme.fawekit.selection.RegionSelectors.of(region));
            session.dispatchCUISelection(actor);

            BlockVector3 sourceMin = region.getMinimumPoint();
            CuboidRegion clipboardRegion = new CuboidRegion(actor.getWorld(), sourceMin, region.getMaximumPoint());
            BlockArrayClipboard clipboard = new BlockArrayClipboard(
                    clipboardRegion, new MemoryOptimizedClipboard(clipboardRegion));
            BlockVector3 worldOrigin = options.centerOrigin()
                    ? region.getCenter().toBlockPoint().withY(region.getMinimumY())
                    : session.getPlacementPosition(actor);
            clipboard.setOrigin(worldOrigin);
            Mask copyMask = null;
            if (options.copyMask() != null) {
                copyMask = WorldEdit.getInstance().getMaskFactory().parseFromInput(options.copyMask(), context);
            }
            if (options.excludeMatches()) {
                copyMask = MaskIntersection.of(copyMask, searchMask.inverse());
            }
            if (copyMask != null) {
                new MaskTraverser(copyMask).reset(editSession);
            }
            int copiedBlocks = 0;
            BlockVector3 sampleTarget = null;
            for (BlockVector3 position : region) {
                if (copyMask == null || copyMask.test(position)) {
                    BlockVector3 target = position;
                    if (!clipboard.setBlock(target, editSession.getFullBlock(position))) {
                        throw new IllegalStateException("Clipboard rejected block at " + target + '.');
                    }
                    copiedBlocks++;
                    if (sampleTarget == null) sampleTarget = target;
                    if (options.copyBiomes()) clipboard.setBiome(target, editSession.getBiome(position));
                }
            }
            if (options.copyEntities()) {
                for (var entity : editSession.getEntities(region)) {
                    var state = entity.getState();
                    if (state == null) continue;
                    Location source = entity.getLocation();
                    clipboard.createEntity(new Location(clipboard,
                            source.x(), source.y(), source.z(),
                            source.getYaw(), source.getPitch()), state);
                }
            }
            clipboard.flush();
            if (sampleTarget != null && clipboard.getBlock(sampleTarget).getMaterial().isAir()
                    && !editSession.getBlock(sampleTarget).getMaterial().isAir()) {
                throw new IllegalStateException("Clipboard storage returned air after writing " + copiedBlocks + " block(s).");
            }
            session.setClipboard(new ClipboardHolder(clipboard));
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Copied " + region.getVolume() + " blocks around "
                    + matches.size() + " match(es); wrote " + copiedBlocks + " clipboard block(s), origin "
                    + clipboard.getOrigin() + ". "
                    + (options.excludeMatches() ? "Matches were excluded." : ""));
        }
    }

    private static List<BlockVector3> findMatches(
            EditSession extent, Mask mask, BlockVector3 center, int distance, long maxChecks
    ) {
        long diameter = distance * 2L + 1;
        long checks = diameter * diameter * diameter;
        if (checks > maxChecks) {
            throw new IllegalArgumentException("Search requires " + checks + " checks; your FAWE limit is "
                    + maxChecks + '.');
        }
        List<BlockVector3> matches = new ArrayList<>();
        long radiusSquared = (long) distance * distance;
        int minY = Math.max(extent.getMinY(), center.y() - distance);
        int maxY = Math.min(extent.getMaxY(), center.y() + distance);
        for (int x = center.x() - distance; x <= center.x() + distance; x++) {
            long dx = (long) x - center.x();
            for (int z = center.z() - distance; z <= center.z() + distance; z++) {
                long dz = (long) z - center.z();
                for (int y = minY; y <= maxY; y++) {
                    long dy = (long) y - center.y();
                    if (dx * dx + dy * dy + dz * dz <= radiusSquared) {
                        BlockVector3 position = BlockVector3.at(x, y, z);
                        if (mask.test(position)) matches.add(position);
                    }
                }
            }
        }
        return matches;
    }

    private static Region regionAround(Player actor, List<BlockVector3> matches) {
        if (matches.size() < 4) {
            BlockVector3 min = matches.get(0);
            BlockVector3 max = min;
            for (BlockVector3 point : matches) {
                min = min.getMinimum(point);
                max = max.getMaximum(point);
            }
            return new CuboidRegion(actor.getWorld(), min, max);
        }
        ConvexPolyhedralRegion region = new ConvexPolyhedralRegion(actor.getWorld());
        for (BlockVector3 match : matches) region.addVertex(match);
        return region;
    }

    private record Options(boolean excludeMatches, boolean copyBiomes, boolean centerOrigin, boolean copyEntities,
                           String copyMask, String searchMask, int distance) {
        private static Options parse(String[] args) {
            boolean exclude = false;
            boolean biomes = false;
            boolean center = false;
            boolean entities = false;
            String copyMask = null;
            List<String> positional = new ArrayList<>();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals("-m")) {
                    if (++i >= args.length) throw usage();
                    copyMask = args[i];
                } else if (arg.startsWith("-") && arg.length() > 1) {
                    for (int j = 1; j < arg.length(); j++) {
                        switch (arg.charAt(j)) {
                            case 'x' -> exclude = true;
                            case 'b' -> biomes = true;
                            case 'c' -> center = true;
                            case 'e' -> entities = true;
                            default -> throw new IllegalArgumentException("Unknown flag: -" + arg.charAt(j));
                        }
                    }
                } else {
                    positional.add(arg);
                }
            }
            if (positional.isEmpty() || positional.size() > 2) throw usage();
            int distance = positional.size() == 2 ? Integer.parseInt(positional.get(1)) : DEFAULT_DISTANCE;
            if (distance < 1 || distance > MAX_DISTANCE) {
                throw new IllegalArgumentException("Distance must be between 1 and " + MAX_DISTANCE + '.');
            }
            return new Options(exclude, biomes, center, entities, copyMask, positional.get(0), distance);
        }

        private static IllegalArgumentException usage() {
            return new IllegalArgumentException("Usage: //copynear [-xbce] [-m <mask>] <mask> [distance=64]");
        }
    }
}
