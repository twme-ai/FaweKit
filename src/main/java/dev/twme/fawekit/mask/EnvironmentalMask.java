package dev.twme.fawekit.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

public final class EnvironmentalMask extends AbstractExtentMask {
    private static final BlockVector3[] DIRECTIONS = {
            BlockVector3.UNIT_X, BlockVector3.UNIT_X.multiply(-1),
            BlockVector3.UNIT_Y, BlockVector3.UNIT_Y.multiply(-1),
            BlockVector3.UNIT_Z, BlockVector3.UNIT_Z.multiply(-1)
    };

    public enum Kind {
        VISIBLE, SKY, TRANSPARENT, CONDUCTIVE, SKY_LIGHT, BLOCK_LIGHT, LIGHT, EMITS_LIGHT, OPACITY, HAS_LIGHT, NO_LIGHT
    }

    private final Kind kind;
    private final int minimum;
    private final int maximum;

    public EnvironmentalMask(Extent extent, Kind kind, int minimum, int maximum) {
        super(extent);
        this.kind = kind;
        this.minimum = minimum;
        this.maximum = maximum;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        return test(getExtent(), vector);
    }

    @Override
    public boolean test(Extent extent, BlockVector3 position) {
        return switch (kind) {
            case VISIBLE -> visible(extent, position);
            case SKY -> sky(extent, position);
            case TRANSPARENT -> transparent(extent, position);
            case CONDUCTIVE -> conductive(extent, position);
            case SKY_LIGHT -> range(extent.getSkyLight(position.x(), position.y(), position.z()));
            case BLOCK_LIGHT -> range(extent.getEmittedLight(position));
            case LIGHT -> range(Math.max(extent.getSkyLight(position.x(), position.y(), position.z()),
                    extent.getEmittedLight(position)));
            case EMITS_LIGHT -> range(extent.getFullBlock(position).getMaterial().getLightValue());
            case OPACITY -> range(extent.getOpacity(position.x(), position.y(), position.z()));
            case HAS_LIGHT -> extent.getSkyLight(position.x(), position.y(), position.z()) > 0
                    || extent.getEmittedLight(position) > 0;
            case NO_LIGHT -> extent.getSkyLight(position.x(), position.y(), position.z()) == 0
                    && extent.getEmittedLight(position) == 0;
        };
    }

    private boolean range(int value) {
        return value >= minimum && value <= maximum;
    }

    private static boolean visible(Extent extent, BlockVector3 position) {
        for (BlockVector3 direction : DIRECTIONS) {
            if (transparent(extent, position.add(direction))) return true;
        }
        return false;
    }

    private static boolean sky(Extent extent, BlockVector3 position) {
        for (int y = position.y() + 1; y <= extent.getMaxY(); y++) {
            if (!extent.getBlock(position.x(), y, position.z()).getMaterial().isAir()) return false;
        }
        return true;
    }

    private static boolean transparent(Extent extent, BlockVector3 position) {
        return !extent.getBlock(position).getMaterial().isOpaque();
    }

    private static boolean conductive(Extent extent, BlockVector3 position) {
        var block = extent.getBlock(position);
        return block.getMaterial().isOpaque() && !block.getBlockType().id().equals("minecraft:observer");
    }

    @Override
    public Mask copy() {
        return new EnvironmentalMask(getExtent(), kind, minimum, maximum);
    }
}
