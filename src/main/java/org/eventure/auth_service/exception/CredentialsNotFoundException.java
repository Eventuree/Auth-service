package org.eventure.auth_service.exception;

public class CredentialsNotFoundException extends RuntimeException {
    public CredentialsNotFoundException(String message) {

        super(message);
    }
}
