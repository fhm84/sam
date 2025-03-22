package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.SheetMusic;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusIntegrationTest
class End2EndIT {

    @Inject
    @RestClient
    SheetsResource sheetsResource;

    @Test
    void testCreateSheetIncludingInstrumentationAndPdf() {
        final SheetMusic sheetMusic = sheetsResource.add(ObjectMother.createFullSheetMusic());
        assertNotNull(sheetMusic.getId());
    }

}
