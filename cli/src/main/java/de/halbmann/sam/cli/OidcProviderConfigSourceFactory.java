package de.halbmann.sam.cli;

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
 * Loads the token settings of the OIDC provider selected by {@code sam.oidc.provider} (env
 * {@code SAM_OIDC_PROVIDER}): {@code pocket-id} (default) or {@code keycloak}, the same setting
 * {@code server} uses.
 *
 * <p>The provider's issuer default, token path, dev secret and token request options live in
 * {@code oidc/<provider>.properties}. Its ordinal sits just below {@code application.properties},
 * so env vars and system properties override it; a plain key in {@code application.properties}
 * would also beat a profiled one ({@code %prod.…}) there, so keep each key in one of the two files.
 *
 * <p>Registered in {@code META-INF/services/io.smallrye.config.ConfigSourceFactory}.
 */
public class OidcProviderConfigSourceFactory implements ConfigSourceFactory {

    static final String PROVIDER = "sam.oidc.provider";
    static final String DEFAULT_PROVIDER = "pocket-id";
    static final int ORDINAL = 245;

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext context) {
        ConfigValue value = context.getValue(PROVIDER);
        String provider = value == null || value.getValue() == null ? DEFAULT_PROVIDER : value.getValue();
        URL url = OidcProviderConfigSourceFactory.class.getResource("/oidc/" + provider + ".properties");
        if (url == null) {
            throw new IllegalArgumentException(
                    "Unknown " + PROVIDER + " '" + provider + "', expected 'pocket-id' or 'keycloak'");
        }
        try {
            return List.of(new PropertiesConfigSource(url, ORDINAL));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + url, e);
        }
    }
}
