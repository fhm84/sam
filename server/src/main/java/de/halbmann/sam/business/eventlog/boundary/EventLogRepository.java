package de.halbmann.sam.business.eventlog.boundary;

import de.halbmann.sam.api.entity.eventlog.EventLogFilterRequest;
import de.halbmann.sam.business.eventlog.entity.EventLogEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
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
public class EventLogRepository implements PanacheRepositoryBase<EventLogEntity, UUID> {

    public record PagedResult(List<EventLogEntity> data, long totalCount) {}

    public PagedResult find(EventLogFilterRequest filter) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (filter.getEventTypes() != null && !filter.getEventTypes().isEmpty()) {
            conditions.add("eventType IN :eventTypes");
            params.put("eventTypes", filter.getEventTypes());
        }
        if (filter.getEntityType() != null && !filter.getEntityType().isBlank()) {
            conditions.add("entityType = :entityType");
            params.put("entityType", filter.getEntityType());
        }
        if (filter.getUserId() != null && !filter.getUserId().isBlank()) {
            conditions.add("userId = :userId");
            params.put("userId", filter.getUserId());
        }
        if (filter.getShareTokenId() != null) {
            conditions.add("shareTokenId = :shareTokenId");
            params.put("shareTokenId", filter.getShareTokenId());
        }

        String query = conditions.isEmpty() ? "" : String.join(" and ", conditions);
        Sort sort = Sort.descending("occurredAt");

        long total = conditions.isEmpty() ? count() : count(query, params);
        List<EventLogEntity> data = (conditions.isEmpty() ? findAll(sort) : find(query, sort, params))
                .page(Page.of(filter.getPage(), filter.getSize()))
                .list();

        return new PagedResult(data, total);
    }
}
