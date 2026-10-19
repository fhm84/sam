package de.halbmann.sam.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.halbmann.sam.api.entity.admin.UserInfo;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;

class KeycloakUserDirectoryTest {

    private final KeycloakUserDirectory directory = new KeycloakUserDirectory();
    private final Keycloak keycloak = mock(Keycloak.class, RETURNS_DEEP_STUBS);

    @BeforeEach
    void setUp() {
        directory.keycloak = keycloak;
        directory.realm = "sam";
    }

    @Test
    void search_mapsRealmUsers() {
        UserRepresentation rep = new UserRepresentation();
        rep.setId("kc-1");
        rep.setUsername("alice");
        rep.setEmail("alice@example.org");
        rep.setFirstName("Alice");
        rep.setLastName("Smith");
        when(keycloak.realm("sam").users().search("ali", 0, 20)).thenReturn(List.of(rep));

        assertEquals(
                List.of(new UserInfo("kc-1", "alice", "alice@example.org", "Alice", "Smith")), directory.search("ali"));
    }

    @Test
    void find_unknownUser_isEmpty() {
        when(keycloak.realm("sam").users().get("missing").toRepresentation()).thenThrow(new NotFoundException());

        assertTrue(directory.find("missing").isEmpty());
    }
}
