/*
 * Copyright (c) 2019 Tony Ho. Some rights reserved.
 */

package com.chuntung.plugin.mybatis.builder.database;

import com.chuntung.plugin.mybatis.builder.model.ConnectionInfo;
import com.chuntung.plugin.mybatis.builder.model.DriverTypeEnum;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;

public class SimpleDataSourceFactory {
    private static SimpleDataSourceFactory instance = new SimpleDataSourceFactory();

    public static SimpleDataSourceFactory getInstance() {
        return instance;
    }

    public DataSource getDataSource(ConnectionInfo connectionInfo) throws SQLException {
        if (DriverTypeEnum.MySQL.equals(connectionInfo.getDriverType())) {
            MysqlDataSource dataSource = new MysqlDataSource();
            dataSource.setServerName(connectionInfo.getHost());
            dataSource.setPort(connectionInfo.getPort());
            dataSource.setUser(connectionInfo.getUserName());
            dataSource.setPassword(connectionInfo.getPassword());
            dataSource.setDatabaseName(connectionInfo.getDatabase());
            dataSource.setCharacterEncoding("utf-8");
            try {
                // dataSource.setLoginTimeout(5);
                dataSource.setConnectTimeout(5000);
                dataSource.setAllowPublicKeyRetrieval(true);
            } catch (SQLException e) {
                // NOOP
            }
            dataSource.setUseInformationSchema(true);
            dataSource.setUseSSL(false);
            return dataSource;
        } else if (DriverTypeEnum.PostgreSQL.equals(connectionInfo.getDriverType())) {
            PGSimpleDataSource dataSource = new PGSimpleDataSource();
            dataSource.setUser(connectionInfo.getUserName());
            dataSource.setPassword(connectionInfo.getPassword());
            dataSource.setServerName(connectionInfo.getHost());
            dataSource.setPortNumber(connectionInfo.getPort());
            dataSource.setDatabaseName(connectionInfo.getDatabase());
            dataSource.setLoginTimeout(5);
            return dataSource;
        } else if (DriverTypeEnum.MariaDB.equals(connectionInfo.getDriverType())
                || DriverTypeEnum.Oracle.equals(connectionInfo.getDriverType())
                || DriverTypeEnum.DuckDB.equals(connectionInfo.getDriverType())
                || DriverTypeEnum.SqlLite.equals(connectionInfo.getDriverType())) {
            String driverClass = connectionInfo.getDriverClass();
            if (driverClass == null || driverClass.isEmpty()) {
                driverClass = connectionInfo.getDriverType().getDriverClass();
            }
            CustomDataSource dataSource = new CustomDataSource(connectionInfo.getDriverLibrary(), driverClass);
            String url = connectionInfo.getUrl();
            if (url == null || url.isEmpty()) {
                url = connectionInfo.getDriverType().getUrlPattern()
                        .replace("${host}", connectionInfo.getHost() != null ? connectionInfo.getHost() : "")
                        .replace("${port}", String.valueOf(connectionInfo.getPort() != null ? connectionInfo.getPort() : connectionInfo.getDriverType().getDefaultPort()))
                        .replace("${db}", connectionInfo.getDatabase() != null ? connectionInfo.getDatabase() : "");
            }
            dataSource.setUrl(url);
            dataSource.setUser(connectionInfo.getUserName());
            dataSource.setPassword(connectionInfo.getPassword());
            try {
                dataSource.setLoginTimeout(5);
            } catch (SQLException e) {
                // NOOP
            }
            return dataSource;
        } else {
            CustomDataSource dataSource = new CustomDataSource(connectionInfo.getDriverLibrary(), connectionInfo.getDriverClass());
            dataSource.setUrl(connectionInfo.getUrl());
            dataSource.setUser(connectionInfo.getUserName());
            dataSource.setPassword(connectionInfo.getPassword());
            try {
                dataSource.setLoginTimeout(5);
            } catch (SQLException e) {
                // NOOP
            }
            return dataSource;
        }

    }
}
