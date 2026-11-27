package training.performtoplatform.http;

final class CreditRequestNotFoundException extends RuntimeException {
    CreditRequestNotFoundException(String requestId) {
        super("No credit request exists with ID " + requestId + ".");
    }
}
