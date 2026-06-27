package de.halbmann.sam.business.collections.controller;

import de.halbmann.sam.api.entity.collections.CollectionItem;
import de.halbmann.sam.api.entity.collections.CollectionItemType;
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

    public byte[] generateToc(final String collectionId, final boolean includeTextItems) {
        SheetCollection collection = collectionService.load(collectionId);

        List<CollectionItem> items = includeTextItems
                ? collection.getItems()
                : collection.getItems().stream()
                        .filter(i -> i.getType() == CollectionItemType.SHEET)
                        .toList();

        List<TocRow> rows = items.stream().map(this::toRow).toList();

        String totalDuration = computeTotalDuration(items);

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

    TocRow toRow(final CollectionItem item) {
        if (item.getType() == CollectionItemType.TEXT) {
            return new TocRow(
                    item.getIdentifier() != null ? item.getIdentifier() : "",
                    null,
                    item.getTextContent() != null ? item.getTextContent() : "",
                    null,
                    null);
        }
        return new TocRow(
                item.getIdentifier() != null ? item.getIdentifier() : "",
                item.getTitle() != null ? item.getTitle() : "",
                item.getSubtitle() != null ? item.getSubtitle() : "",
                item.getGenre() != null ? item.getGenre().name() : "",
                formatDuration(item.getDuration()));
    }

    String formatDuration(final Duration d) {
        if (d == null) {
            return "";
        }
        long totalSeconds = d.getSeconds();
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    String computeTotalDuration(final List<CollectionItem> items) {
        Duration total = items.stream()
                .map(CollectionItem::getDuration)
                .filter(Objects::nonNull)
                .reduce(Duration.ZERO, Duration::plus);
        return total.isZero() ? null : formatDuration(total);
    }

    /**
     * @param title null signals a TEXT item — rendered full-width in the template
     */
    public record TocRow(String identifier, String title, String subtitle, String genre, String duration) {
        public boolean isTextItem() {
            return title == null;
        }
    }
}
