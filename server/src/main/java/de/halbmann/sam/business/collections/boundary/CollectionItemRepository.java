package de.halbmann.sam.business.collections.boundary;

import de.halbmann.sam.business.collections.entity.CollectionItemEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class CollectionItemRepository implements PanacheRepositoryBase<CollectionItemEntity, UUID> {}
