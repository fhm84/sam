package de.halbmann.sam.api.entity.admin;

/**
 * Minimal user representation sourced from the Keycloak Admin API.
 */
public record UserInfo(String id, String username, String email, String firstName, String lastName) {

    /** Returns a human-readable display label for use in dropdowns. */
    public String displayLabel() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) sb.append(firstName).append(" ");
        if (lastName != null && !lastName.isBlank()) sb.append(lastName);
        String fullName = sb.toString().strip();
        if (!fullName.isEmpty() && email != null) return fullName + " (" + email + ")";
        if (!fullName.isEmpty()) return fullName;
        if (email != null) return email;
        return username != null ? username : id;
    }
}
