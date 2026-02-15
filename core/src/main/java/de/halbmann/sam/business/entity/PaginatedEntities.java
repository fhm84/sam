package de.halbmann.sam.business.entity;

import java.util.List;

public record PaginatedEntities<T>(List<T> data, long totalCount) {}
