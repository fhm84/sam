package de.halbmann.sam.business.members.boundary;

import de.halbmann.sam.api.entity.members.MemberFilterRequest;
import de.halbmann.sam.api.entity.shared.SortOrder;
import de.halbmann.sam.business.members.entity.MemberEntity;
import de.halbmann.sam.business.persons.boundary.PersonRepository;
import de.halbmann.sam.core.controller.LikePatterns;
import de.halbmann.sam.core.controller.SortFieldValidator;
import de.halbmann.sam.core.entity.PaginatedEntities;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Query;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class MemberRepository implements PanacheRepositoryBase<MemberEntity, UUID> {

    /** Members are sorted by the person's name only. */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name");

    public Optional<MemberEntity> findByUserId(String userId) {
        return find("userId = :userId", Map.of("userId", userId)).firstResultOptional();
    }

    /**
     * Members sorted by name, filtered by status and by a name search that matches like the person
     * search (substring, trigram word similarity, phonetic code).
     */
    public PaginatedEntities<MemberEntity> findPage(final MemberFilterRequest filter) {
        if (filter.getSortBy() != null) {
            SortFieldValidator.validate(filter.getSortBy(), ALLOWED_SORT_FIELDS);
        }
        final boolean descending = filter.getSortBy() != null && filter.getSortOrder() == SortOrder.DESC;
        if (filter.getName() == null || filter.getName().isBlank()) {
            return findPageWithoutNameSearch(filter, descending);
        }
        return searchByName(filter, descending);
    }

    private PaginatedEntities<MemberEntity> findPageWithoutNameSearch(
            final MemberFilterRequest filter, final boolean descending) {
        final Sort sort = descending ? Sort.descending("person.name") : Sort.ascending("person.name");
        final PanacheQuery<MemberEntity> query = filter.getStatus() == null
                ? findAll(sort)
                : find("membershipStatus = :status", sort, Map.of("status", filter.getStatus()));
        return new PaginatedEntities<>(
                query.page(filter.getPage(), filter.getSize()).list(), query.count());
    }

    @SuppressWarnings("unchecked")
    private PaginatedEntities<MemberEntity> searchByName(final MemberFilterRequest filter, final boolean descending) {
        final String where = PersonRepository.nameMatches("p.name", "p.name_phonetic")
                + (filter.getStatus() != null ? " AND m.membershipStatus = :status" : "");
        final String from = " FROM members m JOIN persons p ON p.id = m.id WHERE " + where;

        final long total = ((Number)
                        withSearchParameters(getEntityManager().createNativeQuery("SELECT COUNT(*)" + from), filter)
                                .getSingleResult())
                .longValue();
        final List<UUID> ids = withSearchParameters(
                        getEntityManager()
                                .createNativeQuery("SELECT m.id" + from + " ORDER BY p.name "
                                        + (descending ? "DESC" : "ASC") + ", m.id LIMIT :limit OFFSET :offset"),
                        filter)
                .setParameter("limit", filter.getSize())
                .setParameter("offset", (long) filter.getPage() * filter.getSize())
                .getResultList();
        if (ids.isEmpty()) {
            return new PaginatedEntities<>(List.of(), total);
        }
        final Map<UUID, MemberEntity> byId =
                find("id IN :ids", Map.of("ids", ids)).stream().collect(Collectors.toMap(MemberEntity::getId, e -> e));
        return new PaginatedEntities<>(
                ids.stream().map(byId::get).filter(Objects::nonNull).toList(), total);
    }

    private static Query withSearchParameters(final Query query, final MemberFilterRequest filter) {
        final String name = filter.getName().trim();
        query.setParameter("name", name).setParameter("namePattern", LikePatterns.escape(name));
        if (filter.getStatus() != null) {
            query.setParameter("status", filter.getStatus().name());
        }
        return query;
    }
}
