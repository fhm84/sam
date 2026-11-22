package de.halbmann.sam.branding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BrandingServiceTest {

    static final String SVG = """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 10"><circle r="5"/></svg>
            """;

    @TempDir
    Path dir;

    @Test
    void nothingConfiguredMeansNoBranding() {
        BrandingService service = load(null, null, null);

        assertTrue(service.name().isEmpty());
        assertTrue(service.logo().isEmpty());
        assertTrue(service.printLogo().isEmpty());
    }

    @Test
    void blankNameAndPathCountAsUnset() {
        BrandingService service = load("  ", " ", null);

        assertTrue(service.name().isEmpty());
        assertTrue(service.logo().isEmpty());
    }

    @Test
    void pngLogoIsAlsoThePrintLogo() throws IOException {
        Path png = write("logo.png", png(400, 100));

        BrandingService service = load(" Band ", png.toString(), null);

        assertEquals(Optional.of("Band"), service.name());
        assertEquals("image/png", service.logo().orElseThrow().mediaType());
        BrandingService.PrintLogo print = service.printLogo().orElseThrow();
        assertTrue(print.dataUri().startsWith("data:image/png;base64,"));
        // 4:1 fits the 5 cm × 1.6 cm box by its width
        assertEquals("5.00cm", print.width());
        assertEquals("1.25cm", print.height());
    }

    @Test
    void tallLogoIsScaledByItsHeight() throws IOException {
        Path png = write("logo.png", png(100, 200));

        BrandingService.PrintLogo print =
                load(null, png.toString(), null).printLogo().orElseThrow();

        assertEquals("0.80cm", print.width());
        assertEquals("1.60cm", print.height());
    }

    @Test
    void svgLogoHasNoPrintLogoOfItsOwn() throws IOException {
        Path svg = write("logo.svg", SVG.getBytes(StandardCharsets.UTF_8));

        BrandingService service = load(null, svg.toString(), null);

        assertEquals("image/svg+xml", service.logo().orElseThrow().mediaType());
        assertTrue(service.printLogo().isEmpty());
    }

    @Test
    void separatePrintLogoNextToSvg() throws IOException {
        Path svg = write("logo.svg", SVG.getBytes(StandardCharsets.UTF_8));
        Path png = write("print.png", png(200, 100));

        BrandingService service = load(null, svg.toString(), png.toString());

        assertEquals("image/svg+xml", service.logo().orElseThrow().mediaType());
        assertTrue(service.printLogo().orElseThrow().dataUri().startsWith("data:image/png;"));
    }

    @Test
    void svgAsPrintLogoIsLeftOut() throws IOException {
        Path svg = write("logo.svg", SVG.getBytes(StandardCharsets.UTF_8));

        assertTrue(load(null, null, svg.toString()).printLogo().isEmpty());
    }

    @Test
    void missingTooLargeOrUnknownFilesAreLeftOut() throws IOException {
        Path tooLarge = write("big.png", new byte[BrandingService.MAX_LOGO_BYTES + 1]);
        Path text = write("logo.png", "not an image".getBytes(StandardCharsets.UTF_8));

        assertTrue(
                load(null, dir.resolve("missing.png").toString(), null).logo().isEmpty());
        assertTrue(load(null, dir.toString(), null).logo().isEmpty());
        assertTrue(load(null, tooLarge.toString(), null).logo().isEmpty());
        assertTrue(load(null, text.toString(), null).logo().isEmpty());
    }

    @Test
    void mediaTypeComesFromTheContent() {
        assertEquals(
                Optional.of("image/jpeg"),
                BrandingService.detectMediaType(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}));
        assertEquals(
                Optional.of("image/gif"),
                BrandingService.detectMediaType("GIF89a".getBytes(StandardCharsets.US_ASCII)));
        assertEquals(
                Optional.of("image/webp"),
                BrandingService.detectMediaType("RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.US_ASCII)));
        assertFalse(BrandingService.detectMediaType(new byte[0]).isPresent());
    }

    @Test
    void versionFollowsTheContent() throws IOException {
        Path a = write("a.png", png(10, 10));
        Path b = write("b.png", png(20, 10));

        String versionA = load(null, a.toString(), null).logo().orElseThrow().version();

        assertEquals(
                versionA, load(null, a.toString(), null).logo().orElseThrow().version());
        assertNotEquals(
                versionA, load(null, b.toString(), null).logo().orElseThrow().version());
    }

    private Path write(final String fileName, final byte[] content) throws IOException {
        return Files.write(dir.resolve(fileName), content);
    }

    static byte[] png(final int width, final int height) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", out);
            return out.toByteArray();
        }
    }

    private static BrandingService load(final String name, final String logo, final String printLogo) {
        BrandingService service = new BrandingService();
        service.load(new BrandingConfig() {
            @Override
            public Optional<String> name() {
                return Optional.ofNullable(name);
            }

            @Override
            public Optional<String> logo() {
                return Optional.ofNullable(logo);
            }

            @Override
            public Optional<String> printLogo() {
                return Optional.ofNullable(printLogo);
            }
        });
        return service;
    }
}
