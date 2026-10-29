package de.halbmann.sam.api.entity.members;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Everything SAM stores about the calling member (GDPR, Art. 15): the full profile, personal data
 * included, and their ensemble seats.
 */
@Data
public class OwnMemberData {

    Member member;

    List<OwnSeat> seats = new ArrayList<>();
}
