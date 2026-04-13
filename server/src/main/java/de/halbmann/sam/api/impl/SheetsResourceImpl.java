package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.boundary.DocumentsResource;
import de.halbmann.sam.api.boundary.InstrumentationsResource;
import de.halbmann.sam.api.boundary.SheetsResource;
import de.halbmann.sam.api.entity.*;
import de.halbmann.sam.api.entity.collections.SheetCollection;
import de.halbmann.sam.api.entity.ensembles.CoverageResult;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.api.entity.sheets.*;
import de.halbmann.sam.business.collections.controller.SheetCollectionService;
import de.halbmann.sam.business.ensembles.controller.CoverageEvaluationService;
import de.halbmann.sam.business.sheets.controller.ExportResult;
import de.halbmann.sam.business.sheets.controller.SheetExportService;
import de.halbmann.sam.business.sheets.controller.SheetService;
import de.halbmann.sam.enrichment.controller.SheetEnrichmentService;
import de.halbmann.sam.security.Roles;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.util.List;
import java.util.Set;

@Authenticated
@RequestScoped
public class SheetsResourceImpl implements SheetsResource {

    @Context
    ResourceContext resourceContext;

    @Inject
    SheetService sheetService;

    @Inject
    CoverageEvaluationService coverageEvaluationService;

    @Inject
    SheetExportService sheetExportService;

    @Inject
    SheetEnrichmentService sheetEnrichmentService;

    @Inject
    SheetCollectionService sheetCollectionService;

    @Override
    public PaginatedResponse<SheetMusicSearchResult> findSheets(final SheetFilterRequest filterRequest) {
        return sheetService.findSheets(filterRequest);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public SheetMusic add(final CreateSheetMusic sheetMusic) {
        return sheetService.addSheet(sheetMusic);
    }

    @Override
    public SheetMusic load(final String sheetId) {
        return sheetService.getSheet(sheetId);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void update(final String sheetId, final SheetMusic sheetMusic) {
        sheetService.updateSheet(sheetId, sheetMusic);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void delete(final String sheetId) {
        sheetService.deleteSheet(sheetId);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void addTags(String sheetId, Set<String> newTags) {
        sheetService.addTags(sheetId, newTags);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void removeTag(String sheetId, Set<String> tagsToRemove) {
        sheetService.removeTags(sheetId, tagsToRemove);
    }

    @Override
    public void favorite(String sheetId) {
        sheetService.favorite(sheetId, true);
    }

    @Override
    public void unfavorite(String sheetId) {
        sheetService.favorite(sheetId, false);
    }

    @Override
    public List<String> getGenres() {
        return sheetService.getDistinctGenres();
    }

    @Override
    public List<String> getAvailableLetters(String genre) {
        return sheetService.getAvailableLetters(genre);
    }

    @Override
    public InstrumentationsResource instrumentations(final String sheetId) {
        return resourceContext.getResource(InstrumentationsResourceImpl.class);
    }

    @Override
    public DocumentsResource documents(String sheetId) {
        return resourceContext.getResource(DocumentsResourceImpl.class);
    }

    @Override
    public CoverageResult coverage(final String sheetId, final String ensembleId) {
        return coverageEvaluationService.evaluate(sheetId, ensembleId);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public SheetEnrichment enrich(final String sheetId) {
        return sheetEnrichmentService.enrich(sheetId);
    }

    @Override
    public PaginatedResponse<SheetCollection> getCollections(
            final String sheetId, final PaginationRequest paginationRequest) {
        return sheetCollectionService.findCollectionsForSheet(sheetId, paginationRequest);
    }

    @Override
    public Response export(final String sheetId, final ExportFormat format) {
        ExportResult result = sheetExportService.exportSheet(sheetId, format);
        return Response.ok((StreamingOutput) result.body()::write)
                .header("Content-Disposition", "attachment; filename=\"" + result.filename() + "\"")
                .type(result.contentType())
                .build();
    }
}
