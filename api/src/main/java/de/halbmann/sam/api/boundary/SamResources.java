package de.halbmann.sam.api.boundary;

import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

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
}
