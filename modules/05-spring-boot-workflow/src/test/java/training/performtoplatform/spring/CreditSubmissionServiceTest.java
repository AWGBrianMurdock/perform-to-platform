package training.performtoplatform.spring;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class CreditSubmissionServiceTest {
    @Autowired
    private CreditSubmissionService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTables() {
        jdbcTemplate.update("DELETE FROM credit_history");
        jdbcTemplate.update("DELETE FROM credit_request");
    }

    @Test
    void acceptsValidRequestAndStoresItsInitialHistory() {
        CreditRequest request = request("REQ-ACCEPTED", "INV-1001", "12.505");

        SubmissionOutcome outcome = service.submit(request, "USER-COURSE-001");

        assertEquals(SubmissionOutcome.ACCEPTED, outcome);
        assertEquals(1, countRows("credit_request", request.requestId()));
        assertEquals(1, countRows("credit_history", request.requestId()));
        assertEquals(new BigDecimal("12.51"), jdbcTemplate.queryForObject(
                "SELECT requested_amount FROM credit_request WHERE request_id = ?",
                BigDecimal.class, request.requestId()));
        assertEquals("USER-COURSE-001", jdbcTemplate.queryForObject(
                "SELECT submitted_by_user_id FROM credit_history WHERE request_id = ?",
                String.class, request.requestId()));
    }

    @Test
    void rejectsInvalidRequestBeforeWritingEitherRow() {
        SubmissionOutcome missingInvoice = service.submit(
                request("REQ-MISSING", " ", "0.00"), "USER-COURSE-001");
        SubmissionOutcome nonpositiveAmount = service.submit(
                request("REQ-ZERO", "INV-1002", "0.004"), "USER-COURSE-001");

        assertEquals(SubmissionOutcome.INVOICE_REQUIRED, missingInvoice);
        assertEquals(SubmissionOutcome.AMOUNT_NOT_POSITIVE, nonpositiveAmount);
        assertEquals(0, countRows("credit_request", "REQ-MISSING"));
        assertEquals(0, countRows("credit_request", "REQ-ZERO"));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_history", Integer.class));
    }

    @Test
    void rollsBackBothWritesWhenHistoryInsertFailsAndAllowsRetry() {
        CreditRequest request = request("REQ-ROLLBACK", "INV-1003", "5.00");

        assertThrows(DataIntegrityViolationException.class, () -> service.submit(request, " "));

        assertEquals(0, countRows("credit_request", request.requestId()));
        assertEquals(0, countRows("credit_history", request.requestId()));

        assertEquals(SubmissionOutcome.ACCEPTED, service.submit(request, "USER-COURSE-001"));
        assertEquals(1, countRows("credit_request", request.requestId()));
        assertEquals(1, countRows("credit_history", request.requestId()));
    }

    private CreditRequest request(String id, String invoiceNumber, String amount) {
        return new CreditRequest(id, invoiceNumber, new BigDecimal(amount));
    }

    private int countRows(String table, String requestId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE request_id = ?",
                Integer.class, requestId);
    }
}
