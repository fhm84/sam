package de.halbmann.sam.core.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LikePatternsTest {

    @Test
    void startsWith_appendsWildcardToPlainPrefix() {
        assertEquals("ab%", LikePatterns.startsWith("ab"));
    }

    @Test
    void startsWith_escapesWildcardsAndEscapeCharacter() {
        assertEquals("\\_%", LikePatterns.startsWith("_"));
        assertEquals("100\\%%", LikePatterns.startsWith("100%"));
        assertEquals("a\\\\b%", LikePatterns.startsWith("a\\b"));
    }

    @Test
    void escapeClause_usesBackslash() {
        assertEquals(" ESCAPE '\\'", LikePatterns.ESCAPE_CLAUSE);
    }
}
