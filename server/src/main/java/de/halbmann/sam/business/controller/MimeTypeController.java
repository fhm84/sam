package de.halbmann.sam.business.controller;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class to detect a mime type.
 *
 * @author FabianHalbmann
 */
@ApplicationScoped
public class MimeTypeController {

    final TikaConfig tikaConfig = TikaConfig.getDefaultConfig();

    /**
     * The Apache Tika™ toolkit detects and extracts metadata and text from over a thousand different file types (such
     * as PPT, XLS, and PDF). All of these file types can be parsed through a single interface, making Tika useful for
     * search engine indexing, content analysis, translation, and much more.
     * <p>
     * The current implementation allows Tika to search for a magic header of the file. However, according to own
     * research, Tika analyses the content of the file, checks whether an identifier of the mime type is given in the
     * first bytes of the stream and then makes a precise "guess" as to which one is most likely.
     *
     * @return detected mime type
     */
    public String detectMimeType(final BufferedInputStream inputStream, final String filename) {
        final Metadata tikaMetaData = new Metadata();
        tikaMetaData.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);

        if (tikaMetaData.get(TikaCoreProperties.RESOURCE_NAME_KEY) == null) {
            throw new IllegalStateException("Tika was not able to set the metadata");
        }

        final InputStream tikaStream = TikaInputStream.get(inputStream);

        final Detector detector = tikaConfig.getDetector();

        try {
            final MediaType mimeTypeByDetector = detector.detect(tikaStream, tikaMetaData);

            return mimeTypeByDetector.toString();
        } catch (final IOException e) {
            throw new RuntimeException("Tika was not able to analyse mimetype");
        }
    }

}