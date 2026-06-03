package de.halbmann.sam;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/oidc-config.json")
@PermitAll
public class OidcConfigResource {

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    Optional<String> issuerUrl;

    @ConfigProperty(name = "quarkus.oidc.client-id", defaultValue = "sam-ui")
    String clientId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public OidcConfigDto config() {
        return new OidcConfigDto(issuerUrl.orElse("http://localhost:8180/realms/sam"), clientId);
    }

    public record OidcConfigDto(String issuerUrl, String clientId) {}
}
