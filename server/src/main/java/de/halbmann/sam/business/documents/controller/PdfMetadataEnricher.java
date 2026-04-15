package de.halbmann.sam.business.documents.controller;

import de.halbmann.sam.api.entity.documents.DocumentDownload;
import de.halbmann.sam.business.sheets.entity.InstrumentationEntity;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

@Slf4j
@ApplicationScoped
public class PdfMetadataEnricher {

    private static final String MIME_PDF = "application/pdf";

    /**
     * Injects PDF metadata derived from the linked sheet/instrumentation into the document stream.
     *
     * <p>Non-PDF documents are returned unchanged. If PDF processing fails the original bytes are
     * re-wrapped and returned so the download is never broken.
     */
    public DocumentDownload enrich(
            DocumentDownload download, SheetMusicEntity sheet, InstrumentationEntity instrumentation) {
        if (!MIME_PDF.equals(download.mimeType())) {
            return download;
        }

        byte[] originalBytes;
        try {
            originalBytes = download.stream().readAllBytes();
            download.stream().close();
        } catch (IOException e) {
            log.warn("Could not read PDF stream for document {}, skipping metadata injection", download.id());
            return download;
        }

        try (PDDocument doc = Loader.loadPDF(originalBytes)) {
            PDDocumentInformation info = doc.getDocumentInformation();

            if (sheet != null) {
                info.setTitle(sheet.getTitle());

                String author = buildAuthor(sheet);
                if (author != null) {
                    info.setAuthor(author);
                }

                String subject = buildSubject(sheet, instrumentation);
                if (subject != null) {
                    info.setSubject(subject);
                }

                String keywords = buildKeywords(sheet);
                if (keywords != null) {
                    info.setKeywords(keywords);
                }
            }

            info.setCreator("SAM");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            byte[] enriched = baos.toByteArray();

            return new DocumentDownload(
                    new ByteArrayInputStream(enriched),
                    download.id(),
                    download.filename(),
                    enriched.length,
                    download.mimeType(),
                    download.checksumSha256());

        } catch (IOException e) {
            log.warn("Failed to inject PDF metadata for document {}, serving original", download.id(), e);
            return new DocumentDownload(
                    new ByteArrayInputStream(originalBytes),
                    download.id(),
                    download.filename(),
                    download.size(),
                    download.mimeType(),
                    download.checksumSha256());
        }
    }

    private String buildAuthor(SheetMusicEntity sheet) {
        List<String> parts = new ArrayList<>();
        if (sheet.getComposer() != null && sheet.getComposer().getName() != null) {
            parts.add(sheet.getComposer().getName());
        }
        if (sheet.getArranger() != null && sheet.getArranger().getName() != null) {
            parts.add("arr. " + sheet.getArranger().getName());
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private String buildSubject(SheetMusicEntity sheet, InstrumentationEntity instrumentation) {
        if (instrumentation != null && instrumentation.getInstrument() != null) {
            String instrumentName = Optional.ofNullable(
                            instrumentation.getInstrument().getDisplayName())
                    .orElse(instrumentation.getInstrument().getName());
            return instrumentation.getPartLabel() != null
                    ? instrumentName + " " + instrumentation.getPartLabel()
                    : instrumentName;
        }
        if (sheet != null) {
            List<String> parts = new ArrayList<>();
            if (sheet.getGenre() != null) {
                parts.add(sheet.getGenre().name());
            }
            if (sheet.getStyle() != null) {
                parts.add(sheet.getStyle().name());
            }
            return parts.isEmpty() ? null : String.join(", ", parts);
        }
        return null;
    }

    private String buildKeywords(SheetMusicEntity sheet) {
        List<String> parts = new ArrayList<>();
        if (sheet.getTags() != null) {
            parts.addAll(sheet.getTags());
        }
        if (sheet.getGenre() != null) {
            parts.add(sheet.getGenre().name());
        }
        if (sheet.getStyle() != null) {
            parts.add(sheet.getStyle().name());
        }
        if (sheet.getIswc() != null) {
            parts.add("ISWC:" + sheet.getIswc());
        }
        if (sheet.getGemaWorkNumber() != null) {
            parts.add("GEMA:" + sheet.getGemaWorkNumber());
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }
}
