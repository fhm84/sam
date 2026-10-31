package de.halbmann.sam.business.documents.controller;

import de.halbmann.sam.api.entity.documents.AttachmentType;
import de.halbmann.sam.business.documents.entity.AttachmentEntity;
import de.halbmann.sam.business.documents.entity.DocumentEntity;
import de.halbmann.sam.business.sheets.boundary.InstrumentationRepository;
import de.halbmann.sam.business.sheets.boundary.SheetRepository;
import de.halbmann.sam.business.sheets.entity.InstrumentationEntity;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

/**
 * The file name a stored document is downloaded under, so a musician gets "1000 Träume weit –
 * Tenorhorn 1 in B.pdf" instead of the scanner's "scan0001.pdf". Used for single downloads, share
 * links and the entries of ZIP bundles alike.
 *
 * <ul>
 *   <li>Part of an instrumentation: {@code <title> – <instrument>}, plus {@code (<type>)} when it is
 *       not a part (e.g. an audio file for that instrument).
 *   <li>Document of the sheet itself: {@code <title> – <type>} ("Partitur", "Titelblatt"), just the
 *       title when the type is unspecified.
 *   <li>Not attached anywhere (upload inbox): the original file name.
 * </ul>
 *
 * The extension comes from the original file name, else from the MIME type. Umlauts and spaces stay;
 * only characters file systems reject are replaced. Type labels follow the requested language
 * (German or English, as in the UI).
 */
@ApplicationScoped
public class DownloadNames {

    static final String SEPARATOR = " – ";

    private static final int MAX_BASE_LENGTH = 150;

    private static final MimeTypes MIME_TYPES = MimeTypes.getDefaultMimeTypes();

    @Inject
    InstrumentationRepository instrumentationRepository;

    @Inject
    SheetRepository sheetRepository;

    /** The download name of an attachment, wherever it is linked; the file name if nowhere. */
    public String forAttachment(AttachmentEntity attachment, Locale locale) {
        Optional<InstrumentationEntity> instrumentation = instrumentationRepository
                .find("select i from InstrumentationEntity i join i.attachments a where a.id = ?1", attachment.getId())
                .firstResultOptional();
        if (instrumentation.isPresent() && instrumentation.get().getSheet() != null) {
            return forInstrumentation(instrumentation.get().getSheet(), instrumentation.get(), attachment, locale);
        }
        Optional<SheetMusicEntity> sheet = sheetRepository
                .find("select s from SheetMusicEntity s join s.attachments a where a.id = ?1", attachment.getId())
                .firstResultOptional();
        if (sheet.isPresent()) {
            return forSheet(sheet.get(), attachment, locale);
        }
        DocumentEntity document = attachment.getDocument();
        return safe(document != null ? document.getFilename() : attachment.getDisplayName(), "download");
    }

    /** An attachment of one of the sheet's instrumentations (a part). */
    public String forInstrumentation(
            SheetMusicEntity sheet, InstrumentationEntity instrumentation, AttachmentEntity attachment, Locale locale) {
        String base = sheet.getTitle() + SEPARATOR + instrumentLabel(instrumentation);
        AttachmentType type = attachment.getType();
        if (type != null && type != AttachmentType.PART && type != AttachmentType.UNSPECIFIED) {
            base += " (" + AttachmentTypeLabels.label(type, locale) + ")";
        }
        return withExtension(base, attachment.getDocument());
    }

    /** An attachment of the sheet itself (score, cover, audio, …). */
    public String forSheet(SheetMusicEntity sheet, AttachmentEntity attachment, Locale locale) {
        AttachmentType type = attachment.getType();
        String base = type == null || type == AttachmentType.UNSPECIFIED
                ? sheet.getTitle()
                : sheet.getTitle() + SEPARATOR + AttachmentTypeLabels.label(type, locale);
        return withExtension(base, attachment.getDocument());
    }

    /**
     * The name of a bundle (ZIP or merged PDF) without its extension: the title, plus the instrument
     * when it holds one instrumentation, else the type when it is limited to one.
     */
    public String forBundle(
            SheetMusicEntity sheet, InstrumentationEntity instrumentation, AttachmentType type, Locale locale) {
        String base = sheet.getTitle();
        if (instrumentation != null) {
            base += SEPARATOR + instrumentLabel(instrumentation);
        } else if (type != null) {
            base += SEPARATOR + AttachmentTypeLabels.label(type, locale);
        }
        return safe(base, "download");
    }

