package com.frauddetection.frauddetection.jdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Concrete Data Access Object implementing {@link DatabaseOperations} using raw JDBC.
 * Performs direct SQL operations on the {@code transactions} table.
 *
 * Demonstrates Academic Rubric:
 * - OOP Implementation: Interface implementation & Polymorphism
 * - Collections & Generics: Generic interface contract, typed collections
 * - Classes for Database Operations: DAO pattern with PreparedStatement, ResultSet
 * - Database Connectivity (JDBC): Transaction commit/rollback, batch updates, resource management
 */
public class TransactionJdbcDao implements DatabaseOperations<TransactionRecord> {

    private static final Logger log = LoggerFactory.getLogger(TransactionJdbcDao.class);

    private static final String SELECT_BY_ID_SQL =
            "SELECT id, amount, transaction_type, transaction_time, status, account_id, ip_address, user_agent " +
            "FROM transactions WHERE id = ?";

    private static final String SELECT_ALL_SQL =
            "SELECT id, amount, transaction_type, transaction_time, status, account_id, ip_address, user_agent " +
            "FROM transactions ORDER BY transaction_time DESC";

    private static final String SELECT_RECENT_SQL =
            "SELECT id, amount, transaction_type, transaction_time, status, account_id, ip_address, user_agent " +
            "FROM transactions ORDER BY transaction_time DESC LIMIT ?";

    private static final String INSERT_SQL =
            "INSERT INTO transactions (amount, transaction_type, transaction_time, status, account_id, ip_address, user_agent) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE transactions SET amount = ?, transaction_type = ?, transaction_time = ?, status = ?, " +
            "account_id = ?, ip_address = ?, user_agent = ? WHERE id = ?";

    private static final String DELETE_SQL =
            "DELETE FROM transactions WHERE id = ?";

    private static final String COUNT_SQL =
            "SELECT COUNT(*) FROM transactions";

    private static final String SELECT_BY_STATUS_SQL =
            "SELECT id, amount, transaction_type, transaction_time, status, account_id, ip_address, user_agent " +
            "FROM transactions WHERE status = ? ORDER BY transaction_time DESC";

    private static final String ANALYTICS_SQL =
            "SELECT " +
            "  COUNT(*) AS total_count, " +
            "  SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) AS approved_count, " +
            "  SUM(CASE WHEN status = 'DECLINED' THEN 1 ELSE 0 END) AS declined_count, " +
            "  SUM(CASE WHEN status = 'BLOCKED' THEN 1 ELSE 0 END) AS blocked_count, " +
            "  COALESCE(SUM(amount), 0) AS total_volume, " +
            "  COALESCE(SUM(CASE WHEN status = 'BLOCKED' THEN amount ELSE 0 END), 0) AS fraud_volume " +
            "FROM transactions";

