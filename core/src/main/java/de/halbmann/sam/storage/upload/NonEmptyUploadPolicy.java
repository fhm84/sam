package de.halbmann.sam.storage.upload;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;

@Priority(10)
@ApplicationScoped
public class NonEmptyUploadPolicy implements UploadPolicy {

    private static final long MIN_SIZE = 1; // bytes

    @Override
    public void verify(UploadContext context) throws IOException {
        if (context.size() < MIN_SIZE) {
            throw new IOException("Uploaded file is too small");
        }
    }

}
