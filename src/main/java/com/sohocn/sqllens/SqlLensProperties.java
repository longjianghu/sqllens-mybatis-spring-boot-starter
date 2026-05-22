package com.sohocn.sqllens;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "sqllens")
public class SqlLensProperties {
    private boolean enabled = true;
    private boolean explainEnabled = false;
    private int maxSqlLength = 4096;
    private boolean printFullSql = true;
    private long slowThreshold = 1000;
    private List<String> excludeTables = Arrays.asList(
        "information_schema.",
        "mysql.",
        "performance_schema."
    );

    public List<String> getExcludeTables() {
        return excludeTables;
    }

    public int getMaxSqlLength() {
        return maxSqlLength;
    }

    public long getSlowThreshold() {
        return slowThreshold;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isExplainEnabled() {
        return explainEnabled;
    }

    public boolean isPrintFullSql() {
        return printFullSql;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setExcludeTables(List<String> excludeTables) {
        this.excludeTables = excludeTables;
    }

    public void setExplainEnabled(boolean explainEnabled) {
        this.explainEnabled = explainEnabled;
    }

    public void setMaxSqlLength(int maxSqlLength) {
        this.maxSqlLength = maxSqlLength;
    }

    public void setPrintFullSql(boolean printFullSql) {
        this.printFullSql = printFullSql;
    }

    public void setSlowThreshold(long slowThreshold) {
        this.slowThreshold = slowThreshold;
    }
}
