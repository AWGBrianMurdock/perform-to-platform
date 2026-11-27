package training.performtoplatform.credit;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class CreditValidationTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("creditCases")
    void returnsExpectedStatus(String caseName, String invoiceNumber, long requestedCents, String expectedStatus) {
        assertEquals(expectedStatus, CreditValidation.validate(invoiceNumber, requestedCents), caseName);
    }

    private static Stream<Arguments> creditCases() {
        return Stream.of(
                arguments("valid request", "INV-1001", 1_250L, "00"),
                arguments("smallest positive amount", "INV-1001", 1L, "00"),
                arguments("largest amount that fits the COBOL field", "INV-1001", 999_999_999L, "00"),
                arguments("valid invoice padded with spaces", "INV-1001    ", 1_250L, "00"),
                arguments("null Java reference", null, 1_250L, "04"),
                arguments("empty Java string", "", 1_250L, "04"),
                arguments("COBOL field filled with spaces", "            ", 1_250L, "04"),
                arguments("Java whitespace-only string", "\t", 1_250L, "04"),
                arguments("zero amount", "INV-1001", 0L, "08"),
                arguments("negative amount", "INV-1001", -1L, "08"),
                arguments("missing invoice takes precedence over invalid amount", "            ", 0L, "04")
        );
    }
}
