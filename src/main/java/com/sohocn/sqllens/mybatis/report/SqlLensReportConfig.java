package com.sohocn.sqllens.mybatis.report;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Sql lens report config.
 *
 * @author longjianghu
 */
public class SqlLensReportConfig {
    private static final Logger log = LoggerFactory.getLogger(SqlLensReportConfig.class);

    private final String serverUrl;
    private final String token;
    private final int reportIntervalSeconds;

    private SqlLensReportConfig(String serverUrl, String token, int reportIntervalSeconds) {
        this.serverUrl = serverUrl;
        this.token = token;
        this.reportIntervalSeconds = reportIntervalSeconds;
    }

    /**
     * Load sql lens report config.
     *
     * @return the SQL lens report config
     */
    public static SqlLensReportConfig load() {
        File ideaDir = findIdeaDir();
        if (ideaDir == null) {
            return new SqlLensReportConfig(null, null, 5);
        }
        File configFile = new File(ideaDir, "sqllens.json");
        if (!configFile.isFile()) {
            return new SqlLensReportConfig(null, null, 5);
        }
        try {
            String content = new String(Files.readAllBytes(configFile.toPath()));
            String serverUrl = extractJsonValue(content, "serverUrl");
            String token = extractJsonValue(content, "token");
            int reportIntervalSeconds = extractJsonIntValue(content, "reportIntervalSeconds", 5);
            SqlLensReportConfig config = new SqlLensReportConfig(serverUrl, token, reportIntervalSeconds);
            if (config.isValid()) {
                log.info("[SqlLens] Remote reporting enabled: {}", serverUrl);
            }
            return config;
        } catch (Exception e) {
            log.warn("[SqlLens] Failed to load report config: {}", e.getMessage());
            return new SqlLensReportConfig(null, null, 5);
        }
    }

    /**
     * Extract json value string.
     *
     * @param json
     *            the JSON
     * @param key
     *            the key
     * @return the string
     */
    static String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex < 0) {
            return null;
        }
        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex < 0) {
            return null;
        }
        int quoteStart = json.indexOf('"', colonIndex + 1);
        if (quoteStart < 0) {
            return null;
        }
        int quoteEnd = json.indexOf('"', quoteStart + 1);
        if (quoteEnd < 0) {
            return null;
        }
        return json.substring(quoteStart + 1, quoteEnd);
    }

    /**
     * Extract json int value int.
     *
     * @param json
     *            the JSON
     * @param key
     *            the key
     * @param defaultValue
     *            the default value
     * @return the int
     */
    static int extractJsonIntValue(String json, String key, int defaultValue) {
        String value = extractJsonValue(json, key);
        if (value == null) {
            return defaultValue;
        }
        try {
            int result = Integer.parseInt(value);
            return result > 0 ? result : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static File findIdeaDir() {
        Path current = new File(System.getProperty("user.dir")).toPath().toAbsolutePath();

        while (current != null) {
            File ideaDir = new File(current.toFile(), ".idea");
            if (ideaDir.isDirectory()) {
                return ideaDir;
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * Gets server url.
     *
     * @return the server url
     */
    public String getServerUrl() {
        return serverUrl;
    }

    /**
     * Gets token.
     *
     * @return the token
     */
    public String getToken() {
        return token;
    }

    /**
     * Gets report interval seconds.
     *
     * @return the report interval seconds
     */
    public int getReportIntervalSeconds() {
        return reportIntervalSeconds;
    }

    /**
     * Is valid boolean.
     *
     * @return the boolean
     */
    public boolean isValid() {
        return serverUrl != null && !serverUrl.isEmpty()
            && token != null && !token.isEmpty();
    }
}
