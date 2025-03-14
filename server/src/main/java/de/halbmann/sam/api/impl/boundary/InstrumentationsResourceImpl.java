package de.halbmann.sam.api.impl.boundary;

import de.halbmann.sam.api.boundary.InstrumentationsResource;
import de.halbmann.sam.api.entity.Instrumentation;
import de.halbmann.sam.business.boundary.InstrumentationRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.PathParam;

import java.util.List;

@RequestScoped
public class InstrumentationsResourceImpl implements InstrumentationsResource {

    @PathParam("sheetId")
    String sheetId;

    @Inject
    InstrumentationRepository instrumentationRepository;

    public List<Instrumentation> listAll() {
        return instrumentationRepository.getInstrumentations(sheetId);
    }

    public Instrumentation get(final String instrumentationId) {
        return instrumentationRepository.getInstrumentation(instrumentationId);
    }

    // FIXME: add endpoint to load file (pdf)

    public void add(final Instrumentation instrumentation) {
        instrumentationRepository.addInstrumentation(sheetId, instrumentation);
    }

    public void update(final String instrumentationId, final Instrumentation instrumentation) {
        //part.persist();
    }

    // FIXME: add file-upload!

    public void delete(final String instrumentationId) {
        // fixme: implement!
    }

}