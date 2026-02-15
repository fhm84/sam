package de.halbmann.sam.api.impl.boundary;

import de.halbmann.sam.api.boundary.DocumentsResource;
import de.halbmann.sam.api.boundary.InstrumentationsResource;
import de.halbmann.sam.api.entity.CreateInstrumentation;
import de.halbmann.sam.api.entity.Instrumentation;
import de.halbmann.sam.business.controller.InstrumentationService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import java.util.List;

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
    public void add(final CreateInstrumentation instrumentation) {
        instrumentationService.addInstrumentation(sheetId, instrumentation);
    }

    @Override
    public void add(List<CreateInstrumentation> instrumentations) {
        instrumentationService.addInstrumentations(sheetId, instrumentations);
    }

    @Override
    public void update(final String instrumentationId, final Instrumentation instrumentation) {
        instrumentationService.updateInstrumentation(instrumentationId, instrumentation);
    }

    @Override
    public void delete(final String instrumentationId) {
        instrumentationService.deleteInstrumentation(instrumentationId);
    }

    @Override
    public DocumentsResource documents(String instrumentationId) {
        return resourceContext.getResource(DocumentsResource.class);
    }
}
