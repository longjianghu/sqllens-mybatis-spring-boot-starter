package com.sohocn.sqllens.mybatis.report;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

class SqlLensReportConfigTest {

    @Test
    void load_readsConfig_whenFileExists() throws IOException {
        File tempDir = Files.createTempDirectory("sqllens-test").toFile();
        File ideaDir = new File(tempDir, ".idea");
        ideaDir.mkdirs();
        File configFile = new File(ideaDir, "sqllens.json");
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("{\"serverUrl\":\"http://localhost:8080\",\"token\":\"abc123\"}");
        }
        System.setProperty("sqllens.project.dir", tempDir.getAbsolutePath());
        try {
            SqlLensReportConfig config = SqlLensReportConfig.load();
            assertTrue(config.isValid());
            assertEquals("http://localhost:8080", config.getServerUrl());
            assertEquals("abc123", config.getToken());
        } finally {
            System.clearProperty("sqllens.project.dir");
            deleteRecursive(tempDir);
        }
    }

    @Test
    void load_returnsInvalid_whenNoConfigFile() throws IOException {
        File tempDir = Files.createTempDirectory("sqllens-test").toFile();
        File ideaDir = new File(tempDir, ".idea");
        ideaDir.mkdirs();
        System.setProperty("sqllens.project.dir", tempDir.getAbsolutePath());
        try {
            SqlLensReportConfig config = SqlLensReportConfig.load();
            assertFalse(config.isValid());
        } finally {
            System.clearProperty("sqllens.project.dir");
            deleteRecursive(tempDir);
        }
    }

    @Test
    void extractJsonValue_parsesCorrectly() {
        String json = "{\"serverUrl\":\"http://localhost:8080\",\"token\":\"xyz\"}";
        assertEquals("http://localhost:8080", SqlLensReportConfig.extractJsonValue(json, "serverUrl"));
        assertEquals("xyz", SqlLensReportConfig.extractJsonValue(json, "token"));
    }

    @Test
    void extractJsonValue_returnsNull_whenKeyNotFound() {
        String json = "{\"serverUrl\":\"http://localhost:8080\"}";
        assertNull(SqlLensReportConfig.extractJsonValue(json, "missing"));
    }

    private static void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }
}
