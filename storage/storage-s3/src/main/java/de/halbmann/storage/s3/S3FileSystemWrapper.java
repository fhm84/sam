package de.halbmann.storage.s3;

import de.halbmann.storage.api.FileSystemWrapper;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class S3FileSystemWrapper implements FileSystemWrapper {

    private final S3Client s3;
    private final String bucket;
    private final String basePrefix;

    public S3FileSystemWrapper(S3Client s3, String bucket, String basePrefix) {
        this.s3 = s3;
        this.bucket = bucket;
        this.basePrefix = normalizePrefix(basePrefix);
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        prefix = prefix.replace("\\", "/");
        if (!prefix.endsWith("/")) {
            prefix += "/";
        }
        if (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        return prefix;
    }

    private String key(String relativePath) {
        return basePrefix + relativePath.replace("\\", "/");
    }

    // ------------------------------------------------------------

    @Override
    public OutputStream openForWrite(String relativePath) throws IOException {
        Path tempFile = Files.createTempFile("s3-upload-", ".tmp");

        return new OutputStream() {

            private final OutputStream fileOut =
                    Files.newOutputStream(tempFile);

            private boolean closed = false;

            @Override
            public void write(int b) throws IOException {
                fileOut.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                fileOut.write(b, off, len);
            }

            @Override
            public void close() throws IOException {
                if (closed) return;
                closed = true;

                fileOut.close();

                try {
                    s3.putObject(
                            PutObjectRequest.builder()
                                    .bucket(bucket)
                                    .key(key(relativePath))
                                    .build(),
                            RequestBody.fromFile(tempFile)
                    );
                } catch (S3Exception e) {
                    throw new IOException("Failed to upload to S3", e);
                } finally {
                    Files.deleteIfExists(tempFile);
                }
            }
        };
    }

    @Override
    public InputStream openForRead(String relativePath) throws IOException {
        try {
            return s3.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(key(relativePath))
                            .build()
            );
        } catch (S3Exception e) {
            throw new IOException("Failed to read from S3", e);
        }
    }

    @Override
    public boolean exists(String relativePath) {
        try {
            s3.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(key(relativePath))
                            .build()
            );
            return true;
        } catch (S3Exception e) {
            return false;
        }
    }

    @Override
    public Path resolve(String relativePath) {
        // Pseudo-path (for logging/debugging only)
        return Path.of("s3://" + bucket + "/" + key(relativePath));
    }

    @Override
    public void move(String sourceRelativePath, String targetRelativePath) throws IOException {
        String sourceKey = key(sourceRelativePath);
        String targetKey = key(targetRelativePath);

        try {
            s3.copyObject(
                    CopyObjectRequest.builder()
                            .sourceBucket(bucket)
                            .sourceKey(sourceKey)
                            .destinationBucket(bucket)
                            .destinationKey(targetKey)
                            .build()
            );
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(sourceKey)
                    .build());
        } catch (S3Exception e) {
            throw new IOException("Failed to move S3 object", e);
        }
    }

    @Override
    public void delete(String relativePath) throws IOException {
        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key(relativePath))
                    .build());
        } catch (S3Exception e) {
            throw new IOException("Failed to delete S3 object", e);
        }
    }

}

