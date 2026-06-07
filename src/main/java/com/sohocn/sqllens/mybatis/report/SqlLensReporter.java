package com.sohocn.sqllens.mybatis.report;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Sql lens reporter.
 *
 * @author longjianghu
 */
public class SqlLensReporter {
    private static final Logger log = LoggerFactory.getLogger(SqlLensReporter.class);
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    private static final int QUEUE_CAPACITY = 1000;

    private volatile SqlLensReportConfig config;
    private final ScheduledExecutorService executor;
    private final ArrayDeque<SqlLensReportData> queue = new ArrayDeque<>(QUEUE_CAPACITY);
    private final ReentrantLock lock = new ReentrantLock();
    private int consecutiveFailures = 0;
    private volatile boolean schedulerStarted;
    private volatile boolean warnedInvalidConfig;

    /**
     * Instantiates a new Sql lens reporter.
     *
     * @param config
     *            the config
     */
    public SqlLensReporter(SqlLensReportConfig config) {
        this.config = config;
        this.executor = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "sqllens-reporter");
            t.setDaemon(true);
            return t;
        });

        if (config.isValid()) {
            this.executor.scheduleWithFixedDelay(this::flush, 5, config.getReportIntervalSeconds(), TimeUnit.SECONDS);
            schedulerStarted = true;
            log.info("[SqlLens] Reporter started, reporting to {}", config.getServerUrl());
        }
    }

    /**
     * Reload config from .idea/sqllens.json if the file has changed.
     * Returns cached config when unchanged (no file I/O overhead).
     */
    private SqlLensReportConfig getEffectiveConfig() {
        SqlLensReportConfig latest = SqlLensReportConfig.load();
        if (latest.isValid() && (config == null || !config.equals(latest))) {
            log.info("[SqlLens] Config updated, new url: {}", latest.getServerUrl());
            config = latest;
        }
        if (config == null || !config.isValid()) {
            config = latest;
        }
        return config;
    }

    /**
     * Report.
     *
     * @param data
     *            the data
     */
    public void report(SqlLensReportData data) {
        if (!config.isValid()) {
            // try reloading — config may have been added since startup
            SqlLensReportConfig newConfig = SqlLensReportConfig.load();
            if (!newConfig.isValid()) {
                if (!warnedInvalidConfig) {
                    warnedInvalidConfig = true;
                    log.warn("[SqlLens] Report skipped: config not loaded. "
                            + "Please create .idea/sqllens.json with serverUrl and token "
                            + "in your project root directory.");
                }
                return;
            }
            config = newConfig;
            warnedInvalidConfig = false;
            startScheduler();
        }
        lock.lock();
        try {
            if (queue.size() >= QUEUE_CAPACITY) {
                queue.pollFirst();
            }
            queue.offerLast(data);
        } finally {
            lock.unlock();
        }
    }

    private synchronized void startScheduler() {
        if (schedulerStarted) {
            return;
        }
        this.executor.scheduleWithFixedDelay(this::flush, 5, config.getReportIntervalSeconds(), TimeUnit.SECONDS);
        schedulerStarted = true;
        log.info("[SqlLens] Reporter started (lazy), reporting to {}", config.getServerUrl());
    }

    /**
     * Shutdown.
     */
    public void shutdown() {
        executor.shutdown();
        this.flush();
    }

    private void flush() {
        List<SqlLensReportData> batch;
        lock.lock();
        try {
            batch = new ArrayList<>(queue);
            queue.clear();
        } finally {
            lock.unlock();
        }
        if (batch.isEmpty()) {
            return;
        }
        boolean success = sendBatch(batch);
        lock.lock();
        try {
            if (success) {
                consecutiveFailures = 0;
            } else {
                consecutiveFailures++;
                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    log.warn("[SqlLens] Report failed {} consecutive times, discarding {} items",
                            consecutiveFailures, batch.size());
                    consecutiveFailures = 0;
                } else {
                    for (int i = batch.size() - 1; i >= 0; i--) {
                        queue.addFirst(batch.get(i));
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean sendBatch(List<SqlLensReportData> batch) {
        SqlLensReportConfig currentConfig = getEffectiveConfig();
        HttpURLConnection conn = null;
        try {
            String jsonArray = toJsonArray(batch);
            byte[] body = jsonArray.getBytes(StandardCharsets.UTF_8);

            URL url = new URL(currentConfig.getServerUrl());
            String token = currentConfig.getToken();
            String maskedToken = maskToken(token);

            log.info("[SqlLens] Pushing report -> url: {}, token: {}, data: {}",
                    url, maskedToken, jsonArray);

            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setDoOutput(true);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            conn.setFixedLengthStreamingMode(body.length);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
                os.flush();
            }

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                log.warn("[SqlLens] Report failed, status: {}", code);
                return false;
            } else {
                log.info("[SqlLens] Report success, count: {}, status: {}", batch.size(), code);
                return true;
            }
        } catch (Exception e) {
            log.warn("[SqlLens] Report failed: {}", e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Mask token for logging, showing only first 4 and last 4 characters.
     *
     * @param token the token
     * @return masked token
     */
    private static String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return "null";
        }
        if (token.length() <= 8) {
            return token.charAt(0) + "***";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }

    private static String toJsonArray(List<SqlLensReportData> batch) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < batch.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(batch.get(i).toJson());
        }
        sb.append("]");
        return sb.toString();
    }
}
