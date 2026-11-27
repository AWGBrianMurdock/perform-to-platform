package training.performtoplatform.http;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class CreditApiExceptionHandler {
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ProblemDetail> badRequest() {
        return problem(HttpStatus.BAD_REQUEST, "The request body is missing or contains invalid fields.");
    }

    @ExceptionHandler(CreditRequestNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(CreditRequestNotFoundException failure) {
        return problem(HttpStatus.NOT_FOUND, failure.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<ProblemDetail> conflict() {
        return problem(HttpStatus.CONFLICT, "A credit request with that ID already exists.");
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ProblemDetail> databaseUnavailable() {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "The request could not be saved because storage is unavailable.");
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
