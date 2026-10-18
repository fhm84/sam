package de.halbmann.sam;

import de.halbmann.sam.security.RoleGroups;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/oidc-config.json")
@PermitAll
public class OidcConfigResource {

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    Optional<String> issuerUrl;

    @ConfigProperty(name = "quarkus.oidc.client-id", defaultValue = "sam-ui")
    String clientId;

    /** Scopes the UI requests at login; Pocket ID only emits {@code groups} when asked for it. */
    @ConfigProperty(name = "sam.oidc.ui.scope", defaultValue = "openid profile email")
    String scope;

    /** Dot-separated path to the roles array, read by the UI from the access token, else the ID token. */
    @ConfigProperty(name = "sam.oidc.ui.role-claim-path", defaultValue = "realm_access.roles")
    String roleClaimPath;

    @Inject
    RoleGroups roleGroups;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public OidcConfigDto config() {
        return new OidcConfigDto(
                issuerUrl.orElse("http://localhost:8180/realms/sam"),
                clientId,
                scope,
                roleClaimPath,
                roleGroups.groupByRole());
    }

    /** {@code roleGroups}: SAM role → provider group, empty when the claim holds SAM's roles directly. */
    public record OidcConfigDto(
            String issuerUrl, String clientId, String scope, String roleClaimPath, Map<String, String> roleGroups) {}
}
