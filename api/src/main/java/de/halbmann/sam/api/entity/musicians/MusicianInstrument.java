package de.halbmann.sam.api.entity.musicians;

import lombok.Data;

/** An instrument assigned to a musician, including a flag indicating their primary instrument. */
@Data
public class MusicianInstrument {

    String instrumentId;

    String instrumentName;

    boolean primary;
}
