package de.halbmann.sam.api.entity.musicians;

import de.halbmann.sam.api.entity.shared.PaginationRequest;
import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;

/** Query parameters for the musician list endpoint. */
@Getter
@Setter
public class MusicianFilterRequest extends PaginationRequest {

    @QueryParam("name")
    private String name;
}
