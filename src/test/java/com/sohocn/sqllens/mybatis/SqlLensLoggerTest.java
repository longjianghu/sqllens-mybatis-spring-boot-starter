package com.sohocn.sqllens.mybatis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class SqlLensLoggerTest {

    @Test
    void formatSql_returnsRawSql_whenPrintFullSqlDisabled() {
        SqlLensLogger logger = new SqlLensLogger(3000, false, 4096);
        BoundSql boundSql = mock(BoundSql.class);
        when(boundSql.getSql()).thenReturn("SELECT * FROM user WHERE id = ?");

        String result = logger.formatSql(boundSql, null);
        assertEquals("SELECT * FROM user WHERE id = ?", result);
    }

    @Test
    void formatSql_replacesStringPlaceholder() {
        SqlLensLogger logger = new SqlLensLogger(3000, true, 4096);
        BoundSql boundSql = mock(BoundSql.class);
        Configuration config = mock(Configuration.class);
        ParameterMapping mapping = mock(ParameterMapping.class);

        when(boundSql.getSql()).thenReturn("SELECT * FROM user WHERE name = ?");
        when(boundSql.getParameterMappings()).thenReturn(Collections.singletonList(mapping));
        when(mapping.getProperty()).thenReturn("name");
        when(boundSql.hasAdditionalParameter("name")).thenReturn(true);
        when(boundSql.getAdditionalParameter("name")).thenReturn("Alice");

        String result = logger.formatSql(boundSql, config);
        assertTrue(result.contains("'Alice'"));
    }

    @Test
    void formatSql_truncatesLongSql() {
        SqlLensLogger logger = new SqlLensLogger(3000, true, 20);
        BoundSql boundSql = mock(BoundSql.class);
        Configuration config = mock(Configuration.class);

        when(boundSql.getSql()).thenReturn("SELECT * FROM very_long_table_name WHERE id = 1");
        when(boundSql.getParameterMappings()).thenReturn(Collections.emptyList());

        String result = logger.formatSql(boundSql, config);
        assertTrue(result.length() <= 50);
        assertTrue(result.contains("truncated"));
    }

    @Test
    void formatParameter_handlesNull() {
        SqlLensLogger logger = new SqlLensLogger(3000, true, 4096);
        assertEquals("NULL", logger.formatParameter(null));
    }

    @Test
    void formatParameter_handlesString() {
        SqlLensLogger logger = new SqlLensLogger(3000, true, 4096);
        assertEquals("'hello'", logger.formatParameter("hello"));
    }

    @Test
    void formatParameter_handlesStringWithQuotes() {
        SqlLensLogger logger = new SqlLensLogger(3000, true, 4096);
        assertEquals("'it''s'", logger.formatParameter("it's"));
    }

    @Test
    void formatParameter_handlesBoolean() {
        SqlLensLogger logger = new SqlLensLogger(3000, true, 4096);
        assertEquals("1", logger.formatParameter(Boolean.TRUE));
        assertEquals("0", logger.formatParameter(Boolean.FALSE));
    }

    @Test
    void formatParameter_handlesNumber() {
        SqlLensLogger logger = new SqlLensLogger(3000, true, 4096);
        assertEquals("42", logger.formatParameter(42));
        assertEquals("3.14", logger.formatParameter(3.14));
    }

    @Test
    void formatParameter_handlesDate() {
        SqlLensLogger logger = new SqlLensLogger(3000, true, 4096);
        Date date = new Date();
        String result = logger.formatParameter(date);
        assertTrue(result.startsWith("'"));
        assertTrue(result.endsWith("'"));
    }
}
