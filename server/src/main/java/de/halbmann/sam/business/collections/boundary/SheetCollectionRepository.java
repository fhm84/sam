package de.halbmann.sam.business.collections.boundary;

import de.halbmann.sam.api.entity.collections.CollectionType;
import de.halbmann.sam.api.entity.collections.CollectionVisibility;
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

    /** JPQL condition on alias {@code sc}: visible to everyone (no visibility set counts as the whole ensemble). */
    private static final String OPEN_TO_ALL = "(sc.visibility is null or sc.visibility = :openToAll)";

    public PaginatedEntities<SheetCollectionEntity> findCollections(
            final PaginationRequest paginationRequest,
            final String nameFilter,
            final CollectionType typeFilter,
            final boolean includeRestricted) {
        Sort sort = Sort.ascending("name");
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (!includeRestricted) {
            conditions.add("(visibility is null or visibility = :openToAll)");
            params.put("openToAll", CollectionVisibility.WHOLE_ENSEMBLE);
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

    public PaginatedEntities<SheetCollectionEntity> findBySheetId(
            UUID sheetId, PaginationRequest pagination, boolean includeRestricted) {
        String where =
                " FROM SheetCollectionEntity sc JOIN sc.items cs WHERE TYPE(cs) = SheetCollectionItemEntity AND TREAT(cs AS SheetCollectionItemEntity).sheet.id = :sheetId"
                        + (includeRestricted ? "" : " AND " + OPEN_TO_ALL);
        var countQuery = getEntityManager().createQuery("SELECT COUNT(DISTINCT sc)" + where, Long.class);
        var pageQuery = getEntityManager()
                .createQuery("SELECT DISTINCT sc" + where + " ORDER BY sc.name ASC", SheetCollectionEntity.class);
        countQuery.setParameter("sheetId", sheetId);
        pageQuery.setParameter("sheetId", sheetId);
        if (!includeRestricted) {
            countQuery.setParameter("openToAll", CollectionVisibility.WHOLE_ENSEMBLE);
            pageQuery.setParameter("openToAll", CollectionVisibility.WHOLE_ENSEMBLE);
        }
        List<SheetCollectionEntity> page = pageQuery
                .setFirstResult(pagination.getPage() * pagination.getSize())
                .setMaxResults(pagination.getSize())
                .getResultList();
        return new PaginatedEntities<>(page, countQuery.getSingleResult());
    }
}
