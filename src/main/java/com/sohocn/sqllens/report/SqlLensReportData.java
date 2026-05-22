package com.sohocn.sqllens.report;

public class SqlLensReportData {
    private final long duration;
    private final String explainResult;
    private final String formattedSql;
    private final String mapperMethod;
    private final String sql;
    private final long timestamp;

    public SqlLensReportData(String sql, String formattedSql, long duration,
                             String explainResult, String mapperMethod) {
        this.sql = sql;
        this.formattedSql = formattedSql;
        this.duration = duration;
        this.explainResult = explainResult;
        this.timestamp = System.currentTimeMillis();
        this.mapperMethod = mapperMethod;
    }

    static String escapeJson(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }

    private static void appendField(StringBuilder sb, String key, String value) {
        sb.append("\"").append(key).append("\":\"");
        if (value != null) {
            sb.append(escapeJson(value));
        }
        sb.append("\"");
    }

    public String getSql() { return sql; }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        appendField(sb, "sql", sql);
        sb.append(",");
        appendField(sb, "formattedSql", formattedSql);
        sb.append(",");

        sb.append("\"duration\":").append(duration);
        sb.append(",");

        if (explainResult != null) {
            appendField(sb, "explainResult", explainResult);
            sb.append(",");
        }

        sb.append("\"timestamp\":").append(timestamp);
        sb.append(",");

        appendField(sb, "mapperMethod", mapperMethod);
        sb.append("}");
        return sb.toString();
    }
}
