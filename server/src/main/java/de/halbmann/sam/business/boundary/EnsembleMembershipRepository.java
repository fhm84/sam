package de.halbmann.sam.business.boundary;

import de.halbmann.sam.business.entity.EnsembleMembershipEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class EnsembleMembershipRepository implements PanacheRepositoryBase<EnsembleMembershipEntity, UUID> {}
