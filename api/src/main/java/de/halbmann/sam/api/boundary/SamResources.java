package de.halbmann.sam.api.boundary;

import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Root JAX-RS entry point for the SAM REST API. Each method returns a sub-resource interface that
 * groups the endpoints for one domain. Used as a MicroProfile REST client by the CLI module.
 */
@Path("/")
@RegisterRestClient(configKey = "sam-api")
public interface SamResources {

    @Path("booklets")
    BookletsResource booklets();

    @Path("documents")
    DocumentsResource documents();

    @Path("instruments")
    InstrumentsResource instruments();

    @Path("musicians")
    MusiciansResource musicians();

    @Path("sheet-collections")
    SheetCollectionsResource sheetCollections();

    @Path("sheets")
    SheetsResource sheets();

    @Path("ensembles")
    EnsemblesResource ensembles();

    @Path("event-logs")
    EventLogResource eventLogs();

    @Path("me/parts")
    MyPartsResource myParts();
}
