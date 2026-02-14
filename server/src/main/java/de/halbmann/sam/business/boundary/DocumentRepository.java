package de.halbmann.sam.business.boundary;

import de.halbmann.sam.business.entity.DocumentEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DocumentRepository implements PanacheRepositoryBase<DocumentEntity, UUID> {

    public Optional<DocumentEntity> findBySha256(String sha256) {
        return find("sha256", sha256).firstResultOptional();
    }

    public void incrementRefCount(DocumentEntity document) {
        document.setRefCount(document.getRefCount() + 1);
        persist(document); // Panache automatically handles merge if needed
    }

    public void decrementRefCount(DocumentEntity document) {
        document.setRefCount(document.getRefCount() - 1);
        if (document.getRefCount() <= 0) {
            delete(document); // deletes from DB
            // TODO: delete physical file from FileSystemWrapper
        } else {
            persist(document);
        }
    }

    public List<DocumentEntity> findUnlinked() {
        // TODO: add pagination!
        return find("refCount = 0").list();
    }
}
