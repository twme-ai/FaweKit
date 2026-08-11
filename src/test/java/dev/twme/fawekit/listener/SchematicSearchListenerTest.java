package dev.twme.fawekit.listener;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SchematicSearchListenerTest {
    @Test
    void contiguousAndSubsequenceMatchesBeatMisses() {
        assertTrue(SchematicSearchListener.fuzzyScore("castle", "cast")
                < SchematicSearchListener.fuzzyScore("coastal_temple", "cast"));
        assertTrue(SchematicSearchListener.fuzzyScore("coastal_temple", "cast")
                < SchematicSearchListener.fuzzyScore("arena", "cast"));
    }
}
