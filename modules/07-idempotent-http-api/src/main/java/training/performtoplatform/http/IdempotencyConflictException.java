package training.performtoplatform.http;

final class IdempotencyConflictException extends RuntimeException {
    IdempotencyConflictException() {
        super("That idempotency key was already used for a different request.");
    }
}