    /**
     * The instrumentation as the UI shows it: display name (else name), the transposition in
     * brackets, then the part label, e.g. "Bariton (C) 1." — so "Bariton (C)" and "Bariton (Bb)" parts
     * don't end up with the same name.
     */
    public static String instrumentLabel(InstrumentationEntity instrumentation) {
        var instrument = instrumentation.getInstrument();
        String label = Optional.ofNullable(instrument.getDisplayName())
                .filter(n -> !n.isBlank())
                .orElse(instrument.getName());
        if (instrument.getTransposition() != null) {
            label += " (" + instrument.getTransposition() + ")";
        }
        String part = instrumentation.getPartLabel();
        return part != null && !part.isBlank() ? label + " " + part : label;
    }

    /**
     * The language for type labels: the first of the request's languages SAM speaks, else English
     * (as the UI falls back to).
     */
    public static Locale locale(List<Locale> acceptable) {
        if (acceptable != null) {
            for (Locale l : acceptable) {
                if (Locale.GERMAN.getLanguage().equals(l.getLanguage())) {
                    return Locale.GERMAN;
                }
                if (Locale.ENGLISH.getLanguage().equals(l.getLanguage())) {
                    return Locale.ENGLISH;
                }
            }
        }
        return Locale.ENGLISH;
    }

    static String withExtension(String base, DocumentEntity document) {
        String safeBase = safe(base, "download");
        if (safeBase.length() > MAX_BASE_LENGTH) {
            safeBase = safeBase.substring(0, MAX_BASE_LENGTH).strip();
        }
        String extension = document != null ? extension(document.getFilename(), document.getMimeType()) : "";
        return extension.isEmpty() ? safeBase : safeBase + "." + extension;
    }

    static String extension(String filename, String mimeType) {
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot > 0 && dot < filename.length() - 1 && filename.length() - dot <= 6) {
                return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
            }
        }
        if (mimeType != null && !mimeType.isBlank()) {
            try {
                String ext = MIME_TYPES.forName(mimeType).getExtension();
                if (ext != null && ext.length() > 1) {
                    return ext.substring(1);
                }
            } catch (MimeTypeException e) {
                // unknown MIME type: no extension
            }
        }
        return "";
    }

    /**
     * Keeps letters (umlauts included), digits, spaces and punctuation; replaces what Windows, macOS
     * or Linux reject in file names ({@code / \ : * ? " < > |} and control characters), collapses
     * whitespace and drops trailing dots.
     */
    static String safe(String name, String fallback) {
        if (name == null) {
            return fallback;
        }
        String s = name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_")
                .replaceAll("\\s+", " ")
                .strip()
                .replaceAll("[. ]+$", "");
        return s.isEmpty() ? fallback : s;
    }

    /** Attachment type labels as the UI shows them ({@code uploads.attachmentTypes} in the i18n files). */
    static final class AttachmentTypeLabels {

        private static final Map<AttachmentType, String> DE = new EnumMap<>(AttachmentType.class);
        private static final Map<AttachmentType, String> EN = new EnumMap<>(AttachmentType.class);

        static {
            put(AttachmentType.FULL_SCORE, "Partitur", "Full Score");
            put(AttachmentType.PART, "Stimme", "Part");
            put(AttachmentType.COVER, "Titelblatt", "Cover");
            put(AttachmentType.LYRICS, "Liedtext", "Lyrics");
            put(AttachmentType.MIDI, "MIDI", "MIDI");
            put(AttachmentType.AUDIO, "Audio", "Audio");
            put(AttachmentType.ANNOTATIONS, "Anmerkungen", "Annotations");
            put(AttachmentType.IMAGE, "Bild", "Image");
            put(AttachmentType.ANALYSIS, "Analyse", "Analysis");
            put(AttachmentType.TRANSCRIPTION, "Transkription", "Transcription");
            put(AttachmentType.EXTERNAL_LINK, "Externer Link", "External Link");
            put(AttachmentType.MUSIC_XML, "MusicXML", "MusicXML");
            put(AttachmentType.PROGRAM_NOTE, "Programmnotiz", "Programme Note");
            put(AttachmentType.OTHER, "Sonstiges", "Other");
            put(AttachmentType.UNSPECIFIED, "Nicht angegeben", "Unspecified");
        }

        private AttachmentTypeLabels() {}

        private static void put(AttachmentType type, String de, String en) {
            DE.put(type, de);
            EN.put(type, en);
        }

        static String label(AttachmentType type, Locale locale) {
            return (Locale.GERMAN.getLanguage().equals(locale.getLanguage()) ? DE : EN).get(type);
        }

        static Map<AttachmentType, String> labels(Locale locale) {
            return Locale.GERMAN.getLanguage().equals(locale.getLanguage()) ? DE : EN;
        }
    }
}
