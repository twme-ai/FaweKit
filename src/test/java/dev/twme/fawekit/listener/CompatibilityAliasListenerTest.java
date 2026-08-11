package dev.twme.fawekit.listener;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompatibilityAliasListenerTest {
    @Test
    void rewritesAliasesWithoutChangingArguments() {
        assertEquals("//stack 4 north", CompatibilityAliasListener.rewrite("//repeat 4 north"));
        assertEquals("//contract 2 up", CompatibilityAliasListener.rewrite("//unextend 2 up"));
        assertEquals("/tracemask stone", CompatibilityAliasListener.rewrite("//tracemask stone"));
        assertEquals("//drawsel", CompatibilityAliasListener.rewrite("//seldraw"));
        assertEquals("//download fast", CompatibilityAliasListener.rewrite("//upload fast"));
    }

    @Test
    void rewritesClearAndRotationForms() {
        assertEquals("//sel", CompatibilityAliasListener.rewrite("//sel clear"));
        assertEquals("//gmask", CompatibilityAliasListener.rewrite("//gmask CLEAR"));
        assertEquals("//rotate 90", CompatibilityAliasListener.rewrite("//rotate 90 clockwise"));
        assertEquals("//rotate -90", CompatibilityAliasListener.rewrite("//rotate 90 counterclockwise"));
    }
}
