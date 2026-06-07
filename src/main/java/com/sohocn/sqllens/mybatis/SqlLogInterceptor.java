package com.sohocn.sqllens.mybatis;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sohocn.sqllens.mybatis.report.SqlLensReportData;
import com.sohocn.sqllens.mybatis.report.SqlLensReporter;

/**
 * The type Sql log interceptor.
 *
 * @author longjianghu
 */
@Intercepts({
    @Signature(type = Executor.class, method = "update",
               args = {MappedStatement.class, Object.class}),
    @Signature(type = Executor.class, method = "query",
               args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "query",
               args = {MappedStatement.class, Object.class, RowBounds.class,
                       ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class SqlLogInterceptor implements Interceptor {
    private static final Logger log = LoggerFactory.getLogger(SqlLogInterceptor.class);

    private final ExplainAnalyzer analyzer;
    private final SqlLensLogger logger;
    private final SqlLensReporter reporter;
    private volatile String dbType;
    private final Object dbTypeLock = new Object();

    /**
     * Instantiates a new Sql log interceptor.
     *
     * @param logger
     *            the logger
     * @param analyzer
     *            the analyzer
     * @param reporter
     *            the reporter
     */
    public SqlLogInterceptor(SqlLensLogger logger, ExplainAnalyzer analyzer, SqlLensReporter reporter) {
        this.logger = logger;
        this.analyzer = analyzer;
        this.reporter = reporter;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs().length > 1 ? invocation.getArgs()[1] : null;
        BoundSql boundSql = ms.getBoundSql(parameter);
        Configuration config = ms.getConfiguration();

        long start = System.nanoTime();
        try {
            return invocation.proceed();
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            try {
                logger.logExecution(boundSql, config, durationMs);
            } catch (Exception e) {
                log.warn("[SqlLens] Logging failed: {}", e.getMessage());
            }
            String explainResult = null;
            try {
                explainResult = analyzer.analyze(boundSql, config, config.getEnvironment().getDataSource());
            } catch (Exception e) {
                log.warn("[SqlLens] EXPLAIN analysis failed: {}", e.getMessage());
            }
            try {
                if (reporter != null) {
                    String formattedSql = logger.formatSql(boundSql, config);
                    String mapperMethod = ms.getId();
                    String mapperId = ms.getId();
                    DataSource dataSource = config.getEnvironment().getDataSource();
                    String detectedDbType = getDbType(dataSource);
                    SqlLensReportData data = new SqlLensReportData(
                            boundSql.getSql().trim(), formattedSql, durationMs,
                            explainResult, mapperMethod, mapperId, detectedDbType
                    );
                    reporter.report(data);
                }
            } catch (Exception e) {
                log.warn("[SqlLens] Report failed: {}", e.getMessage());
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }

    /**
     * Gets db type from DataSource, cached after first detection.
     *
     * @param dataSource
     *            the data source
     * @return the db type
     */
    private String getDbType(DataSource dataSource) {
        if (dbType != null) {
            return dbType;
        }
        synchronized (dbTypeLock) {
            if (dbType != null) {
                return dbType;
            }
            if (dataSource == null) {
                dbType = "unknown";
                return dbType;
            }
            try {
                Connection conn = dataSource.getConnection();
                if (conn == null) {
                    log.warn("[SqlLens] DataSource returned null connection, cannot detect db type");
                    dbType = "unknown";
                    return dbType;
                }
                try {
                    String productName = conn.getMetaData().getDatabaseProductName();
                    dbType = productName != null ? productName.toLowerCase() : "unknown";
                } finally {
                    conn.close();
                }
            } catch (SQLException e) {
                log.warn("[SqlLens] Failed to detect db type: {}", e.getMessage());
                dbType = "unknown";
            }
        }
        return dbType;
    }
}
