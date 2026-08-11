package dev.twme.fawekit.selection;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.CylinderRegion;
import com.sk89q.worldedit.regions.EllipsoidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionIntersection;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.ConvexPolyhedralRegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.selector.CylinderRegionSelector;
import com.sk89q.worldedit.regions.selector.EllipsoidRegionSelector;
import com.sk89q.worldedit.regions.selector.Polygonal2DRegionSelector;

public final class RegionSelectors {
    private RegionSelectors() {
    }

    public static RegionSelector of(Region region) {
        if (region instanceof CuboidRegion cuboid) {
            return new CuboidRegionSelector(cuboid.getWorld(), cuboid.getPos1(), cuboid.getPos2());
        }
        if (region instanceof Polygonal2DRegion polygon) {
            return new Polygonal2DRegionSelector(polygon);
        }
        if (region instanceof ConvexPolyhedralRegion polyhedron) {
            return new ConvexPolyhedralRegionSelector(polyhedron);
        }
        if (region instanceof CylinderRegion cylinder) {
            return new CylinderRegionSelector(cylinder);
        }
        if (region instanceof EllipsoidRegion ellipsoid) {
            return new EllipsoidRegionSelector(ellipsoid.getWorld(), ellipsoid.getCenter().toBlockPoint(),
                    ellipsoid.getRadius());
        }
        if (region instanceof RegionIntersection intersection) {
            return new MultiRegionSelector(intersection.getWorld(), intersection.getRegions());
        }
        BlockVector3 min = region.getMinimumPoint();
        return new CuboidRegionSelector(region.getWorld(), min, region.getMaximumPoint());
    }
}
