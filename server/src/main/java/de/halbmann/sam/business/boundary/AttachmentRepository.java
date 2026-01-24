package de.halbmann.sam.business.boundary;

import de.halbmann.sam.api.entity.Attachment;
import de.halbmann.sam.business.controller.AttachmentMapper;
import de.halbmann.sam.business.entity.AttachmentEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class AttachmentRepository implements PanacheRepositoryBase<AttachmentEntity, UUID> {

  @Inject AttachmentMapper attachmentMapper;

  public Attachment addAttachment(final Attachment attachment) {
    AttachmentEntity attachmentEntity = attachmentMapper.fromDto(attachment);
    persistAndFlush(attachmentEntity);
    return attachmentMapper.toDto(attachmentEntity);
  }

  public Attachment addAttachment(final AttachmentEntity attachment) {
    persistAndFlush(attachment);
    return attachmentMapper.toDto(attachment);
  }
}
