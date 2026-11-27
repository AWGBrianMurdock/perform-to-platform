package training.performtoplatform.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

final class CreditRepository {
    private final String jdbcUrl;

    CreditRepository(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    void initializeSchema() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS credit_request (
                        request_id VARCHAR(32) PRIMARY KEY,
                        invoice_number VARCHAR(32) NOT NULL,
                        requested_amount DECIMAL(12, 2) NOT NULL CHECK (requested_amount > 0)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS credit_history (
                        history_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        request_id VARCHAR(32) NOT NULL REFERENCES credit_request(request_id),
                        event_type VARCHAR(20) NOT NULL,
                        submitted_by_user_id VARCHAR(40) NOT NULL,
                        CONSTRAINT nonblank_history_user_id CHECK (CHAR_LENGTH(TRIM(submitted_by_user_id)) > 0)
                    )
                    """);
        }
    }

    void submit(CreditRequest request, String submittedByUserId) throws SQLException {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                insertRequest(connection, request);
                insertHistory(connection, request.requestId(), submittedByUserId);
                connection.commit();
            } catch (SQLException | RuntimeException failure) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            }
        }
    }

    Optional<CreditRequest> findById(String requestId) throws SQLException {
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement("""
                        SELECT invoice_number, requested_amount
                        FROM credit_request
                        WHERE request_id = ?
                        """)) {
            statement.setString(1, requestId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new CreditRequest(
                        requestId, result.getString("invoice_number"), result.getBigDecimal("requested_amount")));
            }
        }
    }

    private static void insertRequest(Connection connection, CreditRequest request) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO credit_request (request_id, invoice_number, requested_amount)
                VALUES (?, ?, ?)
                """)) {
            statement.setString(1, request.requestId());
            statement.setString(2, request.invoiceNumber());
            statement.setBigDecimal(3, request.requestedAmount());
            statement.executeUpdate();
        }
    }

    private static void insertHistory(Connection connection, String requestId, String submittedByUserId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO credit_history (request_id, event_type, submitted_by_user_id)
                VALUES (?, 'SUBMITTED', ?)
                """)) {
            statement.setString(1, requestId);
            statement.setString(2, submittedByUserId);
            statement.executeUpdate();
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, "sa", "");
    }
}
