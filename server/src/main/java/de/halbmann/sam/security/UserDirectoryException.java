package de.halbmann.sam.security;

/** The OIDC provider's user API failed or is unreachable; mapped to HTTP 502. */
public class UserDirectoryException extends RuntimeException {

    public UserDirectoryException(String message) {
        super(message);
    }

    public UserDirectoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
