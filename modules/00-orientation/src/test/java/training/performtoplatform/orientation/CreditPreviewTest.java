package training.performtoplatform.orientation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreditPreviewTest {
    @Test
    void includesTheSuppliedInvoiceIdInThePreview() {
        assertEquals("Credit preview for invoice INV-2042", CreditPreview.previewMessage("INV-2042"));
    }
}
