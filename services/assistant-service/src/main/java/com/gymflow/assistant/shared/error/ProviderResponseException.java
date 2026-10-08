package com.gymflow.assistant.shared.error;
public class ProviderResponseException extends RuntimeException {
    public ProviderResponseException(String message) { super(message); }
    public ProviderResponseException(String message, Throwable cause) { super(message, cause); }
}
