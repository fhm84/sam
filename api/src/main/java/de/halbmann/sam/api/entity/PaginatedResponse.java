package de.halbmann.sam.api.entity;

import lombok.Data;

import java.util.List;

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
