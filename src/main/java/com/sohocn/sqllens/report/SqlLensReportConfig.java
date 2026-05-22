package com.sohocn.sqllens.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class SqlLensReportConfig {
    private static final Logger log = LoggerFactory.getLogger(SqlLensReportConfig.class);

    private final String serverUrl;
    private final String token;

    private SqlLensReportConfig(String serverUrl, String token) {
        this.serverUrl = serverUrl;
        this.token = token;
    }

    public static SqlLensReportConfig load() {
        File ideaDir = findIdeaDir();
        if (ideaDir == null) {
            return new SqlLensReportConfig(null, null);
        }
        File configFile = new File(ideaDir, "sqllens.json");
        if (!configFile.isFile()) {
            return new SqlLensReportConfig(null, null);
        }
        try {
            String content = new String(Files.readAllBytes(configFile.toPath()));
            String serverUrl = extractJsonValue(content, "serverUrl");
            String token = extractJsonValue(content, "token");
            SqlLensReportConfig config = new SqlLensReportConfig(serverUrl, token);
            if (config.isValid()) {
                log.info("[SqlLens] Remote reporting enabled: {}", serverUrl);
            }
            return config;
        } catch (Exception e) {
            log.warn("[SqlLens] Failed to load report config: {}", e.getMessage());
            return new SqlLensReportConfig(null, null);
        }
    }

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

    public String getServerUrl() {
        return serverUrl;
    }

    public String getToken() {
        return token;
    }

    public boolean isValid() {
        return serverUrl != null && !serverUrl.isEmpty()
            && token != null && !token.isEmpty();
    }
}
