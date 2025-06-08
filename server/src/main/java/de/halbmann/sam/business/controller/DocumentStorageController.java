package de.halbmann.sam.business.controller;

import de.halbmann.sam.EnvConsts;
import de.halbmann.sam.business.entity.AttachmentEntity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.java.Log;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;

/**
 * Controller class for managing how/where the content of the file is stored. The content is stored in the destination configured storage defined by {@link EnvConsts#FILESYSTEM_BASE_PATH}.
 */
@Log
@RequestScoped
public class DocumentStorageController {

    // TODO: normalize filenames!?

    @Inject
    @ConfigProperty(name = EnvConsts.FILESYSTEM_BASE_PATH)
    Optional<String> fsBasePath;

    public void save(final AttachmentEntity attachment, final InputStream inputStream) {
        final CheckedInputStream checkedInputStream = prepareForChecksumCalculation(inputStream);
        try (final FileObject fileObject = uploadFile(fsBasePath.get(), attachment.getReferencePath(),
                attachment.getDocIdentifier(), checkedInputStream)) {
            // nothing to do here
        } catch (final IOException e) {
            throw new RuntimeException(e);
        } finally {
            attachment.setChecksum(checkedInputStream.getChecksum().getValue());
        }
    }

    public void update(final AttachmentEntity attachment, final InputStream inputStream) {
        final CheckedInputStream checkedInputStream = prepareForChecksumCalculation(inputStream);
        try {
            overwriteFile(fsBasePath.get(), attachment.getReferencePath(),
                    attachment.getDocIdentifier(), inputStream);
            attachment.setChecksum(checkedInputStream.getChecksum().getValue());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream load(final AttachmentEntity attachment) throws IOException {
        return loadDocument(fsBasePath.get(), attachment.getReferencePath(),
                attachment.getDocIdentifier());
    }

    public void delete(final AttachmentEntity attachment) {
        try {
            if (fileExists(fsBasePath.get(), attachment.getReferencePath(), attachment.getDocIdentifier())) {
                deleteFile(fsBasePath.get(), attachment.getReferencePath(), attachment.getDocIdentifier());
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream loadDocument(final String baseDirPath, final String referencePath, final String fileName)
            throws IOException {
        final String absolutePath = getAbsolutePath(baseDirPath, referencePath, fileName);
        if (absolutePath.isBlank()) {
            return null;
        }

        final FileSystemManager vfsManager = VFS.getManager();
        final FileObject file = vfsManager.resolveFile(absolutePath);
        if (file.exists()) {
            return new AutoCloseCallbackInputStream(file.getContent().getInputStream(), f -> {
                try {
                    file.close();
                } catch (final FileSystemException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        return null;
    }

    private FileObject uploadFile(final String baseDirPath, final String referencePath, final String fileName,
                                  final InputStream inputStream) throws IOException {
        final FileSystemManager fsManager = VFS.getManager();
        try (final FileObject remoteDir = fsManager.resolveFile(baseDirPath)) {
            remoteDir.createFolder();
        }

        final FileObject destFile = fsManager.resolveFile(getAbsolutePath(baseDirPath, referencePath, fileName));
        if (!destFile.exists()) {
            destFile.createFile();
        }
        try (final OutputStream os = destFile.getContent().getOutputStream()) {
            inputStream.transferTo(os);
        }
        if (!destFile.exists() || destFile.getContent().getSize() == 0) {
            destFile.close();
            throw new WebApplicationException("File could not be stored/uploaded");
        }

        return destFile;
    }

    private boolean overwriteFile(final String baseDirPath, final String referencePath, final String fileName,
                                  final InputStream inputStream) throws IOException {
        final FileSystemManager fsManager = VFS.getManager();

        try (final FileObject destFile = fsManager.resolveFile(getAbsolutePath(baseDirPath, referencePath, fileName))) {
            if (!destFile.exists()) {
                destFile.createFile();
            }
            try (final OutputStream os = destFile.getContent().getOutputStream()) {
                inputStream.transferTo(os);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

            return destFile.exists() && destFile.getContent().getSize() > 0;
        }
    }

    private boolean fileExists(final String baseDirPath, final String referencePath, final String fileName)
            throws IOException {
        final FileSystemManager fsManager = VFS.getManager();
        try (final FileObject destFile = fsManager.resolveFile(getAbsolutePath(baseDirPath, referencePath, fileName))) {
            return destFile.exists();
        }
    }

    private boolean fileExists(final String referencePath) throws IOException {
        final FileSystemManager fsManager = VFS.getManager();
        try (final FileObject destFile = fsManager.resolveFile(referencePath)) {
            return destFile.exists();
        }
    }

    private void deleteFile(final String baseDirPath, final String referencePath, final String fileName)
            throws IOException {
        final FileSystemManager fsManager = VFS.getManager();
        try (final FileObject destFile = fsManager.resolveFile(getAbsolutePath(baseDirPath, referencePath, fileName))) {
            if (!destFile.exists()) {
                throw new NotFoundException("Could not find document " + fileName);
            }
            destFile.delete();
        }
    }

    private String getAbsolutePath(final String baseDirPath, final String referencePath, final String fileName) {
        return baseDirPath + "/" + referencePath + "/" + fileName;
    }

    public long calculateChecksum(final InputStream in) {
        try (final CheckedInputStream checkedInputStream = prepareForChecksumCalculation(in)) {
            final byte[] buffer = new byte[8192];
            while (true) {
                if (checkedInputStream.read(buffer, 0, buffer.length) < 0) {
                    break;
                }
            }
            return checkedInputStream.getChecksum().getValue();
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    CheckedInputStream prepareForChecksumCalculation(final InputStream is) {
        final Checksum adler32 = new Adler32();
        return new CheckedInputStream(new BufferedInputStream(is), adler32);
    }

}