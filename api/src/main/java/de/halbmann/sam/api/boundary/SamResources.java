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

    /**
     * {@return the sub-resource for managing booklets (physical binders / printed compilations)}
     * Planned feature: not implemented on the server yet.
     */
    @Path("booklets")
    BookletsResource booklets();

    /** {@return the sub-resource for managing documents and attachments} */
    @Path("documents")
    DocumentsResource documents();

    /** {@return the sub-resource for managing canonical instrument definitions} */
    @Path("instruments")
    InstrumentsResource instruments();

    /** {@return the sub-resource for the person catalogue (composers, arrangers, members)} */
    @Path("persons")
    PersonsResource persons();

    /** {@return the sub-resource for managing the band's members} */
    @Path("members")
    MembersResource members();

    /** {@return the sub-resource for managing sheet music collections (setlists, songbooks, etc.)} */
    @Path("sheet-collections")
    SheetCollectionsResource sheetCollections();

    /** {@return the sub-resource for managing sheet music entries} */
    @Path("sheets")
    SheetsResource sheets();

    /** {@return the sub-resource for managing ensembles} */
    @Path("ensembles")
    EnsemblesResource ensembles();

    /** {@return the sub-resource for querying the audit event log} */
    @Path("event-logs")
    EventLogResource eventLogs();

    /** {@return the sub-resource listing sheets that match the calling member's instrument assignments} */
    @Path("me/parts")
    MyPartsResource myParts();

    /** {@return the sub-resource with the calling member's own data} */
    @Path("me/member")
    MyMemberResource myMember();

    /** {@return the sub-resource with the calling user's own event-log entries} */
    @Path("me/event-logs")
    MyEventLogResource myEventLogs();
}
