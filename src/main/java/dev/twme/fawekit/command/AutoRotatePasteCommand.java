package dev.twme.fawekit.command;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import dev.twme.fawekit.math.QuarterRotation;
import dev.twme.fawekit.service.CopyDirectionStore;
import dev.twme.fawekit.service.SelectionDirection;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public final class AutoRotatePasteCommand extends PlayerFaweCommand {
    private final CopyDirectionStore directions;

    public AutoRotatePasteCommand(CopyDirectionStore directions) {
        this.directions = directions;
    }

    @Override
    protected void execute(org.bukkit.entity.Player player, Player actor, String[] args) throws Exception {
        Options options = Options.parse(args);
        LocalSession session = session(actor);
        ClipboardHolder holder = session.getClipboard();
        session.getSelection(actor.getWorld());
        BlockVector3 oldDirection = directions.get(player.getUniqueId())
                .orElseThrow(() -> new IllegalArgumentException("Run //copy once after installing this plugin before using //arp."));
        BlockVector3 newDirection = SelectionDirection.get(session, actor.getWorld());
        QuarterRotation rotation = QuarterRotation.between(oldDirection, newDirection);
        holder.setTransform(holder.getTransform().combine(rotation.transform()));
        BlockVector3 destination = options.atOrigin() ? holder.getClipboard().getOrigin()
                : options.relative() ? session.getPlacementPosition(actor)
                : session.getRegionSelector(actor.getWorld()).getPrimaryPosition();
        Mask sourceMask = options.sourceMask() == null ? null
                : WorldEdit.getInstance().getMaskFactory().parseFromInput(options.sourceMask(), parserContext(actor, session));
        try (EditSession editSession = session.createEditSession(actor)) {
            if (!options.onlySelect()) {
                var operation = holder.createPaste(editSession).to(destination)
                        .ignoreAirBlocks(options.ignoreAir()).copyBiomes(options.copyBiomes())
                        .copyEntities(options.copyEntities()).maskSource(sourceMask).build();
                com.sk89q.worldedit.function.operation.Operations.complete(operation);
            }
            if (options.selectPasted() || options.onlySelect()) {
                Region pasted = pastedBounds(holder, destination, actor);
                session.setRegionSelector(actor.getWorld(), dev.twme.fawekit.selection.RegionSelectors.of(pasted));
                session.dispatchCUISelection(actor);
            }
            session.remember(editSession);
        }
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Clipboard rotated and pasted at " + destination + '.');
    }

    private static Region pastedBounds(ClipboardHolder holder, BlockVector3 destination, Player actor) {
        Region source = holder.getClipboard().getRegion();
        BlockVector3 origin = holder.getClipboard().getOrigin();
        BlockVector3 min = null;
        BlockVector3 max = null;
        for (int x : new int[]{source.getMinimumPoint().x(), source.getMaximumPoint().x()}) {
            for (int y : new int[]{source.getMinimumPoint().y(), source.getMaximumPoint().y()}) {
                for (int z : new int[]{source.getMinimumPoint().z(), source.getMaximumPoint().z()}) {
                    Vector3 relative = BlockVector3.at(x, y, z).subtract(origin).toVector3();
                    BlockVector3 point = holder.getTransform().apply(relative).add(destination.toVector3()).toBlockPoint();
                    min = min == null ? point : min.getMinimum(point);
                    max = max == null ? point : max.getMaximum(point);
                }
            }
        }
        return new CuboidRegion(actor.getWorld(), min, max);
    }

    private record Options(boolean ignoreAir, boolean copyBiomes, boolean copyEntities, boolean onlySelect,
                           boolean atOrigin, boolean selectPasted, boolean relative, String sourceMask) {
        private static Options parse(String[] args) {
            boolean a = false, b = false, e = false, n = false, o = false, s = false, r = false;
            String mask = null;
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-m")) {
                    if (++i >= args.length) throw usage();
                    mask = args[i];
                } else if (args[i].startsWith("-") && args[i].length() > 1) {
                    for (int j = 1; j < args[i].length(); j++) {
                        switch (args[i].charAt(j)) {
                            case 'a' -> a = true;
                            case 'b' -> b = true;
                            case 'e' -> e = true;
                            case 'n' -> n = true;
                            case 'o' -> o = true;
                            case 's' -> s = true;
                            case 'r' -> r = true;
                            default -> throw new IllegalArgumentException("Unknown flag: -" + args[i].charAt(j));
                        }
                    }
                } else throw usage();
            }
            if (o && r) throw new IllegalArgumentException("-o and -r cannot be combined.");
            return new Options(a, b, e, n, o, s, r, mask);
        }

        private static IllegalArgumentException usage() {
            return new IllegalArgumentException("Usage: //autorotatepaste [-abenosr] [-m <sourceMask>]");
        }
    }
}
