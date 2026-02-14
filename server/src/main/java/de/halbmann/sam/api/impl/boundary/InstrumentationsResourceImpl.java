package de.halbmann.sam.api.impl.boundary;

import de.halbmann.sam.api.boundary.DocumentsResource;
import de.halbmann.sam.api.boundary.InstrumentationsResource;
import de.halbmann.sam.api.entity.CreateInstrumentation;
import de.halbmann.sam.api.entity.Instrumentation;
import de.halbmann.sam.business.boundary.InstrumentationRepository;
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
    InstrumentationRepository instrumentationRepository;

    @Override
    public List<Instrumentation> listAll() {
        return instrumentationRepository.getInstrumentations(sheetId);
    }

    @Override
    public Instrumentation get(final String instrumentationId) {
        return instrumentationRepository.getInstrumentation(instrumentationId);
    }

    @Override
    public void add(final CreateInstrumentation instrumentation) {
        instrumentationRepository.addInstrumentation(sheetId, instrumentation);
    }

    @Override
    public void add(List<CreateInstrumentation> instrumentations) {
        instrumentationRepository.addInstrumentations(sheetId, instrumentations);
    }

    @Override
    public void update(final String instrumentationId, final Instrumentation instrumentation) {
        instrumentationRepository.updateInstrumentation(instrumentationId, instrumentation);
    }

    @Override
    public void delete(final String instrumentationId) {
        instrumentationRepository.deleteInstrumentation(instrumentationId);
    }

    @Override
    public DocumentsResource documents(String instrumentationId) {
        return resourceContext.getResource(DocumentsResource.class);
    }
}
