-- =============================================================================
-- SafePay Database Schema Migration — Phase C Banking Integrity
-- Non-destructive DDL migration for existing MySQL installations.
-- Safe to run on existing production / staging databases without data loss.
-- DO NOT execute automatically during builds. Run manually when deploying.
-- =============================================================================

USE fraud_detection;

SET @dbname = DATABASE();

-- -----------------------------------------------------------------------------
-- 1. Table: transactions -> Column: receiver_account_id
-- -----------------------------------------------------------------------------
SET @tablename = "transactions";
SET @columnname = "receiver_account_id";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      table_schema = @dbname
      AND table_name = @tablename
      AND column_name = @columnname
  ) > 0,
  "SELECT 'Column receiver_account_id already exists in transactions';",
  "ALTER TABLE transactions ADD COLUMN receiver_account_id BIGINT NULL;"
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 1b. Table: transactions -> Foreign Key: fk_transactions_receiver_account
-- -----------------------------------------------------------------------------
SET @fkname = "fk_transactions_receiver_account";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE
      table_schema = @dbname
      AND table_name = @tablename
      AND constraint_name = @fkname
      AND constraint_type = "FOREIGN KEY"
  ) > 0,
  "SELECT 'Constraint fk_transactions_receiver_account already exists in transactions';",
  "ALTER TABLE transactions ADD CONSTRAINT fk_transactions_receiver_account FOREIGN KEY (receiver_account_id) REFERENCES accounts(id);"
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 2. Table: fraud_alerts -> Column: reviewing_admin_id
-- -----------------------------------------------------------------------------
SET @tablename = "fraud_alerts";
SET @columnname = "reviewing_admin_id";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      table_schema = @dbname
      AND table_name = @tablename
      AND column_name = @columnname
  ) > 0,
  "SELECT 'Column reviewing_admin_id already exists in fraud_alerts';",
  "ALTER TABLE fraud_alerts ADD COLUMN reviewing_admin_id BIGINT NULL;"
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 2b. Table: fraud_alerts -> Foreign Key: fk_fraud_alerts_admin
-- -----------------------------------------------------------------------------
SET @fkname = "fk_fraud_alerts_admin";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE
      table_schema = @dbname
      AND table_name = @tablename
      AND constraint_name = @fkname
      AND constraint_type = "FOREIGN KEY"
  ) > 0,
  "SELECT 'Constraint fk_fraud_alerts_admin already exists in fraud_alerts';",
  "ALTER TABLE fraud_alerts ADD CONSTRAINT fk_fraud_alerts_admin FOREIGN KEY (reviewing_admin_id) REFERENCES users(id);"
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 3. Table: fraud_alerts -> Column: reviewed_at
-- -----------------------------------------------------------------------------
SET @columnname = "reviewed_at";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      table_schema = @dbname
      AND table_name = @tablename
      AND column_name = @columnname
  ) > 0,
  "SELECT 'Column reviewed_at already exists in fraud_alerts';",
  "ALTER TABLE fraud_alerts ADD COLUMN reviewed_at DATETIME(6) NULL;"
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 4. Table: fraud_alerts -> Column: decision
-- -----------------------------------------------------------------------------
SET @columnname = "decision";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      table_schema = @dbname
      AND table_name = @tablename
      AND column_name = @columnname
  ) > 0,
  "SELECT 'Column decision already exists in fraud_alerts';",
  "ALTER TABLE fraud_alerts ADD COLUMN decision VARCHAR(255) NULL;"
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 5. Table: fraud_alerts -> Column: review_notes
-- -----------------------------------------------------------------------------
SET @columnname = "review_notes";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      table_schema = @dbname
      AND table_name = @tablename
      AND column_name = @columnname
  ) > 0,
  "SELECT 'Column review_notes already exists in fraud_alerts';",
  "ALTER TABLE fraud_alerts ADD COLUMN review_notes VARCHAR(255) NULL;"
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 6. Table: transactions -> Column: idempotency_key
-- -----------------------------------------------------------------------------
SET @tablename = "transactions";
SET @columnname = "idempotency_key";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      table_schema = @dbname
      AND table_name = @tablename
      AND column_name = @columnname
  ) > 0,
  "SELECT 'Column idempotency_key already exists in transactions';",
  "ALTER TABLE transactions ADD COLUMN idempotency_key VARCHAR(128) NULL;"
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 6b. Table: transactions -> Unique Index: uk_transactions_idempotency_key
-- -----------------------------------------------------------------------------
SET @idxname = "uk_transactions_idempotency_key";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      table_schema = @dbname
      AND table_name = @tablename
      AND index_name = @idxname
  ) > 0,
  "SELECT 'Unique index uk_transactions_idempotency_key already exists';",
  "ALTER TABLE transactions ADD CONSTRAINT uk_transactions_idempotency_key UNIQUE (idempotency_key);"
));
PREPARE stmt FROM @preparedStatement;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 7. Table: funds_reservations (Phase D: Pending-Review Funds Reservation)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS funds_reservations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    transaction_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    notes VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_funds_reservations_transaction UNIQUE (transaction_id),
    CONSTRAINT fk_funds_reservations_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT fk_funds_reservations_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    INDEX idx_reservations_account_status (account_id, status),
    INDEX idx_reservations_transaction (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
