package dev.twme.fawekit.math;

import com.sk89q.worldedit.math.BlockVector3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuarterRotationTest {
    @Test
    void mapsEveryOctantToRequestedOctant() {
        BlockVector3 from = BlockVector3.at(7, -2, 4);
        BlockVector3 to = BlockVector3.at(-1, 9, -5);
        QuarterRotation rotation = QuarterRotation.between(from, to);
        BlockVector3 result = rotation.apply(from);
        assertEquals(-1, Integer.signum(result.x()));
        assertEquals(1, Integer.signum(result.y()));
        assertEquals(-1, Integer.signum(result.z()));
    }

    @Test
    void identityWinsWhenDirectionsAlreadyMatch() {
        assertEquals(new QuarterRotation(0, 0, 0),
                QuarterRotation.between(BlockVector3.at(1, 2, 3), BlockVector3.at(8, 4, 2)));
    }
}
