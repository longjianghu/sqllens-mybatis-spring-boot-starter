package com.sohocn.sqllens.mybatis;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import com.sohocn.sqllens.mybatis.report.SqlLensReportData;
import com.sohocn.sqllens.mybatis.report.SqlLensReporter;

class SqlLogInterceptorTest {

    @Test
    void intercept_executesOriginalAndLogs() throws Throwable {
        SqlLensLogger logger = mock(SqlLensLogger.class);
        ExplainAnalyzer analyzer = mock(ExplainAnalyzer.class);
        SqlLensReporter reporter = mock(SqlLensReporter.class);
        SqlLogInterceptor interceptor = new SqlLogInterceptor(logger, analyzer, reporter);

        Configuration config = mock(Configuration.class);
        DataSource dataSource = mock(DataSource.class);
        org.apache.ibatis.transaction.TransactionFactory txFactory =
            mock(org.apache.ibatis.transaction.TransactionFactory.class);
        org.apache.ibatis.mapping.Environment env =
            new org.apache.ibatis.mapping.Environment("test", txFactory, dataSource);
        when(config.getEnvironment()).thenReturn(env);

        MappedStatement ms = mock(MappedStatement.class);
        BoundSql boundSql = mock(BoundSql.class);
        when(ms.getBoundSql(any())).thenReturn(boundSql);
        when(ms.getConfiguration()).thenReturn(config);
        when(ms.getId()).thenReturn("com.example.UserMapper.findById");
        when(boundSql.getSql()).thenReturn("SELECT 1");

        Invocation invocation = mock(Invocation.class);
        when(invocation.getArgs()).thenReturn(new Object[]{ms, null});
        when(invocation.proceed()).thenReturn(1);

        Object result = interceptor.intercept(invocation);

        assertEquals(1, result);
        verify(invocation).proceed();
        verify(logger).logExecution(eq(boundSql), eq(config), anyLong());
        verify(reporter).report(any(SqlLensReportData.class));
    }

    @Test
    void intercept_stillLogs_whenExceptionThrown() throws Throwable {
        SqlLensLogger logger = mock(SqlLensLogger.class);
        ExplainAnalyzer analyzer = mock(ExplainAnalyzer.class);
        SqlLensReporter reporter = mock(SqlLensReporter.class);
        SqlLogInterceptor interceptor = new SqlLogInterceptor(logger, analyzer, reporter);

        Configuration config = mock(Configuration.class);
        DataSource dataSource = mock(DataSource.class);
        org.apache.ibatis.transaction.TransactionFactory txFactory =
            mock(org.apache.ibatis.transaction.TransactionFactory.class);
        org.apache.ibatis.mapping.Environment env =
            new org.apache.ibatis.mapping.Environment("test", txFactory, dataSource);
        when(config.getEnvironment()).thenReturn(env);

        MappedStatement ms = mock(MappedStatement.class);
        BoundSql boundSql = mock(BoundSql.class);
        when(ms.getBoundSql(any())).thenReturn(boundSql);
        when(ms.getConfiguration()).thenReturn(config);
        when(ms.getId()).thenReturn("com.example.UserMapper.findById");
        when(boundSql.getSql()).thenReturn("SELECT 1");

        Invocation invocation = mock(Invocation.class);
        when(invocation.getArgs()).thenReturn(new Object[]{ms, null});
        when(invocation.proceed()).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> interceptor.intercept(invocation));
        verify(logger).logExecution(eq(boundSql), eq(config), anyLong());
    }

    @Test
    void intercept_worksWithNullReporter() throws Throwable {
        SqlLensLogger logger = mock(SqlLensLogger.class);
        ExplainAnalyzer analyzer = mock(ExplainAnalyzer.class);
        SqlLogInterceptor interceptor = new SqlLogInterceptor(logger, analyzer, null);

        Configuration config = mock(Configuration.class);
        DataSource dataSource = mock(DataSource.class);
        org.apache.ibatis.transaction.TransactionFactory txFactory =
            mock(org.apache.ibatis.transaction.TransactionFactory.class);
        org.apache.ibatis.mapping.Environment env =
            new org.apache.ibatis.mapping.Environment("test", txFactory, dataSource);
        when(config.getEnvironment()).thenReturn(env);

        MappedStatement ms = mock(MappedStatement.class);
        BoundSql boundSql = mock(BoundSql.class);
        when(ms.getBoundSql(any())).thenReturn(boundSql);
        when(ms.getConfiguration()).thenReturn(config);
        when(boundSql.getSql()).thenReturn("SELECT 1");

        Invocation invocation = mock(Invocation.class);
        when(invocation.getArgs()).thenReturn(new Object[]{ms, null});
        when(invocation.proceed()).thenReturn(1);

        Object result = interceptor.intercept(invocation);
        assertEquals(1, result);
        verify(logger).logExecution(eq(boundSql), eq(config), anyLong());
    }

    @Test
    void plugin_wrapsTarget() {
        SqlLensLogger logger = mock(SqlLensLogger.class);
        ExplainAnalyzer analyzer = mock(ExplainAnalyzer.class);
        SqlLogInterceptor interceptor = new SqlLogInterceptor(logger, analyzer, null);

        Executor target = mock(Executor.class);
        Object result = interceptor.plugin(target);
        assertNotNull(result);
    }

    @Test
    void setProperties_doesNotThrow() {
        SqlLensLogger logger = mock(SqlLensLogger.class);
        ExplainAnalyzer analyzer = mock(ExplainAnalyzer.class);
        SqlLogInterceptor interceptor = new SqlLogInterceptor(logger, analyzer, null);
        assertDoesNotThrow(() -> interceptor.setProperties(new Properties()));
    }
}
