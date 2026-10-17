-- Bank cash account used as the other side of customer deposits.
INSERT INTO account (
    id,
    customer_id,
    account_number,
    account_type,
    status,
    currency,
    balance,
    created_at,
    version
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    NULL,
    'INTERNAL-CASH-USD',
    'ASSET',
    'OPEN',
    'USD',
    0.00,
    TIMESTAMP '2026-01-01 00:00:00',
    0
);
