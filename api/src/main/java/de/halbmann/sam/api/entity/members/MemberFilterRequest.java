package de.halbmann.sam.api.entity.members;

import de.halbmann.sam.api.entity.shared.PaginationRequest;
import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;

/** Query parameters for the member list endpoint. */
@Getter
@Setter
public class MemberFilterRequest extends PaginationRequest {

    /** Case-insensitive part of the member's name. */
    @QueryParam("name")
    private String name;

    @QueryParam("status")
    private MembershipStatus status;
}
