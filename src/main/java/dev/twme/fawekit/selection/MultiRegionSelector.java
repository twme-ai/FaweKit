package dev.twme.fawekit.selection;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionIntersection;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.limit.SelectorLimits;
import com.sk89q.worldedit.world.World;

import java.util.ArrayList;
import java.util.List;

public final class MultiRegionSelector implements RegionSelector {
    private World world;
    private final List<Region> regions;
    private BlockVector3 primary;
    private boolean pendingPrimary;

    public MultiRegionSelector(World world, List<Region> regions) {
        if (regions.isEmpty()) {
            throw new IllegalArgumentException("A multi-selection cannot be empty.");
        }
        this.world = world;
        this.regions = new ArrayList<>(regions.stream().map(Region::clone).toList());
        this.primary = this.regions.get(0).getMinimumPoint();
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public void setWorld(World world) {
        this.world = world;
        regions.forEach(region -> region.setWorld(world));
    }

    @Override
    public boolean selectPrimary(BlockVector3 position, SelectorLimits limits) {
        regions.clear();
        primary = position;
        pendingPrimary = true;
        return true;
    }

    @Override
    public boolean selectSecondary(BlockVector3 position, SelectorLimits limits) {
        if (primary == null) return false;
        regions.clear();
        regions.add(new com.sk89q.worldedit.regions.CuboidRegion(world, primary, position));
        pendingPrimary = false;
        return true;
    }

    @Override
    public void explainPrimarySelection(Actor actor, LocalSession session, BlockVector3 position) {
    }

    @Override
    public void explainSecondarySelection(Actor actor, LocalSession session, BlockVector3 position) {
    }

    @Override
    public void explainRegionAdjust(Actor actor, LocalSession session) {
    }

    @Override
    public BlockVector3 getPrimaryPosition() throws IncompleteRegionException {
        if (primary == null) throw new IncompleteRegionException();
        return primary;
    }

    @Override
    public Region getRegion() throws IncompleteRegionException {
        if (!isDefined()) throw new IncompleteRegionException();
        return new RegionIntersection(world, regions);
    }

    @Override
    public Region getIncompleteRegion() {
        if (regions.isEmpty()) {
            BlockVector3 point = primary == null ? BlockVector3.ZERO : primary;
            return new com.sk89q.worldedit.regions.CuboidRegion(world, point, point);
        }
        return new RegionIntersection(world, regions);
    }

    @Override
    public boolean isDefined() {
        return !pendingPrimary && !regions.isEmpty();
    }

    @Override
    public long getVolume() {
        return isDefined() ? getIncompleteRegion().getVolume() : -1;
    }

    @Override
    public void learnChanges() {
        if (!regions.isEmpty()) primary = regions.get(0).getMinimumPoint();
    }

    @Override
    public void clear() {
        regions.clear();
        primary = null;
        pendingPrimary = true;
    }

    @Override
    public String getTypeName() {
        return "multi";
    }

    @Override
    public List<BlockVector3> getVertices() {
        if (!isDefined()) return primary == null ? List.of() : List.of(primary);
        List<BlockVector3> vertices = new ArrayList<>();
        regions.forEach(region -> {
            vertices.add(region.getMinimumPoint());
            vertices.add(region.getMaximumPoint());
        });
        return vertices;
    }
}
