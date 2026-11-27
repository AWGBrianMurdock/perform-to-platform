package training.performtoplatform.http;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/credit-requests")
public class CreditRequestController {
    private final CreditSubmissionService service;
    private final CourseUserIdentity courseUserIdentity;

    CreditRequestController(CreditSubmissionService service, CourseUserIdentity courseUserIdentity) {
        this.service = service;
        this.courseUserIdentity = courseUserIdentity;
    }

    @PostMapping
    public ResponseEntity<SubmissionResponse> submit(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody SubmitCreditRequest body) {
        CreditRequest request = new CreditRequest(body.requestId(), body.invoiceNumber(), body.requestedAmount());
        SubmissionResult result = service.submit(idempotencyKey, request, courseUserIdentity.userId());
        SubmissionResponse response = new SubmissionResponse(request.requestId(), result.outcome(), result.replayed());

        if (result.outcome() != SubmissionOutcome.ACCEPTED) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(response);
        }
        if (result.replayed()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.created(URI.create("/credit-requests/" + request.requestId())).body(response);
    }

    @GetMapping("/{requestId}")
    public CreditStatusResponse status(@PathVariable String requestId) {
        String status = service.findStatus(requestId)
                .orElseThrow(() -> new CreditRequestNotFoundException(requestId));
        return new CreditStatusResponse(requestId, status);
    }
}
