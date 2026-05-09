package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.boundary.PublicShareResource;
import de.halbmann.sam.api.entity.documents.AttachmentType;
import de.halbmann.sam.api.entity.documents.DocumentDownload;
import de.halbmann.sam.api.entity.shares.PublicShareInfo;
import de.halbmann.sam.api.entity.shares.ShareType;
import de.halbmann.sam.business.collections.boundary.SheetCollectionRepository;
import de.halbmann.sam.business.collections.controller.CollectionTocService;
import de.halbmann.sam.business.collections.entity.SheetCollectionEntity;
import de.halbmann.sam.business.documents.controller.DocumentsService;
import de.halbmann.sam.business.documents.controller.MergedPdfEntry;
import de.halbmann.sam.business.documents.controller.StreamWriter;
import de.halbmann.sam.business.documents.entity.AttachmentEntity;
import de.halbmann.sam.business.shares.controller.ShareService;
import de.halbmann.sam.business.shares.entity.ShareEntity;
import de.halbmann.sam.business.sheets.boundary.InstrumentationRepository;
import de.halbmann.sam.business.sheets.boundary.SheetRepository;
import de.halbmann.sam.business.sheets.entity.InstrumentationEntity;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@PermitAll
@Slf4j
@RequestScoped
public class PublicShareResourceImpl implements PublicShareResource {

    @Inject
    ShareService shareService;

    @Inject
    DocumentsService documentsService;

    @Inject
    CollectionTocService collectionTocService;

    @Inject
    SheetCollectionRepository sheetCollectionRepository;

    @Inject
    InstrumentationRepository instrumentationRepository;

    @Inject
    SheetRepository sheetRepository;

    @Override
    public PublicShareInfo getInfo(UUID token) {
        return shareService.getPublicInfo(token);
    }

