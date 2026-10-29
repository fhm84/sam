package de.halbmann.sam.api.entity.persons;

import de.halbmann.sam.api.entity.shared.PaginationRequest;
import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;

/** Query parameters for the person list endpoint. */
@Getter
@Setter
public class PersonFilterRequest extends PaginationRequest {

    @QueryParam("name")
    private String name;
}
