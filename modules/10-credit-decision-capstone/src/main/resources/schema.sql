CREATE TABLE credit_request (
    request_id VARCHAR(32) PRIMARY KEY,
    invoice_number VARCHAR(32),
    requested_amount DECIMAL(12, 2) NOT NULL CHECK (requested_amount > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED'
        CHECK (status IN ('SUBMITTED', 'APPROVED', 'DENIED'))
);

CREATE TABLE credit_history (
    history_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    request_id VARCHAR(32) NOT NULL REFERENCES credit_request(request_id),
    event_type VARCHAR(20) NOT NULL,
    actor_user_id VARCHAR(40) NOT NULL,
    event_reason VARCHAR(500),
    CONSTRAINT nonblank_history_actor_id CHECK (CHAR_LENGTH(TRIM(actor_user_id)) > 0)
);

CREATE TABLE credit_decision (
    request_id VARCHAR(32) PRIMARY KEY REFERENCES credit_request(request_id),
    decision VARCHAR(10) NOT NULL CHECK (decision IN ('APPROVED', 'DENIED')),
    decided_by_user_id VARCHAR(40) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    CONSTRAINT nonblank_decision_user_id CHECK (CHAR_LENGTH(TRIM(decided_by_user_id)) > 0)
);
