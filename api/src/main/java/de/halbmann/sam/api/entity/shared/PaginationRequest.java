package de.halbmann.sam.api.entity.shared;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import lombok.Getter;
import lombok.Setter;

/**
 * Base JAX-RS {@code @BeanParam} class for paginated query endpoints. Provides {@code page},
 * {@code size}, {@code sortBy}, and {@code sortOrder} query parameters. Pass {@code size=-1} to
 * disable pagination and return all records.
 */
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
