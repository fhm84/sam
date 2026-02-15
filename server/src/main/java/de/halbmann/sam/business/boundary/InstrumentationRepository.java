package de.halbmann.sam.business.boundary;

import de.halbmann.sam.business.entity.InstrumentationEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class InstrumentationRepository implements PanacheRepositoryBase<InstrumentationEntity, UUID> {}
