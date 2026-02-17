package de.halbmann.sam.classification.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

class AttachmentEntityUtilsTest {

    /**
     * Creates a single-page PDF with three distinct horizontal bands:
     * <ul>
     *   <li>Top 20%: red</li>
     *   <li>Middle 65%: white (background)</li>
     *   <li>Bottom 15%: blue</li>
     * </ul>
     * This allows verifying that {@link DocumentUtils#cropImage} correctly extracts
     * the top 20% and bottom 15% and combines them.
     */
    private byte[] createTestPdf() throws IOException {
        try (PDDocument doc = new PDDocument();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();

            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                // Fill entire page white first
                content.setNonStrokingColor(Color.WHITE);
                content.addRect(0, 0, pageWidth, pageHeight);
                content.fill();

                // Top 20% — red (PDF origin is bottom-left, so top starts at 80% of height)
                content.setNonStrokingColor(Color.RED);
                content.addRect(0, pageHeight * 0.80f, pageWidth, pageHeight * 0.20f);
                content.fill();

                // Bottom 15% — blue
                content.setNonStrokingColor(Color.BLUE);
                content.addRect(0, 0, pageWidth, pageHeight * 0.15f);
                content.fill();
            }

            doc.save(baos);
            return baos.toByteArray();
        }
    }

    /**
     * - Programmatically creates an A4 PDF with three colored bands (red top 20%, white middle, blue bottom 15%)
     * - Runs pdfToImage and reads back the resulting PNG
     * - Verifies the output dimensions: width matches the full page, height equals top 20% + bottom 15% of the rendered page
     */
    @Test
    void shouldConvertPdfToImageWithCorrectCroppedDimensions() throws IOException {
        byte[] pdfBytes = createTestPdf();

        ByteArrayOutputStream pngOutput = new ByteArrayOutputStream();
        DocumentUtils.pdfToImage(new ByteArrayInputStream(pdfBytes), pngOutput);

        // Read back the produced PNG
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(pngOutput.toByteArray()));
        assertNotNull(result, "pdfToImage should produce a valid PNG");

        // Render the original PDF page to determine the full-size image dimensions
        try (PDDocument doc = org.apache.pdfbox.Loader.loadPDF(pdfBytes)) {
            var renderer = new org.apache.pdfbox.rendering.PDFRenderer(doc);
            BufferedImage fullPage =
                    renderer.renderImageWithDPI(0, DocumentUtils.dpi, org.apache.pdfbox.rendering.ImageType.BINARY);

            int expectedTopHeight = (int) ((fullPage.getHeight() / 100.0) * 20);
            int expectedBottomHeight = (int) ((fullPage.getHeight() / 100.0) * 15);
            int expectedCombinedHeight = expectedTopHeight + expectedBottomHeight;

            assertThat("cropped width should match full page width", result.getWidth(), is(fullPage.getWidth()));
            assertThat("cropped height should be top 20% + bottom 15%", result.getHeight(), is(expectedCombinedHeight));
        }
    }

    /**
     * - Creates a synthetic 200x1000 grayscale image with black top 20%, white middle, black bottom 15%
     * - Calls cropImage and verifies the output is 200x350 (20% + 15%)
     * - Samples pixels to confirm the top region contains the original top band and the bottom region contains the original bottom band
     */
    @Test
    void shouldCropImagePreservingTopAndBottomRegions() throws IOException {
        // Create a synthetic image with known colored bands:
        // top 20% = dark, middle 65% = light, bottom 15% = dark
        int width = 200;
        int height = 1000; // easy percentages
        BufferedImage source = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = source.createGraphics();

        // Fill middle with white
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Top 20% = black
        g.setColor(Color.RED);
        g.fillRect(0, 0, width, 200);

        // Bottom 15% = black
        g.setColor(Color.BLUE);
        g.fillRect(0, 850, width, 150);

        g.dispose();

        BufferedImage cropped = DocumentUtils.cropImage(source, BufferedImage.TYPE_INT_ARGB);

        // Verify dimensions: 20% of 1000 + 15% of 1000 = 200 + 150 = 350
        assertThat(cropped.getWidth(), is(width));
        assertThat(cropped.getHeight(), is(350));

        // Verify top region (rows 0–199) came from the original top 20% (black)
        int topPixel = cropped.getRGB(width / 2, 100) & 0xFF; // grayscale: 0 = black
        int valueRed = Color.RED.getRGB() & 0xFF;
        assertThat("top region should be dark (from original top 20%)", topPixel, is(valueRed));
        int lastTopPixel = cropped.getRGB(width / 2, 199) & 0xFF; // grayscale: 0 = black
        assertThat("top region should be dark (from original top 20%)", lastTopPixel, is(valueRed));

        int valueBlue = Color.BLUE.getRGB() & 0xFF;
        // Verify bottom region (rows 200–349) came from the original bottom 15% (black)
        int bottomPixel = cropped.getRGB(width / 2, 275) & 0xFF;
        assertThat("bottom region should be dark (from original bottom 15%)", bottomPixel, is(valueBlue));
        int firstBottomPixel = cropped.getRGB(width / 2, 200) & 0xFF;
        assertThat("bottom region should be dark (from original bottom 15%)", firstBottomPixel, is(valueBlue));
    }
}
