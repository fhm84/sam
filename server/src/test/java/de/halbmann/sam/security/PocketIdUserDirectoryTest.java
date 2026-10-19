package de.halbmann.sam.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.halbmann.sam.api.entity.admin.UserInfo;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PocketIdUserDirectoryTest {

    private final PocketIdStub stub = new PocketIdStub();
    private String url;

    @BeforeEach
    void startStub() {
        url = stub.startServer();
    }

    @AfterEach
    void stopStub() {
        stub.stop();
    }

    private PocketIdUserDirectory directory(String apiKey) {
        return new PocketIdUserDirectory(Optional.of(url + "/"), Optional.ofNullable(apiKey));
    }

    @Test
    void search_returnsEnabledUsersOnly_withTheLimitAndKey() {
        List<UserInfo> users = directory(PocketIdStub.API_KEY).search("example.org");

        assertEquals(
                List.of(new UserInfo(PocketIdStub.ALICE_ID, "alice", "alice@example.org", "Alice", "Smith")), users);
        assertTrue(
                stub.lastRequest.get().getQuery().contains("pagination[limit]=" + PocketIdUserDirectory.LIMIT),
                stub.lastRequest.get().toString());
    }

    @Test
    void search_encodesTheQuery() {
        directory(PocketIdStub.API_KEY).search("a&b c");

        assertTrue(stub.lastRequest.get().getRawQuery().startsWith("search=a%26b+c&"));
    }

    @Test
    void find_returnsTheUser_orEmptyIfUnknown() {
        PocketIdUserDirectory directory = directory(PocketIdStub.API_KEY);

        assertEquals(
                "alice", directory.find(PocketIdStub.ALICE_ID).orElseThrow().username());
        assertTrue(directory.find("6f1c2a3b-0000-4000-8000-0000000000ff").isEmpty());
    }

    @Test
    void find_rejectsIdsThatAreNoPathSegment_withoutCallingPocketId() {
        assertTrue(directory(PocketIdStub.API_KEY).find("../settings").isEmpty());
        assertNull(stub.lastRequest.get());
    }

    @Test
    void withoutKey_searchIsOff_andFindEchoesTheId() {
        PocketIdUserDirectory directory = directory(null);

        assertFalse(directory.searchable());
        assertThrows(IllegalStateException.class, () -> directory.search("alice"));
        assertEquals(
                new UserInfo(PocketIdStub.ALICE_ID, PocketIdStub.ALICE_ID, null, null, null),
                directory.find(PocketIdStub.ALICE_ID).orElseThrow());
        assertNull(stub.lastRequest.get());
    }

    @Test
    void rejectedKey_failsWithoutRevealingIt() {
        UserDirectoryException e = assertThrows(
                UserDirectoryException.class,
                () -> directory("wrong-secret-key").search("alice"));

        assertTrue(e.getMessage().contains("401"), e.getMessage());
        assertFalse(e.getMessage().contains("wrong-secret-key"));
    }

    @Test
    void unreachablePocketId_failsAsDirectoryError() {
        stub.stop();

        assertThrows(
                UserDirectoryException.class,
                () -> directory(PocketIdStub.API_KEY).search("alice"));
    }
}
