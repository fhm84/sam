package de.halbmann.storage.s3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class S3FileSystemWrapperExistsTest {

    @Mock
    S3Client s3;

    S3FileSystemWrapper wrapper;

    @BeforeEach
    void setUp() {
        wrapper = new S3FileSystemWrapper(s3, "my-bucket", "prefix/");
    }

    @Test
    void exists_presentKey_returnsTrue() {
        when(s3.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        assertTrue(wrapper.exists("some/key.pdf"));
    }

    @Test
    void exists_notFound_returnsFalse() {
        S3Exception notFound = (S3Exception)
                S3Exception.builder().statusCode(404).message("Not Found").build();
        when(s3.headObject(any(HeadObjectRequest.class))).thenThrow(notFound);

        assertFalse(wrapper.exists("missing/key.pdf"));
    }

    @Test
    void exists_serverError_rethrows() {
        S3Exception serverError = (S3Exception) S3Exception.builder()
                .statusCode(500)
                .message("Internal Server Error")
                .build();
        when(s3.headObject(any(HeadObjectRequest.class))).thenThrow(serverError);

        S3Exception thrown = assertThrows(S3Exception.class, () -> wrapper.exists("some/key.pdf"));
        assertEquals(500, thrown.statusCode());
    }

    @Test
    void exists_authError_rethrows() {
        S3Exception authError = (S3Exception)
                S3Exception.builder().statusCode(403).message("Forbidden").build();
        when(s3.headObject(any(HeadObjectRequest.class))).thenThrow(authError);

        S3Exception thrown = assertThrows(S3Exception.class, () -> wrapper.exists("some/key.pdf"));
        assertEquals(403, thrown.statusCode());
    }
}
