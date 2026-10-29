package de.halbmann.sam.business.members.controller;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Deletes the audit history of erased members (GDPR, Art. 17): old contact details, the login link
 * and ensemble seats would otherwise stay in the {@code _aud} tables forever.
 *
 * <p>The purge is state-based: it deletes the history of every member who no longer exists, not
 * just of the one deleted last. So it can run again at any time and catches up on a purge that
 * failed. It runs after each member deletion (Envers writes the deletion's own audit rows when the
 * transaction completes, so it has to wait for the commit) and at startup.
 */
@Slf4j
@ApplicationScoped
public class MemberAuditPurge {

    @Inject
    EntityManager entityManager;

    void onErased(@Observes(during = TransactionPhase.AFTER_SUCCESS) final MemberErased event) {
        purgeQuietly();
    }

    void onStartup(@Observes final StartupEvent event) {
        purgeQuietly();
    }

    /** Deletes the audit rows of members that no longer exist; returns the number of rows deleted. */
    public int purgeErasedMembers() {
        return QuarkusTransaction.requiringNew().call(() -> {
            // The person's history only where the person went with the member: credited persons stay
            int rows = delete("delete from persons_aud a where not exists (select 1 from persons p where p.id = a.id)"
                    + " and exists (select 1 from members_aud m where m.id = a.id)");
            rows += delete("delete from ensemble_memberships_aud a"
                    + " where not exists (select 1 from members m where m.id = a.member_id)");
            rows += delete("delete from member_instruments_aud a"
                    + " where not exists (select 1 from members m where m.id = a.member_id)");
            rows += delete("delete from members_aud a where not exists (select 1 from members m where m.id = a.id)");
            return rows;
        });
    }

    private void purgeQuietly() {
        try {
            int rows = purgeErasedMembers();
            if (rows > 0) {
                log.atInfo().log("Purged {} audit rows of erased members", rows);
            }
        } catch (RuntimeException e) {
            // Nothing is lost: the next deletion or the next start purges the same rows
            log.atWarn().setCause(e).log("Purging the audit history of erased members failed");
        }
    }

    private int delete(final String sql) {
        return entityManager.createNativeQuery(sql).executeUpdate();
    }
}
