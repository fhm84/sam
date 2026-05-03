package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.entity.documents.DocumentDownload;
import de.halbmann.sam.api.entity.shares.PublicShareInfo;
import de.halbmann.sam.api.entity.shares.ShareType;
import de.halbmann.sam.business.collections.controller.CollectionTocService;
import de.halbmann.sam.business.documents.controller.DocumentsService;
import de.halbmann.sam.business.documents.controller.MergedPdfEntry;
import de.halbmann.sam.business.documents.controller.StreamWriter;
import de.halbmann.sam.business.documents.entity.AttachmentEntity;
import de.halbmann.sam.business.shares.controller.ShareService;
import de.halbmann.sam.business.shares.entity.ShareEntity;
import de.halbmann.sam.business.sheets.boundary.InstrumentationRepository;
import de.halbmann.sam.business.sheets.entity.InstrumentationEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

/**
 * Unauthenticated endpoints for share-link access. Token validation is performed per request;
 * no OIDC token is required.
 */
@Slf4j
@Path("public/share")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class PublicShareResource {

    @Inject
    ShareService shareService;

    @Inject
    DocumentsService documentsService;

    @Inject
    CollectionTocService collectionTocService;

    @Inject
    InstrumentationRepository instrumentationRepository;

    @GET
    @Path("{token}")
    public PublicShareInfo getInfo(@PathParam("token") UUID token) {
        return shareService.getPublicInfo(token);
    }

    /** Download endpoint for INSTRUMENTATION shares — streams the part file(s). */
    @GET
    @Path("{token}/download")
    @Produces(MediaType.WILDCARD)
    public Response download(@PathParam("token") UUID token) {
        ShareEntity share = shareService.validateShare(token);
        if (share.getResourceType() != ShareType.INSTRUMENTATION) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        shareService.logAccess(share.getId(), "instrumentation", share.getResourceId());
        return buildInstrumentationResponse(share.getResourceId());
    }

    /** TOC PDF download for COLLECTION shares. */
    @GET
    @Path("{token}/toc")
    @Produces(MediaType.WILDCARD)
    public Response downloadToc(@PathParam("token") UUID token) {
        ShareEntity share = shareService.validateShare(token);
        if (share.getResourceType() != ShareType.COLLECTION) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        shareService.logAccess(share.getId(), "collection", share.getResourceId());

        byte[] pdf = collectionTocService.generateToc(share.getResourceId().toString());
        return Response.ok(pdf)
                .type("application/pdf")
                .header("Content-Disposition", "attachment; filename*=utf-8''collection-toc.pdf")
                .header("Cache-Control", "no-store")
                .build();
    }

    /** Download a specific instrumentation part within a COLLECTION share. */
    @GET
    @Path("{token}/download/{instrumentationId}")
    @Produces(MediaType.WILDCARD)
    public Response downloadPart(
            @PathParam("token") UUID token, @PathParam("instrumentationId") UUID instrumentationId) {
        ShareEntity share = shareService.validateShare(token);
        if (share.getResourceType() != ShareType.COLLECTION) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (!shareService.isInstrumentationInCollection(share.getResourceId(), instrumentationId)) {
            throw new ForbiddenException();
        }
        shareService.logAccess(share.getId(), "instrumentation", instrumentationId);
        return buildInstrumentationResponse(instrumentationId);
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
