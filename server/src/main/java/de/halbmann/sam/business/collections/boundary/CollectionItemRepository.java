package de.halbmann.sam.business.collections.boundary;

import de.halbmann.sam.business.collections.entity.CollectionItemEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CollectionItemRepository implements PanacheRepositoryBase<CollectionItemEntity, UUID> {

    /** The item, if it belongs to this collection. */
    public Optional<CollectionItemEntity> findInCollection(UUID collectionId, UUID itemId) {
        return getEntityManager()
                .createQuery(
                        "SELECT cs FROM SheetCollectionEntity sc JOIN sc.items cs"
                                + " WHERE sc.id = :collectionId AND cs.id = :itemId",
                        CollectionItemEntity.class)
                .setParameter("collectionId", collectionId)
                .setParameter("itemId", itemId)
                .getResultStream()
                .findFirst();
    }
}
