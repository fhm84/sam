package de.halbmann.sam.business.sheets.boundary;

import de.halbmann.sam.business.documents.entity.AttachmentEntity;
import de.halbmann.sam.business.sheets.entity.InstrumentationEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class InstrumentationRepository implements PanacheRepositoryBase<InstrumentationEntity, UUID> {

    public void removeAttachment(AttachmentEntity attachment) {
        find(
                        "SELECT i FROM InstrumentationEntity i JOIN i.attachments a WHERE a.id = :id",
                        Map.of("id", attachment.getId()))
                .list()
                .forEach(i -> i.getAttachments().remove(attachment));
    }
}
