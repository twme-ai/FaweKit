package dev.twme.fawekit.math;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public record QuarterRotation(int x, int y, int z) {
    public static QuarterRotation between(BlockVector3 from, BlockVector3 to) {
        BlockVector3 expected = signs(to);
        return all().stream()
                .filter(rotation -> signs(rotation.apply(from)).equals(expected))
                .min(Comparator.comparingInt(QuarterRotation::cost))
                .orElseThrow(() -> new IllegalArgumentException("The two selections do not define compatible directions."));
    }

    public AffineTransform transform() {
        return new AffineTransform().rotateX(x * 90.0).rotateY(y * 90.0).rotateZ(z * 90.0);
    }

    BlockVector3 apply(BlockVector3 vector) {
        BlockVector3 result = vector;
        for (int i = 0; i < x; i++) {
            result = BlockVector3.at(result.x(), -result.z(), result.y());
        }
        for (int i = 0; i < y; i++) {
            result = BlockVector3.at(result.z(), result.y(), -result.x());
        }
        for (int i = 0; i < z; i++) {
            result = BlockVector3.at(-result.y(), result.x(), result.z());
        }
        return result;
    }

    private int cost() {
        return quarterCost(x) + quarterCost(y) + quarterCost(z);
    }

    private static int quarterCost(int turns) {
        return Math.min(turns, 4 - turns);
    }

    private static BlockVector3 signs(BlockVector3 vector) {
        return BlockVector3.at(Integer.signum(vector.x()), Integer.signum(vector.y()), Integer.signum(vector.z()));
    }

    private static List<QuarterRotation> all() {
        return IntStream.range(0, 64)
                .mapToObj(value -> new QuarterRotation(value / 16, value / 4 % 4, value % 4))
                .toList();
    }
}
