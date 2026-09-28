package com.campus.emergency.infrastructure;

import com.campus.emergency.config.DatabaseConfig;
import com.campus.emergency.exception.DatabaseException;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {
    private final ConnectionPoolManager poolManager;

    @FunctionalInterface
    public interface TransactionCallback<T> {
        T doInTransaction(Connection connection) throws SQLException;
    }

    public DatabaseManager(DatabaseConfig config) {
        this.poolManager = ConnectionPoolManager.getInstance(config);
    }

    public DatabaseManager(ConnectionPoolManager poolManager) {
        this.poolManager = poolManager;
    }

    public Connection getConnection() throws SQLException {
        return poolManager.getConnection();
    }

    public <T> T executeTransaction(TransactionCallback<T> callback) {
        Connection conn = null;
        boolean originalAutoCommit = true;
        try {
            conn = poolManager.getConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            T result = callback.doInTransaction(conn);

            conn.commit();
            return result;
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
            }
            if (e instanceof DatabaseException) {
                throw (DatabaseException) e;
            }
            throw new DatabaseException("Transaction failed and was rolled back: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                    conn.close();
                } catch (SQLException closeEx) {
                    // Connection close exception ignored in finally
                }
            }
        }
    }

    public void shutdown() {
        poolManager.shutdown();
    }
}
