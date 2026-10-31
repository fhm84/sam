package de.halbmann.storage.s3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

@ExtendWith(MockitoExtension.class)
class S3FileSystemWrapperListTest {

    @Mock
    S3Client s3;

    @Test
    void list_returnsKeysRelativeToThePrefix_withoutDirectoryMarkers() throws Exception {
        when(s3.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
                .thenAnswer(inv -> new ListObjectsV2Iterable(s3, inv.getArgument(0)));
        when(s3.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder()
                        .contents(
                                S3Object.builder().key("prefix/ab/cd/file").build(),
                                S3Object.builder().key("prefix/ab/").build(),
                                S3Object.builder()
                                        .key("prefix/.upload-tmp/x.tmp")
                                        .build())
                        .isTruncated(false)
                        .build());
        S3FileSystemWrapper wrapper = new S3FileSystemWrapper(s3, "my-bucket", "prefix/");

        try (Stream<String> files = wrapper.list()) {
            assertEquals(List.of("ab/cd/file", ".upload-tmp/x.tmp"), files.toList());
        }

        ArgumentCaptor<ListObjectsV2Request> request = ArgumentCaptor.forClass(ListObjectsV2Request.class);
        verify(s3).listObjectsV2(request.capture());
        assertEquals("my-bucket", request.getValue().bucket());
        assertEquals("prefix/", request.getValue().prefix());
    }
}
