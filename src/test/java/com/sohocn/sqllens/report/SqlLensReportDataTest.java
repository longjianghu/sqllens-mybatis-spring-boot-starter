package com.sohocn.sqllens.report;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlLensReportDataTest {

    @Test
    void toJson_serializesCorrectly() {
        SqlLensReportData data = new SqlLensReportData(
            "SELECT * FROM user WHERE id = 1",
            "SELECT * FROM user WHERE id = 1",
            120L,
            "id=1, type=ALL",
            "com.example.UserMapper.findById"
        );

        String json = data.toJson();

        assertTrue(json.contains("\"sql\":\"SELECT * FROM user WHERE id = 1\""));
        assertTrue(json.contains("\"duration\":120"));
        assertTrue(json.contains("\"explainResult\":\"id=1, type=ALL\""));
        assertTrue(json.contains("\"mapperMethod\":\"com.example.UserMapper.findById\""));
        assertTrue(json.contains("\"timestamp\":"));
    }

    @Test
    void toJson_handlesNullExplainResult() {
        SqlLensReportData data = new SqlLensReportData(
            "INSERT INTO user VALUES (1)",
            "INSERT INTO user VALUES (1)",
            50L,
            null,
            "com.example.UserMapper.insert"
        );

        String json = data.toJson();

        assertTrue(json.contains("\"sql\":\"INSERT INTO user VALUES (1)\""));
        assertFalse(json.contains("explainResult"));
    }

    @Test
    void escapeJson_handlesSpecialCharacters() {
        String input = "He said \"hello\"\nnew\\line";
        String escaped = SqlLensReportData.escapeJson(input);
        assertEquals("He said \\\"hello\\\"\\nnew\\\\line", escaped);
    }

    @Test
    void toJson_handlesSqlWithQuotes() {
        SqlLensReportData data = new SqlLensReportData(
            "SELECT * FROM user WHERE name = 'it\\'s'",
            "SELECT * FROM user WHERE name = 'it\\'s'",
            10L,
            null,
            "com.example.UserMapper.findByName"
        );

        String json = data.toJson();
        assertNotNull(json);
        assertTrue(json.contains("\"sql\":"));
    }
}
