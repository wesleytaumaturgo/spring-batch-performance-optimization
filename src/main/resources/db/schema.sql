-- Schema principal — gerenciado pelo Hibernate em dev (ddl-auto=update)
-- Em produção, aplicar via migration tool (Flyway/Liquibase)

CREATE TABLE IF NOT EXISTS transactions (
    id             UUID         NOT NULL PRIMARY KEY,
    source_account VARCHAR(20)  NOT NULL,
    target_account VARCHAR(20)  NOT NULL,
    amount         NUMERIC(19, 2) NOT NULL,
    currency       VARCHAR(3)   NOT NULL DEFAULT 'BRL',
    type           VARCHAR(10)  NOT NULL,
    status         VARCHAR(10)  NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMP    NOT NULL,
    processed_at   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions (status);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions (created_at);
