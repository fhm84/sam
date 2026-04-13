package de.halbmann.sam.business.ensembles.boundary;

import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.api.entity.shared.SortOrder;
import de.halbmann.sam.business.ensembles.entity.EnsembleEntity;
import de.halbmann.sam.core.entity.PaginatedEntities;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
public class EnsembleRepository implements PanacheRepositoryBase<EnsembleEntity, UUID> {

    public PaginatedEntities<EnsembleEntity> findEnsembleEntities(
            final PaginationRequest paginationRequest, final Map<String, Object> parameters) {
        final Map<String, Object> nonNullParams = parameters.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final String filter =
                nonNullParams.keySet().stream().map(o -> o + "=:" + o).collect(Collectors.joining(" and "));

        final Sort sort = prepareSort(paginationRequest);

        long totalItems;
        PanacheQuery<EnsembleEntity> query;
        if (nonNullParams.isEmpty()) {
            query = findAll(sort);
            totalItems = count();
        } else {
            query = find(filter, nonNullParams);
            totalItems = count(filter, nonNullParams);
        }
        final List<EnsembleEntity> ensembles = query.page(paginationRequest.getPage(), paginationRequest.getSize())
                .list();

        return new PaginatedEntities<>(ensembles, totalItems);
    }

    private Sort prepareSort(PaginationRequest paginationRequest) {
        if (paginationRequest.getSortBy() != null) {
            if (SortOrder.DESC == paginationRequest.getSortOrder()) {
                return Sort.descending(paginationRequest.getSortBy());
            } else {
                return Sort.ascending(paginationRequest.getSortBy());
            }
        }
        return Sort.ascending("name");
    }
}
