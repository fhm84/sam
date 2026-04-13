package de.halbmann.sam.api.entity.shared;

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

    public int getSize() {
        // to "disable" paging, we just simply set the size to a negative value -> this is then mapped
        // to Integer.MAX_VALUE
        return size < 0 ? Integer.MAX_VALUE : size;
    }
}
