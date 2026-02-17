package de.halbmann.sam.api.entity;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * An instrument that can fill a particular ensemble voice. Each option links to a canonical
 * instrument and carries a type (primary/alternate/fallback) and a scoring factor that affects the
 * coverage calculation.
 *
 * <p>For example, a "1. Trumpet" voice might have:
 * <ul>
 *   <li>TRUMPET_BB as PRIMARY with factor 1.0</li>
 *   <li>FLUGELHORN_BB as ALTERNATE with factor 0.8</li>
 *   <li>CLARINET_BB as FALLBACK with factor 0.4</li>
 * </ul>
 */
@Data
@EqualsAndHashCode
public class VoiceOption {

    /**
     * Unique identifier of the voice option.
     */
    UUID id;

    /**
     * Canonical instrument ID (e.g. "TRUMPET_BB", "TUBA_C").
     */
    @NotBlank
    String instrumentId;

    /**
     * Priority level of this option (PRIMARY, ALTERNATE, FALLBACK).
     */
    VoiceOptionType type;

    /**
     * Scoring factor applied when this option is used (0.0 - 1.0). A primary option typically
     * has factor 1.0, while alternates and fallbacks have lower values.
     */
    double factor;
}
