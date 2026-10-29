package de.halbmann.sam.api.entity.members;

import lombok.Data;

/** An instrument assigned to a member, including a flag indicating their primary instrument. */
@Data
public class MemberInstrument {

    String instrumentId;

    String instrumentName;

    boolean primary;
}
