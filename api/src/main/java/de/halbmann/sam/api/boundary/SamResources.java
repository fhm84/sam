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

    /** @return sub-resource for managing booklets (physical binders / printed compilations). */
    @Path("booklets")
    BookletsResource booklets();

    /** @return sub-resource for managing documents and attachments. */
    @Path("documents")
    DocumentsResource documents();

    /** @return sub-resource for managing canonical instrument definitions. */
    @Path("instruments")
    InstrumentsResource instruments();

    /** @return sub-resource for managing musician records. */
    @Path("musicians")
    MusiciansResource musicians();

    /** @return sub-resource for managing sheet music collections (setlists, songbooks, etc.). */
    @Path("sheet-collections")
    SheetCollectionsResource sheetCollections();

    /** @return sub-resource for managing sheet music entries. */
    @Path("sheets")
    SheetsResource sheets();

    /** @return sub-resource for managing ensembles. */
    @Path("ensembles")
    EnsemblesResource ensembles();

    /** @return sub-resource for querying the audit event log. */
    @Path("event-logs")
    EventLogResource eventLogs();

    /** @return sub-resource listing sheets that match the calling musician's instrument assignments. */
    @Path("me/parts")
    MyPartsResource myParts();
}
