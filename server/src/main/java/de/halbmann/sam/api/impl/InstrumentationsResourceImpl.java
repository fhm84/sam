package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.boundary.DocumentsResource;
import de.halbmann.sam.api.boundary.InstrumentationsResource;
import de.halbmann.sam.api.entity.sheets.CreateInstrumentation;
import de.halbmann.sam.api.entity.sheets.Instrumentation;
import de.halbmann.sam.business.sheets.controller.InstrumentationService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import java.util.List;

@Authenticated
@RequestScoped
public class InstrumentationsResourceImpl implements InstrumentationsResource {

    @PathParam("sheetId")
    String sheetId;

    @Context
    ResourceContext resourceContext;

    @Inject
    InstrumentationService instrumentationService;

    @Override
    public List<Instrumentation> listAll() {
        return instrumentationService.getInstrumentations(sheetId);
    }

    @Override
    public Instrumentation get(final String instrumentationId) {
        return instrumentationService.getInstrumentation(instrumentationId);
    }

    @Override
    @RolesAllowed({"music_librarian", "admin"})
    public void add(final CreateInstrumentation instrumentation) {
        instrumentationService.addInstrumentation(sheetId, instrumentation);
    }

    @Override
    @RolesAllowed({"music_librarian", "admin"})
    public void add(List<CreateInstrumentation> instrumentations) {
        instrumentationService.addInstrumentations(sheetId, instrumentations);
    }

    @Override
    @RolesAllowed({"music_librarian", "admin"})
    public void update(final String instrumentationId, final Instrumentation instrumentation) {
        instrumentationService.updateInstrumentation(instrumentationId, instrumentation);
    }

    @Override
    @RolesAllowed({"music_librarian", "admin"})
    public void delete(final String instrumentationId) {
        instrumentationService.deleteInstrumentation(instrumentationId);
    }

    @Override
    public DocumentsResource documents(String instrumentationId) {
        return resourceContext.getResource(DocumentsResource.class);
    }
}
