package de.halbmann.sam.business.instruments.boundary;

import de.halbmann.sam.api.entity.instruments.InstrumentMatch;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.api.entity.shared.SortOrder;
import de.halbmann.sam.business.instruments.entity.InstrumentEntity;
import de.halbmann.sam.core.controller.SortFieldValidator;
import de.halbmann.sam.core.entity.PaginatedEntities;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
public class InstrumentRepository implements PanacheRepositoryBase<InstrumentEntity, String> {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "name", "displayName", "transposition");

    /**
     * Returns up to {@code limit} instruments whose name is similar to {@code name},
     * ordered by trigram similarity (best match first). Only instruments with a similarity
     * score ≥ {@code threshold} are included. Requires the {@code pg_trgm} extension.
     */
    @SuppressWarnings("unchecked")
    public List<InstrumentMatch> findCandidates(String name, double threshold, int limit) {
        List<Object[]> rows = getEntityManager()
                .createNativeQuery(
                        "SELECT id, name, displayname, transposition, similarity(lower(name), lower(:name)) AS score"
                                + " FROM instruments"
                                + " WHERE similarity(lower(name), lower(:name)) >= :threshold"
                                + " ORDER BY score DESC"
                                + " LIMIT :limit")
                .setParameter("name", name)
                .setParameter("threshold", threshold)
                .setParameter("limit", limit)
                .getResultList();
        return rows.stream()
                .map(r -> new InstrumentMatch(
                        (String) r[0], (String) r[1], (String) r[2], (String) r[3], ((Number) r[4]).doubleValue()))
                .toList();
    }

    public Optional<InstrumentEntity> findByName(String name) {
        try {
            return Optional.ofNullable(
                    find("lower(name) = lower(:name)", Map.of("name", name)).firstResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public PaginatedEntities<InstrumentEntity> findInstrumentEntities(
            final PaginationRequest paginationRequest, final Map<String, Object> parameters) {
        final Map<String, Object> nonNullParams = parameters.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        final String filter =
                nonNullParams.keySet().stream().map(o -> o + "=:" + o).collect(Collectors.joining(" and "));

        final Sort sort = prepareSort(paginationRequest);

        long totalItems;
        PanacheQuery<InstrumentEntity> query;
        if (nonNullParams.isEmpty()) {
            query = findAll(sort);
            totalItems = count();
        } else {
            query = find(filter, nonNullParams);
            totalItems = count(filter, nonNullParams);
        }
        final List<InstrumentEntity> instruments = query.page(paginationRequest.getPage(), paginationRequest.getSize())
                .list();

        return new PaginatedEntities<>(instruments, totalItems);
    }

    private Sort prepareSort(PaginationRequest paginationRequest) {
        if (paginationRequest.getSortBy() != null) {
            SortFieldValidator.validate(paginationRequest.getSortBy(), ALLOWED_SORT_FIELDS);
            if (SortOrder.DESC == paginationRequest.getSortOrder()) {
                return Sort.descending(paginationRequest.getSortBy());
            } else {
                return Sort.ascending(paginationRequest.getSortBy());
            }
        }
        return Sort.ascending("name");
    }
}
