package de.halbmann.sam.security;

import io.quarkus.arc.Arc;
import org.hibernate.envers.RevisionListener;

public class SamRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        // InstanceHandle is AutoCloseable; for the @RequestScoped CurrentUserService close() is a no-op
        // (it only destroys @Dependent instances), so the request-scoped bean is left intact.
        try (var currentUser = Arc.container().instance(CurrentUserService.class)) {
            if (currentUser.isAvailable()) {
                ((SamRevision) revisionEntity).setUserId(currentUser.get().getUserId());
            }
        }
    }
}
