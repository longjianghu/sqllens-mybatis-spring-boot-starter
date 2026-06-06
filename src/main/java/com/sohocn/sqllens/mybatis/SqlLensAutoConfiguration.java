package com.sohocn.sqllens.mybatis;

import org.apache.ibatis.executor.Executor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sohocn.sqllens.mybatis.report.SqlLensReportConfig;
import com.sohocn.sqllens.mybatis.report.SqlLensReporter;

/**
 * The type Sql lens auto configuration.
 *
 * @author longjianghu
 */
@Configuration
@ConditionalOnClass(Executor.class)
@EnableConfigurationProperties(SqlLensProperties.class)
@ConditionalOnProperty(prefix = "sqllens", name = "enabled", havingValue = "true")
public class SqlLensAutoConfiguration {
    /**
     * Explain analyzer.
     *
     * @param props
     *            the props
     * @return the explain analyzer
     */
    @Bean
    @ConditionalOnMissingBean
    public ExplainAnalyzer explainAnalyzer(SqlLensProperties props) {
        return new ExplainAnalyzer(
                props.isExplainEnabled(),
                props.isExplainAnalyze(),
                props.getExcludeTables()
        );
    }

    /**
     * Sql lens logger sql lens logger.
     *
     * @param props
     *            the props
     * @return the sql lens logger
     */
    @Bean
    @ConditionalOnMissingBean
    public SqlLensLogger sqlLensLogger(SqlLensProperties props) {
        return new SqlLensLogger(
            props.getSlowThreshold(),
            props.isPrintFullSql(),
            props.getMaxSqlLength()
        );
    }

    /**
     * Sql lens reporter sql lens reporter.
     *
     * @return the sql lens reporter
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public SqlLensReporter sqlLensReporter() {
        SqlLensReportConfig config = SqlLensReportConfig.load();
        return new SqlLensReporter(config);
    }

    /**
     * Sql log interceptor sql log interceptor.
     *
     * @param logger
     *            the logger
     * @param analyzer
     *            the analyzer
     * @param reporter
     *            the reporter
     * @return the SQL log interceptor
     */
    @Bean
    @ConditionalOnMissingBean
    public SqlLogInterceptor sqlLogInterceptor(SqlLensLogger logger, ExplainAnalyzer analyzer,
                                               SqlLensReporter reporter) {
        return new SqlLogInterceptor(logger, analyzer, reporter);
    }
}
