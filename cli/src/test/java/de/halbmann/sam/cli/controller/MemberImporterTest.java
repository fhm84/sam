package de.halbmann.sam.cli.controller;

import static de.halbmann.sam.cli.controller.ImporterTestSupport.page;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.halbmann.sam.api.boundary.MembersResource;
import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.cli.entity.ImportResult;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MemberImporterTest {

    MembersResource members = mock(MembersResource.class);
    MemberImporter importer;

    @BeforeEach
    void setUp() {
        SamResources client = mock(SamResources.class);
        when(client.members()).thenReturn(members);
        when(members.findMembers(any())).thenReturn(page(List.of()));
        importer = ImporterTestSupport.wire(new MemberImporter());
        importer.client = client;
    }

    @Test
    void memberFile_isAccepted() {
        ImportResult result = importer.importFile(
                Paths.get("", "src", "test", "resources", "test-member.json").toFile(), true, false);

        assertEquals(ImportResult.Status.CREATED, result.status());
        verify(members, never()).add(any());
    }

    @Test
    void personFile_isRejected() {
        // A person export parses as a member (unknown fields are ignored) but must not become one
        ImportResult result = importer.importFile(
                Paths.get("", "src", "test", "resources", "test-person.json").toFile(), true, false);

        assertEquals(ImportResult.Status.FAILED, result.status());
        verify(members, never()).findMembers(any());
    }
}
