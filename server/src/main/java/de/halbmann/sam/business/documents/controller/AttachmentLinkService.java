package de.halbmann.sam.business.documents.controller;

import de.halbmann.sam.api.entity.documents.*;
import de.halbmann.sam.business.documents.boundary.AttachmentRepository;
import de.halbmann.sam.business.documents.boundary.DocumentRepository;
import de.halbmann.sam.business.documents.entity.AttachmentEntity;
import de.halbmann.sam.business.documents.entity.DocumentEntity;
import de.halbmann.sam.business.sheets.boundary.InstrumentationRepository;
import de.halbmann.sam.business.sheets.boundary.SheetRepository;
import de.halbmann.sam.business.sheets.entity.InstrumentationEntity;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import de.halbmann.sam.core.exception.EntityNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.*;

@ApplicationScoped
@Transactional
public class AttachmentLinkService {

    @Inject
    DocumentStore documentStore;

    @Inject
    DocumentRepository documentRepository;

    @Inject
    AttachmentRepository attachmentRepository;

    @Inject
    SheetRepository sheetRepository;

    @Inject
    InstrumentationRepository instrumentationRepository;

    @Inject
    AttachmentMapper attachmentMapper;

    @Inject
    PdfMetadataEnricher pdfMetadataEnricher;

    public List<Attachment> loadAttachmentsByInstrumentation(String instrumentationId) {
        if (instrumentationId == null) {
            return List.of();
        }
        InstrumentationEntity instrumentation = instrumentationRepository.findById(UUID.fromString(instrumentationId));
        if (instrumentation != null) {
            return instrumentation.getAttachments().stream()
                    .map(attachmentMapper::toDto)
                    .toList();
        }
        return List.of();
    }

    public DocumentDownload loadAttachmentByInstrumentation(String instrumentationId, String docId) {
        if (instrumentationId == null) {
            return null;
        }
        InstrumentationEntity instrumentation = instrumentationRepository.findById(UUID.fromString(instrumentationId));
        if (instrumentation != null) {
            Optional<UUID> attachmentId = instrumentation.getAttachments().stream()
                    .map(AttachmentEntity::getId)
                    .filter(id -> id.toString().equals(docId))
                    .findFirst();
            DocumentDownload download = documentStore.loadAttachment(
                    attachmentId.map(String::valueOf).orElse(null));
            if (download == null) {
                return null;
            }
            return pdfMetadataEnricher.enrich(download, instrumentation.getSheet(), instrumentation);
        }
        return null;
    }

    public List<Attachment> loadAttachmentsBySheet(String sheetId) {
        if (sheetId == null) {
            return List.of();
        }
        SheetMusicEntity sheetMusic = sheetRepository.findById(UUID.fromString(sheetId));
        if (sheetMusic != null) {
            return sheetMusic.getAttachments().stream()
                    .map(attachmentMapper::toDto)
                    .toList();
        }
        return List.of();
    }

    public DocumentDownload loadAttachmentBySheet(String sheetId, String docId) {
        if (sheetId == null) {
            return null;
        }
        SheetMusicEntity sheetMusic = sheetRepository.findById(UUID.fromString(sheetId));
        if (sheetMusic != null) {
            Optional<UUID> attachmentId = sheetMusic.getAttachments().stream()
                    .map(AttachmentEntity::getId)
                    .filter(id -> id.toString().equals(docId))
                    .findFirst();
            DocumentDownload download = documentStore.loadAttachment(
                    attachmentId.map(String::valueOf).orElse(null));
            if (download == null) {
                return null;
            }
            return pdfMetadataEnricher.enrich(download, sheetMusic, null);
        }
        return null;
    }

    public List<AttachmentEntity> loadAttachmentEntitiesBySheet(String sheetId, AttachmentType type) {
        SheetMusicEntity sheet = sheetRepository.findById(UUID.fromString(sheetId));
        if (sheet == null) {
            return List.of();
        }
        return sheet.getAttachments().stream()
                .filter(a -> type == null || type == a.getType())
                .toList();
    }

    public List<AttachmentEntity> loadAttachmentEntitiesByInstrumentation(
            String instrumentationId, AttachmentType type) {
        InstrumentationEntity instr = instrumentationRepository.findById(UUID.fromString(instrumentationId));
        if (instr == null) {
            return List.of();
        }
        return instr.getAttachments().stream()
                .filter(a -> type == null || type == a.getType())
                .toList();
    }

