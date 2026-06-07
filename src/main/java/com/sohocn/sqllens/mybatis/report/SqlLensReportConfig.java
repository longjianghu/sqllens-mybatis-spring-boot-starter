package com.sohocn.sqllens.mybatis.report;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    private static volatile SqlLensReportConfig cachedConfig;
    private static long cachedLastModified = -1;
    private static final Object loadLock = new Object();

    private SqlLensReportConfig(String serverUrl, String token, int reportIntervalSeconds) {
        this.serverUrl = serverUrl;
        this.token = token;
        this.reportIntervalSeconds = reportIntervalSeconds;
    }

    /**
     * Load sql lens report config. Returns cached instance if the config file hasn't changed.
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
            log.debug("[SqlLens] Config file not found: {}", configFile.getAbsolutePath());
            cachedConfig = null;
            cachedLastModified = -1;
            return new SqlLensReportConfig(null, null, 5);
        }

        log.info("[SqlLens] Loading config from: {}", configFile.getAbsolutePath());

        long lastModified = configFile.lastModified();
        SqlLensReportConfig cached = cachedConfig;
        if (cached != null && lastModified == cachedLastModified) {
            return cached;
        }

        synchronized (loadLock) {
            // double-check after acquiring lock
            if (cachedConfig != null && lastModified == cachedLastModified) {
                return cachedConfig;
            }
            try {
                String content = new String(Files.readAllBytes(configFile.toPath()));
                String serverUrl = extractJsonValue(content, "serverUrl");
                String token = extractJsonValue(content, "token");
                int reportIntervalSeconds = extractJsonIntValue(content, "reportIntervalSeconds", 5);
                SqlLensReportConfig config = new SqlLensReportConfig(serverUrl, token, reportIntervalSeconds);
                cachedConfig = config;
                cachedLastModified = lastModified;
                if (config.isValid()) {
                    log.info("[SqlLens] Remote reporting enabled: {}", serverUrl);
                }
                return config;
            } catch (Exception e) {
                log.warn("[SqlLens] Failed to load report config: {}", e.getMessage());
                return new SqlLensReportConfig(null, null, 5);
            }
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

    /**
     * Find the .idea directory of the project.
     * Search order:
     * 1. System property {@code sqllens.project.dir} if set
     * 2. JVM working directory ({@code user.dir}) — preferred when this
     *    library runs as a dependency JAR
     * 3. Class file location (e.g. target/classes inside the project)
     * <p>
     * Starting from the resolved directory, walk up the filesystem tree
     * until a {@code .idea} directory is found.
     *
     * @return the .idea directory, or null if not found
     */
    private static File findIdeaDir() {
        Path start = resolveProjectRoot();
        Path current = start.toAbsolutePath();

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
     * Resolve the project root directory to start the .idea search from.
     */
    private static Path resolveProjectRoot() {
        // 1. Explicit system property override
        String explicitDir = System.getProperty("sqllens.project.dir");
        if (explicitDir != null && !explicitDir.isEmpty()) {
            return Paths.get(explicitDir);
        }

        // 2. Try JVM working directory first — most reliable for finding the
        //    user's project .idea, especially when this library runs as a
        //    dependency JAR (where class location points into ~/.m2/repository).
        Path userDir = new File(System.getProperty("user.dir")).toPath();
        if (containsIdeaDir(userDir)) {
            return userDir;
        }

        // 3. Infer from class file location (works in IDE dev and standalone JAR)
        Path classLocation = getClassLocation();
        if (classLocation != null) {
            return classLocation;
        }

        // 4. Fall back to JVM working directory as-is
        return userDir;
    }

    /**
     * Check whether a .idea directory exists at or above the given path.
     */
    private static boolean containsIdeaDir(Path start) {
        Path current = start.toAbsolutePath();
        while (current != null) {
            if (new File(current.toFile(), ".idea").isDirectory()) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    /**
     * Get the directory where this class is loaded from.
     * In an IDE this is typically {@code target/classes}; from the parent
     * directory (the project root) we can find {@code .idea}.
     * In a fat JAR deployment this returns the JAR's parent directory.
     *
     * @return the directory, or null if it cannot be determined
     */
    private static Path getClassLocation() {
        try {
            URL url = SqlLensReportConfig.class.getProtectionDomain().getCodeSource().getLocation();
            if (url == null) {
                return null;
            }
            Path path = Paths.get(url.toURI()).toAbsolutePath();
            // If it's a JAR file, start from its parent directory
            if (Files.isRegularFile(path)) {
                return path.getParent();
            }
            // It's a directory (e.g. target/classes); walk up from here
            return path;
        } catch (URISyntaxException e) {
            return null;
        }
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SqlLensReportConfig that = (SqlLensReportConfig)o;
        return reportIntervalSeconds == that.reportIntervalSeconds
            && (serverUrl != null ? serverUrl.equals(that.serverUrl) : that.serverUrl == null)
            && (token != null ? token.equals(that.token) : that.token == null);
    }

    @Override
    public int hashCode() {
        int result = serverUrl != null ? serverUrl.hashCode() : 0;
        result = 31 * result + (token != null ? token.hashCode() : 0);
        result = 31 * result + reportIntervalSeconds;
        return result;
    }
}
