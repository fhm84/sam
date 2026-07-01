package de.halbmann.sam.core.controller;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HashingTest {

    @Test
    void knownEmptyStringHash() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", Hashing.sha256(""));
    }

    @Test
    void knownHelloHash() {
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", Hashing.sha256("hello"));
    }

    @Test
    void deterministic() {
        String input = "sam-fingerprint-test";
        assertEquals(Hashing.sha256(input), Hashing.sha256(input));
    }

    @Test
    void differentInputsDifferentHashes() {
        assertNotEquals(Hashing.sha256("a"), Hashing.sha256("b"));
    }

    @Test
    void outputIs64HexChars() {
        assertTrue(Hashing.sha256("anything").matches("[0-9a-f]{64}"));
    }
}
