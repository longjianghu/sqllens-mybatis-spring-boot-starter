package com.sohocn.sqllens.mybatis;

import java.sql.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Explain analyzer.
 *
 * @author longjianghu
 */
public class ExplainAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(ExplainAnalyzer.class);

    private final Set<String> excludeTablesUpper;
    private final boolean explainEnabled;
    private final boolean explainAnalyze;

    /**
     * Instantiates a new Explain analyzer.
     *
     * @param explainEnabled
     *            to explain enabled
     * @param explainAnalyze
     *            to explain analyze
     * @param excludeTables
     *            the exclude tables
     */
    public ExplainAnalyzer(boolean explainEnabled, boolean explainAnalyze, List<String> excludeTables) {
        this.explainEnabled = explainEnabled;
        this.explainAnalyze = explainAnalyze;
        this.excludeTablesUpper = excludeTables == null
            ? Collections.emptySet()
            : excludeTables.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    /**
     * Analyze string.
     *
     * @param boundSql
     *            the bound SQL
     * @param configuration
     *            the configuration
     * @param dataSource
     *            the data source
     * @return the string
     */
    public String analyze(BoundSql boundSql, Configuration configuration, DataSource dataSource) {
        if (boundSql == null || configuration == null || dataSource == null) {
            return null;
        }
        String sql = boundSql.getSql().trim();
        if (!this.shouldExplain(sql)) {
            return null;
        }
        String prefix = explainAnalyze ? "EXPLAIN ANALYZE " : "EXPLAIN ";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = this.createExplainStatement(prefix, sql, boundSql, configuration, connection);
             ResultSet rs = stmt.executeQuery()) {
            return this.parseExplainResult(rs);
        } catch (SQLException e) {
            log.warn("[SqlLens] EXPLAIN failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Should explain boolean.
     *
     * @param sql
     *            the SQL
     * @return the boolean
     */
    public boolean shouldExplain(String sql) {
        if (!explainEnabled || sql == null) {
            return false;
        }
        String upperSql = sql.trim().toUpperCase();
        if (!upperSql.startsWith("SELECT")) {
            return false;
        }
        for (String prefix : excludeTablesUpper) {
            if (upperSql.contains(prefix)) {
                return false;
            }
        }
        return true;
    }

    private PreparedStatement createExplainStatement(String prefix, String sql, BoundSql boundSql,
                                                     Configuration configuration,
                                                     Connection connection) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(prefix + sql);
        List<ParameterMapping> mappings = boundSql.getParameterMappings();
        for (int i = 0; i < mappings.size(); i++) {
            String property = mappings.get(i).getProperty();
            Object value = SqlLensParamResolver.resolve(boundSql, property, configuration);
            stmt.setObject(i + 1, value);
        }
        return stmt;
    }

    private String parseExplainResult(ResultSet rs) throws SQLException {
        if (!rs.next()) {
            return null;
        }
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        StringBuilder sb = new StringBuilder();

        do {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            for (int i = 1; i <= cols; i++) {
                if (i > 1) {
                    sb.append(", ");
                }
                if (cols == 1) {
                    // EXPLAIN ANALYZE returns a single column with tree output,
                    // skip the column name and "-> " tree markers.
                    String val = String.valueOf(rs.getObject(i));
                    sb.append(val.replaceAll("(?m)^-> ", ""));
                } else {
                    sb.append(meta.getColumnName(i)).append("=").append(rs.getObject(i));
                }
            }
        } while (rs.next());

        String result = sb.toString();
        log.info("[SqlLens][EXPLAIN] {}", result);
        return result;
    }
}
