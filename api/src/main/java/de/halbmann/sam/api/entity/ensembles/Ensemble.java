package de.halbmann.sam.api.entity.ensembles;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents an ensemble definition (e.g. "Brass Quintet", "Big Band", "Blaskapelle"). An ensemble
 * defines a set of weighted voices, each specifying which instruments can fill that role. Used to
 * evaluate whether a piece of sheet music has all the parts needed for a given ensemble.
 */
@Data
@EqualsAndHashCode
public class Ensemble {

    /**
     * Unique identifier of the ensemble.
     */
    UUID id;

    /**
     * Display name of the ensemble (e.g. "Brass Quintet", "Blaskapelle 20-piece").
     */
    @NotBlank
    String name;

    /**
     * Optional description providing context about the ensemble configuration.
     */
    String description;

    /**
     * The voices (instrument slots) that make up this ensemble.
     */
    List<EnsembleVoice> voices;
}
