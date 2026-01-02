package de.halbmann.storage.s3;

import de.halbmann.storage.api.FileSystemWrapper;
import de.halbmann.storage.spi.FileSystemProvider;
import de.halbmann.storage.spi.StorageLocation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.s3.S3Client;

@ApplicationScoped
public class S3FileSystemProvider implements FileSystemProvider {

    @Inject
    S3Client s3;

    @Override
    public boolean supports(StorageLocation location) {
        return "s3".equalsIgnoreCase(location.scheme());
    }

    @Override
    public FileSystemWrapper create(StorageLocation location) {
        String bucket = location.authority();
        String prefix = location.path().replaceFirst("^/", "");
        return new S3FileSystemWrapper(s3, bucket, prefix);
    }

}
