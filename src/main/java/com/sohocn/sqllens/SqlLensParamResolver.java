package com.sohocn.sqllens;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;

import java.util.ArrayList;
import java.util.List;

public class SqlLensParamResolver {
    private SqlLensParamResolver() {
    }

    public static List<Object> extractAll(BoundSql boundSql, Configuration configuration) {
        List<ParameterMapping> mappings = boundSql.getParameterMappings();
        List<Object> values = new ArrayList<>(mappings.size());
        for (ParameterMapping mapping : mappings) {
            values.add(resolve(boundSql, mapping.getProperty(), configuration));
        }
        return values;
    }

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
