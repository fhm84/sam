package de.halbmann.sam.business.collections.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The PDF renderer must not load anything from outside, whatever the HTML asks for. */
class CollectionTocRenderingTest {

    private HttpServer server;
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private String base;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.add(exchange.getRequestURI().toString());
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void externalImagesStyleSheetsAndBackgroundsAreNotFetched() throws IOException {
        String html = """
                <html xmlns="http://www.w3.org/1999/xhtml"><head>
                <link rel="stylesheet" type="text/css" href="%1$s/sheet.css"/>
                <style type="text/css">
                  @import url("%1$s/imported.css");
                  body { background-image: url("%1$s/background.png"); }
                </style>
                </head><body>
                <p>Still rendered</p>
                <img src="%1$s/image.png"/>
                </body></html>
                """.formatted(base);

        byte[] pdf = CollectionTocService.render(html);

        assertEquals(List.of(), requests, "the renderer fetched something");
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertTrue(new PDFTextStripper().getText(document).contains("Still rendered"));
        }
    }
}
