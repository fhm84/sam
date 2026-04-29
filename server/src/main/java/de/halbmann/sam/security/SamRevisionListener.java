package de.halbmann.sam.security;

import io.quarkus.arc.Arc;
import org.hibernate.envers.RevisionListener;

public class SamRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        var currentUser = Arc.container().instance(CurrentUserService.class);
        if (currentUser.isAvailable()) {
            ((SamRevision) revisionEntity).setUserId(currentUser.get().getUserId());
        }
    }
}
