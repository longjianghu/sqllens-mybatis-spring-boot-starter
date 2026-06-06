package com.sohocn.sqllens.mybatis;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;

/**
 * The type Sql lens param resolver.
 *
 * @author longjianghu
 */
public class SqlLensParamResolver {
    private SqlLensParamResolver() {}

    /**
     * Extract all list.
     *
     * @param boundSql
     *            the bound SQL
     * @param configuration
     *            the configuration
     * @return the list
     */
    public static List<Object> extractAll(BoundSql boundSql, Configuration configuration) {
        List<ParameterMapping> mappings = boundSql.getParameterMappings();
        List<Object> values = new ArrayList<>(mappings.size());
        for (ParameterMapping mapping : mappings) {
            values.add(resolve(boundSql, mapping.getProperty(), configuration));
        }
        return values;
    }

    /**
     * Resolve object.
     *
     * @param boundSql
     *            the bound SQL
     * @param property
     *            the property
     * @param configuration
     *            the configuration
     * @return the object
     */
    public static Object resolve(BoundSql boundSql, String property, Configuration configuration) {
        if (boundSql.hasAdditionalParameter(property)) {
            return boundSql.getAdditionalParameter(property);
        }

        Object parameterObject = boundSql.getParameterObject();
        if (parameterObject == null) {
            return null;
        }

        MetaObject metaObject = configuration.newMetaObject(parameterObject);
        if (metaObject.hasGetter(property)) {
            return metaObject.getValue(property);
        }

        return null;
    }
}
