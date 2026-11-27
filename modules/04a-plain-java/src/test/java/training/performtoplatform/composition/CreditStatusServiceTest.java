package training.performtoplatform.composition;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreditStatusServiceTest {
    @Test
    void reportsFoundAndMissingRequests() {
        CreditStatusRepository repository = new InMemoryCreditStatusRepository(Map.of("REQ-1001", "SUBMITTED"));
        CreditStatusService service = new CreditStatusService(repository);

        assertEquals("SUBMITTED", service.lookupStatus("REQ-1001"));
        assertEquals("NOT_FOUND", service.lookupStatus("REQ-9999"));
    }
}