    @Override
    public Optional<TransactionRecord> findById(Long id) throws DatabaseOperationException {
        if (id == null) {
            return Optional.empty();
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.prepareStatement(SELECT_BY_ID_SQL);
            stmt.setLong(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToRecord(rs));
            }
            return Optional.empty();
        } catch (SQLException ex) {
            log.error("Error executing JDBC findById for id={}: {}", id, ex.getMessage());
            throw new DatabaseOperationException("JDBC findById query failed", ex.getSQLState(), ex.getErrorCode(), ex);
        } finally {
            DatabaseConnection.closeQuietly(rs, stmt, conn);
        }
    }

    @Override
    public List<TransactionRecord> findAll() throws DatabaseOperationException {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        List<TransactionRecord> results = new ArrayList<>();

        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(SELECT_ALL_SQL);

            while (rs.next()) {
                results.add(mapResultSetToRecord(rs));
            }
            return results;
        } catch (SQLException ex) {
            log.error("Error executing JDBC findAll: {}", ex.getMessage());
            throw new DatabaseOperationException("JDBC findAll query failed", ex.getSQLState(), ex.getErrorCode(), ex);
        } finally {
            DatabaseConnection.closeQuietly(rs, stmt, conn);
        }
    }

    public List<TransactionRecord> findRecentTransactions(int limit) throws DatabaseOperationException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<TransactionRecord> results = new ArrayList<>();

        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.prepareStatement(SELECT_RECENT_SQL);
            stmt.setInt(1, Math.max(1, limit));
            rs = stmt.executeQuery();

            while (rs.next()) {
                results.add(mapResultSetToRecord(rs));
            }
            return results;
        } catch (SQLException ex) {
            log.error("Error executing JDBC findRecentTransactions: {}", ex.getMessage());
            throw new DatabaseOperationException("JDBC findRecentTransactions query failed", ex.getSQLState(), ex.getErrorCode(), ex);
        } finally {
            DatabaseConnection.closeQuietly(rs, stmt, conn);
        }
    }

    public List<TransactionRecord> findByStatus(String status) throws DatabaseOperationException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<TransactionRecord> results = new ArrayList<>();

        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.prepareStatement(SELECT_BY_STATUS_SQL);
            stmt.setString(1, status);
            rs = stmt.executeQuery();

            while (rs.next()) {
                results.add(mapResultSetToRecord(rs));
            }
            return results;
        } catch (SQLException ex) {
            log.error("Error executing JDBC findByStatus: {}", ex.getMessage());
            throw new DatabaseOperationException("JDBC findByStatus query failed", ex.getSQLState(), ex.getErrorCode(), ex);
        } finally {
            DatabaseConnection.closeQuietly(rs, stmt, conn);
        }
    }

    @Override
    public TransactionRecord save(TransactionRecord entity) throws DatabaseOperationException {
        if (entity == null) {
            throw new IllegalArgumentException("Transaction entity cannot be null");
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet generatedKeys = null;

        try {
            conn = DatabaseConnection.getConnection();

            if (entity.getId() == null) {
                // INSERT
                stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS);
                stmt.setBigDecimal(1, entity.getAmount());
                stmt.setString(2, entity.getTransactionType());
                stmt.setTimestamp(3, entity.getTransactionTime() != null ?
                        Timestamp.valueOf(entity.getTransactionTime()) : Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(4, entity.getStatus());
                if (entity.getAccountId() != null) {
                    stmt.setLong(5, entity.getAccountId());
                } else {
                    stmt.setNull(5, java.sql.Types.BIGINT);
                }
                stmt.setString(6, entity.getIpAddress());
                stmt.setString(7, entity.getUserAgent());

                int affected = stmt.executeUpdate();
                if (affected > 0) {
                    generatedKeys = stmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        entity.setId(generatedKeys.getLong(1));
                    }
                }
            } else {
                // UPDATE
                stmt = conn.prepareStatement(UPDATE_SQL);
                stmt.setBigDecimal(1, entity.getAmount());
                stmt.setString(2, entity.getTransactionType());
                stmt.setTimestamp(3, entity.getTransactionTime() != null ?
                        Timestamp.valueOf(entity.getTransactionTime()) : Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(4, entity.getStatus());
                if (entity.getAccountId() != null) {
                    stmt.setLong(5, entity.getAccountId());
                } else {
                    stmt.setNull(5, java.sql.Types.BIGINT);
                }
                stmt.setString(6, entity.getIpAddress());
                stmt.setString(7, entity.getUserAgent());
                stmt.setLong(8, entity.getId());

                stmt.executeUpdate();
            }

            return entity;
        } catch (SQLException ex) {
            log.error("Error executing JDBC save: {}", ex.getMessage());
            throw new DatabaseOperationException("JDBC save operation failed", ex.getSQLState(), ex.getErrorCode(), ex);
        } finally {
            DatabaseConnection.closeQuietly(generatedKeys, stmt, conn);
        }
    }

    @Override
    public boolean delete(Long id) throws DatabaseOperationException {
        if (id == null) {
            return false;
        }

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.prepareStatement(DELETE_SQL);
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            log.error("Error executing JDBC delete for id={}: {}", id, ex.getMessage());
            throw new DatabaseOperationException("JDBC delete query failed", ex.getSQLState(), ex.getErrorCode(), ex);
        } finally {
            DatabaseConnection.closeQuietly(stmt, conn);
        }
    }

    @Override
    public long count() throws DatabaseOperationException {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(COUNT_SQL);
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException ex) {
            log.error("Error executing JDBC count: {}", ex.getMessage());
            throw new DatabaseOperationException("JDBC count query failed", ex.getSQLState(), ex.getErrorCode(), ex);
        } finally {
            DatabaseConnection.closeQuietly(rs, stmt, conn);
        }
    }

    /**
     * Demonstrates JDBC Batch Execution and Transaction Management (commit/rollback).
     */
    public int[] executeBatchInsert(List<TransactionRecord> records) throws DatabaseOperationException {
        if (records == null || records.isEmpty()) {
            return new int[0];
        }

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseConnection.getConnection();
            // Transaction demarcation: disable auto-commit
            conn.setAutoCommit(false);

            stmt = conn.prepareStatement(INSERT_SQL);
            for (TransactionRecord record : records) {
                stmt.setBigDecimal(1, record.getAmount());
                stmt.setString(2, record.getTransactionType());
                stmt.setTimestamp(3, record.getTransactionTime() != null ?
                        Timestamp.valueOf(record.getTransactionTime()) : Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(4, record.getStatus());
                if (record.getAccountId() != null) {
                    stmt.setLong(5, record.getAccountId());
                } else {
                    stmt.setNull(5, java.sql.Types.BIGINT);
                }
                stmt.setString(6, record.getIpAddress());
                stmt.setString(7, record.getUserAgent());
                stmt.addBatch();
            }

            int[] batchResults = stmt.executeBatch();
            // Commit transaction
            conn.commit();
            return batchResults;
        } catch (SQLException ex) {
            if (conn != null) {
                try {
                    conn.rollback();
                    log.info("Rolled back batch transaction due to error: {}", ex.getMessage());
                } catch (SQLException rbEx) {
                    log.warn("Rollback failed: {}", rbEx.getMessage());
                }
            }
            throw new DatabaseOperationException("Batch insert failed and transaction rolled back",
                    ex.getSQLState(), ex.getErrorCode(), ex);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
            DatabaseConnection.closeQuietly(stmt, conn);
        }
    }

    /**
     * Computes real-time fraud metrics via analytical SQL aggregate queries.
     */
    public FraudAnalyticsSummary getSummaryAnalytics() throws DatabaseOperationException {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(ANALYTICS_SQL);

            if (rs.next()) {
                long total = rs.getLong("total_count");
                long approved = rs.getLong("approved_count");
                long declined = rs.getLong("declined_count");
                long blocked = rs.getLong("blocked_count");
                BigDecimal totalVolume = rs.getBigDecimal("total_volume");
                BigDecimal fraudVolume = rs.getBigDecimal("fraud_volume");

                double fraudRate = total > 0 ? ((double) blocked / total) * 100.0 : 0.0;

                return new FraudAnalyticsSummary(total, approved, declined, blocked, totalVolume, fraudVolume, fraudRate);
            }

            return new FraudAnalyticsSummary();
        } catch (SQLException ex) {
            log.error("Error calculating summary analytics via JDBC: {}", ex.getMessage());
            throw new DatabaseOperationException("JDBC analytics query failed", ex.getSQLState(), ex.getErrorCode(), ex);
        } finally {
            DatabaseConnection.closeQuietly(rs, stmt, conn);
        }
    }

    private TransactionRecord mapResultSetToRecord(ResultSet rs) throws SQLException {
        TransactionRecord record = new TransactionRecord();
        record.setId(rs.getLong("id"));
        record.setAmount(rs.getBigDecimal("amount"));
        record.setTransactionType(rs.getString("transaction_type"));

        Timestamp ts = rs.getTimestamp("transaction_time");
        if (ts != null) {
            record.setTransactionTime(ts.toLocalDateTime());
        }

        record.setStatus(rs.getString("status"));
        long accountId = rs.getLong("account_id");
        if (!rs.wasNull()) {
            record.setAccountId(accountId);
        }
        record.setIpAddress(rs.getString("ip_address"));
        record.setUserAgent(rs.getString("user_agent"));

        return record;
    }
}
