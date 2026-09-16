package com.campus.emergency.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

/**
 * Database configuration and HikariCP connection pool manager stub.
 */
public class DatabaseConfig {

    private static HikariDataSource dataSource;

    private DatabaseConfig() {}

    public static synchronized DataSource getDataSource() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://localhost:3306/smart_campus_emergency?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
            config.setUsername("root");
            config.setPassword("root");
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(30000);
            config.setConnectionTimeout(10000);
            config.setPoolName("SmartCampusHikariCP");

            dataSource = new HikariDataSource(config);
        }
        return dataSource;
    }

    public static synchronized void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
