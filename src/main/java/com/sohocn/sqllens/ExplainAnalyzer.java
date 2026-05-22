package com.sohocn.sqllens;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ExplainAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(ExplainAnalyzer.class);

    private final Set<String> excludeTablesUpper;
    private final boolean explainEnabled;

    public ExplainAnalyzer(boolean explainEnabled, List<String> excludeTables) {
        this.explainEnabled = explainEnabled;
        this.excludeTablesUpper = excludeTables == null
            ? Collections.emptySet()
            : excludeTables.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    public String analyze(BoundSql boundSql, Configuration configuration, DataSource dataSource) {
        if (boundSql == null || configuration == null || dataSource == null) {
            return null;
        }
        String sql = boundSql.getSql().trim();
        if (!this.shouldExplain(sql)) {
            return null;
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = this.createExplainStatement(sql, boundSql, configuration, connection);
             ResultSet rs = stmt.executeQuery()) {
            return this.parseExplainResult(rs);
        } catch (SQLException e) {
            log.warn("[SqlLens] EXPLAIN failed: {}", e.getMessage());
            return null;
        }
    }

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

    private PreparedStatement createExplainStatement(String sql, BoundSql boundSql,
                                                     Configuration configuration,
                                                     Connection connection) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("EXPLAIN " + sql);
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
                sb.append(meta.getColumnName(i)).append("=").append(rs.getObject(i));
            }
        } while (rs.next());

        String result = sb.toString();
        log.info("[SqlLens][EXPLAIN] {}", result);
        return result;
    }
}
