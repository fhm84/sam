package de.halbmann.sam.api.entity.members;

import lombok.Data;

/** Contact details stored per member — all fields are optional. */
@Data
public class MemberContact {

    String email;

    String mobile;

    /** Organisational notes only; no health data. */
    String notes;
}
