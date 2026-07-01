package de.halbmann.sam.business.ensembles.boundary;

import de.halbmann.sam.business.ensembles.entity.EnsembleMembershipEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class EnsembleMembershipRepository implements PanacheRepositoryBase<EnsembleMembershipEntity, UUID> {}
