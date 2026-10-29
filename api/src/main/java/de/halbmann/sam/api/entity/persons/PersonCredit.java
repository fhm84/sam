package de.halbmann.sam.api.entity.persons;

import java.util.UUID;
import lombok.Data;

/** A sheet that credits a person, and how. A person credited twice on one sheet has two entries. */
@Data
public class PersonCredit {

    UUID sheetId;

    String title;

    String subtitle;

    CreditRole role;
}
