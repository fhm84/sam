package de.halbmann.sam.business.collections.controller;

import de.halbmann.sam.api.entity.collections.CollectionSheet;
import de.halbmann.sam.api.entity.collections.SheetCollection;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.xhtmlrenderer.pdf.ITextRenderer;

@ApplicationScoped
public class CollectionTocService {

    @Inject
    SheetCollectionService collectionService;

    @Inject
    @Location("collection-toc")
    Template tocTemplate;

    public byte[] generateToc(final String collectionId) {
        SheetCollection collection = collectionService.load(collectionId);

        List<TocRow> rows = collection.getSheets().stream().map(this::toRow).toList();

        String totalDuration = computeTotalDuration(collection.getSheets());

        String html = tocTemplate
                .data("collection", collection)
                .data("rows", rows)
                .data("totalDuration", totalDuration)
                .data("generatedAt", LocalDate.now())
                .render();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate collection TOC PDF", e);
        }
    }

    TocRow toRow(final CollectionSheet sheet) {
        return new TocRow(
                sheet.getIdentifier() != null ? sheet.getIdentifier() : "",
                sheet.getTitle() != null ? sheet.getTitle() : "",
                sheet.getSubtitle() != null ? sheet.getSubtitle() : "",
                sheet.getGenre() != null ? sheet.getGenre().name() : "",
                formatDuration(sheet.getDuration()));
    }

    String formatDuration(final Duration d) {
        if (d == null) return "";
        long totalSeconds = d.getSeconds();
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    String computeTotalDuration(final List<CollectionSheet> sheets) {
        Duration total = sheets.stream()
                .map(CollectionSheet::getDuration)
                .filter(Objects::nonNull)
                .reduce(Duration.ZERO, Duration::plus);
        return total.isZero() ? null : formatDuration(total);
    }

    public record TocRow(String identifier, String title, String subtitle, String genre, String duration) {}
}
