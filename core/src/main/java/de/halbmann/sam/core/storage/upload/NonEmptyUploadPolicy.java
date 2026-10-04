package de.halbmann.sam.core.storage.upload;

import de.halbmann.sam.core.exception.ValidationException;
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
            // A rejected upload is a client error (400), not an I/O failure (500)
            throw new ValidationException("Uploaded file is empty");
        }
    }
}
