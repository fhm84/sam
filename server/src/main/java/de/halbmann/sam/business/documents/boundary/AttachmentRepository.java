package de.halbmann.sam.business.documents.boundary;

import de.halbmann.sam.business.documents.entity.AttachmentEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class AttachmentRepository implements PanacheRepositoryBase<AttachmentEntity, UUID> {}
