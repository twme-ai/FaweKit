package dev.twme.fawekit.service;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SelectionStackServiceTest {
    @Test
    void stackSupportsNegativeIndexesAndUndoRedo() {
        SelectionStackService.History history = new SelectionStackService().history(java.util.UUID.randomUUID());
        history.push(region(1), 0);
        history.push(region(2), 0);
        assertEquals(1, history.get(-1).getMaximumPoint().x());
        history.delete(1);
        assertEquals(1, history.stack().size());
        history.undo();
        assertEquals(2, history.stack().size());
        history.redo();
        assertEquals(1, history.stack().size());
    }

    private static CuboidRegion region(int max) {
        return new CuboidRegion(BlockVector3.ZERO, BlockVector3.at(max, max, max));
    }
}
