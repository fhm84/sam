package de.halbmann.sam.business.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class Hashing {

    private Hashing() {}

    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to calculate SHA-256", e);
        }
    }
}
