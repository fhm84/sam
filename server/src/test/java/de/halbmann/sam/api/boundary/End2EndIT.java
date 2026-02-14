package de.halbmann.sam.api.boundary;

import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.api.entity.CreateInstrumentation;
import de.halbmann.sam.api.entity.Instrumentation;
import de.halbmann.sam.api.entity.SheetMusic;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import java.net.URI;
import java.util.List;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.MultipleFailuresError;

@QuarkusIntegrationTest
class End2EndIT {

    SheetsResource sheetsResource;
    InstrumentsResource instrumentsResource;

    @BeforeEach
    void setup() {
        URI baseUri = URI.create("http://localhost:8081");
        sheetsResource = RestClientBuilder.newBuilder().baseUri(baseUri).build(SheetsResource.class);
        instrumentsResource = RestClientBuilder.newBuilder().baseUri(baseUri).build(InstrumentsResource.class);
    }

    @Test
    void testCreateSheetIncludingInstrumentationAndPdf() {
        // Create instruments first
        instrumentsResource.add(ObjectMother.createFluteInstrument());
        instrumentsResource.add(ObjectMother.createTrumpetInstrument());

        final SheetMusic sheetMusic = sheetsResource.add(ObjectMother.createFullSheetMusic());
        assertNotNull(sheetMusic.getId());

        String sheetId = sheetMusic.getId().toString();
        CreateInstrumentation flute1C = ObjectMother.flute1C();
        sheetsResource.instrumentations(sheetId).add(flute1C);
        CreateInstrumentation trumpet2Bb = ObjectMother.trumpet2Bb();
        sheetsResource.instrumentations(sheetId).add(trumpet2Bb);

        List<Instrumentation> instrumentations =
                sheetsResource.instrumentations(sheetId).listAll();
        assertNotNull(instrumentations);
        assertEquals(2, instrumentations.size());
        assertTrue(instrumentations.stream().anyMatch(i -> matches(flute1C, i)));
        assertTrue(instrumentations.stream().anyMatch(i -> matches(trumpet2Bb, i)));
    }

    boolean matches(final CreateInstrumentation expected, final Instrumentation actual) {
        try {
            assertAll(
                    () -> assertEquals(
                            expected.getInstrumentId(), actual.getInstrument().getId()),
                    () -> assertSame(expected.getNotationType(), actual.getNotationType()),
                    () -> assertEquals(expected.getPartLabel(), actual.getPartLabel()));
            return true;
        } catch (MultipleFailuresError e) {
            e.printStackTrace();
            return false;
        }
    }
}
