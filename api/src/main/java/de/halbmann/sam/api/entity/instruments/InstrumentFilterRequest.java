package de.halbmann.sam.api.entity.instruments;

import de.halbmann.sam.api.entity.shared.PaginationRequest;
import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstrumentFilterRequest extends PaginationRequest {

    @QueryParam("name")
    private String name;

    @QueryParam("transposition")
    private InstrumentTransposing transposition;
}
