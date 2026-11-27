package training.performtoplatform.modeling;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static training.performtoplatform.modeling.ValidationOutcome.AMOUNT_NOT_POSITIVE;
import static training.performtoplatform.modeling.ValidationOutcome.INVOICE_BEFORE_CUTOFF;
import static training.performtoplatform.modeling.ValidationOutcome.INVOICE_REQUIRED;
import static training.performtoplatform.modeling.ValidationOutcome.VALID;

class CreditValidationTest {
    private static final LocalDate CUTOFF = LocalDate.of(2026, 1, 1);
    private static final LocalDate DAY_BEFORE = CUTOFF.minusDays(1);
    private static final LocalDate DAY_AFTER = CUTOFF.plusDays(1);

    @ParameterizedTest(name = "{0}")
    @MethodSource("validationCases")
    void normalizesAndValidates(
            String caseName, String invoiceNumber, String amount, LocalDate invoiceDate,
            String expectedAmount, ValidationOutcome expectedOutcome) {
        CreditRequest request = new CreditRequest(invoiceNumber, new BigDecimal(amount), invoiceDate);

        assertAll(caseName,
                () -> assertEquals(new BigDecimal(expectedAmount), request.requestedAmount(), "amount and scale"),
                () -> assertEquals(expectedOutcome, CreditValidation.validate(request, CUTOFF), "validation outcome"));
    }

    private static Stream<Arguments> validationCases() {
        return Stream.of(
                arguments("valid request after cutoff", "INV-1001", "12.50", DAY_AFTER, "12.50", VALID),
                arguments("cutoff day is valid and scale becomes two", "INV-1001", "12.5", CUTOFF, "12.50", VALID),
                arguments("day before cutoff", "INV-1001", "12.50", DAY_BEFORE, "12.50", INVOICE_BEFORE_CUTOFF),
                arguments("below rounding tie", "INV-1001", "12.504", DAY_AFTER, "12.50", VALID),
                arguments("rounding tie", "INV-1001", "12.505", DAY_AFTER, "12.51", VALID),
                arguments("half cent rounds to one cent", "INV-1001", "0.005", DAY_AFTER, "0.01", VALID),
                arguments("amount rounds to zero", "INV-1001", "0.004", DAY_AFTER, "0.00", AMOUNT_NOT_POSITIVE),
                arguments("zero amount", "INV-1001", "0", DAY_AFTER, "0.00", AMOUNT_NOT_POSITIVE),
                arguments("negative half-cent tie", "INV-1001", "-0.005", DAY_AFTER, "-0.01", AMOUNT_NOT_POSITIVE),
                arguments("blank invoice number", "            ", "12.50", DAY_AFTER, "12.50", INVOICE_REQUIRED),
                arguments("missing invoice reference", null, "12.50", DAY_AFTER, "12.50", INVOICE_REQUIRED),
                arguments("missing invoice wins over zero and cutoff", "", "0", DAY_BEFORE, "0.00", INVOICE_REQUIRED),
                arguments("amount rejection wins over cutoff", "INV-1001", "0", DAY_BEFORE, "0.00", AMOUNT_NOT_POSITIVE)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unparseableInputs")
    void malformedArgumentsFailBeforeValidation(
            String caseName, String amount, String invoiceDate, String cutoffDate,
            Class<? extends RuntimeException> expectedFailure) {
        assertThrows(expectedFailure,
                () -> CreditValidation.main(new String[]{"INV-1001", amount, invoiceDate, cutoffDate}), caseName);
    }

    private static Stream<Arguments> unparseableInputs() {
        return Stream.of(
                arguments("malformed amount", "twelve", "2026-01-02", "2026-01-01", NumberFormatException.class),
                arguments("invalid invoice date", "12.50", "2026-02-30", "2026-01-01", DateTimeParseException.class),
                arguments("malformed cutoff", "12.50", "2026-01-02", "January", DateTimeParseException.class)
        );
    }
}
