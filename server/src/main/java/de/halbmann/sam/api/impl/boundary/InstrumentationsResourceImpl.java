package de.halbmann.sam.api.impl.boundary;

import de.halbmann.sam.api.boundary.DocumentsResource;
import de.halbmann.sam.api.boundary.InstrumentationsResource;
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

    public List<Instrumentation> listAll() {
        return instrumentationRepository.getInstrumentations(sheetId);
    }

    public Instrumentation get(final String instrumentationId) {
        return instrumentationRepository.getInstrumentation(instrumentationId);
    }

    public void add(final Instrumentation instrumentation) {
        instrumentationRepository.addInstrumentation(sheetId, instrumentation);
    }

    public void update(final String instrumentationId, final Instrumentation instrumentation) {
        //part.persist();
    }

    public void delete(final String instrumentationId) {
        // fixme: implement!
    }

}