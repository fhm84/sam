package de.halbmann.storage.s3;

import de.halbmann.storage.api.FileSystemWrapper;
import de.halbmann.storage.spi.FileSystemProvider;
import de.halbmann.storage.spi.StorageLocation;
import jakarta.enterprise.context.ApplicationScoped;
import software.amazon.awssdk.services.s3.S3Client;

@ApplicationScoped
public class S3FileSystemProvider implements FileSystemProvider {

    @Override
    public boolean supports(StorageLocation location) {
        return "s3".equalsIgnoreCase(location.scheme());
    }

    @Override
    public FileSystemWrapper create(StorageLocation location) {
        String bucket = location.authority();
        String prefix = location.path().replaceFirst("^/", "");
        // Built here, not injected: no S3Client CDI producer exists in deployments that use
        // local storage, and the client must only be created when an s3:// path is configured.
        // Region, credentials, and endpoint come from the AWS default chain
        // (AWS_REGION, AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY, AWS_ENDPOINT_URL_S3).
        return new S3FileSystemWrapper(S3Client.create(), bucket, prefix);
    }
}
