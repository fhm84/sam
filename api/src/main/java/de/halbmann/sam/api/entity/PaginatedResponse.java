package de.halbmann.sam.api.entity;

import lombok.Data;

import java.util.List;

@Data
public class PaginatedResponse<T> {

    private List<T> data;

    private int size;
    private long totalCount;

}