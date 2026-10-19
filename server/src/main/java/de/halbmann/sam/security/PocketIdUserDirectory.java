package de.halbmann.sam.security;

import static java.nio.charset.StandardCharsets.UTF_8;

import de.halbmann.sam.EnvConsts;
import de.halbmann.sam.api.entity.admin.UserInfo;
import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Searches Pocket ID's users through its admin API ({@code /api/users}) with an API key.
 *
 * <p>Pocket ID API keys cannot be scoped: the key acts with full admin rights on Pocket ID and
 * every app behind it. So it comes only from the server's env, is never logged, and is optional:
 * without it, search is off and {@link #find(String)} returns just the ID, which an admin entered
 * by hand (open question #14 of the Pocket ID plan).
 */
@ApplicationScoped
@LookupIfProperty(name = EnvConsts.OIDC_PROVIDER, stringValue = "pocket-id")
public class PocketIdUserDirectory implements UserDirectory {

    static final int LIMIT = 20;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    // Pocket ID user IDs are UUIDs; anything else must not reach the URL path
    private static final Pattern USER_ID = Pattern.compile("[A-Za-z0-9-]+");

    private final Optional<String> baseUrl;
    private final Optional<String> apiKey;
    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final Jsonb jsonb = JsonbBuilder.create();

    PocketIdUserDirectory(
            @ConfigProperty(name = EnvConsts.POCKET_ID_URL) Optional<String> baseUrl,
            @ConfigProperty(name = EnvConsts.POCKET_ID_API_KEY) Optional<String> apiKey) {
        this.baseUrl = baseUrl.map(url -> url.replaceAll("/+$", ""));
        this.apiKey = apiKey;
    }

    @Override
    public boolean searchable() {
        return baseUrl.isPresent() && apiKey.isPresent();
    }

    @Override
    public List<UserInfo> search(String query) {
        if (!searchable()) {
            throw new IllegalStateException("Pocket ID user search needs " + EnvConsts.POCKET_ID_API_KEY);
        }
        String path = "/api/users?search=" + URLEncoder.encode(query == null ? "" : query, UTF_8)
                + "&pagination%5Blimit%5D=" + LIMIT;
        return get(path, UserPage.class)
                .orElseThrow(() -> new UserDirectoryException("Pocket ID has no user API at " + baseUrl.get()))
                .data()
                .stream()
                .filter(user -> !user.disabled())
                .map(PocketIdUser::toUserInfo)
                .toList();
    }

    @Override
    public Optional<UserInfo> find(String userId) {
        if (userId == null || !USER_ID.matcher(userId).matches()) {
            return Optional.empty();
        }
        if (!searchable()) {
            return Optional.of(new UserInfo(userId, userId, null, null, null));
        }
        return get("/api/users/" + userId, PocketIdUser.class).map(PocketIdUser::toUserInfo);
    }

    private <T> Optional<T> get(String path, Class<T> type) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl.get() + path))
                .timeout(TIMEOUT)
                .header("X-API-KEY", apiKey.get())
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new UserDirectoryException("Pocket ID is not reachable at " + baseUrl.get(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UserDirectoryException("Interrupted while calling Pocket ID", e);
        }
        return switch (response.statusCode()) {
            case 200 -> Optional.of(jsonb.fromJson(response.body(), type));
            case 404 -> Optional.empty();
            case 401, 403 ->
                throw new UserDirectoryException("Pocket ID rejected the API key (HTTP " + response.statusCode() + ")");
            default -> throw new UserDirectoryException("Pocket ID answered HTTP " + response.statusCode());
        };
    }

    /** One page of {@code GET /api/users}; the pagination block is not needed. */
    @RegisterForReflection
    public record UserPage(List<PocketIdUser> data) {}

    /** The fields of a Pocket ID user that SAM uses; the rest is ignored. */
    @RegisterForReflection
    public record PocketIdUser(
            String id, String username, String email, String firstName, String lastName, boolean disabled) {

        UserInfo toUserInfo() {
            return new UserInfo(id, username, email, firstName, lastName);
        }
    }
}
