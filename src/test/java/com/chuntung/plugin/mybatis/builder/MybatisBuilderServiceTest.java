/*
 * Copyright (c) 2019 Tony Ho. Some rights reserved.
 */

package com.chuntung.plugin.mybatis.builder;

import com.chuntung.plugin.mybatis.builder.model.ColumnInfo;
import com.chuntung.plugin.mybatis.builder.model.TableInfo;
import com.chuntung.plugin.mybatis.builder.model.ConnectionInfo;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Assert;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class MybatisBuilderServiceTest extends BasePlatformTestCase {
    private MybatisBuilderService service;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        service = new MybatisBuilderService(getProject());
        service.saveConnectionInfo(Arrays.asList(TestConnectionFactory.getTestConnectionInfo()));
    }

    public void testConnection() {
        try {
            service.testConnection(TestConnectionFactory.getTestConnectionInfo());
        } catch (SQLException e) {
            Assert.fail(e.getMessage());
        }
    }

    public void testFetchDatabases() throws SQLException {
        List<?> databases = service.fetchDatabases(TestConnectionFactory.getTestConnectionInfo().getId());
        Assert.assertTrue(databases.size() > 0);
    }

    public void testFetchTables() throws SQLException {
        List<?> tables = service.fetchTables(TestConnectionFactory.getTestConnectionInfo().getId(), "TEST");
        Assert.assertTrue(tables.size() > 0);
    }

    public void testFetchColumns() throws SQLException {
        List<ColumnInfo> columns = service.fetchColumns(TestConnectionFactory.getTestConnectionInfo(), new TableInfo("TEST", "USER", null));
        Assert.assertTrue(columns.size() > 0);
    }
}
