package de.halbmann.sam.api.impl.boundary;

import de.halbmann.sam.api.boundary.DocumentsResource;
import de.halbmann.sam.api.boundary.InstrumentationsResource;
import de.halbmann.sam.api.boundary.SheetsResource;
import de.halbmann.sam.api.entity.*;
import de.halbmann.sam.business.controller.CoverageEvaluationService;
import de.halbmann.sam.business.controller.SheetService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import java.util.List;
import java.util.Set;

@RequestScoped
public class SheetsResourceImpl implements SheetsResource {

    @Context
    ResourceContext resourceContext;

    @Inject
    SheetService sheetService;

    @Inject
    CoverageEvaluationService coverageEvaluationService;

    @Override
    public PaginatedResponse<SheetMusicSearchResult> findSheets(final SheetFilterRequest filterRequest) {
        return sheetService.findSheets(filterRequest);
    }

    @Override
    public SheetMusic add(final CreateSheetMusic sheetMusic) {
        return sheetService.addSheet(sheetMusic);
    }

    @Override
    public SheetMusic load(final String sheetId) {
        return sheetService.getSheet(sheetId);
    }

    @Override
    public void update(final String sheetId, final SheetMusic sheetMusic) {
        sheetService.updateSheet(sheetId, sheetMusic);
    }

    @Override
    public void delete(final String sheetId) {
        sheetService.deleteSheet(sheetId);
    }

    @Override
    public void addTags(String sheetId, Set<String> newTags) {
        sheetService.addTags(sheetId, newTags);
    }

    @Override
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
}
