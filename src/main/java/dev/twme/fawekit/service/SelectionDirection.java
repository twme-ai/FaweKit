package dev.twme.fawekit.service;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;

import java.util.List;

public final class SelectionDirection {
    private SelectionDirection() {
    }

    public static BlockVector3 get(LocalSession session, World world) throws Exception {
        Region region = session.getSelection(world);
        BlockVector3 primary = session.getRegionSelector(world).getPrimaryPosition();
        BlockVector3 secondary = secondary(region);
        BlockVector3 result = secondary.subtract(primary);
        if (result.equals(BlockVector3.ZERO)) {
            throw new IllegalArgumentException("Selection positions must define a direction.");
        }
        return result;
    }

    private static BlockVector3 secondary(Region region) {
        if (region instanceof CuboidRegion cuboid) {
            return cuboid.getPos2();
        }
        if (region instanceof Polygonal2DRegion polygon) {
            List<BlockVector2> points = polygon.getPoints();
            BlockVector2 point = points.get(points.size() - 1);
            return BlockVector3.at(point.x(), region.getMaximumY(), point.z());
        }
        if (region instanceof ConvexPolyhedralRegion polyhedron) {
            BlockVector3 result = null;
            for (BlockVector3 vertex : polyhedron.getVertices()) {
                result = vertex;
            }
            if (result != null) {
                return result;
            }
        }
        return region.getMaximumPoint();
    }
}
