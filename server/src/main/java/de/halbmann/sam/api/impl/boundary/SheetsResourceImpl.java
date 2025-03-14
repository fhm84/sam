package de.halbmann.sam.api.impl.boundary;

import de.halbmann.sam.api.boundary.InstrumentationsResource;
import de.halbmann.sam.api.boundary.SheetsResource;
import de.halbmann.sam.api.entity.PaginatedResponse;
import de.halbmann.sam.api.entity.SheetFilterRequest;
import de.halbmann.sam.api.entity.SheetMusic;
import de.halbmann.sam.business.boundary.SheetRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;

@RequestScoped
public class SheetsResourceImpl implements SheetsResource {

    @Context
    ResourceContext resourceContext;

    @Inject
    SheetRepository sheetRepository;

    public PaginatedResponse<SheetMusic> findSheets(final SheetFilterRequest filterRequest) {
        return sheetRepository.findSheets(filterRequest);
    }

    public SheetMusic add(final SheetMusic sheetMusic) {
        return sheetRepository.addSheet(sheetMusic);
    }

    public SheetMusic load(final String sheetId) {
        return sheetRepository.getSheet(sheetId);
    }

    public void update(final String sheetId, final SheetMusic sheetMusic) {
        sheetRepository.updateSheet(sheetId, sheetMusic);
    }

    public void delete(final String sheetId) {
        sheetRepository.deleteSheet(sheetId);
    }

    public InstrumentationsResource instrumentations(final String sheetId) {
        return resourceContext.getResource(InstrumentationsResourceImpl.class);
    }

}