package dev.twme.fawekit.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.biome.BiomeType;

import java.util.Set;

public final class BiomeMask extends AbstractExtentMask {
    private final Set<BiomeType> biomes;

    public BiomeMask(Extent extent, Set<BiomeType> biomes) {
        super(extent);
        this.biomes = Set.copyOf(biomes);
    }

    @Override
    public boolean test(BlockVector3 vector) {
        return test(getExtent(), vector);
    }

    @Override
    public boolean test(Extent extent, BlockVector3 position) {
        return biomes.contains(extent.getBiome(position));
    }

    @Override
    public Mask copy() {
        return new BiomeMask(getExtent(), biomes);
    }
}
