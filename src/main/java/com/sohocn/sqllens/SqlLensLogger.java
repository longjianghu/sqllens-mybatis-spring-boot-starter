package com.sohocn.sqllens;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class SqlLensLogger {
    private static final Logger log = LoggerFactory.getLogger(SqlLensLogger.class);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final int maxSqlLength;
    private final boolean printFullSql;
    private final long slowThreshold;

    public SqlLensLogger(long slowThreshold, boolean printFullSql, int maxSqlLength) {
        this.slowThreshold = slowThreshold;
        this.printFullSql = printFullSql;
        this.maxSqlLength = maxSqlLength;
    }

    public String formatParameter(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String || value instanceof Character) {
            return "'" + value.toString().replace("'", "''") + "'";
        }
        if (value instanceof Date) {
            return "'" + value + "'";
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "1" : "0";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        return value.toString();
    }

    public String formatSql(BoundSql boundSql, Configuration configuration) {
        if (!printFullSql) {
            return this.truncate(boundSql.getSql().trim());
        }
        String rawSql = boundSql.getSql().trim();
        List<Object> paramValues;
        try {
            paramValues = SqlLensParamResolver.extractAll(boundSql, configuration);
        } catch (Exception e) {
            log.warn("[SqlLens] Parameter extraction failed: {}", e.getMessage());
            paramValues = Collections.emptyList();
        }
        String result = this.replacePlaceholders(rawSql, paramValues);
        result = WHITESPACE.matcher(result).replaceAll(" ").trim();
        return this.truncate(result);
    }

    public void logExecution(BoundSql boundSql, Configuration configuration, long durationMs) {
        if (durationMs >= slowThreshold) {
            if (log.isWarnEnabled()) {
                log.warn("[SqlLens][{}ms] {}", durationMs, this.formatSql(boundSql, configuration));
            }
        } else {
            if (log.isInfoEnabled()) {
                log.info("[SqlLens][{}ms] {}", durationMs, this.formatSql(boundSql, configuration));
            }
        }
    }

    String truncate(String sql) {
        if (sql.length() > maxSqlLength) {
            return sql.substring(0, maxSqlLength) + "...(truncated)";
        }
        return sql;
    }

    private String replacePlaceholders(String sql, List<Object> parameterValues) {
        if (parameterValues.isEmpty()) {
            return sql;
        }
        StringBuilder sb = new StringBuilder(sql.length() * 2);
        int paramIndex = 0;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '?' && paramIndex < parameterValues.size()) {
                sb.append(this.formatParameter(parameterValues.get(paramIndex++)));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
