package com.sohocn.sqllens.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class SqlLensReporter {
    private static final Logger log = LoggerFactory.getLogger(SqlLensReporter.class);
    private static final int QUEUE_CAPACITY = 1000;

    private final SqlLensReportConfig config;
    private final ScheduledExecutorService executor;
    private final ArrayDeque<SqlLensReportData> queue = new ArrayDeque<>(QUEUE_CAPACITY);
    private final ReentrantLock lock = new ReentrantLock();

    public SqlLensReporter(SqlLensReportConfig config) {
        this.config = config;
        this.executor = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "sqllens-reporter");
            t.setDaemon(true);
            return t;
        });
        if (config.isValid()) {
            this.executor.scheduleWithFixedDelay(this::flush, 5, 5, TimeUnit.SECONDS);
            log.info("[SqlLens] Reporter started, reporting to {}", config.getServerUrl());
        }
    }

    public void report(SqlLensReportData data) {
        if (!config.isValid()) {
            return;
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
        for (SqlLensReportData data : batch) {
            this.send(data);
        }
    }

    private void send(SqlLensReportData data) {
        HttpURLConnection conn = null;

        try {
            URL url = new URL(config.getServerUrl());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Authorization", "Bearer " + config.getToken());
            conn.setDoOutput(true);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);

            byte[] body = data.toJson().getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(body.length);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
                os.flush();
            }

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                log.warn("[SqlLens] Report failed, status: {}", code);
            }
        } catch (Exception e) {
            log.warn("[SqlLens] Report failed: {}", e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
