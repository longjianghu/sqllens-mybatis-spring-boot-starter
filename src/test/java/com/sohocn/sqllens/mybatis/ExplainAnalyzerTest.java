package com.sohocn.sqllens.mybatis;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Collections;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

class ExplainAnalyzerTest {

    @Test
    void shouldExplain_returnsFalse_whenDisabled() {
        ExplainAnalyzer analyzer = new ExplainAnalyzer(false, false, Collections.singletonList("information_schema."));
        assertFalse(analyzer.shouldExplain("SELECT * FROM user"));
    }

    @Test
    void shouldExplain_returnsFalse_forNonSelect() {
        ExplainAnalyzer analyzer = new ExplainAnalyzer(true, false, Collections.singletonList("information_schema."));
        assertFalse(analyzer.shouldExplain("INSERT INTO user VALUES (1, 'test')"));
    }

    @Test
    void shouldExplain_returnsFalse_forSystemTable() {
        ExplainAnalyzer analyzer = new ExplainAnalyzer(true, false, java.util.Arrays.asList("information_schema.", "mysql."));
        assertFalse(analyzer.shouldExplain("SELECT * FROM information_schema.tables"));
    }

    @Test
    void shouldExplain_returnsTrue_forSelect() {
        ExplainAnalyzer analyzer = new ExplainAnalyzer(true, false, Collections.singletonList("information_schema."));
        assertTrue(analyzer.shouldExplain("SELECT * FROM user WHERE id = 1"));
    }

    @Test
    void shouldExplain_isCaseInsensitive() {
        ExplainAnalyzer analyzer = new ExplainAnalyzer(true, false, Collections.singletonList("information_schema."));
        assertTrue(analyzer.shouldExplain("select * from user"));
    }

    @Test
    void analyze_returnsExplainResult() throws Exception {
        ExplainAnalyzer analyzer = new ExplainAnalyzer(true, false, Collections.singletonList("information_schema."));

        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);

        org.apache.ibatis.mapping.BoundSql boundSql = mock(org.apache.ibatis.mapping.BoundSql.class);
        org.apache.ibatis.session.Configuration config = mock(org.apache.ibatis.session.Configuration.class);
        when(boundSql.getSql()).thenReturn("SELECT * FROM user WHERE id = 1");
        when(boundSql.getParameterMappings()).thenReturn(Collections.emptyList());

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("EXPLAIN SELECT * FROM user WHERE id = 1")).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnName(1)).thenReturn("id");
        when(rs.getObject(1)).thenReturn(1);
        when(metaData.getColumnName(2)).thenReturn("type");
        when(rs.getObject(2)).thenReturn("ALL");

        String result = analyzer.analyze(boundSql, config, dataSource);

        assertNotNull(result);
        assertTrue(result.contains("id=1"));
        assertTrue(result.contains("type=ALL"));
        verify(stmt).executeQuery();
        verify(connection).close();
    }

    @Test
    void analyze_returnsNull_whenNotSelect() throws Exception {
        ExplainAnalyzer analyzer = new ExplainAnalyzer(true, false, Collections.singletonList("information_schema."));

        DataSource dataSource = mock(DataSource.class);
        org.apache.ibatis.mapping.BoundSql boundSql = mock(org.apache.ibatis.mapping.BoundSql.class);
        org.apache.ibatis.session.Configuration config = mock(org.apache.ibatis.session.Configuration.class);
        when(boundSql.getSql()).thenReturn("INSERT INTO user VALUES (1)");

        String result = analyzer.analyze(boundSql, config, dataSource);

        assertNull(result);
        verify(dataSource, never()).getConnection();
    }

    @Test
    void analyze_returnsNull_whenDisabled() {
        ExplainAnalyzer analyzer = new ExplainAnalyzer(false, false, Collections.singletonList("information_schema."));

        DataSource dataSource = mock(DataSource.class);
        org.apache.ibatis.mapping.BoundSql boundSql = mock(org.apache.ibatis.mapping.BoundSql.class);
        org.apache.ibatis.session.Configuration config = mock(org.apache.ibatis.session.Configuration.class);
        when(boundSql.getSql()).thenReturn("SELECT * FROM user");

        String result = analyzer.analyze(boundSql, config, dataSource);
        assertNull(result);
    }
}
