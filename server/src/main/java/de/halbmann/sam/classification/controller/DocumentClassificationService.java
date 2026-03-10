package de.halbmann.sam.classification.controller;

import de.halbmann.sam.api.entity.*;
import de.halbmann.sam.business.boundary.*;
import de.halbmann.sam.business.entity.*;
import de.halbmann.sam.business.exception.EntityNotFoundException;
import de.halbmann.sam.classification.boundary.ClassificationAgent;
import de.halbmann.sam.classification.boundary.ClassificationService;
import de.halbmann.sam.classification.entity.SheetAnalyzerResult;
import de.halbmann.storage.api.FileSystemWrapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Orchestrates AI-based document classification and entity resolution/creation.
 *
 * <p>Two-step workflow:
 *
 * <ol>
 *   <li>{@link #classify} — runs AI analysis on the stored file, pre-matches existing entities,
 *       and builds a pre-filled {@link ClassificationApplyRequest} for user review (Option A). When
 *       {@code sam.classification.agentic=true}, a second agentic AI pass autonomously resolves
 *       entities via tool calls (Option B).
 *   <li>{@link #apply} — creates/resolves entities and links the document as an attachment.
 * </ol>
 */
@Slf4j
@ApplicationScoped
@Transactional
public class DocumentClassificationService {

    @Inject
    ClassificationService classificationService;

    @Inject
    ClassificationAgent classificationAgent;

    @Inject
    DocumentRepository documentRepository;

    @Inject
    FileSystemWrapper filesystem;

    @Inject
    MusicianRepository musicianRepository;

    @Inject
    InstrumentRepository instrumentRepository;

    @Inject
    SheetRepository sheetRepository;

    @Inject
    AttachmentRepository attachmentRepository;

    @Inject
    InstrumentationRepository instrumentationRepository;

    @ConfigProperty(name = "sam.classification.agentic", defaultValue = "false")
    boolean agenticMode;

    /**
     * Runs AI classification on the stored document and pre-matches existing entities by name/title.
     */
    public SheetClassification classify(UUID documentId) {
        DocumentEntity doc = documentRepository
                .findByIdOptional(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document", documentId));

        log.info("Classifying document {} ({})", documentId, doc.getMimeType());

        SheetAnalyzerResult result;
        try (InputStream stream = filesystem.openForRead(doc.getPath())) {
            if ("application/pdf".equals(doc.getMimeType())) {
                result = classificationService.analyzePdf(stream);
            } else {
                result = classificationService.analyzeImage(stream);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read document for classification", e);
        }

        log.info("Classification result for {}: title={}, composer={}", documentId, result.title(), result.composer());

        // Pre-match existing musicians
        UUID matchedComposerId = matchMusician(result.composer());
        UUID matchedArrangerId = matchMusician(result.arranger());

        // Pre-match existing instruments via trigram similarity
        SheetAnalyzerResult.InstrumentationAnalyzerResult instr = result.instrumentation();
        List<InstrumentMatch> instrumentCandidates = instr != null && instr.instrumentName() != null
                ? instrumentRepository.findCandidates(instr.instrumentName(), 0.3, 5)
                : List.of();

        // Pre-match existing sheet by title
        UUID matchedSheetId = null;
        String matchedSheetTitle = null;
        if (result.title() != null) {
            Optional<SheetMusicEntity> matchedSheet = sheetRepository.findByTitle(result.title());
            if (matchedSheet.isPresent()) {
                matchedSheetId = matchedSheet.get().getId();
                matchedSheetTitle = matchedSheet.get().getTitle();
            }
        }

        // Build pre-filled suggestion (Option A), optionally replaced by agentic resolution (Option B)
        ClassificationApplyRequest suggested =
                buildSuggestion(result, matchedComposerId, matchedArrangerId, matchedSheetId, instrumentCandidates);

        if (agenticMode) {
            suggested = resolveWithAgent(result, suggested);
        }

        return new SheetClassification(
                documentId,
                ClassificationStatus.CLASSIFIED,
                result.title(),
                result.subtitle(),
                result.publisher(),
                result.composer(),
                result.arranger(),
                result.genre(),
                result.yearOfComposition(),
                result.edition(),
                result.iswc(),
                instr != null ? instr.instrumentName() : null,
                instr != null ? instr.partLabel() : null,
                instr != null ? instr.transposition() : null,
                instr != null ? instr.clef() : null,
                instr != null ? instr.notationType() : null,
                matchedSheetId,
                matchedSheetTitle,
                matchedComposerId,
                matchedArrangerId,
                instrumentCandidates,
                suggested);
    }

    /**
     * Resolves or creates entities based on the reviewed classification request and links the
     * document as an attachment to the resulting sheet or instrumentation.
     */
    public ClassificationApplyResult apply(UUID documentId, ClassificationApplyRequest request) {
        DocumentEntity document = documentRepository
                .findByIdOptional(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document", documentId));

        MusicianEntity composer = resolveMusician(request.getComposerId(), request.getComposerName());
        MusicianEntity arranger = resolveMusician(request.getArrangerId(), request.getArrangerName());
        SheetMusicEntity sheet = resolveSheet(request, composer, arranger);
        InstrumentEntity instrument = resolveInstrument(request.getInstrumentId(), request.getInstrumentName());

        InstrumentationEntity instrumentation = null;
        if (instrument != null) {
            instrumentation = new InstrumentationEntity();
            instrumentation.setInstrument(instrument);
            instrumentation.setPartLabel(request.getPartLabel());
            instrumentation.setClef(request.getClef());
            instrumentation.setNotationType(request.getNotationType());
            instrumentation.setSheet(sheet);
            sheet.getInstrumentations().add(instrumentation);
            instrumentationRepository.persistAndFlush(instrumentation);
        }

        AttachmentEntity attachment = new AttachmentEntity();
        attachment.setType(
                request.getAttachmentType() != null ? request.getAttachmentType() : AttachmentType.UNSPECIFIED);
        attachment.setDisplayName(document.getFilename());
        attachment.setDocument(document);
        documentRepository.incrementRefCount(document);
        attachmentRepository.persistAndFlush(attachment);

        if (instrumentation != null) {
            if (instrumentation.getAttachments() == null) {
                instrumentation.setAttachments(new HashSet<>());
            }
            instrumentation.getAttachments().add(attachment);
        } else {
            if (sheet.getAttachments() == null) {
                sheet.setAttachments(new HashSet<>());
            }
            sheet.getAttachments().add(attachment);
            sheetRepository.persist(sheet);
        }

        log.info(
                "Applied classification for document {}: sheet={}, instrumentation={}, attachment={}",
                documentId,
                sheet.getId(),
                instrumentation != null ? instrumentation.getId() : null,
                attachment.getId());

        return new ClassificationApplyResult(
                sheet.getId(), instrumentation != null ? instrumentation.getId() : null, attachment.getId());
    }

    // -------------------------------------------------------------------------
    // Suggestion building (Option A)
    // -------------------------------------------------------------------------

    /**
     * Builds a pre-filled {@link ClassificationApplyRequest} from classification results and
     * pre-matched entity candidates. The user can review and adjust before calling apply.
     */
    private ClassificationApplyRequest buildSuggestion(
            SheetAnalyzerResult result,
            UUID matchedComposerId,
            UUID matchedArrangerId,
            UUID matchedSheetId,
            List<InstrumentMatch> instrumentCandidates) {

        ClassificationApplyRequest req = new ClassificationApplyRequest();

        // Sheet
        req.setSheetId(matchedSheetId);
        req.setTitle(result.title());
        req.setSubtitle(result.subtitle());
        req.setPublisher(result.publisher());
        req.setYearOfComposition(result.yearOfComposition());
        req.setEdition(result.edition());
        req.setIswc(result.iswc());

        // Genre — map string to enum best-effort
        if (result.genre() != null) {
            try {
                req.setGenre(Genre.valueOf(result.genre().toUpperCase().replace(' ', '_')));
            } catch (IllegalArgumentException ignored) {
                // leave null if no matching enum constant
            }
        }

        // Composer
        if (matchedComposerId != null) {
            req.setComposerId(matchedComposerId);
        } else {
            req.setComposerName(result.composer());
        }

        // Arranger
        if (matchedArrangerId != null) {
            req.setArrangerId(matchedArrangerId);
        } else {
            req.setArrangerName(result.arranger());
        }

        // Instrument — use top candidate if score is high enough
        SheetAnalyzerResult.InstrumentationAnalyzerResult instr = result.instrumentation();
        if (instr != null) {
            req.setPartLabel(instr.partLabel());
            req.setClef(instr.clef());
            req.setNotationType(instr.notationType());

            if (!instrumentCandidates.isEmpty() && instrumentCandidates.get(0).score() >= 0.5) {
                req.setInstrumentId(instrumentCandidates.get(0).id());
            } else {
                req.setInstrumentName(instr.instrumentName());
            }
        }

        return req;
    }

    // -------------------------------------------------------------------------
    // Agentic resolution (Option B)
    // -------------------------------------------------------------------------

    /**
     * Uses the {@link ClassificationAgent} to autonomously resolve entity references via tool calls.
     * Falls back to the pre-built suggestion on any error.
     */
    private ClassificationApplyRequest resolveWithAgent(
            SheetAnalyzerResult result, ClassificationApplyRequest fallback) {
        try {
            String metadata = toDescription(result);
            log.debug("Running agentic classification with metadata:\n{}", metadata);
            ClassificationApplyRequest resolved = classificationAgent.resolve(metadata);
            log.info("Agentic classification resolved successfully");
            return resolved;
        } catch (Exception e) {
            log.warn("Agentic classification failed, falling back to pre-filled suggestion: {}", e.getMessage());
            return fallback;
        }
    }

    /**
     * Formats a {@link SheetAnalyzerResult} as a human-readable text block for the agent prompt.
     */
    private static String toDescription(SheetAnalyzerResult r) {
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, "Title", r.title());
        appendIfPresent(sb, "Subtitle", r.subtitle());
        appendIfPresent(sb, "Publisher", r.publisher());
        appendIfPresent(sb, "Composer", r.composer());
        appendIfPresent(sb, "Arranger", r.arranger());
        appendIfPresent(sb, "Genre", r.genre());
        if (r.yearOfComposition() != null)
            sb.append("Year: ").append(r.yearOfComposition()).append("\n");
        appendIfPresent(sb, "Edition", r.edition());
        appendIfPresent(sb, "ISWC", r.iswc());
        if (r.instrumentation() != null) {
            SheetAnalyzerResult.InstrumentationAnalyzerResult i = r.instrumentation();
            appendIfPresent(sb, "Instrument", i.instrumentName());
            appendIfPresent(sb, "Part label", i.partLabel());
            appendIfPresent(sb, "Part number", i.partNumber());
            appendIfPresent(sb, "Transposition", i.transposition());
            if (i.clef() != null) sb.append("Clef: ").append(i.clef()).append("\n");
            if (i.notationType() != null)
                sb.append("Notation type: ").append(i.notationType()).append("\n");
        }
        return sb.toString().trim();
    }

    private static void appendIfPresent(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append(": ").append(value).append("\n");
        }
    }

    // -------------------------------------------------------------------------
    // Entity resolution helpers
    // -------------------------------------------------------------------------

    private UUID matchMusician(String name) {
        if (name == null || name.isBlank()) return null;
        return musicianRepository
                .findMusicianByName(name)
                .map(MusicianEntity::getId)
                .orElse(null);
    }

    private MusicianEntity resolveMusician(UUID id, String name) {
        if (id != null) {
            return musicianRepository.findByIdOptional(id).orElse(null);
        }
        if (name != null && !name.isBlank()) {
            return musicianRepository.findMusicianByName(name).orElseGet(() -> {
                MusicianEntity m = new MusicianEntity();
                m.setName(name);
                musicianRepository.persist(m);
                return m;
            });
        }
        return null;
    }

    private SheetMusicEntity resolveSheet(
            ClassificationApplyRequest request, MusicianEntity composer, MusicianEntity arranger) {
        if (request.getSheetId() != null) {
            return sheetRepository
                    .findByIdOptional(request.getSheetId())
                    .orElseThrow(() -> new EntityNotFoundException("Sheet", request.getSheetId()));
        }

        SheetMusicEntity sheet = new SheetMusicEntity();
        sheet.setTitle(request.getTitle() != null && !request.getTitle().isBlank() ? request.getTitle() : "Unknown");
        sheet.setSubtitle(request.getSubtitle());
        sheet.setPublisher(request.getPublisher());
        sheet.setComposer(composer);
        sheet.setArranger(arranger);
        sheet.setYearOfComposition(request.getYearOfComposition());
        sheet.setEdition(request.getEdition());
        sheet.setIswc(request.getIswc());
        sheet.setGenre(request.getGenre());
        sheetRepository.persistAndFlush(sheet);
        return sheet;
    }

    private InstrumentEntity resolveInstrument(String id, String name) {
        if (id != null) {
            return instrumentRepository.findByIdOptional(id).orElse(null);
        }
        if (name != null && !name.isBlank()) {
            return instrumentRepository.findByName(name).orElseGet(() -> {
                String slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("-+$", "");
                InstrumentEntity inst = new InstrumentEntity();
                inst.setId(slug);
                inst.setName(name);
                inst.setDisplayName(name);
                instrumentRepository.persist(inst);
                return inst;
            });
        }
        return null;
    }
}
