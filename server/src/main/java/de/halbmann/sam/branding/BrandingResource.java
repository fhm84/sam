package de.halbmann.sam.branding;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * The band's name and logo for the UI. Public, like the share links' API it sits next to: the share
 * page shows them to guests, who never log in, and the UI sends no token to {@code /api/public/}.
 */
@Path("/public/branding")
@PermitAll
public class BrandingResource {

    /**
     * Keeps the logo inert when opened directly: an SVG can carry scripts. In an {@code <img>} they
     * never run anyway.
     */
    static final String LOGO_CSP = "default-src 'none'; style-src 'unsafe-inline'; sandbox";

    @Inject
    BrandingService branding;

    /** {@code logoVersion}: append as {@code ?v=} to the logo URL; null when there is no logo. */
    public record BrandingDto(String name, String logoVersion) {}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public BrandingDto get() {
        return new BrandingDto(
                branding.name().orElse(null),
                branding.logo().map(BrandingService.Logo::version).orElse(null));
    }

    /** With the current {@code v}, the browser may keep the logo for good: a new file gets a new version. */
    @GET
    @Path("logo")
    public Response logo(@QueryParam("v") final String version) {
        return branding.logo()
                .map(logo -> Response.ok(logo.bytes(), logo.mediaType())
                        .header(
                                "Cache-Control",
                                logo.version().equals(version) ? "public, max-age=31536000, immutable" : "no-cache")
                        .header("Content-Security-Policy", LOGO_CSP)
                        .build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }
}
