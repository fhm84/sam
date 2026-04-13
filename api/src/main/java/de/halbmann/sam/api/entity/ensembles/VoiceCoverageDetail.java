package de.halbmann.sam.api.entity.ensembles;

import lombok.Data;

/**
 * Detailed coverage information for a single ensemble voice. Explains which instrumentation was
 * matched (if any), the quality of the match, and a human-readable explanation.
 */
@Data
public class VoiceCoverageDetail {

    /** Anzeige / Referenz */
    private String voiceId;

    private String voiceLabel;

    /** Fachliche Regeln */
    private boolean required;

    private int minCount;
    private int targetCount;

    /** Bewertung */
    private double weight;

    /** Ergebnis */
    private double effectiveCount; // z.B. 2.85

    private double score; // 0..1 (nach Baseline & Normalisierung)

    /** Erklärung für UI / Debug */
    private String explanation;

    // --------------------
    // Derived helpers
    // --------------------

    public boolean isMissingRequired() {
        return required && effectiveCount < minCount;
    }

    public boolean isPresent() {
        return effectiveCount >= 1.0;
    }
}
