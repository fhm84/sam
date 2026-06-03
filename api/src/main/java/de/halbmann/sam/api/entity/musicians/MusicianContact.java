package de.halbmann.sam.api.entity.musicians;

import lombok.Data;

/** Contact details stored per musician — all fields are optional. */
@Data
public class MusicianContact {

    String email;

    String mobile;

    String notes;
}
