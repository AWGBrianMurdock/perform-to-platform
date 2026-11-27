package training.performtoplatform.spring;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
final class CreditDemo implements CommandLineRunner {
    private final CreditSubmissionService service;

    CreditDemo(CreditSubmissionService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        CreditRequest request = new CreditRequest("REQ-DEMO", "INV-DEMO", new BigDecimal("12.50"));
        SubmissionOutcome outcome = service.submit(request, "USER-COURSE-001");
        System.out.println("Submission result for " + request.requestId() + ": " + outcome);
    }
}
