package dev.twme.fawekit.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShortcutServiceTest {
    @Test
    void expandsNumberedAndDefaultParameters() {
        assertEquals("//replace air stone", ShortcutService.parameters("//replace air ${1}", new String[]{"stone"}));
        assertEquals("stone", ShortcutService.parameters("${1:-stone}", new String[0]));
        assertEquals("yes", ShortcutService.parameters("${1:+yes}", new String[]{"set"}));
        assertEquals("a b", ShortcutService.parameters("${@}", new String[]{"a", "b"}));
    }

    @Test
    void requiredParameterFailsClearly() {
        assertThrows(IllegalArgumentException.class,
                () -> ShortcutService.parameters("${1:?block required}", new String[0]));
    }
}
