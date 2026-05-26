package de.halbmann.sam.api.entity.musicians;

import lombok.Data;

@Data
public class MusicianInstrument {

    String instrumentId;

    String instrumentName;

    boolean primary;
}
