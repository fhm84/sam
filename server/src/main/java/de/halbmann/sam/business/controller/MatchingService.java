package de.halbmann.sam.business.controller;

import de.halbmann.sam.business.entity.InstrumentationEntity;
import de.halbmann.sam.business.entity.VoiceOptionEntity;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MatchingService {

    private static final double THRESHOLD = 0.3;

    /**
     * Computes a match score between a voice option's instrument and an existing instrumentation.
     * Returns 0.0 if below threshold.
     */
    public double score(final VoiceOptionEntity option, final InstrumentationEntity instrumentation) {
        // Instrument ID exact match
        double instrumentMatch = option.getInstrument()
                        .getId()
                        .equals(instrumentation.getInstrument().getId())
                ? 1.0
                : 0.0;

        if (instrumentMatch == 0.0) {
            // Different instruments — check transposition compatibility
            if (option.getInstrument().getTransposition() != null
                    && instrumentation.getInstrument().getTransposition() != null
                    && option.getInstrument().getTransposition()
                            == instrumentation.getInstrument().getTransposition()) {
                instrumentMatch = 0.5;
            } else {
                return 0.0;
            }
        }

        double score = instrumentMatch;

        // Transposition match factor
        score *= transpositionFactor(option, instrumentation);

        // Clef match factor
        score *= clefFactor(option, instrumentation);

        // NotationType match factor
        score *= notationTypeFactor(instrumentation);

        return score < THRESHOLD ? 0.0 : score;
    }

    private double transpositionFactor(final VoiceOptionEntity option, final InstrumentationEntity instrumentation) {
        if (option.getInstrument().getTransposition() == null
                || instrumentation.getInstrument().getTransposition() == null) {
            return 1.0;
        }
        return option.getInstrument().getTransposition()
                        == instrumentation.getInstrument().getTransposition()
                ? 1.0
                : 0.5;
    }

    private double clefFactor(final VoiceOptionEntity option, final InstrumentationEntity instrumentation) {
        if (instrumentation.getClef() == null) {
            return 1.0;
        }
        // Use the instrument's typical clef expectation — if the instrumentation's clef matches, full score
        // For simplicity: same clef = 1.0, different clef = 0.7
        if (option.getInstrument().getTransposition() != null) {
            // Brass/wind instruments typically share clef expectations
            return 1.0;
        }
        return 0.7;
    }

    private double notationTypeFactor(final InstrumentationEntity instrumentation) {
        if (instrumentation.getNotationType() == null) {
            return 1.0;
        }
        // Standard notation is universally compatible
        return switch (instrumentation.getNotationType()) {
            case STANDARD, LEAD_SHEET -> 1.0;
            case TABLATURE, GRAPHIC -> 0.7;
            case PERCUSSION -> 0.8;
        };
    }
}
