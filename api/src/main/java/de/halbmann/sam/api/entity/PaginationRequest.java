package de.halbmann.sam.api.entity;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaginationRequest {

    @QueryParam("page")
    private int page = 0;

    @QueryParam("size")
    private int size = 10;

    @QueryParam("sortOrder")
    @DefaultValue("ASC")
    private SortOrder sortOrder = SortOrder.ASC;

    @QueryParam("sortBy")
    private String[] sortBy;

}
