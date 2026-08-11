package dev.twme.fawekit.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClipboardCommandTest {
    @Test
    void parsesAbsoluteAndPercentageSizes() {
        assertEquals(5, ClipboardCommand.size("5", 20));
        assertEquals(10, ClipboardCommand.size("50%", 20));
        assertEquals(1, ClipboardCommand.size("0%", 20));
        assertEquals(25, ClipboardCommand.size("~5", 20));
        assertEquals(30, ClipboardCommand.size("~50%", 20));
        assertEquals(20, ClipboardCommand.size("~", 20));
    }

    @Test
    void nearestNeighborKeepsBothEdges() {
        assertEquals(0, ClipboardCommand.nearest(0, 5, 3));
        assertEquals(1, ClipboardCommand.nearest(2, 5, 3));
        assertEquals(2, ClipboardCommand.nearest(4, 5, 3));
    }
}
