package de.halbmann.sam.branding;

import io.smallrye.config.ConfigMapping;
import java.util.Optional;

/**
 * The band's name and logo, shown next to SAM's own logo (top bar, share page, TOC PDF). All
 * optional: without them SAM looks as it always did. The logo is a file on the server, never part
 * of the repository or the image.
 */
@ConfigMapping(prefix = "sam.branding")
public interface BrandingConfig {

    /** The band's name. Env var {@code SAM_BRANDING_NAME}. */
    Optional<String> name();

    /**
     * Path to the logo shown in the UI: SVG, PNG, JPEG, GIF or WebP, at most 1 MiB. Env var
     * {@code SAM_BRANDING_LOGO}.
     */
    Optional<String> logo();

    /**
     * Path to a PNG, JPEG or GIF for PDFs, which can't draw SVG. Defaults to {@link #logo()} when
     * that is a PNG, JPEG or GIF. Env var {@code SAM_BRANDING_PRINT_LOGO}.
     */
    Optional<String> printLogo();
}
