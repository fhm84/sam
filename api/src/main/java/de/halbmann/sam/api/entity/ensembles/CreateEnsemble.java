package de.halbmann.sam.api.entity.ensembles;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request data for creating a new ensemble. Voices are added separately via the voices sub-resource
 * after ensemble creation.
 */
@Data
@EqualsAndHashCode
public class CreateEnsemble {

    /**
     * Display name of the ensemble.
     */
    @NotBlank
    String name;

    /**
     * Optional description providing context about the ensemble configuration.
     */
    String description;
}
