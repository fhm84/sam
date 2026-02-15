package de.halbmann.sam.business.controller;

import lombok.Getter;

public final class FingerprintService {

    @Getter
    private static final int FINGERPRINT_VERSION = 1;

    public static String sheetFingerprint(String title, String publisher, String composer, String arranger) {
        String normalizedTitle = TextNormalizer.normalize(title);
        String normalizedPublisher = TextNormalizer.normalizeMusician(publisher);
        String normalizedComposer = TextNormalizer.normalizeMusician(composer);
        String normalizedArranger = TextNormalizer.normalizeMusician(arranger);

        String canonical = String.join(
                "|",
                "v" + FINGERPRINT_VERSION,
                normalizedTitle,
                normalizedPublisher,
                normalizedComposer,
                normalizedArranger);

        return Hashing.sha256(canonical);
    }

    public static String instrumentationFingerprint() {
        // FIXME: implement
        String canonical = String.join("|", "v" + FINGERPRINT_VERSION);

        return Hashing.sha256(canonical);
    }
}
