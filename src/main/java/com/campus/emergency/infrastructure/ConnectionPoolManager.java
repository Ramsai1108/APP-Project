package com.campus.emergency.infrastructure;

import com.campus.emergency.config.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

public class ConnectionPoolManager {
    private static final Logger LOGGER = Logger.getLogger(ConnectionPoolManager.class.getName());
    private static ConnectionPoolManager instance;
    private HikariDataSource dataSource;

    private ConnectionPoolManager(DatabaseConfig config) {
        initializeDataSource(config);
    }

    private synchronized void initializeDataSource(DatabaseConfig config) {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                return;
            }
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(config.getJdbcUrl());
            hikariConfig.setUsername(config.getUsername());
            hikariConfig.setPassword(config.getPassword());
            hikariConfig.setDriverClassName(config.getDriverClassName());
            hikariConfig.setMaximumPoolSize(config.getMaximumPoolSize());
            hikariConfig.setMinimumIdle(config.getMinimumIdle());
            hikariConfig.setIdleTimeout(config.getIdleTimeout());
            hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
            hikariConfig.setMaxLifetime(config.getMaxLifetime());

            // Prepared statement caching performance settings
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");

            this.dataSource = new HikariDataSource(hikariConfig);
            LOGGER.info("HikariCP Connection Pool initialized for database: " + config.getDatabaseName());
        } catch (Exception e) {
            LOGGER.warning("Could not initialize HikariDataSource connection pool: " + e.getMessage());
        }
    }

    public static synchronized ConnectionPoolManager getInstance(DatabaseConfig config) {
        if (instance == null) {
            instance = new ConnectionPoolManager(config);
        } else if (instance.dataSource == null || instance.dataSource.isClosed()) {
            instance.initializeDataSource(config);
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }

    public synchronized Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            initializeDataSource(new DatabaseConfig());
        }
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("HikariDataSource is not initialized or closed.");
        }
        return dataSource.getConnection();
    }

    public synchronized void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("HikariCP Connection Pool shut down.");
        }
    }
}
