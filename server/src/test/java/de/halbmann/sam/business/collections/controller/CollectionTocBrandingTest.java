package de.halbmann.sam.business.collections.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.halbmann.sam.api.entity.collections.CollectionType;
import de.halbmann.sam.api.entity.collections.SheetCollection;
import de.halbmann.sam.branding.BrandedProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

/** With branding configured, the TOC's header carries the band's name and logo. */
@QuarkusTest
@TestProfile(BrandedProfile.class)
class CollectionTocBrandingTest {

    @Inject
    CollectionTocService service;

    @Test
    void headerShowsBandNameAndLogo() throws IOException {
        SheetCollection collection = new SheetCollection();
        collection.setName("Spring concert");
        collection.setType(CollectionType.SETLIST);

        byte[] pdf = CollectionTocService.render(service.renderHtml(collection, true));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Spring concert"), text);
            // text-transform: uppercase
            assertTrue(text.contains("TEST BAND"), text);

            PDResources resources = document.getPage(0).getResources();
            int images = 0;
            for (COSName name : resources.getXObjectNames()) {
                if (resources.getXObject(name) instanceof PDImageXObject image) {
                    images++;
                    assertEquals(40, image.getWidth());
                    assertEquals(10, image.getHeight());
                }
            }
            assertEquals(1, images, "the logo is drawn once");
            assertFalse(text.contains("data:image"), "the data URI was printed as text");
        }
    }
}
