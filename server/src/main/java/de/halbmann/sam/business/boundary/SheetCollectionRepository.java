package de.halbmann.sam.business.boundary;

import de.halbmann.sam.api.entity.CollectionType;
import de.halbmann.sam.api.entity.PaginationRequest;
import de.halbmann.sam.business.entity.PaginatedEntities;
import de.halbmann.sam.business.entity.SheetCollectionEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class SheetCollectionRepository implements PanacheRepositoryBase<SheetCollectionEntity, UUID> {

    public PaginatedEntities<SheetCollectionEntity> findCollections(
            final PaginationRequest paginationRequest, final String nameFilter, final CollectionType typeFilter) {
        Sort sort = Sort.ascending("name");
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (nameFilter != null && !nameFilter.isEmpty()) {
            conditions.add("lower(name) like :name");
            params.put("name", "%" + nameFilter.toLowerCase() + "%");
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
}
