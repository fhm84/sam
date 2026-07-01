package de.halbmann.sam.api.entity.admin;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UserInfoTest {

    @Test
    void fullNameAndEmailCombined() {
        UserInfo u = new UserInfo("id1", "jdoe", "j@example.com", "John", "Doe");
        assertEquals("John Doe (j@example.com)", u.displayLabel());
    }

    @Test
    void firstNameOnlyWithEmail() {
        UserInfo u = new UserInfo("id1", "jdoe", "j@example.com", "John", null);
        assertEquals("John (j@example.com)", u.displayLabel());
    }

    @Test
    void lastNameOnlyWithEmail() {
        UserInfo u = new UserInfo("id1", "jdoe", "j@example.com", null, "Doe");
        assertEquals("Doe (j@example.com)", u.displayLabel());
    }

    @Test
    void fullNameNoEmail() {
        UserInfo u = new UserInfo("id1", "jdoe", null, "John", "Doe");
        assertEquals("John Doe", u.displayLabel());
    }

    @Test
    void emailOnlyWhenNoName() {
        UserInfo u = new UserInfo("id1", "jdoe", "j@example.com", null, null);
        assertEquals("j@example.com", u.displayLabel());
    }

    @Test
    void usernameWhenNeitherNameNorEmail() {
        UserInfo u = new UserInfo("id1", "jdoe", null, null, null);
        assertEquals("jdoe", u.displayLabel());
    }

    @Test
    void idFallbackWhenUsernameNull() {
        UserInfo u = new UserInfo("id1", null, null, null, null);
        assertEquals("id1", u.displayLabel());
    }

    @Test
    void blankFirstAndLastNameFallToEmail() {
        UserInfo u = new UserInfo("id1", "jdoe", "j@example.com", "  ", "  ");
        assertEquals("j@example.com", u.displayLabel());
    }
}
