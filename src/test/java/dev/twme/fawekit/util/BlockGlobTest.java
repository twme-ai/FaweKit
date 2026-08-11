package dev.twme.fawekit.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockGlobTest {
    @Test
    void matchesPosixStyleWildcards() {
        assertTrue(BlockGlob.matches("mud_brick_*", "mud_brick_slab"));
        assertTrue(BlockGlob.matches("*_brick_????", "mud_brick_wall"));
        assertFalse(BlockGlob.matches("mud_brick_*", "mud_bricks"));
    }
}
