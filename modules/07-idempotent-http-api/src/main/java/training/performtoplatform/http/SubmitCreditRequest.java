package training.performtoplatform.http;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubmitCreditRequest(
        @NotBlank @Pattern(regexp = "[A-Z0-9-]{1,32}") String requestId,
        @Size(max = 32) String invoiceNumber,
        @NotNull BigDecimal requestedAmount) {
}
