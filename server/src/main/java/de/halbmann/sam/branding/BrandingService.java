package de.halbmann.sam.branding;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;

/**
 * Loads the configured logo files once at startup and keeps them in memory. A logo that can't be
 * used (missing, too large, not an image) is left out with a warning: it is decoration, and
 * shouldn't keep the archive from starting.
 */
@Slf4j
@Startup
@ApplicationScoped
public class BrandingService {

    static final int MAX_LOGO_BYTES = 1024 * 1024;

    /** The box the print logo is scaled into, keeping its aspect ratio. */
    static final double PRINT_MAX_WIDTH_CM = 5.0;

    static final double PRINT_MAX_HEIGHT_CM = 1.6;

    private static final Set<String> PRINTABLE = Set.of("image/png", "image/jpeg", "image/gif");

    @Inject
    BrandingConfig config;

    private Optional<String> name = Optional.empty();
    private Optional<Logo> logo = Optional.empty();
    private Optional<PrintLogo> printLogo = Optional.empty();

    /** A logo file as served to the browser; {@code version} changes with its content. */
    public record Logo(byte[] bytes, String mediaType, String version) {}

    /** A logo for the PDF renderer: a data URI and the size to draw it at. */
    public record PrintLogo(String dataUri, String width, String height) {}

    @PostConstruct
    void init() {
        load(config);
    }

    void load(final BrandingConfig cfg) {
        name = cfg.name().map(String::strip).filter(s -> !s.isEmpty());
        logo = cfg.logo().flatMap(path -> read(path, "sam.branding.logo"));
        Optional<Logo> print = cfg.printLogo().isPresent()
                ? cfg.printLogo().flatMap(path -> read(path, "sam.branding.print-logo"))
                : logo.filter(l -> PRINTABLE.contains(l.mediaType()));
        printLogo = print.flatMap(BrandingService::toPrintLogo);
        log.atInfo()
                .addArgument(name.isPresent())
                .addArgument(logo.map(Logo::mediaType).orElse("none"))
                .addArgument(printLogo.isPresent())
                .log("Branding: name {}, logo {}, print logo {}");
    }

    public Optional<String> name() {
        return name;
    }

    public Optional<Logo> logo() {
        return logo;
    }

    public Optional<PrintLogo> printLogo() {
        return printLogo;
    }

    private static Optional<Logo> read(final String location, final String property) {
        String trimmed = location.strip();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        Path path = Path.of(trimmed);
        try {
            if (!Files.isRegularFile(path)) {
                log.atWarn().addArgument(property).addArgument(path).log("{}: {} is not a file, left out");
                return Optional.empty();
            }
            if (Files.size(path) > MAX_LOGO_BYTES) {
                log.atWarn()
                        .addArgument(property)
                        .addArgument(path)
                        .addArgument(MAX_LOGO_BYTES / 1024)
                        .log("{}: {} is larger than {} KiB, left out");
                return Optional.empty();
            }
            byte[] bytes = Files.readAllBytes(path);
            Optional<String> mediaType = detectMediaType(bytes);
            if (mediaType.isEmpty()) {
                log.atWarn()
                        .addArgument(property)
                        .addArgument(path)
                        .log("{}: {} is not an SVG, PNG, JPEG, GIF or WebP image, left out");
                return Optional.empty();
            }
            return Optional.of(new Logo(bytes, mediaType.get(), version(bytes)));
        } catch (IOException e) {
            log.atWarn()
                    .addArgument(property)
                    .addArgument(path)
                    .addArgument(e.getMessage())
                    .log("{}: {} can't be read ({}), left out");
            return Optional.empty();
        }
    }

    /** By content, not file name: what the browser gets must match what the file is. */
    static Optional<String> detectMediaType(final byte[] b) {
        if (startsWith(b, 0, 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n')) {
            return Optional.of("image/png");
        }
        if (startsWith(b, 0, 0xFF, 0xD8, 0xFF)) {
            return Optional.of("image/jpeg");
        }
        if (startsWith(b, 0, 'G', 'I', 'F', '8')) {
            return Optional.of("image/gif");
        }
        if (startsWith(b, 0, 'R', 'I', 'F', 'F') && startsWith(b, 8, 'W', 'E', 'B', 'P')) {
            return Optional.of("image/webp");
        }
        // SVG is text: look for the root element near the start (after an XML declaration,
        // comments or a doctype).
        String head = new String(b, 0, Math.min(b.length, 1024), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        if (head.contains("<svg")) {
            return Optional.of("image/svg+xml");
        }
        return Optional.empty();
    }

    private static boolean startsWith(final byte[] bytes, final int offset, final int... expected) {
        if (bytes.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((bytes[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static Optional<PrintLogo> toPrintLogo(final Logo source) {
        if (!PRINTABLE.contains(source.mediaType())) {
            log.atWarn()
                    .addArgument(source.mediaType())
                    .log("sam.branding.print-logo: {} can't be drawn in PDFs (PNG, JPEG or GIF only), left out");
            return Optional.empty();
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(source.bytes()));
            if (image == null || image.getWidth() == 0 || image.getHeight() == 0) {
                log.atWarn().log("sam.branding.print-logo: the image can't be decoded, left out");
                return Optional.empty();
            }
            double scale = Math.min(PRINT_MAX_WIDTH_CM / image.getWidth(), PRINT_MAX_HEIGHT_CM / image.getHeight());
            String dataUri = "data:" + source.mediaType() + ";base64,"
                    + Base64.getEncoder().encodeToString(source.bytes());
            return Optional.of(new PrintLogo(dataUri, cm(image.getWidth() * scale), cm(image.getHeight() * scale)));
        } catch (IOException e) {
            log.atWarn().addArgument(e.getMessage()).log("sam.branding.print-logo: can't be decoded ({}), left out");
            return Optional.empty();
        }
    }

    private static String cm(final double value) {
        return String.format(Locale.ROOT, "%.2fcm", value);
    }

    private static String version(final byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(Arrays.copyOf(digest, 6));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
