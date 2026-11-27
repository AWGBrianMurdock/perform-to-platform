package training.performtoplatform.debugging;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreditLimitTest {
    @Test
    void allowsRequestForExactlyTheAvailableCredit() {
        assertTrue(CreditLimit.allows(new BigDecimal("100.00"), new BigDecimal("100.00")));
    }

    @ParameterizedTest(name = "{0} requested with {1} available expects allowed={2}")
    @CsvSource({
            "99.99, 100.00, true",
            "100.01, 100.00, false",
            "0.01, 0.00, false"
    })
    void preservesNeighboringAndZeroCreditCases(String requested, String available, boolean allowed) {
        assertEquals(allowed, CreditLimit.allows(new BigDecimal(requested), new BigDecimal(available)));
    }
}
