package de.halbmann.sam.api.entity;

import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SheetFilterRequest extends PaginationRequest {

    @QueryParam("title")
    private String title;

}
