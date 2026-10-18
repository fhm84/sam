package de.halbmann.sam.security;

import de.halbmann.sam.EnvConsts;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.PropertiesConfigSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.List;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Loads the settings of the OIDC provider selected by {@code sam.oidc.provider} (env
 * {@code SAM_OIDC_PROVIDER}): {@code pocket-id} (default) or {@code keycloak}.
 *
 * <p>Everything that differs per provider (role source and claim path, the {@code m2m} tenant, the
 * scope and role claim for the UI, the Keycloak admin client) lives in
 * {@code oidc/<provider>.properties}. Its ordinal sits just below {@code application.properties},
 * so env vars and system properties override it; keep a key in only one of the two files, because
 * a plain key in the higher-ordinal file also beats a profiled one ({@code %dev.…}) in the lower.
 *
 * <p>Registered in {@code META-INF/services/io.smallrye.config.ConfigSourceFactory}.
 */
public class OidcProviderConfigSourceFactory implements ConfigSourceFactory {

    static final String DEFAULT_PROVIDER = "pocket-id";
    static final int ORDINAL = 245;

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext context) {
        ConfigValue value = context.getValue(EnvConsts.OIDC_PROVIDER);
        String provider = value == null || value.getValue() == null ? DEFAULT_PROVIDER : value.getValue();
        URL url = OidcProviderConfigSourceFactory.class.getResource("/oidc/" + provider + ".properties");
        if (url == null) {
            throw new IllegalArgumentException(
                    "Unknown " + EnvConsts.OIDC_PROVIDER + " '" + provider + "', expected 'pocket-id' or 'keycloak'");
        }
        try {
            return List.of(new PropertiesConfigSource(url, ORDINAL));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + url, e);
        }
    }
}
