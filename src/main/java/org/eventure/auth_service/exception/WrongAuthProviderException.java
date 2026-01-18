package org.eventure.auth_service.exception;

public class WrongAuthProviderException extends RuntimeException {
    public WrongAuthProviderException(String message) {
        super(message);
    }
}
