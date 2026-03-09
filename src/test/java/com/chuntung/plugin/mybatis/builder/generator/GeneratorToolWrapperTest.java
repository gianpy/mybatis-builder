/*
 * Copyright (c) 2019 Tony Ho. Some rights reserved.
 */

package com.chuntung.plugin.mybatis.builder.generator;

import com.chuntung.plugin.mybatis.builder.TestConnectionFactory;
import com.chuntung.plugin.mybatis.builder.database.ConnectionUrlBuilder;
import com.chuntung.plugin.mybatis.builder.model.ConnectionInfo;
import com.chuntung.plugin.mybatis.builder.model.TableInfo;
import org.junit.Test;
import org.mybatis.generator.config.JDBCConnectionConfiguration;
import org.mybatis.generator.config.JavaClientGeneratorConfiguration;
import org.mybatis.generator.config.JavaModelGeneratorConfiguration;
import org.mybatis.generator.config.SqlMapGeneratorConfiguration;

import java.util.Arrays;
import java.util.Properties;

import static org.junit.Assert.fail;

public class GeneratorToolWrapperTest {

    @Test
    public void generate() {
        GeneratorParamWrapper param = new GeneratorParamWrapper();
        param.setDefaultParameters(new DefaultParameters());

        ConnectionInfo connectionInfo = TestConnectionFactory.getTestConnectionInfo();
        
        JDBCConnectionConfiguration jdbcConfig = new JDBCConnectionConfiguration();
        String connectionUrl = new ConnectionUrlBuilder(connectionInfo).getConnectionUrl();
        jdbcConfig.setConnectionURL(connectionUrl);
        jdbcConfig.setDriverClass(connectionInfo.getDriverClass());
        jdbcConfig.setUserId(connectionInfo.getUserName());
        jdbcConfig.setPassword(connectionInfo.getPassword());
        param.setJdbcConfig(jdbcConfig);

        JavaClientGeneratorConfiguration javaClientConfig = new JavaClientGeneratorConfiguration();
        javaClientConfig.setConfigurationType("XMLMAPPER");
        javaClientConfig.setTargetProject("./src/test/java");
        javaClientConfig.setTargetPackage("mybatis.builder.example.mapper");
        param.setJavaClientConfig(javaClientConfig);

        JavaModelGeneratorConfiguration javaModelConfig = new JavaModelGeneratorConfiguration();
        javaModelConfig.setTargetProject("./src/test/java");
        javaModelConfig.setTargetPackage("mybatis.builder.example.model");
        param.setJavaModelConfig(javaModelConfig);

        SqlMapGeneratorConfiguration sqlMapConfig = new SqlMapGeneratorConfiguration();
        sqlMapConfig.setTargetProject("./src/test/resources");
        sqlMapConfig.setTargetPackage("sqlmap");
        param.setSqlMapConfig(sqlMapConfig);

        TableInfo tableInfo = new TableInfo();
        tableInfo.setTableName("user");
        tableInfo.setDomainName("gene.User");
        param.setSelectedTables(Arrays.asList(tableInfo));

        GeneratorToolWrapper tool = new GeneratorToolWrapper(param, null);
        try {
            tool.generate();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void runWithConfigurationFile() {
        try {
            // init mem db
            ConnectionInfo connectionInfo = TestConnectionFactory.getTestConnectionInfo();

            Properties properties = new Properties();
            properties.setProperty("PROJECT_DIR", ".");
            properties.setProperty("CURRENT_DIR", "./src/test/resources");
            // GeneratorToolWrapper.runWithConfigurationFile("./src/test/resources/generator-config.xml", properties, null);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
