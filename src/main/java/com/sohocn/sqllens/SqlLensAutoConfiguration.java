package com.sohocn.sqllens;

import com.sohocn.sqllens.report.SqlLensReportConfig;
import com.sohocn.sqllens.report.SqlLensReporter;
import org.apache.ibatis.executor.Executor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(Executor.class)
@EnableConfigurationProperties(SqlLensProperties.class)
@ConditionalOnProperty(prefix = "sqllens", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SqlLensAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public ExplainAnalyzer explainAnalyzer(SqlLensProperties props) {
        return new ExplainAnalyzer(
                props.isExplainEnabled(),
                props.getExcludeTables()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public SqlLensLogger sqlLensLogger(SqlLensProperties props) {
        return new SqlLensLogger(
            props.getSlowThreshold(),
            props.isPrintFullSql(),
            props.getMaxSqlLength()
        );
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public SqlLensReporter sqlLensReporter() {
        SqlLensReportConfig config = SqlLensReportConfig.load();
        return new SqlLensReporter(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public SqlLogInterceptor sqlLogInterceptor(SqlLensLogger logger, ExplainAnalyzer analyzer,
                                               SqlLensReporter reporter) {
        return new SqlLogInterceptor(logger, analyzer, reporter);
    }
}
