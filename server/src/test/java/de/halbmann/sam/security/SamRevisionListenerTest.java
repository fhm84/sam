package de.halbmann.sam.security;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.quarkus.arc.ClientProxy;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SamRevisionListenerTest {

    @Inject
    CurrentUserService currentUserService;

    @Test
    @ActivateRequestContext
    void newRevision_leavesRequestScopedCurrentUserServiceIntact() {
        final Object before = ((ClientProxy) currentUserService).arc_contextualInstance();
        final SamRevision revision = new SamRevision();

        new SamRevisionListener().newRevision(revision);

        // Closing the InstanceHandle must not destroy the request-scoped bean for the rest of the request.
        assertSame(before, ((ClientProxy) currentUserService).arc_contextualInstance());
        // Anonymous request in the test profile: no user id to record.
        assertNull(revision.getUserId());
    }
}
