package com.campus.emergency.config;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class DatabaseConfig {
    private String host;
    private int port;
    private String databaseName;
    private String username;
    private String password;
    private String jdbcUrl;
    private String driverClassName;
    private int maximumPoolSize;
    private int minimumIdle;
    private long idleTimeout;
    private long connectionTimeout;
    private long maxLifetime;

    public DatabaseConfig() {
        this.host = resolveConfig("localhost", "DB_HOST", "db_host", "db.host");
        String portStr = resolveConfig("3306", "DB_PORT", "db_port", "db.port");
        try {
            this.port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            this.port = 3306;
        }
        this.databaseName = resolveConfig("campus_emergency_db", "DB_NAME", "db_name", "db.name", "DB_DATABASE");
        this.username = resolveConfig("root", "DB_USERNAME", "db_username", "db.username", "DB_USER", "db_user", "db.user");
        this.password = resolveConfig("", "DB_PASSWORD", "db_password", "db.password", "DB_PASS", "db_pass", "db.pass", "MYSQL_PWD");

        String customUrl = resolveConfig(null, "DB_URL", "db_url", "db.url", "JDBC_URL");
        if (customUrl != null && !customUrl.trim().isEmpty()) {
            this.jdbcUrl = customUrl;
        } else {
            this.jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                    host, port, databaseName);
        }

        this.driverClassName = "com.mysql.cj.jdbc.Driver";
        this.maximumPoolSize = Integer.parseInt(resolveConfig("10", "DB_POOL_MAX", "db_pool_max", "db.pool.max"));
        this.minimumIdle = Integer.parseInt(resolveConfig("2", "DB_POOL_IDLE", "db_pool_idle", "db.pool.idle"));
        this.idleTimeout = 600000L; // 10 minutes
        this.connectionTimeout = 30000L; // 30 seconds
        this.maxLifetime = 1800000L; // 30 minutes
    }

    public DatabaseConfig(String host, int port, String databaseName, String username, String password) {
        this();
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.username = username;
        this.password = password != null ? password : "";
        this.jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                host, port, databaseName);
    }

    private static String resolveConfig(String defaultValue, String... keys) {
        // 1. Process Environment Variables (Highest priority)
        for (String key : keys) {
            String val = System.getenv(key);
            if (val != null && !val.trim().isEmpty()) {
                return cleanValue(val);
            }
        }

        // 2. JVM System Properties (-Dkey=value)
        for (String key : keys) {
            String val = System.getProperty(key);
            if (val != null && !val.trim().isEmpty()) {
                return cleanValue(val);
            }
        }

        // 3. Windows Registry User Environment (HKCU\Environment) & System Environment
        if (isWindows()) {
            for (String key : keys) {
                String val = readWindowsRegistry("HKCU\\Environment", key);
                if (val != null && !val.trim().isEmpty()) {
                    return cleanValue(val);
                }
            }
            for (String key : keys) {
                String val = readWindowsRegistry("HKLM\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment", key);
                if (val != null && !val.trim().isEmpty()) {
                    return cleanValue(val);
                }
            }
        }

        return defaultValue;
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("win");
    }

    private static String readWindowsRegistry(String location, String key) {
        try {
            Process process = new ProcessBuilder("reg", "query", location, "/v", key).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(key) && (line.contains("REG_SZ") || line.contains("REG_EXPAND_SZ"))) {
                        String[] parts = line.trim().split("\\s+REG_[A-Z_]+\\s+", 2);
                        if (parts.length > 1) {
                            return parts[1].trim();
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String cleanValue(String val) {
        if (val == null) return null;
        String trimmed = val.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
                (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getDatabaseName() { return databaseName; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getJdbcUrl() { return jdbcUrl; }
    public String getDriverClassName() { return driverClassName; }
    public int getMaximumPoolSize() { return maximumPoolSize; }
    public int getMinimumIdle() { return minimumIdle; }
    public long getIdleTimeout() { return idleTimeout; }
    public long getConnectionTimeout() { return connectionTimeout; }
    public long getMaxLifetime() { return maxLifetime; }
}
