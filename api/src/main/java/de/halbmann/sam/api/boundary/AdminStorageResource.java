package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.admin.StorageIntegrityReport;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/** Admin-only maintenance of the document file store. */
@Path("admin/storage")
@Produces(MediaType.APPLICATION_JSON)
public interface AdminStorageResource {

    /**
     * Re-hashes every stored document file and compares it with the database, and lists files no
     * document points to. Read-only: it reports, it doesn't repair. Takes as long as reading the
     * whole archive once; run it while nobody uploads.
     *
     * @return what is damaged, missing, orphaned or left over
     */
    @POST
    @Path("integrity-check")
    StorageIntegrityReport checkIntegrity();
}