    @Override
    public Response download(UUID token, AttachmentType type) {
        ShareEntity share = shareService.validateShare(token);
        if (share.getResourceType() == ShareType.INSTRUMENTATION) {
            shareService.logAccess(share.getId(), "instrumentation", share.getResourceId());
            return buildInstrumentationResponse(share.getResourceId());
        } else if (share.getResourceType() == ShareType.SHEET) {
            AttachmentType effectiveType = type != null ? type : AttachmentType.PART;
            shareService.logAccess(share.getId(), "sheet", share.getResourceId());
            return buildSheetAllResponse(share.getResourceId(), effectiveType);
        } else if (share.getResourceType() == ShareType.COLLECTION) {
            AttachmentType effectiveType = type != null ? type : AttachmentType.PART;
            shareService.logAccess(share.getId(), "collection", share.getResourceId());
            return buildCollectionAllResponse(share.getResourceId(), effectiveType);
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @Override
    public Response downloadToc(UUID token) {
        ShareEntity share = shareService.validateShare(token);
        if (share.getResourceType() != ShareType.COLLECTION) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        shareService.logAccess(share.getId(), "collection", share.getResourceId());

        byte[] pdf = collectionTocService.generateToc(share.getResourceId().toString(), true);
        return Response.ok(pdf)
                .type("application/pdf")
                .header("Content-Disposition", "attachment; filename*=utf-8''collection-toc.pdf")
                .header("Cache-Control", "no-store")
                .build();
    }

    @Override
    public Response downloadPart(UUID token, UUID instrumentationId) {
        ShareEntity share = shareService.validateShare(token);
        if (share.getResourceType() == ShareType.COLLECTION) {
            if (!shareService.isInstrumentationInCollection(share.getResourceId(), instrumentationId)) {
                throw new ForbiddenException();
            }
        } else if (share.getResourceType() == ShareType.SHEET) {
            if (!shareService.isInstrumentationInSheet(share.getResourceId(), instrumentationId)) {
                throw new ForbiddenException();
            }
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        shareService.logAccess(share.getId(), "instrumentation", instrumentationId);
        return buildInstrumentationResponse(instrumentationId);
    }

    @Override
    public Response downloadSheetAttachment(UUID token, UUID attachmentId) {
        ShareEntity share = shareService.validateShare(token);
        if (share.getResourceType() != ShareType.SHEET) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (!shareService.isAttachmentOnSheet(share.getResourceId(), attachmentId)) {
            throw new ForbiddenException();
        }
        shareService.logAccess(share.getId(), "attachment", attachmentId);
        DocumentDownload dl = documentsService.loadAttachment(attachmentId.toString());
        if (dl == null) {
            return Response.noContent().build();
        }
        String encodedFilename =
                URLEncoder.encode(dl.filename(), StandardCharsets.UTF_8).replace("+", "%20");
        final StreamingOutput streamingOutput = output -> {
            try (InputStream in = dl.stream()) {
                in.transferTo(output);
            }
        };
        return Response.ok(streamingOutput)
                .type(dl.mimeType())
                .header("Content-Length", dl.size())
                .header("Content-Disposition", "attachment; filename*=utf-8''" + encodedFilename)
                .header("Cache-Control", "no-store")
                .build();
    }

    private Response buildSheetAllResponse(UUID sheetId, AttachmentType type) {
        List<AttachmentEntity> attachments =
                documentsService.loadAttachmentEntitiesBySheetInstrumentations(sheetId.toString(), type);
        if (attachments.isEmpty()) {
            return Response.noContent().build();
        }
        SheetMusicEntity sheet = sheetRepository.findById(sheetId);
        String title = sheet != null ? sheet.getTitle() : "sheet";
        String encodedFilename =
                URLEncoder.encode(title + ".zip", StandardCharsets.UTF_8).replace("+", "%20");
        StreamWriter zip = documentsService.buildZipForSheetInstrumentations(sheetId.toString(), type);
        return Response.ok((StreamingOutput) zip::write)
                .type("application/zip")
                .header("Content-Disposition", "attachment; filename*=utf-8''" + encodedFilename)
                .header("Cache-Control", "no-store")
                .build();
    }

    private Response buildCollectionAllResponse(UUID collectionId, AttachmentType type) {
        StreamWriter zip = documentsService.buildZipForCollectionInstrumentations(collectionId.toString(), type);
        if (zip == null) {
            return Response.noContent().build();
        }
        SheetCollectionEntity collection = sheetCollectionRepository.findById(collectionId);
        String name = collection != null ? collection.getName() : "collection";
        String encodedFilename =
                URLEncoder.encode(name + ".zip", StandardCharsets.UTF_8).replace("+", "%20");
        return Response.ok((StreamingOutput) zip::write)
                .type("application/zip")
                .header("Content-Disposition", "attachment; filename*=utf-8''" + encodedFilename)
                .header("Cache-Control", "no-store")
                .build();
    }

    private Response buildInstrumentationResponse(UUID instrumentationId) {
        List<MergedPdfEntry> entries =
                documentsService.buildMergeEntriesForInstrumentation(instrumentationId.toString(), null);
        if (entries.isEmpty()) {
            return Response.noContent().build();
        }

        if (entries.size() == 1) {
            DocumentDownload dl = documentsService.loadAttachment(
                    entries.get(0).attachment().getId().toString());
            if (dl == null) {
                return Response.noContent().build();
            }
            String encodedFilename =
                    URLEncoder.encode(dl.filename(), StandardCharsets.UTF_8).replace("+", "%20");
            final StreamingOutput streamingOutput = output -> {
                try (InputStream in = dl.stream()) {
                    in.transferTo(output);
                }
            };
            return Response.ok(streamingOutput)
                    .type(dl.mimeType())
                    .header("Content-Length", dl.size())
                    .header("Content-Disposition", "attachment; filename*=utf-8''" + encodedFilename)
                    .header("Cache-Control", "no-store")
                    .build();
        }

        List<AttachmentEntity> attachments =
                entries.stream().map(MergedPdfEntry::attachment).toList();
        InstrumentationEntity instr = instrumentationRepository.findById(instrumentationId);
        String baseName = instr != null && instr.getInstrument() != null
                ? instr.getInstrument().getName()
                : "instrumentation";
        StreamWriter zip = documentsService.buildZip(attachments, baseName);
        String encodedFilename =
                URLEncoder.encode(baseName + ".zip", StandardCharsets.UTF_8).replace("+", "%20");
        return Response.ok((StreamingOutput) zip::write)
                .type("application/zip")
                .header("Content-Disposition", "attachment; filename*=utf-8''" + encodedFilename)
                .header("Cache-Control", "no-store")
                .build();
    }
}
