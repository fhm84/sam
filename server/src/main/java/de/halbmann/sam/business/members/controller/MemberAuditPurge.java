package de.halbmann.sam.business.members.controller;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.UUID;

/**
 * Deletes the audit history of an erased member (GDPR, Art. 17): old contact details, the login
 * link and ensemble seats would otherwise stay in the {@code _aud} tables forever.
 *
 * <p>Envers writes the deletion's own audit rows when the transaction completes (and the validity
 * strategy updates the previous rows), so the purge runs afterwards, in a transaction of its own.
 */
@ApplicationScoped
public class MemberAuditPurge {

    @Inject
    EntityManager entityManager;

    void onErased(@Observes(during = TransactionPhase.AFTER_SUCCESS) final MemberErased event) {
        QuarkusTransaction.requiringNew().run(() -> purge(event));
    }

    void purge(final MemberErased event) {
        delete("delete from ensemble_memberships_aud where member_id = :id", event.memberId());
        delete("delete from member_instruments_aud where member_id = :id", event.memberId());
        delete("delete from members_aud where id = :id", event.memberId());
        if (event.personDeleted()) {
            delete("delete from persons_aud where id = :id", event.memberId());
        }
    }

    private void delete(final String sql, final UUID id) {
        entityManager.createNativeQuery(sql).setParameter("id", id).executeUpdate();
    }
}
