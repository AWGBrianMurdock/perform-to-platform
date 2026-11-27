CREATE TABLE credit_request (
    request_id VARCHAR(32) PRIMARY KEY,
    invoice_number VARCHAR(32),
    requested_amount DECIMAL(12, 2) NOT NULL CHECK (requested_amount > 0)
);

CREATE TABLE credit_history (
    history_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    request_id VARCHAR(32) NOT NULL REFERENCES credit_request(request_id),
    event_type VARCHAR(20) NOT NULL,
    submitted_by_user_id VARCHAR(40) NOT NULL,
    CONSTRAINT nonblank_history_user_id CHECK (CHAR_LENGTH(TRIM(submitted_by_user_id)) > 0)
);