    public List<AttachmentEntity> loadAttachmentEntitiesBySheetInstrumentations(String sheetId, AttachmentType type) {
        SheetMusicEntity sheet = sheetRepository.findById(UUID.fromString(sheetId));
        if (sheet == null) {
            return List.of();
        }
        List<AttachmentEntity> result = new ArrayList<>();
        sheet.getAttachments().stream()
                .filter(a -> type == null || type == a.getType())
                .forEach(result::add);
        sheet.getInstrumentations().stream()
                .sorted(Comparator.comparing(
                                (InstrumentationEntity i) -> i.getInstrument().getName())
                        .thenComparing(
                                i -> Optional.ofNullable(i.getPartLabel()).orElse("")))
                .flatMap(i -> i.getAttachments().stream())
                .filter(a -> type == null || type == a.getType())
                .forEach(result::add);
        return result;
    }

    public List<AttachmentEntity> loadAttachmentEntitiesByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .map(id -> attachmentRepository.findByIdOptional(id).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    public void unlinkAttachments(Collection<AttachmentEntity> attachments) {
        for (AttachmentEntity attachment : attachments) {
            DocumentEntity document = attachment.getDocument();
            attachmentRepository.delete(attachment);
            if (document != null) {
                documentRepository.decrementRefCount(document);
                // Intentionally no deleteIfUnlinked: document stays in the uploads pool
            }
        }
    }

    public void deleteAttachment(String attachmentId) {
        AttachmentEntity attachment = attachmentRepository
                .findByIdOptional(UUID.fromString(attachmentId))
                .orElseThrow(() -> new EntityNotFoundException("Attachment", UUID.fromString(attachmentId)));

        instrumentationRepository.removeAttachment(attachment);
        sheetRepository.removeAttachment(attachment);

        DocumentEntity document = attachment.getDocument();
        attachmentRepository.delete(attachment);

        if (document != null) {
            documentRepository.decrementRefCount(document);
            documentStore.deleteIfUnlinked(document);
        }
    }

    public void linkAttachmentToSheet(String attachmentId, String sheetId) {
        AttachmentEntity attachment = attachmentRepository.findById(UUID.fromString(attachmentId));
        SheetMusicEntity sheet = sheetRepository.findById(UUID.fromString(sheetId));
        if (sheet != null && attachment != null) {
            sheet.getAttachments().add(attachment);
            sheetRepository.persist(sheet);
        }
    }

    public void linkAttachmentToInstrumentation(String attachmentId, String instrumentationId) {
        AttachmentEntity attachment = attachmentRepository.findById(UUID.fromString(attachmentId));
        InstrumentationEntity instrumentation = instrumentationRepository.findById(UUID.fromString(instrumentationId));
        if (instrumentation != null && attachment != null) {
            instrumentation.getAttachments().add(attachment);
            instrumentationRepository.persist(instrumentation);
        }
    }

    public Attachment linkDocument(UUID docIdentifier, DocumentLinkRequest documentLink) {
        Optional<AttachmentEntity> existingAttachment = attachmentRepository.findByIdOptional(docIdentifier);
        if (existingAttachment.isPresent()) {
            AttachmentEntity attachment = existingAttachment.get();

            instrumentationRepository.removeAttachment(attachment);
            sheetRepository.removeAttachment(attachment);

            if (documentLink.getAttachmentType() != null) {
                attachment.setType(documentLink.getAttachmentType());
            }

            linkToTarget(attachment, documentLink);
            attachmentRepository.persistAndFlush(attachment);
            return attachmentMapper.toDto(attachment);
        }

        DocumentEntity document = documentRepository
                .findByIdOptional(docIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Document", docIdentifier));

        AttachmentEntity attachment = new AttachmentEntity();
        attachment.setType(Optional.ofNullable(documentLink.getAttachmentType()).orElse(AttachmentType.UNSPECIFIED));
        attachment.setDisplayName(document.getFilename());

        if (documentLink.getAttachmentType() != AttachmentType.EXTERNAL_LINK) {
            attachment.setDocument(document);
            documentRepository.incrementRefCount(document);
        }

        linkToTarget(attachment, documentLink);
        attachmentRepository.persistAndFlush(attachment);
        return attachmentMapper.toDto(attachment);
    }

    private void linkToTarget(AttachmentEntity attachment, DocumentLinkRequest request) {
        if (request.getInstrumentationId() != null) {
            InstrumentationEntity instrumentation = instrumentationRepository
                    .findByIdOptional(request.getInstrumentationId())
                    .orElseThrow(() -> new EntityNotFoundException("Instrumentation", request.getInstrumentationId()));
            instrumentation.getAttachments().add(attachment);
            instrumentationRepository.persist(instrumentation);
        } else if (request.getSheetId() != null) {
            SheetMusicEntity sheet = sheetRepository
                    .findByIdOptional(request.getSheetId())
                    .orElseThrow(() -> new EntityNotFoundException("Sheet", request.getSheetId()));
            sheet.getAttachments().add(attachment);
            sheetRepository.persist(sheet);
        }
        // Neither → attachment stays/becomes unlinked (physical file preserved)
    }
}
