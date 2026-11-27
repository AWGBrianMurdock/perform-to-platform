package training.performtoplatform.http;

final class InvalidIdempotencyKeyException extends RuntimeException {
    InvalidIdempotencyKeyException() {
        super("The Idempotency-Key header must contain 1 to 64 letters, digits, or hyphens.");
    }
}
