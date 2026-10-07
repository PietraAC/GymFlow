package com.gymflow.gym.shared.error;

public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) { super(message); }
}

