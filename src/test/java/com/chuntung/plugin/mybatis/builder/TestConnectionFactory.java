package com.chuntung.plugin.mybatis.builder;

import com.chuntung.plugin.mybatis.builder.database.SimpleDataSourceFactory;
import com.chuntung.plugin.mybatis.builder.model.ConnectionInfo;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.junit.Assert.fail;

public class TestConnectionFactory {
    private static ConnectionInfo info;

    public static ConnectionInfo getTestConnectionInfo() {
        if (info != null) {
            return info;
        }

        // connection info for test
        ConnectionInfo newInfo = new ConnectionInfo();
        newInfo.setId("junit-test-connection");
        newInfo.setDriverClass("org.h2.Driver");
        newInfo.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        newInfo.setDatabase("TEST");
        newInfo.setUserName("");
        newInfo.setPassword("");

        try {
            DataSource dataSource = SimpleDataSourceFactory.getInstance().getDataSource(newInfo);
            dataSource.getConnection().createStatement().execute("create table user(id int PRIMARY KEY, name varchar(30), sex varchar(1));");
        } catch (SQLException e) {
            fail(e.getMessage());
        }

        info = newInfo;
        return info;
    }
}
