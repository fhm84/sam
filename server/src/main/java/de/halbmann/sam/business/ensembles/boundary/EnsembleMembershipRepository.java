package de.halbmann.sam.business.ensembles.boundary;

import de.halbmann.sam.business.ensembles.entity.EnsembleMembershipEntity;
import de.halbmann.sam.business.members.entity.MemberEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class EnsembleMembershipRepository implements PanacheRepositoryBase<EnsembleMembershipEntity, UUID> {

    /**
     * The instruments a member plays across all their seats, each once. Seats without an
     * instrument (e.g. the conductor's) are skipped.
     */
    public List<String> instrumentIdsOf(final MemberEntity member) {
        return getEntityManager()
                .createQuery(
                        "select distinct em.instrument.id from EnsembleMembershipEntity em"
                                + " where em.member = :member and em.instrument is not null",
                        String.class)
                .setParameter("member", member)
                .getResultList();
    }
}
