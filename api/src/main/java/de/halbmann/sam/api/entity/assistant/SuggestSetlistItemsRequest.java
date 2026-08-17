package de.halbmann.sam.api.entity.assistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Free-text goal/constraints for the AI setlist assistant's program builder, e.g. "45 min
 * opener, upbeat, avoid two marches in a row".
 */
@Data
public class SuggestSetlistItemsRequest {

    @NotBlank
    @Size(max = 2000)
    String goal;
}
