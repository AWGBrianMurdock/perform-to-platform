package training.performtoplatform.jdbc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;

public final class CreditLab {
    private static final String JDBC_URL = "jdbc:h2:file:./target/credit-lab";
    private static final String USAGE = "Use init, submit <request-id> <invoice-number> <amount> <submitted-by-user-id>, "
            + "or find <request-id>.";

    public static void main(String[] args) throws SQLException {
        if (args.length == 0) {
            throw new IllegalArgumentException(USAGE);
        }
        CreditRepository repository = new CreditRepository(JDBC_URL);
        switch (args[0]) {
            case "init" -> {
                requireArguments(args, 1);
                repository.initializeSchema();
                System.out.println("SCHEMA_READY");
            }
            case "submit" -> {
                requireArguments(args, 5);
                BigDecimal amount = new BigDecimal(args[3]).setScale(2, RoundingMode.UNNECESSARY);
                repository.submit(new CreditRequest(args[1], args[2], amount), args[4]);
                System.out.println("SUBMITTED " + args[1]);
            }
            case "find" -> {
                requireArguments(args, 2);
                String requestId = args[1];
                String output = repository.findById(requestId)
                        .map(request -> "FOUND " + request.requestId() + " " + request.invoiceNumber()
                                + " " + request.requestedAmount().toPlainString())
                        .orElse("NOT_FOUND " + requestId);
                System.out.println(output);
            }
            default -> throw new IllegalArgumentException(USAGE);
        }
    }

    private static void requireArguments(String[] args, int expected) {
        if (args.length != expected) {
            throw new IllegalArgumentException(USAGE);
        }
    }
}
