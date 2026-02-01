package de.halbmann.sam.storage.upload;

import java.io.IOException;

public interface UploadPolicy {

    void verify(UploadContext context) throws IOException;
}
