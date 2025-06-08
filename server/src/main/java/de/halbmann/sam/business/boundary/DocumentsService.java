package de.halbmann.sam.business.boundary;

import de.halbmann.sam.api.entity.Attachment;
import de.halbmann.sam.business.controller.AttachmentMapper;
import de.halbmann.sam.business.controller.CountingInputStream;
import de.halbmann.sam.business.controller.DocumentStorageController;
import de.halbmann.sam.business.controller.MimeTypeController;
import de.halbmann.sam.business.entity.AbstractEntity;
import de.halbmann.sam.business.entity.AttachmentEntity;
import de.halbmann.sam.business.entity.AttachmentWrapper;
import de.halbmann.sam.business.entity.InstrumentationEntity;
import de.halbmann.sam.classification.boundary.SheetAnalyzer;
import de.halbmann.sam.classification.controller.DocumentUtils;
import de.halbmann.sam.classification.entity.SheetAnalyzerResult;
import dev.langchain4j.data.image.Image;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@ApplicationScoped
@Transactional
public class DocumentsService {

    private final MimeTypeController mimeTypeController;
    private final DocumentStorageController documentStorageController;
    private final InstrumentationRepository instrumentationRepository;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentMapper attachmentMapper;
    private final SheetAnalyzer analyzer;

    private final boolean debug = true;

    public DocumentsService(MimeTypeController mimeTypeController,
                            DocumentStorageController documentStorageController,
                            InstrumentationRepository instrumentationRepository,
                            AttachmentRepository attachmentRepository,
                            AttachmentMapper attachmentMapper,
                            SheetAnalyzer sheetAnalyzer) {
        this.mimeTypeController = mimeTypeController;
        this.documentStorageController = documentStorageController;
        this.instrumentationRepository = instrumentationRepository;
        this.attachmentRepository = attachmentRepository;
        this.attachmentMapper = attachmentMapper;
        this.analyzer = sheetAnalyzer;
    }

    /**
     * Save incoming document. This will also directly check the file-/content-type of and automatically add related metadata.
     * <p>
     * Future features:
     * - antivirus/malware checks!?
     *
     * @param filename    the (final/display filename)
     * @param inputStream the file content
     */
    public Attachment save(String filename, InputStream inputStream) {
        final AttachmentEntity attachment = new AttachmentEntity();
        attachment.setDisplayName(filename);
        attachment.setDocIdentifier(UUID.randomUUID().toString());
        // Write the input stream into a buffered input stream so that Tika won't mess up with the file size
        final BufferedInputStream bufferedInputStream = new BufferedInputStream(new CountingInputStream(attachment, inputStream));
        attachment.setMimeType(mimeTypeController.detectMimeType(bufferedInputStream, attachment.getDisplayName()));
        // FIXME: set documentIdentifier and referencePath
        attachment.setReferencePath(attachment.getDocIdentifier());
        // upload/store the file and directly set/update the file size and checksum
        documentStorageController.save(attachment, bufferedInputStream);
        // store document to database
        return attachmentRepository.addAttachment(attachment);
    }

    public AttachmentWrapper loadAttachment(String instrumentationId, String docIdentifier) throws IOException {
        InstrumentationEntity instrumentation = instrumentationRepository.findById(UUID.fromString(instrumentationId));
        if (instrumentation != null) {
            Optional<UUID> attachmentId = instrumentation.getAttachments().stream()
                    .filter(a -> a.getDocIdentifier().equals(docIdentifier))
                    .map(AbstractEntity::getId)
                    .findFirst();

            if (attachmentId.isPresent()) {
                AttachmentEntity attachment = attachmentRepository.findById(attachmentId.get());
                InputStream fileStream = documentStorageController.load(attachment);
                return new AttachmentWrapper(attachmentMapper.toDto(attachment), fileStream);
            }
        }
        return null;
    }

    public SheetAnalyzerResult analyzePdf(InputStream fileInputStream) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // in case of a pdf sheet, we first "prepare" the pdf for analyzing (we just need the top and bottom parts
            // of the file ... and delegate to image analyzation then)
            DocumentUtils.pdfToImage(fileInputStream, baos);
            // in case of debugging, we write out the (temporary) generated image file
            // TODO: write out generated image file in case of debugging
            if (debug) {
                Files.write(Files.createTempFile("sam-sheet-upload", ".png"), baos.toByteArray());
            }

            return analyzeImage(IOUtils.copy(baos));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SheetAnalyzerResult analyzeImage(InputStream fileInputStream) {
        return analyzer.analyze(Image.builder()
                .base64Data(encodeFileToBase64(fileInputStream))
                .mimeType("image/png")
                .build());
    }

    String encodeFileToBase64(InputStream fileInputStream) {
        try {
            return Base64.getEncoder().encodeToString(fileInputStream.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
