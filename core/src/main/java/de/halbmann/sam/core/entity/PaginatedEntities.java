package de.halbmann.sam.core.entity;

import java.util.List;

public record PaginatedEntities<T>(List<T> data, long totalCount) {}
