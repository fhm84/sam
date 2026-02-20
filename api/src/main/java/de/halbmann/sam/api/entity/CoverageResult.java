package de.halbmann.sam.api.entity;

import java.time.Instant;
import java.util.List;
import lombok.Data;

/**
 * Result of evaluating a sheet music's instrumentations against an ensemble definition. Contains an
 * overall score, a status classification, and a per-voice breakdown explaining how each voice was
 * matched.
 */
@Data
public class CoverageResult {

    /**
     * Referenzen
     */
    private String sheetMusicId;

    private String ensembleId;

    /**
     * Zeitstempel
     */
    private Instant evaluatedAt = Instant.now();

    /**
     * Aggregierte Bewertung
     */
    private double coverageScore; // 0..1 (gewichteter Mittelwert)

    /**
     * Detailergebnisse
     */
    private List<VoiceCoverageDetail> details;

    /**
     * Vorberechnet im Service
     */
    private boolean missingRequired;

    // --------------------
    // Core decisions
    // --------------------

    /**
     * Binäre Spielbarkeitsentscheidung.
     * TRUE, wenn keine Pflichtstimme fehlt.
     */
    public boolean isPlayable() {
        return !missingRequired;
    }

    /**
     * UX-/Anzeige-Logik (Ampel).
     * NICHT für fachliche Entscheidungen verwenden.
     */
    public CoverageStatus getStatus() {
        if (!isPlayable()) {
            return CoverageStatus.INCOMPLETE;
        }
        if (coverageScore >= 0.85) {
            return CoverageStatus.COMPLETE;
        }
        return CoverageStatus.PLAYABLE;
    }

    // --------------------
    // Convenience helpers
    // --------------------

    public long getMissingRequiredCount() {
        return details.stream().filter(VoiceCoverageDetail::isMissingRequired).count();
    }
}
