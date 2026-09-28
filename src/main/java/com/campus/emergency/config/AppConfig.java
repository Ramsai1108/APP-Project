package com.campus.emergency.config;

public class AppConfig {
    private static final AppConfig INSTANCE = new AppConfig();

    private final String appName = "Campus Emergency Response System";
    private final String appVersion = "1.0.0";
    private final DatabaseConfig databaseConfig;

    private AppConfig() {
        this.databaseConfig = new DatabaseConfig();
    }

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    public String getAppName() { return appName; }
    public String getAppVersion() { return appVersion; }
    public DatabaseConfig getDatabaseConfig() { return databaseConfig; }
}
