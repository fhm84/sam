package de.halbmann.sam.branding;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Name "Test Band" and a 40 × 10 PNG logo, written to a temporary file (no logo in the repository). */
public class BrandedProfile implements QuarkusTestProfile {

    static final byte[] LOGO;
    static final Path LOGO_FILE;

    static {
        try {
            LOGO = BrandingServiceTest.png(40, 10);
            LOGO_FILE = Files.createTempFile("sam-branding", ".png");
            Files.write(LOGO_FILE, LOGO);
            LOGO_FILE.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("sam.branding.name", "Test Band", "sam.branding.logo", LOGO_FILE.toString());
    }
}
