package de.halbmann.sam.business.sheets.boundary;

import de.halbmann.sam.business.sheets.entity.VoiceOptionEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class VoiceOptionRepository implements PanacheRepositoryBase<VoiceOptionEntity, UUID> {}
