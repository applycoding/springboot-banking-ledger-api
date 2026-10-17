CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE customer (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    email VARCHAR(320) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE account (
    id UUID PRIMARY KEY,
    customer_id UUID NULL REFERENCES customer (id),
    account_number VARCHAR(32) NOT NULL UNIQUE,
    account_type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT account_type_chk CHECK (account_type IN ('ASSET', 'LIABILITY')),
    CONSTRAINT account_status_chk CHECK (status IN ('OPEN', 'FROZEN', 'CLOSED')),
    CONSTRAINT account_balance_nonneg_chk CHECK (balance >= 0),
    CONSTRAINT account_owner_chk CHECK (
        (account_type = 'ASSET' AND customer_id IS NULL)
        OR (account_type = 'LIABILITY' AND customer_id IS NOT NULL)
    )
);

CREATE TABLE journal_entry (
    id UUID PRIMARY KEY,
    entry_type VARCHAR(16) NOT NULL,
    description VARCHAR(280) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    request_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT entry_type_chk CHECK (entry_type IN ('DEPOSIT', 'TRANSFER'))
);

CREATE TABLE journal_line (
    id UUID PRIMARY KEY,
    journal_entry_id UUID NOT NULL REFERENCES journal_entry (id),
    account_id UUID NOT NULL REFERENCES account (id),
    direction VARCHAR(8) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    CONSTRAINT direction_chk CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT line_amount_positive_chk CHECK (amount > 0)
);

CREATE INDEX idx_account_customer ON account (customer_id);
CREATE INDEX idx_journal_line_account ON journal_line (account_id);
CREATE INDEX idx_journal_line_entry ON journal_line (journal_entry_id);
