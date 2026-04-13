package de.halbmann.sam.api.entity.ensembles;

import de.halbmann.sam.api.entity.shared.PaginationRequest;
import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;

/**
 * Filter criteria for paginated ensemble queries.
 */
@Getter
@Setter
public class EnsembleFilterRequest extends PaginationRequest {

    /**
     * Filter by exact ensemble name.
     */
    @QueryParam("name")
    private String name;
}
