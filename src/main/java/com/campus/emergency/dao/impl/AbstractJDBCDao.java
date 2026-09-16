package com.campus.emergency.dao.impl;

import com.campus.emergency.config.DatabaseConfig;
import com.campus.emergency.dao.IGenericDao;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 * Abstract Base JDBC DAO managing connection acquisition.
 */
public abstract class AbstractJDBCDao<T, ID> implements IGenericDao<T, ID> {

    protected DataSource getDataSource() {
        return DatabaseConfig.getDataSource();
    }

    protected Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }
}
