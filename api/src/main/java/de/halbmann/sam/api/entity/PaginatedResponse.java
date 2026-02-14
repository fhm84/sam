package de.halbmann.sam.api.entity;

import java.util.List;
import lombok.Data;

/**
 * Generic wrapper for a paginated response (data list) including some metadata like size and
 * totalCount.
 *
 * @param <T>
 */
@Data
public class PaginatedResponse<T> {

    private List<T> data;

    private int page;
    private int size;
    private long totalCount;
}
