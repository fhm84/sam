package de.halbmann.sam.security;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A minimal Pocket ID user API ({@code GET /api/users?search=}, {@code GET /api/users/{id}}) that
 * accepts only {@link #API_KEY}. Users: alice (enabled) and bob (disabled).
 */
public class PocketIdStub implements QuarkusTestResourceLifecycleManager {

    public static final String API_KEY = "stub-api-key";
    public static final String ALICE_ID = "6f1c2a3b-0000-4000-8000-000000000001";
    public static final String BOB_ID = "6f1c2a3b-0000-4000-8000-000000000002";

    private static final String ALICE = """
            {"id":"%s","username":"alice","email":"alice@example.org","firstName":"Alice","lastName":"Smith",\
            "isAdmin":false,"userGroups":[{"id":"g1","name":"sam-member"}],"disabled":false}""".formatted(ALICE_ID);
    private static final String BOB = """
            {"id":"%s","username":"bob","email":"bob@example.org","firstName":"Bob","lastName":"Jones",\
            "isAdmin":false,"userGroups":[],"disabled":true}""".formatted(BOB_ID);

    /** The last request's URI, for asserting what SAM sent. */
    public final AtomicReference<URI> lastRequest = new AtomicReference<>();

    private HttpServer server;

    /** Starts the stub on a free port and returns its base URL. */
    public String startServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.createContext("/api/users", this::handle);
        server.start();
        return "http://localhost:" + server.getAddress().getPort();
    }

    @Override
    public Map<String, String> start() {
        return Map.of("sam.oidc.pocket-id.url", startServer(), "sam.oidc.pocket-id.api-key", API_KEY);
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        lastRequest.set(exchange.getRequestURI());
        if (!API_KEY.equals(exchange.getRequestHeaders().getFirst("X-API-KEY"))) {
            respond(exchange, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/api/users")) {
            String query = exchange.getRequestURI().getQuery();
            String search = query == null ? "" : query.replaceFirst("^search=([^&]*).*$", "$1");
            List<String> users = List.of(ALICE, BOB).stream()
                    .filter(user -> user.contains(search))
                    .toList();
            respond(
                    exchange,
                    200,
                    "{\"data\":[" + String.join(",", users) + "],\"pagination\":{\"totalPages\":1,\"totalItems\":"
                            + users.size() + "}}");
        } else if (path.equals("/api/users/" + ALICE_ID)) {
            respond(exchange, 200, ALICE);
        } else {
            respond(exchange, 404, "{\"error\":\"User not found\"}");
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
