package de.halbmann.sam.business.collections.boundary;

import de.halbmann.sam.api.entity.collections.CollectionType;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.business.collections.entity.SheetCollectionEntity;
import de.halbmann.sam.core.controller.LikePatterns;
import de.halbmann.sam.core.entity.PaginatedEntities;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class SheetCollectionRepository implements PanacheRepositoryBase<SheetCollectionEntity, UUID> {

    public PaginatedEntities<SheetCollectionEntity> findCollections(
            final PaginationRequest paginationRequest,
            final String nameFilter,
            final CollectionType typeFilter,
            final CollectionReader reader) {
        Sort sort = Sort.ascending("name");
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        String visible = reader.condition("", params);
        if (visible != null) {
            conditions.add(visible);
        }
        if (nameFilter != null && !nameFilter.isEmpty()) {
            conditions.add("lower(name) like :name" + LikePatterns.ESCAPE_CLAUSE);
            params.put("name", LikePatterns.contains(nameFilter.toLowerCase(Locale.ROOT)));
        }
        if (typeFilter != null) {
            conditions.add("type = :type");
            params.put("type", typeFilter);
        }

        long totalItems;
        List<SheetCollectionEntity> entities;

        if (params.isEmpty()) {
            entities = findAll(sort)
                    .page(paginationRequest.getPage(), paginationRequest.getSize())
                    .list();
            totalItems = count();
        } else {
            String filter = String.join(" and ", conditions);
            entities = find(filter, sort, params)
                    .page(paginationRequest.getPage(), paginationRequest.getSize())
                    .list();
            totalItems = count(filter, params);
        }
        return new PaginatedEntities<>(entities, totalItems);
    }

    /** Collections containing the sheet, limited to what {@code reader} may see ({@code null}: all of them). */
    public PaginatedEntities<SheetCollectionEntity> findBySheetId(
            UUID sheetId, PaginationRequest pagination, CollectionReader reader) {
        Map<String, Object> params = new HashMap<>();
        params.put("sheetId", sheetId);
        String visible = reader == null ? null : reader.condition("sc.", params);
        String where =
                " FROM SheetCollectionEntity sc JOIN sc.items cs WHERE TYPE(cs) = SheetCollectionItemEntity AND TREAT(cs AS SheetCollectionItemEntity).sheet.id = :sheetId"
                        + (visible == null ? "" : " AND " + visible);
        var countQuery = getEntityManager().createQuery("SELECT COUNT(DISTINCT sc)" + where, Long.class);
        var pageQuery = getEntityManager()
                .createQuery("SELECT DISTINCT sc" + where + " ORDER BY sc.name ASC", SheetCollectionEntity.class);
        params.forEach((name, value) -> {
            countQuery.setParameter(name, value);
            pageQuery.setParameter(name, value);
        });
        List<SheetCollectionEntity> page = pageQuery
                .setFirstResult(pagination.getPage() * pagination.getSize())
                .setMaxResults(pagination.getSize())
                .getResultList();
        return new PaginatedEntities<>(page, countQuery.getSingleResult());
    }
}
