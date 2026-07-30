package com.vicinity24.api.linked.store.usertype;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.SqlTypes;
import org.hibernate.usertype.UserType;

import java.io.IOException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class JsonbUserType implements UserType<Map<String, Object>> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    @Override
    public int getSqlType() {
        return SqlTypes.JSON;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<Map<String, Object>> returnedClass() {
        return (Class<Map<String, Object>>) (Class<?>) Map.class;
    }

    @Override
    public boolean equals(Map<String, Object> x, Map<String, Object> y) {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(Map<String, Object> x) {
        return Objects.hashCode(x);
    }

    @Override
    public Map<String, Object> nullSafeGet(
            ResultSet rs,
            int position,
            SharedSessionContractImplementor session,
            Object owner
    ) throws SQLException {
        Object raw = rs.getObject(position);
        if (raw == null) {
            return new LinkedHashMap<>();
        }
        return readMap(raw.toString());
    }

    @Override
    public void nullSafeSet(
            PreparedStatement st,
            Map<String, Object> value,
            int index,
            SharedSessionContractImplementor session
    ) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
            return;
        }
        st.setObject(index, writeJson(value), Types.OTHER);
    }

    @Override
    public Map<String, Object> deepCopy(Map<String, Object> value) {
        if (value == null) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(value, MAP_TYPE);
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(Map<String, Object> value) {
        return value == null ? null : writeJson(value);
    }

    @Override
    public Map<String, Object> assemble(Serializable cached, Object owner) {
        return cached == null ? null : readMap(cached.toString());
    }

    @Override
    public Map<String, Object> replace(Map<String, Object> detached, Map<String, Object> managed, Object owner) {
        return deepCopy(detached);
    }

    private static LinkedHashMap<String, Object> readMap(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new LinkedHashMap<>();
            }
            return OBJECT_MAPPER.readValue(json, MAP_TYPE);
        } catch (IOException ex) {
            throw new HibernateException("Failed to deserialize jsonb payload", ex);
        }
    }

    private static String writeJson(Map<String, Object> value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new HibernateException("Failed to serialize jsonb payload", ex);
        }
    }
}


