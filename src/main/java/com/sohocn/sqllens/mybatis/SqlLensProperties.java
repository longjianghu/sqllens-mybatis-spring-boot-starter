package com.sohocn.sqllens.mybatis;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The type Sql lens properties.
 *
 * @author longjianghu
 */
@ConfigurationProperties(prefix = "sqllens")
public class SqlLensProperties {
    private boolean enabled = false;
    private boolean explainEnabled = false;
    private boolean explainAnalyze = false;
    private int maxSqlLength = 0;
    private boolean printFullSql = true;
    private long slowThreshold = 1000;
    private List<String> excludeTables = Arrays.asList(
        "information_schema.",
        "mysql.",
        "performance_schema."
    );

    /**
     * Gets exclude tables.
     *
     * @return the exclude tables
     */
    public List<String> getExcludeTables() {
        return excludeTables;
    }

    /**
     * Gets max sql length.
     *
     * @return the max sql length
     */
    public int getMaxSqlLength() {
        return maxSqlLength;
    }

    /**
     * Gets slow threshold.
     *
     * @return the slow threshold
     */
    public long getSlowThreshold() {
        return slowThreshold;
    }

    /**
     * Is enabled boolean.
     *
     * @return the boolean
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Is explain enabled boolean.
     *
     * @return the boolean
     */
    public boolean isExplainEnabled() {
        return explainEnabled;
    }

    /**
     * Is explain analyze boolean.
     *
     * @return the explain analyze
     */
    public boolean isExplainAnalyze() {
        return explainAnalyze;
    }

    /**
     * Is print full sql boolean.
     *
     * @return the boolean
     */
    public boolean isPrintFullSql() {
        return printFullSql;
    }

    /**
     * Sets enabled.
     *
     * @param enabled
     *            the enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Sets exclude tables.
     *
     * @param excludeTables
     *            the exclude tables
     */
    public void setExcludeTables(List<String> excludeTables) {
        this.excludeTables = excludeTables;
    }

    /**
     * Sets explain enabled.
     *
     * @param explainEnabled
     *            the explain enabled
     */
    public void setExplainEnabled(boolean explainEnabled) {
        this.explainEnabled = explainEnabled;
    }

    /**
     * Sets explain analyze.
     *
     * @param explainAnalyze
     *            the explain analyze
     */
    public void setExplainAnalyze(boolean explainAnalyze) {
        this.explainAnalyze = explainAnalyze;
    }

    /**
     * Sets max sql length.
     *
     * @param maxSqlLength
     *            the max sql length
     */
    public void setMaxSqlLength(int maxSqlLength) {
        this.maxSqlLength = maxSqlLength;
    }

    /**
     * Sets print full sql.
     *
     * @param printFullSql
     *            the print full sql
     */
    public void setPrintFullSql(boolean printFullSql) {
        this.printFullSql = printFullSql;
    }

    /**
     * Sets slow threshold.
     *
     * @param slowThreshold
     *            the slow threshold
     */
    public void setSlowThreshold(long slowThreshold) {
        this.slowThreshold = slowThreshold;
    }
}
