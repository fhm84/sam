package de.halbmann.sam.business.shares.boundary;

import de.halbmann.sam.business.shares.entity.ShareEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Data-access layer for {@link ShareEntity}. */
@ApplicationScoped
public class ShareRepository implements PanacheRepositoryBase<ShareEntity, UUID> {

    /** Returns all shares created by the given user, newest first. */
    public List<ShareEntity> findByCreator(String userId) {
        return find("creatorUserId = ?1 order by createdAt desc", userId).list();
    }

    /**
     * Looks up a share by token and returns it only if it is still valid (not expired, not
     * revoked). Returns {@link Optional#empty()} for unknown, expired, or revoked tokens.
     */
    public Optional<ShareEntity> findValid(UUID id) {
        return findByIdOptional(id).filter(ShareEntity::isValid);
    }
}
