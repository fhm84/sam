package de.halbmann.sam.business.boundary;

import de.halbmann.sam.business.entity.VoiceOptionEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class VoiceOptionRepository implements PanacheRepositoryBase<VoiceOptionEntity, UUID> {}
